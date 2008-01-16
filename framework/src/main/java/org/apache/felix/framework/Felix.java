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

import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.Certificate;
import java.util.*;

import org.apache.felix.framework.cache.*;
import org.apache.felix.framework.searchpolicy.*;
import org.apache.felix.framework.util.*;
import org.apache.felix.framework.util.manifestparser.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.startlevel.StartLevel;

public class Felix extends FelixBundle
{
    // The secure action used to do privileged calls
    static SecureAction m_secureAction = new SecureAction();

    // The extension manager to handle extension bundles
    private ExtensionManager m_extensionManager;

    // Logging related member variables.
    private Logger m_logger = null; // TODO: KARL - Why package private?
    // Immutable config properties.
    private Map m_configMap = null;
    // Mutable configuration properties passed into constructor.
    private Map m_configMutableMap = null;

    // MODULE FACTORY.
    private IModuleFactory m_factory = null;
    private R4SearchPolicyCore m_policyCore = null;

    // Object used as a lock when calculating which bundles
    // when performing an operation on one or more bundles.
    private Object[] m_bundleLock = new Object[0];

    // Maps a bundle location to a bundle location;
    // used to reserve a location when installing a bundle.
    private Map m_installRequestMap = new HashMap();
    // This lock must be acquired to modify m_installRequestMap;
    // to help avoid deadlock this lock as priority 1 and should
    // be acquired before locks with lower priority.
    private Object[] m_installRequestLock_Priority1 = new Object[0];

    // Maps a bundle location to a bundle.
    private HashMap m_installedBundleMap = new HashMap();
    // This lock must be acquired to modify m_installedBundleMap;
    // to help avoid deadlock this lock as priority 2 and should
    // be acquired before locks with lower priority.
    private Object[] m_installedBundleLock_Priority2 = new Object[0];

    // An array of uninstalled bundles before a refresh occurs.
    private FelixBundle[] m_uninstalledBundles = null;
    // This lock must be acquired to modify m_uninstalledBundles;
    // to help avoid deadlock this lock as priority 3 and should
    // be acquired before locks with lower priority.
    private Object[] m_uninstalledBundlesLock_Priority3 = new Object[0];

    // Framework's active start level.
    private int m_activeStartLevel =
        FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;

    // Local file system cache.
    private BundleCache m_cache = null;

    // System bundle bundle info instance.
    private BundleInfo m_systemBundleInfo = null;
    // System bundle activator list.
    List m_activatorList = null;
    Map m_activatorContextMap = null;

    // Next available bundle identifier.
    private long m_nextId = 1L;
    private Object m_nextIdLock = new Object[0];

    // List of event listeners.
    private EventDispatcher m_dispatcher = null;

    // Service registry.
    private ServiceRegistry m_registry = null;

    // Reusable bundle URL stream handler.
    private URLStreamHandler m_bundleStreamHandler = null;

    // Execution environment.
    private String m_executionEnvironment = "";
    private Set m_executionEnvironmentCache = new HashSet();

    // Shutdown thread.
    private Thread m_shutdownThread = null;

    /**
     * Creates a new Felix framework instance with a default logger.
     * 
     * @param configMutableMap An map for obtaining configuration properties,
     *        may be <tt>null</tt>.
     * @param activatorList A list of System Bundle activators.
     * 
     * @see #Felix(Logger, Map, List)
     */
    public Felix(Map configMutableMap, List activatorList)
    {
        this(null, configMutableMap, activatorList);
    }

    /**
     * <p>
     * This method creates the framework instance; instances of the framework
     * are not active until they are started. The constructor accepts a
     * <tt>Map</tt> instance that will be used to obtain configuration or
     * framework properties. Configuration properties are used internally by
     * the framework and its extensions to alter its default behavior.
     * Framework properties are used by bundles and are accessible from
     * <tt>BundleContext.getProperty()</tt>. This map instance is used
     * directly (i.e., it is not copied), which means that it is possible to
     * change the property values externally at run time, but this is strongly
     * discouraged for the framework's configuration properties.
     * </p>
     * <p>
     * Configuration properties are the sole means to configure the framework's
     * default behavior; the framework does not refer to any system properties for
     * configuration information. If a <tt>Map</tt> is supplied to this method
     * for configuration properties, then the framework will consult the
     * <tt>Map</tt> instance for any and all configuration properties. It is
     * possible to specify a <tt>null</tt> for the configuration property map,
     * in which case the framework will use its default behavior in all cases.
     * However, if the
     * <a href="cache/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is used, then at a minimum a profile name or profile directory must
     * be specified.
     * </p>
     * <p>
     * The following configuration properties can be specified:
     * </p>
     * <ul>
     *   <li><tt>felix.log.level</tt> - An integer value indicating the degree
     *       of logging reported by the framework; the higher the value the more
     *       logging is reported. If zero ('0') is specified, then logging is
     *       turned off completely. The log levels match those specified in the
     *       OSGi Log Service (i.e., 1 = error, 2 = warning, 3 = information,
     *       and 4 = debug). The default value is 1.
     *   </li>
     *   <li><tt>felix.startlevel.framework</tt> - The initial start level
     *       of the framework once it starts execution; the default
     *       value is 1.
     *   </li>
     *   <li><tt>felix.startlevel.bundle</tt> - The default start level for
     *       newly installed bundles; the default value is 1.
     *   </li>
     *   <li><tt>framework.service.urlhandlers</tt> - Flag to indicate whether
     *       to activate the URL Handlers service for the framework instance;
     *       the default value is "<tt>true</tt>". Activating the URL Handlers
     *       service will result in the <tt>URL.setURLStreamHandlerFactory()</tt>
     *       and <tt>URLConnection.setContentHandlerFactory()</tt> being called.
     *   </li>
     *   <li><tt>felix.embedded.execution</tt> - Flag to indicate whether
     *       the framework is embedded into a host application; the default value is
     *       "<tt>false</tt>". If this flag is "<tt>true</tt>" then the framework
     *       will not called <tt>System.exit()</tt> upon termination.
     *   </li>
     *   <li><tt>felix.strict.osgi</tt> - Flag to indicate whether the framework is
     *       running in strict OSGi mode; the default value is "<tt>true</tt>".
     *       If this flag is "<tt>false</tt>" it enables a non-OSGi-compliant
     *       feature by persisting <tt>BundleActivator</tt>s that implement
     *       <tt>Serializable</tt>. This feature is not recommended since
     *       it is non-compliant.
     *   </li>
     * </ul>
     * <p>
     * Besides the above framework configuration properties, it is also
     * possible to specify properties for the bundle cache. The available
     * bundle cache properties depend on the cache implementation
     * being used. For the properties of the default bundle cache, refer to the
     * <a href="cache/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * API documentation.
     * </p>
     * <p>
     * The <a href="Main.html"><tt>Main</tt></a> class implements some
     * functionality for default property file handling, which makes it
     * possible to specify configuration properties and framework properties
     * in files that are automatically loaded when starting the framework. If you
     * plan to create your own framework instance, you may be
     * able to take advantage of the features it provides; refer to its
     * class documentation for more information.
     * </p>
     *
     * @param logger The logger for use by the framework or <code>null</code>
     *        use the default logger.
     * @param configMutableMap A map for obtaining configuration properties,
     *        may be <tt>null</tt>.
     * @param activatorList A list of System Bundle activators.
    **/
    public Felix(Logger logger, Map configMutableMap, List activatorList)
    {
        // Initialize member variables.
        m_configMutableMap = (configMutableMap == null)
            ? new StringMap(false) : configMutableMap;
        m_configMap = createUnmodifiableMap(m_configMutableMap);
        m_activatorList = activatorList;

        // Create logger with appropriate log level. Even though the
        // logger needs the system bundle's context for tracking log
        // services, it is created now because it is needed before
        // the system bundle is created. The system bundle's context
        // will be set below after the system bundle is created.\
        m_logger = (logger == null) ? new Logger() : logger;
        try
        {
            m_logger.setLogLevel(
                Integer.parseInt(
                    (String) m_configMutableMap.get(FelixConstants.LOG_LEVEL_PROP)));
        }
        catch (NumberFormatException ex)
        {
            // Ignore and just use the default logging level.
        }

        // Initialize framework properties.
        initializeFrameworkProperties();

        // Create the bundle cache since we need it for the system bundle
        // archive, which in turn is needed by the system bundle info,
        // which we need to keep track of the framework state.
        try
        {
            m_cache = new BundleCache(m_logger, m_configMap);
        }
        catch (Exception ex)
        {
            System.err.println("Error creating bundle cache:");
            ex.printStackTrace();

            // Only shutdown the JVM if the framework is running stand-alone.
            String embedded = (String) m_configMap.get(
                FelixConstants.EMBEDDED_EXECUTION_PROP);
            boolean isEmbedded = (embedded == null)
                ? false : embedded.equals("true");
            if (!isEmbedded)
            {
                m_secureAction.exit(-1);
            }
            else
            {
                throw new RuntimeException(ex.toString());
            }
        }

        // Create the module factory and add the system bundle
        // module to it, since we need the system bundle info
        // object to keep track of the framework state.
        m_factory = new ModuleFactoryImpl(m_logger);
        m_systemBundleInfo = new BundleInfo(
            m_logger, new SystemBundleArchive(m_cache), null);
        m_extensionManager =
            new ExtensionManager(m_logger, m_configMap, m_systemBundleInfo);
        m_systemBundleInfo.addModule(
            m_factory.createModule("0", m_extensionManager));
    }

    private Map createUnmodifiableMap(Map mutableMap)
    {
        Map result = Collections.unmodifiableMap(mutableMap);

        // Work around a bug in certain version of J9 where a call to
        // Collections.unmodifiableMap().keySet().iterator() throws
        // a NoClassDefFoundError. We try to detect this and return
        // the given mutableMap instead.
        try
        {
            result.keySet().iterator();
        }
        catch (NoClassDefFoundError ex)
        {
            return mutableMap;
        }

        return result;
    }

    //
    // System Bundle methods.
    //

    /* package private */ BundleInfo getInfo()
    {
        return m_systemBundleInfo;
    }

    public BundleContext getBundleContext()
    {
// TODO: SECURITY - We need a security check here.
        if (m_systemBundleInfo != null)
        {
            return m_systemBundleInfo.getBundleContext();
        }
        return null;
    }

    public long getBundleId()
    {
        return 0;
    }

