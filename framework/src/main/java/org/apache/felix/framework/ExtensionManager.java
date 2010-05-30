/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.framework;

import java.io.IOException;
import java.net.InetAddress;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.AccessControlException;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.felix.framework.Felix.FelixResolver;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.CapabilityImpl;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.resolver.Content;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

/**
 * The ExtensionManager class is used in several ways.
 * <p>
 * First, a private instance is added (as URL with the instance as
 * URLStreamHandler) to the classloader that loaded the class.
 * It is assumed that this is an instance of URLClassloader (if not extension
 * bundles will not work). Subsequently, extension bundles can be managed by
 * instances of this class (their will be one instance per framework instance).
 * </p>
 * <p>
 * Second, it is used as module definition of the systembundle. Added extension
 * bundles with exported packages will contribute their exports to the
 * systembundle export.
 * </p>
 * <p>
 * Third, it is used as content loader of the systembundle. Added extension
 * bundles exports will be available via this loader.
 * </p>
 */
// The general approach is to have one private static instance that we register
// with the parent classloader and one instance per framework instance that
// keeps track of extension bundles and systembundle exports for that framework
// instance.
class ExtensionManager extends URLStreamHandler implements Content
{
    // The private instance that is added to Felix.class.getClassLoader() -
    // will be null if extension bundles are not supported (i.e., we are not
    // loaded by an instance of URLClassLoader)
    static final ExtensionManager m_extensionManager;

    static
    {
        // pre-init the url sub-system as otherwise we don't work on gnu/classpath
        ExtensionManager extensionManager = new ExtensionManager();
        try 
        {
            (new URL("http://felix.extensions:9/")).openConnection();
        }
        catch (Throwable t)
        {
            // This doesn't matter much - we only need the above to init the url subsystem
        }
        
        // We use the secure action of Felix to add a new instance to the parent
        // classloader.
        try
        {
            Felix.m_secureAction.addURLToURLClassLoader(Felix.m_secureAction.createURL(
                Felix.m_secureAction.createURL(null, "http:", extensionManager),
                "http://felix.extensions:9/", extensionManager),
                Felix.class.getClassLoader());
        }
        catch (Throwable ex)
        {
            // extension bundles will not be supported.
            extensionManager = null;
        }
        m_extensionManager = extensionManager;
    }

    private final Logger m_logger;
    private final Map m_headerMap = new StringMap(false);
    private final Module m_systemBundleModule;
    private List<Capability> m_capabilities = null;
    private Set m_exportNames = null;
    private Object m_securityContext = null;
    private final List m_extensions;
    private volatile Bundle[] m_extensionsCache;
    private final Set m_names;
    private final Map m_sourceToExtensions;

    // This constructor is only used for the private instance added to the parent
    // classloader.
    private ExtensionManager()
    {
        m_logger = null;
        m_systemBundleModule = null;
        m_extensions = new ArrayList();
        m_extensionsCache = new Bundle[0];
        m_names = new HashSet();
        m_sourceToExtensions = new HashMap();
    }

