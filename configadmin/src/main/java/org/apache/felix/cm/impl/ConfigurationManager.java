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
 *
 * @author fmeschbe
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

    // The name of the LogService (not using the class, which might be missing)
    private static final String LOG_SERVICE_NAME = "org.osgi.service.log.LogService";

    // random number generator to create configuration PIDs for factory
    // configurations
    private static SecureRandom numberGenerator;

    // comparator used to keep the ordered persistence manager map
    private static final Comparator cmRankComp = new RankingComparator( true, ConfigurationPlugin.CM_RANKING );

    // the BundleContext of the Configuration Admin Service bundle
    private BundleContext bundleContext;

    // the service registration of the configuration admin
    private ServiceRegistration configurationAdminRegistration;

    // the service reference to the ConfigurationAdmin service
    private ServiceReference configurationAdminReference;

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
    private Map factories;

    // the cache of Configuration instances mapped by their PID
    private Map configurations;


    public void start( BundleContext bundleContext )
    {
        // track the log service using a ServiceTracker
        logTracker = new ServiceTracker( bundleContext, LOG_SERVICE_NAME , null );
        logTracker.open();

        // set up some fields
        this.bundleContext = bundleContext;
        this.factories = new HashMap();
        this.configurations = new HashMap();

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
        }
        catch ( IllegalArgumentException iae )
        {
            log( LogService.LOG_ERROR, "Cannot create the FilePersistenceManager", iae );
        }

        // register as bundle and service listener
        bundleContext.addBundleListener( this );

        // get all persistence managers to begin with
        pmtCount = 1; // make sure to get the persistence managers at least
        // once
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

        // immediately unregister the Configuration Admin before cleaning up
        configurationAdminRegistration.unregister();
        configurationAdminRegistration = null;

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

        this.bundleContext = null;
        this.configurations = null;
    }


    // ---------- Configuration caching support --------------------------------

    ConfigurationImpl getCachedConfiguration( String pid )
    {
        synchronized ( configurations )
        {
            return ( ConfigurationImpl ) configurations.get( pid );
        }
    }


    Iterator getCachedConfigurations()
    {
        synchronized ( configurations )
        {
            return configurations.values().iterator();
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


    // ---------- ConfigurationAdminImpl support -------------------------------

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
        updateThread.schedule( new DeleteConfiguration( config ) );
    }


    void updated( ConfigurationImpl config )
    {
        updateThread.schedule( new UpdateConfiguration( config ) );
    }


    void fireConfigurationEvent( int type, String pid, String factoryPid )
    {

        updateThread.schedule( new FireConfigurationEvent( type, pid, factoryPid) );
    }


    // ---------- BundleListener -----------------------------------------------

    public void bundleChanged( BundleEvent event )
    {
        if ( event.getType() == BundleEvent.UNINSTALLED )
        {
            String location = event.getBundle().getLocation();

            try
            {
                PersistenceManager[] pmList = getPersistenceManagers();
                for ( int i = 0; i < pmList.length; i++ )
                {
                    Enumeration configs = pmList[i].getDictionaries();
                    while ( configs.hasMoreElements() )
                    {
                        Dictionary config = ( Dictionary ) configs.nextElement();

                        String pid = ( String ) config.get( Constants.SERVICE_PID );
                        if ( pid != null )
                        {
                            ConfigurationImpl cfg = getCachedConfiguration( pid );
                            if ( cfg == null )
                            {
                                cfg = new ConfigurationImpl( this, pmList[i], config );
                            }

                            if ( location.equals( cfg.getBundleLocation() ) )
                            {
                                cfg.setBundleLocation( null );
                            }
                        }
                        else
                        {

                            Factory factory = Factory.getFactory( pmList[i], config );
                            if ( factory != null )
                            {
                                Factory cachedFactory = getCachedFactory( factory.getFactoryPid() );
                                if ( cachedFactory != null )
                                {
                                    factory = cachedFactory;
                                }

                                if ( location.equals( factory.getBundleLocation() ) )
                                {
                                    factory.setBundleLocation( null );
                                }
                            }
                        }
                    }
                }

            }
            catch ( Exception e )
            {
                log( LogService.LOG_WARNING, "Problem unbinding configurations for bundle " + location, e );
            }
        }
    }


    // ---------- internal -----------------------------------------------------

    private PersistenceManager[] getPersistenceManagers()
    {
        int currentPmtCount = persistenceManagerTracker.getTrackingCount();
        if ( persistenceManagers == null || currentPmtCount > pmtCount )
        {

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
                pm = new PersistenceManager[pms.size()];
                int pmIndex = 0;
                for ( Iterator pi = pms.iterator(); pi.hasNext(); pmIndex++)
                {
                    ServiceReference ref = ( ServiceReference ) pi.next();
                    pm[pmIndex] = ( PersistenceManager ) persistenceManagerTracker.getService( ref );
                }
            }

            pmtCount = currentPmtCount;
            persistenceManagers = pm;
        }

        return persistenceManagers;
    }


    private void configure( ServiceReference sr, ManagedService service )
    {
        String pid = ( String ) sr.getProperty( Constants.SERVICE_PID );
        if ( pid != null )
        {
            ManagedServiceUpdate update = new ManagedServiceUpdate( pid, sr, service );
            updateThread.schedule( update );
        }

    }


    private void configure( ServiceReference sr, ManagedServiceFactory service )
    {
        String pid = ( String ) sr.getProperty( Constants.SERVICE_PID );
        if ( pid != null )
        {
            ManagedServiceFactoryUpdate update = new ManagedServiceFactoryUpdate( pid, sr, service );
            updateThread.schedule( update );
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
     *            <code>null</code> if the new configuration object belongs to
     *            a factory. The configuration object will not be persisted if
     *            this parameter is not <code>null</code>.
     * @param bundleLocation
     *            The bundle location of the bundle to which the configuration
     *            belongs or <code>null</code> if the configuration is not
     *            bound yet.
     * @return The new configuration object
     * @throws IOException
     *             May be thrown if an error occurrs persisting the new
     *             configuration object.
     */
    ConfigurationImpl createConfiguration( String pid, String factoryPid, String bundleLocation ) throws IOException
    {
        return new ConfigurationImpl( this, getPersistenceManagers()[0], pid, factoryPid, bundleLocation );
    }


    Factory getCachedFactory( String factoryPid )
    {
        synchronized ( factories )
        {
            return ( Factory ) factories.get( factoryPid );
        }
    }


    void cacheFactory( Factory factory )
    {
        synchronized ( factories )
        {
            factories.put( factory.getFactoryPid(), factory );
        }
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
                factory = Factory.load( pmList[i], factoryPid );
                cacheFactory( factory );
                return factory;
            }
        }

        // if getting here, there is no configuration yet, optionally create new
        return createFactory( factoryPid );
    }


    Factory createFactory( String factoryPid )
    {
        Factory factory = new Factory( getPersistenceManagers()[0], factoryPid );
        cacheFactory( factory );
        return factory;
    }


    /**
     * Calls the registered configuration plugins on the given configuration
     * object unless the configuration has just been created and not been
     * updated yet.
     *
     * @param sr The service reference of the managed service (factory) which
     *            is to be updated with configuration
     * @param cfg The configuration object whose properties have to be passed
     *            through the plugins
     * @return The properties from the configuration object passed through the
     *         plugins or <code>null</code> if the configuration object has
     *         been newly created and no properties exist yet.
     */
    private Dictionary callPlugins( ServiceReference sr, ConfigurationImpl cfg )
    {
        Dictionary props = cfg.getProperties();

        // guard against NPE for new configuration never updated
        if (props == null) {
            return null;
        }

        ServiceReference[] plugins = null;
        try
        {
            String pid = ( String ) sr.getProperty( Constants.SERVICE_PID );
            String filter = "(|(!(cm.target=*))(cm.target=" + pid + "))";
            plugins = bundleContext.getServiceReferences( ConfigurationPlugin.class.getName(), filter );

        }
        catch ( InvalidSyntaxException ise )
        {
            // no filter, no exception ...
        }

        // abort early if there are no plugins
        if ( plugins == null || plugins.length == 0 )
        {
            return props;
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
                log( LogService.LOG_ERROR, "Unexpected problem calling" + " configuration plugin", t );
            }
            finally
            {
                // ensure ungetting the plugin
                bundleContext.ungetService( pluginRef );
            }
            cfg.setAutoProperties( props, false );
        }

        return props;
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
        Object log = logTracker.getService();
        if ( log != null )
        {
            ( ( LogService ) log ).log( configurationAdminReference, level, message, t );
            return;
        }

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

    // ---------- inner classes ------------------------------------------------

    private class ManagedServiceUpdate implements Runnable
    {
        private String pid;

        private ServiceReference sr;

        private ManagedService service;


        ManagedServiceUpdate( String pid, ServiceReference sr, ManagedService service )
        {
            this.pid = pid;
            this.sr = sr;
            this.service = service;
        }


        public void run()
        {
            // get or load configuration for the pid
            ConfigurationImpl cfg;
            try
            {
                cfg = getExistingConfiguration( pid );
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Error loading configuration for " + pid, ioe );
                return;
            }

            // this will be set below to be given to the service
            Dictionary dictionary;

            // check configuration and call plugins if existing and not new
            if ( cfg != null && !cfg.isNew() )
            {

                if ( cfg.isDelivered() )
                {
                    log( LogService.LOG_DEBUG, "Configuration " + pid + " has already been delivered", null );
                    return;
                }
                
                // 104.3 Ignore duplicate PIDs from other bundles and report
                // them to the log
                // 104.4.1 No update call back for PID already bound to another
                // bundle location
                // 104.4.1 assign configuration to bundle if unassigned
                String bundleLocation = sr.getBundle().getLocation();
                if ( cfg.getBundleLocation() == null )
                {
                    cfg.setBundleLocation( bundleLocation );
                }
                else if ( !bundleLocation.equals( cfg.getBundleLocation() ) )
                {
                    log( LogService.LOG_ERROR, "Cannot use configuration for " + pid + " requested by bundle "
                        + sr.getBundle().getLocation() + " but belongs to " + cfg.getBundleLocation(), null );
                    return;
                }

                // 104.3 Report an error in the log if more than one service
                // with
                // the same PID asks for the configuration
                if ( cfg.getServiceReference() != null && !sr.equals( cfg.getServiceReference() ) )
                {
                    log( LogService.LOG_ERROR, "Configuration for " + pid + " has already been used for service "
                        + cfg.getServiceReference() + " and will now also be given to " + sr, null );
                }
                else
                {
                    // assign the configuration to the service
                    cfg.setServiceReference( sr );
                }

                // prepare the configuration for the service (call plugins)
                dictionary = callPlugins( sr, cfg );
            }
            else
            {
                // 104.5.3 ManagedService.updated must be called with null
                // if no configuration is available
                dictionary = null;
            }

            // update the service with the configuration
            try
            {
                service.updated( dictionary );

                // if there is nothing to set, don't
                if ( cfg != null )
                {
                    cfg.setDelivered( true );
                }
            }
            catch ( ConfigurationException ce )
            {
                if ( ce.getProperty() != null )
                {
                    log( LogService.LOG_ERROR, sr + ": Updating configuration property " + ce.getProperty()
                        + " caused a problem: " + ce.getReason(), ce );
                }
                else
                {
                    log( LogService.LOG_ERROR, sr + ": Updating configuration caused a problem: " + ce.getReason(), ce );

                }
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, sr + ": Unexpected problem updating configuration", t );
            }
        }
        
        public String toString()
        {
            return "ManagedService Update: pid=" + pid;
        }
    }

    private class ManagedServiceFactoryUpdate implements Runnable
    {
        private String factoryPid;

        private ServiceReference sr;

        private ManagedServiceFactory service;


        ManagedServiceFactoryUpdate( String factoryPid, ServiceReference sr, ManagedServiceFactory service )
        {
            this.factoryPid = factoryPid;
            this.sr = sr;
            this.service = service;
        }


        public void run()
        {
            Factory factory;
            try
            {
                factory = getFactory( factoryPid );
            }
            catch ( IOException ioe )
            {
                log( LogService.LOG_ERROR, "Cannot get factory mapping for factory PID " + factoryPid, ioe );
                return;
            }

            String bundleLocation = sr.getBundle().getLocation();
            if ( factory.getBundleLocation() == null )
            {
                // bind to the location of the service if unbound
                factory.setBundleLocation( bundleLocation );
            }
            else if ( !bundleLocation.equals( factory.getBundleLocation() ) )
            {
                // factory PID is bound to another bundle
                log( LogService.LOG_ERROR, "Cannot use Factory configuration for " + factoryPid
                    + " requested by bundle " + sr.getBundle().getLocation() + " but belongs to "
                    + factory.getBundleLocation(), null );
                return;
            }

            Set pids = factory.getPIDs();

            for ( Iterator pi = pids.iterator(); pi.hasNext(); )
            {
                String pid = ( String ) pi.next();
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

                // do not re-updated unmodified configuration
                if ( cfg.isDelivered() )
                {
                    log( LogService.LOG_DEBUG, "Configuration " + pid + " has already been updated", null );
                    continue;
                }
                
                // check bundle location of configuration
                if ( cfg.getBundleLocation() == null )
                {
                    // bind to the location of the service if unbound
                    cfg.setBundleLocation( bundleLocation );
                }
                else if ( !bundleLocation.equals( cfg.getBundleLocation() ) )
                {
                    // configuration is bound to another bundle
                    log( LogService.LOG_ERROR, "Configuration " + pid + " (factory " + factoryPid
                        + ") belongs to bundle " + cfg.getBundleLocation() + " but was requested for bundle "
                        + bundleLocation, null );
                    continue;
                }

                // prepare the configuration for the service (call plugins)
                Dictionary dictionary = callPlugins( sr, cfg );

                // update the service with the configuration
                try
                {
                    // only, if there is non-null configuration data
                    if ( dictionary != null )
                    {
                        log( LogService.LOG_DEBUG, sr + ": Updating configuration pid=" + pid, null );
                        service.updated( pid, dictionary );
                        cfg.setDelivered( true );
                    }
                }
                catch ( ConfigurationException ce )
                {
                    if ( ce.getProperty() != null )
                    {
                        log( LogService.LOG_ERROR, sr + ": Updating configuration property " + ce.getProperty()
                            + " caused a problem: " + ce.getReason(), ce );
                    }
                    else
                    {
                        log( LogService.LOG_ERROR, sr + ": Updating configuration caused a problem: " + ce.getReason(),
                            ce );

                    }
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_ERROR, sr + ": Unexpected problem updating configuration", t );
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

        private ConfigurationImpl config;


        UpdateConfiguration( ConfigurationImpl config )
        {
            this.config = config;
        }


        public void run()
        {
            if ( config.isDelivered() )
            {
                log( LogService.LOG_DEBUG, "Configuration " + config.getPid() + " has already been updated", null );
                return;
            }
            
            try
            {
                if ( config.getFactoryPid() == null )
                {
                    ServiceReference[] sr = bundleContext.getServiceReferences( ManagedService.class.getName(), "("
                        + Constants.SERVICE_PID + "=" + config.getPid() + ")" );
                    if ( sr != null && sr.length > 0 )
                    {
                        ManagedService srv = ( ManagedService ) bundleContext.getService( sr[0] );
                        try
                        {
                            // bind the configuration, fail if bound to another
                            // bundle !!
                            // check bundle location of configuration
                            String bundleLocation = sr[0].getBundle().getLocation();
                            if ( config.getBundleLocation() == null )
                            {
                                // bind to the location of the service if
                                // unbound
                                config.setBundleLocation( bundleLocation );
                            }
                            else if ( !bundleLocation.equals( config.getBundleLocation() ) )
                            {
                                // configuration is bound to another bundle
                                log( LogService.LOG_ERROR, "Configuration " + config.getPid() + " belongs to bundle "
                                    + config.getBundleLocation() + " but was requested for bundle " + bundleLocation,
                                    null );
                                return;
                            }

                            // prepare the configuration for the service (call plugins)
                            Dictionary dictionary = callPlugins( sr[0], config );

                            // update the ManagedService with the properties
                            srv.updated( dictionary );
                            config.setDelivered( true );
                        }
                        finally
                        {
                            bundleContext.ungetService( sr[0] );
                        }
                    }
                }
                else
                {
                    ServiceReference[] sr = bundleContext.getServiceReferences( ManagedServiceFactory.class.getName(),
                        "(" + Constants.SERVICE_PID + "=" + config.getFactoryPid() + ")" );
                    if ( sr != null && sr.length > 0 )
                    {
                        ManagedServiceFactory srv = ( ManagedServiceFactory ) bundleContext.getService( sr[0] );
                        try
                        {
                            // bind the configuration, fail if bound to another
                            // bundle !!
                            // check bundle location of configuration
                            String bundleLocation = sr[0].getBundle().getLocation();
                            if ( config.getBundleLocation() == null )
                            {
                                // bind to the location of the service if
                                // unbound
                                config.setBundleLocation( bundleLocation );
                            }
                            else if ( !bundleLocation.equals( config.getBundleLocation() ) )
                            {
                                // configuration is bound to another bundle
                                log( LogService.LOG_ERROR, "Configuration " + config.getPid() + " (factory "
                                    + config.getFactoryPid() + ") belongs to bundle " + config.getBundleLocation()
                                    + " but was requested for bundle " + bundleLocation, null );
                                return;
                            }


                            // prepare the configuration for the service (call plugins)
                            Dictionary dictionary = callPlugins( sr[0], config );

                            // update the ManagedServiceFactory with the properties
                            // only, if there is non-null configuration data
                            if ( dictionary != null )
                            {
                                srv.updated( config.getPid(), dictionary );
                                config.setDelivered( true );
                            }
                        }
                        finally
                        {
                            bundleContext.ungetService( sr[0] );
                        }
                    }
                }
            }
            catch ( ConfigurationException ce )
            {
                if ( ce.getProperty() != null )
                {
                    log( LogService.LOG_ERROR, "Updating configuration property " + ce.getProperty()
                        + " caused a problem: " + ce.getReason(), ce );
                }
                else
                {
                    log( LogService.LOG_ERROR, "Updating configuration caused a problem: " + ce.getReason(), ce );

                }
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Unexpected problem updating configuration", t );
            }

            fireConfigurationEvent( ConfigurationEvent.CM_UPDATED, config.getPid(), config.getFactoryPid() );
        }
        
        public String toString()
        {
            return "Update: pid=" + config.getPid();
        }
    }

    private class DeleteConfiguration implements Runnable
    {

        private ConfigurationImpl config;
        private String pid;
        private String factoryPid;


        DeleteConfiguration( ConfigurationImpl config )
        {
            this.config = config;
            this.pid = config.getPid();
            this.factoryPid = config.getFactoryPid();
        }


        public void run()
        {
            if ( config.isDelivered() )
            {
                log( LogService.LOG_DEBUG, "Deletion of configuration " + pid + " has already been delivered", null );
                return;
            }
            
            try
            {
                if ( factoryPid == null )
                {
                    ServiceReference[] sr = bundleContext.getServiceReferences( ManagedService.class.getName(), "("
                        + Constants.SERVICE_PID + "=" + pid + ")" );
                    if ( sr != null && sr.length > 0 )
                    {
                        ManagedService srv = ( ManagedService ) bundleContext.getService( sr[0] );
                        try
                        {
                            srv.updated( null );
                            config.setDelivered( true );
                        }
                        finally
                        {
                            bundleContext.ungetService( sr[0] );
                        }
                    }
                }
                else
                {
                    // remove the pid from the factory
                    Factory factory = getFactory( factoryPid );
                    factory.removePID( pid );
                    factory.store();

                    ServiceReference[] sr = bundleContext.getServiceReferences( ManagedServiceFactory.class.getName(),
                        "(" + Constants.SERVICE_PID + "=" + factoryPid + ")" );
                    if ( sr != null && sr.length > 0 )
                    {
                        ManagedServiceFactory srv = ( ManagedServiceFactory ) bundleContext.getService( sr[0] );
                        try
                        {
                            srv.deleted( pid );
                            config.setDelivered( true );
                        }
                        finally
                        {
                            bundleContext.ungetService( sr[0] );
                        }
                    }
                }
            }
            catch ( ConfigurationException ce )
            {
                if ( ce.getProperty() != null )
                {
                    log( LogService.LOG_ERROR, "Updating configuration property " + ce.getProperty()
                        + " caused a problem: " + ce.getReason(), ce );
                }
                else
                {
                    log( LogService.LOG_ERROR, "Updating configuration caused a problem: " + ce.getReason(), ce );

                }
            }
            catch ( Throwable t )
            {
                log( LogService.LOG_ERROR, "Unexpected problem updating configuration", t );
            }

            fireConfigurationEvent( ConfigurationEvent.CM_DELETED, pid, factoryPid );
        }
        
        public String toString()
        {
            return "Delete: pid=" + pid;
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

            ConfigurationEvent event = new ConfigurationEvent( configurationAdminReference, type, factoryPid, pid);

            for ( int i = 0; i < srs.length; i++ )
            {
                ConfigurationListener cl = ( ConfigurationListener ) configurationListenerTracker.getService( srs[i] );
                try
                {
                    cl.configurationEvent( event );
                }
                catch ( Throwable t )
                {
                    log( LogService.LOG_ERROR, "Unexpected problem delivery configuration event to " + srs[i], t );
                }
            }
        }
        
        public String toString()
        {
            return "Fire ConfigurationEvent: pid=" + pid;
        }
    }

    private static abstract class AbstractManagedServiceTracker extends ServiceTracker
    {
        protected final ConfigurationManager cm;

        AbstractManagedServiceTracker( ConfigurationManager cm, String className )
        {
            super( cm.bundleContext, className, null );
            this.cm = cm;
            open();
        }


        public void removedService( ServiceReference reference, Object service )
        {
            // check whether we can take back the configuration object
            String pid = ( String ) reference.getProperty( Constants.SERVICE_PID );
            if ( pid != null )
            {
                ConfigurationImpl cfg = cm.getCachedConfiguration( pid );
                if ( cfg != null && reference.equals( cfg.getServiceReference() ) )
                {
                    cfg.setServiceReference( null );
                    cfg.setDelivered( false );
                }
            }

            super.removedService( reference, service );
        }
    }

    private static class ManagedServiceTracker extends AbstractManagedServiceTracker
    {

        ManagedServiceTracker(ConfigurationManager cm)
        {
            super( cm, ManagedService.class.getName() );
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
    }

    private static class ManagedServiceFactoryTracker extends AbstractManagedServiceTracker
    {
        ManagedServiceFactoryTracker(ConfigurationManager cm)
        {
            super( cm, ManagedServiceFactory.class.getName() );
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
            String factoryPid = ( String ) reference.getProperty( Constants.SERVICE_PID );
            if ( factoryPid != null )
            {
                Factory factory = cm.getCachedFactory( factoryPid );
                if ( factory != null )
                {
                    for ( Iterator pi = factory.getPIDs().iterator(); pi.hasNext(); )
                    {
                        String pid = ( String ) pi.next();
                        ConfigurationImpl cfg = cm.getCachedConfiguration( pid );
                        if ( cfg != null )
                        {
                            cfg.setDelivered( false );
                        }
                    }
                }
            }

            super.removedService( reference, service );
        }

    }
}
