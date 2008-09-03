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

import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.framework.util.manifestparser.ManifestParser;
import org.apache.felix.framework.util.manifestparser.R4Attribute;
import org.apache.felix.framework.util.manifestparser.R4Directive;
import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IContent;
import org.apache.felix.moduleloader.IContentLoader;
import org.apache.felix.moduleloader.IModuleDefinition;
import org.apache.felix.moduleloader.IRequirement;
import org.apache.felix.moduleloader.ISearchPolicy;
import org.apache.felix.moduleloader.IURLPolicy;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

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
class ExtensionManager extends URLStreamHandler implements IModuleDefinition, IContentLoader, IContent
{
    // The private instance that is added to Felix.class.getClassLoader() -
    // will be null if extension bundles are not supported (i.e., we are not 
    // loaded by an instance of URLClassLoader)
    static final ExtensionManager m_extensionManager;
    
    static
    {
        // We use the secure action of Felix to add a new instance to the parent 
        // classloader. 
        ExtensionManager extensionManager = new ExtensionManager();
        try
        {
            Felix.m_secureAction.addURLToURLClassLoader(Felix.m_secureAction.createURL(
                Felix.m_secureAction.createURL(null, "felix:", extensionManager),
                "felix://extensions/", extensionManager),
                Felix.class.getClassLoader());
        }
        catch (Exception ex) 
        {
            // extension bundles will not be supported. 
            extensionManager = null;
        }
        m_extensionManager = extensionManager;
    }

    private Logger m_logger = null;
    private BundleInfo m_systemBundleInfo = null;
    private ICapability[] m_capabilities = null;
    private Set m_exportNames = null;
    private ISearchPolicy m_searchPolicy = null;
    private IURLPolicy m_urlPolicy = null;
    private Object m_securityContext = null;
    private final List m_extensions;
    private final Set m_names;
    private final Map m_sourceToExtensions;