    /**
     * This constructor is used to create one instance per framework instance.
     * The general approach is to have one private static instance that we register
     * with the parent classloader and one instance per framework instance that
     * keeps track of extension bundles and systembundle exports for that framework
     * instance.
     *
     * @param logger the logger to use.
     * @param config the configuration to read properties from.
     * @param systemBundleInfo the info to change if we need to add exports.
     */
    ExtensionManager(Logger logger, Felix felix)
    {
        m_systemBundleModule = new ExtensionManagerModule(felix);
        m_extensions = null;
        m_extensionsCache = null;
        m_names = null;
        m_sourceToExtensions = null;
        m_logger = logger;

// TODO: FRAMEWORK - Not all of this stuff really belongs here, probably only exports.
        // Populate system bundle header map.
        m_headerMap.put(FelixConstants.BUNDLE_VERSION,
            felix.getConfig().get(FelixConstants.FELIX_VERSION_PROPERTY));
        m_headerMap.put(FelixConstants.BUNDLE_SYMBOLICNAME,
            FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
        m_headerMap.put(FelixConstants.BUNDLE_NAME, "System Bundle");
        m_headerMap.put(FelixConstants.BUNDLE_DESCRIPTION,
            "This bundle is system specific; it implements various system services.");
        m_headerMap.put(FelixConstants.EXPORT_SERVICE,
            "org.osgi.service.packageadmin.PackageAdmin," +
            "org.osgi.service.startlevel.StartLevel," +
            "org.osgi.service.url.URLHandlers");

        // The system bundle exports framework packages as well as
        // arbitrary user-defined packages from the system class path.
        // We must construct the system bundle's export metadata.
        // Get configuration property that specifies which class path
        // packages should be exported by the system bundle.
        String syspkgs = (String) felix.getConfig().get(FelixConstants.FRAMEWORK_SYSTEMPACKAGES);
        // If no system packages were specified, load our default value.
        syspkgs = (syspkgs == null)
            ? Util.getDefaultProperty(logger, Constants.FRAMEWORK_SYSTEMPACKAGES)
            : syspkgs;
        syspkgs = (syspkgs == null) ? "" : syspkgs;
        // If any extra packages are specified, then append them.
        String extra = (String) felix.getConfig().get(FelixConstants.FRAMEWORK_SYSTEMPACKAGES_EXTRA);
        syspkgs = (extra == null) ? syspkgs : syspkgs + "," + extra;
        m_headerMap.put(FelixConstants.BUNDLE_MANIFESTVERSION, "2");
        m_headerMap.put(FelixConstants.EXPORT_PACKAGE, syspkgs);
        try
        {
            ManifestParser mp = new ManifestParser(
                m_logger, felix.getConfig(), m_systemBundleModule, m_headerMap);
            List<Capability> caps = aliasSymbolicName(mp.getCapabilities());
            setCapabilities(caps);
        }
        catch (Exception ex)
        {
            m_capabilities = new ArrayList<Capability>(0);
            m_logger.log(
                Logger.LOG_ERROR,
                "Error parsing system bundle export statement: "
                + syspkgs, ex);
        }
    }

    private static List<Capability> aliasSymbolicName(List<Capability> caps)
    {
        if (caps == null)
        {
            return new ArrayList<Capability>(0);
        }

        List<Capability> aliasCaps = new ArrayList<Capability>(caps);

        for (int capIdx = 0; capIdx < aliasCaps.size(); capIdx++)
        {
            // Get the attributes and search for bundle symbolic name.
            List<Attribute> attrs = aliasCaps.get(capIdx).getAttributes();
            for (int i = 0; i < attrs.size(); i++)
            {
                // If there is a bundle symbolic name attribute, add the
                // standard alias as a value.
                if (attrs.get(i).getName().equalsIgnoreCase(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    // Make a copy of the attribute array.
                    List<Attribute> aliasAttrs = new ArrayList<Attribute>(attrs);
                    // Add the aliased value.
                    aliasAttrs.set(i, new Attribute(
                        Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE,
                        new String[] {
                            (String) attrs.get(i).getValue(), Constants.SYSTEM_BUNDLE_SYMBOLICNAME},
                        false));
                    // Create the aliased capability to replace the old capability.
                    aliasCaps.set(capIdx, new CapabilityImpl(
                        caps.get(capIdx).getModule(),
                        caps.get(capIdx).getNamespace(),
                        caps.get(capIdx).getDirectives(),
                        aliasAttrs));
                    // Continue with the next capability.
                    break;
                }
            }
        }

        return aliasCaps;
    }

    public Module getModule()
    {
        return m_systemBundleModule;
    }

    public synchronized Object getSecurityContext()
    {
        return m_securityContext;
    }

    public synchronized void setSecurityContext(Object securityContext)
    {
        m_securityContext = securityContext;
    }

    /**
     * Add an extension bundle. The bundle will be added to the parent classloader
     * and it's exported packages will be added to the module definition
     * exports of this instance. Subsequently, they are available form the
     * instance in it's role as content loader.
     *
     * @param felix the framework instance the given extension bundle comes from.
     * @param bundle the extension bundle to add.
     * @throws BundleException if extension bundles are not supported or this is
     *          not a framework extension.
     * @throws SecurityException if the caller does not have the needed
     *          AdminPermission.EXTENSIONLIFECYCLE and security is enabled.
     * @throws Exception in case something goes wrong.
     */
    synchronized void addExtensionBundle(Felix felix, BundleImpl bundle)
        throws SecurityException, BundleException, Exception
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(
                    new AdminPermission(bundle, AdminPermission.EXTENSIONLIFECYCLE));
            }
            catch (SecurityException ex)
            {
                // TODO: SECURITY - we need to throw this exception because of the 4.2.0 ct
                throw new AccessControlException(ex.getMessage());
            }
        }

