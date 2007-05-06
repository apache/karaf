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
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

import org.apache.felix.framework.cache.*;
import org.apache.felix.framework.util.*;
import org.apache.felix.framework.util.manifestparser.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.*;

class SystemBundle extends BundleImpl implements IModuleDefinition, PrivilegedAction
{
    private List m_activatorList = null;
    private SystemBundleActivator m_activator = null;
    private Thread m_shutdownThread = null;
    private ICapability[] m_exports = null;
    private IContentLoader m_contentLoader = null;
    private Set m_exportNames = null;

    protected SystemBundle(Felix felix, BundleInfo info, List activatorList)
    {
        super(felix, info);

        // Create an activator list if necessary.
        if (activatorList == null)
        {
            activatorList = new ArrayList();
        }

        // Add the bundle activator for the package admin service.
        activatorList.add(new PackageAdminActivator(felix));

        // Add the bundle activator for the start level service.
        activatorList.add(new StartLevelActivator(felix));

        // Add the bundle activator for the url handler service.
        activatorList.add(new URLHandlersActivator(felix));

        m_activatorList = activatorList;

        // The system bundle exports framework packages as well as
        // arbitrary user-defined packages from the system class path.
        // We must construct the system bundle's export metadata.

        // Get system property that specifies which class path
        // packages should be exported by the system bundle.
        try
        {
            m_exports = ManifestParser.parseExportHeader(
                getFelix().getConfig().get(Constants.FRAMEWORK_SYSTEMPACKAGES));
        }
        catch (Exception ex)
        {
            m_exports = new ICapability[0];
            getFelix().getLogger().log(
                Logger.LOG_ERROR,
                "Error parsing system bundle export statement: "
                + getFelix().getConfig().get(Constants.FRAMEWORK_SYSTEMPACKAGES), ex);
        }

        m_contentLoader = new SystemBundleContentLoader();

        // Initialize header map as a case insensitive map.
        Map map = new StringMap(false);
        map.put(FelixConstants.BUNDLE_VERSION,
            getFelix().getConfig().get(FelixConstants.FELIX_VERSION_PROPERTY));
        map.put(FelixConstants.BUNDLE_SYMBOLICNAME,
            FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
        map.put(FelixConstants.BUNDLE_NAME, "System Bundle");
        map.put(FelixConstants.BUNDLE_DESCRIPTION,
            "This bundle is system specific; it implements various system services.");
        map.put(FelixConstants.EXPORT_SERVICE, "org.osgi.service.packageadmin.PackageAdmin,org.osgi.service.startlevel.StartLevel");

        parseAndAddExports(map);

        ((SystemBundleArchive) getInfo().getArchive()).setManifestHeader(map);
    }

    private void parseAndAddExports(Map headers)
    {
        StringBuffer exportSB = new StringBuffer("");
        Set exportNames = new HashSet();

        for (int i = 0; i < m_exports.length; i++)
        {
            if (i > 0)
            {
                exportSB.append(", ");
            }

            exportSB.append(((Capability) m_exports[i]).getPackageName());
            exportSB.append("; version=\"");
            exportSB.append(((Capability) m_exports[i]).getPackageVersion().toString());
            exportSB.append("\"");

            exportNames.add(((Capability) m_exports[i]).getPackageName());
        }

        m_exportNames = exportNames;

        headers.put(FelixConstants.EXPORT_PACKAGE, exportSB.toString());
    }

    public ICapability[] getExports()
    {
        return m_exports;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    public void start() throws BundleException
    {
        // The system bundle is only started once and it
        // is started by the framework.
        if (getState() == Bundle.ACTIVE)
        {
            return;
        }

        getInfo().setState(Bundle.STARTING);

        try
        {
            getInfo().setContext(new BundleContextImpl(getFelix(), this));
            getActivator().start(getInfo().getContext());
        }
        catch (Throwable throwable)
        {
            throw new BundleException(
                "Unable to start system bundle.", throwable);
        }

        // Do NOT set the system bundle state to active yet, this
        // must be done after all other bundles have been restarted.
        // This will be done after the framework is initialized.
    }

    /**
     * According to the spec, this method asynchronously stops the framework.
     * To prevent multiple creations of useless separate threads in case of
     * multiple calls to this method, the shutdown thread is only started if
     * the framework is still in running state.
     */
    public void stop()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        // Spec says stop() on SystemBundle should return immediately and
        // shutdown framework on another thread.
        if (getFelix().getStatus() == Felix.RUNNING_STATUS)
        {
            // Initial call of stop, so kick off shutdown.
            m_shutdownThread = new Thread("FelixShutdown") {
                public void run()
                {
                    try
                    {
                        getFelix().shutdown();
                    }
                    catch (Exception ex)
                    {
                        getFelix().getLogger().log(
                            Logger.LOG_ERROR,
                            "SystemBundle: Error while shutting down.", ex);
                    }
                }
            };
            getInfo().setState(Bundle.STOPPING);
            m_shutdownThread.start();
        }
    }

    public void uninstall() throws BundleException
    {
        throw new BundleException("Cannot uninstall the system bundle.");
    }

    public void update() throws BundleException
    {
        update(null);
    }

    public void update(InputStream is) throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        // TODO: This is supposed to stop and then restart the framework.
        throw new BundleException("System bundle update not implemented yet.");
    }

