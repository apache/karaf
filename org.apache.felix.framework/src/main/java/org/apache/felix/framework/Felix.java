/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework;

import java.io.*;
import java.net.*;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.*;

import org.apache.felix.framework.cache.*;
import org.apache.felix.framework.searchpolicy.*;
import org.apache.felix.framework.util.*;
import org.apache.felix.moduleloader.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.startlevel.StartLevel;

public class Felix
{
    // Logging related member variables.
    private Logger m_logger = new Logger();
    // Config properties.
    private PropertyResolver m_config = new ConfigImpl();
    // Configuration properties passed into constructor.
    private MutablePropertyResolver m_configMutable = null;

    // MODULE FACTORY.
    private IModuleFactory m_factory = null;
    private R4SearchPolicyCore m_policyCore = null;

    // Object used as a lock when calculating which bundles
    // when performing an operation on one or more bundles.
    private Object[] m_bundleLock = new Object[0];

    // Maps a bundle location to a bundle location;
    // used to reserve a location when installing a bundle.
    private Map m_installRequestMap = null;
    // This lock must be acquired to modify m_installRequestMap;
    // to help avoid deadlock this lock as priority 1 and should
    // be acquired before locks with lower priority.
    private Object[] m_installRequestLock_Priority1 = new Object[0];

    // Maps a bundle location to a bundle.
    private HashMap m_installedBundleMap = null;
    // This lock must be acquired to modify m_installedBundleMap;
    // to help avoid deadlock this lock as priority 2 and should
    // be acquired before locks with lower priority.
    private Object[] m_installedBundleLock_Priority2 = new Object[0];

    // An array of uninstalled bundles before a refresh occurs.
    private BundleImpl[] m_uninstalledBundles = null;
    // This lock must be acquired to modify m_uninstalledBundles;
    // to help avoid deadlock this lock as priority 3 and should
    // be acquired before locks with lower priority.
    private Object[] m_uninstalledBundlesLock_Priority3 = new Object[0];

    // Status flag for framework.
    public static final int INITIAL_STATUS  = -1;
    public static final int RUNNING_STATUS  = 0;
    public static final int STARTING_STATUS = 1;
    public static final int STOPPING_STATUS = 2;
    private int m_frameworkStatus = INITIAL_STATUS;

    // Framework's active start level.
    private int m_activeStartLevel =
        FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;

    // Local file system cache.
    private BundleCache m_cache = null;

    // Next available bundle identifier.
    private long m_nextId = 1L;
    private Object m_nextIdLock = new Object[0];

    // List of event listeners.
    private FelixDispatchQueue m_dispatchQueue = null;
    // Re-usable event dispatchers.
    private Dispatcher m_frameworkDispatcher = null;
    private Dispatcher m_bundleDispatcher = null;
    private Dispatcher m_serviceDispatcher = null;

    // Service registry.
    private ServiceRegistry m_registry = null;

    // Reusable bundle URL stream handler.
    private URLStreamHandler m_bundleStreamHandler = null;

    // Reusable admin permission object for all instances
    // of the BundleImpl.
    private static AdminPermission m_adminPerm = new AdminPermission();