        if (!((BundleProtectionDomain) bundle.getProtectionDomain()).impliesDirect(new AllPermission()))
        {
            throw new SecurityException("Extension Bundles must have AllPermission");
        }

        Directive dir = ManifestParser.parseExtensionBundleHeader((String)
            bundle.getCurrentModule().getHeaders().get(Constants.FRAGMENT_HOST));

        // We only support classpath extensions (not bootclasspath).
        if (!Constants.EXTENSION_FRAMEWORK.equals(dir.getValue()))
        {
            throw new BundleException("Unsupported Extension Bundle type: " +
                dir.getValue(), new UnsupportedOperationException(
                "Unsupported Extension Bundle type!"));
        }

        try
        {
            // Merge the exported packages with the exported packages of the systembundle.
            List<Capability> exports = null;
            try
            {
                exports = ManifestParser.parseExportHeader(
                    m_logger, m_systemBundleModule,
                    (String) bundle.getCurrentModule().getHeaders().get(Constants.EXPORT_PACKAGE),
                    m_systemBundleModule.getSymbolicName(), m_systemBundleModule.getVersion());
                exports = aliasSymbolicName(exports);
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Error parsing extension bundle export statement: "
                    + bundle.getCurrentModule().getHeaders().get(Constants.EXPORT_PACKAGE), ex);
                return;
            }

