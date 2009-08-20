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
package org.apache.felix.cm.impl;


import java.io.IOException;
import java.security.SecureRandom;
import java.util.*;

import org.apache.felix.cm.PersistenceManager;
import org.apache.felix.cm.file.FilePersistenceManager;
import org.osgi.framework.*;
import org.osgi.service.cm.*;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>ConfigurationManager</code> is the central class in this
 * implementation of the Configuration Admin Service Specification. As such it
 * has the following tasks:
 * <ul>
 * <li>It is a <code>BundleActivator</code> which is called when the bundle
 * is started and stopped.
 * <li>It is a <code>BundleListener</code> which gets informed when the
 * states of bundles change. Mostly this is needed to unbind any bound
 * configuration in case a bundle is uninstalled.
 * <li>It is a <code>ServiceListener</code> which gets informed when
 * <code>ManagedService</code> and <code>ManagedServiceFactory</code>
 * services are registered and unregistered. This is used to provide
 * configuration to these services. As a service listener it also listens for
 * {@link PersistenceManager} instances being registered to support different
 * configuration persistence layers.
 * <li>A {@link ConfigurationAdminFactory} instance is registered as the
 * <code>ConfigurationAdmin</code> service.
 * <li>A {@link FilePersistenceManager} instance is registered as a default
 * {@link PersistenceManager}.
 * <li>Last but not least this instance manages all tasks laid out in the
 * specification such as maintaining configuration, taking care of configuration
 * events, etc.
 * </ul>
 * <p>
 * The default {@link FilePersistenceManager} is configured with a configuration
 * location taken from the <code>felix.cm.dir</code> framework property. If
 * this property is not set the <code>config</code> directory in the current
 * working directory as specified in the <code>user.dir</code> system property
 * is used.
 */
public class ConfigurationManager implements BundleActivator, BundleListener
{

    /**
     * The name of the bundle context property defining the location for the
     * configuration files (value is "felix.cm.dir").
     *
     * @see #start(BundleContext)
     */
    public static final String CM_CONFIG_DIR = "felix.cm.dir";

    /**
     * The name of the bundle context property defining the maximum log level
     * (value is "felix.cm.loglevel"). The log level setting is only used if
     * there is no OSGi LogService available. Otherwise this setting is ignored.
     * <p>
     * This value of this property is expected to be an integer number
     * corresponding to the log level values of the OSGi LogService. That is 1
     * for errors, 2 for warnings, 3 for informational messages and 4 for debug
     * messages. The default value is 2, such that only warnings and errors are
     * logged in the absence of a LogService.
     */
    public static final String CM_LOG_LEVEL = "felix.cm.loglevel";

    // The name of the LogService (not using the class, which might be missing)
    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    private static final int CM_LOG_LEVEL_DEFAULT = 2;

    // random number generator to create configuration PIDs for factory
    // configurations
    private static SecureRandom numberGenerator;

    // comparator used to keep the ordered persistence manager map
    private static final Comparator cmRankComp = new RankingComparator( true, ConfigurationPlugin.CM_RANKING );

    // the BundleContext of the Configuration Admin Service bundle
    private BundleContext bundleContext;

    // the service registration of the configuration admin
    private ServiceRegistration configurationAdminRegistration;

    // the ServiceTracker to emit log services (see log(int, String, Throwable))
    private ServiceTracker logTracker;

    // the ConfigurationEvent listeners
    private ServiceTracker configurationListenerTracker;

    // service tracker for managed services
    private ServiceTracker managedServiceTracker;

    // service tracker for managed service factories
    private ServiceTracker managedServiceFactoryTracker;

    // PersistenceManager services
    private ServiceTracker persistenceManagerTracker;

    // the thread used to schedule tasks required to run asynchronously
    private UpdateThread updateThread;

    /**
     * The actual list of {@link PersistenceManager persistence managers} to use
     * when looking for configuration data. This list is built from the
     * {@link #persistenceManagerMap}, which is ordered according to the
     * {@link RankingComparator}.
     */
    private PersistenceManager[] persistenceManagers;

    // the persistenceManagerTracker.getTrackingCount when the
    // persistenceManagers were last got
    private int pmtCount;

    // the cache of Factory instances mapped by their factory PID
    private final Map factories = new HashMap();

    // the cache of Configuration instances mapped by their PID
    // have this always set to prevent NPE on bundle shutdown
    private final Map configurations = new HashMap();

    /**
     * The map of dynamic configuration bindings. This maps the
     * PID of the dynamically bound configuration or factory to its bundle
     * location.
     * <p>
     * On bundle startup this map is loaded from persistence and validated
     * against the locations of installed bundles: Entries pointing to bundle
     * locations not currently installed are removed.
     * <p>
     * The map is written to persistence on each change.
     */
    private DynamicBindings dynamicBindings;

    // the maximum log level when no LogService is available
    private int logLevel = CM_LOG_LEVEL_DEFAULT;

    // flag indicating whether BundleChange events should be consumed (FELIX-979)
    private volatile boolean handleBundleEvents;