    // This constructor is only used for the private instance added to the parent
    // classloader.
    private ExtensionManager()
    {
        m_extensions = new ArrayList();
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
    ExtensionManager(Logger logger, Map configMap, BundleInfo systemBundleInfo)
    {
        m_extensions = null;
        m_names = null;
        m_sourceToExtensions = null;
        m_logger = logger;
        m_systemBundleInfo = systemBundleInfo;

// TODO: FRAMEWORK - Not all of this stuff really belongs here, probably only exports.
        // Populate system bundle header map.
        // Note: This is a reference to the actual header map,
        // so these changes are saved. Kind of hacky.
        Map map = ((SystemBundleArchive) m_systemBundleInfo.getArchive()).getManifestHeader(0);
        // Initialize header map as a case insensitive map.
        map.put(FelixConstants.BUNDLE_VERSION,
            configMap.get(FelixConstants.FELIX_VERSION_PROPERTY));
        map.put(FelixConstants.BUNDLE_SYMBOLICNAME,
            FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
        map.put(FelixConstants.BUNDLE_NAME, "System Bundle");
        map.put(FelixConstants.BUNDLE_DESCRIPTION,
            "This bundle is system specific; it implements various system services.");
        map.put(FelixConstants.EXPORT_SERVICE,
            "org.osgi.service.packageadmin.PackageAdmin," +
            "org.osgi.service.startlevel.StartLevel," +
            "org.osgi.service.url.URLHandlers");

        // The system bundle exports framework packages as well as
        // arbitrary user-defined packages from the system class path.
        // We must construct the system bundle's export metadata.
        // Get system property that specifies which class path
        // packages should be exported by the system bundle.
        try
        {
            setCapabilities(
                addModuleCapability(map,
                    ManifestParser.parseExportHeader(
                        (String) configMap.get(Constants.FRAMEWORK_SYSTEMPACKAGES))));
        }
        catch (Exception ex)
        {
            m_capabilities = new ICapability[0];
            m_logger.log(
                Logger.LOG_ERROR,
                "Error parsing system bundle export statement: "
                + configMap.get(Constants.FRAMEWORK_SYSTEMPACKAGES), ex);
        }
    }

    private ICapability[] addModuleCapability(Map headerMap, ICapability[] caps)
    {
        try
        {
            // First parse the symbolic name header.
            ICapability moduleCap = ManifestParser.parseBundleSymbolicName(headerMap);
            // Create a copy of the module capability with the standard
            // alias for the bundle symbolic name.
            R4Directive[] dirs = ((Capability) moduleCap).getDirectives();
            R4Attribute[] attrs = ((Capability) moduleCap).getAttributes();
            R4Attribute[] attrsAlias = new R4Attribute[attrs.length];
            System.arraycopy(attrs, 0, attrsAlias, 0, attrs.length);
            // Modify the alias attributes to have the standard symbolic name.
            for (int i = 0; i < attrsAlias.length; i++)
            {
                if (attrsAlias[i].getName().equalsIgnoreCase(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    attrsAlias[i] = new R4Attribute(
                        Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, Constants.SYSTEM_BUNDLE_SYMBOLICNAME, false);
                }
            }
            // Create the alias capability.
            ICapability aliasCap = new Capability(ICapability.MODULE_NAMESPACE, dirs, attrsAlias);
            // Finally, add the module and alias capabilities to the
            // existing capabilities.
            ICapability[] temp = new ICapability[caps.length + 2];
            System.arraycopy(caps, 0, temp, 2, caps.length);
            temp[0] = moduleCap;
            temp[1] = aliasCap;
            return temp;
        }
        catch (BundleException ex)
        {
            // This should not happen, but in case it does, then just
            // return the original array of capabilities.
            return caps;
        }
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
     * Check whether the given manifest headers are from an extension bundle.
     */
    boolean isExtensionBundle(Map headers)
    {
        R4Directive dir = ManifestParser.parseExtensionBundleHeader((String)
            headers.get(Constants.FRAGMENT_HOST));

        return (dir != null) && (Constants.EXTENSION_FRAMEWORK.equals(
            dir.getValue()) || Constants.EXTENSION_BOOTCLASSPATH.equals(
            dir.getValue()));
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
    void addExtensionBundle(Felix felix, FelixBundle bundle) 
        throws SecurityException, BundleException, Exception
    {
        Object sm = System.getSecurityManager();
        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(
                new AdminPermission(bundle, AdminPermission.EXTENSIONLIFECYCLE));
        }

        if (!((BundleProtectionDomain) bundle.getInfo().getProtectionDomain()).impliesDirect(new AllPermission()))
        {
            throw new SecurityException("Extension Bundles must have AllPermission");
        }

        R4Directive dir = ManifestParser.parseExtensionBundleHeader((String)
            bundle.getInfo().getCurrentHeader().get(Constants.FRAGMENT_HOST));

        // We only support classpath extensions (not bootclasspath).
        if (!Constants.EXTENSION_FRAMEWORK.equals(dir.getValue()))
        {
            throw new BundleException("Unsupported Extension Bundle type: " +
                dir.getValue(), new UnsupportedOperationException(
                "Unsupported Extension Bundle type!"));
        }

        // Not sure whether this is a good place to do it but we need to lock
        felix.acquireBundleLock(felix);

        try
        {
            bundle.getInfo().setExtension(true);

            SystemBundleArchive systemArchive =
                (SystemBundleArchive) felix.getInfo().getArchive();

            // Merge the exported packages with the exported packages of the systembundle.
            Map headers = null;
            ICapability[] exports = null;
            try
            {
                exports = ManifestParser.parseExportHeader((String)
                    bundle.getInfo().getCurrentHeader().get(Constants.EXPORT_PACKAGE));
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Error parsing extension bundle export statement: "
                    + bundle.getInfo().getCurrentHeader().get(Constants.EXPORT_PACKAGE), ex);
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
            ICapability[] temp = new ICapability[getCapabilities().length + exports.length];
            System.arraycopy(getCapabilities(), 0, temp, 0, getCapabilities().length);
            System.arraycopy(exports, 0, temp, getCapabilities().length, exports.length);
            setCapabilities(temp);
        }
        catch (Exception ex)
        {
            bundle.getInfo().setExtension(false);
            throw ex;
        }
        finally
        {
            felix.releaseBundleLock(felix);
        }
    
        bundle.getInfo().setState(Bundle.RESOLVED);
    }

    /**
     * This is a Felix specific extension mechanism that allows extension bundles
     * to have activators and be started via this method.
     * 
     * @param felix the framework instance the extension bundle is installed in.
     * @param bundle the extension bundle to start if it has a Felix specific activator.
     */
    void startExtensionBundle(Felix felix, FelixBundle bundle)
    {
        String activatorClass = (String)
        bundle.getInfo().getCurrentHeader().get(
            FelixConstants.FELIX_EXTENSION_ACTIVATOR);
        
        if (activatorClass != null)
        {
            try
            {
                BundleActivator activator = (BundleActivator)
                    felix.getClass().getClassLoader().loadClass(
                        activatorClass.trim()).newInstance();
                
                felix.m_activatorList.add(activator);
                
                BundleContext context = m_systemBundleInfo.getBundleContext();
                
                bundle.getInfo().setBundleContext(context);
                
                if (felix.getInfo().getState() == Bundle.ACTIVE)
                {
                    Felix.m_secureAction.startActivator(activator, context);
                }
            }
            catch (Throwable ex)
            {
                ex.printStackTrace();
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

    //
    // IModuleDefinition
    //
    public ICapability[] getCapabilities()
    {
        return m_capabilities;
    }

    void setCapabilities(ICapability[] capabilities)
    {
        m_capabilities = capabilities;

        // Note: This is a reference to the actual header map,
        // so these changes are saved. Kind of hacky.
        Map map = ((SystemBundleArchive) m_systemBundleInfo.getArchive()).getManifestHeader(0);
        map.put(Constants.EXPORT_PACKAGE, convertCapabilitiesToHeaders(map));
    }

    public IRequirement[] getDynamicRequirements()
    {
        return null;
    }

    public R4Library[] getLibraries()
    {
        return null;
    }

    public IRequirement[] getRequirements()
    {
        return null;
    }

    private String convertCapabilitiesToHeaders(Map headers)
    {
        StringBuffer exportSB = new StringBuffer("");
        Set exportNames = new HashSet();

        for (int i = 0; (m_capabilities != null) && (i < m_capabilities.length); i++)
        {
            if (m_capabilities[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                // Add a comma separate if there is an existing package.
                if (exportSB.length() > 0)
                {
                    exportSB.append(", ");
                }

                // Append exported package information.
                exportSB.append(((Capability) m_capabilities[i]).getPackageName());
                exportSB.append("; version=\"");
                exportSB.append(((Capability) m_capabilities[i]).getPackageVersion().toString());
                exportSB.append("\"");

                // Remember exported packages.
                exportNames.add(((Capability) m_capabilities[i]).getPackageName());
            }
        }

        m_exportNames = exportNames;

        return exportSB.toString();
    }
    
    //
    // IContentLoader
    //
    
    public void open()
    {
        // Nothing needed here.
    }

    public void close()
    {
        // Nothing needed here.
    }

    public IContent getContent()
    {
        return this;
    }

    public ISearchPolicy getSearchPolicy()
    {
        return m_searchPolicy;
    }

    public void setSearchPolicy(ISearchPolicy searchPolicy)
    {
        m_searchPolicy = searchPolicy;
    }

    public IURLPolicy getURLPolicy()
    {
        return m_urlPolicy;
    }

    public void setURLPolicy(IURLPolicy urlPolicy)
    {
        m_urlPolicy = urlPolicy;
    }

    public Class getClass(String name)
    {
        if (!m_exportNames.contains(Util.getClassPackage(name)))
        {
            return null;
        }

        try
        {
            return getClass().getClassLoader().loadClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public URL getResource(String name)
    {
        return getClass().getClassLoader().getResource(name);
    }

    public Enumeration getResources(String name)
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

    public URL getResourceFromContent(String name)
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

    public String findLibrary(String name)
    {
        // No native libs associated with the system bundle.
        return null;
    }
    
    //
    // Classpath Extension
    //

    /*
     * See whether any registered extension provides the class requested. If not
     * throw an IOException.
     */
    protected synchronized URLConnection openConnection(URL url) throws IOException
    {
        String path = url.getPath();

        if (path.trim().equals("/"))
        {
            return new URLHandlersBundleURLConnection(url);
        }

        for (Iterator iter = m_extensions.iterator(); iter.hasNext();)
        {
            URL result = ((FelixBundle) iter.next()).getInfo().getCurrentModule().getContentLoader().getResourceFromContent(path);

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

    public IContent getEntryAsContent(String name)
    {
        return null;
    }

    public String getEntryAsNativeLibrary(String name)
    {
        return null;
    }
}