            // Add the bundle as extension if we support extensions
            if (m_extensionManager != null)
            {
                // This needs to be the private instance.
                m_extensionManager.addExtension(felix, bundle);
            }
            else
            {
                // We don't support extensions (i.e., the parent is not an URLClassLoader).
                m_logger.log(Logger.LOG_WARNING,
                    "Unable to add extension bundle to FrameworkClassLoader - Maybe not an URLClassLoader?");
                throw new UnsupportedOperationException(
                    "Unable to add extension bundle to FrameworkClassLoader - Maybe not an URLClassLoader?");
            }
            List<Capability> temp = new ArrayList<Capability>(m_capabilities.size() + exports.size());
            temp.addAll(m_capabilities);
            temp.addAll(exports);
            setCapabilities(temp);
        }
        catch (Exception ex)
        {
            throw ex;
        }

        felix.setBundleStateAndNotify(bundle, Bundle.RESOLVED);
    }

    /**
     * This is a Felix specific extension mechanism that allows extension bundles
     * to have activators and be started via this method.
     *
     * @param felix the framework instance the extension bundle is installed in.
     * @param bundle the extension bundle to start if it has a Felix specific activator.
     */
    void startExtensionBundle(Felix felix, BundleImpl bundle)
    {
        String activatorClass = (String)
        bundle.getCurrentModule().getHeaders().get(
            FelixConstants.FELIX_EXTENSION_ACTIVATOR);

        if (activatorClass != null)
        {
            try
            {
// TODO: SECURITY - Should this consider security?
                BundleActivator activator = (BundleActivator)
                    felix.getClass().getClassLoader().loadClass(
                        activatorClass.trim()).newInstance();

// TODO: EXTENSIONMANAGER - This is kind of hacky, can we improve it?
                felix.m_activatorList.add(activator);

                BundleContext context = felix._getBundleContext();

                bundle.setBundleContext(context);

                if ((felix.getState() == Bundle.ACTIVE) || (felix.getState() == Bundle.STARTING))
                {
                    Felix.m_secureAction.startActivator(activator, context);
                }
            }
            catch (Throwable ex)
            {
                m_logger.log(Logger.LOG_WARNING,
                    "Unable to start Felix Extension Activator", ex);
            }
        }
    }

    /**
     * Remove all extension registered by the given framework instance. Note, it
     * is not possible to unregister allready loaded classes form those extensions.
     * That is why the spec requires a JVM restart.
     *
     * @param felix the framework instance whose extensions need to be unregistered.
     */
    void removeExtensions(Felix felix)
    {
        if (m_extensionManager != null)
        {
            m_extensionManager._removeExtensions(felix);
        }
    }

    private void setCapabilities(List<Capability> capabilities)
    {
        m_capabilities = capabilities;
        m_headerMap.put(Constants.EXPORT_PACKAGE, convertCapabilitiesToHeaders(m_headerMap));
    }

    private String convertCapabilitiesToHeaders(Map headers)
    {
        StringBuffer exportSB = new StringBuffer("");
        Set exportNames = new HashSet();

        for (int i = 0; (m_capabilities != null) && (i < m_capabilities.size()); i++)
        {
            if (m_capabilities.get(i).getNamespace().equals(Capability.PACKAGE_NAMESPACE))
            {
                // Add a comma separate if there is an existing package.
                if (exportSB.length() > 0)
                {
                    exportSB.append(", ");
                }

                // Append exported package information.
                exportSB.append(m_capabilities.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue());
                exportSB.append("; version=\"");
                exportSB.append(m_capabilities.get(i).getAttribute(Capability.VERSION_ATTR).getValue());
                exportSB.append("\"");

                // Remember exported packages.
                exportNames.add(m_capabilities.get(i).getAttribute(Capability.PACKAGE_ATTR).getValue());
            }
        }

        m_exportNames = exportNames;

        return exportSB.toString();
    }

    //
    // Classpath Extension
    //

    /*
     * See whether any registered extension provides the class requested. If not
     * throw an IOException.
     */
    public URLConnection openConnection(URL url) throws IOException
    {
        String path = url.getPath();

        if (path.trim().equals("/"))
        {
            return new URLHandlersBundleURLConnection(url);
        }

        Bundle[] extensions = m_extensionsCache;
        URL result = null;
        for (Bundle extBundle : extensions)
        {
            try
            {
                result = ((ModuleImpl) ((BundleImpl) extBundle).getCurrentModule()).getResourceLocal(path);
            }
            catch (Exception ex)
            {
                // Maybe the bundle went away, so ignore this exception.
            }
            if (result != null)
            {
                return result.openConnection();
            }
        }

        throw new IOException("Resource not provided by any extension!");
    }

    protected InetAddress getHostAddress(URL u)
    {
        // the extension URLs do not address real hosts
        return null;
    }

    private synchronized void addExtension(Object source, Bundle extension)
    {
        List sourceExtensions = (List) m_sourceToExtensions.get(source);

        if (sourceExtensions == null)
        {
            sourceExtensions = new ArrayList();
            m_sourceToExtensions.put(source, sourceExtensions);
        }

        sourceExtensions.add(extension);

        _add(extension.getSymbolicName(), extension);
        m_extensionsCache = (Bundle[])
                m_extensions.toArray(new Bundle[m_extensions.size()]);
    }

    private synchronized void _removeExtensions(Object source)
    {
        if (m_sourceToExtensions.remove(source) == null)
        {
            return;
        }

        m_extensions.clear();
        m_names.clear();

        for (Iterator iter = m_sourceToExtensions.values().iterator(); iter.hasNext();)
        {
            List extensions = (List) iter.next();
            for (Iterator extIter = extensions.iterator(); extIter.hasNext();)
            {
                Bundle bundle = (Bundle) extIter.next();
                _add(bundle.getSymbolicName(), bundle);
            }
            m_extensionsCache = (Bundle[])
                m_extensions.toArray(new Bundle[m_extensions.size()]);
        }
    }

    private void _add(String name, Bundle extension)
    {
        if (!m_names.contains(name))
        {
            m_names.add(name);
            m_extensions.add(extension);
        }
    }

    public void close()
    {
        // Do nothing on close, since we have nothing open.
    }

    public Enumeration getEntries()
    {
        return new Enumeration()
        {
            public boolean hasMoreElements()
            {
                return false;
            }

            public Object nextElement() throws NoSuchElementException
            {
                throw new NoSuchElementException();
            }
        };
    }

    public boolean hasEntry(String name) {
        return false;
    }

    public byte[] getEntryAsBytes(String name)
    {
        return null;
    }

    public InputStream getEntryAsStream(String name) throws IOException
    {
        return null;
    }

    public Content getEntryAsContent(String name)
    {
        return null;
    }

    public String getEntryAsNativeLibrary(String name)
    {
        return null;
    }

    //
    // Utility methods.
    //

    class ExtensionManagerModule extends ModuleImpl
    {
        private final Version m_version;
        ExtensionManagerModule(Felix felix)
        {
            super(m_logger, felix.getConfig(), felix, "0",
                felix.getBootPackages(), felix.getBootPackageWildcards());
            m_version = new Version((String)
                felix.getConfig().get(FelixConstants.FELIX_VERSION_PROPERTY));
        }

        public Map getHeaders()
        {
            synchronized (ExtensionManager.this)
            {
                return m_headerMap;
            }
        }

        public List<Capability> getCapabilities()
        {
            synchronized (ExtensionManager.this)
            {
                return m_capabilities;
            }
        }

        public String getSymbolicName()
        {
            return FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME;
        }

        public Version getVersion()
        {
            return m_version;
        }

        public Class getClassByDelegation(String name) throws ClassNotFoundException
        {
            Class clazz = null;
            String pkgName = Util.getClassPackage(name);
            if (shouldBootDelegate(pkgName))
            {
                try
                {
                    // Get the appropriate class loader for delegation.
                    ClassLoader bdcl = getBootDelegationClassLoader();
                    clazz = bdcl.loadClass(name);
                    // If this is a java.* package, then always terminate the
                    // search; otherwise, continue to look locally if not found.
                    if (pkgName.startsWith("java.") || (clazz != null))
                    {
                        return clazz;
                    }
                }
                catch (ClassNotFoundException ex)
                {
                    // If this is a java.* package, then always terminate the
                    // search; otherwise, continue to look locally if not found.
                    if (pkgName.startsWith("java."))
                    {
                        throw ex;
                    }
                }
            }
            if (clazz == null)
            {
                synchronized (ExtensionManager.this)
                {
                    if (!m_exportNames.contains(Util.getClassPackage(name)))
                    {
                        throw new ClassNotFoundException(name);
                    }
                }

                clazz = getClass().getClassLoader().loadClass(name);
            }
            return clazz;
        }

        public URL getResourceByDelegation(String name)
        {
            return getClass().getClassLoader().getResource(name);
        }

        public Enumeration getResourcesByDelegation(String name)
        {
           try
           {
               return getClass().getClassLoader().getResources(name);
           }
           catch (IOException ex)
           {
               return null;
           }
        }

        public Logger getLogger()
        {
            return m_logger;
        }

        public Map getConfig()
        {
            return null;
        }

        public FelixResolver getResolver()
        {
            return null;
        }

        public void attachFragmentContents(Content[] fragmentContents)
            throws Exception
        {
            throw new UnsupportedOperationException("Should not be used!");
        }

        public void close()
        {
            // Nothing needed here.
        }

        public Content getContent()
        {
            return ExtensionManager.this;
        }

        public URL getEntry(String name)
        {
            // There is no content for the system bundle, so return null.
            return null;
        }

        public boolean hasInputStream(int index, String urlPath)
        {
            return (getClass().getClassLoader().getResource(urlPath) != null);
        }

        public InputStream getInputStream(int index, String urlPath)
        {
            return getClass().getClassLoader().getResourceAsStream(urlPath);
        }
    }
}