    /**
     * <p>
     * This method starts the framework instance; instances of the framework
     * are dormant until this method is called. The caller may also provide
     * <tt>MutablePropertyResolver</tt> implementations that the instance will
     * use to obtain configuration or framework properties. Configuration
     * properties are used internally by the framework and its extensions to alter
     * its default behavior. Framework properties are used by bundles
     * and are accessible from <tt>BundleContext.getProperty()</tt>.
     * </p>
     * <p>
     * Configuration properties are the sole means to configure the framework's
     * default behavior; the framework does not refer to any system properties for
     * configuration information. If a <tt>MutablePropertyResolver</tt> is
     * supplied to this method for configuration properties, then the framework will
     * consult the <tt>MutablePropertyResolver</tt> instance for any and all
     * configuration properties. It is possible to specify a <tt>null</tt>
     * configuration property resolver, in which case the framework will use its
     * default behavior in all cases. However, if the
     * <a href="cache/DefaultBundleCache.html"><tt>DefaulBundleCache</tt></a>
     * is used, then at a minimum a profile name or profile directory must
     * be specified.
     * </p>
     * <p>
     * The following configuration properties can be specified:
     * </p>
     * <ul>
     *   <li><tt>felix.auto.install.&lt;n&gt;</tt> - Space-delimited list of
     *       bundles to automatically install into start level <tt>n</tt> when
     *       the framework is started. Append a specific start level to this
     *       property name to assign the bundles' start level
     *       (e.g., <tt>felix.auto.install.2</tt>).
     *   </li>
     *   <li><tt>felix.auto.start.&lt;n&gt;</tt> - Space-delimited list of
     *       bundles to automatically install and start into start level
     *       <tt>n</tt> when the framework is started. Append a
     *       specific start level to this property name to assign the
     *       bundles' start level(e.g., <tt>felix.auto.start.2</tt>).
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
     * Framework properties are somewhat misnamed, since they are not used by
     * the framework, but by bundles via <tt>BundleContext.getProperty()</tt>.
     * Please refer to bundle documentation of your specific bundle for any
     * available properties.
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
     * @param configMutable An object for obtaining configuration properties,
     *        may be <tt>null</tt>.
     * @param frameworkProps An object for obtaining framework properties,
     *        may be <tt>null</tt>.
     * @param activatorList A list of System Bundle activators.
    **/
    public synchronized void start(
        MutablePropertyResolver configMutable,
        List activatorList)
    {
        if (m_frameworkStatus != INITIAL_STATUS)
        {
            throw new IllegalStateException("Invalid framework status: " + m_frameworkStatus);
        }

        // The framework is now in its startup sequence.
        m_frameworkStatus = STARTING_STATUS;

        // Initialize member variables.
        m_factory = null;
        m_configMutable = (configMutable == null)
            ? new MutablePropertyResolverImpl(new StringMap(false)) : configMutable;
        m_activeStartLevel = FelixConstants.FRAMEWORK_INACTIVE_STARTLEVEL;
        m_installRequestMap = new HashMap();
        m_installedBundleMap = new HashMap();
        m_uninstalledBundles = null;
        m_cache = null;
        m_nextId = 1L;
        m_dispatchQueue = null;
        m_bundleStreamHandler = new URLHandlersBundleStreamHandler(this);
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

        try
        {
            m_cache = new BundleCache(m_config, m_logger);
        }
        catch (Exception ex)
        {
            System.err.println("Error creating bundle cache:");
            ex.printStackTrace();

            // Only shutdown the JVM if the framework is running stand-alone.
            String embedded = m_config.get(
                FelixConstants.EMBEDDED_EXECUTION_PROP);
            boolean isEmbedded = (embedded == null)
                ? false : embedded.equals("true");
            if (!isEmbedded)
            {
                System.exit(-1);
            }
            else
            {
                throw new RuntimeException(ex.toString());
            }
        }

        // Create search policy for module loader.
        m_policyCore = new R4SearchPolicyCore(m_logger, m_config);

        // Add a resolver listener to the search policy
        // so that we will be notified when modules are resolved
        // in order to update the bundle state.
        m_policyCore.addResolverListener(new ResolveListener() {
            public void moduleResolved(ModuleEvent event)
            {
                BundleImpl bundle = null;
                try
                {
                    long id = Util.getBundleIdFromModuleId(
                        event.getModule().getId());
                    if (id >= 0)
                    {
                        // Update the bundle's state to resolved when the
                        // current module is resolved; just ignore resolve
                        // events for older revisions since this only occurs
                        // when an update is done on an unresolved bundle
                        // and there was no refresh performed.
                        bundle = (BundleImpl) getBundle(id);

                        // Lock the bundle first.
                        try
                        {
                            acquireBundleLock(bundle);
                            if (bundle.getInfo().getCurrentModule() == event.getModule())
                            {
                                bundle.getInfo().setState(Bundle.RESOLVED);
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

        m_factory = new ModuleFactoryImpl(m_logger);
        m_policyCore.setModuleFactory(m_factory);

        // Initialize dispatch queue.
        m_dispatchQueue = new FelixDispatchQueue(m_logger);

        // Initialize framework properties.
        initializeFrameworkProperties();

        // Before we reload any cached bundles, let's create a system
        // bundle that is responsible for providing specific container
        // related services.
        SystemBundle systembundle = null;
        try
        {
            // Create a simple bundle info for the system bundle.
            BundleInfo info = new BundleInfo(
                m_logger, new SystemBundleArchive(), null);
            systembundle = new SystemBundle(this, info, activatorList);
            systembundle.getInfo().addModule(m_factory.createModule("0"));
            systembundle.getContentLoader().setSearchPolicy(
                new R4SearchPolicy(
                    m_policyCore, systembundle.getInfo().getCurrentModule()));
            m_factory.setContentLoader(
                systembundle.getInfo().getCurrentModule(),
                systembundle.getContentLoader());
            m_policyCore.setExports(
                systembundle.getInfo().getCurrentModule(), systembundle.getExports());

            m_installedBundleMap.put(
                systembundle.getInfo().getLocation(), systembundle);

            // Manually resolve the System Bundle, which will cause its
            // state to be set to RESOLVED.
            try
            {
                m_policyCore.resolve(systembundle.getInfo().getCurrentModule());
            }
            catch (ResolveException ex)
            {
                // This should never happen.
                throw new BundleException(
                        "Unresolved package in System Bundle:"
                        + ex.getPackage());
            }

            // Start the system bundle; this will set its state
            // to STARTING, we must set its state to ACTIVE after
            // all bundles are restarted below according to the spec.
            systembundle.start();
        }
        catch (Exception ex)
        {
            m_factory = null;
            DispatchQueue.shutdown();
            m_logger.log(Logger.LOG_ERROR, "Unable to start system bundle.", ex);
            throw new RuntimeException("Unable to start system bundle.");
        }
        
        // Reload and cached bundles.
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
                "Unable to list saved bundles: " + ex, ex);
            archives = null;
        }

        BundleImpl bundle = null;

        // Now install all cached bundles.
        for (int i = 0; (archives != null) && (i < archives.length); i++)
        {
            try
            {
                // Make sure our id generator is not going to overlap.
                // TODO: This is not correct since it may lead to re-used
                // ids, which is not okay according to OSGi.
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
                    bundle = (BundleImpl) installBundle(
                        archives[i].getId(), archives[i].getLocation(), null);
                }
            }
            catch (Exception ex)
            {
                fireFrameworkEvent(FrameworkEvent.ERROR, bundle, ex);
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
                // TODO: Perhaps we should remove the cached bundle?
            }
        }

        // Get the framework's default start level.
        int startLevel = FelixConstants.FRAMEWORK_DEFAULT_STARTLEVEL;
        String s = m_config.get(FelixConstants.FRAMEWORK_STARTLEVEL_PROP);
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

        // Load bundles from auto-install and auto-start properties;
        processAutoProperties();

        // Set the start level using the start level service;
        // this ensures that all start level requests are
        // serialized.
        // NOTE: There is potentially a specification compliance
        // issue here, since the start level request is asynchronous;
        // this means that the framework will fire its STARTED event
        // before all bundles have officially been restarted and it
        // is not clear if this is really an issue or not.
        try
        {
            StartLevel sl = (StartLevel) getService(
                getBundle(0),
                getServiceReferences((BundleImpl) getBundle(0), StartLevel.class.getName(), null)[0]);
            sl.setStartLevel(startLevel);
        }
        catch (InvalidSyntaxException ex)
        {
            // Should never happen.
        }

        // The framework is now running.
        m_frameworkStatus = RUNNING_STATUS;

        // Set the system bundle state to ACTIVE.
        systembundle.getInfo().setState(Bundle.ACTIVE);

        // Fire started event for system bundle.
        fireBundleEvent(BundleEvent.STARTED, systembundle);

        // Send a framework event to indicate the framework has started.
        fireFrameworkEvent(FrameworkEvent.STARTED, getBundle(0), null);
    }

    /**
     * This method cleanly shuts down the framework, it must be called at the
     * end of a session in order to shutdown all active bundles.
    **/
    public synchronized void shutdown()
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        // Change framework status from running to stopping.
        // If framework is not running, then just return.
        if (m_frameworkStatus != RUNNING_STATUS)
        {
            return;
        }

        // The framework is now in its shutdown sequence.
        m_frameworkStatus = STOPPING_STATUS;

        // Use the start level service to set the start level to zero
        // in order to stop all bundles in the framework. Since framework
        // shutdown happens on its own thread, we can wait for the start
        // level service to finish before proceeding by calling the
        // non-spec setStartLevelAndWait() method.
        try
        {
            StartLevelImpl sl = (StartLevelImpl) getService(
                getBundle(0),
                getServiceReferences((BundleImpl) getBundle(0), StartLevel.class.getName(), null)[0]);
            sl.setStartLevelAndWait(0);
        }
        catch (InvalidSyntaxException ex)
        {
            // Should never happen.
        }

        // Just like initialize() called the system bundle's start()
        // method, we must call its stop() method here so that it
        // can perform any necessary clean up.
        try
        {
            getBundle(0).stop();
        }
        catch (Exception ex)
        {
            fireFrameworkEvent(FrameworkEvent.ERROR, getBundle(0), ex);
            m_logger.log(Logger.LOG_ERROR, "Error stopping system bundle.", ex);
        }

        // Loop through all bundles and update any updated bundles.
        Bundle[] bundles = getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            BundleImpl bundle = (BundleImpl) bundles[i];
            if (bundle.getInfo().isRemovalPending())
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

        // Remove any uninstalled bundles.
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
        DispatchQueue.shutdown();

        // The framework is no longer in a usable state.
        m_frameworkStatus = INITIAL_STATUS;

        // Remove all bundles from the module factory so that any
        // open resources will be closed.
        bundles = getBundles();
        for (int i = 0; i < bundles.length; i++)
        {
            BundleImpl bundle = (BundleImpl) bundles[i];
            try
            {
                IModule[] modules = bundle.getInfo().getModules();
                for (int j = 0; j < modules.length; j++)
                {
                    m_factory.removeModule(modules[j]);
                }
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR,
                   "Unable to clean up " + bundle.getInfo().getLocation(), ex);
            }
        }
    }

    public int getStatus()
    {
        return m_frameworkStatus;
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
                        BundleImpl b1 = (BundleImpl) o1;
                        BundleImpl b2 = (BundleImpl) o2;
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
                        BundleImpl b1 = (BundleImpl) o1;
                        BundleImpl b2 = (BundleImpl) o2;
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
            BundleImpl impl = (BundleImpl) bundles[i];

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

        fireFrameworkEvent(FrameworkEvent.STARTLEVEL_CHANGED, getBundle(0), null);
    }

    /**
     * Returns the start level into which newly installed bundles will
     * be placed by default; this method implements functionality for
     * the Start Level service.
     * @return The default start level for newly installed bundles.
    **/
    protected int getInitialBundleStartLevel()
    {
        String s = m_config.get(FelixConstants.BUNDLE_STARTLEVEL_PROP);

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
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        if (startLevel <= 0)
        {
            throw new IllegalArgumentException(
                "Initial start level must be greater than zero.");
        }

        m_configMutable.put(
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

        return ((BundleImpl) bundle).getInfo().getStartLevel(getInitialBundleStartLevel());
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
        acquireBundleLock((BundleImpl) bundle);
        
        Throwable rethrow = null;

        try
        {
            if (bundle.getState() == Bundle.UNINSTALLED)
            {
                throw new IllegalArgumentException("Bundle is uninstalled.");
            }

            if (startLevel >= 1)
            {
                BundleImpl impl = (BundleImpl) bundle;
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
            releaseBundleLock((BundleImpl) bundle);
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

        return (((BundleImpl) bundle).getInfo().getPersistentState() == Bundle.ACTIVE);
    }

    //
    // Implementation of Bundle interface methods.
    //

    /**
     * Implementation for Bundle.getHeaders().
    **/
    protected Dictionary getBundleHeaders(BundleImpl bundle)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        return new MapToDictionary(bundle.getInfo().getCurrentHeader());
    }

    /**
     * Implementation for Bundle.getLocation().
    **/
    protected String getBundleLocation(BundleImpl bundle)
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }
        return bundle.getInfo().getLocation();
    }

    /**
     * Implementation for Bundle.getResource().
    **/
    protected URL getBundleResource(BundleImpl bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.checkPermission(
                    new AdminPermission(bundle, AdminPermission.RESOURCE));
            }
            catch (SecurityException ex)
            {
                // Spec says to return null if there is a security exception.
                return null;
            }
        }
        return bundle.getInfo().getCurrentModule().getResource(name);
    }