    public URL getEntry(String name)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getBundleEntry(this, name);
    }

    public Enumeration getEntryPaths(String path)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getBundleEntryPaths(this, path);
    }

    public Enumeration findEntries(String path, String filePattern, boolean recurse)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return findBundleEntries(this, path, filePattern, recurse);
    }

    public Dictionary getHeaders()
    {
        return getHeaders(Locale.getDefault().toString());
    }

    public Dictionary getHeaders(String locale)
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }
        return getBundleHeaders(this, locale);
    }

    public long getLastModified()
    {
        if (m_systemBundleInfo != null)
        {
            return m_systemBundleInfo.getLastModified();
        }
        return -1;
    }

    public String getLocation()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.METADATA));
        }
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    public URL getResource(String name)
    {
        return getBundleResource(this, name);
    }

    public Enumeration getResources(String name) throws IOException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.RESOURCE));
            }
            catch (Exception e)
            {
                return null; // No permission
            }
        }

        return getBundleResources(this, name);
    }

    public ServiceReference[] getRegisteredServices()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = getBundleRegisteredServices(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0;i < refs.length;i++)
            {
                String[] objectClass = (String[]) refs[i].getProperty(
                    Constants.OBJECTCLASS);

                if (objectClass == null)
                {
                    continue;
                }

                for (int j = 0;j < objectClass.length;j++)
                {
                    try
                    {
                        ((SecurityManager) sm).checkPermission(new ServicePermission(
                            objectClass[j], ServicePermission.GET));

                        result.add(refs[i]);

                        break;
                    }
                    catch (Exception ex)
                    {
                        // Silently ignore.
                    }
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }
        else
        {
            return getBundleRegisteredServices(this);
        }
    }

    public ServiceReference[] getServicesInUse()
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ServiceReference[] refs = getBundleServicesInUse(this);

            if (refs == null)
            {
                return refs;
            }

            List result = new ArrayList();

            for (int i = 0;i < refs.length;i++)
            {
                String[] objectClass = (String[]) refs[i].getProperty(
                    Constants.OBJECTCLASS);

                if (objectClass == null)
                {
                    continue;
                }

                for (int j = 0;j < objectClass.length;j++)
                {
                    try
                    {
                        ((SecurityManager) sm).checkPermission(new ServicePermission(
                            objectClass[j], ServicePermission.GET));

                        result.add(refs[i]);

                        break;
                    }
                    catch (Exception e)
                    {
                        // Silently ignore.
                    }
                }
            }

            if (result.isEmpty())
            {
                return null;
            }

            return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);
        }

        return getBundleServicesInUse(this);
    }

    public int getState()
    {
        return m_systemBundleInfo.getState();
    }

    public String getSymbolicName()
    {
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    public boolean hasPermission(Object obj)
    {
        return true;
    }

    public Class loadClass(String name) throws ClassNotFoundException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            try
            {
                ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                    AdminPermission.CLASS));
            }
            catch (Exception e)
            {
                throw new ClassNotFoundException("No permission.", e);
            }
        }

        return loadBundleClass(this, name);
    }

    public synchronized void start() throws BundleException
    {
        // The system bundle is only started once and it
        // is started by the framework.
        if (getState() == Bundle.ACTIVE)
        {
            return;
        }
        else if (m_systemBundleInfo.getState() != Bundle.INSTALLED)
        {
            throw new IllegalStateException("Invalid framework state: " + m_systemBundleInfo.getState());
        }

        // The framework is now in its startup sequence.
        m_systemBundleInfo.setState(Bundle.STARTING);

        // Create default bundle stream handler.
        m_bundleStreamHandler = new URLHandlersBundleStreamHandler(this);

        // Create service registry.
        m_registry = new ServiceRegistry(m_logger);
        // Add a listener to the service registry; this is
        // used to distribute service registry events to
        // service listeners.
        m_registry.addServiceListener(new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                fireServiceEvent(event);
            }
        });

        // Create search policy for module loader.
        m_policyCore = new R4SearchPolicyCore(m_logger, m_configMap);

        // Add a resolver listener to the search policy
        // so that we will be notified when modules are resolved
        // in order to update the bundle state.
        m_policyCore.addResolverListener(new ResolveListener() {
            public void moduleResolved(ModuleEvent event)
            {
                FelixBundle bundle = null;
                try
                {
                    long id = Util.getBundleIdFromModuleId(
                        event.getModule().getId());
                    if (id > 0)
                    {
                        // Update the bundle's state to resolved when the
                        // current module is resolved; just ignore resolve
                        // events for older revisions since this only occurs
                        // when an update is done on an unresolved bundle
                        // and there was no refresh performed.
                        bundle = (FelixBundle) getBundle(id);

                        // Lock the bundle first.
                        try
                        {
                            acquireBundleLock(bundle);
                            if (bundle.getInfo().getCurrentModule() == event.getModule())
                            {
                                if (bundle.getInfo().getState() != Bundle.INSTALLED)
                                {
                                    m_logger.log(
                                        Logger.LOG_WARNING,
                                        "Received a resolve event for a bundle that has already been resolved.");
                                }
                                else
                                {
                                    bundle.getInfo().setState(Bundle.RESOLVED);
                                    fireBundleEvent(BundleEvent.RESOLVED, bundle);
                                }
                            }
                        }
                        finally
                        {
                            releaseBundleLock(bundle);
                        }
                    }
                }
                catch (NumberFormatException ex)
                {
                    // Ignore.
                }
            }

            public void moduleUnresolved(ModuleEvent event)
            {
                // We can ignore this, because the only time it
                // should happen is when a refresh occurs. The
                // refresh operation resets the bundle's state
                // by calling BundleInfo.reset(), thus it is not
                // necessary for us to reset the bundle's state
                // here.
            }
        });

        m_policyCore.setModuleFactory(m_factory);

        // Initialize event dispatcher.
        m_dispatcher = EventDispatcher.start(m_logger);

        // Reload the cached bundles bundles before creating and starting
        // the system bundle, since we want all cached bundles to be reloaded
        // when we activate the system bundle and any subsequent custom
        // framework activators passed into the framework constructor.
        BundleArchive[] archives = null;

        // First get cached bundle identifiers.
        try
        {
            archives = m_cache.getArchives();
        }
        catch (Exception ex)
        {
            m_logger.log(
                Logger.LOG_ERROR,
                "Unable to list saved bundles.",
                ex);
            archives = null;
        }

        // create the system bundle that is responsible for providing specific 
        // container related services.
        IContentLoader cl = m_extensionManager;
        cl.setSearchPolicy(
            new R4SearchPolicy(
                m_policyCore, m_systemBundleInfo.getCurrentModule()));
        m_factory.setContentLoader(
            m_systemBundleInfo.getCurrentModule(),
            cl);
        m_factory.setSecurityContext(
            m_systemBundleInfo.getCurrentModule(),
            getClass().getProtectionDomain());

        m_installedBundleMap.put(
            m_systemBundleInfo.getLocation(), this);
        
        FelixBundle bundle = null;

        // Now install all cached bundles.
        for (int i = 0; (archives != null) && (i < archives.length); i++)
        {
            try
            {
                // Keep track of the max bundle ID currently in use since we
                // will need to use this as our next bundle ID value if the
                // persisted value cannot be read.
                m_nextId = Math.max(m_nextId, archives[i].getId() + 1);

                // It is possible that a bundle in the cache was previously
                // uninstalled, but not completely deleted (perhaps because
                // of a crash or a locked file), so if we see an archive
                // with an UNINSTALLED persistent state, then try to remove
                // it now.
                if (archives[i].getPersistentState() == Bundle.UNINSTALLED)
                {
                    m_cache.remove(archives[i]);
                }
                // Otherwise re-install the cached bundle.
                else
                {
                    // Install the cached bundle.
                    bundle = (FelixBundle) installBundle(
                        archives[i].getId(), archives[i].getLocation(), null);
                }
            }
            catch (Exception ex)
            {
ex.printStackTrace();
                fireFrameworkEvent(FrameworkEvent.ERROR, this, ex);
                try
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to re-install " + archives[i].getLocation(),
                        ex);
                }
                catch (Exception ex2)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to re-install cached bundle.",
                        ex);
                }
                // TODO: FRAMEWORK - Perhaps we should remove the cached bundle?
            }
        }

        // Now that we have loaded all cached bundles and have determined the
        // max bundle ID of cached bundles, we need to try to load the next
        // bundle ID from persistent storage. In case of failure, we should
        // keep the max value.
        m_nextId = Math.max(m_nextId, loadNextId());

        // Get the framework's default start level.
        int startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
        String s = (String) m_configMap.get(FelixConstants.FRAMEWORK_STARTLEVEL_PROP);
        if (s != null)
        {
            try
            {
                startLevel = Integer.parseInt(s);
            }
            catch (NumberFormatException ex)
            {
                startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
            }
        }

        // Now that the cached bundles are reloaded, 
        // activating all custom framework activators.
        try
        {
            // Manually resolve the System Bundle, which will cause its
            // state to be set to RESOLVED.
            try
            {
                m_policyCore.resolve(m_systemBundleInfo.getCurrentModule());
            }
            catch (ResolveException ex)
            {
                // This should never happen.
                throw new BundleException(
                    "Unresolved package in System Bundle:"
                    + ex.getRequirement());
            }

            // Create system bundle activator.
            m_systemBundleInfo.setActivator(new SystemBundleActivator());

            // Create the bundle context for the system bundle and
            // then activate it.
            m_systemBundleInfo.setBundleContext(
                new BundleContextImpl(m_logger, this, this));
            Felix.m_secureAction.startActivator(m_systemBundleInfo.getActivator(), 
                m_systemBundleInfo.getBundleContext());
        }
        catch (Throwable ex)
        {
            m_factory = null;
            EventDispatcher.shutdown();
            m_logger.log(Logger.LOG_ERROR, "Unable to start system bundle.", ex);
            throw new RuntimeException("Unable to start system bundle.");
        }

        // Now that the system bundle is successfully created we can give
        // its bundle context to the logger so that it can track log services.
        m_logger.setSystemBundleContext(m_systemBundleInfo.getBundleContext());

        // Set the start level using the start level service;
        // this ensures that all start level requests are
        // serialized.
        try
        {
            StartLevel sl = (StartLevel) getService(
                getBundle(0),getServiceReferences((FelixBundle) getBundle(0), 
                StartLevel.class.getName(), null, true)[0]);
            if (sl instanceof StartLevelImpl)
            {
                ((StartLevelImpl) sl).setStartLevelAndWait(startLevel);
            }
            else
            {
                sl.setStartLevel(startLevel);
            }
        }
        catch (InvalidSyntaxException ex)
        {
            // Should never happen.
        }

        // The framework is now running.
        m_systemBundleInfo.setState(Bundle.ACTIVE);

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, this);

        // Send a framework event to indicate the framework has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, this, null);
    }

    /**
     * This method cleanly shuts down the framework, it must be called at the
     * end of a session in order to shutdown all active bundles.
    **/
    public void stop() throws BundleException
    {
        Object sm = System.getSecurityManager();

        if (sm != null)
        {
            ((SecurityManager) sm).checkPermission(new AdminPermission(this,
                AdminPermission.EXECUTE));
        }

        stopBundle(this, true);
    }

    public void stopAndWait()
    {
        // Shut the framework down by calling stop() on the system bundle.
        // Since stop() on the system bundle will return immediately, we
        // will synchronize on the framework instance so we can use it to
        // be notified when the shutdown is complete.
        synchronized (this)
        {
            if (m_systemBundleInfo.getState() == Bundle.ACTIVE)
            {
                try
                {
                    getBundle(0).stop();
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, this, ex);
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Error stopping system bundle.",
                        ex);
                }
            }

            while (m_systemBundleInfo.getState() != Bundle.UNINSTALLED)
            {
                try
                {
                    wait();
                }
                catch (InterruptedException ex)
                {
                    // Keep waiting if necessary.
                }
            }
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

        // TODO: FRAMEWORK - This is supposed to stop and then restart the framework.
        throw new BundleException("System bundle update not implemented yet.");
    }

    public String toString()
    {
        return getSymbolicName() + " [" + getBundleId() +"]";
    }

    /**
     * Returns the active start level of the framework; this method
     * implements functionality for the Start Level service.
     * @return The active start level of the framework.
    **/
    protected int getStartLevel()
    {
        return m_activeStartLevel;
    }

    /**
     * Implements the functionality of the <tt>setStartLevel()</tt>
     * method for the StartLevel service, but does not do the security or
     * parameter check. The security and parameter check are done in the
     * StartLevel service implementation because this method is called on
     * a separate thread and the caller's thread would already be gone if
     * we did the checks in this method. This method should not be called
     * directly.
     * @param requestedLevel The new start level of the framework.
    **/
    protected void setFrameworkStartLevel(int requestedLevel)
    {
        Bundle[] bundles = null;

        // Synchronization for changing the start level is rather loose.
        // The install lock is grabbed initially to atomically change the
        // framework's start level and to grab a sorted snapshot of the
        // currently installed bundles, but then this lock is freed immediately.
        // No locks are held while processing the currently installed bundles
        // for starting/stopping based on the new start level. The only locking
        // that occurs is for individual bundles when startBundle()/stopBundle()
        // is called, but this locking is done in the respective method.
        //
        // This approach does mean that it is possible for a for individual
        // bundle states to change during this operation. For example, bundle
        // start levels can be changed or bundles can be uninstalled. If a
        // bundle's start level changes, then it is possible for it to be
        // processed out of order. Uninstalled bundles are just logged and
        // ignored. I had a bit of discussion with Peter Kriens about these
        // issues and he felt they were consistent with the spec, which
        // intended Start Level to have some leeway.
        //
        // Calls to this method are only made by the start level thread, which
        // serializes framework start level changes. Thus, it is not possible
        // for two requests to change the framework's start level to interfere
        // with each other.

        synchronized (m_installedBundleLock_Priority2)
        {
            // Determine if we are lowering or raising the
            // active start level.
            boolean lowering = (requestedLevel < m_activeStartLevel);

            // Record new start level.
            m_activeStartLevel = requestedLevel;

            // Get a snapshot of all installed bundles.
            bundles = getBundles();

            // Sort bundle array by start level either ascending or
            // descending depending on whether the start level is being
            // lowered or raised to that the bundles can be efficiently
            // processed in order. Within a start level sort by bundle ID.
            Comparator comparator = null;
            if (lowering)
            {
                // Sort descending to stop highest start level first.
                comparator = new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        FelixBundle b1 = (FelixBundle) o1;
                        FelixBundle b2 = (FelixBundle) o2;
                        if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            < b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return 1;
                        }
                        else if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            > b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return -1;
                        }
                        else if (b1.getInfo().getBundleId() < b2.getInfo().getBundleId())
                        {
                            return 1;
                        }
                        return -1;
                    }
                };
            }
            else
            {
                // Sort ascending to start lowest start level first.
                comparator = new Comparator() {
                    public int compare(Object o1, Object o2)
                    {
                        FelixBundle b1 = (FelixBundle) o1;
                        FelixBundle b2 = (FelixBundle) o2;
                        if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            > b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return 1;
                        }
                        else if (b1.getInfo().getStartLevel(getInitialBundleStartLevel())
                            < b2.getInfo().getStartLevel(getInitialBundleStartLevel()))
                        {
                            return -1;
                        }
                        else if (b1.getInfo().getBundleId() > b2.getInfo().getBundleId())
                        {
                            return 1;
                        }
                        return -1;
                    }
                };
            }

            Arrays.sort(bundles, comparator);
        }

        // Stop or start the bundles according to the start level.
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            FelixBundle impl = (FelixBundle) bundles[i];

            // Ignore the system bundle, since its start() and
            // stop() methods get called explicitly in Felix.start()
            // and Felix.shutdown(), respectively.
            if (impl.getInfo().getBundleId() == 0)
            {
                continue;
            }

            // Lock the current bundle.
            acquireBundleLock(impl);

            try
            {
                // Start the bundle if necessary.
                if ((impl.getInfo().getPersistentState() == Bundle.ACTIVE) &&
                    (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                        <= m_activeStartLevel))
                {
                    try
                    {
                        startBundle(impl, false);
                    }
                    catch (Throwable th)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Error starting " + impl.getInfo().getLocation(), th);
                    }
                }
                // Stop the bundle if necessary.
                else if (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                    > m_activeStartLevel)
                {
                    try
                    {
                        stopBundle(impl, false);
                    }
                    catch (Throwable th)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Error stopping " + impl.getInfo().getLocation(), th);
                    }
                }
            }
            finally
            {
                // Always release bundle lock.
                releaseBundleLock(impl);
            }
        }

        if (m_systemBundleInfo.getState() == Bundle.ACTIVE)
        {
            fireFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, this, null);
        }
    }

    /**
     * Returns the start level into which newly installed bundles will
     * be placed by default; this method implements functionality for
     * the Start Level service.
     * @return The default start level for newly installed bundles.
    **/
    protected int getInitialBundleStartLevel()
    {
        String s = (String) m_configMap.get(FelixConstants.BUNDLE_STARTLEVEL_PROP);

        if (s != null)
        {
            try
            {
                int i = Integer.parseInt(s);
                return (i > 0) ? i : FelixConstants.BUNDLE_DEFAULT_STARTLEVEL;
            }
            catch (NumberFormatException ex)
            {
                // Ignore and return the default value.
            }
        }
        return FelixConstants.BUNDLE_DEFAULT_STARTLEVEL;
    }

    /**
     * Sets the default start level into which newly installed bundles
     * will be placed; this method implements functionality for the Start
     * Level service.
     * @param startLevel The new default start level for newly installed
     *        bundles.
     * @throws java.lang.IllegalArgumentException If the specified start
     *         level is not greater than zero.
     * @throws java.security.SecurityException If the caller does not
     *         have <tt>AdminPermission</tt>.
    **/
    protected void setInitialBundleStartLevel(int startLevel)
    {
        if (startLevel <= 0)
        {
            throw new IllegalArgumentException(
                "Initial start level must be greater than zero.");
        }

        m_configMutableMap.put(
            FelixConstants.BUNDLE_STARTLEVEL_PROP, Integer.toString(startLevel));
    }

    /**
     * Returns the start level for the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle The bundle to examine.
     * @return The start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    protected int getBundleStartLevel(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((FelixBundle) bundle).getInfo().getStartLevel(getInitialBundleStartLevel());
    }

    /**
     * Sets the start level of the specified bundle; this method
     * implements functionality for the Start Level service.
     * @param bundle The bundle whose start level is to be modified.
     * @param startLevel The new start level of the specified bundle.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle is the system bundle or if the bundle has been
     *          uninstalled.
     * @throws java.security.SecurityException If the caller does not
     *          have <tt>AdminPermission</tt>.
    **/
    protected void setBundleStartLevel(Bundle bundle, int startLevel)
    {
        // Acquire bundle lock.
        acquireBundleLock((FelixBundle) bundle);

        Throwable rethrow = null;

        try
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalArgumentException("Bundle is uninstalled.");
            }

            if (startLevel >= 1)
            {
                FelixBundle impl = (FelixBundle) bundle;
                impl.getInfo().setStartLevel(startLevel);

                try
                {
                    // Start the bundle if necessary.
                    if ((impl.getInfo().getPersistentState() == Bundle.ACTIVE) &&
                        (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                            <= m_activeStartLevel))
                    {
                        startBundle(impl, false);
                    }
                    // Stop the bundle if necessary.
                    else if (impl.getInfo().getStartLevel(getInitialBundleStartLevel())
                        > m_activeStartLevel)
                    {
                        stopBundle(impl, false);
                    }
                }
                catch (Throwable th)
                {
                    rethrow = th;
                    m_logger.log(Logger.LOG_ERROR, "Error starting/stopping bundle.", th);
                }
            }
            else
            {
                m_logger.log(Logger.LOG_WARNING, "Bundle start level must be greater than zero.");
            }
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock((FelixBundle) bundle);
        }

        if (rethrow != null)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, rethrow);
        }
    }

    /**
     * Returns whether a bundle is persistently started; this is an
     * method implementation for the Start Level service.
     * @param bundle The bundle to examine.
     * @return <tt>true</tt> if the bundle is marked as persistently
     *          started, <tt>false</tt> otherwise.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    protected boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return (((FelixBundle) bundle).getInfo().getPersistentState() == Bundle.ACTIVE);
    }

    //
    // Implementation of Bundle interface methods.
    //

    /**
     * Implementation for Bundle.getSymbolicName().
    **/
    protected String getBundleSymbolicName(FelixBundle bundle)
    {
        return (String) bundle.getInfo().getCurrentHeader().get(Constants.BUNDLE_SYMBOLICNAME);
    }

    /**
     * Get bundle headers and resolve any localized strings from resource bundles.
     * @param bundle
     * @param locale
     * @return localized bundle headers dictionary.
    **/
    protected Dictionary getBundleHeaders(FelixBundle bundle, String locale)
    {
        return new MapToDictionary(bundle.getInfo().getCurrentLocalizedHeader(locale));
    }

    /**
     * Implementation for Bundle.getLocation().
    **/
    protected String getBundleLocation(FelixBundle bundle)
    {
        return bundle.getInfo().getLocation();
    }

    /**
     * Implementation for Bundle.getResource().
    **/
    protected URL getBundleResource(FelixBundle bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        return bundle.getInfo().getCurrentModule().getResource(name);
    }

    /**
     * Implementation for Bundle.getResources().
    **/
    protected Enumeration getBundleResources(FelixBundle bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        return bundle.getInfo().getCurrentModule().getResources(name);
    }

    /**
     * Implementation for Bundle.getEntry().
    **/
    protected URL getBundleEntry(FelixBundle bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        return bundle.getInfo().getCurrentModule()
            .getContentLoader().getResourceFromContent(name);
    }

    /**
     * Implementation for Bundle.getEntryPaths().
    **/
    protected Enumeration getBundleEntryPaths(FelixBundle bundle, String path)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Get the entry enumeration from the module content and
        // create a wrapper enumeration to filter it.
        Enumeration enumeration = new GetEntryPathsEnumeration(bundle, path);

        // Return the enumeration if it has elements.
        return (!enumeration.hasMoreElements()) ? null : enumeration;
    }

    /**
     * Implementation for findEntries().
    **/
    protected Enumeration findBundleEntries(
        FelixBundle bundle, String path, String filePattern, boolean recurse)
    {
        // Try to resolve the bundle per the spec.
        resolveBundles(new Bundle[] { bundle });

        // Get the entry enumeration from the module content and
        // create a wrapper enumeration to filter it.
        Enumeration enumeration =
            new FindEntriesEnumeration(bundle, path, filePattern, recurse);

        // Return the enumeration if it has elements.
        return (!enumeration.hasMoreElements()) ? null : enumeration;
    }

    protected ServiceReference[] getBundleRegisteredServices(FelixBundle bundle)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Filter list of registered service references.
        ServiceReference[] refs = m_registry.getRegisteredServices(bundle);

        return refs;
    }

    protected ServiceReference[] getBundleServicesInUse(Bundle bundle)
    {
        // Filter list of "in use" service references.
        ServiceReference[] refs = m_registry.getServicesInUse(bundle);

        return refs;
    }

    protected boolean bundleHasPermission(FelixBundle bundle, Object obj)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        if (System.getSecurityManager() != null)
        {
            try
            {
                return (obj instanceof java.security.Permission)
                    ? ((ProtectionDomain)
                    bundle.getInfo().getCurrentModule().getSecurityContext())
                    .implies((java.security.Permission) obj)
                    : false;
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Exception while evaluating the permission.",
                    ex);
                return false;
            }
        }

        return true;
    }

    /**
     * Implementation for Bundle.loadClass().
    **/
    protected Class loadBundleClass(FelixBundle bundle, String name) throws ClassNotFoundException
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("Bundle is uninstalled");
        }
        else if (bundle.getInfo().getState() == Bundle.INSTALLED)
        {
            try
            {
                _resolveBundle(bundle);
            }
            catch (BundleException ex)
            {
                // The spec says we must fire a framework error.
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                // Then throw a class not found exception.
                throw new ClassNotFoundException(name);
            }
        }
        Class clazz = bundle.getInfo().getCurrentModule().getClass(name);
        if (clazz == null)
        {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    /**
     * Implementation for Bundle.start().
    **/
    protected void startBundle(FelixBundle bundle, boolean record)
        throws BundleException
    {
        // CONCURRENCY NOTE:
        // Starting a bundle may actually impact many bundles, since
        // the bundle being started my need to be resolved, which in
        // turn may need to resolve other bundles. Despite this fact,
        // we only acquire the lock for the bundle being started, because
        // when resolve is called on this bundle, it will eventually
        // call resolve on the module loader search policy, which does
        // its own locking on the module factory instance. Since the
        // resolve algorithm is locking the module factory instance, it
        // is not possible for other bundles to be installed or removed,
        // so we don't have to worry about these possibilities.
        //
        // Further, if other bundles are started during this operation,
        // then either they will resolve first because they got the lock
        // on the module factory or we will resolve first since we got
        // the lock on the module factory, so there should be no interference.
        // If other bundles are stopped or uninstalled, this should pose
        // no problems, since this does not impact their resolved state.
        // If a refresh occurs, then the refresh algorithm ulimately has
        // to acquire the module factory instance lock too before it can
        // completely purge old modules, so it should also complete either
        // before or after this bundle is started. At least that's the
        // theory.

        // Acquire bundle lock.
        acquireBundleLock(bundle);

        try
        {
            _startBundle(bundle, record);
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    private void _startBundle(FelixBundle bundle, boolean record)
        throws BundleException
    {
        // The spec doesn't say whether it is possible to start an extension
        // We just do nothing
        if (bundle.getInfo().isExtension())
        {
            return;
        }

        // Set and save the bundle's persistent state to active
        // if we are supposed to record state change.
        if (record)
        {
            bundle.getInfo().setPersistentStateActive();
        }

        // Try to start the bundle.
        BundleInfo info = bundle.getInfo();

        // Ignore bundles whose persistent state is not active
        // or whose start level is greater than the framework's.
        if ((info.getPersistentState() != Bundle.ACTIVE)
            || (info.getStartLevel(getInitialBundleStartLevel()) > getStartLevel()))
        {
            return;
        }

        switch (info.getState())
        {
            case Bundle.UNINSTALLED:
                throw new IllegalStateException("Cannot start an uninstalled bundle.");
            case Bundle.STARTING:
            case Bundle.STOPPING:
                throw new BundleException("Starting a bundle that is starting or stopping is currently not supported.");
            case Bundle.ACTIVE:
                return;
            case Bundle.INSTALLED:
                _resolveBundle(bundle);
                // No break.
            case Bundle.RESOLVED:
                info.setState(Bundle.STARTING);
                fireBundleEvent(BundleEvent.STARTING, bundle);
                break;
        }

        try
        {
            // Set the bundle's context.
            info.setBundleContext(new BundleContextImpl(m_logger, this, bundle));

            // Set the bundle's activator.
            info.setActivator(createBundleActivator(bundle.getInfo()));

            // Activate the bundle if it has an activator.
            if (bundle.getInfo().getActivator() != null)
            {
                m_secureAction.startActivator(info.getActivator(),info.getBundleContext());
            }

            // TODO: CONCURRENCY - Reconsider firing event outside of the
            // bundle lock.
            info.setState(Bundle.ACTIVE);
            fireBundleEvent(BundleEvent.STARTED, bundle);
        }
        catch (Throwable th)
        {
            // If there was an error starting the bundle,
            // then reset its state to RESOLVED.
            info.setState(Bundle.RESOLVED);

            // Clean up the bundle context.
            ((BundleContextImpl) info.getBundleContext()).invalidate();
            info.setBundleContext(null);

            // Unregister any services offered by this bundle.
            m_registry.unregisterServices(bundle);

            // Release any services being used by this bundle.
            m_registry.ungetServices(bundle);

            // Remove any listeners registered by this bundle.
            m_dispatcher.removeListeners(bundle);

            // The spec says to expect BundleException or
            // SecurityException, so rethrow these exceptions.
            if (th instanceof BundleException)
            {
                throw (BundleException) th;
            }
            else if (th instanceof SecurityException)
            {
                throw (SecurityException) th;
            }
            else if ((System.getSecurityManager() != null) &&
                (th instanceof java.security.PrivilegedActionException))
            {
                th = ((java.security.PrivilegedActionException) th).getException();
            }

            // Rethrow all other exceptions as a BundleException.
            throw new BundleException("Activator start error.", th);
        }
    }

    protected void _resolveBundle(FelixBundle bundle)
        throws BundleException
    {
        // If a security manager is installed, then check for permission
        // to import the necessary packages.
        if (System.getSecurityManager() != null)
        {
            ProtectionDomain pd = (ProtectionDomain)
                bundle.getInfo().getCurrentModule().getSecurityContext();

            IRequirement[] imports =
                bundle.getInfo().getCurrentModule().getDefinition().getRequirements();

/*
 TODO: RB - We need to fix this import check by looking at the wire
            associated with it, not the import since we don't know the
            package name associated with the import since it is a filter.

            for (int i = 0; i < imports.length; i++)
            {
                if (imports[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    PackagePermission perm = new PackagePermission(
                        imports[i].???,
                        PackagePermission.IMPORT);

                    if (!pd.implies(perm))
                    {
                        throw new java.security.AccessControlException(
                            "PackagePermission.IMPORT denied for import: " +
                            imports[i].getName(), perm);
                    }
                }
            }
*/
            // Check export permission for all exports of the current module.
            ICapability[] exports =
                bundle.getInfo().getCurrentModule().getDefinition().getCapabilities();
            for (int i = 0; i < exports.length; i++)
            {
                if (exports[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                {
                    PackagePermission perm = new PackagePermission(
                        (String) exports[i].getProperties().get(ICapability.PACKAGE_PROPERTY), PackagePermission.EXPORT);

                    if (!pd.implies(perm))
                    {
                        throw new java.security.AccessControlException(
                            "PackagePermission.EXPORT denied for export: " +
                            exports[i].getProperties().get(ICapability.PACKAGE_PROPERTY), perm);
                    }
                }
            }
        }

        verifyExecutionEnvironment(bundle);

        IModule module = bundle.getInfo().getCurrentModule();
        try
        {
            m_policyCore.resolve(module);
        }
        catch (ResolveException ex)
        {
            if (ex.getModule() != null)
            {
                throw new BundleException(
                    "Unresolved package in bundle "
                    + Util.getBundleIdFromModuleId(ex.getModule().getId())
                    + ": " + ex.getRequirement());
            }
            else
            {
                throw new BundleException(ex.getMessage());
            }
        }
    }

    protected void updateBundle(FelixBundle bundle, InputStream is)
        throws BundleException
    {
        // Acquire bundle lock.
        acquireBundleLock(bundle);

        try
        {
            _updateBundle(bundle, is);
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    protected void _updateBundle(FelixBundle bundle, InputStream is)
        throws BundleException
    {
        // We guarantee to close the input stream, so put it in a
        // finally clause.

        try
        {
            // Variable to indicate whether bundle is active or not.
            Throwable rethrow = null;

            // Cannot update an uninstalled bundle.
            BundleInfo info = bundle.getInfo();
            if (info.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("The bundle is uninstalled.");
            }

            // First get the update-URL from our header.
            String updateLocation = (String)
                info.getCurrentHeader().get(Constants.BUNDLE_UPDATELOCATION);

            // If no update location specified, use original location.
            if (updateLocation == null)
            {
                updateLocation = info.getLocation();
            }

            // Stop the bundle, but do not change the persistent state.
            stopBundle(bundle, false);

            try
            {
                // Get the bundle's archive.
                BundleArchive archive = m_cache.getArchive(info.getBundleId());
                // Update the bundle; this operation will increase
                // the revision count for the bundle.
                archive.revise(updateLocation, is);
                // Create a module for the new revision; the revision is
                // base zero, so subtract one from the revision count to
                // get the revision of the new update.
                try
                {
                    Object sm = System.getSecurityManager();

                    if (sm != null)
                    {
                        ((SecurityManager) sm).checkPermission(
                            new AdminPermission(bundle, AdminPermission.LIFECYCLE));
                    }

                    // We need to check whether this is an update to an
                    // extension bundle (info.isExtension) or an update from
                    // a normal bundle to an extension bundle
                    // (isExtensionBundle())
                    IModule module = createModule(
                        info.getBundleId(),
                        archive.getRevisionCount() - 1,
                        info.getCurrentHeader(),
                        createBundleProtectionDomain(archive),
                        bundle.getInfo().isExtension() || m_extensionManager.isExtensionBundle(
                            bundle.getInfo().getCurrentHeader()));

                    // Add module to bundle info.
                    info.addModule(module);

                    // If this is an update from a normal to an extension bundle
                    // then attach the extension or else if this already is
                    // an extension bundle then done allow it to be resolved
                    // again as per spec.
                    if (!bundle.getInfo().isExtension() &&
                        m_extensionManager.isExtensionBundle(bundle.getInfo().getCurrentHeader()))
                    {
                        m_extensionManager.addExtensionBundle(this, bundle);
                        m_factory.refreshModule(m_systemBundleInfo.getCurrentModule());
                        bundle.getInfo().setState(Bundle.RESOLVED);
                    }
                    else if (bundle.getInfo().isExtension())
                    {
                        bundle.getInfo().setState(Bundle.INSTALLED);
                    }
                }
                catch (Throwable ex)
                {
                    try
                    {
                        archive.undoRevise();
                    }
                    catch (Exception busted)
                    {
                        m_logger.log(Logger.LOG_ERROR, "Unable to rollback.", busted);
                    }

                    throw ex;
                }
            }
            catch (Throwable ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to update the bundle.", ex);
                rethrow = ex;
            }

            // Set new state, mark as needing a refresh, and fire updated event
            // if successful.
            if (rethrow == null)
            {
                info.setLastModified(System.currentTimeMillis());

                if (!info.isExtension())
                {
                    info.setState(Bundle.INSTALLED);
                }

                fireBundleEvent(BundleEvent.UNRESOLVED, bundle);

                // Mark previous the bundle's old module for removal since
                // it can no longer be used to resolve other modules per the spec.
                ((ModuleImpl) info.getModules()[info.getModules().length - 2])
                    .setRemovalPending(true);

                fireBundleEvent(BundleEvent.UPDATED, bundle);

                // Determine if the bundle is in use by anyone.
                boolean used = false;
                IModule[] modules = info.getModules();
                for (int i = 0; !used && (i < modules.length); i++)
                {
                    IModule[] dependents = ((ModuleImpl) modules[i]).getDependents();
                    for (int j = 0; (dependents != null) && (j < dependents.length) && !used; j++)
                    {
                        if (dependents[j] != modules[i])
                        {
                            used = true;
                        }
                    }
                }

                // If the bundle is not used by anyone, then garbage
                // collect it now.
                if (!used)
                {
                    try
                    {
                        refreshPackages(new Bundle[] { bundle });
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to immediately purge the bundle revisions.", ex);
                    }
                }
            }

            // Restart bundle, but do not change the persistent state.
            // This will not start the bundle if it was not previously
            // active.
            startBundle(bundle, false);

            // If update failed, rethrow exception.
            if (rethrow != null)
            {
                if ((System.getSecurityManager() != null) &&
                    (rethrow instanceof SecurityException))
                {
                    throw (SecurityException) rethrow;
                }

                throw new BundleException("Update failed.", rethrow);
            }
        }
        finally
        {
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to close input stream.", ex);
            }
        }
    }

    protected void stopBundle(FelixBundle bundle, boolean record)
        throws BundleException
    {
        // Acquire bundle lock.
        acquireBundleLock(bundle);

        try
        {
            _stopBundle(bundle, record);
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    private void _stopBundle(FelixBundle bundle, boolean record)
        throws BundleException
    {
        Throwable rethrow = null;

        // Set the bundle's persistent state to inactive if necessary.
        if (record)
        {
            bundle.getInfo().setPersistentStateInactive();
        }

        BundleInfo info = bundle.getInfo();

        switch (info.getState())
        {
            case Bundle.UNINSTALLED:
                throw new IllegalStateException("Cannot stop an uninstalled bundle.");
            case Bundle.STARTING:
            case Bundle.STOPPING:
                throw new BundleException("Stopping a bundle that is starting or stopping is currently not supported.");
            case Bundle.INSTALLED:
            case Bundle.RESOLVED:
                return;
            case Bundle.ACTIVE:
                // Set bundle state..
                info.setState(Bundle.STOPPING);
                fireBundleEvent(BundleEvent.STOPPING, bundle);
                break;
        }

        try
        {
            if (info.getActivator() != null)
            {
                m_secureAction.stopActivator(info.getActivator(), info.getBundleContext());
            }

            // Try to save the activator in the cache.
            // NOTE: This is non-standard OSGi behavior and only
            // occurs if strictness is disabled.
            String strict = (String) m_configMap.get(FelixConstants.STRICT_OSGI_PROP);
            boolean isStrict = (strict == null) ? true : strict.equals("true");
            if (!isStrict)
            {
                try
                {
                    m_cache.getArchive(info.getBundleId())
                        .setActivator(info.getActivator());
                }
                catch (Exception ex)
                {
                    // Problem saving activator, so ignore it.
                    // TODO: FRAMEWORK - Perhaps we should handle this some other way?
                }
            }
        }
        catch (Throwable th)
        {
            m_logger.log(Logger.LOG_ERROR, "Error stopping bundle.", th);
            rethrow = th;
        }

        // Do not clean up after the system bundle since it will
        // clean up after itself.
        if (info.getBundleId() != 0)
        {
            // Clean up the bundle context.
            ((BundleContextImpl) info.getBundleContext()).invalidate();
            info.setBundleContext(null);

            // Unregister any services offered by this bundle.
            m_registry.unregisterServices(bundle);

            // Release any services being used by this bundle.
            m_registry.ungetServices(bundle);

            // The spec says that we must remove all event
            // listeners for a bundle when it is stopped.
            m_dispatcher.removeListeners(bundle);

            info.setState(Bundle.RESOLVED);
            fireBundleEvent(BundleEvent.STOPPED, bundle);
        }

        // Throw activator error if there was one.
        if (rethrow != null)
        {
            // The spec says to expect BundleException or
            // SecurityException, so rethrow these exceptions.
            if (rethrow instanceof BundleException)
            {
                throw (BundleException) rethrow;
            }
            else if (rethrow instanceof SecurityException)
            {
                throw (SecurityException) rethrow;
            }
            else if ((System.getSecurityManager() != null) &&
                (rethrow instanceof java.security.PrivilegedActionException))
            {
                rethrow = ((java.security.PrivilegedActionException) rethrow).getException();
            }

            // Rethrow all other exceptions as a BundleException.
            throw new BundleException("Activator stop error.", rethrow);
        }
    }

    protected void uninstallBundle(FelixBundle bundle) throws BundleException
    {
        // Acquire bundle lock.
        acquireBundleLock(bundle);

        try
        {
            _uninstallBundle(bundle);
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    private void _uninstallBundle(FelixBundle bundle) throws BundleException
    {
        BundleInfo info = bundle.getInfo();
        if (info.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Extension Bundles are not removed until the framework is shutdown
        if (bundle.getInfo().isExtension())
        {
            bundle.getInfo().setPersistentStateUninstalled();
            bundle.getInfo().setState(Bundle.INSTALLED);
            return;
        }

        // The spec says that uninstall should always succeed, so
        // catch an exception here if stop() doesn't succeed and
        // rethrow it at the end.
        try
        {
            stopBundle(bundle, true);
        }
        catch (BundleException ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
        }

        // Remove the bundle from the installed map.
        FelixBundle target = null;
        synchronized (m_installedBundleLock_Priority2)
        {
            target = (FelixBundle) m_installedBundleMap.remove(info.getLocation());
        }

        // Finally, put the uninstalled bundle into the
        // uninstalled list for subsequent refreshing.
        if (target != null)
        {
            // Set the bundle's persistent state to uninstalled.
            target.getInfo().setPersistentStateUninstalled();

            // Mark current module for removal since it can no longer
            // be used to resolve other modules per the spec.
            ((ModuleImpl) target.getInfo().getCurrentModule()).setRemovalPending(true);

            // Put bundle in uninstalled bundle array.
            rememberUninstalledBundle(bundle);
        }
        else
        {
            m_logger.log(
                Logger.LOG_ERROR, "Unable to remove bundle from installed map!");
        }

        // Set state to uninstalled.
        info.setState(Bundle.UNINSTALLED);
        info.setLastModified(System.currentTimeMillis());

        // Fire bundle event.
        fireBundleEvent(BundleEvent.UNINSTALLED, bundle);

        // Determine if the bundle is in use by anyone.
        boolean used = false;
        IModule[] modules = info.getModules();
        for (int i = 0; !used && (i < modules.length); i++)
        {
            IModule[] dependents = ((ModuleImpl) modules[i]).getDependents();
            for (int j = 0; (dependents != null) && (j < dependents.length) && !used; j++)
            {
                if (dependents[j] != modules[i])
                {
                    used = true;
                }
            }
        }

        // If the bundle is not used by anyone, then garbage
        // collect it now.
        if (!used)
        {
            try
            {
                refreshPackages(new Bundle[] { bundle });
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to immediately garbage collect the bundle.", ex);
            }
        }
    }

    //
    // Implementation of BundleContext interface methods.
    //

    protected void addRequirement(FelixBundle bundle, String s) throws BundleException
    {
        // TODO: EXPERIMENTAL - Experimental implicit wire concept to try
        //       to deal with code generation.
        synchronized (m_factory)
        {
            IRequirement[] reqs = ManifestParser.parseImportHeader(s);
            IRequirement[] dynamics = bundle.getInfo().getCurrentModule()
                .getDefinition().getDynamicRequirements();
            if (dynamics == null)
            {
                dynamics = reqs;
            }
            else
            {
                IRequirement[] tmp = new IRequirement[dynamics.length + reqs.length];
                System.arraycopy(dynamics, 0, tmp, 0, dynamics.length);
                System.arraycopy(reqs, 0, tmp, dynamics.length, reqs.length);
                dynamics = tmp;
            }
            ((ModuleDefinition) bundle.getInfo().getCurrentModule().getDefinition())
                .setDynamicRequirements(dynamics);
        }
    }

    /**
     * Implementation for BundleContext.getProperty(). Returns
     * environment property associated with the framework.
     *
     * @param key The name of the property to retrieve.
     * @return The value of the specified property or null.
    **/
    protected String getProperty(String key)
    {
        // First, check the config properties.
        String val = (String) m_configMap.get(key);
        // If not found, then try the system properties.
        return (val == null) ? System.getProperty(key) : val;
    }

    protected Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        return installBundle(-1, location, is);
    }

    private Bundle installBundle(long id, String location, InputStream is)
        throws BundleException
    {
        FelixBundle bundle = null;

        // Acquire an install lock.
        acquireInstallLock(location);

        try
        {
            // Check to see if the framework is still running;
            if ((getState() == Bundle.STOPPING) ||
                (getState() == Bundle.UNINSTALLED))
            {
                throw new BundleException("The framework has been shutdown.");
            }

            // If bundle location is already installed, then
            // return it as required by the OSGi specification.
            bundle = (FelixBundle) getBundle(location);
            if (bundle != null)
            {
                return bundle;
            }

            // Determine if this is a new or existing bundle.
            boolean isNew = (id < 0);

            // If the bundle is new we must cache its JAR file.
            if (isNew)
            {
                // First generate an identifier for it.
                id = getNextId();

                try
                {
                    // Add the bundle to the cache.
                    m_cache.create(id, location, is);
                }
                catch (Exception ex)
                {
                    throw new BundleException(
                        "Unable to cache bundle: " + location, ex);
                }
                finally
                {
                    try
                    {
                        if (is != null) is.close();
                    }
                    catch (IOException ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to close input stream.", ex);
                    }
                }
            }
            else
            {
                // If the bundle we are installing is not new,
                // then try to purge old revisions before installing
                // it; this is done just in case a "refresh"
                // didn't occur last session...this would only be
                // due to an error or system crash.
                try
                {
                    if (m_cache.getArchive(id).getRevisionCount() > 1)
                    {
                        m_cache.getArchive(id).purge();
                    }
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Could not purge bundle.", ex);
                }
            }

            try
            {
                BundleArchive archive = m_cache.getArchive(id);
                bundle = new BundleImpl(this, createBundleInfo(archive,
                    m_extensionManager.isExtensionBundle(archive.getRevision(
                    archive.getRevisionCount() - 1).getManifestHeader())));

                verifyExecutionEnvironment(bundle);

                if (!bundle.getInfo().isExtension())
                {
                    Object sm = System.getSecurityManager();
                    if (sm != null)
                    {
                        ((SecurityManager) sm).checkPermission(
                            new AdminPermission(bundle, AdminPermission.LIFECYCLE));
                    }
                }
                else
                {
                    m_extensionManager.addExtensionBundle(this, bundle);
                    m_factory.refreshModule(m_systemBundleInfo.getCurrentModule());
                }

            }
            catch (Throwable ex)
            {
                // If the bundle is new, then remove it from the cache.
                // TODO: FRAMEWORK - Perhaps it should be removed if it is not new too.
                if (isNew)
                {
                    try
                    {
                        m_cache.remove(m_cache.getArchive(id));
                    }
                    catch (Exception ex1)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Could not remove from cache.", ex1);
                    }
                }

                if (bundle != null)
                {
                    ((ModuleImpl) bundle.getInfo().getCurrentModule()).setRemovalPending(true);
                }

                if ((System.getSecurityManager() != null) &&
                    (ex instanceof SecurityException))
                {
                    throw (SecurityException) ex;
                }

                ex.printStackTrace();

                throw new BundleException("Could not create bundle object.", ex);
            }

            // If the bundle is new, then set its start level; existing
            // bundles already have their start level set.
            if (isNew)
            {
                // This will persistently set the bundle's start level.
                bundle.getInfo().setStartLevel(getInitialBundleStartLevel());
                bundle.getInfo().setLastModified(System.currentTimeMillis());
            }

            synchronized (m_installedBundleLock_Priority2)
            {
                m_installedBundleMap.put(location, bundle);
            }

            if (bundle.getInfo().isExtension())
            {
                FelixBundle systemBundle = (FelixBundle) getBundle(0);
                acquireBundleLock(systemBundle);

                try
                {
                    m_extensionManager.startExtensionBundle(this, bundle);
                }
                finally
                {
                    releaseBundleLock(systemBundle);
                }
            }
        }
        finally
        {
            // Always release install lock.
            releaseInstallLock(location);

            // Always try to close the input stream.
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex)
            {
                m_logger.log(
                    Logger.LOG_ERROR,
                    "Unable to close input stream.", ex);
                // Not much else we can do.
            }
        }

        // Fire bundle event.
        fireBundleEvent(BundleEvent.INSTALLED, bundle);

        // Return new bundle.
        return bundle;
    }

    /**
     * Checks the passed in bundle and checks to see if there is a required execution environment.
     * If there is, it gets the execution environment string and verifies that the framework provides it.
     * @param bundle The bundle to verify
     * @throws BundleException if the bundle's required execution environment does
     *         not match the current execution environment.
    **/
    private void verifyExecutionEnvironment(FelixBundle bundle)
        throws BundleException
    {
        String bundleEnvironment = (String)
            bundle.getInfo().getCurrentHeader().get(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT);
        if (bundleEnvironment != null)
        {
            bundleEnvironment = bundleEnvironment.trim();
            if (!bundleEnvironment.equals(""))
            {
                if (!isMatchingExecutionEnvironment(bundleEnvironment))
                {
                    throw new BundleException("Execution Environment not supported: " + bundleEnvironment);
                }
            }
        }
    }

    /**
     * Check the required bundle execution environment against the framework provided
     * exectution environment.
     * @param bundleEnvironment The required execution environment string
     *        (from Bundle-RequiredExecutionEnvironment manifest header
     * @return True if the required bundle execution environment is provided by the framework
     *         False if none of the provided framework execution environments match
    **/
    private boolean isMatchingExecutionEnvironment(String bundleEnvironment)
    {
        String frameworkEnvironment = getProperty(Constants.FRAMEWORK_EXECUTIONENVIRONMENT);
        if (frameworkEnvironment == null)
        {
            // If no framework execution environment is set, then all are valid
            return true;
        }

        frameworkEnvironment = frameworkEnvironment.trim();
        if ("".equals(frameworkEnvironment))
        {
            // If no framework execution environment is set, then all are valid
            return true;
        }

        // The execution environment has changed, so update the cache and EE set
        if (!m_executionEnvironment.equals(frameworkEnvironment))
        {
            updateFrameworkExecutionEnvironment(frameworkEnvironment);
        }

        StringTokenizer tokens = new StringTokenizer(bundleEnvironment, ",");
        while (tokens.hasMoreTokens())
        {
            if (m_executionEnvironmentCache.contains(tokens.nextToken().trim()))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates the framework wide execution environment string and a cached Set of
     * execution environment tokens from the comma delimited list specified by the
     * system variable 'org.osgi.framework.executionenvironment'.
     * @param frameworkEnvironment Comma delimited string of provided execution environments
    **/
    private void updateFrameworkExecutionEnvironment(String frameworkEnvironment)
    {
        StringTokenizer tokens = new StringTokenizer(frameworkEnvironment, ",");

        Set newSet = new HashSet(tokens.countTokens());
        while (tokens.hasMoreTokens())
        {
            newSet.add(tokens.nextToken().trim());
        }

        synchronized (m_executionEnvironmentCache)
        {
            m_executionEnvironment = frameworkEnvironment;
            m_executionEnvironmentCache = newSet;
        }
    }

    /**
     * Retrieves a bundle from its location.
     *
     * @param location The location of the bundle to retrieve.
     * @return The bundle associated with the location or null if there
     *         is no bundle associated with the location.
    **/
    protected Bundle getBundle(String location)
    {
        synchronized (m_installedBundleLock_Priority2)
        {
            return (Bundle) m_installedBundleMap.get(location);
        }
    }

    /**
     * Implementation for BundleContext.getBundle(). Retrieves a
     * bundle from its identifier.
     *
     * @param id The identifier of the bundle to retrieve.
     * @return The bundle associated with the identifier or null if there
     *         is no bundle associated with the identifier.
    **/
    protected Bundle getBundle(long id)
    {
        synchronized (m_installedBundleLock_Priority2)
        {
            FelixBundle bundle = null;

            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                bundle = (FelixBundle) i.next();
                if (bundle.getInfo().getBundleId() == id)
                {
                    return bundle;
                }
            }
        }

        synchronized (m_uninstalledBundlesLock_Priority3)
        {
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                i++)
            {
                if (m_uninstalledBundles[i].getInfo().getBundleId() == id)
                {
                    return m_uninstalledBundles[i];
                }
            }
        }

        return null;
    }

    // Private member for method below.
    private Comparator m_comparator = null;

    /**
     * Implementation for BundleContext.getBundles(). Retrieves
     * all installed bundles.
     *
     * @return An array containing all installed bundles or null if
     *         there are no installed bundles.
    **/
    protected Bundle[] getBundles()
    {
        if (m_comparator == null)
        {
            m_comparator = new Comparator() {
                public int compare(Object o1, Object o2)
                {
                    Bundle b1 = (Bundle) o1;
                    Bundle b2 = (Bundle) o2;
                    if (b1.getBundleId() > b2.getBundleId())
                        return 1;
                    else if (b1.getBundleId() < b2.getBundleId())
                        return -1;
                    return 0;
                }
            };
        }

        Bundle[] bundles = null;

        synchronized (m_installedBundleLock_Priority2)
        {
            if (m_installedBundleMap.size() == 0)
            {
                return null;
            }

            bundles = new Bundle[m_installedBundleMap.size()];
            int counter = 0;
            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                bundles[counter++] = (Bundle) i.next();
            }
        }

        Arrays.sort(bundles, m_comparator);

        return bundles;
    }

    protected void addBundleListener(Bundle bundle, BundleListener l)
    {
        m_dispatcher.addListener(bundle, BundleListener.class, l, null);
    }

    protected void removeBundleListener(Bundle bundle, BundleListener l)
    {
        m_dispatcher.removeListener(bundle, BundleListener.class, l);
    }

    /**
     * Implementation for BundleContext.addServiceListener().
     * Adds service listener to the listener list so that is
     * can listen for <code>ServiceEvent</code>s.
     *
     * @param bundle The bundle that registered the listener.
     * @param l The service listener to add to the listener list.
     * @param f The filter for the listener; may be null.
    **/
    protected void addServiceListener(Bundle bundle, ServiceListener l, String f)
        throws InvalidSyntaxException
    {
        m_dispatcher.addListener(
            bundle, ServiceListener.class, l, (f == null) ? null : new FilterImpl(m_logger, f));
    }

    /**
     * Implementation for BundleContext.removeServiceListener().
     * Removes service listeners from the listener list.
     *
     * @param bundle The context bundle of the listener
     * @param l The service listener to remove from the listener list.
    **/
    protected void removeServiceListener(Bundle bundle, ServiceListener l)
    {
        m_dispatcher.removeListener(bundle, ServiceListener.class, l);
    }

    protected void addFrameworkListener(Bundle bundle, FrameworkListener l)
    {
        m_dispatcher.addListener(bundle, FrameworkListener.class, l, null);
    }

    protected void removeFrameworkListener(Bundle bundle, FrameworkListener l)
    {
        m_dispatcher.removeListener(bundle, FrameworkListener.class, l);
    }

    /**
     * Implementation for BundleContext.registerService(). Registers
     * a service for the specified bundle bundle.
     *
     * @param classNames A string array containing the names of the classes
     *                under which the new service is available.
     * @param svcObj The service object or <code>ServiceFactory</code>.
     * @param dict A dictionary of properties that further describe the
     *             service or null.
     * @return A <code>ServiceRegistration</code> object or null.
    **/
    protected ServiceRegistration registerService(
        FelixBundle bundle, String[] classNames, Object svcObj, Dictionary dict)
    {
        if (classNames == null)
        {
            throw new NullPointerException("Service class names cannot be null.");
        }
        else if (svcObj == null)
        {
            throw new IllegalArgumentException("Service object cannot be null.");
        }

        // Acquire bundle lock.
        acquireBundleLock(bundle);

        ServiceRegistration reg = null;

        try
        {
            BundleInfo info = bundle.getInfo();

            // Can only register services if starting or active.
            if ((info.getState() & (Bundle.STARTING | Bundle.ACTIVE)) == 0)
            {
                throw new IllegalStateException(
                    "Can only register services while bundle is active or activating.");
            }

            // Check to make sure that the service object is
            // an instance of all service classes; ignore if
            // service object is a service factory.
            if (!(svcObj instanceof ServiceFactory))
            {
                for (int i = 0; i < classNames.length; i++)
                {
                    Class clazz = Util.loadClassUsingClass(svcObj.getClass(), classNames[i]);
                    if (clazz == null)
                    {
                        throw new IllegalArgumentException(
                            "Cannot cast service: " + classNames[i]);
                    }
                    else if (!clazz.isAssignableFrom(svcObj.getClass()))
                    {
                        throw new IllegalArgumentException(
                            "Service object is not an instance of \""
                            + classNames[i] + "\".");
                    }
                }
            }

            reg = m_registry.registerService(bundle, classNames, svcObj, dict);
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }

        // TODO: CONCURRENCY - Reconsider firing event here, outside of the
        // bundle lock.

        // NOTE: The service registered event is fired from the service
        // registry to the framework, where it is then redistributed to
        // interested service event listeners.

        return reg;
    }

    /**
     * Retrieves an array of {@link ServiceReference} objects based on calling bundle,
     * service class name, and filter expression.  Optionally checks for isAssignable to
     * make sure that the service can be cast to the
     * @param bundle Calling Bundle
     * @param className Service Classname or <code>null</code> for all
     * @param expr Filter Criteria or <code>null</code>
     * @return Array of ServiceReference objects that meet the criteria
     * @throws InvalidSyntaxException
     */
    protected ServiceReference[] getServiceReferences(
        FelixBundle bundle, String className, String expr, boolean checkAssignable)
        throws InvalidSyntaxException
    {
        // Define filter if expression is not null.
        Filter filter = null;
        if (expr != null)
        {
            filter = new FilterImpl(m_logger, expr);
        }

        // Ask the service registry for all matching service references.
        List refList = m_registry.getServiceReferences(className, filter);

        // Filter on assignable references
        if (checkAssignable)
        {
            for (int refIdx = 0; (refList != null) && (refIdx < refList.size()); refIdx++)
            {
                // Get the current service reference.
                ServiceReference ref = (ServiceReference) refList.get(refIdx);

                // Now check for castability.
                if (!Util.isServiceAssignable(bundle, ref))
                {
                    refList.remove(refIdx);
                    refIdx--;
                }
            }
        }

        if (refList.size() > 0)
        {
            return (ServiceReference[]) refList.toArray(new ServiceReference[refList.size()]);
        }

        return null;
    }

    /**
     * Retrieves Array of {@link ServiceReference} objects based on calling bundle, service class name,
     * optional filter expression, and optionally filters further on the version.
     * If running under a {@link SecurityManager}, checks that the calling bundle has permissions to
     * see the service references and removes references that aren't.
     * @param bundle Calling Bundle
     * @param className Service Classname or <code>null</code> for all
     * @param expr Filter Criteria or <code>null</code>
     * @param checkAssignable <code>true</code> to check for isAssignable, <code>false</code> to return all versions
     * @return Array of ServiceReference objects that meet the criteria
     * @throws InvalidSyntaxException
     */
    protected ServiceReference[] getAllowedServiceReferences(
        FelixBundle bundle, String className, String expr, boolean checkAssignable)
        throws InvalidSyntaxException
    {
        ServiceReference[] refs = getServiceReferences(bundle, className, expr, checkAssignable);

        Object sm = System.getSecurityManager();

        if ((sm == null) || (refs == null))
        {
            return refs;
        }

        List result = new ArrayList();

        for (int i = 0;i < refs.length;i++)
        {
            String[] objectClass = (String[]) refs[i].getProperty(Constants.OBJECTCLASS);

            if (objectClass == null)
            {
                continue;
            }

            for (int j = 0; j < objectClass.length; j++)
            {
                try
                {
                    ((SecurityManager) sm).checkPermission(new ServicePermission(
                        objectClass[j], ServicePermission.GET));
                    result.add(refs[i]);
                    break;
                }
                catch (Exception ex)
                {
                    // Ignore, since we are just testing permission.
                }
            }
        }

        if (result.isEmpty())
        {
            return null;
        }

        return (ServiceReference[]) result.toArray(new ServiceReference[result.size()]);

    }

    protected Object getService(Bundle bundle, ServiceReference ref)
    {
        // Check that the bundle has permission to get at least
        // one of the service interfaces; the objectClass property
        // of the service stores its service interfaces.
        String[] objectClass = (String[])
            ref.getProperty(Constants.OBJECTCLASS);
        if (objectClass == null)
        {
            return null;
        }

        return m_registry.getService(bundle, ref);
    }

    protected boolean ungetService(Bundle bundle, ServiceReference ref)
    {
        return m_registry.ungetService(bundle, ref);
    }

    protected File getDataFile(FelixBundle bundle, String s)
    {
        // The spec says to throw an error if the bundle
        // is stopped, which I assume means not active,
        // starting, or stopping.
        // Additionally, we make an exception for extension bundles
        if ((bundle.getInfo().getState() != Bundle.ACTIVE) &&
            (bundle.getInfo().getState() != Bundle.STARTING) &&
            (bundle.getInfo().getState() != Bundle.STOPPING) &&
            !bundle.getInfo().isExtension())
        {
            throw new IllegalStateException("Only active bundles can create files.");
        }
        try
        {
            return m_cache.getArchive(
                bundle.getInfo().getBundleId()).getDataFile(s);
        }
        catch (Exception ex)
        {
            m_logger.log(Logger.LOG_ERROR, ex.getMessage());
            return null;
        }
    }

    //
    // PackageAdmin related methods.
    //

    /**
     * This method returns the bundle associated with the specified class if
     * the class was loaded from a bundle from this framework instance. If the
     * class was not loaded from a bundle or was loaded by a bundle in another
     * framework instance, then <tt>null</tt> is returned.
     *
     * @param clazz the class for which to find its associated bundle.
     * @return the bundle associated with the specified class or <tt>null</tt>
     *         if the class was not loaded by a bundle or its associated
     *         bundle belongs to a different framework instance.
    **/
    protected Bundle getBundle(Class clazz)
    {
        if (clazz.getClassLoader() instanceof ContentClassLoader)
        {
            IContentLoader contentLoader =
                ((ContentClassLoader) clazz.getClassLoader()).getContentLoader();
            IModule[] modules = m_factory.getModules();
            for (int i = 0; i < modules.length; i++)
            {
                if (modules[i].getContentLoader() == contentLoader)
                {
                    long id = Util.getBundleIdFromModuleId(modules[i].getId());
                    return getBundle(id);
                }
            }
        }
        return (getInfo().getCurrentModule().getClass(clazz.getName()) == clazz) ? this : null;
    }

    /**
     * Returns the exported packages associated with the specified
     * package name. This is used by the PackageAdmin service
     * implementation.
     *
     * @param pkgName The name of the exported package to find.
     * @return The exported package or null if no matching package was found.
    **/
    protected ExportedPackage[] getExportedPackages(String pkgName)
    {
        // First, get all exporters of the package.
        R4SearchPolicyCore.PackageSource[] exporters =
            m_policyCore.getInUseCandidates(
                new Requirement(
                    ICapability.PACKAGE_NAMESPACE,
                    null,
                    null,
                    new R4Attribute[] { new R4Attribute(ICapability.PACKAGE_PROPERTY, pkgName, false) }));

        if (exporters != null)
        {
            List pkgs = new ArrayList();

            Requirement req = new Requirement(ICapability.PACKAGE_NAMESPACE,
                null,
                null,
                new R4Attribute[] { new R4Attribute(ICapability.PACKAGE_PROPERTY, pkgName, false) });

            for (int pkgIdx = 0; pkgIdx < exporters.length; pkgIdx++)
            {
                // Get the bundle associated with the current exporting module.
                FelixBundle bundle = (FelixBundle) getBundle(
                    Util.getBundleIdFromModuleId(exporters[pkgIdx].m_module.getId()));

                // We need to find the version of the exported package, but this
                // is tricky since there may be multiple versions of the package
                // offered by a given bundle, since multiple revisions of the
                // bundle JAR file may exist if the bundle was updated without
                // refreshing the framework. In this case, each revision of the
                // bundle JAR file is represented as a module in the BundleInfo
                // module array, which is ordered from oldest to newest. We assume
                // that the first module found to be exporting the package is the
                // provider of the package, which makes sense since it must have
                // been resolved first.
                IModule[] modules = bundle.getInfo().getModules();
                for (int modIdx = 0; modIdx < modules.length; modIdx++)
                {
                    ICapability[] ec = modules[modIdx].getDefinition().getCapabilities();
                    for (int i = 0; (ec != null) && (i < ec.length); i++)
                    {
                        if (ec[i].getNamespace().equals(req.getNamespace()) &&
                            req.isSatisfied(ec[i]))
                        {
                            pkgs.add(new ExportedPackageImpl(this, bundle, modules[modIdx], (Capability) ec[i]));
                        }
                    }
                }
            }

            return (pkgs.isEmpty()) ? null : (ExportedPackage[]) pkgs.toArray(new ExportedPackage[pkgs.size()]);
        }

        return null;
    }

    /**
     * Returns an array of all actively exported packages from the specified
     * bundle or if the specified bundle is <tt>null</tt> an array
     * containing all actively exported packages by all bundles.
     *
     * @param b The bundle whose exported packages are to be retrieved
     *        or <tt>null</tt> if the exported packages of all bundles are
     *        to be retrieved.
     * @return An array of exported packages.
    **/
    protected ExportedPackage[] getExportedPackages(Bundle b)
    {
        List list = new ArrayList();

        // If a bundle is specified, then return its
        // exported packages.
        if (b != null)
        {
            FelixBundle bundle = (FelixBundle) b;
            getExportedPackages(bundle, list);
        }
        // Otherwise return all exported packages.
        else
        {
            // To create a list of all exported packages, we must look
            // in the installed and uninstalled sets of bundles. To
            // ensure a somewhat consistent view, we will gather all
            // of this information from within the installed bundle
            // lock.
            synchronized (m_installedBundleLock_Priority2)
            {
                // First get exported packages from uninstalled bundles.
                synchronized (m_uninstalledBundlesLock_Priority3)
                {
                    for (int bundleIdx = 0;
                        (m_uninstalledBundles != null) && (bundleIdx < m_uninstalledBundles.length);
                        bundleIdx++)
                    {
                        FelixBundle bundle = m_uninstalledBundles[bundleIdx];
                        getExportedPackages(bundle, list);
                    }
                }

                // Now get exported packages from installed bundles.
                Bundle[] bundles = getBundles();
                for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
                {
                    FelixBundle bundle = (FelixBundle) bundles[bundleIdx];
                    getExportedPackages(bundle, list);
                }
            }
        }

        return (ExportedPackage[]) list.toArray(new ExportedPackage[list.size()]);
    }

    /**
     * Adds any current active exported packages from the specified bundle
     * to the passed in list.
     * @param bundle The bundle from which to retrieve exported packages.
     * @param list The list to which the exported packages are added
    **/
    private void getExportedPackages(FelixBundle bundle, List list)
    {
        // Since a bundle may have many modules associated with it,
        // one for each revision in the cache, search each module
        // for each revision to get all exports.
        IModule[] modules = bundle.getInfo().getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            ICapability[] caps = modules[modIdx].getDefinition().getCapabilities();
            if ((caps != null) && (caps.length > 0))
            {
                for (int capIdx = 0; capIdx < caps.length; capIdx++)
                {
                    // See if the target bundle's module is one of the
                    // "in use" exporters of the package.
                    if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        R4SearchPolicyCore.PackageSource[] inUseModules = m_policyCore.getInUseCandidates(
                            new Requirement(
                                ICapability.PACKAGE_NAMESPACE,
                                null,
                                null,
                                new R4Attribute[] { new R4Attribute(ICapability.PACKAGE_PROPERTY, ((Capability) caps[capIdx]).getPackageName(), false) }));

                        // Search through the current providers to find the target
                        // module.
                        for (int i = 0; (inUseModules != null) && (i < inUseModules.length); i++)
                        {
                            if (inUseModules[i].m_module == modules[modIdx])
                            {
                                list.add(new ExportedPackageImpl(
                                    this, bundle, modules[modIdx], (Capability) caps[capIdx]));
                            }
                        }
                    }
                }
            }
        }
    }

    protected Bundle[] getDependentBundles(FelixBundle exporter)
    {
        // Get exporting bundle.
        BundleInfo exporterInfo = exporter.getInfo();

        // Create list for storing importing bundles.
        List list = new ArrayList();

        // Get all dependent modules from all exporter module revisions.
        IModule[] modules = exporterInfo.getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            IModule[] dependents = ((ModuleImpl) modules[modIdx]).getDependents();
            for (int depIdx = 0;
                (dependents != null) && (depIdx < dependents.length);
                depIdx++)
            {
                Bundle b = getBundle(Util.getBundleIdFromModuleId(dependents[depIdx].getId()));
                list.add(b);
            }
        }

        // Return the results.
        if (list.size() > 0)
        {
            return (Bundle[]) list.toArray(new Bundle[list.size()]);
        }

        return null;
    }

    protected Bundle[] getImportingBundles(ExportedPackage ep)
    {
        // Create list for storing importing bundles.
        List list = new ArrayList();

        // Get exporting bundle information.
        FelixBundle exporter = (FelixBundle)
            ((ExportedPackage) ep).getExportingBundle();

        // Search the dependents of the exporter's module revisions
        // for importers of the specific package.
        IModule[] expModules = exporter.getInfo().getModules();
        for (int expIdx = 0; (expModules != null) && (expIdx < expModules.length); expIdx++)
        {
            IModule[] depModules = ((ModuleImpl) expModules[expIdx]).getDependents();
            for (int depIdx = 0; (depModules != null) && (depIdx < depModules.length); depIdx++)
            {
                // ExportedPackage.getImportingBundles() does not expect bundles
                // to depend on themselves, so we will filter that case here.
                if (!expModules[expIdx].equals(depModules[depIdx]))
                {
                    // See if the dependent module has a wire for the specific
                    // package. If so, see if the provider module is from the
                    // exporter and record it if it is.
                    IWire wire = Util.getWire(depModules[depIdx], ep.getName());
                    if ((wire != null) && expModules[expIdx].equals(wire.getExporter()) &&
                        wire.getRequirement().isSatisfied(
                        new Capability(ICapability.PACKAGE_NAMESPACE, null, new R4Attribute[] {
                            new R4Attribute(ICapability.PACKAGE_PROPERTY, ep.getName(), false),
                            new R4Attribute(ICapability.VERSION_PROPERTY, ep.getVersion(), false)
                        })))
                    {
                        // Add the bundle to the list of importers.
                        list.add(getBundle(Util.getBundleIdFromModuleId(depModules[depIdx].getId())));
                    }
                }
            }
        }

        // Return the results.
        if (list.size() > 0)
        {
            return (Bundle[]) list.toArray(new Bundle[list.size()]);
        }

        return null;
    }

    protected boolean resolveBundles(Bundle[] targets)
    {
        // Acquire locks for all bundles to be resolved.
        FelixBundle[] bundles = acquireBundleResolveLocks(targets);

        try
        {
            boolean result = true;

            // If there are targets, then resolve each one.
            if (bundles != null)
            {
                for (int i = 0; i < bundles.length; i++)
                {
                    try
                    {
                        _resolveBundle(bundles[i]);
                    }
                    catch (BundleException ex)
                    {
                        result = false;
                        m_logger.log(
                            Logger.LOG_WARNING,
                            "Unable to resolve bundle " + bundles[i].getBundleId(),
                            ex);
                    }
                }
            }

            return result;
        }
        finally
        {
            // Always release all bundle locks.
            releaseBundleLocks(bundles);
        }
    }

    protected void refreshPackages(Bundle[] targets)
    {
        // Acquire locks for all impacted bundles.
        FelixBundle[] bundles = acquireBundleRefreshLocks(targets);

        boolean restart = false;

        Bundle systemBundle = getBundle(0);

        // We need to restart the framework if either an extension bundle is
        // refreshed or the system bundle is refreshed and any extension bundle
        // has been updated or uninstalled.
        for (int i = 0; (bundles != null) && !restart && (i < bundles.length); i++)
        {
            if (bundles[i].getInfo().isExtension())
            {
                restart = true;
            }
            else if (systemBundle == bundles[i])
            {
                Bundle[] allBundles = getBundles();
                for (int j = 0; !restart && j < allBundles.length; j++)
                {
                    if (((FelixBundle) allBundles[j]).getInfo().isExtension() &&
                        (allBundles[j].getState() == Bundle.INSTALLED))
                    {
                        restart = true;
                    }
                }
            }
        }

        if (restart)
        {
// TODO: Extension Bundle - We need a way to restart the framework
            m_logger.log(Logger.LOG_WARNING, "Framework restart not implemented.");
        }

        // Remove any targeted bundles from the uninstalled bundles
        // array, since they will be removed from the system after
        // the refresh.
        for (int i = 0; (bundles != null) && (i < bundles.length); i++)
        {
            forgetUninstalledBundle(bundles[i]);
        }

        try
        {
            // If there are targets, then refresh each one.
            if (bundles != null)
            {
                // At this point the map contains every bundle that has been
                // updated and/or removed as well as all bundles that import
                // packages from these bundles.

                // Create refresh helpers for each bundle.
                RefreshHelper[] helpers = new RefreshHelper[bundles.length];
                for (int i = 0; i < bundles.length; i++)
                {
                    if (!bundles[i].getInfo().isExtension())
                    {
                        helpers[i] = new RefreshHelper(bundles[i]);
                    }
                }

                // Stop, purge or remove, and reinitialize all bundles first.
                for (int i = 0; i < helpers.length; i++)
                {
                    if (helpers[i] != null)
                    {
                        helpers[i].stop();
                        helpers[i].purgeOrRemove();
                        helpers[i].reinitialize();
                    }
                }

                // Then restart all bundles that were previously running.
                for (int i = 0; i < helpers.length; i++)
                {
                    if (helpers[i] != null)
                    {
                        helpers[i].restart();
                    }
                }
            }
        }
        finally
        {
            // Always release all bundle locks.
            releaseBundleLocks(bundles);
        }

        fireFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, this, null);
    }

    private void populateImportGraph(FelixBundle exporter, Map map)
    {
        // Get all dependent bundles of this bundle.
        Bundle[] importers = getDependentBundles(exporter);

        for (int impIdx = 0;
            (importers != null) && (impIdx < importers.length);
            impIdx++)
        {
            // Avoid cycles if the bundle is already in map.
            if (!map.containsKey(importers[impIdx]))
            {
                // Add each importing bundle to map.
                map.put(importers[impIdx], importers[impIdx]);
                // Now recurse into each bundle to get its importers.
                populateImportGraph(
                    (FelixBundle) importers[impIdx], map);
            }
        }
    }

    //
    // Miscellaneous private methods.
    //

    private BundleInfo createBundleInfo(BundleArchive archive, boolean isExtension)
        throws Exception
    {
        // Get the bundle manifest.
        Map headerMap = null;
        try
        {
            // Although there should only ever be one revision at this
            // point, get the header for the current revision to be safe.
            headerMap = archive.getRevision(archive.getRevisionCount() - 1).getManifestHeader();
        }
        catch (Exception ex)
        {
            throw new BundleException("Unable to read JAR manifest.", ex);
        }

        // We can't do anything without the manifest header.
        if (headerMap == null)
        {
            throw new BundleException("Unable to read JAR manifest header.");
        }

        // Create the module for the bundle; although there should only
        // ever be one revision at this point, create the module for
        // the current revision to be safe.
        IModule module = createModule(
            archive.getId(), archive.getRevisionCount() - 1, headerMap,
            createBundleProtectionDomain(archive), isExtension);

        // Finally, create an return the bundle info.
        BundleInfo info = new BundleInfo(m_logger, archive, module);
        info.setExtension(isExtension);

        return info;
    }

    private ProtectionDomain createBundleProtectionDomain(BundleArchive archive)
        throws Exception
    {
// TODO: Security - create a real ProtectionDomain for the Bundle
        FakeURLStreamHandler handler = new FakeURLStreamHandler();
        URL context = new URL(null, "location:", handler);
        CodeSource codesource = new CodeSource(m_secureAction.createURL(context,
            archive.getLocation(), handler), (Certificate[]) null);

        Permissions allPerms = new Permissions();
        allPerms.add(new AllPermission());
        ProtectionDomain pd = new ProtectionDomain(codesource,
            allPerms);
        return pd;
    }

    /**
     * Creates a module for a given bundle by reading the bundle's
     * manifest meta-data and converting it to work with the underlying
     * import/export search policy of the module loader.
     * @param targetId The identifier of the bundle for which the module should
     *        be created.
     * @param headerMap The headers map associated with the bundle.
     * @return The initialized and/or newly created module.
    **/
    private IModule createModule(long targetId, int revision, Map headerMap,
        Object securityContext, boolean isExtensionBundle)
        throws Exception
    {
        ManifestParser mp = new ManifestParser(m_logger, m_configMap, headerMap);

        // Verify that the bundle symbolic name and version is unique.
        if (mp.getManifestVersion().equals("2"))
        {
            Version bundleVersion = mp.getBundleVersion();
            bundleVersion = (bundleVersion == null) ? Version.emptyVersion : bundleVersion;
            String symName = (String) mp.getSymbolicName();

            Bundle[] bundles = getBundles();
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                long id = ((FelixBundle) bundles[i]).getInfo().getBundleId();
                String sym = (String) ((FelixBundle) bundles[i])
                    .getInfo().getCurrentHeader().get(Constants.BUNDLE_SYMBOLICNAME);
                Version ver = Version.parseVersion((String) ((FelixBundle) bundles[i])
                    .getInfo().getCurrentHeader().get(Constants.BUNDLE_VERSION));
                if (symName.equals(sym) && bundleVersion.equals(ver) && (targetId != id))
                {
                    throw new BundleException("Bundle symbolic name and version are not unique.");
                }
            }
        }

        // Now that we have all of the metadata associated with the
        // module, we need to create the module itself. This is somewhat
        // complicated because a module is constructed out of several
        // interrelated pieces (e.g., module definition, content loader,
        // search policy, url policy). We need to create all of these
        // pieces and bind them together.

        // Create the module definition for the new module.
        // Note, in case this is an extension bundle it's exports are removed -
        // they will be added to the system bundle directly later on.
        IModuleDefinition md = new ModuleDefinition(
            (isExtensionBundle) ? null : mp.getCapabilities(),
            mp.getRequirements(),
            mp.getDynamicRequirements(),
            mp.getLibraries(m_cache.getArchive(targetId).getRevision(revision)));

        // Create the module using the module definition.
        IModule module = m_factory.createModule(
            Long.toString(targetId) + "." + Integer.toString(revision), md);

        m_factory.setSecurityContext(module, securityContext);

        // Create the content loader from the module archive.
        IContentLoader contentLoader = new ContentLoaderImpl(
                m_logger,
                m_cache.getArchive(targetId).getRevision(revision).getContent(),
                m_cache.getArchive(targetId).getRevision(revision).getContentPath(),
                (ProtectionDomain) module.getSecurityContext());
        // Set the content loader's search policy.
        contentLoader.setSearchPolicy(
                new R4SearchPolicy(m_policyCore, module));
        // Set the content loader's URL policy.
        contentLoader.setURLPolicy(
// TODO: ML - SUCKS NEEDING URL POLICY PER MODULE.
                new URLPolicyImpl(
                    m_logger, m_bundleStreamHandler, module));

        // Set the module's content loader to the created content loader.
        m_factory.setContentLoader(module, contentLoader);

        // Done, so return the module.
        return module;
    }

    private BundleActivator createBundleActivator(BundleInfo info)
        throws Exception
    {
        // CONCURRENCY NOTE:
        // This method is called indirectly from startBundle() (via _startBundle()),
        // which has the exclusion lock, so there is no need to do any locking here.

        BundleActivator activator = null;

        String strict = (String) m_configMap.get(FelixConstants.STRICT_OSGI_PROP);
        boolean isStrict = (strict == null) ? true : strict.equals("true");
        if (!isStrict)
        {
            try
            {
                activator =
                    m_cache.getArchive(info.getBundleId())
                        .getActivator(info.getCurrentModule());
            }
            catch (Exception ex)
            {
                activator = null;
            }
        }

        // If there was no cached activator, then get the activator
        // class from the bundle manifest.
        if (activator == null)
        {
            // Get the associated bundle archive.
            BundleArchive ba = m_cache.getArchive(info.getBundleId());
            // Get the manifest from the current revision; revision is
            // base zero so subtract one from the count to get the
            // current revision.
            Map headerMap = ba.getRevision(ba.getRevisionCount() - 1).getManifestHeader();
            // Get the activator class attribute.
            String className = (String) headerMap.get(Constants.BUNDLE_ACTIVATOR);
            // Try to instantiate activator class if present.
            if (className != null)
            {
                className = className.trim();
                Class clazz = info.getCurrentModule().getClass(className);
                if (clazz == null)
                {
                    throw new BundleException("Not found: "
                        + className, new ClassNotFoundException(className));
                }
                activator = (BundleActivator) clazz.newInstance();
            }
        }

        return activator;
    }

    private void purgeBundle(FelixBundle bundle) throws Exception
    {
        // Acquire bundle lock.
        acquireBundleLock(bundle);

        try
        {
            BundleInfo info = bundle.getInfo();

            // In case of a refresh, then we want to physically
            // remove the bundle's modules from the module manager.
            // This is necessary for two reasons: 1) because
            // under Windows we won't be able to delete the bundle
            // because files might be left open in the resource
            // sources of its modules and 2) we want to make sure
            // that no references to old modules exist since they
            // will all be stale after the refresh. The only other
            // way to do this is to remove the bundle, but that
            // would be incorrect, because this is a refresh operation
            // and should not trigger bundle REMOVE events.
            IModule[] modules = info.getModules();
            for (int i = 0; i < modules.length; i++)
            {
                m_factory.removeModule(modules[i]);
            }

            // Purge all bundle revisions, but the current one.
            m_cache.getArchive(info.getBundleId()).purge();
        }
        finally
        {
            // Always release the bundle lock.
            releaseBundleLock(bundle);
        }
    }

    private void garbageCollectBundle(FelixBundle bundle) throws Exception
    {
        // CONCURRENCY NOTE: There is no reason to lock this bundle,
        // because this method is only called during shutdown or a
        // refresh operation and these are already guarded by locks.

        // Remove the bundle's associated modules from
        // the module manager.
        IModule[] modules = bundle.getInfo().getModules();
        for (int i = 0; i < modules.length; i++)
        {
            m_factory.removeModule(modules[i]);
        }

        // Remove the bundle from the cache.
        m_cache.remove(m_cache.getArchive(bundle.getInfo().getBundleId()));
    }

    //
    // Event-related methods.
    //

    /**
     * Fires bundle events.
    **/
    private void fireFrameworkEvent(
        int type, Bundle bundle, Throwable throwable)
    {
        m_dispatcher.fireFrameworkEvent(new FrameworkEvent(type, bundle, throwable));
    }

    /**
     * Fires bundle events.
     *
     * @param type The type of bundle event to fire.
     * @param bundle The bundle associated with the event.
    **/
    private void fireBundleEvent(int type, Bundle bundle)
    {
        m_dispatcher.fireBundleEvent(new BundleEvent(type, bundle));
    }

    /**
     * Fires service events.
     *
     * @param type The type of service event to fire.
     * @param ref The service reference associated with the event.
    **/
    private void fireServiceEvent(ServiceEvent event)
    {
        m_dispatcher.fireServiceEvent(event);
    }

    //
    // Property related methods.
    //

    private void initializeFrameworkProperties()
    {
        // Standard OSGi properties.
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_VERSION,
            FelixConstants.FRAMEWORK_VERSION_VALUE);
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_VENDOR,
            FelixConstants.FRAMEWORK_VENDOR_VALUE);
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_LANGUAGE,
            System.getProperty("user.language"));
        m_configMutableMap.put(
            FelixConstants.FRAMEWORK_OS_VERSION,
            System.getProperty("os.version"));

        String s = null;
        s = R4LibraryClause.normalizeOSName(System.getProperty("os.name"));
        m_configMutableMap.put(FelixConstants.FRAMEWORK_OS_NAME, s);
        s = R4LibraryClause.normalizeProcessor(System.getProperty("os.arch"));
        m_configMutableMap.put(FelixConstants.FRAMEWORK_PROCESSOR, s);
        m_configMutableMap.put(
            FelixConstants.FELIX_VERSION_PROPERTY, getFrameworkVersion());
    }

    /**
     * Read the framework version from the property file.
     * @return the framework version as a string.
    **/
    private static String getFrameworkVersion()
    {
        // The framework version property.
        Properties props = new Properties();
        InputStream in = Felix.class.getResourceAsStream("Felix.properties");
        if (in != null)
        {
            try
            {
                props.load(in);
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }

        // Maven uses a '-' to separate the version qualifier,
        // while OSGi uses a '.', so we need to convert to a '.'
        StringBuffer sb =
            new StringBuffer(
                props.getProperty(
                    FelixConstants.FELIX_VERSION_PROPERTY, "0.0.0"));
        if (sb.toString().indexOf("-") >= 0)
        {
            sb.setCharAt(sb.toString().indexOf("-"), '.');
        }
        return sb.toString();
    }

    //
    // Private utility methods.
    //

    /**
     * Generated the next valid bundle identifier.
    **/
    private long loadNextId()
    {
        synchronized (m_nextIdLock)
        {
            // Read persisted next bundle identifier.
            InputStream is = null;
            BufferedReader br = null;
            try
            {
                File file = m_cache.getSystemBundleDataFile("bundle.id");
                is = m_secureAction.getFileInputStream(file);
                br = new BufferedReader(new InputStreamReader(is));
                return Long.parseLong(br.readLine());
            }
            catch (FileNotFoundException ex)
            {
                // Ignore this case because we assume that this is the
                // initial startup of the framework and therefore the
                // file does not exist yet.
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Unable to initialize next bundle identifier from persistent storage.",
                    ex);
            }
            finally
            {
                try
                {
                    if (br != null) br.close();
                    if (is != null) is.close();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Unable to close next bundle identifier file.",
                        ex);
                }
            }
        }

        return -1;
    }

    private long getNextId()
    {
        synchronized (m_nextIdLock)
        {
            // Save the current id.
            long id = m_nextId;

            // Increment the next id.
            m_nextId++;

            // Write the bundle state.
            OutputStream os = null;
            BufferedWriter bw = null;
            try
            {
                File file = m_cache.getSystemBundleDataFile("bundle.id");
                os = m_secureAction.getFileOutputStream(file);
                bw = new BufferedWriter(new OutputStreamWriter(os));
                String s = Long.toString(m_nextId);
                bw.write(s, 0, s.length());
            }
            catch (Exception ex)
            {
                m_logger.log(
                    Logger.LOG_WARNING,
                    "Unable to save next bundle identifier to persistent storage.",
                    ex);
            }
            finally
            {
                try
                {
                    if (bw != null) bw.close();
                    if (os != null) os.close();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Unable to close next bundle identifier file.",
                        ex);
                }
            }

            return id;
        }
    }

    //
    // Miscellaneous inner classes.
    //

    class SystemBundleActivator implements BundleActivator, Runnable
    {
        public void start(BundleContext context) throws Exception
        {
            // Create an activator list if necessary.
            if (m_activatorList == null)
            {
                m_activatorList = new ArrayList();
            }

            // Add the bundle activator for the package admin service.
            m_activatorList.add(0, new PackageAdminActivator(Felix.this));
            // Add the bundle activator for the start level service.
            m_activatorList.add(0, new StartLevelActivator(m_logger, Felix.this));
            // Add the bundle activator for the url handler service.
            m_activatorList.add(0, new URLHandlersActivator(m_configMap, Felix.this));

            // Start all activators.
            for (int i = 0; i < m_activatorList.size(); i++)
            {
                Felix.m_secureAction.startActivator(
                    (BundleActivator) m_activatorList.get(i), context);
            }
        }

        public void stop(BundleContext context)
        {
            // Spec says stop() on SystemBundle should return immediately and
            // shutdown framework on another thread.
            if (m_shutdownThread == null)
            {
                // Initial call of stop, so kick off shutdown.
                m_shutdownThread = new Thread(this, "FelixShutdown");
                m_shutdownThread.start();
            }
        }

        public void run()
        {
            // First, start the framework shutdown, which will
            // stop all bundles.
            synchronized (Felix.this)
            {
                // Change framework state from active to stopping.
                // If framework is not active, then just return.
                if (m_systemBundleInfo.getState() != Bundle.STOPPING)
                {
                    return;
                }
            }

            // Use the start level service to set the start level to zero
            // in order to stop all bundles in the framework. Since framework
            // shutdown happens on its own thread, we can wait for the start
            // level service to finish before proceeding by calling the
            // non-spec setStartLevelAndWait() method.
            try
            {
                StartLevelImpl sl = (StartLevelImpl) getService(
                    Felix.this,
                    getServiceReferences(Felix.this, StartLevel.class.getName(), null, true)[0]);
                sl.setStartLevelAndWait(0);
            }
            catch (InvalidSyntaxException ex)
            {
                // Should never happen.
            }

            // Since there may be updated and uninstalled bundles that
            // have not been refreshed, we will take care of refreshing
            // them during shutdown.

            // First loop through all bundled and purge old revisions
            // from updated bundles.
            Bundle[] bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                FelixBundle bundle = (FelixBundle) bundles[i];
                if (bundle.getInfo().getArchive().getRevisionCount() > 1)
                {
                    try
                    {
                        purgeBundle(bundle);
                    }
                    catch (Exception ex)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                        m_logger.log(Logger.LOG_ERROR, "Unable to purge bundle "
                            + bundle.getInfo().getLocation(), ex);
                    }
                }
            }

            // Next garbage collection any uninstalled bundles.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                i++)
            {
                try
                {
                    garbageCollectBundle(m_uninstalledBundles[i]);
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to remove "
                        + m_uninstalledBundles[i].getInfo().getLocation(), ex);
                }
            }

            // Shutdown event dispatching queue.
            EventDispatcher.shutdown();

            // Remove all bundles from the module factory so that any
            // open resources will be closed.
            bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                FelixBundle bundle = (FelixBundle) bundles[i];
                IModule[] modules = bundle.getInfo().getModules();
                for (int j = 0; j < modules.length; j++)
                {
                    try
                    {
                        m_factory.removeModule(modules[j]);
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(Logger.LOG_ERROR,
                           "Unable to clean up " + bundle.getInfo().getLocation(), ex);
                    }
                }
            }

            // Next, stop all system bundle activators.
            if (m_activatorList != null)
            {
                // Stop all activators.
                for (int i = 0; i < m_activatorList.size(); i++)
                {
                    try
                    {
                        if ((m_activatorContextMap != null) &&
                            m_activatorContextMap.containsKey(m_activatorList.get(i)))
                        {
                            Felix.m_secureAction.stopActivator((BundleActivator) m_activatorList.get(i),
                                (BundleContext) m_activatorContextMap.get(m_activatorList.get(i)));
                        }
                        else
                        {
                            Felix.m_secureAction.stopActivator((BundleActivator) m_activatorList.get(i),
                                getInfo().getBundleContext());
                        }
                    }
                    catch (Throwable throwable)
                    {
                        m_logger.log(
                            Logger.LOG_WARNING,
                            "Exception stopping a system bundle activator.",
                            throwable);
                    }
                }
            }

            if (m_extensionManager != null)
            {
                m_extensionManager.removeExtensions(Felix.this);
            }

            // Notify any waiters that the framework is back in its initial state.
            synchronized (Felix.this)
            {
                m_systemBundleInfo.setState(Bundle.UNINSTALLED);
                Felix.this.notifyAll();
            }

            // Finally shutdown the JVM if the framework is running stand-alone.
            String embedded = (String) m_configMap.get(FelixConstants.EMBEDDED_EXECUTION_PROP);
            boolean isEmbedded = (embedded == null) ? false : embedded.equals("true");
            if (!isEmbedded)
            {
                m_secureAction.exit(0);
            }
        }
    }

    /**
     * Simple class that is used in <tt>refreshPackages()</tt> to embody
     * the refresh logic in order to keep the code clean. This class is
     * not static because it needs access to framework event firing methods.
    **/
    private class RefreshHelper
    {
        private FelixBundle m_bundle = null;

        public RefreshHelper(Bundle bundle)
        {
            m_bundle = (FelixBundle) bundle;
        }

        public void stop()
        {
            if (m_bundle.getInfo().getState() == Bundle.ACTIVE)
            {
                try
                {
                    stopBundle(m_bundle, false);
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }

        public void purgeOrRemove()
        {
            try
            {
                BundleInfo info = m_bundle.getInfo();

                // Mark the bundle as stale.
                info.setStale();

                // Remove or purge the bundle depending on its
                // current state.
                if (info.getState() == Bundle.UNINSTALLED)
                {
                    // This physically removes the bundle from memory
                    // as well as the bundle cache.
                    garbageCollectBundle(m_bundle);
                    m_bundle = null;
                }
                else
                {
                    // This physically removes all old revisions of the
                    // bundle from memory and only maintains the newest
                    // version in the bundle cache.
                    purgeBundle(m_bundle);
                }
            }
            catch (Exception ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
            }
        }

        public void reinitialize()
        {
            if (m_bundle != null)
            {
                try
                {
                    BundleInfo info = m_bundle.getInfo();
                    BundleInfo newInfo = createBundleInfo(info.getArchive(),
                        info.isExtension());
                    newInfo.syncLock(info);
                    ((BundleImpl) m_bundle).setInfo(newInfo);
                    fireBundleEvent(BundleEvent.UNRESOLVED, m_bundle);
                }
                catch (Exception ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }

        public void restart()
        {
            if (m_bundle != null)
            {
                try
                {
                    startBundle(m_bundle, false);
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }
    }

    //
    // Locking related methods.
    //

    private void rememberUninstalledBundle(FelixBundle bundle)
    {
        synchronized (m_uninstalledBundlesLock_Priority3)
        {
            // Verify that the bundle is not already in the array.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                i++)
            {
                if (m_uninstalledBundles[i] == bundle)
                {
                    return;
                }
            }

            if (m_uninstalledBundles != null)
            {
                FelixBundle[] newBundles =
                    new FelixBundle[m_uninstalledBundles.length + 1];
                System.arraycopy(m_uninstalledBundles, 0,
                    newBundles, 0, m_uninstalledBundles.length);
                newBundles[m_uninstalledBundles.length] = bundle;
                m_uninstalledBundles = newBundles;
            }
            else
            {
                m_uninstalledBundles = new FelixBundle[] { bundle };
            }
        }
    }

    private void forgetUninstalledBundle(FelixBundle bundle)
    {
        synchronized (m_uninstalledBundlesLock_Priority3)
        {
            if (m_uninstalledBundles == null)
            {
                return;
            }

            int idx = -1;
            for (int i = 0; i < m_uninstalledBundles.length; i++)
            {
                if (m_uninstalledBundles[i] == bundle)
                {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0)
            {
                // If this is the only bundle, then point to empty list.
                if ((m_uninstalledBundles.length - 1) == 0)
                {
                    m_uninstalledBundles = new FelixBundle[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    FelixBundle[] newBundles =
                        new FelixBundle[m_uninstalledBundles.length - 1];
                    System.arraycopy(m_uninstalledBundles, 0, newBundles, 0, idx);
                    if (idx < newBundles.length)
                    {
                        System.arraycopy(
                            m_uninstalledBundles, idx + 1,
                            newBundles, idx, newBundles.length - idx);
                    }
                    m_uninstalledBundles = newBundles;
                }
            }
        }
    }

    protected void acquireInstallLock(String location)
        throws BundleException
    {
        synchronized (m_installRequestLock_Priority1)
        {
            while (m_installRequestMap.get(location) != null)
            {
                try
                {
                    m_installRequestLock_Priority1.wait();
                }
                catch (InterruptedException ex)
                {
                    throw new BundleException("Unable to install, thread interrupted.");
                }
            }

            m_installRequestMap.put(location, location);
        }
    }

    protected void releaseInstallLock(String location)
    {
        synchronized (m_installRequestLock_Priority1)
        {
            m_installRequestMap.remove(location);
            m_installRequestLock_Priority1.notifyAll();
        }
    }

    protected void acquireBundleLock(FelixBundle bundle)
    {
        synchronized (m_bundleLock)
        {
            while (!bundle.getInfo().isLockable())
            {
                try
                {
                    m_bundleLock.wait();
                }
                catch (InterruptedException ex)
                {
                    // Ignore and just keep waiting.
                }
            }
            bundle.getInfo().lock();
        }
    }

    protected boolean acquireBundleLockOrFail(FelixBundle bundle)
    {
        synchronized (m_bundleLock)
        {
            if (!bundle.getInfo().isLockable())
            {
                return false;
            }
            bundle.getInfo().lock();
            return true;
        }
    }

    protected void releaseBundleLock(FelixBundle bundle)
    {
        synchronized (m_bundleLock)
        {
            bundle.getInfo().unlock();
            m_bundleLock.notifyAll();
        }
    }

    protected FelixBundle[] acquireBundleResolveLocks(Bundle[] targets)
    {
        // Hold bundles to be locked.
        FelixBundle[] bundles = null;
        // Convert existing target bundle array to bundle impl array.
        if (targets != null)
        {
            bundles = new FelixBundle[targets.length];
            for (int i = 0; i < targets.length; i++)
            {
                bundles[i] = (FelixBundle) targets[i];
            }
        }

        synchronized (m_bundleLock)
        {
            boolean success = false;
            while (!success)
            {
                // If targets is null, then resolve all unresolved bundles.
                if (targets == null)
                {
                    List list = new ArrayList();

                    // Add all unresolved bundles to the list.
                    synchronized (m_installedBundleLock_Priority2)
                    {
                        Iterator iter = m_installedBundleMap.values().iterator();
                        while (iter.hasNext())
                        {
                            FelixBundle bundle = (FelixBundle) iter.next();
                            if (bundle.getInfo().getState() == Bundle.INSTALLED)
                            {
                                list.add(bundle);
                            }
                        }
                    }

                    // Create an array.
                    if (list.size() > 0)
                    {
                        bundles = (FelixBundle[]) list.toArray(new FelixBundle[list.size()]);
                    }
                }

                // Check if all unresolved bundles can be locked.
                boolean lockable = true;
                if (bundles != null)
                {
                    for (int i = 0; lockable && (i < bundles.length); i++)
                    {
                        lockable = bundles[i].getInfo().isLockable();
                    }

                    // If we can lock all bundles, then lock them.
                    if (lockable)
                    {
                        for (int i = 0; i < bundles.length; i++)
                        {
                            bundles[i].getInfo().lock();
                        }
                        success = true;
                    }
                    // Otherwise, wait and try again.
                    else
                    {
                        try
                        {
                            m_bundleLock.wait();
                        }
                        catch (InterruptedException ex)
                        {
                            // Ignore and just keep waiting.
                        }
                    }
                }
                else
                {
                    // If there were no bundles to lock, then we can just
                    // exit the lock loop.
                    success = true;
                }
            }
        }

        return bundles;
    }

    protected FelixBundle[] acquireBundleRefreshLocks(Bundle[] targets)
    {
        // Hold bundles to be locked.
        FelixBundle[] bundles = null;

        synchronized (m_bundleLock)
        {
            boolean success = false;
            while (!success)
            {
                // If targets is null, then refresh all pending bundles.
                Bundle[] newTargets = targets;
                if (newTargets == null)
                {
                    List list = new ArrayList();

                    // First add all uninstalled bundles.
                    synchronized (m_uninstalledBundlesLock_Priority3)
                    {
                        for (int i = 0;
                            (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                            i++)
                        {
                            list.add(m_uninstalledBundles[i]);
                        }
                    }

                    // Then add all updated bundles.
                    synchronized (m_installedBundleLock_Priority2)
                    {
                        Iterator iter = m_installedBundleMap.values().iterator();
                        while (iter.hasNext())
                        {
                            FelixBundle bundle = (FelixBundle) iter.next();
                            if (bundle.getInfo().getArchive().getRevisionCount() > 1)
                            {
                                list.add(bundle);
                            }
                        }
                    }

                    // Create an array.
                    if (list.size() > 0)
                    {
                        newTargets = (Bundle[]) list.toArray(new Bundle[list.size()]);
                    }
                }

                // If there are targets, then find all dependencies
                // for each one.
                if (newTargets != null)
                {
                    // Create map of bundles that import the packages
                    // from the target bundles.
                    Map map = new HashMap();
                    for (int targetIdx = 0; targetIdx < newTargets.length; targetIdx++)
                    {
                        // Add the current target bundle to the map of
                        // bundles to be refreshed.
                        FelixBundle target = (FelixBundle) newTargets[targetIdx];
                        map.put(target, target);
                        // Add all importing bundles to map.
                        populateImportGraph(target, map);
                    }

                    bundles = (FelixBundle[]) map.values().toArray(new FelixBundle[map.size()]);
                }

                // Check if all corresponding bundles can be locked.
                boolean lockable = true;
                if (bundles != null)
                {
                    for (int i = 0; lockable && (i < bundles.length); i++)
                    {
                        lockable = bundles[i].getInfo().isLockable();
                    }

                    // If we can lock all bundles, then lock them.
                    if (lockable)
                    {
                        for (int i = 0; i < bundles.length; i++)
                        {
                            bundles[i].getInfo().lock();
                        }
                        success = true;
                    }
                    // Otherwise, wait and try again.
                    else
                    {
                        try
                        {
                            m_bundleLock.wait();
                        }
                        catch (InterruptedException ex)
                        {
                            // Ignore and just keep waiting.
                        }
                    }
                }
                else
                {
                    // If there were no bundles to lock, then we can just
                    // exit the lock loop.
                    success = true;
                }
            }
        }

        return bundles;
    }

    protected void releaseBundleLocks(FelixBundle[] bundles)
    {
        // Always unlock any locked bundles.
        synchronized (m_bundleLock)
        {
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                bundles[i].getInfo().unlock();
            }
            m_bundleLock.notifyAll();
        }
    }
}