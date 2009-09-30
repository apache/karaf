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

import org.osgi.framework.launch.Framework;
import java.io.*;
import java.net.*;
import java.security.*;
import java.util.*;

import org.apache.felix.framework.cache.*;
import org.apache.felix.framework.ext.SecurityProvider;
import org.apache.felix.framework.searchpolicy.*;
import org.apache.felix.framework.ServiceRegistry.ServiceRegistryCallbacks;
import org.apache.felix.framework.util.*;
import org.apache.felix.framework.util.manifestparser.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.*;
import org.osgi.framework.BundleReference;
import org.osgi.framework.hooks.service.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.startlevel.StartLevel;

public class Felix extends BundleImpl implements Framework
{
    // The secure action used to do privileged calls
    static final SecureAction m_secureAction = new SecureAction();

    // The extension manager to handle extension bundles
    ExtensionManager m_extensionManager;

    // Logging related member variables.
    private final Logger m_logger;
    // Immutable config properties.
    private final Map m_configMap;
    // Mutable configuration properties passed into constructor.
    private final Map m_configMutableMap;

    // MODULE FACTORY.
    private final FelixResolverState m_resolverState;
    private final FelixResolver m_felixResolver;

    // Lock object used to determine if an individual bundle
    // lock or the global lock can be acquired.
    private final Object[] m_bundleLock = new Object[0];
    // Keeps track of threads wanting to acquire the global lock.
    private final List m_globalLockWaitersList = new ArrayList();
    // The thread currently holding the global lock.
    private Thread m_globalLockThread = null;
    // How many times the global lock was acquired by the thread holding
    // the global lock; if this value is zero, then it means the global
    // lock is free.
    private int m_globalLockCount = 0;

    // Maps a bundle location to a bundle location;
    // used to reserve a location when installing a bundle.
    private final Map m_installRequestMap = new HashMap();
    // This lock must be acquired to modify m_installRequestMap;
    // to help avoid deadlock this lock as priority 1 and should
    // be acquired before locks with lower priority.
    private final Object[] m_installRequestLock_Priority1 = new Object[0];

    // Maps a bundle location to a bundle.
    private HashMap m_installedBundleMap;
    private SortedMap m_installedBundleIndex;
    // This lock must be acquired to modify m_installedBundleMap;
    // to help avoid deadlock this lock as priority 2 and should
    // be acquired before locks with lower priority.
    private final Object[] m_installedBundleLock_Priority2 = new Object[0];

    // An array of uninstalled bundles before a refresh occurs.
    private BundleImpl[] m_uninstalledBundles = null;
    // This lock must be acquired to modify m_uninstalledBundles;
    // to help avoid deadlock this lock as priority 3 and should
    // be acquired before locks with lower priority.
    private final Object[] m_uninstalledBundlesLock_Priority3 = new Object[0];

    // Framework's active start level.
    private volatile int m_activeStartLevel = FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;

    // Local bundle cache.
    private BundleCache m_cache = null;

    // System bundle activator list.
    List m_activatorList = null;

    // Next available bundle identifier.
    private long m_nextId = 1L;
    private final Object m_nextIdLock = new Object[0];

    // List of event listeners.
    private EventDispatcher m_dispatcher = null;

    // Service registry.
    private ServiceRegistry m_registry = null;

    // Reusable bundle URL stream handler.
    private final URLStreamHandler m_bundleStreamHandler;

    // Boot package delegation.
    private final String[] m_bootPkgs;
    private final boolean[] m_bootPkgWildcards;

    // Shutdown thread.
    private Thread m_shutdownThread = null;
    private volatile ThreadGate m_shutdownGate = null;

    /**
     * <p>
     * This constructor creates a framework instance with a specified <tt>Map</tt>
     * of configuration properties. Configuration properties are used internally
     * by the framework to alter its default behavior. The configuration properties
     * should have a <tt>String</tt> key and an <tt>Object</tt> value. The passed
     * in <tt>Map</tt> is copied by the framework and all keys are converted to
     * <tt>String</tt>s.
     * </p>
     * <p>
     * Configuration properties are generally the sole means to configure the
     * framework's default behavior; the framework does not typically refer to
     * any system properties for configuration information. If a <tt>Map</tt> is
     * supplied to this method for configuration properties, then the framework
     * will consult the <tt>Map</tt> instance for any and all configuration
     * properties. It is possible to specify a <tt>null</tt> for the configuration
     * property map, in which case the framework will use its default behavior
     * in all cases.
     * </p>
     * <p>
     * The following configuration properties can be specified (properties starting
     * with "<tt>felix</tt>" are specific to Felix, while those starting with
     * "<tt>org.osgi</tt>" are standard OSGi properties):
     * </p>
     * <ul>
     *   <li><tt>org.osgi.framework.storage</tt> - Sets the directory to use as
     *       the bundle cache; by default bundle cache directory is
     *       <tt>felix-cache</tt> in the current working directory. The value
     *       should be a valid directory name. The directory name can be either absolute
     *       or relative. Relative directory names are relative to the current working
     *       directory. The specified directory will be created if it does
     *       not exist.
     *   </li>
     *   <li><tt>org.osgi.framework.storage.clean</tt> - Determines whether the
     *       bundle cache is flushed. The value can either be "<tt>none</tt>"
     *       or "<tt>onFirstInit</tt>", where "<tt>none</tt>" does not flush
     *       the bundle cache and "<tt>onFirstInit</tt>" flushes the bundle
     *       cache when the framework instance is first initialized. The default
     *       value is "<tt>none</tt>".
     *   </li>
     *   <li><tt>felix.cache.rootdir</tt> - Sets the root directory to use to
     *       calculate the bundle cache directory for relative directory names. If
     *       <tt>org.osgi.framework.storage</tt> is set to a relative name, by
     *       default it is relative to the current working directory. If this
     *       property is set, then it will be calculated as being relative to
     *       the specified root directory.
     *   </li>
     *   <li><tt>felix.cache.bufsize</tt> - Sets the buffer size to be used by
     *       the cache; the default value is 4096. The integer value of this
     *       string provides control over the size of the internal buffer of the
     *       disk cache for performance reasons.
     *   </li>
     *   <li><tt>org.osgi.framework.system.packages</tt> - Specifies a
     *       comma-delimited list of packages that should be exported via the
     *       System Bundle from the parent class loader. The framework will set
     *       this to a reasonable default. If the value is specified, it
     *       replaces any default value.
     *   </li>
     *   <li><tt>org.osgi.framework.system.packages.extra</tt> - Specifies a
     *       comma-delimited list of packages that should be exported via the
     *       System Bundle from the parent class loader in addition to the
     *       packages in <tt>org.osgi.framework.system.packages</tt>. The default
     *       value is empty. If a value is specified, it is appended to the list
     *       of default or specified packages in
     *       <tt>org.osgi.framework.system.packages</tt>.
     *   </li>
     *   <li><tt>org.osgi.framework.bootdelegation</tt> - Specifies a
     *       comma-delimited list of packages that should be made implicitly
     *       available to all bundles from the parent class loader. It is
     *       recommended not to use this property since it breaks modularity.
     *       The default value is empty.
     *   </li>
     *   <li><tt>felix.systembundle.activators</tt> - A <tt>List</tt> of
     *       <tt>BundleActivator</tt> instances that are started/stopped when
     *       the System Bundle is started/stopped. The specified instances will
     *       receive the System Bundle's <tt>BundleContext</tt> when invoked.
     *   </li>
     *   <li><tt>felix.log.logger</tt> - An instance of <tt>Logger</tt> that the
     *       framework uses as its default logger.
     *   </li>
     *   <li><tt>felix.log.level</tt> - An integer value indicating the degree
     *       of logging reported by the framework; the higher the value the more
     *       logging is reported. If zero ('0') is specified, then logging is
     *       turned off completely. The log levels match those specified in the
     *       OSGi Log Service (i.e., 1 = error, 2 = warning, 3 = information,
     *       and 4 = debug). The default value is 1.
     *   </li>
     *   <li><tt>org.osgi.framework.startlevel.beginning</tt> - The initial
     *       start level of the framework once it starts execution; the default
     *       value is 1.
     *   </li>
     *   <li><tt>felix.startlevel.bundle</tt> - The default start level for
     *       newly installed bundles; the default value is 1.
     *   </li>
     *   <li><tt>felix.service.urlhandlers</tt> - Flag to indicate whether
     *       to activate the URL Handlers service for the framework instance;
     *       the default value is "<tt>true</tt>". Activating the URL Handlers
     *       service will result in the <tt>URL.setURLStreamHandlerFactory()</tt>
     *       and <tt>URLConnection.setContentHandlerFactory()</tt> being called.
     *   </li>
     *   <li><tt>felix.fragment.validation</tt> - Determines if installing
     *       unsupported fragment bundles throws an exception or logs a warning.
     *       Possible values are "<tt>exception</tt>" or "<tt>warning</tt>". The
     *       default value is "<tt>exception</tt>".
     * </ul>
     * <p>
     * The <a href="Main.html"><tt>Main</tt></a> class implements some
     * functionality for default property file handling, which makes it
     * possible to specify configuration properties and framework properties
     * in files that are automatically loaded when starting the framework. If you
     * plan to create your own framework instance, you may be
     * able to take advantage of the features it provides; refer to its
     * class documentation for more information.
     * </p>
     * <p>
     * The framework is not actually started until the <tt>start()</tt> method
     * is called.
     * </p>
     *
     * @param configMap A map for obtaining configuration properties,
     *        may be <tt>null</tt>.
    **/
    public Felix(Map configMap)
    {
        super();
        // Copy the configuration properties; convert keys to strings.
        m_configMutableMap = new StringMap(false);
        if (configMap != null)
        {
            for (Iterator i = configMap.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry) i.next();
                m_configMutableMap.put(entry.getKey().toString(), entry.getValue());
            }
        }
        m_configMap = createUnmodifiableMap(m_configMutableMap);

        // Create logger with appropriate log level. Even though the
        // logger needs the system bundle's context for tracking log
        // services, it is created now because it is needed before
        // the system bundle is activated. The system bundle's context
        // will be set in the init() method after the system bundle
        // is activated.
        if (m_configMutableMap.get(FelixConstants.LOG_LOGGER_PROP) != null)
        {
            m_logger = (Logger) m_configMutableMap.get(FelixConstants.LOG_LOGGER_PROP);
        }
        else
        {
            m_logger = new Logger();
        }
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

        // Create default bundle stream handler.
        m_bundleStreamHandler = new URLHandlersBundleStreamHandler(this);

        // Create a resolver and its state.
        m_resolverState = new FelixResolverState(m_logger);
        m_felixResolver = new FelixResolver(
            new Resolver(m_logger,
                (String) m_configMap.get(Constants.FRAMEWORK_EXECUTIONENVIRONMENT)),
            m_resolverState);

        // Create the extension manager, which we will use as the module
        // definition for creating the system bundle module.
        m_extensionManager = new ExtensionManager(m_logger, this);
        try
        {
            addModule(m_extensionManager.getModule());
        }
        catch (Exception ex)
        {
            // This should not throw an exception, but if so, lets convert it to
            // a runtime exception.
            throw new RuntimeException(ex.getMessage());
        }