    protected BundleActivator getActivator()
    {
        if (m_activator == null)
        {
            m_activator = new SystemBundleActivator(m_activatorList);
        }
        return m_activator;
    }

    /**
     * Actually shuts down the system bundle. This method does what actually
     * the {@link #stop()} method would do for regular bundles. Since the system
     * bundle has to shutdown the framework, a separate method is used to stop
     * the system bundle during framework shutdown.
     *
     * @throws BundleException If an error occurrs stopping the system bundle
     *      and any activators "started" on framework start time.
     */
    void shutdown() throws BundleException
    {
        // Callback from shutdown thread, so do our own stop.
        try
        {
            getFelix().m_secureAction.stopActivator(getActivator(),
                getInfo().getContext());
        }
        catch (Throwable throwable)
        {
            throw new BundleException( "Unable to stop system bundle.", throwable );
        }
    }

    boolean exports(String packageName)
    {
        return m_exportNames.contains(packageName);
    }

    public ICapability[] getCapabilities()
    {
        return m_exports;
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

    private final ThreadLocal m_tempBundle = new ThreadLocal();

    void addExtensionBundle(BundleImpl bundle)
    {
        if (System.getSecurityManager() != null)
        {
            m_tempBundle.set(bundle);

            try
            {
                AccessController.doPrivileged(this);
            }
            finally
            {
                m_tempBundle.set(null);
            }
        }
        else
        {
            _addExtensionBundle(bundle);
        }
    }
    
    void startExtensionBundle(BundleImpl bundle) 
    {
        String activatorClass = (String)
        bundle.getInfo().getCurrentHeader().get(
            FelixConstants.FELIX_EXTENSION_ACTIVATOR);
        
        if (activatorClass != null)
        {
            try
            {
                m_activator.addActivator(((BundleActivator)
                    getClass().getClassLoader().loadClass(
                    activatorClass.trim()).newInstance()),
                    new BundleContextImpl(getFelix(), bundle));
            }
            catch (Throwable ex)
            {
                getFelix().getLogger().log(Logger.LOG_WARNING,
                    "Unable to start Felix Extension Activator", ex);
            }
        }
    }

    public Object run()
    {
        _addExtensionBundle((BundleImpl) m_tempBundle.get());
        return null;
    }

    private void _addExtensionBundle(BundleImpl bundle)
    {
        SystemBundleArchive systemArchive =
            (SystemBundleArchive) getInfo().getArchive();

        Map headers;
        ICapability[] exports;
        try
        {
            headers = new StringMap(systemArchive.getManifestHeader(
                systemArchive.getRevisionCount() - 1), false);

            exports = ManifestParser.parseExportHeader((String)
                bundle.getInfo().getCurrentHeader().get(Constants.EXPORT_PACKAGE));
        }
        catch (Exception ex)
        {
            getFelix().getLogger().log(
                Logger.LOG_ERROR,
                "Error parsing extension bundle export statement: "
                + bundle.getInfo().getCurrentHeader().get(Constants.EXPORT_PACKAGE), ex);

            return;
        }

        try
        {
            Method addURL =
                URLClassLoader.class.getDeclaredMethod("addURL",
                new Class[] {URL.class});
            addURL.setAccessible(true);
            addURL.invoke(getClass().getClassLoader(),
                new Object[] {bundle.getEntry("/")});
        }
        catch (Exception ex)
        {
            getFelix().getLogger().log(Logger.LOG_WARNING,
                "Unable to add extension bundle to FrameworkClassLoader - Maybe not an URLClassLoader?", ex);
            throw new UnsupportedOperationException(
                "Unable to add extension bundle to FrameworkClassLoader - Maybe not an URLClassLoader?");
        }

        ICapability[] temp = new ICapability[m_exports.length + exports.length];

        System.arraycopy(m_exports, 0, temp, 0, m_exports.length);
        System.arraycopy(exports, 0, temp, m_exports.length, exports.length);

        m_exports = temp;

        parseAndAddExports(headers);

        systemArchive.setManifestHeader(headers);
    }

    private class SystemBundleContentLoader implements IContentLoader
    {
        private ISearchPolicy m_searchPolicy = null;
        private IURLPolicy m_urlPolicy = null;

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
            return null;
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
                getFelix().getLogger().log(
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

        public boolean hasInputStream(String urlPath) throws IOException
        {
            return (getClass().getClassLoader().getResource(urlPath) != null);
        }

        public InputStream getInputStream(String urlPath) throws IOException
        {
            return getClass().getClassLoader().getResourceAsStream(urlPath);
        }

        public String findLibrary(String name)
        {
            // No native libs associated with the system bundle.
            return null;
        }
    }
}