    public void start( BundleContext bundleContext )
    {
        // track the log service using a ServiceTracker
        logTracker = new ServiceTracker( bundleContext, LOG_SERVICE_NAME , null );
        logTracker.open();

        // assign the log level
        String logLevelProp = bundleContext.getProperty( CM_LOG_LEVEL );
        if ( logLevelProp == null )
        {
            logLevel = CM_LOG_LEVEL_DEFAULT;
        }
        else
        {
            try
            {
                logLevel = Integer.parseInt( logLevelProp );
            }
            catch ( NumberFormatException nfe )
            {
                logLevel = CM_LOG_LEVEL_DEFAULT;
            }
        }

        // set up some fields
        this.bundleContext = bundleContext;

        // configurationlistener support
        configurationListenerTracker = new ServiceTracker( bundleContext, ConfigurationListener.class.getName(), null );
        configurationListenerTracker.open();

        // initialize the asynchonous updater thread
        this.updateThread = new UpdateThread( this );
        this.updateThread.start();

        // set up the location (might throw IllegalArgumentException)
        try
        {
            FilePersistenceManager fpm = new FilePersistenceManager( bundleContext, bundleContext
                .getProperty( CM_CONFIG_DIR ) );
            Hashtable props = new Hashtable();
            props.put( Constants.SERVICE_PID, fpm.getClass().getName() );
            props.put( Constants.SERVICE_DESCRIPTION, "Platform Filesystem Persistence Manager" );
            props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
            props.put( Constants.SERVICE_RANKING, new Integer( Integer.MIN_VALUE ) );
            bundleContext.registerService( PersistenceManager.class.getName(), fpm, props );

            // setup dynamic configuration bindings
            dynamicBindings = new DynamicBindings( bundleContext, fpm );
        }
        catch ( IOException ioe )
        {
            log( LogService.LOG_ERROR, "Failure setting up dynamic configuration bindings", ioe );
        }
        catch ( IllegalArgumentException iae )
        {
            log( LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae );
        }

        // register as bundle and service listener
        handleBundleEvents = true;
        bundleContext.addBundleListener( this );

        // get all persistence managers to begin with
        pmtCount = 1; // make sure to get the persistence managers at least once
        persistenceManagerTracker = new ServiceTracker( bundleContext, PersistenceManager.class.getName(), null );
        persistenceManagerTracker.open();

        // create and register configuration admin - start after PM tracker ...
        ConfigurationAdminFactory caf = new ConfigurationAdminFactory( this );
        Hashtable props = new Hashtable();
        props.put( Constants.SERVICE_PID, "org.apache.felix.cm.ConfigurationAdmin" );
        props.put( Constants.SERVICE_DESCRIPTION, "Configuration Admin Service Specification 1.2 Implementation" );
        props.put( Constants.SERVICE_VENDOR, "Apache Software Foundation" );
        configurationAdminRegistration = bundleContext.registerService( ConfigurationAdmin.class.getName(), caf, props );

        // start handling ManagedService[Factory] services
        managedServiceTracker = new ManagedServiceTracker(this);
        managedServiceFactoryTracker = new ManagedServiceFactoryTracker(this);
    }


    public void stop( BundleContext bundleContext )
    {

        // stop handling bundle events immediately
        handleBundleEvents = false;

        // immediately unregister the Configuration Admin before cleaning up
        // clearing the field before actually unregistering the service
        // prevents IllegalStateException in getServiceReference() if
        // the field is not null but the service already unregistered
        if (configurationAdminRegistration != null) {
            ServiceRegistration reg = configurationAdminRegistration;
            configurationAdminRegistration = null;
            reg.unregister();
        }

        // stop handling ManagedService[Factory] services
        managedServiceFactoryTracker.close();
        managedServiceTracker.close();

        // don't care for PersistenceManagers any more
        persistenceManagerTracker.close();

        // stop listening for events
        bundleContext.removeBundleListener( this );

        if ( configurationListenerTracker != null )
        {
            configurationListenerTracker.close();
        }

        if ( updateThread != null )
        {
            // terminate asynchrounous updates
            updateThread.terminate();

            // wait for all updates to terminate
            try
            {
                updateThread.join();
            }
            catch ( InterruptedException ie )
            {
                // don't really care
            }
        }

        if ( logTracker != null )
        {
            logTracker.close();
        }

        // just ensure the configuration cache is empty
        synchronized ( configurations )
        {
            configurations.clear();
        }

        // just ensure the factory cache is empty
        synchronized ( factories )
        {
            factories.clear();
        }

        this.bundleContext = null;
    }


    // ---------- Configuration caching support --------------------------------

    ConfigurationImpl getCachedConfiguration( String pid )
    {
        synchronized ( configurations )
        {
            return ( ConfigurationImpl ) configurations.get( pid );
        }
    }


    ConfigurationImpl[] getCachedConfigurations()
    {
        synchronized ( configurations )
        {
            return ( ConfigurationImpl[] ) configurations.values().toArray(
                new ConfigurationImpl[configurations.size()] );
        }
    }


    ConfigurationImpl cacheConfiguration( ConfigurationImpl configuration )
    {
        synchronized ( configurations )
        {
            Object existing = configurations.get( configuration.getPid() );
            if ( existing != null )
            {
                return ( ConfigurationImpl ) existing;
            }

            configurations.put( configuration.getPid(), configuration );
            return configuration;
        }
    }


    void removeConfiguration( ConfigurationImpl configuration )
    {
        synchronized ( configurations )
        {
            configurations.remove( configuration.getPid() );
        }
    }


    Factory getCachedFactory( String factoryPid )
    {
        synchronized ( factories )
        {
            return ( Factory ) factories.get( factoryPid );
        }
    }


    Factory[] getCachedFactories()
    {
        synchronized ( factories )
        {
            return ( Factory[] ) factories.values().toArray( new Factory[factories.size()] );
        }
    }


    void cacheFactory( Factory factory )
    {
        synchronized ( factories )
        {
            factories.put( factory.getFactoryPid(), factory );
        }
    }


    // ---------- ConfigurationAdminImpl support

