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

import java.io.InputStream;
import java.util.*;

import org.apache.felix.framework.cache.SystemBundleArchive;
import org.apache.felix.framework.searchpolicy.*;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.IContentLoader;
import org.osgi.framework.*;

class SystemBundle extends BundleImpl
{
    private List m_activatorList = null;
    private BundleActivator m_activator = null;
    private Thread m_shutdownThread = null;
    private R4Export[] m_exports = null;
    private IContentLoader m_contentLoader = null;

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

        // Add the bundle activator for the URL Handlers service.
        activatorList.add(new URLHandlersActivator(felix));

        m_activatorList = activatorList;

        // The system bundle exports framework packages as well as
        // arbitrary user-defined packages from the system class path.
        // We must construct the system bundle's export metadata.

        // Get system property that specifies which class path
        // packages should be exported by the system bundle.
        R4Package[] classPathPkgs = null;
        try
        {
            classPathPkgs = ManifestParser.parseImportExportHeader(
                getFelix().getConfig().get(Constants.FRAMEWORK_SYSTEMPACKAGES));
        }
        catch (Exception ex)
        {
            classPathPkgs = new R4Package[0];
            getFelix().getLogger().log(
                Logger.LOG_ERROR,
                "Error parsing system bundle export statement: "
                + getFelix().getConfig().get(Constants.FRAMEWORK_SYSTEMPACKAGES), ex);
        }

        // Now, create the list of standard framework exports for
        // the system bundle.
        m_exports = new R4Export[classPathPkgs.length];

        // Copy the class path exported packages.
        for (int i = 0; i < classPathPkgs.length; i++)
        {
            m_exports[i] = new R4Export(classPathPkgs[i]);
        }

        m_contentLoader = new SystemBundleContentLoader(getFelix().getLogger());

        StringBuffer exportSB = new StringBuffer("");
        for (int i = 0; i < m_exports.length; i++)
        {
            if (i > 0)
            {
                exportSB.append(", ");
            }

            exportSB.append(m_exports[i].getName());
            exportSB.append("; version=\"");
            exportSB.append(m_exports[i].getVersion().toString());
            exportSB.append("\"");
        }

        // Initialize header map as a case insensitive map.
        Map map = new StringMap(false);
        map.put(FelixConstants.BUNDLE_VERSION,
            getFelix().getConfig().get(FelixConstants.FELIX_VERSION_PROPERTY));
        map.put(FelixConstants.BUNDLE_SYMBOLICNAME,
            FelixConstants.SYSTEM_BUNDLE_SYMBOLICNAME);
        map.put(FelixConstants.BUNDLE_NAME, "System Bundle");
        map.put(FelixConstants.BUNDLE_DESCRIPTION,
            "This bundle is system specific; it implements various system services.");
        map.put(FelixConstants.EXPORT_PACKAGE, exportSB.toString());
        map.put(FelixConstants.EXPORT_SERVICE, "org.osgi.service.packageadmin.PackageAdmin,org.osgi.service.startlevel.StartLevel");
        ((SystemBundleArchive) getInfo().getArchive()).setManifestHeader(map);

        // TODO: FRAMEWORK - We need some systematic way for framework services
        // to add packages and services to the system bundle's headers, something
        // that will allow for them to be independently configured.
    }

    public R4Export[] getExports()
    {
        return m_exports;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    public synchronized void start() throws BundleException
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
    public synchronized void stop()
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

    public synchronized void uninstall() throws BundleException
    {
        throw new BundleException("Cannot uninstall the system bundle.");
    }

    public synchronized void update() throws BundleException
    {
        update(null);
    }

    public synchronized void update(InputStream is) throws BundleException
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
            m_activator = new SystemBundleActivator(getFelix(), m_activatorList);
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
            getActivator().stop(getInfo().getContext());
        }
        catch (Throwable throwable)
        {
            throw new BundleException( "Unable to stop system bundle.", throwable );
        }
    }
}