        // Read the boot delegation property and parse it.
        String s = (m_configMap == null)
            ? null
            : (String) m_configMap.get(Constants.FRAMEWORK_BOOTDELEGATION);
        s = (s == null) ? "java.*" : s + ",java.*";
        StringTokenizer st = new StringTokenizer(s, " ,");
        m_bootPkgs = new String[st.countTokens()];
        m_bootPkgWildcards = new boolean[m_bootPkgs.length];
        for (int i = 0; i < m_bootPkgs.length; i++)
        {
            s = st.nextToken();
            if (s.equals("*") || s.endsWith(".*"))
            {
                m_bootPkgWildcards[i] = true;
                s = s.substring(0, s.length() - 1);
            }
            m_bootPkgs[i] = s;
        }
    }

    Logger getLogger()
    {
        return m_logger;
    }

    Map getConfig()
    {
        return m_configMap;
    }

    FelixResolver getResolver()
    {
        return m_felixResolver;
    }

    FelixResolverState getResolverState()
    {
        return m_resolverState;
    }

    URLStreamHandler getBundleStreamHandler()
    {
        return m_bundleStreamHandler;
    }

    String[] getBootPackages()
    {
        return m_bootPkgs;
    }

    boolean[] getBootPackageWildcards()
    {
        return m_bootPkgWildcards;
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

    // This overrides the default behavior of BundleImpl.getFramework()
    // to return "this", since the system bundle is the framework.
    Felix getFramework()
    {
        return this;
    }

    public long getBundleId()
    {
        return 0;
    }

    public long getLastModified()
    {
        return 0;
    }

    void setLastModified(long l)
    {
        // Ignore.
    }

    String _getLocation()
    {
        return Constants.SYSTEM_BUNDLE_LOCATION;
    }

    public int getPersistentState()
    {
        return Bundle.ACTIVE;
    }

    public void setPersistentStateInactive()
    {
        // Ignore.
    }

    public void setPersistentStateActive()
    {
        // Ignore.
    }

    public void setPersistentStateUninstalled()
    {
        // Ignore.
    }

    /**
     * Overrides standard <tt>BundleImpl.getStartLevel()</tt> behavior to
     * always return zero for the system bundle.
     * @param defaultLevel This parameter is ignored by the system bundle.
     * @return Always returns zero.
    **/
    int getStartLevel(int defaultLevel)
    {
        return 0;
    }

    /**
     * Overrides standard <tt>BundleImpl.setStartLevel()</tt> behavior to
     * always throw an exception since the system bundle's start level cannot
     * be changed.
     * @param level This parameter is ignored by the system bundle.
     * @throws IllegalArgumentException Always throws exception since system
     *         bundle's start level cannot be changed.
    **/
    void setStartLevel(int level)
    {
        throw new IllegalArgumentException("Cannot set the system bundle's start level.");
    }

    public boolean hasPermission(Object obj)
    {
        return true;
    }

    /**
     * This method initializes the framework, which is comprised of resolving
     * the system bundle, reloading any cached bundles, and activating the system
     * bundle. The framework is left in the <tt>Bundle.STARTING</tt> state and
     * reloaded bundles are in the <tt>Bundle.INSTALLED</tt> state. After
     * successfully invoking this method, <tt>getBundleContext()</tt> will
     * return a valid <tt>BundleContext</tt> for the system bundle. To finish
     * starting the framework, invoke the <tt>start()</tt> method.
     *
     * @throws org.osgi.framework.BundleException if any error occurs.
    **/
    public void init() throws BundleException
    {
        // The system bundle can only be initialized if it currently isn't started.
        acquireBundleLock(this,
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        try
        {
            if ((getState() == Bundle.INSTALLED) || (getState() == Bundle.RESOLVED))
            {
                // Get any system bundle activators.
                m_activatorList = (List) m_configMutableMap.get(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP);
                m_activatorList = (m_activatorList == null) ? new ArrayList() : new ArrayList(m_activatorList);

                // Initialize event dispatcher.
                m_dispatcher = EventDispatcher.start(m_logger);

                // Create the bundle cache, if necessary, so that we can reload any
                // installed bundles.
                m_cache = (BundleCache) m_configMutableMap.get(
                    FelixConstants.FRAMEWORK_BUNDLECACHE_IMPL);
                if (m_cache == null)
                {
                       try
                       {
                           m_cache = new BundleCache(m_logger, m_configMap);
                       }
                       catch (Exception ex)
                       {
                           m_logger.log(Logger.LOG_ERROR, "Error creating bundle cache.", ex);
                           throw new BundleException("Error creating bundle cache.", ex);
                       }
                }

                // If this is the first time init is called, check to see if
                // we need to flush the bundle cache.
                if (getState() == Bundle.INSTALLED)
                {
                    String clean = (String) m_configMap.get(Constants.FRAMEWORK_STORAGE_CLEAN);
                    if ((clean != null)
                        && clean.equalsIgnoreCase(Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT))
                    {
                        try
                        {
                            m_cache.delete();
                        }
                        catch (Exception ex)
                        {
                            throw new BundleException("Unable to flush bundle cache.", ex);
                        }
                    }
                }

                // Initialize installed bundle data structures.
                m_installedBundleMap = new HashMap();
                m_installedBundleIndex = new TreeMap();

                // Add the system bundle to the set of installed bundles.
                m_installedBundleMap.put(_getLocation(), this);
                m_installedBundleIndex.put(new Long(0), this);

                // Manually resolve the system bundle, which will cause its
                // state to be set to RESOLVED.
                try
                {
                    m_felixResolver.resolve(getCurrentModule());
                }
                catch (ResolveException ex)
                {
                    // This should never happen.
                    throw new BundleException(
                        "Unresolved constraint in System Bundle:"
                        + ex.getRequirement());
                }

                // Reload the cached bundles before creating and starting the
                // system bundle, since we want all cached bundles to be reloaded
                // when we activate the system bundle and any subsequent system
                // bundle activators passed into the framework constructor.
                BundleArchive[] archives = null;

                // First get cached bundle identifiers.
                try
                {
                    archives = m_cache.getArchives();
                }
                catch (Exception ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Unable to list saved bundles.", ex);
                    archives = null;
                }

                // Now load all cached bundles.
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
                            archives[i].closeAndDelete();
                        }
                        // Otherwise re-install the cached bundle.
                        else
                        {
                            // Install the cached bundle.
                            installBundle(
                                archives[i].getId(), archives[i].getLocation(), archives[i], null);
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

                // Create service registry.
                m_registry = new ServiceRegistry(m_logger, new ServiceRegistryCallbacks() {
                    public void serviceChanged(ServiceEvent event, Dictionary oldProps)
                    {
                        fireServiceEvent(event, oldProps);
                    }
                });
                m_dispatcher.setServiceRegistry(m_registry);

                // The framework is now in its startup sequence.
                setBundleStateAndNotify(this, Bundle.STARTING);

                // Now it is possible for threads to wait for the framework to stop,
                // so create a gate for that purpose.
                m_shutdownGate = new ThreadGate();

                // Create system bundle activator and bundle context so we can activate it.
                setActivator(new SystemBundleActivator());
                setBundleContext(new BundleContextImpl(m_logger, this, this));
                try
                {
                    Felix.m_secureAction.startActivator(
                        getActivator(), getBundleContext());
                }
                catch (Throwable ex)
                {
                    EventDispatcher.shutdown();
                    m_logger.log(Logger.LOG_ERROR, "Unable to start system bundle.", ex);
                    throw new RuntimeException("Unable to start system bundle.");
                }

                // Now that the system bundle is successfully created we can give
                // its bundle context to the logger so that it can track log services.
                m_logger.setSystemBundleContext(getBundleContext());
            }
        }
        finally
        {
            releaseBundleLock(this);
        }
    }

    /**
     * This method starts the framework instance, which will transition the
     * framework from start level 0 to its active start level as specified in
     * its configuration properties (1 by default). If the <tt>init()</tt> was
     * not explicitly invoked before calling this method, then it will be
     * implicitly invoked before starting the framework.
     *
     * @throws org.osgi.framework.BundleException if any error occurs.
    **/
    public void start() throws BundleException
    {
        int startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;

        acquireBundleLock(this,
            Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        try
        {
            // Initialize if necessary.
            if ((getState() == Bundle.INSTALLED) || (getState() == Bundle.RESOLVED))
            {
                init();
            }

            // If the current state is STARTING, then the system bundle can be started.
            if (getState() == Bundle.STARTING)
            {
                // Get the framework's default start level.
                String s = (String) m_configMap.get(Constants.FRAMEWORK_BEGINNING_STARTLEVEL);
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

                // Set the start level using the start level service;
                // this ensures that all start level requests are
                // serialized.
                StartLevel sl = null;
                try
                {
                    sl = (StartLevel) getService(
                        getBundle(0), getServiceReferences((BundleImpl) getBundle(0),
                        StartLevel.class.getName(), null, true)[0]);
                }
                catch (InvalidSyntaxException ex)
                {
                    // Should never happen.
                }

                if (sl instanceof StartLevelImpl)
                {
                    ((StartLevelImpl) sl).setStartLevelAndWait(startLevel);
                }
                else
                {
                    sl.setStartLevel(startLevel);
                }

                // The framework is now running.
                setBundleStateAndNotify(this, Bundle.ACTIVE);
            }
        }
        finally
        {
            releaseBundleLock(this);
        }

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, this);

        // Send a framework event to indicate the framework has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, this, null);
    }

    public void start(int options) throws BundleException
    {
        // TODO: FRAMEWORK - For now, ignore all options when starting the
        //       system bundle.
        start();
    }

    /**
     * This method asynchronously shuts down the framework, it must be called at the
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

    public void stop(int options) throws BundleException
    {
        // TODO: FRAMEWORK - For now, ignore all options when stopping the
        //       system bundle.
        stop();
    }

    /**
     * This method will cause the calling thread to block until the framework
     * shuts down.
     * @param timeout A timeout value.
     * @throws java.lang.InterruptedException If the thread was interrupted.
    **/
    public FrameworkEvent waitForStop(long timeout) throws InterruptedException
    {
        // Throw exception if timeout is negative.
        if (timeout < 0)
        {
            throw new IllegalArgumentException("Timeout cannot be negative.");
        }

        // If there is a gate, wait on it; otherwise, return immediately.
        // Grab a copy of the gate, since it is volatile.
        ThreadGate gate = m_shutdownGate;
        boolean open = false;
        if (gate != null)
        {
            open = gate.await(timeout);
        }

        FrameworkEvent event;
        if (open && (gate.getMessage() != null))
        {
            event = (FrameworkEvent) gate.getMessage();
        }
        else if (!open && (gate != null))
        {
            event = new FrameworkEvent(FrameworkEvent.WAIT_TIMEDOUT, this, null);
        }
        else
        {
            event = new FrameworkEvent(FrameworkEvent.STOPPED, this, null);
        }
        return event;
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

        // Spec says to close input stream first.
        try
        {
            if (is != null) is.close();
        }
        catch (IOException ex)
        {
            m_logger.log(Logger.LOG_WARNING, "Exception closing input stream.", ex);
        }

        // Then to stop and restart the framework on a separate thread.
        new Thread(new Runnable() {
            public void run()
            {
                try
                {
                    // First acquire the system bundle lock to verify the state.
                    acquireBundleLock(Felix.this, Bundle.STARTING | Bundle.ACTIVE);
                    // Set the reason for the shutdown.
                    m_shutdownGate.setMessage(
                        new FrameworkEvent(FrameworkEvent.STOPPED_UPDATE, Felix.this, null));
                    // Record the state and stop the system bundle.
                    int oldState = Felix.this.getState();
                    try
                    {
                        stop();
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Exception stopping framework.", ex);
                    }
                    finally
                    {
                        releaseBundleLock(Felix.this);
                    }

                    // Make sure the framework is stopped.
                    try
                    {
                        waitForStop(0);
                    }
                    catch (InterruptedException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Did not wait for framework to stop.", ex);
                    }

                    // Depending on the old state, restart the framework.
                    try
                    {
                        switch (oldState)
                        {
                            case Bundle.STARTING:
                                init();
                                break;
                            case Bundle.ACTIVE:
                                start();
                                break;
                        }
                    }
                    catch (BundleException ex)
                    {
                        m_logger.log(Logger.LOG_WARNING, "Exception restarting framework.", ex);
                    }
                }
                catch (Exception ex)
                {
                    m_logger.log(Logger.LOG_WARNING, "Cannot update an inactive framework.");
                }
            }
        }).start();
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
    int getActiveStartLevel()
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
    void setActiveStartLevel(int requestedLevel)
    {
        Bundle[] bundles = null;

        // Do nothing if the requested start level is the same as the
        // active start level.
        if (requestedLevel != getActiveStartLevel())
        {
            // Synchronization for changing the start level is rather loose.
            // The framework's active start level is volatile, so no lock is
            // needed to access it. The install lock is acquired to attain a
            // sorted snapshot of the currently installed bundles, but then this
            // lock is freed immediately. No locks are held while processing the
            // currently installed bundles for starting/stopping based on the new
            // active start level. The only locking that occurs is for individual
            // bundles when startBundle()/stopBundle() is called, but this locking
            // is done in the respective method.
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

            // Determine if we are lowering or raising the
            // active start level, then udpate active start level.
            boolean lowering = (requestedLevel < getActiveStartLevel());
            m_activeStartLevel = requestedLevel;

            synchronized (m_installedBundleLock_Priority2)
            {
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
                            BundleImpl b1 = (BundleImpl) o1;
                            BundleImpl b2 = (BundleImpl) o2;
                            if (b1.getStartLevel(getInitialBundleStartLevel())
                                < b2.getStartLevel(getInitialBundleStartLevel()))
                            {
                                return 1;
                            }
                            else if (b1.getStartLevel(getInitialBundleStartLevel())
                                > b2.getStartLevel(getInitialBundleStartLevel()))
                            {
                                return -1;
                            }
                            else if (b1.getBundleId() < b2.getBundleId())
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
                            BundleImpl b1 = (BundleImpl) o1;
                            BundleImpl b2 = (BundleImpl) o2;
                            if (b1.getStartLevel(getInitialBundleStartLevel())
                                > b2.getStartLevel(getInitialBundleStartLevel()))
                            {
                                return 1;
                            }
                            else if (b1.getStartLevel(getInitialBundleStartLevel())
                                < b2.getStartLevel(getInitialBundleStartLevel()))
                            {
                                return -1;
                            }
                            else if (b1.getBundleId() > b2.getBundleId())
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
                BundleImpl impl = (BundleImpl) bundles[i];

                // Ignore the system bundle, since its start() and
                // stop() methods get called explicitly in Felix.start()
                // and Felix.stop(), respectively.
                if (impl.getBundleId() == 0)
                {
                    continue;
                }

                // Lock the current bundle.
                try
                {
                    acquireBundleLock(impl,
                        Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE
                        | Bundle.STARTING | Bundle.STOPPING);
                }
                catch (IllegalStateException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, impl, ex);
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Error locking " + impl._getLocation(), ex);
                    continue;
                }

                try
                {
                    // Start the bundle if necessary.
                    if (((impl.getPersistentState() == Bundle.ACTIVE)
                        || (impl.getPersistentState() == Bundle.STARTING))
                        && (impl.getStartLevel(getInitialBundleStartLevel())
                            <= getActiveStartLevel()))
                    {
                        try
                        {
// TODO: LAZY - Not sure if this is the best way...
                            int options = Bundle.START_TRANSIENT;
                            options = (impl.getPersistentState() == Bundle.STARTING)
                                ? options | Bundle.START_ACTIVATION_POLICY
                                : options;
                            startBundle(impl, options);
                        }
                        catch (Throwable th)
                        {
                            fireFrameworkEvent(FrameworkEvent.ERROR, impl, th);
                            m_logger.log(
                                Logger.LOG_ERROR,
                                "Error starting " + impl._getLocation(), th);
                        }
                    }
                    // Stop the bundle if necessary.
                    else if (((impl.getState() == Bundle.ACTIVE)
                        || (impl.getState() == Bundle.STARTING))
                        && (impl.getStartLevel(getInitialBundleStartLevel())
                            > getActiveStartLevel()))
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
                                "Error stopping " + impl._getLocation(), th);
                        }
                    }
                }
                finally
                {
                    // Always release bundle lock.
                    releaseBundleLock(impl);
                }
                // Hint to GC to collect bundle; not sure why this
                // is necessary, but it appears to help.
                bundles[i] = null;
            }
        }

        if (getState() == Bundle.ACTIVE)
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
    int getInitialBundleStartLevel()
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
    void setInitialBundleStartLevel(int startLevel)
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
    int getBundleStartLevel(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((BundleImpl) bundle).getStartLevel(getInitialBundleStartLevel());
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
    void setBundleStartLevel(Bundle bundle, int startLevel)
    {
        // Acquire bundle lock.
        BundleImpl impl = (BundleImpl) bundle;
        try
        {
            acquireBundleLock(impl,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE
                | Bundle.STARTING | Bundle.STOPPING);
        }
        catch (IllegalStateException ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, impl, ex);
            m_logger.log(
                Logger.LOG_ERROR,
                "Error locking " + impl._getLocation(), ex);
            return;
        }

        Throwable rethrow = null;

        try
        {
            if (startLevel >= 1)
            {
                // NOTE: The start level was persistently recorded inside
                // the start level impl because the spec requires it to be
                // done synchronously.

                try
                {
                    // Start the bundle if necessary.
                    if (((impl.getPersistentState() == Bundle.ACTIVE)
                        || (impl.getPersistentState() == Bundle.STARTING))
                        && (startLevel <= getActiveStartLevel()))
                    {
// TODO: LAZY - Not sure if this is the best way...
                        int options = Bundle.START_TRANSIENT;
                        options = (impl.getPersistentState() == Bundle.STARTING)
                            ? options | Bundle.START_ACTIVATION_POLICY
                            : options;
                        startBundle(impl, options);
                    }
                    // Stop the bundle if necessary.
                    else if (((impl.getState() == Bundle.ACTIVE)
                        || (impl.getState() == Bundle.STARTING))
                        && (startLevel > getActiveStartLevel()))
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
            releaseBundleLock(impl);
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
    boolean isBundlePersistentlyStarted(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return (((BundleImpl) bundle).getPersistentState() == Bundle.ACTIVE)
            || (((BundleImpl) bundle).getPersistentState() == Bundle.STARTING);
    }

    /**
     * Returns whether the bundle is using its declared activation policy;
     * this is an method implementation for the Start Level service.
     * @param bundle The bundle to examine.
     * @return <tt>true</tt> if the bundle is using its declared activation
     *         policy, <tt>false</tt> otherwise.
     * @throws java.lang.IllegalArgumentException If the specified
     *          bundle has been uninstalled.
    **/
    boolean isBundleActivationPolicyUsed(Bundle bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalArgumentException("Bundle is uninstalled.");
        }

        return ((BundleImpl) bundle).isDeclaredActivationPolicyUsed();
    }

    //
    // Implementation of Bundle interface methods.
    //

    /**
     * Get bundle headers and resolve any localized strings from resource bundles.
     * @param bundle
     * @param locale
     * @return localized bundle headers dictionary.
    **/
    Dictionary getBundleHeaders(BundleImpl bundle, String locale)
    {
        return new MapToDictionary(bundle.getCurrentLocalizedHeader(locale));
    }

    /**
     * Implementation for Bundle.getResource().
    **/
    URL getBundleResource(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (Util.isFragment(bundle.getCurrentModule()))
        {
            return null;
        }
        return bundle.getCurrentModule().getResourceByDelegation(name);
    }

    /**
     * Implementation for Bundle.getResources().
    **/
    Enumeration getBundleResources(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (Util.isFragment(bundle.getCurrentModule()))
        {
            return null;
        }
        return bundle.getCurrentModule().getResourcesByDelegation(name);
    }

    /**
     * Implementation for Bundle.getEntry().
    **/
    URL getBundleEntry(BundleImpl bundle, String name)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        return bundle.getCurrentModule().getEntry(name);
    }

    /**
     * Implementation for Bundle.getEntryPaths().
    **/
    Enumeration getBundleEntryPaths(BundleImpl bundle, String path)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
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
    Enumeration findBundleEntries(
        BundleImpl bundle, String path, String filePattern, boolean recurse)
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

    ServiceReference[] getBundleRegisteredServices(BundleImpl bundle)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Filter list of registered service references.
        ServiceReference[] refs = m_registry.getRegisteredServices(bundle);

        return refs;
    }

    ServiceReference[] getBundleServicesInUse(Bundle bundle)
    {
        // Filter list of "in use" service references.
        ServiceReference[] refs = m_registry.getServicesInUse(bundle);

        return refs;
    }

    boolean bundleHasPermission(BundleImpl bundle, Object obj)
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        if (System.getSecurityManager() != null)
        {
            try
            {
                return (obj instanceof java.security.Permission)
                    ? impliesBundlePermission(
                    (BundleProtectionDomain)
                    bundle.getProtectionDomain(),
                    (java.security.Permission) obj, true)
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
    Class loadBundleClass(BundleImpl bundle, String name) throws ClassNotFoundException
    {
        if (bundle.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("Bundle is uninstalled");
        }
        else if (Util.isFragment(bundle.getCurrentModule()))
        {
            throw new ClassNotFoundException("Fragments cannot load classes.");
        }
        else if (bundle.getState() == Bundle.INSTALLED)
        {
            try
            {
                resolveBundle(bundle);
            }
            catch (BundleException ex)
            {
                // The spec says we must fire a framework error.
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                // Then throw a class not found exception.
                throw new ClassNotFoundException(name, ex);
            }
        }
        return bundle.getCurrentModule().getClassByDelegation(name);
    }

    /**
     * Implementation for Bundle.start().
    **/
    void startBundle(BundleImpl bundle, int options) throws BundleException
    {
        // CONCURRENCY NOTE:
        // We will first acquire the bundle lock for the specific bundle
        // as long as the bundle is INSTALLED, RESOLVED, or ACTIVE. If this
        // bundle is not yet resolved, then it will be resolved too. In
        // that case, the global lock will be acquired to make sure no
        // bundles can be installed or uninstalled during the resolve.

        int eventType;

        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot start an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be started, since it is either starting or stopping.");
            }
        }

        // Record whether the bundle is using its declared activation policy.
        boolean wasDeferred = bundle.isDeclaredActivationPolicyUsed()
            && (bundle.getCurrentModule().getDeclaredActivationPolicy() == IModule.LAZY_ACTIVATION);
        bundle.setDeclaredActivationPolicyUsed(
            (options & Bundle.START_ACTIVATION_POLICY) != 0);

        BundleException rethrow = null;
        try
        {
            // The spec doesn't say whether it is possible to start an extension
            // We just do nothing
            if (bundle.isExtension())
            {
                return;
            }

            // As per the OSGi spec, fragment bundles can not be started and must
            // throw a BundleException when there is an attempt to start one.
            if (Util.isFragment(bundle.getCurrentModule()))
            {
                throw new BundleException("Fragment bundles can not be started.");
            }

            // Set and save the bundle's persistent state to active
            // if we are supposed to record state change.
            if ((options & Bundle.START_TRANSIENT) == 0)
            {
                if ((options & Bundle.START_ACTIVATION_POLICY) != 0)
                {
                    bundle.setPersistentStateStarting();
                }
                else
                {
                    bundle.setPersistentStateActive();
                }
            }

            // Check to see if the bundle's start level is greater than the
            // the framework's active start level.
            if (bundle.getStartLevel(getInitialBundleStartLevel()) > getActiveStartLevel())
            {
                // Throw an exception for transient starts.
                if ((options & Bundle.START_TRANSIENT) != 0)
                {
                    throw new BundleException(
                        "Cannot start bundle " + bundle + " because its start level is "
                        + bundle.getStartLevel(getInitialBundleStartLevel())
                        + ", which is greater than the framework's start level of "
                        + getActiveStartLevel() + ".");
                }
                // Ignore persistent starts.
                return;
            }

            switch (bundle.getState())
            {
                case Bundle.UNINSTALLED:
                    throw new IllegalStateException("Cannot start an uninstalled bundle.");
                case Bundle.STARTING:
                    if (!wasDeferred)
                    {
                        throw new BundleException(
                            "Bundle " + bundle
                            + " cannot be started, since it is starting.");
                    }
                    break;
                case Bundle.STOPPING:
                    throw new BundleException(
                        "Bundle " + bundle
                        + " cannot be started, since it is stopping.");
                case Bundle.ACTIVE:
                    return;
                case Bundle.INSTALLED:
                    resolveBundle(bundle);
                    // No break.
                case Bundle.RESOLVED:
                    // Set the bundle's context.
                    bundle.setBundleContext(new BundleContextImpl(m_logger, this, bundle));
                    // At this point, no matter if the bundle's activation policy is
                    // eager or deferred, we need to set the bundle's state to STARTING.
                    // We don't fire a BundleEvent here for this state change, since
                    // STARTING events are only fired if we are invoking the activator,
                    // which we may not do if activation is deferred.
                    setBundleStateAndNotify(bundle, Bundle.STARTING);
                    break;
            }

            // If the bundle's activation policy is eager or activation has already
            // been triggered, then activate the bundle immediately.
            if (!bundle.isDeclaredActivationPolicyUsed()
                || (bundle.getCurrentModule().getDeclaredActivationPolicy() != IModule.LAZY_ACTIVATION)
                || ((ModuleImpl) bundle.getCurrentModule()).isActivationTriggered())
            {
                // Record the event type for the final event and activate.
                eventType = BundleEvent.STARTED;
                // Note that the STARTING event is thrown in the activateBundle() method.
                try
                {
                    activateBundle(bundle, false);
                }
                catch (BundleException ex)
                {
                    rethrow = ex;
                }
            }
            // Otherwise, defer bundle activation.
            else
            {
                // Record the event type for the final event.
                eventType = BundleEvent.LAZY_ACTIVATION;
            }

            // We still need to fire the STARTED event, but we will do
            // it later so we can release the bundle lock.
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STARTED
        // or LAZY_ACTIVATION event here without holding the lock. Otherwise,
        // fire STOPPED and rethrow exception.
        if (rethrow == null)
        {
            fireBundleEvent(eventType, bundle);
        }
        else
        {
            fireBundleEvent(BundleEvent.STOPPED, bundle);
            throw rethrow;
        }
    }

    void activateBundle(BundleImpl bundle, boolean fireEvent) throws BundleException
    {
        // CONCURRENCY NOTE:
        // We will first acquire the bundle lock for the specific bundle
        // as long as the bundle is INSTALLED, RESOLVED, or ACTIVE. If this
        // bundle is not yet resolved, then it will be resolved too. In
        // that case, the global lock will be acquired to make sure no
        // bundles can be installed or uninstalled during the resolve.

        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.STARTING | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            throw new IllegalStateException(
                "Activation only occurs for bundles in STARTING state.");
        }

        try
        {
            // If the bundle is already active or its start level is not met,
            // simply return.
            if ((bundle.getState() == Bundle.ACTIVE) ||
                (bundle.getStartLevel(getInitialBundleStartLevel()) > getActiveStartLevel()))
            {
                return;
            }

            // Fire STARTING event to signify call to bundle activator.
            fireBundleEvent(BundleEvent.STARTING, bundle);

            try
            {
                // Set the bundle's activator.
                bundle.setActivator(createBundleActivator(bundle));

                // Activate the bundle if it has an activator.
                if (bundle.getActivator() != null)
                {
                    m_secureAction.startActivator(
                        bundle.getActivator(), bundle.getBundleContext());
                }

                setBundleStateAndNotify(bundle, Bundle.ACTIVE);

                // We still need to fire the STARTED event, but we will do
                // it later so we can release the bundle lock.
            }
            catch (Throwable th)
            {
                // Spec says we must fire STOPPING event.
                fireBundleEvent(BundleEvent.STOPPING, bundle);

                // If there was an error starting the bundle,
                // then reset its state to RESOLVED.
                setBundleStateAndNotify(bundle, Bundle.RESOLVED);

                // Clean up the bundle activator
                bundle.setActivator(null);

                // Unregister any services offered by this bundle.
                m_registry.unregisterServices(bundle);

                // Release any services being used by this bundle.
                m_registry.ungetServices(bundle);

                // Remove any listeners registered by this bundle.
                m_dispatcher.removeListeners(bundle);

                // Clean up the bundle context.
                ((BundleContextImpl) bundle.getBundleContext()).invalidate();
                bundle.setBundleContext(null);

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
                throw new BundleException("Activator start error in bundle " + bundle + ".", th);
            }
        }
        finally
        {
            // Release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STARTED
        // event here without holding the lock if specified.
        // TODO: LAZY - It would be nice to figure out how to do this without
        //       duplicating code; this method is called from two different
        //       places -- one fires the event itself the other one needs it.
        if (fireEvent)
        {
            fireBundleEvent(BundleEvent.STARTED, bundle);
        }
    }

    void updateBundle(BundleImpl bundle, InputStream is)
        throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot update an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be update, since it is either starting or stopping.");
            }
        }

        // We must release the lock and close the input stream, so do both
        // in a finally block.
        try
        {
            // Variable to indicate whether bundle is active or not.
            Throwable rethrow = null;

            final int oldState = bundle.getState();

            // First get the update-URL from our header.
            String updateLocation = (String)
                bundle.getCurrentModule().getHeaders().get(
                    Constants.BUNDLE_UPDATELOCATION);

            // If no update location specified, use original location.
            if (updateLocation == null)
            {
                updateLocation = bundle._getLocation();
            }

            // Stop the bundle if it is active, but do not change its
            // persistent state.
            if (oldState == Bundle.ACTIVE)
            {
                stopBundle(bundle, false);
            }

            try
            {
                // Revising the bundle creates a new module, which modifies
                // the global state, so we need to acquire the global lock
                // before revising.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    throw new BundleException(
                        "Cannot acquire global lock to update the bundle.");
                }
                boolean wasExtension = bundle.isExtension();
                try
                {
// REFACTOR - This adds the module to the resolver state, but should we do the
//            security check first?
                    bundle.revise(updateLocation, is);
                }
                finally
                {
                    // Always release the global lock.
                    releaseGlobalLock();
                }

                // Verify updated bundle.
                try
                {
                    Object sm = System.getSecurityManager();

                    if (sm != null)
                    {
                        ((SecurityManager) sm).checkPermission(
                            new AdminPermission(bundle, AdminPermission.LIFECYCLE));
                    }

                    // If this is an update from a normal to an extension bundle
                    // then attach the extension
                    if (!wasExtension && bundle.isExtension())
                    {
                        m_extensionManager.addExtensionBundle(this, bundle);
// TODO: REFACTOR - Perhaps we could move this into extension manager.
                        m_resolverState.refreshSystemBundleModule(m_extensionManager.getModule());
// TODO: REFACTOR - Not clear why this is here. We should look at all of these steps more closely.
                        setBundleStateAndNotify(bundle, Bundle.RESOLVED);
                    }
                    else if (wasExtension)
                    {
                        setBundleStateAndNotify(bundle, Bundle.INSTALLED);
                    }
                }
                catch (Throwable ex)
                {
                    try
                    {
                        bundle.rollbackRevise();
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
                bundle.setLastModified(System.currentTimeMillis());

                if (!bundle.isExtension())
                {
                    setBundleStateAndNotify(bundle, Bundle.INSTALLED);
                }
                else
                {
                    m_extensionManager.startExtensionBundle(this, bundle);
                }

                fireBundleEvent(BundleEvent.UNRESOLVED, bundle);

                fireBundleEvent(BundleEvent.UPDATED, bundle);

                // Acquire global lock to check if we should auto-refresh.
                boolean locked = acquireGlobalLock();
                // If we did not get the global lock, then do not try to
                // auto-refresh.
                if (locked)
                {
                    try
                    {
                        if (!bundle.isUsed() && !bundle.isExtension())
                        {
                            try
                            {
                                refreshPackages(new BundleImpl[] { bundle });
                            }
                            catch (Exception ex)
                            {
                                m_logger.log(
                                    Logger.LOG_ERROR,
                                    "Unable to immediately purge the bundle revisions.", ex);
                            }
                        }
                    }
                    finally
                    {
                        // Always release the global lock.
                        releaseGlobalLock();
                    }
                }
            }

            // If the old state was active, but the new module is a fragment,
            // then mark the persistent state to inactive.
            if ((oldState == Bundle.ACTIVE) && Util.isFragment(bundle.getCurrentModule()))
            {
                bundle.setPersistentStateInactive();
                m_logger.log(Logger.LOG_WARNING,
                    "Previously active bundle was updated to a fragment, resetting state to inactive: "
                    + bundle);
            }
            // Otherwise, restart the bundle if it was previously active,
            // but do not change its persistent state.
            else if (oldState == Bundle.ACTIVE)
            {
                startBundle(bundle, Bundle.START_TRANSIENT);
            }

            // If update failed, rethrow exception.
            if (rethrow != null)
            {
                if ((System.getSecurityManager() != null) &&
                    (rethrow instanceof SecurityException))
                {
                    throw (SecurityException) rethrow;
                }

                throw new BundleException("Update of bundle " + bundle + " failed.", rethrow);
            }
        }
        finally
        {
            // Close the input stream.
            try
            {
                if (is != null) is.close();
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to close input stream.", ex);
            }

            // Release bundle lock.
            releaseBundleLock(bundle);
        }
    }

    void stopBundle(BundleImpl bundle, boolean record)
        throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot stop an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be stopped since it is already stopping.");
            }
        }

        try
        {
            Throwable rethrow = null;

            // Set the bundle's persistent state to inactive if necessary.
            if (record)
            {
                bundle.setPersistentStateInactive();
            }

            // If the bundle is not persistently started, then we
            // need to reset the activation policy flag, since it
            // does not persist across persistent stops or transient
            // stops.
            if (!isBundlePersistentlyStarted(bundle))
            {
                bundle.setDeclaredActivationPolicyUsed(false);
            }

            // As per the OSGi spec, fragment bundles can not be stopped and must
            // throw a BundleException when there is an attempt to stop one.
            if (Util.isFragment(bundle.getCurrentModule()))
            {
                throw new BundleException("Fragment bundles can not be stopped: " + bundle);
            }

            boolean wasActive = false;
            switch (bundle.getState())
            {
                case Bundle.UNINSTALLED:
                    throw new IllegalStateException("Cannot stop an uninstalled bundle.");
                case Bundle.STARTING:
                    if (bundle.isDeclaredActivationPolicyUsed()
                        && bundle.getCurrentModule().getDeclaredActivationPolicy() != IModule.LAZY_ACTIVATION)
                    {
                        throw new BundleException(
                            "Stopping a starting or stopping bundle is currently not supported.");
                    }
                    break;
                case Bundle.STOPPING:
                    throw new BundleException(
                        "Stopping a starting or stopping bundle is currently not supported.");
                case Bundle.INSTALLED:
                case Bundle.RESOLVED:
                    return;
                case Bundle.ACTIVE:
                    wasActive = true;
                    break;
            }

            // At this point, no matter if the bundle's activation policy is
            // eager or deferred, we need to set the bundle's state to STOPPING
            // and fire the STOPPING event.
            setBundleStateAndNotify(bundle, Bundle.STOPPING);
            fireBundleEvent(BundleEvent.STOPPING, bundle);

            // If the bundle was active, then invoke the activator stop() method
            // or if we are stopping the system bundle.
            if ((wasActive) || (bundle.getBundleId() == 0))
            {
                try
                {
                    if (bundle.getActivator() != null)
                    {
                        m_secureAction.stopActivator(bundle.getActivator(), bundle.getBundleContext());
                    }
                }
                catch (Throwable th)
                {
                    m_logger.log(Logger.LOG_ERROR, "Error stopping bundle.", th);
                    rethrow = th;
                }
            }

            // Do not clean up after the system bundle since it will
            // clean up after itself.
            if (bundle.getBundleId() != 0)
            {
                // Clean up the bundle activator.
                bundle.setActivator(null);

                // Unregister any services offered by this bundle.
                m_registry.unregisterServices(bundle);

                // Release any services being used by this bundle.
                m_registry.ungetServices(bundle);

                // The spec says that we must remove all event
                // listeners for a bundle when it is stopped.
                m_dispatcher.removeListeners(bundle);

                // Clean up the bundle context.
                ((BundleContextImpl) bundle.getBundleContext()).invalidate();
                bundle.setBundleContext(null);

                setBundleStateAndNotify(bundle, Bundle.RESOLVED);

                // We still need to fire the STOPPED event, but we will do
                // it later so we can release the bundle lock.
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
                throw new BundleException(
                    "Activator stop error in bundle " + bundle + ".", rethrow);
            }
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }

        // If there was no exception, then we should fire the STOPPED event
        // here without holding the lock.
        fireBundleEvent(BundleEvent.STOPPED, bundle);
    }

    void uninstallBundle(BundleImpl bundle) throws BundleException
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle,
                Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
        }
        catch (IllegalStateException ex)
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalStateException("Cannot uninstall an uninstalled bundle.");
            }
            else
            {
                throw new BundleException(
                    "Bundle " + bundle
                    + " cannot be uninstalled since it is stopping.");
            }
        }

        try
        {
            // The spec says that uninstall should always succeed, so
            // catch an exception here if stop() doesn't succeed and
            // rethrow it at the end.
            if (!bundle.isExtension() && (bundle.getState() == Bundle.ACTIVE))
            {
                try
                {
                    stopBundle(bundle, true);
                }
                catch (BundleException ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                }
            }

            // Remove the bundle from the installed map.
            BundleImpl target = null;
            synchronized (m_installedBundleLock_Priority2)
            {
                target = (BundleImpl) m_installedBundleMap.remove(bundle.getLocation());
                m_installedBundleIndex.remove(new Long(target.getBundleId()));
            }

            // Finally, put the uninstalled bundle into the
            // uninstalled list for subsequent refreshing.
            if (target != null)
            {
                // Set the bundle's persistent state to uninstalled.
                bundle.setPersistentStateUninstalled();

                // Put bundle in uninstalled bundle array.
                rememberUninstalledBundle(bundle);
            }
            else
            {
                m_logger.log(
                    Logger.LOG_ERROR, "Unable to remove bundle from installed map!");
            }

            setBundleStateAndNotify(bundle, Bundle.INSTALLED);

            // Unfortunately, fire UNRESOLVED event while holding the lock,
            // since we still need to change the bundle state.
            fireBundleEvent(BundleEvent.UNRESOLVED, bundle);

            // Set state to uninstalled.
            setBundleStateAndNotify(bundle, Bundle.UNINSTALLED);
            bundle.setLastModified(System.currentTimeMillis());
        }
        finally
        {
            // Always release bundle lock.
            releaseBundleLock(bundle);
        }

        // Fire UNINSTALLED event without holding the lock.
        fireBundleEvent(BundleEvent.UNINSTALLED, bundle);

        // Acquire global lock to check if we should auto-refresh.
        boolean locked = acquireGlobalLock();
        if (locked)
        {
            try
            {
                // If the bundle is not used by anyone, then garbage
                // collect it now.
                if (!bundle.isUsed())
                {
                    try
                    {
                        refreshPackages(new BundleImpl[] { bundle });
                    }
                    catch (Exception ex)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Unable to immediately garbage collect the bundle.", ex);
                    }
                }
            }
            finally
            {
                // Always release the global lock.
                releaseGlobalLock();
            }
        }
    }

    //
    // Implementation of BundleContext interface methods.
    //

    /**
     * Implementation for BundleContext.getProperty(). Returns
     * environment property associated with the framework.
     *
     * @param key The name of the property to retrieve.
     * @return The value of the specified property or null.
    **/
    String getProperty(String key)
    {
        // First, check the config properties.
        String val = (String) m_configMap.get(key);
        // If not found, then try the system properties.
        return (val == null) ? System.getProperty(key) : val;
    }

    Bundle installBundle(String location, InputStream is)
        throws BundleException
    {
        return installBundle(-1, location, null, is);
    }

    private Bundle installBundle(long id, String location, BundleArchive ba, InputStream is)
        throws BundleException
    {
        BundleImpl bundle = null;

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
            bundle = (BundleImpl) getBundle(location);
            if (bundle != null)
            {
                return bundle;
            }

            // Determine if this is a new or existing bundle.
            boolean isNew = (ba == null);

            // If the bundle is new we must cache its JAR file.
            if (isNew)
            {
                // First generate an identifier for it.
                id = getNextId();

                try
                {
                    // Add the bundle to the cache.
                    ba = m_cache.create(id, location, is);
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
                    if (ba.getRevisionCount() > 1)
                    {
                        ba.purge();
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
                // Acquire the global lock to create the bundle,
                // since this impacts the global state.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    throw new BundleException(
                        "Unable to acquire the global lock to install the bundle.");
                }
                try
                {
                    bundle = new BundleImpl(this, ba);
                }
                finally
                {
                    // Always release the global lock.
                    releaseGlobalLock();
                }

                if (!bundle.isExtension())
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
                    m_resolverState.refreshSystemBundleModule(m_extensionManager.getModule());
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
                        if (bundle != null)
                        {
                            bundle.closeAndDelete();
                        }
                        else if (ba != null)
                        {
                            ba.closeAndDelete();
                        }
                    }
                    catch (Exception ex1)
                    {
                        m_logger.log(
                            Logger.LOG_ERROR,
                            "Could not remove from cache.", ex1);
                    }
                }

                if ((System.getSecurityManager() != null) &&
                    (ex instanceof SecurityException))
                {
                    throw (SecurityException) ex;
                }

                ex.printStackTrace();

                if (ex instanceof BundleException)
                {
                    throw (BundleException) ex;
                }
                else
                {
                    throw new BundleException("Could not create bundle object.", ex);
                }
            }

            // If the bundle is new, then set its start level; existing
            // bundles already have their start level set.
            if (isNew)
            {
                // This will persistently set the bundle's start level.
                bundle.setStartLevel(getInitialBundleStartLevel());
                bundle.setLastModified(System.currentTimeMillis());
            }

            synchronized (m_installedBundleLock_Priority2)
            {
                m_installedBundleMap.put(location, bundle);
                m_installedBundleIndex.put(new Long(bundle.getBundleId()), bundle);
            }

            if (bundle.isExtension())
            {
                m_extensionManager.startExtensionBundle(this, bundle);
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
     * Retrieves a bundle from its location.
     *
     * @param location The location of the bundle to retrieve.
     * @return The bundle associated with the location or null if there
     *         is no bundle associated with the location.
    **/
    Bundle getBundle(String location)
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
    Bundle getBundle(long id)
    {
        synchronized (m_installedBundleLock_Priority2)
        {
            BundleImpl bundle = (BundleImpl) m_installedBundleIndex.get(new Long(id));
            if (bundle != null)
            {
                return bundle;
            }
        }

        synchronized (m_uninstalledBundlesLock_Priority3)
        {
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                i++)
            {
                if (m_uninstalledBundles[i].getBundleId() == id)
                {
                    return m_uninstalledBundles[i];
                }
            }
        }

        return null;
    }

    /**
     * Implementation for BundleContext.getBundles(). Retrieves
     * all installed bundles.
     *
     * @return An array containing all installed bundles or null if
     *         there are no installed bundles.
    **/
    Bundle[] getBundles()
    {
        synchronized (m_installedBundleLock_Priority2)
        {
            if (m_installedBundleMap.size() == 0)
            {
                return null;
            }

            return (Bundle[]) m_installedBundleIndex.values().toArray(
                new Bundle[m_installedBundleIndex.size()]);
        }
    }

    void addBundleListener(Bundle bundle, BundleListener l)
    {
        m_dispatcher.addListener(bundle, BundleListener.class, l, null);
    }

    void removeBundleListener(Bundle bundle, BundleListener l)
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
    void addServiceListener(Bundle bundle, ServiceListener l, String f)
        throws InvalidSyntaxException
    {
        Filter oldFilter = m_dispatcher.addListener(
            bundle, ServiceListener.class, l, (f == null) ? null : FrameworkUtil.createFilter(f));

        List listenerHooks = m_registry.getListenerHooks();
        if (oldFilter != null)
        {
            final Collection removed = Collections.singleton(
                new ListenerHookInfoImpl(bundle.getBundleContext(), l, oldFilter.toString(), true));
            InvokeHookCallback removedCallback = new ListenerHookRemovedCallback(removed);
            for (int i = 0; i < listenerHooks.size(); i++)
            {
                m_registry.invokeHook((ServiceReference) listenerHooks.get(i), this, removedCallback);
            }
        }

        // Invoke the ListenerHook.added() on all hooks.
        final Collection added = Collections.singleton(
            new ListenerHookInfoImpl(bundle.getBundleContext(), l, f, false));
        InvokeHookCallback addedCallback = new InvokeHookCallback()
        {
            public void invokeHook(Object hook)
            {
                ((ListenerHook) hook).added(added);
            }
        };
        for (int i = 0; i < listenerHooks.size(); i++)
        {
            m_registry.invokeHook((ServiceReference) listenerHooks.get(i), this, addedCallback);
        }
    }

    /**
     * Implementation for BundleContext.removeServiceListener().
     * Removes service listeners from the listener list.
     *
     * @param bundle The context bundle of the listener
     * @param l The service listener to remove from the listener list.
    **/
    void removeServiceListener(Bundle bundle, ServiceListener l)
    {
        ListenerHook.ListenerInfo listener =
            m_dispatcher.removeListener(bundle, ServiceListener.class, l);

        if (listener != null)
        {
            // Invoke the ListenerHook.removed() on all hooks.
            List listenerHooks = m_registry.getListenerHooks();
            Collection c = Collections.singleton(listener);
            InvokeHookCallback callback = new ListenerHookRemovedCallback(c);
            for (int i = 0; i < listenerHooks.size(); i++)
            {
                m_registry.invokeHook((ServiceReference) listenerHooks.get(i), this, callback);
            }
        }
    }

    void addFrameworkListener(Bundle bundle, FrameworkListener l)
    {
        m_dispatcher.addListener(bundle, FrameworkListener.class, l, null);
    }

    void removeFrameworkListener(Bundle bundle, FrameworkListener l)
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
    ServiceRegistration registerService(
        BundleImpl bundle, String[] classNames, Object svcObj, Dictionary dict)
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
        try
        {
            if (bundle.isExtension())
            {
// TODO: EXTENSIONMANAGER - Verify this.
                acquireBundleLock(bundle, Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE);
            }
            else
            {
                acquireBundleLock(bundle, Bundle.STARTING | Bundle.ACTIVE);
            }
        }
        catch (IllegalStateException ex)
        {
            throw new IllegalStateException(
                "Can only register services while bundle is active or activating.");
        }

        ServiceRegistration reg = null;

        try
        {
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

        // Check to see if this a listener hook; if so, then we need
        // to invoke the callback with all existing service listeners.
        if (ServiceRegistry.isHook(classNames, ListenerHook.class, svcObj))
        {
            m_registry.invokeHook(reg.getReference(), this, new InvokeHookCallback()
            {
                public void invokeHook(Object hook)
                {
                    ((ListenerHook) hook).
                        added(m_dispatcher.wrapAllServiceListeners(false));
                }
            });
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
    ServiceReference[] getServiceReferences(
        final BundleImpl bundle, final String className,
        final String expr, final boolean checkAssignable)
        throws InvalidSyntaxException
    {
        // Define filter if expression is not null.
        Filter filter = null;
        if (expr != null)
        {
            filter = FrameworkUtil.createFilter(expr);
        }

        // Ask the service registry for all matching service references.
        final List refList = m_registry.getServiceReferences(className, filter);

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

        // activate findhooks
        List findHooks = m_registry.getFindHooks();
        InvokeHookCallback callback = new InvokeHookCallback()
        {
            public void invokeHook(Object hook)
            {
                ((FindHook) hook).find(bundle.getBundleContext(),
                    className,
                    expr,
                    !checkAssignable,
                    new ShrinkableCollection(refList));
            }
        };
        for (int i = 0; i < findHooks.size(); i++)
        {
            m_registry.invokeHook((ServiceReference) findHooks.get(i), this, callback);
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
    ServiceReference[] getAllowedServiceReferences(
        BundleImpl bundle, String className, String expr, boolean checkAssignable)
        throws InvalidSyntaxException
    {
        ServiceReference[] refs = getServiceReferences(bundle, className, expr, checkAssignable);

        Object sm = System.getSecurityManager();

        if ((sm == null) || (refs == null))
        {
            return refs;
        }

        List result = new ArrayList();

        for (int i = 0; i < refs.length; i++)
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

    Object getService(Bundle bundle, ServiceReference ref)
    {
        try
        {
            return m_registry.getService(bundle, ref);
        }
        catch (ServiceException ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, ref.getBundle(), ex);
        }

        return null;
    }

    boolean ungetService(Bundle bundle, ServiceReference ref)
    {
        return m_registry.ungetService(bundle, ref);
    }

    File getDataFile(BundleImpl bundle, String s)
    {
        try
        {
            if (bundle == this)
            {
                return m_cache.getSystemBundleDataFile(s);
            }

            return bundle.getArchive().getDataFile(s);
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
    Bundle getBundle(Class clazz)
    {
        if (clazz.getClassLoader() instanceof BundleReference)
        {
            // Only return the bundle if it is from this framework.
            BundleReference br = (BundleReference) clazz.getClassLoader();
            return ((br.getBundle() instanceof BundleImpl)
                && (((BundleImpl) br.getBundle()).getFramework() == this))
                    ? br.getBundle() : null;
        }
        try
        {
            return (m_extensionManager.getModule().getClassByDelegation(clazz.getName()) == clazz)
                ? this : null;
        }
        catch(ClassNotFoundException ex)
        {
            return null;
        }
    }

    /**
     * Returns the exported packages associated with the specified
     * package name. This is used by the PackageAdmin service
     * implementation.
     *
     * @param pkgName The name of the exported package to find.
     * @return The exported package or null if no matching package was found.
    **/
    ExportedPackage[] getExportedPackages(String pkgName)
    {
        // First, get all exporters of the package.
        PackageSource[] exporters =
            m_resolverState.getResolvedCandidates(
                new Requirement(
                    ICapability.PACKAGE_NAMESPACE,
                    null,
                    new R4Attribute[] { new R4Attribute(ICapability.PACKAGE_PROPERTY, pkgName, false) }));

        if (exporters != null)
        {
            List pkgs = new ArrayList();

            Requirement req = new Requirement(ICapability.PACKAGE_NAMESPACE,
                null,
                new R4Attribute[] { new R4Attribute(ICapability.PACKAGE_PROPERTY, pkgName, false) });

            for (int pkgIdx = 0; pkgIdx < exporters.length; pkgIdx++)
            {
                // Get the bundle associated with the current exporting module.
                BundleImpl bundle = (BundleImpl) exporters[pkgIdx].m_module.getBundle();

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
                IModule[] modules = bundle.getModules();
                for (int modIdx = 0; modIdx < modules.length; modIdx++)
                {
                    ICapability[] ec = modules[modIdx].getCapabilities();
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
    ExportedPackage[] getExportedPackages(Bundle b)
    {
        List list = new ArrayList();

        // If a bundle is specified, then return its
        // exported packages.
        if (b != null)
        {
            BundleImpl bundle = (BundleImpl) b;
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
                        BundleImpl bundle = m_uninstalledBundles[bundleIdx];
                        getExportedPackages(bundle, list);
                    }
                }

                // Now get exported packages from installed bundles.
                Bundle[] bundles = getBundles();
                for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
                {
                    BundleImpl bundle = (BundleImpl) bundles[bundleIdx];
                    getExportedPackages(bundle, list);
                }
            }
        }

        return (list.isEmpty())
            ? null
            : (ExportedPackage[]) list.toArray(new ExportedPackage[list.size()]);
    }

    /**
     * Adds any current active exported packages from the specified bundle
     * to the passed in list.
     * @param bundle The bundle from which to retrieve exported packages.
     * @param list The list to which the exported packages are added
    **/
    private void getExportedPackages(BundleImpl bundle, List list)
    {
        // Since a bundle may have many modules associated with it,
        // one for each revision in the cache, search each module
        // for each revision to get all exports.
        IModule[] modules = bundle.getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            ICapability[] caps = modules[modIdx].getCapabilities();
            if ((caps != null) && (caps.length > 0))
            {
                for (int capIdx = 0; capIdx < caps.length; capIdx++)
                {
                    // See if the target bundle's module is one of the
                    // "in use" exporters of the package.
                    if (caps[capIdx].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                    {
                        PackageSource[] inUseModules = m_resolverState.getResolvedCandidates(
                            new Requirement(
                                ICapability.PACKAGE_NAMESPACE,
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

    Bundle[] getDependentBundles(BundleImpl exporter)
    {
        // Create list for storing importing bundles.
        List list = new ArrayList();

        // Get all dependent modules from all exporter module revisions.
        IModule[] modules = exporter.getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            IModule[] dependents = ((ModuleImpl) modules[modIdx]).getDependents();
            for (int depIdx = 0;
                (dependents != null) && (depIdx < dependents.length);
                depIdx++)
            {
                list.add(dependents[depIdx].getBundle());
            }
        }

        // Return the results.
        if (list.size() > 0)
        {
            return (Bundle[]) list.toArray(new Bundle[list.size()]);
        }

        return null;
    }

    Bundle[] getImportingBundles(ExportedPackage ep)
    {
        // Create list for storing importing bundles.
        List list = new ArrayList();

        // Get exporting bundle information.
        BundleImpl exporter = (BundleImpl) ep.getExportingBundle();

        // Get all importers and requirers for all revisions of the bundle.
        // The spec says that require-bundle should be returned with importers.
        IModule[] expModules = exporter.getModules();
        for (int expIdx = 0; (expModules != null) && (expIdx < expModules.length); expIdx++)
        {
            // Include any importers that have wires to the specific
            // exported package.
            IModule[] dependents = ((ModuleImpl) expModules[expIdx]).getDependentImporters();
            for (int depIdx = 0; (dependents != null) && (depIdx < dependents.length); depIdx++)
            {
                IWire[] wires = dependents[depIdx].getWires();
                for (int wireIdx = 0; (wires != null) && (wireIdx < wires.length); wireIdx++)
                {
                    if ((wires[wireIdx].getExporter() == expModules[expIdx])
                        && (wires[wireIdx].hasPackage(ep.getName())))
                    {
                        list.add(dependents[depIdx].getBundle());
                    }
                }
            }
            dependents = ((ModuleImpl) expModules[expIdx]).getDependentRequirers();
            for (int depIdx = 0; (dependents != null) && (depIdx < dependents.length); depIdx++)
            {
                list.add(dependents[depIdx].getBundle());
            }
        }

        // Return the results.
        if (list.size() > 0)
        {
            return (Bundle[]) list.toArray(new Bundle[list.size()]);
        }

        return null;
    }

    boolean resolveBundles(Bundle[] targets)
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                "Unable to acquire global lock to perform resolve.",
                null);
            return false;
        }

        try
        {
            // Determine set of bundles to be resolved, which is either the
            // specified bundles or all bundles if null.
            if (targets == null)
            {
                List list = new ArrayList();

                // Add all unresolved bundles to the list.
                synchronized (m_installedBundleLock_Priority2)
                {
                    Iterator iter = m_installedBundleMap.values().iterator();
                    while (iter.hasNext())
                    {
                        BundleImpl bundle = (BundleImpl) iter.next();
                        if (bundle.getState() == Bundle.INSTALLED)
                        {
                            list.add(bundle);
                        }
                    }
                }

                // Create an array.
                if (list.size() > 0)
                {
                    targets = (Bundle[]) list.toArray(new BundleImpl[list.size()]);
                }
            }

            // Now resolve each target bundle.
            boolean result = true;

            // If there are targets, then resolve each one.
            for (int i = 0; (targets != null) && (i < targets.length); i++)
            {
                try
                {
                    resolveBundle((BundleImpl) targets[i]);
                }
                catch (BundleException ex)
                {
                    result = false;
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Unable to resolve bundle " + targets[i].getBundleId(),
                        ex);
                }
            }

            return result;
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }
    }

    private void resolveBundle(BundleImpl bundle) throws BundleException
    {
        try
        {
            m_felixResolver.resolve(bundle.getCurrentModule());
        }
        catch (ResolveException ex)
        {
            if (ex.getModule() != null)
            {
                Bundle b = ((ModuleImpl) ex.getModule()).getBundle();
                throw new BundleException(
                    "Unresolved constraint in bundle " + b + ": "
                    + ((ex.getRequirement() == null)
                        ? ex.getMessage() : ex.getRequirement().toString()));
            }
            else
            {
                throw new BundleException(ex.getMessage());
            }
        }
    }

    void refreshPackages(Bundle[] targets)
    {
        // Acquire global lock.
        boolean locked = acquireGlobalLock();
        if (!locked)
        {
            // If the thread calling holds bundle locks, then we might not
            // be able to get the global lock. However, in practice this
            // should not happen since the calls to this method have either
            // already acquired the global lock or it is PackageAdmin which
            // doesn't hold bundle locks.
            throw new IllegalStateException(
                "Unable to acquire global lock for refresh.");
        }

        // Determine set of bundles to refresh, which is all transitive
        // dependencies of specified set or all transitive dependencies
        // of all bundles if null is specified.
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
                    BundleImpl bundle = (BundleImpl) iter.next();
                    if (bundle.isRemovalPending())
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

        // If there are targets, then find all dependencies for each one.
        BundleImpl[] bundles = null;
        if (newTargets != null)
        {
            // Create map of bundles that import the packages
            // from the target bundles.
            Map map = new HashMap();
            for (int targetIdx = 0; targetIdx < newTargets.length; targetIdx++)
            {
                // Add the current target bundle to the map of
                // bundles to be refreshed.
                BundleImpl target = (BundleImpl) newTargets[targetIdx];
                map.put(target, target);
                // Add all importing bundles to map.
                populateDependentGraph(target, map);
            }

            bundles = (BundleImpl[]) map.values().toArray(new BundleImpl[map.size()]);
        }

        // Now refresh each bundle.
        try
        {
            boolean restart = false;

            Bundle systemBundle = this;

            // We need to restart the framework if either an extension bundle is
            // refreshed or the system bundle is refreshed and any extension bundle
            // has been updated or uninstalled.
            for (int i = 0; (bundles != null) && !restart && (i < bundles.length); i++)
            {
                if (systemBundle == bundles[i])
                {
                    Bundle[] allBundles = getBundles();
                    for (int j = 0; !restart && j < allBundles.length; j++)
                    {
                        if (((BundleImpl) allBundles[j]).isExtension() &&
                            (allBundles[j].getState() == Bundle.INSTALLED))
                        {
                            restart = true;
                        }
                    }
                }
            }

            // Remove any targeted bundles from the uninstalled bundles
            // array, since they will be removed from the system after
            // the refresh.
            // TODO: FRAMEWORK - Is this correct?
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                forgetUninstalledBundle(bundles[i]);
            }
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
                    helpers[i] = new RefreshHelper(bundles[i]);
                }

                // Stop, purge or remove, and reinitialize all bundles first.
                // TODO: FRAMEWORK - this will stop the system bundle if
                // somebody called refresh 0. Is this what we want?
                for (int i = 0; i < helpers.length; i++)
                {
                    if (helpers[i] != null)
                    {
                        helpers[i].stop();
                        helpers[i].refreshOrRemove();
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

            if (restart)
            {
                try
                {
                    update();
                }
                catch (BundleException ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Framework restart error.", ex);
                }
            }
        }
        finally
        {
            // Always release the global lock.
            releaseGlobalLock();
        }

        fireFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, this, null);
    }

    private void populateDependentGraph(BundleImpl exporter, Map map)
    {
        // Get all dependent bundles of this bundle.
        Bundle[] dependents = getDependentBundles(exporter);

        for (int depIdx = 0;
            (dependents != null) && (depIdx < dependents.length);
            depIdx++)
        {
            // Avoid cycles if the bundle is already in map.
            if (!map.containsKey(dependents[depIdx]))
            {
                // Add each importing bundle to map.
                map.put(dependents[depIdx], dependents[depIdx]);
                // Now recurse into each bundle to get its importers.
                populateDependentGraph(
                    (BundleImpl) dependents[depIdx], map);
            }
        }
    }

    //
    // Miscellaneous private methods.
    //

    private volatile SecurityProvider m_securityProvider;

    SecurityProvider getSecurityProvider()
    {
        return m_securityProvider;
    }

    void setSecurityProvider(SecurityProvider securityProvider)
    {
        m_securityProvider = securityProvider;
    }

    Object getSignerMatcher(BundleImpl bundle)
    {
        if ((bundle != this) && (m_securityProvider != null))
        {
            return m_securityProvider.getSignerMatcher(bundle);
        }
        return null;
    }

    boolean impliesBundlePermission(BundleProtectionDomain bundleProtectionDomain, Permission permission, boolean direct)
    {
        if (m_securityProvider != null)
        {
            return m_securityProvider.hasBundlePermission(bundleProtectionDomain, permission, direct);
        }
        return true;
    }

    private BundleActivator createBundleActivator(BundleImpl impl)
        throws Exception
    {
        // CONCURRENCY NOTE:
        // This method is called indirectly from startBundle() (via _startBundle()),
        // which has the bundle lock, so there is no need to do any locking here.

        // Get the activator class from the header map.
        BundleActivator activator = null;
        Map headerMap = impl.getCurrentModule().getHeaders();
        String className = (String) headerMap.get(Constants.BUNDLE_ACTIVATOR);
        // Try to instantiate activator class if present.
        if (className != null)
        {
            className = className.trim();
            Class clazz;
            try
            {
                clazz = impl.getCurrentModule().getClassByDelegation(className);
            }
            catch (ClassNotFoundException ex)
            {
                throw new BundleException("Not found: " + className, ex);
            }
            activator = (BundleActivator) clazz.newInstance();
        }

        return activator;
    }

    private void refreshBundle(BundleImpl bundle) throws Exception
    {
        // Acquire bundle lock.
        try
        {
            acquireBundleLock(bundle, Bundle.INSTALLED | Bundle.RESOLVED);
        }
        catch (IllegalStateException ex)
        {
            throw new BundleException(
                "Bundle state has changed unexpectedly during refresh.");
        }

        try
        {
            // Reset the bundle object and fire UNRESOLVED event.
            ((BundleImpl) bundle).refresh();
        }
        catch (Exception ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
        }
        finally
        {
            // Always release the bundle lock.
            releaseBundleLock(bundle);
        }
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
     * @param event The service event to fire.
     * @param reg The service registration associated with the service object.
    **/
    private void fireServiceEvent(ServiceEvent event, Dictionary oldProps)
    {
        m_dispatcher.fireServiceEvent(event, oldProps, this);
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
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_EXTENSION,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_FRAGMENT,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_FRAMEWORK_REQUIREBUNDLE,
            "true");
        m_configMutableMap.put(
            FelixConstants.SUPPORTS_BOOTCLASSPATH_EXTENSION,
            "false");

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

    public class FelixResolver
    {
        private final Resolver m_resolver;
        private final FelixResolverState m_resolverState;

        public FelixResolver(Resolver resolver, FelixResolverState resolverState)
        {
            m_resolver = resolver;
            m_resolverState = resolverState;
        }

        public void resolve(IModule rootModule) throws ResolveException
        {
            // Although there is a race condition to check the bundle state
            // then lock it, we do this because we don't want to acquire the
            // a lock just to check if the module is resolved, which itself
            // is a safe read. If the module isn't resolved, we end up double
            // check the resolved status later.
            if (!rootModule.isResolved())
            {
                // Acquire global lock.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    throw new ResolveException(
                        "Unable to acquire global lock for resolve.", rootModule, null);
                }

                try
                {
                    BundleImpl bundle = (BundleImpl) rootModule.getBundle();

                    // Extensions are resolved differently.
                    if (bundle.isExtension())
                    {
                        return;
                    }

                    // If a security manager is installed, then check for permission
                    // to import the necessary packages.
                    if (System.getSecurityManager() != null)
                    {
                        BundleProtectionDomain pd = (BundleProtectionDomain)
                            bundle.getProtectionDomain();

/*
 TODO: SECURITY - We need to fix this import check by looking at the wire
            associated with it, not the import since we don't know the
            package name associated with the import since it is a filter.

                    IRequirement[] imports = bundle.getInfo().getCurrentModule().getRequirements();
                    for (int i = 0; i < imports.length; i++)
                    {
                        if (imports[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                        {
                            PackagePermission perm = new PackagePermission(
                                imports[i].???,
                                PackagePermission.IMPORT);

                            if (!pd.impliesDirect(perm))
                            {
                                throw new java.security.AccessControlException(
                                    "PackagePermission.IMPORT denied for import: " +
                                    imports[i].getName(), perm);
                            }
                        }
                    }
*/
                        // Check export permission for all exports of the current module.
                        ICapability[] exports = rootModule.getCapabilities();
                        for (int i = 0; i < exports.length; i++)
                        {
                            if (exports[i].getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
                            {
                                PackagePermission perm = new PackagePermission(
                                    (String) exports[i].getProperties().get(ICapability.PACKAGE_PROPERTY), PackagePermission.EXPORT);

                                if (!pd.impliesDirect(perm))
                                {
                                    throw new java.security.AccessControlException(
                                        "PackagePermission.EXPORT denied for export: " +
                                        exports[i].getProperties().get(ICapability.PACKAGE_PROPERTY), perm);
                                }
                            }
                        }
                    }

                    // Before trying to resolve, tell the resolver state to
                    // merge all fragments into hosts, which may result in the
                    // rootModule changing if the root is a fragment.
                    IModule newRootModule;
                    try
                    {
                        newRootModule = m_resolverState.mergeFragments(rootModule);
                    }
                    catch (Exception ex)
                    {
                        ex.printStackTrace();
                        throw new ResolveException("Unable to merge fragments", rootModule, null);
                    }

                    if (!Util.isFragment(newRootModule))
                    {
                        // Resolve the module.
                        Map resolvedModuleWireMap = m_resolver.resolve(m_resolverState, newRootModule);

                        // Mark all modules as resolved.
                        markResolvedModules(resolvedModuleWireMap);
                    }
                }
                finally
                {
                    // Always release the global lock.
                    releaseGlobalLock();
                }
            }
        }

        public IWire resolveDynamicImport(IModule importer, String pkgName) throws ResolveException
        {
            IWire candidateWire = null;

            if (importer.isResolved())
            {
                // Acquire global lock.
                boolean locked = acquireGlobalLock();
                if (!locked)
                {
                    throw new ResolveException(
                        "Unable to acquire global lock for resolve.", importer, null);
                }

                try
                {
                    // Double check to make sure that someone hasn't beaten us to
                    // dynamically importing the package, which can happen if two
                    // threads are racing to do so. If we have an existing wire,
                    // then just return it instead.
                    IWire[] wires = importer.getWires();
                    for (int i = 0; (wires != null) && (i < wires.length); i++)
                    {
                        if (wires[i].hasPackage(pkgName))
                        {
                            return wires[i];
                        }
                    }

                    // Before trying to resolve, tell the resolver state to
                    // merge all fragments into their hosts.
// TODO: FRAGMENT - We need to rethink how we do fragment merging...probably merging
//       as bundles are installed would be better.
                    if (Resolver.isDynamicImportAllowed(importer, pkgName))
                    {
                        try
                        {
                            m_resolverState.mergeFragments(null);
                        }
                        catch (Exception ex)
                        {
                            ex.printStackTrace();
                            throw new ResolveException("Unable to merge fragments", importer, null);
                        }
                    }

                    Object[] result = m_resolver.resolveDynamicImport(m_resolverState, importer, pkgName);
                    if (result != null)
                    {
                        candidateWire = (IWire) result[0];
                        Map resolvedModuleWireMap = (Map) result[1];

                        // Mark all modules as resolved.
                        markResolvedModules(resolvedModuleWireMap);

                        // Dynamically add new wire to importing module.
                        if (candidateWire != null)
                        {
                            wires = importer.getWires();
                            IWire[] newWires = null;
                            if (wires == null)
                            {
                                newWires = new IWire[1];
                            }
                            else
                            {
                                newWires = new IWire[wires.length + 1];
                                System.arraycopy(wires, 0, newWires, 0, wires.length);
                            }

                            newWires[newWires.length - 1] = candidateWire;
                            ((ModuleImpl) importer).setWires(newWires);
m_logger.log(Logger.LOG_DEBUG, "DYNAMIC WIRE: " + newWires[newWires.length - 1]);
                        }
                    }
                }
                finally
                {
                    // Always release the global lock.
                    releaseGlobalLock();
                }
            }

            return candidateWire;
        }

        public synchronized PackageSource[] getResolvedCandidates(IRequirement req)
        {
            return m_resolverState.getResolvedCandidates(req);
        }

        public synchronized PackageSource[] getUnresolvedCandidates(IRequirement req)
        {
            return m_resolverState.getUnresolvedCandidates(req);
        }

        private void markResolvedModules(Map resolvedModuleWireMap)
        {
            if (resolvedModuleWireMap != null)
            {
                Iterator iter = resolvedModuleWireMap.entrySet().iterator();
                // Iterate over the map to mark the modules as resolved and
                // update our resolver data structures.
                List wireList = new ArrayList();
                while (iter.hasNext())
                {
                    wireList.clear();

                    Map.Entry entry = (Map.Entry) iter.next();
                    IModule module = (IModule) entry.getKey();
                    IWire[] wires = (IWire[]) entry.getValue();

                    // Only add wires attribute if some exist; export
                    // only modules may not have wires.
// TODO: RESOLVER - Seems stupid that we package these up as wires to tear them apart.
                    if (wires.length > 0)
                    {
                        for (int wireIdx = 0; wireIdx < wires.length; wireIdx++)
                        {
                            wireList.add(wires[wireIdx]);
                            m_logger.log(
                                Logger.LOG_DEBUG,
                                "WIRE: " + wires[wireIdx]);
                        }
                        wires = (IWire[]) wireList.toArray(new IWire[wireList.size()]);
                        ((ModuleImpl) module).setWires(wires);
                    }

                    // Resolve all attached fragments.
                    IModule[] fragments = ((ModuleImpl) module).getFragments();
                    for (int i = 0; (fragments != null) && (i < fragments.length); i++)
                    {
                        ((ModuleImpl) fragments[i]).setResolved();
                        // Update the state of the module's bundle to resolved as well.
                        markBundleResolved(fragments[i]);
                        m_logger.log(
                            Logger.LOG_DEBUG,
                            "FRAGMENT WIRE: " + fragments[i] + " -> hosted by -> " + module);
                    }
                    // Update the resolver state to show the module as resolved.
                    ((ModuleImpl) module).setResolved();
                    m_resolverState.moduleResolved(module);
                    // Update the state of the module's bundle to resolved as well.
                    markBundleResolved(module);
                }
            }
        }

        private void markBundleResolved(IModule module)
        {
            // Update the bundle's state to resolved when the
            // current module is resolved; just ignore resolve
            // events for older revisions since this only occurs
            // when an update is done on an unresolved bundle
            // and there was no refresh performed.
            BundleImpl bundle = (BundleImpl) module.getBundle();

            // Lock the bundle first.
            try
            {
// TODO: RESOLVER - Seems like we should release the lock before we fire the event.
                // Acquire bundle lock.
                try
                {
                    acquireBundleLock(bundle, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE);
                }
                catch (IllegalStateException ex)
                {
                    // There is nothing we can do.
                }
                if (bundle.getCurrentModule() == module)
                {
                    if (bundle.getState() != Bundle.INSTALLED)
                    {
                        m_logger.log(
                            Logger.LOG_WARNING,
                            "Received a resolve event for a bundle that has already been resolved.");
                    }
                    else
                    {
                        setBundleStateAndNotify(bundle, Bundle.RESOLVED);
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

    class SystemBundleActivator implements BundleActivator, Runnable
    {
        public void start(BundleContext context) throws Exception
        {
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
            // The state of the framework should be STOPPING, so
            // acquire the bundle lock to verify it.
            acquireBundleLock(Felix.this, Bundle.STOPPING);
            releaseBundleLock(Felix.this);

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

            // Shutdown event dispatching queue.
            EventDispatcher.shutdown();

            // Since there may be updated and uninstalled bundles that
            // have not been refreshed, we will take care of refreshing
            // them during shutdown.

            // Refresh all updated bundles.
            Bundle[] bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                BundleImpl bundle = (BundleImpl) bundles[i];
                if (bundle.isRemovalPending())
                {
                    try
                    {
                        refreshBundle(bundle);
                    }
                    catch (Exception ex)
                    {
                        fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
                        m_logger.log(Logger.LOG_ERROR, "Unable to purge bundle "
                            + bundle._getLocation(), ex);
                    }
                }
            }

            // Delete uninstalled bundles.
            for (int i = 0;
                (m_uninstalledBundles != null) && (i < m_uninstalledBundles.length);
                i++)
            {
                try
                {
                    m_uninstalledBundles[i].closeAndDelete();
                }
                catch (Exception ex)
                {
                    m_logger.log(
                        Logger.LOG_ERROR,
                        "Unable to remove "
                        + m_uninstalledBundles[i]._getLocation(), ex);
                }
            }

            // Dispose of the bundles to close their associated contents.
            bundles = getBundles();
            for (int i = 0; i < bundles.length; i++)
            {
                ((BundleImpl) bundles[i]).close();
            }

            // Stop all system bundle activators.
            for (int i = 0; i < m_activatorList.size(); i++)
            {
                try
                {
                    Felix.m_secureAction.stopActivator((BundleActivator)
                        m_activatorList.get(i), getBundleContext());
                }
                catch (Throwable throwable)
                {
                    m_logger.log(
                        Logger.LOG_WARNING,
                        "Exception stopping a system bundle activator.",
                        throwable);
                }
            }

            if (m_extensionManager != null)
            {
                m_extensionManager.removeExtensions(Felix.this);
            }

            // Set the framework state to resolved.
            acquireBundleLock(Felix.this, Bundle.STOPPING);
            try
            {
                // Clean up the bundle context.
                ((BundleContextImpl) getBundleContext()).invalidate();
                setBundleContext(null);

                // Set the framework state to resolved and open
                // the shutdown gate.
                setBundleStateAndNotify(Felix.this, Bundle.RESOLVED);
                m_shutdownGate.open();
                m_shutdownGate = null;
                m_shutdownThread = null;
            }
            finally
            {
                releaseBundleLock(Felix.this);
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
        private BundleImpl m_bundle = null;
        private int m_oldState = Bundle.INSTALLED;

        public RefreshHelper(Bundle bundle)
        {
            m_bundle = (BundleImpl) bundle;
        }

        public void stop()
        {
// TODO: LOCKING - This is not really correct.
            if (m_bundle.getState() == Bundle.ACTIVE)
            {
                m_oldState = Bundle.ACTIVE;
                try
                {
                    stopBundle(m_bundle, false);
                }
                catch (Throwable ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }

        public void refreshOrRemove()
        {
            try
            {
                // Delete or refresh the bundle depending on its
                // current state.
                if (m_bundle.getState() == Bundle.UNINSTALLED)
                {
                    m_bundle.closeAndDelete();
                    m_bundle = null;
                }
                else
                {
                    // This removes all old bundle modules from memory and
                    // all old revisions from disk. It only maintains the
                    // newest version in the bundle cache.
                    refreshBundle(m_bundle);
                }
            }
            catch (Throwable ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
            }
        }

        public void restart()
        {
            if ((m_bundle != null) && (m_oldState == Bundle.ACTIVE))
            {
                try
                {
// TODO: LAZY - Not sure if this is the best way...
                    int options = Bundle.START_TRANSIENT;
                    options = (m_bundle.getPersistentState() == Bundle.STARTING)
                        ? options | Bundle.START_ACTIVATION_POLICY
                        : options;
                    startBundle(m_bundle, options);
                }
                catch (Throwable ex)
                {
                    fireFrameworkEvent(FrameworkEvent.ERROR, m_bundle, ex);
                }
            }
        }
    }

    private static class ListenerHookRemovedCallback implements InvokeHookCallback
    {
        private final Collection /* ListenerHookInfo */ m_removed;

        ListenerHookRemovedCallback(Collection /* ListenerHookInfo */ removed)
        {
            m_removed = removed;
        }

        public void invokeHook(Object hook)
        {
            ((ListenerHook) hook).removed(m_removed);
        }
    }

    //
    // Locking related methods.
    //

    private void rememberUninstalledBundle(BundleImpl bundle)
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
                BundleImpl[] newBundles =
                    new BundleImpl[m_uninstalledBundles.length + 1];
                System.arraycopy(m_uninstalledBundles, 0,
                    newBundles, 0, m_uninstalledBundles.length);
                newBundles[m_uninstalledBundles.length] = bundle;
                m_uninstalledBundles = newBundles;
            }
            else
            {
                m_uninstalledBundles = new BundleImpl[] { bundle };
            }
        }
    }

    private void forgetUninstalledBundle(BundleImpl bundle)
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
                    m_uninstalledBundles = new BundleImpl[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    BundleImpl[] newBundles =
                        new BundleImpl[m_uninstalledBundles.length - 1];
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

    void acquireInstallLock(String location)
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

    void releaseInstallLock(String location)
    {
        synchronized (m_installRequestLock_Priority1)
        {
            m_installRequestMap.remove(location);
            m_installRequestLock_Priority1.notifyAll();
        }
    }

    void setBundleStateAndNotify(BundleImpl bundle, int state)
    {
        synchronized (m_bundleLock)
        {
            bundle.__setState(state);
            m_bundleLock.notifyAll();
        }
    }

    /**
     * This method acquires the lock for the specified bundle as long as the
     * bundle is in one of the specified states. If it is not, an exception
     * is thrown. Bundle state changes will be monitored to avoid deadlocks.
     * @param bundle The bundle to lock.
     * @param desiredStates Logically OR'ed desired bundle states.
     * @throws java.lang.IllegalStateException If the bundle is not in one of the
     *         specified desired states.
    **/
    void acquireBundleLock(BundleImpl bundle, int desiredStates)
        throws IllegalStateException
    {
        synchronized (m_bundleLock)
        {
            // Wait if the desired bundle is already locked by someone else
            // or if any thread has the global lock, unless the current thread
            // holds the global lock.
            while (!bundle.isLockable() ||
                ((m_globalLockThread != null) && (m_globalLockThread != Thread.currentThread())))
            {
                // Check to make sure the bundle is in a desired state.
                // If so, keep waiting. If not, throw an exception.
                if ((desiredStates & bundle.getState()) == 0)
                {
                    throw new IllegalStateException("Bundle in unexpected state.");
                }
                // If the calling thread already owns the global lock, then make
                // sure no other thread is trying to promote a bundle lock to a
                // global lock. If so, interrupt the other thread to avoid deadlock.
                else if (m_globalLockThread == Thread.currentThread()
                    && (bundle.getLockingThread() != null)
                    && m_globalLockWaitersList.contains(bundle.getLockingThread()))
                {
                    bundle.getLockingThread().interrupt();
                }

                try
                {
                    m_bundleLock.wait();
                }
                catch (InterruptedException ex)
                {
                    // Ignore and just keep waiting.
                }
            }

            // Now that we can acquire the bundle lock, let's check to make sure
            // it is in a desired state; if not, throw an exception and do not
            // lock it.
            if ((desiredStates & bundle.getState()) == 0)
            {
                throw new IllegalStateException("Bundle in unexpected state.");
            }

            // Acquire the bundle lock.
            bundle.lock();
        }
    }

    /**
     * Releases the bundle's lock.
     * @param bundle The bundle whose lock is to be released.
     * @throws java.lang.IllegalStateException If the calling thread does not
     *         own the bundle lock.
    **/
    void releaseBundleLock(BundleImpl bundle)
    {
        synchronized (m_bundleLock)
        {
            // Unlock the bundle.
            bundle.unlock();
            // If the thread no longer holds the bundle lock,
            // then remove it from the held lock map.
            if (bundle.getLockingThread() == null)
            {
                m_bundleLock.notifyAll();
            }
        }
    }

    /**
     * Promotes a bundle lock to the global lock. This is called by a thread
     * wanting the global lock, but already holding a bundle lock (currently
     * only when updating a bundle). Since it is possible to deadlock when
     * trying to acquire the global lock while holding a bundle lock, this
     * method may fail if a potential deadlock is detected.
     * @param bundle The bundle already locked by the calling thread.
     * @return <tt>true</tt> if the global lock was successfully acquired,
     *         <tt>false</tt> otherwise.
    **/
    private boolean acquireGlobalLock()
    {
        synchronized (m_bundleLock)
        {
            // Wait as long as some other thread holds the global lock
            // and the current thread is not interrupted.
            boolean interrupted = false;
            while (!interrupted
                && (m_globalLockThread != null)
                && (m_globalLockThread != Thread.currentThread()))
            {
                // Add calling thread to global lock waiters list.
                m_globalLockWaitersList.add(Thread.currentThread());
                // We need to wake up all waiting threads so we can
                // recheck for potential deadlock in acquireBundleLock()
                // if this thread was holding a bundle lock and is now
                // trying to promote it to a global lock.
                m_bundleLock.notifyAll();
                // Now wait for the global lock.
                try
                {
                    m_bundleLock.wait();
                }
                catch (InterruptedException ex)
                {
                    interrupted = true;
                }
                // At this point we are either interrupted or will get the
                // global lock, so remove the thread from the waiters list.
                m_globalLockWaitersList.remove(Thread.currentThread());
            }

            // Check to see if we were interrupted, which means someone
            // with the global lock wants our bundle lock, so we should
            // fail gracefully.
            if (!interrupted)
            {
                // Increment the current thread's global lock count.
                m_globalLockCount++;
                m_globalLockThread = Thread.currentThread();
            }

            // Note: If the thread was interrupted, there is no reason to notify
            // anyone, since the thread was likely interrupted to force it to give
            // up a bundle lock it is holding. When it does give up the bundle
            // lock, it will do a notifyAll() in there.

            return !interrupted;
        }
    }

    /**
     * Releases the global lock.
     * @throws java.lang.IllegalStateException If the calling thread does not
     *         own the global lock.
    **/
    private void releaseGlobalLock()
    {
        synchronized (m_bundleLock)
        {
            // Decrement the current thread's global lock count;
            if (m_globalLockThread == Thread.currentThread())
            {
                m_globalLockCount--;
                if (m_globalLockCount == 0)
                {
                    m_globalLockThread = null;
                    m_bundleLock.notifyAll();
                }
            }
            else
            {
                throw new IllegalStateException(
                    "The current thread doesn't own the global lock.");
            }
        }
    }

    private volatile URLHandlersActivator m_urlHandlersActivator;
    
    void setURLHandlersActivator(URLHandlersActivator urlHandlersActivator)
    {
        m_urlHandlersActivator = urlHandlersActivator;
    }
    
    Object getStreamHandlerService(String protocol)
    {
        return m_urlHandlersActivator.getStreamHandlerService(protocol);
    }
    
    Object getContentHandlerService(String mimeType)
    {
        return m_urlHandlersActivator.getContentHandlerService(mimeType);
    }
}