    /**
     * Implementation for Bundle.getEntry().
    **/
    protected URL getBundleEntry(BundleImpl bundle, String name)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.checkPermission(
                    new AdminPermission(bundle, AdminPermission.RESOURCE));
            }
            catch (SecurityException ex)
            {
                // Spec says to return null if there is a security exception.
                return null;
            }
        }
        return ((ContentLoaderImpl) bundle.getInfo().getCurrentModule()
            .getContentLoader()).getResourceFromContent(name);
    }

    /**
     * Implementation for Bundle.getEntryPaths().
    **/
    protected Enumeration getBundleEntryPaths(BundleImpl bundle, String path)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }
        else if (System.getSecurityManager() != null)
        {
            try
            {
                AccessController.checkPermission(
                    new AdminPermission(bundle, AdminPermission.RESOURCE));
            }
            catch (SecurityException ex)
            {
                // Spec says to return null if there is a security exception.
                return null;
            }
        }
        // Strip leading '/' if present.
        if ((path.length() > 0) && (path.charAt(0) == '/'))
        {
            path = path.substring(1);
        }
        return bundle.getInfo().getCurrentModule()
            .getContentLoader().getContent().getEntryPaths(path);
    }

    protected ServiceReference[] getBundleRegisteredServices(BundleImpl bundle)
    {
        if (bundle.getInfo().getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
        }

        // Filter list of registered service references.
        ServiceReference[] refs = m_registry.getRegisteredServices(bundle);
        List list = new ArrayList();
        for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++)
        {
            // Check that the current security context has permission
            // to get at least one of the service interfaces; the
            // objectClass property of the service stores its service
            // interfaces.
            boolean hasPermission = false;
            if (System.getSecurityManager() != null)
            {
                String[] objectClass = (String[])
                    refs[refIdx].getProperty(Constants.OBJECTCLASS);
                if (objectClass == null)
                {
                    return null;
                }
                for (int ifcIdx = 0;
                    !hasPermission && (ifcIdx < objectClass.length);
                    ifcIdx++)
                {
                    try
                    {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[ifcIdx], ServicePermission.GET);
                        AccessController.checkPermission(perm);
                        hasPermission = true;
                    }
                    catch (Exception ex)
                    {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            if (hasPermission)
            {
                list.add(refs[refIdx]);
            }
        }

        if (list.size() > 0)
        {
            return (ServiceReference[])
                list.toArray(new ServiceReference[list.size()]);
        }

        return null;
    }

    protected ServiceReference[] getBundleServicesInUse(Bundle bundle)
    {
        // Filter list of "in use" service references.
        ServiceReference[] refs = m_registry.getServicesInUse(bundle);
        List list = new ArrayList();
        for (int refIdx = 0; (refs != null) && (refIdx < refs.length); refIdx++)
        {
            // Check that the current security context has permission
            // to get at least one of the service interfaces; the
            // objectClass property of the service stores its service
            // interfaces.
            boolean hasPermission = false;
            if (System.getSecurityManager() != null)
            {
                String[] objectClass = (String[])
                    refs[refIdx].getProperty(Constants.OBJECTCLASS);
                if (objectClass == null)
                {
                    return null;
                }
                for (int ifcIdx = 0;
                    !hasPermission && (ifcIdx < objectClass.length);
                    ifcIdx++)
                {
                    try
                    {
                        ServicePermission perm =
                            new ServicePermission(
                                objectClass[ifcIdx], ServicePermission.GET);
                        AccessController.checkPermission(perm);
                        hasPermission = true;
                    }
                    catch (Exception ex)
                    {
                    }
                }
            }
            else
            {
                hasPermission = true;
            }

            if (hasPermission)
            {
                list.add(refs[refIdx]);
            }
        }

        if (list.size() > 0)
        {
            return (ServiceReference[])
                list.toArray(new ServiceReference[list.size()]);
        }

        return null;
    }

    protected boolean bundleHasPermission(BundleImpl bundle, Object obj)
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
                    ? java.security.Policy.getPolicy().getPermissions(
                        new java.security.CodeSource(
                            new java.net.URL(bundle.getInfo().getLocation()),
                            (java.security.cert.Certificate[]) null))
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
    protected Class loadBundleClass(BundleImpl bundle, String name) throws ClassNotFoundException
    {
        Class clazz = bundle.getInfo().getCurrentModule().getClass(name);
        if (clazz == null)
        {
            // Throw exception.
            ClassNotFoundException ex = new ClassNotFoundException(name);

            // The spec says we must fire a framework error.
            fireFrameworkEvent(
                FrameworkEvent.ERROR, bundle,
                new BundleException(ex.getMessage()));

            throw ex;
        }
        return clazz;
    }

    /**
     * Implementation for Bundle.start().
    **/
    protected void startBundle(BundleImpl bundle, boolean record)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

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

    private void _startBundle(BundleImpl bundle, boolean record)
        throws BundleException
    {
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
            case Bundle.RESOLVED:
                info.setState(Bundle.STARTING);
        }

        try
        {
            // Set the bundle's activator.
            bundle.getInfo().setActivator(createBundleActivator(bundle.getInfo()));

            // Activate the bundle if it has an activator.
            if (bundle.getInfo().getActivator() != null)
            {
                if (info.getContext() == null)
                {
                    info.setContext(new BundleContextImpl(this, bundle));
                }

                if (System.getSecurityManager() != null)
                {
//                    m_startStopPrivileged.setAction(StartStopPrivileged.START_ACTION);
//                    m_startStopPrivileged.setBundle(bundle);
//                    AccessController.doPrivileged(m_startStopPrivileged);
                }
                else
                {
                    info.getActivator().start(info.getContext());
                }
            }

            info.setState(Bundle.ACTIVE);

            // TODO: CONCURRENCY - Reconsider firing event outside of the
            // bundle lock.

            fireBundleEvent(BundleEvent.STARTED, bundle);
        }
        catch (Throwable th)
        {
            // If there was an error starting the bundle,
            // then reset its state to RESOLVED.
            info.setState(Bundle.RESOLVED);

            // Clean up the bundle context, if necessary.
            if (info.getContext() != null)
            {
                ((BundleContextImpl) info.getContext()).invalidate();
                info.setContext(null);
            }

            // Unregister any services offered by this bundle.
            m_registry.unregisterServices(bundle);

            // Release any services being used by this bundle.
            m_registry.ungetServices(bundle);

            // Remove any listeners registered by this bundle.
            removeListeners(bundle);

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
            // Convert a privileged action exception to the
            // nested exception.
            else if (th instanceof PrivilegedActionException)
            {
                th = ((PrivilegedActionException) th).getException();
            }

            // Rethrow all other exceptions as a BundleException.
            throw new BundleException("Activator start error.", th);
        }
    }

    protected void _resolveBundle(BundleImpl bundle)
        throws BundleException
    {
        // If a security manager is installed, then check for permission
        // to import the necessary packages.
        if (System.getSecurityManager() != null)
        {
            URL url = null;
            try
            {
                url = new URL(bundle.getInfo().getLocation());
            }
            catch (MalformedURLException ex)
            {
                throw new BundleException("Cannot resolve, bad URL "
                    + bundle.getInfo().getLocation());
            }

//            try
//            {
//                AccessController.doPrivileged(new CheckImportsPrivileged(url, bundle));
//            }
//            catch (PrivilegedActionException ex)
//            {
//                Exception thrown = ((PrivilegedActionException) ex).getException();
//                if (thrown instanceof AccessControlException)
//                {
//                    throw (AccessControlException) thrown;
//                }
//                else
//                {
//                    throw new BundleException("Problem resolving: " + ex);
//                }
//            }
        }

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
                    + ": " + ex.getPackage());
            }
            else
            {
                throw new BundleException(ex.getMessage());
            }
        }

        bundle.getInfo().setState(Bundle.RESOLVED);
    }

    protected void updateBundle(BundleImpl bundle, InputStream is)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

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

    protected void _updateBundle(BundleImpl bundle, InputStream is)
        throws BundleException
    {
        // We guarantee to close the input stream, so put it in a
        // finally clause.
    
        try
        {
            // Variable to indicate whether bundle is active or not.
            Exception rethrow = null;

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
                IModule module = createModule(
                    info.getBundleId(),
                    archive.getRevisionCount() - 1,
                    info.getCurrentHeader());
                // Add module to bundle info.
                info.addModule(module);
            }
            catch (Exception ex)
            {
                m_logger.log(Logger.LOG_ERROR, "Unable to update the bundle.", ex);
                rethrow = ex;
            }

            info.setState(Bundle.INSTALLED);
            info.setLastModified(System.currentTimeMillis());

            // Mark as needing a refresh.
            info.setRemovalPending();
    
            // Fire updated event if successful.
            if (rethrow == null)
            {
                fireBundleEvent(BundleEvent.UPDATED, bundle);
            }
    
            // Restart bundle, but do not change the persistent state.
            // This will not start the bundle if it was not previously
            // active.
            startBundle(bundle, false);

            // If update failed, rethrow exception.
            if (rethrow != null)
            {
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

    protected void stopBundle(BundleImpl bundle, boolean record)
        throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

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

    private void _stopBundle(BundleImpl bundle, boolean record)
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
        }
            
        try
        {
            if (bundle.getInfo().getActivator() != null)
            {
                if (System.getSecurityManager() != null)
                {
//                    m_startStopPrivileged.setAction(StartStopPrivileged.STOP_ACTION);
//                    m_startStopPrivileged.setBundle(bundle);
//                    AccessController.doPrivileged(m_startStopPrivileged);
                }
                else
                {
                    info.getActivator().stop(info.getContext());
                }
            }
        
            // Try to save the activator in the cache.
            // NOTE: This is non-standard OSGi behavior and only
            // occurs if strictness is disabled.
            String strict = m_config.get(FelixConstants.STRICT_OSGI_PROP);
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
                    // TODO: Perhaps we should handle this some other way?
                }
            }
        }
        catch (Throwable th)
        {
            m_logger.log(Logger.LOG_ERROR, "Error stopping bundle.", th);
            rethrow = th;
        }
                  
        // Clean up the bundle context, if necessary.
        if (info.getContext() != null)
        {
            ((BundleContextImpl) info.getContext()).invalidate();
            info.setContext(null);
        }

        // Unregister any services offered by this bundle.
        m_registry.unregisterServices(bundle);
        
        // Release any services being used by this bundle.
        m_registry.ungetServices(bundle);
        
        // The spec says that we must remove all event
        // listeners for a bundle when it is stopped.
        removeListeners(bundle);
        
        info.setState(Bundle.RESOLVED);
        fireBundleEvent(BundleEvent.STOPPED, bundle);
        
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
            else if (rethrow instanceof PrivilegedActionException)
            {
                rethrow = ((PrivilegedActionException) rethrow).getException();
            }
    
            // Rethrow all other exceptions as a BundleException.
            throw new BundleException("Activator stop error.", rethrow);
        }
    }

    protected void uninstallBundle(BundleImpl bundle) throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

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

    private void _uninstallBundle(BundleImpl bundle) throws BundleException
    {
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        BundleInfo info = bundle.getInfo();
        if (info.getState() == Bundle.UNINSTALLED)
        {
            throw new IllegalStateException("The bundle is uninstalled.");
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
        BundleImpl target = null;
        synchronized (m_installedBundleLock_Priority2)
        {
            target = (BundleImpl) m_installedBundleMap.remove(info.getLocation());
        }

        // Finally, put the uninstalled bundle into the
        // uninstalled list for subsequent refreshing.
        if (target != null)
        {
            // Set the bundle's persistent state to uninstalled.
            target.getInfo().setPersistentStateUninstalled();

            // Mark bundle for removal.
            target.getInfo().setRemovalPending();

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
    protected String getProperty(String key)
    {
        // First, check the config properties.
        String val = (String) m_config.get(key);
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
        if (System.getSecurityManager() != null)
        {
            AccessController.checkPermission(m_adminPerm);
        }

        BundleImpl bundle = null;

        // Acquire an install lock.
        acquireInstallLock(location);

        try
        {
            // Check to see if the framework is still running;
            if ((getStatus() == Felix.STOPPING_STATUS) ||
                (getStatus() == Felix.INITIAL_STATUS))
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
                bundle = new BundleImpl(this, createBundleInfo(archive));
            }
            catch (Exception ex)
            {
                // If the bundle is new, then remove it from the cache.
                // TODO: Perhaps it should be removed if it is not new too.
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
            BundleImpl bundle = null;

            for (Iterator i = m_installedBundleMap.values().iterator(); i.hasNext(); )
            {
                bundle = (BundleImpl) i.next();
                if (bundle.getInfo().getBundleId() == id)
                {
                    return bundle;
                }
            }
        }

        synchronized (m_uninstalledBundlesLock_Priority3)
        {
            for (int i = 0; i < m_uninstalledBundles.length; i++)
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
        // The spec says do nothing if the listener is
        // already registered.
        BundleListenerWrapper old = (BundleListenerWrapper)
            m_dispatchQueue.getListener(BundleListener.class, l);
        if (old == null)
        {
            l = new BundleListenerWrapper(bundle, l);
            m_dispatchQueue.addListener(BundleListener.class, l);
        }
    }

    protected void removeBundleListener(BundleListener l)
    {
        m_dispatchQueue.removeListener(BundleListener.class, l);
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
        // The spec says if the listener is already registered,
        // then replace filter.
        ServiceListenerWrapper old = (ServiceListenerWrapper)
            m_dispatchQueue.getListener(ServiceListener.class, l);
        if (old != null)
        {
            old.setFilter((f == null) ? null : new FilterImpl(m_logger, f));
        }
        else
        {
            l = new ServiceListenerWrapper(
                bundle, l, (f == null) ? null : new FilterImpl(m_logger, f));
            m_dispatchQueue.addListener(ServiceListener.class, l);
        }
    }

    /**
     * Implementation for BundleContext.removeServiceListener().
     * Removes service listeners from the listener list.
     *
     * @param l The service listener to remove from the listener list.
    **/
    protected void removeServiceListener(ServiceListener l)
    {
        m_dispatchQueue.removeListener(ServiceListener.class, l);
    }

    protected void addFrameworkListener(Bundle bundle, FrameworkListener l)
    {
        // The spec says do nothing if the listener is
        // already registered.
        FrameworkListenerWrapper old = (FrameworkListenerWrapper)
            m_dispatchQueue.getListener(FrameworkListener.class, l);
        if (old == null)
        {
            l = new FrameworkListenerWrapper(bundle, l);
            m_dispatchQueue.addListener(FrameworkListener.class, l);
        }
    }

    protected void removeFrameworkListener(FrameworkListener l)
    {
        m_dispatchQueue.removeListener(FrameworkListener.class, l);
    }

    /**
     * Remove all of the specified bundle's event listeners from
     * the framework.
     * @param bundle The bundle whose listeners are to be removed.
    **/
    private void removeListeners(Bundle bundle)
    {
        if (bundle == null)
        {
            return;
        }

        // Remove all listeners associated with the supplied bundle;
        // it is only possible to know the bundle associated with a
        // listener if the listener was wrapper by a ListenerWrapper,
        // so look for those.
        Object[] listeners = m_dispatchQueue.getListeners();
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            // Check for listener wrappers and then compare the bundle.
            if (listeners[i + 1] instanceof ListenerWrapper)
            {
                ListenerWrapper lw = (ListenerWrapper) listeners[i + 1];
                if ((lw.getBundle() != null) && (lw.getBundle().equals(bundle)))
                {
                    m_dispatchQueue.removeListener(
                        (Class) listeners[i], (EventListener) listeners[i+1]);
                }
            }
        }
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

        // Check for permission to register all passed in interface names.
        if (System.getSecurityManager() != null)
        {
            for (int i = 0; i < classNames.length; i++)
            {
                ServicePermission perm = new ServicePermission(
                    classNames[i], ServicePermission.REGISTER);
                AccessController.checkPermission(perm);
            }
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

    protected ServiceReference[] getServiceReferences(
        BundleImpl bundle, String className, String expr)
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

        // The returned reference list must be filtered for two cases:
        // 1) The requesting bundle may not be wired to the same class
        //    as the providing bundle (i.e, different versions), so filter
        //    any services for which the requesting bundle might get a
        //    class cast exception.
        // 2) Security is enabled and the requesting bundle does not have
        //    permission access the service.
        for (int refIdx = 0; (refList != null) && (refIdx < refList.size()); refIdx++)
        {
            // Get the current service reference.
            ServiceReference ref = (ServiceReference) refList.get(refIdx);

            // Get the service's objectClass property.
            String[] objectClass = (String[]) ref.getProperty(FelixConstants.OBJECTCLASS);

            // Boolean flag.
            boolean allow = false;

            // Filter the service reference if the requesting bundle
            // does not have permission.
            if (System.getSecurityManager() != null)
            {
                for (int classIdx = 0;
                    !allow && (classIdx < objectClass.length);
                    classIdx++)
                {
                    try
                    {
                        ServicePermission perm = new ServicePermission(
                            objectClass[classIdx], ServicePermission.GET);
                        AccessController.checkPermission(perm);
                        // The bundle only needs permission for one
                        // of the service interfaces, so break out
                        // of the loop when permission is granted.
                        allow = true;
                    }
                    catch (Exception ex)
                    {
                        // We do not throw this exception since the bundle
                        // is not supposed to know about the service at all
                        // if it does not have permission.
                        m_logger.log(Logger.LOG_ERROR, ex.getMessage());
                    }
                }
                
                if (!allow)
                {
                    refList.remove(refIdx);
                    refIdx--;
                    continue;
                }
            }

            // Now check for castability.
            if (!isServiceAssignable(bundle, ref))
            {
                refList.remove(refIdx);
                refIdx--;
            }
        }

        if (refList.size() > 0)
        {
            return (ServiceReference[]) refList.toArray(new ServiceReference[refList.size()]);
        }

        return null;
    }

    /**
     * This method determines if the requesting bundle is able to cast
     * the specified service reference based on class visibility rules
     * of the underlying modules.
     * @param requester The bundle requesting the service.
     * @param ref The service in question.
     * @return <tt>true</tt> if the requesting bundle is able to case
     *         the service object to a known type.
    **/
    protected boolean isServiceAssignable(BundleImpl requester, ServiceReference ref)
    {
        // Boolean flag.
        boolean allow = true;
        // Get the service's objectClass property.
        String[] objectClass = (String[]) ref.getProperty(FelixConstants.OBJECTCLASS);

        // The the service reference is not assignable when the requesting
        // bundle is wired to a different version of the service object.
        // NOTE: We are pessimistic here, if any class in the service's
        // objectClass is not usable by the requesting bundle, then we
        // disallow the service reference.
        for (int classIdx = 0; (allow) && (classIdx < objectClass.length); classIdx++)
        {
            if (!ref.isAssignableTo(requester, objectClass[classIdx]))
            {
                allow = false;
            }
        }
        return allow;
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

        boolean hasPermission = false;
        if (System.getSecurityManager() != null)
        {
            for (int i = 0;
                !hasPermission && (i < objectClass.length);
                i++)
            {
                try
                {
                    ServicePermission perm =
                        new ServicePermission(
                            objectClass[i], ServicePermission.GET);
                    AccessController.checkPermission(perm);
                    hasPermission = true;
                }
                catch (Exception ex)
                {
                }
            }
        }
        else
        {
            hasPermission = true;
        }

        // If the bundle does not permission to access the service,
        // then return null.
        if (!hasPermission)
        {
            return null;
        }

        return m_registry.getService(bundle, ref);
    }

    protected boolean ungetService(Bundle bundle, ServiceReference ref)
    {
        return m_registry.ungetService(bundle, ref);
    }

    protected File getDataFile(BundleImpl bundle, String s)
    {
        // The spec says to throw an error if the bundle
        // is stopped, which I assume means not active,
        // starting, or stopping.
        if ((bundle.getInfo().getState() != Bundle.ACTIVE) &&
            (bundle.getInfo().getState() != Bundle.STARTING) &&
            (bundle.getInfo().getState() != Bundle.STOPPING))
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
        return null;
    }

    /**
     * Returns the exported packages associated with the specified
     * package name. This is used by the PackageAdmin service
     * implementation.
     *
     * @param name The name of the exported package to find.
     * @return The exported package or null if no matching package was found.
    **/
    protected ExportedPackage[] getExportedPackages(String name)
    {
        // First, get all exporters of the package.
        ExportedPackage[] pkgs = null;
        IModule[] exporters = m_policyCore.getInUseExporters(new R4Import(name, null, null));
        if (exporters != null)
        {
            pkgs = new ExportedPackage[exporters.length];
            for (int pkgIdx = 0; pkgIdx < pkgs.length; pkgIdx++)
            {
                // Get the bundle associated with the current exporting module.
                BundleImpl bundle = (BundleImpl) getBundle(
                    Util.getBundleIdFromModuleId(exporters[pkgIdx].getId()));

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
                    R4Export export = Util.getExportPackage(modules[modIdx], name);
                    if (export != null)
                    {
                        pkgs[pkgIdx] =
                            new ExportedPackageImpl(
                                this, bundle, name, export.getVersion());
                    }
                }
            }
        }

        return pkgs;
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

        return (ExportedPackage[]) list.toArray(new ExportedPackage[list.size()]);
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
        IModule[] modules = bundle.getInfo().getModules();
        for (int modIdx = 0; modIdx < modules.length; modIdx++)
        {
            R4Export[] exports = m_policyCore.getExports(modules[modIdx]);
            if ((exports != null) && (exports.length > 0))
            {
                for (int expIdx = 0; expIdx < exports.length; expIdx++)
                {
                    // See if the target bundle's module is one of the
                    // "in use" exporters of the package.
                    IModule[] inUseModules =
                        m_policyCore.getInUseExporters(
                            new R4Import(exports[expIdx].getName(), null, null));
                    // Search through the current providers to find the target
                    // module.
                    for (int i = 0; (inUseModules != null) && (i < inUseModules.length); i++)
                    {
                        if (inUseModules[i] == modules[modIdx])
                        {
                            list.add(new ExportedPackageImpl(
                                this, bundle, exports[expIdx].getName(),
                                exports[expIdx].getVersion()));
                        }
                    }
                }
            }
        }
    }

    protected Bundle[] getImportingBundles(ExportedPackage ep)
    {
        // Get exporting bundle.
        BundleImpl exporter = (BundleImpl)
            ((ExportedPackage) ep).getExportingBundle();
        BundleInfo exporterInfo = exporter.getInfo();

        // Create list for storing importing bundles.
        List list = new ArrayList();
        Bundle[] bundles = getBundles();

        // Check all bundles to see who imports the package.
        for (int bundleIdx = 0; bundleIdx < bundles.length; bundleIdx++)
        {
            BundleImpl importer = (BundleImpl) bundles[bundleIdx];

            // Ignore the bundle if it imports from itself.
            if (exporter != importer)
            {
                // Check the import wires of all modules for all bundles.
                IModule[] modules = importer.getInfo().getModules();
                for (int modIdx = 0; modIdx < modules.length; modIdx++)
                {
                    R4Wire wire = Util.getWire(modules[modIdx], ep.getName());
    
                    // If the resolving module is associated with the
                    // exporting bundle, then add current bundle to
                    // import list.
                    if ((wire != null) && exporterInfo.hasModule(wire.getExportingModule()))
                    {
                        // Add the bundle to the list of importers.
                        list.add(bundles[bundleIdx]);
                        break;
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
        if (System.getSecurityManager() != null)
        {
// TODO: FW SECURITY - Perform proper security check.
            AccessController.checkPermission(m_adminPerm);
        }

        // Acquire locks for all bundles to be resolved.
        BundleImpl[] bundles = acquireBundleResolveLocks(targets);

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
        if (System.getSecurityManager() != null)
        {
// TODO: FW SECURITY - Perform proper security check.
            AccessController.checkPermission(m_adminPerm);
        }

        // Acquire locks for all impacted bundles.
        BundleImpl[] bundles = acquireBundleRefreshLocks(targets);

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
                    helpers[i] = new RefreshHelper(bundles[i]);
                }

                // Stop, purge or remove, and reinitialize all bundles first.
                for (int i = 0; i < helpers.length; i++)
                {
                    helpers[i].stop();
                    helpers[i].purgeOrRemove();
                    helpers[i].reinitialize();
                }

                // Then restart all bundles that were previously running.
                for (int i = 0; i < helpers.length; i++)
                {
                    helpers[i].restart();
                }
            }
        }
        finally
        {
            // Always release all bundle locks.
            releaseBundleLocks(bundles);
        }

        fireFrameworkEvent(FrameworkEvent.PACKAGES_REFRESHED, getBundle(0), null);
    }

    private void populateImportGraph(BundleImpl target, Map map)
    {
        // Get the exported packages for the specified bundle.
        ExportedPackage[] pkgs = getExportedPackages(target);

        for (int pkgIdx = 0; (pkgs != null) && (pkgIdx < pkgs.length); pkgIdx++)
        {
            // Get all imports of this package.
            Bundle[] importers = getImportingBundles(pkgs[pkgIdx]);

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
                        (BundleImpl) importers[impIdx], map);
                }
            }
        }
    }

    //
    // Miscellaneous private methods.
    //

    private BundleInfo createBundleInfo(BundleArchive archive)
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
            archive.getId(), archive.getRevisionCount() - 1, headerMap);

        // Finally, create an return the bundle info.
        return new BundleInfo(m_logger, archive, module);
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
    private IModule createModule(long targetId, int revision, Map headerMap)
        throws Exception
    {
        // Get the manifest version.
        String manifestVersion = (String) headerMap.get(FelixConstants.BUNDLE_MANIFESTVERSION);
        manifestVersion = (manifestVersion == null) ? "1" : manifestVersion;
        if (!manifestVersion.equals("1") && !manifestVersion.equals("2"))
        {
            throw new BundleException("Unknown 'Bundle-ManifestVersion' value: " + manifestVersion);
        }

        // Create map to check for duplicate imports/exports.
        Map dupeMap = new HashMap();

        // Get export packages from bundle manifest.
        R4Package[] pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.EXPORT_PACKAGE));

        // Create non-duplicated export array.
        dupeMap.clear();
        for (int i = 0; i < pkgs.length; i++)
        {
            if (dupeMap.get(pkgs[i].getName()) == null)
            {
                dupeMap.put(pkgs[i].getName(), new R4Export(pkgs[i]));
            }
            else
            {
                // TODO: FRAMEWORK - Exports can be duplicated, so fix this.
                m_logger.log(Logger.LOG_WARNING,
                    "Duplicate export - " + pkgs[i].getName());
            }
        }
        R4Export[] exports =
            (R4Export[]) dupeMap.values().toArray(new R4Export[dupeMap.size()]);

        // Get import packages from bundle manifest.
        pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.IMPORT_PACKAGE));

        // Create non-duplicated import array.
        dupeMap.clear();
        for (int i = 0; i < pkgs.length; i++)
        {
            if (dupeMap.get(pkgs[i].getName()) == null)
            {
                dupeMap.put(pkgs[i].getName(), new R4Import(pkgs[i]));
            }
            else
            {
                // TODO: FRAMEWORK - Determine if we should error here.
                m_logger.log(Logger.LOG_WARNING,
                    "Duplicate import - " + pkgs[i].getName());
            }
        }
        R4Import[] imports =
            (R4Import[]) dupeMap.values().toArray(new R4Import[dupeMap.size()]);

        // Get dynamic import packages from bundle manifest.
        pkgs = R4Package.parseImportOrExportHeader(
            (String) headerMap.get(Constants.DYNAMICIMPORT_PACKAGE));

        // Create non-duplicated dynamic import array.
        dupeMap.clear();
        for (int i = 0; i < pkgs.length; i++)
        {
            if (dupeMap.get(pkgs[i].getName()) == null)
            {
                dupeMap.put(pkgs[i].getName(), new R4Import(pkgs[i]));
            }
            else
            {
                // TODO: FRAMEWORK - Determine if we should error here.
                m_logger.log(Logger.LOG_WARNING,
                    "Duplicate import - " + pkgs[i].getName());
            }
        }
        R4Import[] dynamics =
            (R4Import[]) dupeMap.values().toArray(new R4Import[dupeMap.size()]);

        // Do some validity checking on bundles with R4 headers.
