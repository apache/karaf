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
package org.apache.felix.das;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.felix.das.util.DriverLoader;
import org.apache.felix.das.util.DriverMatcher;
import org.apache.felix.das.util.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.device.DriverLocator;
import org.osgi.service.device.DriverSelector;
import org.osgi.service.device.Match;
import org.osgi.service.log.LogService;


/**
 * TODO: add javadoc
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DeviceManager implements Log
{

    private final long DEFAULT_TIMEOUT_SEC = 1;

    // the logger
    private LogService m_log;

    // the bundle context
    private final BundleContext m_context;

    // the driver selector
    private DriverSelector m_selector;

    // the driver locators
    private List<DriverLocator> m_locators;

    // the devices
    private Map<ServiceReference, Object> m_devices;

    // the drivers
    private Map<ServiceReference, DriverAttributes> m_drivers;

    // performs all the background actions
    private ExecutorService m_worker;

    // used to add delayed actions
    private ScheduledExecutorService m_delayed;

    private Filter m_deviceImplFilter;

    private Filter m_driverImplFilter;


    public DeviceManager( BundleContext context )
    {
        m_context = context;
    }


    public void debug( String message )
    {
        m_log.log( LogService.LOG_DEBUG, message );
    }


    public void info( String message )
    {
        m_log.log( LogService.LOG_INFO, message );
    }


    public void warning( String message )
    {
        m_log.log( LogService.LOG_WARNING, message );
    }


    public void error( String message, Throwable e )
    {
        System.err.println( message );
        if ( e != null )
        {
            e.printStackTrace();
        }
        m_log.log( LogService.LOG_ERROR, message, e );
    }


    // dependency manager methods
    @SuppressWarnings("unused")
    private void init() throws InvalidSyntaxException
    {
        m_locators = Collections.synchronizedList( new ArrayList<DriverLocator>() );
        m_worker = Executors.newSingleThreadExecutor( new NamedThreadFactory( "Apache Felix Device Manager" ) );
        m_delayed = Executors.newScheduledThreadPool( 1, new NamedThreadFactory(
            "Apache Felix Device Manager - delayed" ) );
        m_deviceImplFilter = Util.createFilter( "(%s=%s)", new Object[]
            { org.osgi.framework.Constants.OBJECTCLASS, Device.class.getName() } );
        m_driverImplFilter = Util.createFilter( "(%s=%s)", new Object[]
            { org.osgi.framework.Constants.OBJECTCLASS, Driver.class.getName() } );
    }


    @SuppressWarnings("unused")
    private void start()
    {
        m_drivers = new HashMap<ServiceReference, DriverAttributes>();
        m_devices = new HashMap<ServiceReference, Object>();
        submit( new WaitForStartFramework() );
    }


    public void stop()
    {
        // nothing to do ?
    }


    public void destroy()
    {
        m_worker.shutdownNow();
        m_delayed.shutdownNow();
    }


    // callback methods

    public void locatorAdded( DriverLocator locator )
    {
        m_locators.add( locator );
        debug( "driver locator appeared" );
    }


    public void locatorRemoved( DriverLocator locator )
    {
        m_locators.remove( locator );
        debug( "driver locator lost" );
    }


    public void driverAdded( ServiceReference ref, Object obj )
    {
        final Driver driver = Driver.class.cast( obj );
        m_drivers.put( ref, new DriverAttributes( ref, driver ) );

        debug( "driver appeared: " + Util.showDriver( ref ) );
    }


    public void driverRemoved( ServiceReference ref )
    {
        String driverId = String.class.cast( ref.getProperty( Constants.DRIVER_ID ) );
        debug( "driver lost: " + Util.showDriver( ref ) );
        m_drivers.remove( driverId );

        // check if devices have become idle
        // after some time
        schedule( new CheckForIdleDevices() );

    }


    public void deviceAdded( ServiceReference ref, Object device )
    {
        m_devices.put( ref, device );
        debug( "device appeared: " + Util.showDevice( ref ) );
        submit( new DriverAttachAlgorithm( ref, device ) );
    }


    public void deviceModified( ServiceReference ref, Object device )
    {
        debug( "device modified: " + Util.showDevice( ref ) );
        // nothing further to do ?
        // DeviceAttributes da = m_devices.get(ref);
        // submit(new DriverAttachAlgorithm(da));
    }


    public void deviceRemoved( ServiceReference ref )
    {
        debug( "device removed: " + Util.showDevice( ref ) );
        m_devices.remove( ref );
        // nothing further to do ?
        // the services that use this
        // device should track it.
    }


    /**
     * perform this task as soon as possible.
     * 
     * @param task
     *            the task
     */
    private void submit( Callable<Object> task )
    {
        m_worker.submit( new LoggedCall( task ) );
    }


    /**
     * perform this task after the default delay.
     * 
     * @param task
     *            the task
     */
    private void schedule( Callable<Object> task )
    {
        m_delayed.schedule( new DelayedCall( task ), DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS );
    }

    // worker callables

    /**
     * Callable used to start the DeviceManager. It either waits (blocking the
     * worker thread) for the framework to start, or if it has already started,
     * returns immediately, freeing up the worker thread.
     * 
     * @author dennisg
     * 
     */
    private class WaitForStartFramework implements Callable<Object>, FrameworkListener
    {

        private final CountDownLatch m_latch = new CountDownLatch( 1 );


        public Object call() throws Exception
        {
            boolean addedAsListener = false;
            if ( m_context.getBundle( 0 ).getState() == Bundle.ACTIVE )
            {
                m_latch.countDown();
                debug( "Starting Device Manager immediately" );
            }
            else
            {
                m_context.addFrameworkListener( this );
                addedAsListener = true;
                debug( "Waiting for framework to start" );
            }

            m_latch.await();
            for ( Map.Entry<ServiceReference, Object> entry : m_devices.entrySet() )
            {
                submit( new DriverAttachAlgorithm( entry.getKey(), entry.getValue() ) );
            }
            // cleanup
            if ( addedAsListener )
            {
                m_context.removeFrameworkListener( this );
            }
            return null;
        }


        // FrameworkListener method
        public void frameworkEvent( FrameworkEvent event )
        {
            switch ( event.getType() )
            {
                case FrameworkEvent.STARTED:
                    debug( "Framework has started" );
                    m_latch.countDown();
                    break;
            }
        }


        @Override
        public String toString()
        {
            return getClass().getSimpleName();
        }
    }

    private class LoggedCall implements Callable<Object>
    {

        private final Callable<Object> m_call;


        public LoggedCall( Callable<Object> call )
        {
            m_call = call;
        }


        private String getName()
        {
            return m_call.getClass().getSimpleName();
        }


        public Object call() throws Exception
        {

            try
            {
                return m_call.call();
            }
            catch ( Exception e )
            {
                error( "call failed: " + getName(), e );
                throw e;
            }
            catch ( Throwable e )
            {
                error( "call failed: " + getName(), e );
                throw new RuntimeException( e );
            }
        }

    }

    private class DelayedCall implements Callable<Object>
    {

        private final Callable<Object> m_call;


        public DelayedCall( Callable<Object> call )
        {
            m_call = call;
        }


        private String getName()
        {
            return m_call.getClass().getSimpleName();
        }


        public Object call() throws Exception
        {
            info( "Delayed call: " + getName() );
            return m_worker.submit( m_call );
        }
    }

    /**
     * Checks for Idle devices, and attaches them
     * 
     * @author dennisg
     * 
     */
    private class CheckForIdleDevices implements Callable<Object>
    {

        public Object call() throws Exception
        {
            debug( "START - check for idle devices" );
            for ( ServiceReference ref : getIdleDevices() )
            {
                info( "IDLE: " + ref.getBundle().getSymbolicName() );
                submit( new DriverAttachAlgorithm( ref, m_devices.get( ref ) ) );
            }

            submit( new IdleDriverUninstallAlgorithm() );
            debug( "STOP - check for idle devices" );
            return null;
        }


        /**
         * get a list of all idle devices.
         * 
         * @return
         */
        private List<ServiceReference> getIdleDevices()
        {
            List<ServiceReference> list = new ArrayList<ServiceReference>();

            for ( ServiceReference ref : m_devices.keySet() )
            {
                info( "checking if idle: " + ref.getBundle().getSymbolicName() );

                final Bundle[] usingBundles = ref.getUsingBundles();
                for ( Bundle bundle : usingBundles )
                {
                    if ( isDriverBundle( bundle ) )
                    {
                        info( "used by driver: " + bundle.getSymbolicName() );
                        debug( "not idle: " + ref.getBundle().getSymbolicName() );
                        break;
                    }
                    
                    list.add( ref );

                }
            }
            return list;
        }
    }


    private boolean isDriverBundle( Bundle bundle )
    {
        ServiceReference[] refs = bundle.getRegisteredServices();
        for ( ServiceReference ref : refs )
        {
            if ( m_driverImplFilter.match( ref ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 
     * Used to uninstall unused drivers
     * 
     * @author dennisg
     * 
     */
    private class IdleDriverUninstallAlgorithm implements Callable<Object>
    {

        public Object call() throws Exception
        {

            info( "cleaning driver cache" );
            for ( DriverAttributes da : m_drivers.values() )
            {
                // just call the tryUninstall; the da itself
                // will know if it should really uninstall the driver.
                da.tryUninstall();
            }

            return null;
        }
    }

    private class DriverAttachAlgorithm implements Callable<Object>
    {

        private final ServiceReference m_ref;

        private final Device m_device;

        private List<DriverAttributes> m_included;

        private List<DriverAttributes> m_excluded;

        private final DriverLoader m_driverLoader;

        private DriverAttributes m_finalDriver;


        public DriverAttachAlgorithm( ServiceReference ref, Object obj )
        {
            m_ref = ref;
            if ( m_deviceImplFilter.match( ref ) )
            {
                m_device = Device.class.cast( obj );
            }
            else
            {
                m_device = null;
            }

            m_driverLoader = new DriverLoader( DeviceManager.this, m_context );
        }


        @SuppressWarnings("all")
        private Dictionary createDictionary( ServiceReference ref )
        {
            final Properties p = new Properties();

            for ( String key : ref.getPropertyKeys() )
            {
                p.put( key, ref.getProperty( key ) );
            }
            return p;
        }


        @SuppressWarnings("all")
        public Object call() throws Exception
        {
            info( "finding suitable driver for: " + Util.showDevice( m_ref ) );

            final Dictionary dict = createDictionary( m_ref );

            // first create a copy of all the drivers that are already there.
            // during the process, drivers will be added, but also excluded.
            m_included = new ArrayList<DriverAttributes>( m_drivers.values() );
            m_excluded = new ArrayList<DriverAttributes>();

            // first find matching driver bundles
            // if there are no driver locators
            // we'll have to do with the drivers that where
            // added 'manually'
            List<String> driverIds = m_driverLoader.findDrivers( m_locators, dict );

            // remove the driverIds that are already available
            for ( DriverAttributes da : m_drivers.values() )
            {
                driverIds.remove( da.getDriverId() );
            }
            driverIds.removeAll( m_drivers.keySet() );
            try
            {
                return driverAttachment( dict, driverIds.toArray( new String[0] ) );
            }
            finally
            {
                // unload loaded drivers
                // that were unnecessarily loaded
                m_driverLoader.unload( m_finalDriver );
            }
        }


        @SuppressWarnings("all")
        private Object driverAttachment( Dictionary dict, String[] driverIds ) throws Exception
        {
            m_finalDriver = null;

            // remove the excluded drivers
            m_included.removeAll( m_excluded );

            // now load the drivers
            List<ServiceReference> driverRefs = m_driverLoader.loadDrivers( m_locators, driverIds );
            // these are the possible driver references that have been added
            // add the to the list of included drivers
            for ( ServiceReference serviceReference : driverRefs )
            {
                DriverAttributes da = m_drivers.get( serviceReference );
                if ( da != null )
                {
                    m_included.add( da );
                }
            }

            // now start matching all drivers
            final DriverMatcher mi = new DriverMatcher( DeviceManager.this );

            for ( DriverAttributes driver : m_included )
            {
                try
                {
                    int match = driver.match( m_ref );
                    if ( match <= Device.MATCH_NONE )
                        continue;
                    mi.add( match, driver );
                }
                catch ( Throwable t )
                {
                    error( "match threw an exception", new Exception( t ) );
                }
            }

            // get the best match
            Match bestMatch;

            // local copy
            final DriverSelector selector = m_selector;
            if ( selector != null )
            {
                bestMatch = mi.selectBestMatch( m_ref, selector );
            }
            else
            {
                bestMatch = mi.getBestMatch();
            }

            if ( bestMatch == null )
            {
                noDriverFound();
                // really return
                return null;
            }

            String driverId = String.class.cast( bestMatch.getDriver().getProperty( Constants.DRIVER_ID ) );

            debug( "best match: " + driverId );
            m_finalDriver = m_drivers.get( bestMatch.getDriver() );

            if ( m_finalDriver == null )
            {
                error( "we found a driverId, but not the corresponding driver: " + driverId, null );
                noDriverFound();
                return null;
            }

            // here we get serious...
            try
            {
                debug( "attaching to: " + driverId );
                String newDriverId = m_finalDriver.attach( m_ref );
                if ( newDriverId == null )
                {
                    // successful attach
                    return null;
                }
                // its a referral
                info( "attach led to a referral to: " + newDriverId );
                m_excluded.add( m_finalDriver );
                return driverAttachment( dict, new String[]
                    { newDriverId } );
            }
            catch ( Throwable t )
            {
                error( "attach failed due to an exception", t );
            }
            m_excluded.add( m_finalDriver );
            return driverAttachment( dict, driverIds );
        }


        private void noDriverFound()
        {
            debug( "no suitable driver found for: " + Util.showDevice( m_ref ) );
            if ( m_device != null )
            {
                m_device.noDriverFound();
            }
        }


        @Override
        public String toString()
        {
            return getClass().getSimpleName();// + ": " +
            // Util.showDevice(m_ref);
        }

    }
}