    void setDynamicBundleLocation( final String pid, final String location )
    {
        if ( dynamicBindings != null )
        {
            try
            {
                dynamicBindings.putLocation( pid, location );
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Failed storing dynamic configuration binding for " + pid + " to "
                    + location, ioe );
            }
        }
    }


    String getDynamicBundleLocation( final String pid )
    {
        if ( dynamicBindings != null )
        {
            return dynamicBindings.getLocation( pid );
        }

        return null;
    }


    // ---------- ConfigurationAdminImpl support

    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String)
     */
    ConfigurationImpl createFactoryConfiguration( ConfigurationAdminImpl configurationAdmin, String factoryPid )
        throws IOException
    {
        // check Persmission if factory is bound to another bundle
        Factory factory = getFactory( factoryPid );
        if ( factory.getBundleLocation() != null
            && !factory.getBundleLocation().equals( configurationAdmin.getBundle().getLocation() ) )
        {
            configurationAdmin.checkPermission();
        }

        // create the configuration
        String pid = createPid( factoryPid );
        ConfigurationImpl config = createConfiguration( pid, factoryPid, configurationAdmin.getBundle().getLocation() );

        return config;
    }


    /*
     * (non-Javadoc)
     *
     * @see org.osgi.service.cm.ConfigurationAdmin#createFactoryConfiguration(java.lang.String,
     *      java.lang.String)
     */
    ConfigurationImpl createFactoryConfiguration( String factoryPid, String location ) throws IOException
    {
        // create the configuration
        String pid = createPid( factoryPid );
        ConfigurationImpl config = createConfiguration( pid, factoryPid, location );

        return config;
    }


    ConfigurationImpl getExistingConfiguration( String pid ) throws IOException
    {
        ConfigurationImpl config = getCachedConfiguration( pid );
        if ( config != null )
        {
            return config;
        }

        PersistenceManager[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            if ( pmList[i].exists( pid ) )
            {
                Dictionary props = pmList[i].load( pid );
                config = new ConfigurationImpl( this, pmList[i], props );
                return cacheConfiguration( config );
            }
        }

        // neither the cache nor any persistence manager has configuration
        return null;
    }


    ConfigurationImpl getConfiguration( String pid, String bundleLocation ) throws IOException
    {
        // check for existing (cached or persistent) configuration
        ConfigurationImpl config = getExistingConfiguration( pid );
        if ( config != null )
        {
            return config;
        }

        // else create new configuration also setting the bundle location
        // and cache the new configuration
        config = createConfiguration( pid, null, bundleLocation );
        return cacheConfiguration( config );
    }


    ConfigurationImpl[] listConfigurations( ConfigurationAdminImpl configurationAdmin, String filterString )
        throws IOException, InvalidSyntaxException
    {
        Filter filter = null;
        if ( filterString != null )
        {
            filter = bundleContext.createFilter( filterString );
        }

        boolean unprivileged = configurationAdmin != null && !configurationAdmin.hasPermission();
        String location = unprivileged ? configurationAdmin.getBundle().getLocation() : null;

        List configList = new ArrayList();

        PersistenceManager[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            Enumeration configs = pmList[i].getDictionaries();
            while ( configs.hasMoreElements() )
            {
                Dictionary config = ( Dictionary ) configs.nextElement();

                // ignore non-Configuration dictionaries
                String pid = ( String ) config.get( Constants.SERVICE_PID );
                if ( pid == null )
                {
                    continue;
                }

                // ignore this config if not privileged and not bound to bundle
                if ( unprivileged )
                {
                    Object boundLocation = config.get( ConfigurationAdmin.SERVICE_BUNDLELOCATION );
                    if ( !location.equals( boundLocation ) )
                    {
                        continue;
                    }
                }

                // check filter
                if ( filter == null || filter.match( config ) )
                {
                    // ensure the service.pid and returned a cached config if available
                    ConfigurationImpl cfg = getCachedConfiguration( pid );
                    if ( cfg == null )
                    {
                        cfg = new ConfigurationImpl( this, pmList[i], config );
                    }

                    // FELIX-611: Ignore configuration objects without props
                    if ( !cfg.isNew() )
                    {
                        configList.add( cfg );
                    }
                }
            }
        }

        return ( ConfigurationImpl[] ) configList.toArray( new ConfigurationImpl[configList
            .size()] );
    }


    void deleted( ConfigurationImpl config )
    {
        // remove the configuration from the cache
        removeConfiguration( config );
        updateThread.schedule( new DeleteConfiguration( config, true ) );
    }


    void updated( ConfigurationImpl config, boolean fireEvent )
    {
        updateThread.schedule( new UpdateConfiguration( config, fireEvent ) );
    }


    void revokeConfiguration( ConfigurationImpl config )
    {
        updateThread.schedule( new DeleteConfiguration( config, false ) );

        // immediately unbind the configuration
        config.setDynamicBundleLocation( null );
        config.setServiceReference( null );
    }


    void reassignConfiguration( ConfigurationImpl config )
    {
        updateThread.schedule( new UpdateConfiguration( config, false ) );
    }


    void fireConfigurationEvent( int type, String pid, String factoryPid )
    {

        updateThread.schedule( new FireConfigurationEvent( type, pid, factoryPid) );
    }


    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.UNINSTALLED && handleBundleEvents )
        {
            final String location = event.getBundle().getLocation();

            // we only reset dynamic bindings, which are only present in
            // cached configurations, hence only consider cached configs here
            final ConfigurationImpl[] configs = getCachedConfigurations();
            for ( int i = 0; i < configs.length; i++ )
            {
                final ConfigurationImpl cfg = configs[i];
                if ( location.equals( cfg.getDynamicBundleLocation() ) )
                {
                    cfg.setDynamicBundleLocation( null );
                }
            }

            // we only reset dynamic bindings, which are only present in
            // cached factories, hence only consider cached factories here
            final Factory[] factories = getCachedFactories();
            for ( int i = 0; i < factories.length; i++ )
            {
                final Factory factory = factories[i];
                if ( location.equals( factory.getDynamicBundleLocation() ) )
                {
                    factory.setDynamicBundleLocation( null );
                }
            }
        }
    }


    // ---------- internal -----------------------------------------------------

    private PersistenceManager[] getPersistenceManagers()
    {
        int currentPmtCount = persistenceManagerTracker.getTrackingCount();
        if ( persistenceManagers == null || currentPmtCount > pmtCount )
        {

            List pmList = new ArrayList();
            PersistenceManager[] pm;

            ServiceReference[] refs = persistenceManagerTracker.getServiceReferences();
            if ( refs == null || refs.length == 0 )
            {
                pm = new PersistenceManager[0];
            }
            else
            {
                // sort the references according to the cmRanking property
                SortedSet pms = new TreeSet( new RankingComparator( false ) );
                for ( int i = 0; i < refs.length; i++ )
                {
                    pms.add( refs[i] );
                }

                // create the service array from the sorted set of referenecs
                int pmIndex = 0;
                for ( Iterator pi = pms.iterator(); pi.hasNext(); pmIndex++ )
                {
                    ServiceReference ref = ( ServiceReference ) pi.next();
                    Object service = persistenceManagerTracker.getService( ref );
                    if ( service != null )
                    {
                        pmList.add( service );
                    }
                }

                pm = ( PersistenceManager[] ) pmList.toArray( new PersistenceManager[pmList.size()] );
            }

            pmtCount = pm.length;
            persistenceManagers = pm;
        }

        return persistenceManagers;
    }


    private ServiceReference getServiceReference()
    {
        ServiceRegistration reg = configurationAdminRegistration;
        return ( reg != null ) ? reg.getReference() : null;
    }


    private void configure( ServiceReference sr, ManagedService service )
    {
        String[] pids = getServicePid( sr );
        if ( pids != null )
        {
            for ( int i = 0; i < pids.length; i++ )
            {
                ManagedServiceUpdate update = new ManagedServiceUpdate( pids[i], sr, service );
                updateThread.schedule( update );
            }
        }
    }


    private void configure( ServiceReference sr, ManagedServiceFactory service )
    {
        String[] pids = getServicePid( sr );
        if ( pids != null )
        {
            for ( int i = 0; i < pids.length; i++ )
            {
                ManagedServiceFactoryUpdate update = new ManagedServiceFactoryUpdate( pids[i], sr, service );
                updateThread.schedule( update );
            }
        }
    }


    /**
     * Factory method to create a new configuration object. The configuration
     * object returned is not stored in configuration cache and only persisted
     * if the <code>factoryPid</code> parameter is <code>null</code>.
     *
     * @param pid
     *            The PID of the new configuration object. Must not be
     *            <code>null</code>.
     * @param factoryPid
     *            The factory PID of the new configuration. Not
     *            <code>null</code> if the new configuration object belongs to a
     *            factory. The configuration object will not be persisted if
     *            this parameter is not <code>null</code>.
     * @param bundleLocation
     *            The bundle location of the bundle to which the configuration
     *            belongs or <code>null</code> if the configuration is not bound
     *            yet.
     * @return The new configuration object
     * @throws IOException
     *             May be thrown if an error occurrs persisting the new
     *             configuration object.
     */
    ConfigurationImpl createConfiguration( String pid, String factoryPid, String bundleLocation ) throws IOException
    {
        return new ConfigurationImpl( this, getPersistenceManagers()[0], pid, factoryPid, bundleLocation );
    }


    Factory getFactory( String factoryPid ) throws IOException
    {
        Factory factory = getCachedFactory( factoryPid );
        if ( factory != null )
        {
            return factory;
        }

        PersistenceManager[] pmList = getPersistenceManagers();
        for ( int i = 0; i < pmList.length; i++ )
        {
            if ( Factory.exists( pmList[i], factoryPid ) )
            {
                factory = Factory.load( this, pmList[i], factoryPid );
                cacheFactory( factory );
                return factory;
            }
        }

        // if getting here, there is no configuration yet, optionally create new
        return createFactory( factoryPid );
    }


    Factory createFactory( String factoryPid )
    {
        Factory factory = new Factory( this, getPersistenceManagers()[0], factoryPid );
        cacheFactory( factory );
        return factory;
    }


    /**
     * Calls the registered configuration plugins on the given configuration
     * properties from the given configuration object unless the configuration
     * has just been created and not been updated yet.
     *
     * @param props The configuraiton properties run through the registered
     *          ConfigurationPlugin services. This may be <code>null</code>
     *          in which case this method just immediately returns.
     * @param targetPid The identification of the configuration update used to
     *          select the plugins according to their cm.target service
     *          property
     * @param sr The service reference of the managed service (factory) which
     *          is to be updated with configuration
     * @param cfg The configuration object whose properties have to be passed
     *          through the plugins
     */
    private void callPlugins( final Dictionary props, final String targetPid, final ServiceReference sr,
        final ConfigurationImpl cfg )
    {
        // guard against NPE for new configuration never updated
        if (props == null) {
            return;
        }

        ServiceReference[] plugins = null;
        try
        {
            String filter = "(|(!(cm.target=*))(cm.target=" + targetPid + "))";
            plugins = bundleContext.getServiceReferences( ConfigurationPlugin.class.getName(), filter );
        }
        catch ( InvalidSyntaxException ise )
        {
            // no filter, no exception ...
        }

        // abort early if there are no plugins
        if ( plugins == null || plugins.length == 0 )
        {
            return;
        }

        // sort the plugins by their service.cmRanking
        SortedSet pluginSet = new TreeSet( cmRankComp );
        for ( int i = 0; i < plugins.length; i++ )
        {
            pluginSet.add( plugins[i] );
        }

        // call the plugins in order
        for ( Iterator pi = pluginSet.iterator(); pi.hasNext(); )
        {
            ServiceReference pluginRef = ( ServiceReference ) pi.next();
            ConfigurationPlugin plugin = ( ConfigurationPlugin ) bundleContext.getService( pluginRef );
            try
            {
                plugin.modifyConfiguration( sr, props );
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Unexpected problem calling configuration plugin " + toString( pluginRef ),
                    t );
            }
            finally
            {
                // ensure ungetting the plugin
                bundleContext.ungetService( pluginRef );
            }
            cfg.setAutoProperties( props, false );
        }
    }


    /**
     * Creates a PID for the given factoryPid
     *
     * @param factoryPid
     * @return
     */
    private static String createPid( String factoryPid )
    {
        SecureRandom ng = numberGenerator;
        if ( ng == null )
        {
            numberGenerator = ng = new SecureRandom();
        }

        byte[] randomBytes = new byte[16];
        ng.nextBytes( randomBytes );
        randomBytes[6] &= 0x0f; /* clear version */
        randomBytes[6] |= 0x40; /* set to version 4 */
        randomBytes[8] &= 0x3f; /* clear variant */
        randomBytes[8] |= 0x80; /* set to IETF variant */

        StringBuffer buf = new StringBuffer( factoryPid.length() + 1 + 36 );

        // prefix the new pid with the factory pid
        buf.append( factoryPid ).append( "." );

        // serialize the UUID into the buffer
        for ( int i = 0; i < randomBytes.length; i++ )
        {

            if ( i == 4 || i == 6 || i == 8 || i == 10 )
            {
                buf.append( '-' );
            }

            int val = randomBytes[i] & 0xff;
            buf.append( Integer.toHexString( val >> 4 ) );
            buf.append( Integer.toHexString( val & 0xf ) );
        }

        return buf.toString();
    }


    void log( int level, String message, Throwable t )
    {
        // log using the LogService if available
        Object log = logTracker.getService();
        if ( log != null )
        {
            ( ( LogService ) log ).log( getServiceReference(), level, message, t );
            return;
        }

        // Otherwise only log if more serious than the configured level
        if ( level <= logLevel )
        {
            String code;
            switch ( level )
            {
                case LogService.LOG_INFO:
                    code = "*INFO *";
                    break;

                case LogService.LOG_WARNING:
                    code = "*WARN *";
                    break;

                case LogService.LOG_ERROR:
                    code = "*ERROR*";
                    break;

                case LogService.LOG_DEBUG:
                default:
                    code = "*DEBUG*";
            }

            System.err.println( code + " " + message );
            if ( t != null )
            {
                t.printStackTrace( System.err );
            }
        }
    }


    /**
     * Returns the <code>service.pid</code> property of the service reference as
     * an array of strings or <code>null</code> if the service reference does
     * not have a service PID property.
     * <p>
     * The service.pid property may be a single string, in which case a single
     * element array is returned. If the property is an array of string, this
     * array is returned. If the property is a collection it is assumed to be a
     * collection of strings and the collection is converted to an array to be
     * returned. Otherwise (also if the property is not set) <code>null</code>
     * is returned.
     *
     * @throws NullPointerException
     *             if reference is <code>null</code>
     * @throws ArrayStoreException
     *             if the service pid is a collection and not all elements are
     *             strings.
     */
    static String[] getServicePid( ServiceReference reference )
    {
        Object pidObj = reference.getProperty( Constants.SERVICE_PID );
        if ( pidObj instanceof String )
        {
            return new String[]
                { ( String ) pidObj };
        }
        else if ( pidObj instanceof String[] )
        {
            return ( String[] ) pidObj;
        }
        else if ( pidObj instanceof Collection )
        {
            Collection pidCollection = ( Collection ) pidObj;
            return ( String[] ) pidCollection.toArray( new String[pidCollection.size()] );
        }

        return null;
    }


    static String toString( ServiceReference ref )
    {
        String[] ocs = ( String[] ) ref.getProperty( "objectClass" );
        String oc = "[";
        for ( int i = 0; i < ocs.length; i++ )
        {
            oc += ocs[i];
            if ( i < ocs.length - 1 )
                oc += ", ";
        }

        oc += ", id=" + ref.getProperty( Constants.SERVICE_ID );
        oc += ", bundle=" + ref.getBundle().getBundleId();
        oc += "]";
        return oc;
    }


    void handleCallBackError( final Throwable error, final ServiceReference target, final ConfigurationImpl config )
    {
        if ( error instanceof ConfigurationException )
        {
            final ConfigurationException ce = ( ConfigurationException ) error;
            if ( ce.getProperty() != null )
            {
                log( LogService.LOG_ERROR, toString( target ) + ": Updating configuration property " + ce.getProperty()
                    + " caused a problem: " + ce.getReason(), ce );
            }
            else
            {
                log( LogService.LOG_ERROR, toString( target ) + ": Updating configuration caused a problem: "
                    + ce.getReason(), ce );

            }
        }
        else
        {
            {
                log( LogService.LOG_ERROR, toString( target ) + ": Unexpected problem updating " + config, error );
            }

        }
    }

    // ---------- inner classes ------------------------------------------------

    private class ManagedServiceUpdate implements Runnable
    {
        private final String pid;

        private final ServiceReference sr;

        private final ManagedService service;

        private final ConfigurationImpl config;

        private final Dictionary rawProperties;

        ManagedServiceUpdate( String pid, ServiceReference sr, ManagedService service )
        {
            this.pid = pid;
            this.sr = sr;
            this.service = service;

            // get or load configuration for the pid
            ConfigurationImpl config = null;
            Dictionary rawProperties = null;
            try
            {
                config = getExistingConfiguration( pid );
                if (config != null) {
                    rawProperties = config.getProperties( true );
                }
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Error loading configuration for " + pid, ioe );
            }

            this.config = config;
            this.rawProperties = rawProperties;
        }


        public void run()
        {
            // check configuration and call plugins if existing
            Dictionary properties = rawProperties;
            if ( config != null )
            {
                Bundle serviceBundle = sr.getBundle();
                if ( serviceBundle == null )
                {
                    log( LogService.LOG_INFO, "ServiceFactory for PID " + pid
                        + " seems to already have been unregistered, not updating with configuration", null );
                    return;
                }

                // 104.3 Ignore duplicate PIDs from other bundles and report
                // them to the log
                // 104.4.1 No update call back for PID already bound to another
                // bundle location
                // 104.4.1 assign configuration to bundle if unassigned
                if ( !config.tryBindLocation( serviceBundle.getLocation() ) )
                {
                    log( LogService.LOG_ERROR, "Cannot use configuration " + pid + " for "
                        + ConfigurationManager.toString( sr ) + ": Configuration bound to bundle "
                        + config.getBundleLocation(), null );
                    return;
                }

                // 104.3 Report an error in the log if more than one service
                // with the same PID asks for the configuration
                if ( config.getServiceReference() == null )
                {
                    // assign the configuration to the service
                    config.setServiceReference( sr );
                }
                else if ( !sr.equals( config.getServiceReference() ) )
                {
                    log( LogService.LOG_ERROR, "Configuration for " + pid + " has already been used for service "
                        + ConfigurationManager.toString( config.getServiceReference() )
                        + " and will now also be given to " + ConfigurationManager.toString( sr ), null );
                }

                // prepare the configuration for the service (call plugins)
                callPlugins( properties, pid, sr, config );
            }
            else
            {
                // 104.5.3 ManagedService.updated must be called with null
                // if no configuration is available
                properties = null;
            }

            // update the service with the configuration
            try
            {
                service.updated( properties );
            }
            catch ( Throwable t )
            {
                handleCallBackError( t, sr, config );
            }
        }

        public String toString()
        {
            return "ManagedService Update: pid=" + pid;
        }
    }

    private class ManagedServiceFactoryUpdate implements Runnable
    {
        private final String factoryPid;

        private final ServiceReference sr;

        private final ManagedServiceFactory service;

        private final Factory factory;

        private final Map configs;

        ManagedServiceFactoryUpdate( String factoryPid, ServiceReference sr, ManagedServiceFactory service )
        {
            this.factoryPid = factoryPid;
            this.sr = sr;
            this.service = service;

            Factory factory = null;
            Map configs = null;
            try
            {
                factory = getFactory( factoryPid );
                if (factory != null) {
                    configs = new HashMap();
                    for ( Iterator pi = factory.getPIDs().iterator(); pi.hasNext(); )
                    {
                        final String pid = ( String ) pi.next();
                        ConfigurationImpl cfg;
                        try
                        {
                            cfg = getExistingConfiguration( pid );
                        }
                        catch ( IOException ioe )
                        {
                            log( LogService.LOG_ERROR, "Error loading configuration for " + pid, ioe );
                            continue;
                        }

                        // sanity check on the configuration
                        if ( cfg == null )
                        {
                            log( LogService.LOG_ERROR, "Configuration " + pid + " referred to by factory " + factoryPid
                                + " does not exist", null );
                            factory.removePID( pid );
                            factory.storeSilently();
                            continue;
                        }
                        else if ( cfg.isNew() )
                        {
                            // Configuration has just been created but not yet updated
                            // we currently just ignore it and have the update mechanism
                            // provide the configuration to the ManagedServiceFactory
                            // As of FELIX-612 (not storing new factory configurations)
                            // this should not happen. We keep this for added stability
                            // but raise the logging level to error.
                            log( LogService.LOG_ERROR, "Ignoring new configuration pid=" + pid, null );
                            continue;
                        }
                        else if ( !factoryPid.equals( cfg.getFactoryPid() ) )
                        {
                            log( LogService.LOG_ERROR, "Configuration " + pid + " referred to by factory " + factoryPid
                                + " seems to belong to factory " + cfg.getFactoryPid(), null );
                            factory.removePID( pid );
                            factory.storeSilently();
                            continue;
                        }

                        // get the configuration properties for later
                        configs.put(cfg, cfg.getProperties( true ));
                    }
                }
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Cannot get factory mapping for factory PID " + factoryPid, ioe );
            }

            this.factory = factory;
            this.configs = configs;
        }


        public void run()
        {
            Bundle serviceBundle = sr.getBundle();
            if ( serviceBundle == null )
            {
                log( LogService.LOG_INFO, "ManagedServiceFactory for factory PID " + factoryPid
                    + " seems to already have been unregistered, not updating with factory", null );
                return;
            }

            final String serviceBundleLocation = serviceBundle.getLocation();
            if ( !factory.tryBindLocation( serviceBundleLocation ) )
            {
                // factory PID is bound to another bundle
                log( LogService.LOG_ERROR, "Cannot use factory configuration " + factoryPid + " for "
                    + ConfigurationManager.toString( sr ) + ": Configuration bound to bundle "
                    + factory.getBundleLocation(), null );

                return;
            }

            for ( Iterator ci=configs.entrySet().iterator(); ci.hasNext(); )
            {
                final Map.Entry entry = (Map.Entry) ci.next();
                final ConfigurationImpl cfg = (ConfigurationImpl) entry.getKey();
                final Dictionary properties = (Dictionary) entry.getValue();

                // check bundle location of configuration
                if ( !cfg.tryBindLocation( serviceBundleLocation ) )
                {
                    // configuration is bound to another bundle
                    log( LogService.LOG_ERROR, "Cannot use configuration " + cfg.getPid() + " (factory " + factoryPid
                        + ") for " + ConfigurationManager.toString( sr ) + ": Configuration bound to bundle "
                        + cfg.getBundleLocation(), null );

                    continue;
                }

                // 104.3 Report an error in the log if more than one service
                // with the same PID asks for the configuration
                if ( cfg.getServiceReference() == null )
                {
                    // assign the configuration to the service
                    cfg.setServiceReference( sr );
                }
                else if ( !sr.equals( cfg.getServiceReference() ) )
                {
                    log( LogService.LOG_ERROR, "Configuration for " + cfg.getPid() + " (factory " + factoryPid
                        + ") has already been used for service "
                        + ConfigurationManager.toString( cfg.getServiceReference() )
                        + " and will now also be given to " + ConfigurationManager.toString( sr ), null );
                }

                // prepare the configuration for the service (call plugins)
                // call the plugins with cm.target set to the service's factory PID
                // (clarification in Section 104.9.1 of Compendium 4.2)
                callPlugins( properties, factoryPid, sr, cfg );

                // update the service with the configuration (if non-null)
                if ( properties != null )
                {
                    log( LogService.LOG_DEBUG, sr + ": Updating configuration pid=" + cfg.getPid(), null );
                    try
                    {
                        service.updated( cfg.getPid(), properties );
                    }
                    catch ( Throwable t )
                    {
                        handleCallBackError( t, sr, cfg );
                    }
                }
            }
        }


        public String toString()
        {
            return "ManagedServiceFactory Update: factoryPid=" + factoryPid;
        }
    }

    private class UpdateConfiguration implements Runnable
    {

        private final ConfigurationImpl config;
        private final Dictionary properties;
        private final boolean fireEvent;


        UpdateConfiguration( final ConfigurationImpl config, boolean fireEvent )
        {
            this.config = config;
            this.properties = config.getProperties( true );
            this.fireEvent = fireEvent;
        }


        public void run()
        {
            try
            {
                if ( config.getFactoryPid() == null )
                {
                    final ServiceReference[] srList = bundleContext.getServiceReferences( ManagedService.class
                        .getName(), "(" + Constants.SERVICE_PID + "=" + config.getPid() + ")" );
                    if ( srList != null )
                    {
                        // find the primary configuration owner
                        final ServiceReference ownerRef = getOwner( config, srList );
                        final String bundleLocation = ( ownerRef != null ) ? ownerRef.getBundle().getLocation()
                            : config.getBoundBundleLocation();

                        // if the configuration is unbound, bind to owner
                        if ( config.getBundleLocation() == null )
                        {
                            config.setDynamicBundleLocation( bundleLocation );
                        }
                        final String configBundleLocation = config.getBundleLocation();

                        // provide configuration to all services from the
                        // correct bundle
                        for ( int i = 0; i < srList.length; i++ )
                        {

                            final ServiceReference userRef = srList[i];
                            final String userRefLocation = userRef.getBundle().getLocation();

                            // only consider the entry if in the same bundle
                            if ( !userRefLocation.equals( configBundleLocation ) )
                            {
                                log( LogService.LOG_ERROR, "Cannot use configuration " + config.getPid() + " for "
                                    + ConfigurationManager.toString( userRef ) + ": Configuration bound to bundle "
                                    + configBundleLocation, null );

                                continue;
                            }

                            // 104.3 Report an error in the log if more than one
                            // service with the same PID asks for the
                            // configuration
                            if ( userRef != ownerRef )
                            {
                                log( LogService.LOG_ERROR, "Configuration for " + config.getPid()
                                    + " has already been used for service " + ConfigurationManager.toString( ownerRef )
                                    + " and will now also be given to " + ConfigurationManager.toString( userRef ), null );
                            }

                            try
                            {
                                final ManagedService srv = ( ManagedService ) bundleContext.getService( userRef );
                                if ( srv != null )
                                {
                                    Dictionary props = new CaseInsensitiveDictionary( properties );
                                    callPlugins( props, config.getPid(), userRef, config );
                                    srv.updated( props );
                                }
                            }
                            catch ( Throwable t )
                            {
                                handleCallBackError( t, userRef, config );
                            }
                            finally
                            {
                                bundleContext.ungetService( userRef );
                            }
                        }
                    }
                }
                else
                {
                    ServiceReference[] srList = bundleContext.getServiceReferences( ManagedServiceFactory.class
                        .getName(), "(" + Constants.SERVICE_PID + "=" + config.getFactoryPid() + ")" );
                    if ( srList != null && srList.length > 0 )
                    {
                        // find the primary configuration owner
                        final ServiceReference ownerRef = getOwner( config, srList );
                        final String bundleLocation = ( ownerRef != null ) ? ownerRef.getBundle().getLocation()
                            : config.getBoundBundleLocation();

                        // if the configuration is unbound, bind to owner
                        if ( config.getBundleLocation() == null )
                        {
                            config.setDynamicBundleLocation( bundleLocation );
                        }
                        final String configBundleLocation = config.getBundleLocation();

                        // provide configuration to all services from the
                        // correct bundle
                        for ( int i = 0; i < srList.length; i++ )
                        {
                            final ServiceReference ref = srList[i];
                            final String refLocation = ref.getBundle().getLocation();

                            // only consider the entry if in the same bundle
                            if ( !refLocation.equals( configBundleLocation ) )
                            {
                                log( LogService.LOG_ERROR, "Cannot use configuration " + config.getPid() + " (factory "
                                    + config.getFactoryPid() + ") for " + ConfigurationManager.toString( ref )
                                    + ": Configuration bound to bundle " + configBundleLocation, null );

                                continue;
                            }

                            // 104.3 Report an error in the log if more than one
                            // service with the same PID asks for the
                            // configuration
                            if ( ref != ownerRef )
                            {
                                log( LogService.LOG_ERROR, "Configuration for " + config.getPid() + " (factory "
                                    + config.getFactoryPid() + ") has already been used for service "
                                    + ConfigurationManager.toString( ownerRef ) + " and will now also be given to "
                                    + ConfigurationManager.toString( ref ), null );
                            }

                            try
                            {
                                final ManagedServiceFactory srv = ( ManagedServiceFactory ) bundleContext
                                    .getService( ref );
                                if ( srv != null && properties != null )
                                {
                                    Dictionary props = new CaseInsensitiveDictionary( properties );
                                    callPlugins( props, config.getFactoryPid(), ref, config );
                                    srv.updated( config.getPid(), props );
                                }
                            }
                            catch ( Throwable t )
                            {
                                handleCallBackError( t, ref, config );
                            }
                            finally
                            {
                                bundleContext.ungetService( ref );
                            }
                        }
                    }
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                log( LogService.LOG_ERROR, "Service selection filter is invalid to update " + config, ise );
            }
            finally
            {
                // the update event has to be sent regardless of whether the
                // configuration was updated in a managed service or not
                if ( fireEvent )
                {
                    fireConfigurationEvent( ConfigurationEvent.CM_UPDATED, config.getPid(), config.getFactoryPid() );
                }
            }
        }


        private ServiceReference getOwner( ConfigurationImpl config, ServiceReference[] srList )
        {
            // find the current owner among the references (if any)
            if ( config.getServiceReference() != null )
            {
                for ( int i = 0; i < srList.length; i++ )
                {
                    if ( srList[i].equals( config.getServiceReference() ) )
                    {
                        return srList[i];
                    }
                }
            }
            // configuration has never been supplied or the binding is stale

            // if the configuration is location bound, find a service reference
            // from the same bundle
            final String configLocation = config.getBoundBundleLocation();
            if (configLocation != null) {
                for ( int i = 0; i < srList.length; i++ )
                {
                    if ( configLocation.equals(srList[i].getBundle().getLocation() ) )
                    {
                        return srList[i];
                    }
                }
                // no service from the same bundle found, thus we cannot
                // find a new owner !!
                return null;
            }
            // configuration is not location bound (yet)

            // just use the first entry in the list as the new owner
            final ServiceReference ownerRef = srList[0];
            config.setServiceReference( ownerRef );
            return ownerRef;
        }


        public String toString()
        {
            return "Update: pid=" + config.getPid();
        }
    }

    private class DeleteConfiguration implements Runnable
    {

        private final ConfigurationImpl config;
        private final String configLocation;
        private final boolean fireEvent;


        DeleteConfiguration( ConfigurationImpl config, boolean fireEvent )
        {
            /*
             * NOTE: We keep the configuration because it might be cleared just
             * after calling this method. The pid and factoryPid fields are
             * final and cannot be reset.
             */
            this.config = config;
            this.configLocation = config.getBoundBundleLocation();
            this.fireEvent = fireEvent;
        }


        public void run()
        {
            final String pid = config.getPid();
            final String factoryPid = config.getFactoryPid();

            try
            {
                if ( factoryPid == null )
                {
                    ServiceReference[] srList = bundleContext.getServiceReferences( ManagedService.class.getName(), "("
                        + Constants.SERVICE_PID + "=" + pid + ")" );
                    if ( srList != null )
                    {
                        for ( int i = 0; i < srList.length; i++ )
                        {
                            final ServiceReference sr = srList[i];
                            if ( sr.getBundle().getLocation().equals( configLocation ) )
                            {
                                // only if the service is from the bound bundle
                                final ManagedService srv = ( ManagedService ) bundleContext.getService( sr );
                                try
                                {
                                    srv.updated( null );
                                }
                                catch ( Throwable t )
                                {
                                    handleCallBackError( t, sr, config );
                                }
                                finally
                                {
                                   bundleContext.ungetService( sr );
                                }
                            }
                        }
                    }
                }
                else
                {
                    // remove the pid from the factory
                    try
                    {
                        Factory factory = getFactory( factoryPid );
                        factory.removePID( pid );
                        factory.store();
                    }
                    catch ( IOException ioe )
                    {
                        log( LogService.LOG_ERROR, "Failed removing " + pid + " from the factory " + factoryPid, ioe );
                    }

                    ServiceReference[] srList = bundleContext.getServiceReferences( ManagedServiceFactory.class
                        .getName(), "(" + Constants.SERVICE_PID + "=" + factoryPid + ")" );
                    if ( srList != null)
                    {
                        for ( int i = 0; i < srList.length; i++ )
                        {
                            final ServiceReference sr = srList[i];
                            if ( sr.getBundle().getLocation().equals( configLocation ) )
                            {
                                // only if the service is from the bound bundle
                                final ManagedServiceFactory srv = ( ManagedServiceFactory ) bundleContext
                                    .getService( sr );
                                try
                                {
                                    srv.deleted( pid );
                                }
                                catch ( Throwable t )
                                {
                                    handleCallBackError( t, sr, config );
                                }
                                finally
                                {
                                    bundleContext.ungetService( sr );
                                }
                            }
                        }
                    }
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                log( LogService.LOG_ERROR, "Service selection filter is invalid to update " + config, ise );
            }
            finally
            {
                // the delete event has to be sent regardless of whether the
                // configuration was updated in a managed service or not
                if ( fireEvent )
                {
                    fireConfigurationEvent( ConfigurationEvent.CM_DELETED, pid, factoryPid );
                }
            }
        }

        public String toString()
        {
            return "Delete: pid=" + config.getPid();
        }
    }

    private class FireConfigurationEvent implements Runnable
    {
        private int type;

        private String pid;

        private String factoryPid;


        FireConfigurationEvent( int type, String pid, String factoryPid )
        {
            this.type = type;
            this.pid = pid;
            this.factoryPid = factoryPid;
        }


        public void run()
        {
            // get the listeners
            ServiceReference[] srs = configurationListenerTracker.getServiceReferences();
            if ( srs == null || srs.length == 0 )
            {
                return;
            }

            ConfigurationEvent event = new ConfigurationEvent( getServiceReference(), type, factoryPid, pid );

            for ( int i = 0; i < srs.length; i++ )
            {
                ConfigurationListener cl = ( ConfigurationListener ) configurationListenerTracker.getService( srs[i] );
                try
                {
                    cl.configurationEvent( event );
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem delivery configuration event to "
                        + ConfigurationManager.toString( srs[i] ), t );
                }
            }
        }

        public String toString()
        {
            return "Fire ConfigurationEvent: pid=" + pid;
        }
    }

    private static class ManagedServiceTracker extends ServiceTracker
    {

        private final ConfigurationManager cm;


        ManagedServiceTracker( ConfigurationManager cm )
        {
            super( cm.bundleContext, ManagedService.class.getName(), null );
            this.cm = cm;
            open();
        }


        public Object addingService( ServiceReference reference )
        {
            Object serviceObject = super.addingService( reference );

            // configure the managed service
            if ( serviceObject instanceof ManagedService )
            {
                cm.configure(reference, ( ManagedService ) serviceObject);
            }
            else
            {
                cm.log( LogService.LOG_WARNING, "Service " + serviceObject + " is not a ManagedService", null );
            }

            return serviceObject;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            // check whether we can take back the configuration object
            String[] pids = getServicePid( reference );
            if ( pids != null )
            {
                for ( int i = 0; i < pids.length; i++ )
                {
                    ConfigurationImpl cfg = cm.getCachedConfiguration( pids[i] );
                    if ( cfg != null && reference.equals( cfg.getServiceReference() ) )
                    {
                        cfg.setServiceReference( null );
                    }
                }
            }

            super.removedService( reference, service );
        }
    }

    private static class ManagedServiceFactoryTracker extends ServiceTracker
    {
        private final ConfigurationManager cm;


        ManagedServiceFactoryTracker( ConfigurationManager cm )
        {
            super( cm.bundleContext, ManagedServiceFactory.class.getName(), null );
            this.cm = cm;
            open();
        }


        public Object addingService( ServiceReference reference )
        {
            Object serviceObject = super.addingService( reference );

            // configure the managed service factory
            if ( serviceObject instanceof ManagedServiceFactory )
            {
                cm.configure( reference, ( ManagedServiceFactory ) serviceObject );
            }
            else
            {
                cm.log( LogService.LOG_WARNING, "Service " + serviceObject + " is not a ManagedServiceFactory", null );
            }

            return serviceObject;
        }


        public void removedService( ServiceReference reference, Object service )
        {
            // check whether we can take back the configuration objects
            String[] factoryPids = getServicePid( reference );
            if ( factoryPids != null )
            {
                for ( int i = 0; i < factoryPids.length; i++ )
                {
                    Factory factory = cm.getCachedFactory( factoryPids[i] );
                    if ( factory != null )
                    {
                        for ( Iterator pi = factory.getPIDs().iterator(); pi.hasNext(); )
                        {
                            String pid = ( String ) pi.next();
                            ConfigurationImpl cfg = cm.getCachedConfiguration( pid );
                            if ( cfg != null && reference.equals( cfg.getServiceReference() ) )
                            {
                                cfg.setServiceReference( null );
                            }
                        }
                    }
                }
            }

            super.removedService( reference, service );
        }

    }
}