// TODO: FRAMEWORK - Perhaps these verifications and conversions can be done more efficiently.
        if (manifestVersion.equals("2"))
        {
            // Verify that bundle symbolic name is specified.
            String targetSym = (String) headerMap.get(FelixConstants.BUNDLE_SYMBOLICNAME);
            if (targetSym == null)
            {
                throw new BundleException("R4 bundle manifests must include bundle symbolic name.");
            }

            // Verify that the bundle symbolic name and version is unique.
            String targetVer = (String) headerMap.get(FelixConstants.BUNDLE_VERSION);
            targetVer = (targetVer == null) ? "0.0.0" : targetVer;
            Bundle[] bundles = getBundles();
            for (int i = 0; (bundles != null) && (i < bundles.length); i++)
            {
                long id = ((BundleImpl) bundles[i]).getInfo().getBundleId();
                String sym = (String) ((BundleImpl) bundles[i])
                    .getInfo().getCurrentHeader().get(Constants.BUNDLE_SYMBOLICNAME);
                String ver = (String) ((BundleImpl) bundles[i])
                    .getInfo().getCurrentHeader().get(Constants.BUNDLE_VERSION);
                ver = (ver == null) ? "0.0.0" : ver;
                if (targetSym.equals(sym) && targetVer.equals(ver) && (targetId != id))
                {
                    throw new BundleException("Bundle symbolic name and version are not unique.");
                }
            }

            // Need to add symbolic name and bundle version to all R4 exports.
            for (int i = 0; (exports != null) && (i < exports.length); i++)
            {
                R4Attribute[] attrs = exports[i].getAttributes();
                R4Attribute[] newAttrs = new R4Attribute[attrs.length + 2];
                System.arraycopy(attrs, 0, newAttrs, 0, attrs.length);
                newAttrs[attrs.length] = new R4Attribute(
                    Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, targetSym, false);
                newAttrs[attrs.length + 1] = new R4Attribute(
                    Constants.BUNDLE_VERSION_ATTRIBUTE, targetVer, false);
                exports[i] = new R4Export(
                    exports[i].getName(), exports[i].getDirectives(), newAttrs);
            }
        }
        // Do some validity checking and conversion on bundles with R3 headers.
        else if (manifestVersion.equals("1"))
        {
            // Check to make sure that R3 bundles have only specified
            // the 'specification-version' attribute and no directives
            // on their exports.
            for (int i = 0; (exports != null) && (i < exports.length); i++)
            {
                if (exports[i].getDirectives().length != 0)
                {
                    throw new BundleException("R3 exports cannot contain directives.");
                }
                // NOTE: This is checking for "version" rather than "specification-version"
                // because the package class normalizes to "version" to avoid having
                // future special cases. This could be changed if more strict behavior
                // is required.
                if ((exports[i].getAttributes().length > 1) ||
                    ((exports[i].getAttributes().length == 1) &&
                        (!exports[i].getAttributes()[0].getName().equals(FelixConstants.VERSION_ATTRIBUTE))))
                {
                    throw new BundleException(
                        "Export does not conform to R3 syntax: " + exports[i]);
                }
            }
            
            // Check to make sure that R3 bundles have only specified
            // the 'specification-version' attribute and no directives
            // on their imports.
            for (int i = 0; (imports != null) && (i < imports.length); i++)
            {
                if (imports[i].getDirectives().length != 0)
                {
                    throw new BundleException("R3 imports cannot contain directives.");
                }
                // NOTE: This is checking for "version" rather than "specification-version"
                // because the package class normalizes to "version" to avoid having
                // future special cases. This could be changed if more strict behavior
                // is required.
                if ((imports[i].getVersionHigh() != null) ||
                    (imports[i].getAttributes().length > 1) ||
                    ((imports[i].getAttributes().length == 1) &&
                        (!imports[i].getAttributes()[0].getName().equals(FelixConstants.VERSION_ATTRIBUTE))))
                {
                    throw new BundleException(
                        "Import does not conform to R3 syntax: " + imports[i]);
                }
            }

            // Since all R3 exports imply an import, add a corresponding
            // import for each existing export. Create non-duplicated import array.
            dupeMap.clear();
            // Add existing imports.
            for (int i = 0; i < imports.length; i++)
            {
                dupeMap.put(imports[i].getName(), imports[i]);
            }
            // Add import for each export.
            for (int i = 0; i < exports.length; i++)
            {
                if (dupeMap.get(exports[i].getName()) == null)
                {
                    dupeMap.put(exports[i].getName(), new R4Import(exports[i]));
                }
            }
            imports =
                (R4Import[]) dupeMap.values().toArray(new R4Import[dupeMap.size()]);

            // Add a "uses" directive onto each export of R3 bundles
            // that references every other import (which will include
            // exports, since export implies import); this is
            // necessary since R3 bundles assumed a single class space,
            // but R4 allows for multiple class spaces.
            String usesValue = "";
            for (int i = 0; (imports != null) && (i < imports.length); i++)
            {
                usesValue = usesValue
                    + ((usesValue.length() > 0) ? "," : "")
                    + imports[i].getName();
            }
            R4Directive uses = new R4Directive(
                FelixConstants.USES_DIRECTIVE, usesValue);
            for (int i = 0; (exports != null) && (i < exports.length); i++)
            {
                exports[i] = new R4Export(
                    exports[i].getName(),
                    new R4Directive[] { uses },
                    exports[i].getAttributes());
            }

            // Check to make sure that R3 bundles have no attributes or
            // directives on their dynamic imports.
            for (int i = 0; (dynamics != null) && (i < dynamics.length); i++)
            {
                if (dynamics[i].getDirectives().length != 0)
                {
                    throw new BundleException("R3 dynamic imports cannot contain directives.");
                }
                if (dynamics[i].getAttributes().length != 0)
                {
                    throw new BundleException("R3 dynamic imports cannot contain attributes.");
                }
            }
        }

        // Get native library entry names for module library sources.
        R4LibraryHeader[] libraryHeaders =
            Util.parseLibraryStrings(
                m_logger,
                Util.parseDelimitedString(
                    (String) headerMap.get(Constants.BUNDLE_NATIVECODE), ","));
        R4Library[] libraries = new R4Library[libraryHeaders.length];
        for (int i = 0; i < libraries.length; i++)
        {
            libraries[i] = new R4Library(
                m_logger, m_cache, targetId, revision,
                getProperty(Constants.FRAMEWORK_OS_NAME),
                getProperty(Constants.FRAMEWORK_PROCESSOR),
                libraryHeaders[i]);
        }

        // Now that we have all of the metadata associated with the
        // module, we need to create the module itself. This is somewhat
        // complicated because a module is constructed out of several
        // interrelated pieces (e.g., content loader, search policy,
        // url policy). We need to create all of these pieces and bind
        // them together.

        // First, create the module.
        IModule module = m_factory.createModule(
            Long.toString(targetId) + "." + Integer.toString(revision));
        // Attach the R4 search policy metadata to the module.
        m_policyCore.setExports(module, exports);
        m_policyCore.setImports(module, imports);
        m_policyCore.setDynamicImports(module, dynamics);
        m_policyCore.setLibraries(module, libraries);

        // Create the content loader associated with the module archive.
        IContentLoader contentLoader = new ContentLoaderImpl(
                m_logger,
                m_cache.getArchive(targetId).getRevision(revision).getContent(),
                m_cache.getArchive(targetId).getRevision(revision).getContentPath());
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
    
        String strict = m_config.get(FelixConstants.STRICT_OSGI_PROP);
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

    private void purgeBundle(BundleImpl bundle) throws Exception
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

    private void garbageCollectBundle(BundleImpl bundle) throws Exception
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
        if (m_frameworkDispatcher == null)
        {
            m_frameworkDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
                    ((FrameworkListener) l)
                        .frameworkEvent((FrameworkEvent) eventObj);
                }
            };
        }
        FrameworkEvent event = new FrameworkEvent(type, bundle, throwable);
        m_dispatchQueue.dispatch(
            m_frameworkDispatcher, FrameworkListener.class, event);
    }

    /**
     * Fires bundle events.
     *
     * @param type The type of bundle event to fire.
     * @param bundle The bundle associated with the event.
    **/
    private void fireBundleEvent(int type, Bundle bundle)
    {
        if (m_bundleDispatcher == null)
        {
            m_bundleDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
                    ((BundleListener) l)
                        .bundleChanged((BundleEvent) eventObj);
                }
            };
        }
        BundleEvent event = null;
        event = new BundleEvent(type, bundle);
        m_dispatchQueue.dispatch(m_bundleDispatcher,
            BundleListener.class, event);
    }

    /**
     * Fires service events.
     *
     * @param type The type of service event to fire.
     * @param ref The service reference associated with the event.
    **/
    private void fireServiceEvent(ServiceEvent event)
    {
        if (m_serviceDispatcher == null)
        {
            m_serviceDispatcher = new Dispatcher() {
                public void dispatch(EventListener l, EventObject eventObj)
                {
// TODO: Filter service events based on service permissions.
                    if (l instanceof ListenerWrapper)
                    {
                        BundleImpl bundle = (BundleImpl) ((ServiceListenerWrapper) l).getBundle();
                        if (isServiceAssignable(bundle, ((ServiceEvent) eventObj).getServiceReference()))
                        {
                            ((ServiceListener) l)
                                .serviceChanged((ServiceEvent) eventObj);
                        }
                    }
                    else
                    {
                        ((ServiceListener) l)
                            .serviceChanged((ServiceEvent) eventObj);
                    }
                }
            };
        }
        m_dispatchQueue.dispatch(m_serviceDispatcher,
            ServiceListener.class, event);
    }

    //
    // Property related methods.
    //

    private void initializeFrameworkProperties()
    {
        // Standard OSGi properties.
        m_configMutable.put(
            FelixConstants.FRAMEWORK_VERSION,
            FelixConstants.FRAMEWORK_VERSION_VALUE);
        m_configMutable.put(
            FelixConstants.FRAMEWORK_VENDOR,
            FelixConstants.FRAMEWORK_VENDOR_VALUE);
        m_configMutable.put(
            FelixConstants.FRAMEWORK_LANGUAGE,
            System.getProperty("user.language"));
        m_configMutable.put(
            FelixConstants.FRAMEWORK_OS_VERSION,
            System.getProperty("os.version"));

        String s = null;
        s = R4Library.normalizePropertyValue(
            FelixConstants.FRAMEWORK_OS_NAME,
            System.getProperty("os.name"));
        m_configMutable.put(FelixConstants.FRAMEWORK_OS_NAME, s);
        s = R4Library.normalizePropertyValue(
            FelixConstants.FRAMEWORK_PROCESSOR,
            System.getProperty("os.arch"));
        m_configMutable.put(FelixConstants.FRAMEWORK_PROCESSOR, s);
        m_configMutable.put(
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
        try
        {
            props.load(in);
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        } 
 
        // Maven uses a '-' to separate the version qualifier,
        // while OSGi uses a '.', so we need to convert to a '.'
        StringBuffer sb =
            new StringBuffer(
                props.getProperty(
                    FelixConstants.FELIX_VERSION_PROPERTY, "unknown"));
        if (sb.toString().indexOf("-") >= 0)
        {
            sb.setCharAt(sb.toString().indexOf("-"), '.');
        }
        return sb.toString();
    }
    
    private void processAutoProperties()
    {
        // The auto-install property specifies a space-delimited list of
        // bundle URLs to be automatically installed into each new profile;
        // the start level to which the bundles are assigned is specified by
        // appending a ".n" to the auto-install property name, where "n" is
        // the desired start level for the list of bundles.
        String[] keys = m_config.getKeys();
        for (int i = 0; (keys != null) && (i < keys.length); i++)
        {
            if (keys[i].startsWith(FelixConstants.AUTO_INSTALL_PROP))
            {
                int startLevel = 1;
                try
                {
                    startLevel = Integer.parseInt(keys[i].substring(keys[i].lastIndexOf('.') + 1));
                }
                catch (NumberFormatException ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Invalid property: " + keys[i]);
                }
                StringTokenizer st = new StringTokenizer(m_config.get(keys[i]), "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            try
                            {
                                BundleImpl b = (BundleImpl) installBundle(location, null);
                                b.getInfo().setStartLevel(startLevel);
                            }
                            catch (Exception ex)
                            {
                                m_logger.log(
                                    Logger.LOG_ERROR, "Auto-properties install.", ex);
                            }
                        }
                    }
                    while (location != null);
                }
            }
        }

        // The auto-start property specifies a space-delimited list of
        // bundle URLs to be automatically installed and started into each
        // new profile; the start level to which the bundles are assigned
        // is specified by appending a ".n" to the auto-start property name,
        // where "n" is the desired start level for the list of bundles.
        // The following code starts bundles in two passes, first it installs
        // them, then it starts them.
        for (int i = 0; (keys != null) && (i < keys.length); i++)
        {
            if (keys[i].startsWith(FelixConstants.AUTO_START_PROP))
            {
                int startLevel = 1;
                try
                {
                    startLevel = Integer.parseInt(keys[i].substring(keys[i].lastIndexOf('.') + 1));
                }
                catch (NumberFormatException ex)
                {
                    m_logger.log(Logger.LOG_ERROR, "Invalid property: " + keys[i]);
                }
                StringTokenizer st = new StringTokenizer(m_config.get(keys[i]), "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            try
                            {
                                BundleImpl b = (BundleImpl) installBundle(location, null);
                                b.getInfo().setStartLevel(startLevel);
                            }
                            catch (Exception ex)
                            {
                                m_logger.log(Logger.LOG_ERROR, "Auto-properties install.", ex);
                            }
                        }
                    }
                    while (location != null);
                }
            }
        }

        // Now loop through and start the installed bundles.
        for (int i = 0; (keys != null) && (i < keys.length); i++)
        {
            if (keys[i].startsWith(FelixConstants.AUTO_START_PROP))
            {
                StringTokenizer st = new StringTokenizer(m_config.get(keys[i]), "\" ",true);
                if (st.countTokens() > 0)
                {
                    String location = null;
                    do
                    {
                        location = nextLocation(st);
                        if (location != null)
                        {
                            // Installing twice just returns the same bundle.
                            try
                            {
                                BundleImpl bundle = (BundleImpl) installBundle(location, null);
                                if (bundle != null)
                                {
                                    startBundle(bundle, true);
                                }
                            }
                            catch (Exception ex)
                            {
                                m_logger.log(
                                    Logger.LOG_ERROR, "Auto-properties start.", ex);
                            }
                        }
                    }
                    while (location != null);
                }
            }
        }
    }

    private String nextLocation(StringTokenizer st)
    {
        String retVal = null;

        if (st.countTokens() > 0)
        {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit))
            {
                tok = st.nextToken(tokenList);
                if (tok.equals("\""))
                {
                    inQuote = ! inQuote;
                    if (inQuote)
                    {
                        tokenList = "\"";
                    }
                    else
                    {
                        tokenList = "\" ";
                    }

                }
                else if (tok.equals(" "))
                {
                    if (tokStarted)
                    {
                        retVal = tokBuf.toString();
                        tokStarted=false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                }
                else
                {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted))
            {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }

    //
    // Private utility methods.
    //

    /**
     * Generated the next valid bundle identifier.
    **/
    private long getNextId()
    {
        synchronized (m_nextIdLock)
        {
            return m_nextId++;
        }
    }

    //
    // Configuration methods and inner classes.
    //

    public PropertyResolver getConfig()
    {
        return m_config;
    }

    private class ConfigImpl implements PropertyResolver
    {
        public String get(String key)
        {
            return (m_configMutable == null) ? null : m_configMutable.get(key);
        }

        public String[] getKeys()
        {
            return m_configMutable.getKeys();
        }
    }

    //
    // Logging methods and inner classes.
    //

    public Logger getLogger()
    {
        return m_logger;
    }

    /**
     * Simple class that is used in <tt>refreshPackages()</tt> to embody
     * the refresh logic in order to keep the code clean. This class is
     * not static because it needs access to framework event firing methods.
    **/
    private class RefreshHelper
    {
        private BundleImpl m_bundle = null;

        public RefreshHelper(Bundle bundle)
        {
            m_bundle = (BundleImpl) bundle;
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
                    BundleInfo newInfo = createBundleInfo(info.getArchive());
                    newInfo.syncLock(info);
                    m_bundle.setInfo(newInfo);
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

    protected void acquireBundleLock(BundleImpl bundle)
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
    
    protected boolean acquireBundleLockOrFail(BundleImpl bundle)
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

    protected void releaseBundleLock(BundleImpl bundle)
    {
        synchronized (m_bundleLock)
        {
            bundle.getInfo().unlock();
            m_bundleLock.notifyAll();
        }
    }

    protected BundleImpl[] acquireBundleResolveLocks(Bundle[] targets)
    {
        // Hold bundles to be locked.
        BundleImpl[] bundles = null;
        // Convert existing target bundle array to bundle impl array.
        if (targets != null)
        {
            bundles = new BundleImpl[targets.length];
            for (int i = 0; i < targets.length; i++)
            {
                bundles[i] = (BundleImpl) targets[i];
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
                            BundleImpl bundle = (BundleImpl) iter.next();
                            if (bundle.getInfo().getState() == Bundle.INSTALLED)
                            {
                                list.add(bundle);
                            }
                        }
                    }

                    // Create an array.
                    if (list.size() > 0)
                    {
                        bundles = (BundleImpl[]) list.toArray(new BundleImpl[list.size()]);
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

    protected BundleImpl[] acquireBundleRefreshLocks(Bundle[] targets)
    {
        // Hold bundles to be locked.
        BundleImpl[] bundles = null;

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
                            BundleImpl bundle = (BundleImpl) iter.next();
                            if (bundle.getInfo().isRemovalPending())
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
                        BundleImpl target = (BundleImpl) newTargets[targetIdx];
                        map.put(target, target);
                        // Add all importing bundles to map.
                        populateImportGraph(target, map);
                    }
                    
                    bundles = (BundleImpl[]) map.values().toArray(new BundleImpl[map.size()]);
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

    protected void releaseBundleLocks(BundleImpl[] bundles)
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
