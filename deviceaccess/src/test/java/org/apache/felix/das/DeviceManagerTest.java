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


import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

import org.apache.felix.das.util.DriverLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.device.Device;
import org.osgi.service.device.Driver;
import org.osgi.service.device.DriverLocator;
import org.osgi.service.device.DriverSelector;
import org.osgi.service.device.Match;
import org.osgi.service.log.LogService;



/**
 * Test the actual implementation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DeviceManagerTest
{

	@Mock
    private DeviceManager m_manager;

	@Mock Bundle m_systemBundle;
	
	@Mock
	private LogService m_log;
	
	
	private BundleContext m_context;
	
	
    private OSGiMock m_osgi;


    @Before
    public void setUp() throws Exception
    {
        m_osgi = new OSGiMock();

    	MockitoAnnotations.initMocks(this);
    	
    	m_context = m_osgi.getBundleContext();
    	
        m_manager = new DeviceManager( m_context );
        
        Utils.invoke( m_manager, "init" );
        
        Utils.inject( m_manager, LogService.class, m_log );
        
        Mockito.when( m_context.getBundle( 0 ) ).thenReturn( m_systemBundle );

        final CountDownLatch latch = new CountDownLatch( 1 );

        Answer<Integer> answer = new Answer<Integer>()
        {
        	public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return Bundle.ACTIVE;
            }
        };

        Mockito.when( m_systemBundle.getState() ).thenAnswer( answer );

        Utils.invoke( m_manager, "start" );
        latch.await( 5, TimeUnit.SECONDS );

        Mockito.when( m_context.installBundle(Mockito.isA( String.class ), ( InputStream ) Mockito.isNull() ) )
        	.thenThrow(new NullPointerException( "inputstream is null exception" ) );
    }


    @After
    public void tearDown() throws Exception
    {
        Utils.invoke( m_manager, "stop" );
        Utils.invoke( m_manager, "destroy" );
    }

    private Driver tstCreateDriver( String driverId, int match ) throws Exception
    {
        Properties p = new Properties();
        p.put( Constants.DRIVER_ID, driverId );
        p.put( "match", Integer.toString( match ) );

        return tstCreateDriver( p );
    }


    private Driver tstCreateDriver( Properties p ) throws Exception
    {
        Driver driver = Mockito.mock( Driver.class );
        
        ServiceReference ref = m_osgi.registerService( 
        		new String[]{ Driver.class.getName() }, driver, p );

        MatchAnswer answer = new MatchAnswer( ref );

        Mockito.when( driver.match( Mockito.isA( ServiceReference.class ) ) )
        	.thenAnswer( answer );
        
        Bundle bundle = m_osgi.getBundle( ref );
        Mockito.when( bundle.getLocation() )
        	.thenReturn( 
            DriverLoader.DRIVER_LOCATION_PREFIX  + p.getProperty( Constants.DRIVER_ID ));
        
        return driver;
    }


    private Device tstCreateDevice( String[] cat )
    {
        return tstCreateDevice( cat, true );
    }


    private Device tstCreateDevice( String[] cat, boolean isDevice )
    {
        Properties p = new Properties();
        p.put( Constants.DEVICE_CATEGORY, cat );
        if ( isDevice )
        {
            return ( Device ) tstCreateService( p, Device.class );
        }
        return tstCreateService( p, Object.class );
    }


    @SuppressWarnings("unchecked")
    private <T> T tstCreateService( Properties p, Class<?> iface )
    {
        T svc = ( T ) Mockito.mock( iface, iface.getSimpleName() );
        
        m_osgi.registerService( new String[]
            { iface.getName() }, svc, p );
        return svc;
    }


    /**
     * 
     * prepared all expected behavior for the installation of a dynamic driver
     * bundle based on an acquired InputStream.
     * 
     * 
     * @param driverId
     * @param match
     * @param in
     * @return
     * @throws BundleException
     * @throws Exception
     */
    private Driver tstExpectInstallDriverBundle( String driverId, int match, InputStream in ) throws BundleException,
        Exception
    {

        Bundle bundle = Mockito.mock( Bundle.class, "driverBundle" );
        Mockito.when( m_context.installBundle( 
        		Mockito.eq( "_DD_" + driverId ), Mockito.eq( in ) ) )
        		.thenReturn( bundle );

        final Driver driver = tstCreateDriver( driverId, match );
        final ServiceReference driverRef = m_osgi.getReference( driver );

        Answer<Object> answer = new Answer<Object>()
        {
        	
        	public Object answer(InvocationOnMock invocation) throws Throwable
            {
                m_manager.driverAdded( driverRef, driver );
                return null;
            }
        };

        //bundle start leads to the addition of the driver to
        //the device manager.
        Mockito.doAnswer(answer).when(bundle).start();

        Mockito.when( bundle.getRegisteredServices() )
        	.thenReturn( new ServiceReference[]{ driverRef } );

        return driver;
    }


    /**
     * returns a CountDownLatch.
     * This countdown latch will count down as soon as <code>Device.noDriverFound()</code>
     * has been called.
     * @param device the Device
     * @return the countdown latch
     */
    private CountDownLatch tstExpectNoDriverFound( Device device )
    {
        final CountDownLatch latch = new CountDownLatch( 1 );

        //countdown when noDriverFound is called
        Answer<Object> answer = new Answer<Object>()
        {
        	public Object answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return null;
            }
        };

        Mockito.doAnswer( answer ).when(device).noDriverFound();

        return latch;

    }


    private CountDownLatch tstExpectAttach( Driver driver, Object device ) throws Exception
    {
        final CountDownLatch latch = new CountDownLatch( 1 );
        Answer<String> answer = new Answer<String>()
        {
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return null;
            }
        };

        //successful attach
        Mockito.when( driver.attach( m_osgi.getReference( device ) ) )
        	.thenAnswer( answer );

        return latch;
    }

    private CountDownLatch tstExpectUnloadDriverBundle( Driver driver ) throws BundleException {
    	
    	
    	final CountDownLatch latch = new CountDownLatch( 1 );
        Answer<String> answer = new Answer<String>()
        {
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                return null;
            }
        };

        Bundle bundle = m_osgi.getBundle( m_osgi.getReference( driver ) );
        
        Mockito.doAnswer(answer).when( bundle ).uninstall();

        return latch;    	
    }

    /**
     * This method generates behavior on the provided DriverLocator.
     * 
     * The given driver Ids and their matches are expected as drivers found
     * by this driver locator.
     * Also, if a driver is found, we can also expect that loadDriver is called;
     * resulting in an InputStream. That particular input stream should, when installed
     * using a bundle context, lead to the registration of a driver with 
     * the correcsponding driver id.
     *  
     * @param locator
     * @param driverIds
     * @param matches
     * @return
     * @throws Exception
     */
    private Map<String, Driver> tstExpectDriverLocatorFor( final DriverLocator locator, final String[] driverIds,
        int[] matches ) throws Exception
    {

        Mockito.when( locator.findDrivers( Mockito.isA( Dictionary.class ) ) )
        	.thenReturn( driverIds );

        Map<String, Driver> drivers = new HashMap<String, Driver>();

        final Map<String, InputStream> streams = new HashMap<String, InputStream>();

        for ( String driverId : driverIds )
        {
            InputStream in = Mockito.mock(InputStream.class, "[InputStream for: " + driverId + "]");
            streams.put( driverId, in );
        }

        Answer<InputStream> answer = new Answer<InputStream>()
        {
        	public InputStream answer(InvocationOnMock invocation) throws Throwable
            {
                final String id = invocation.getArguments()[0].toString();

                for ( String driverId : driverIds )
                {
                    if ( id.equals( driverId ) )
                    {
                        return streams.get( id );
                    }
                }
                throw new IOException( "no such driverId defined in this locator: " + locator );
            }
        };

        
        Mockito.when( locator.loadDriver( Mockito.isA( String.class ) ) )
        	.thenAnswer( answer );

        int i = 0;
        for ( String driverId : driverIds )
        {
            Driver driver = tstExpectInstallDriverBundle( driverId, matches[i], streams.get( driverId ) );
            drivers.put( driverId, driver );
            i++;
        }

        return drivers;

    }


    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition
     */
    @Test
    public void LocatorAdded()
    {

        DriverLocator locator = Mockito.mock( DriverLocator.class );
        m_manager.locatorAdded( locator );

    }


    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition/ removal
     */
    @Test
    public void LocatorRemoved()
    {

        DriverLocator locator = Mockito.mock( DriverLocator.class );

        m_manager.locatorAdded( locator );
        m_manager.locatorRemoved( locator );

    }


    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition
     * @throws Exception 
     */
    @Test
    public void DriverAdded() throws Exception
    {

        Driver driver = tstCreateDriver( "org.apache.felix.driver-1.0", 1 );

        m_manager.driverAdded( m_osgi.getReference( driver ), driver );

    }


    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition/ removal
     * @throws Exception 
     */
    @Test
    public void DriverRemoved() throws Exception
    {

        Driver driver = tstCreateDriver( "org.apache.felix.driver-1.0", 1 );

        ServiceReference ref = m_osgi.getReference( driver );

        m_manager.driverAdded( ref, driver );
        m_manager.driverRemoved( ref );
    }

    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition/ removal
     * @throws Exception 
     */
    @Test
    public void DeviceRemoved() throws Exception
    {

    	Properties p = new Properties();
    	p.put(Constants.DEVICE_CATEGORY, new String[]{"dummy"});
    	
        ServiceReference ref = OSGiMock.createReference(p);
        
        Object device = new Object();

        m_manager.deviceAdded( ref, device );
        m_manager.deviceRemoved( ref );
    }

    /**
     * does not really test anything special, but ensures that the internal
     * structure is able to parse the addition/ removal
     * @throws Exception 
     */
    @Test
    public void DeviceModified() throws Exception
    {

    	Properties p = new Properties();
    	p.put(Constants.DEVICE_CATEGORY, new String[]{"dummy"});
    	
        ServiceReference ref = OSGiMock.createReference(p);
        Object device = new Object();
        
        m_manager.deviceAdded( ref, new Object() );
        m_manager.deviceModified(ref, device);
    }
    //intended flow, various configurations
    /**
     * 	We add a device, but there are no driver locators, so
     *  the noDriverFound method must be called
     * @throws InterruptedException 
     */
    @Test
    public void DeviceAddedNoDriverLocator() throws InterruptedException
    {

        //create a mocked device
        Device device = tstCreateDevice( new String[]
            { "org.apache.felix" } );

        CountDownLatch latch = tstExpectNoDriverFound( device );

        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !latch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected call noDriverFound" );
        }
        
    }


    /**
     * 	We add a device, but there are no driver locators, however, there is a driver
     *  that matches. Thus an attach must follow.
     * @throws Exception 
     */
    @Test
    public void DeviceAddedNoDriverLocatorSuccessfulAttach() throws Exception
    {

        Device device = tstCreateDevice( new String[] { "org.apache.felix" } );
        Driver driver = tstCreateDriver( "org.apache.felix.driver-1.0", 1 );

        CountDownLatch attachLatch = tstExpectAttach( driver, device );

        m_manager.driverAdded( m_osgi.getReference( driver ), driver );
        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !attachLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected attach" );
        }

    }


    /**
     * 	We add a device, but there are no driver locators, however, there is a driver
     *  but it sadly doesn't match. Thus a <code>noDriverFound()</code> is called.
     *  
     * @throws Exception 
     */
    @Test
    public void DeviceAddedNoDriverLocatorAttachFails() throws Exception
    {

        Device device = tstCreateDevice( new String[] { "org.apache.felix" } );
        Driver driver = tstCreateDriver( "org.apache.felix.driver-1.0", Device.MATCH_NONE );

        CountDownLatch attachLatch = tstExpectNoDriverFound( device );

        m_manager.driverAdded( m_osgi.getReference( driver ), driver );
        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !attachLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected attach" );
        }

    }


    /**
     * We add a device while there's one driverlocator that will successfully
     * locate and load two driver bundles. We expect a <code>Driver.attach()</code> for 
     * the best matching driver. There's already a driver loaded that should not match.
     * 
     * @throws Exception
     */
    @Test
    public void DeviceAddedWithADriverLocator() throws Exception
    {

        final String driverId1 = "org.apache.felix.driver-1.0";
        final String driverId2 = "org.apache.felix.driver-1.1";
        final String notMatchingButLoadedDriverId = "dotorg.apache.felix.driver-1.0";


        DriverLocator locator = Mockito.mock( DriverLocator.class );

        Map<String, Driver> drivers = tstExpectDriverLocatorFor( locator, 
    		new String[] { driverId1, driverId2 }, 
    		new int[] { 30, 3 } );

        Driver noMatcher = tstCreateDriver( notMatchingButLoadedDriverId, 100 );

        Device device = tstCreateDevice( new String[]{ "org.apache.felix" } );

        final CountDownLatch attachLatch = tstExpectAttach( drivers.get( driverId1 ), device );

        final CountDownLatch unloadDriverLatch = tstExpectUnloadDriverBundle( drivers.get ( driverId2 ) );

        m_manager.locatorAdded( locator );

        m_manager.driverAdded( m_osgi.getReference( noMatcher ), noMatcher );

        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !attachLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an attach" );
        }
        
        //since driver1 is attached, we expect an uninstall()
        //of all other (dynamically loaded) driver bundles
        if ( !unloadDriverLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an unload" );
        }

    }

    @Test
    public void DeviceAddedWithADriverLocatorUnloadFails() throws Exception
    {

        final String driverId1 = "org.apache.felix.driver-1.0";
        final String driverId2 = "org.apache.felix.driver-1.1";
        final String notMatchingButLoadedDriverId = "dotorg.apache.felix.driver-1.0";


        DriverLocator locator = Mockito.mock( DriverLocator.class );

        Map<String, Driver> drivers = tstExpectDriverLocatorFor( locator, 
    		new String[] { driverId1, driverId2 }, 
    		new int[] { 30, 3 } );

        Driver noMatcher = tstCreateDriver( notMatchingButLoadedDriverId, 100 );

        Device device = tstCreateDevice( new String[]{ "org.apache.felix" } );

        final CountDownLatch attachLatch = tstExpectAttach( drivers.get( driverId1 ), device );

        final CountDownLatch unloadDriverLatch = new CountDownLatch( 1 );
        
        ServiceReference driver2Ref = m_osgi.getReference( drivers.get( driverId2 ) );
        Bundle driver2Bundle = m_osgi.getBundle( driver2Ref );
        
        Answer<Object> answer = new Answer<Object>() {
        	
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		try {
        			throw new BundleException("test driverBundle uninstall failed");
        		}
        		finally {
        			unloadDriverLatch.countDown();
        		}
        	}
        };
        
        Mockito.doAnswer(answer).when(driver2Bundle).uninstall();
        
        m_manager.locatorAdded( locator );

        m_manager.driverAdded( m_osgi.getReference( noMatcher ), noMatcher );

        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !attachLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an attach" );
        }
        
        if ( !unloadDriverLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an unload" );
        }

        
        //since driver1 is attached, we expect an uninstall()
        //of all other (dynamically loaded) driver bundles
        //Driver driver = drivers.get( driverId2 );
        //tstVerifyBundleUninstall( driver );
    }

    /**
     * Two drivers equally match the device. There is a driver selector
     * that comes to the rescue that selects driver2. 
     * 
     * @throws Exception
     */
    @Test
    public void EqualMatchWithDriverSelector() throws Exception
    {

        final String driverId1 = "org.apache.felix.driver-1.0";
        final String driverId2 = "org.apache.felix.driver-1.1";

        DriverLocator locator = Mockito.mock( DriverLocator.class );

        Map<String, Driver> drivers = tstExpectDriverLocatorFor( locator, 
    		new String[] { driverId1, driverId2 }, 
    		new int[] { 20, 20 } );

        Device device = tstCreateDevice( new String[]{ "org.apache.felix" } );

        DriverSelector selector = Mockito.mock( DriverSelector.class );

        SelectorMatcher matcher = new SelectorMatcher( driverId2 );

        Mockito.when( selector.select( 
        		Mockito.eq( m_osgi.getReference( device ) ), 
        		Mockito.isA(Match[].class) ) ).thenAnswer( matcher );

        final CountDownLatch attachLatch = tstExpectAttach( drivers.get( driverId2 ), device );


        Utils.inject( m_manager, DriverSelector.class, selector );

        m_manager.locatorAdded( locator );

        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !attachLatch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an attach" );
        }
        
        
        //driver2 is attached, so driver1 bundle should uninstall.
        //Driver driver = drivers.get( driverId1 );
        //tstVerifyBundleUninstall( driver );

    }


    //exceptional flow
    @Test
    public void DriverLocator_findDriverFails() throws Exception
    {

        final CountDownLatch latch = new CountDownLatch( 1 );

        Answer<String[]> answer = new Answer<String[]>()
        {

            public String[] answer(InvocationOnMock invocation) throws Throwable
            {
                latch.countDown();
                throw new RuntimeException( "test exception" );
            }
        };

        DriverLocator locator = Mockito.mock( DriverLocator.class, "locator" );
        Mockito.when( locator.findDrivers( Mockito.isA( Dictionary.class ) ) )
        	.thenAnswer( answer );

        Device device = tstCreateDevice( new String[]
            { "org.apache.felix" } );

        final CountDownLatch latch2 = new CountDownLatch( 1 );

        Answer<Object> answer2 = new Answer<Object>()
        {
            public Object answer(InvocationOnMock invocation) throws Throwable
            {
                latch2.countDown();
                return null;
            }
        };
        
        Mockito.doAnswer(answer2).when(device).noDriverFound();
        

        m_manager.locatorAdded( locator );
        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        if ( !latch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected a call to DriverLocator.findDrivers" );
        }

        if ( !latch2.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected a call to Driver.noDriverFound" );
        }

    }


    /**
     * This test verified correct behavior when after a driver
     * attach led to a referral, this referral leads to an exception.
     * 
     * 
     * @throws Exception
     */
    @Ignore
    public void DriverReferral_ReferralFails() throws Exception
    {

        final String referredDriver = "org.apache.felix.driver-2.0";

        String[] driverIds = new String[]
            { "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1" };
        
        int[] driverMatches = new int[]{ 1, Device.MATCH_NONE };
        
        DriverLocator locator = Mockito.mock( DriverLocator.class, "locator for v1.x" );
        Map<String, Driver> drivers = tstExpectDriverLocatorFor( locator, driverIds, driverMatches );

        
        DriverLocator locatorv2 = Mockito.mock( DriverLocator.class, "locator for v2.x (fails always)" );
        Mockito.when( locatorv2.findDrivers( Mockito.isA( Dictionary.class ) ) )
        	.thenReturn( null );

        Mockito.when( locatorv2.loadDriver( Mockito.startsWith( "org.apache.felix.driver-1" ) ) )
    		.thenReturn( null );
        
        InputStream referredInputStream = Mockito.mock(InputStream.class);
        Mockito.when( locatorv2.loadDriver( referredDriver ) ).thenReturn( referredInputStream );


        //this is what initial driver referral eventually leads
        //to: the loading of a driver bundle
        //we fake it, so that it fails
        Mockito.when( m_context.installBundle( 
        		Mockito.anyString(), 
        		Mockito.isA( InputStream.class ) ) )
        	.thenThrow(new BundleException( "test exception" ) );

        Driver matched = drivers.get( "org.apache.felix.driver-1.0" );

        final CountDownLatch latch = new CountDownLatch( 1 );

        Answer<String> driver10_attach = new Answer<String>()
        {
            public String answer(InvocationOnMock invocation) throws Throwable
            {
            	System.out.println("driver10_attach()");
                latch.countDown();
                return referredDriver;
            }
        };

        Device device = tstCreateDevice( new String[]{ "org.apache.felix" } );
        

        Mockito.when( matched.match( m_osgi.getReference( device ) ) ).thenReturn( 10 );

        Mockito.when( matched.attach( Mockito.isA( ServiceReference.class ) ) )
        	.thenAnswer( driver10_attach );

//        for ( String driverId : driverIds )
//        {
//            Driver driver = drivers.get( driverId );
//            tstExpectBundleUninstall( driver );
//        }


        //the actual test
        
        m_manager.locatorAdded( locator );
        m_manager.locatorAdded( locatorv2 );
        
        //depman induced callback
        m_manager.deviceAdded( m_osgi.getReference( device ), device );

        
        if ( !latch.await( 5, TimeUnit.SECONDS ) )
        {
            Assert.fail( "expected an attach to: " + driverIds[0] );
        }

        
        Mockito.verify(device).noDriverFound();
    }

    /**
     * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
     */
    private class MatchAnswer implements Answer<Integer>
    {

        private final ServiceReference m_driverRef;

        public MatchAnswer( ServiceReference driverRef )
        {
            m_driverRef = driverRef;
        }


        public Integer answer(InvocationOnMock invocation) throws Throwable
        {
            ServiceReference deviceRef = ( ServiceReference ) invocation.getArguments()[0];
            String[] categories = String[].class.cast( deviceRef.getProperty( Constants.DEVICE_CATEGORY ) );
            String driverId = String.class.cast( m_driverRef.getProperty( Constants.DRIVER_ID ) );

            for ( String string : categories )
            {
                if ( driverId.startsWith( string ) )
                {
                    Object match = m_driverRef.getProperty( "match" );
                    return Integer.valueOf( match.toString() );
                }
            }
            return Device.MATCH_NONE;
        }

    }
        

    private class SelectorMatcher implements Answer<Integer>
    {

        private String m_driverId;


        public SelectorMatcher( String driverId )
        {
            m_driverId = driverId;
        }

		public Integer answer(InvocationOnMock invocation) throws Throwable
        {
            int i = 0;
            Match[] matches = (Match[])invocation.getArguments()[1];
            
            for ( Match match : matches )
            {
                if ( match.getDriver().getProperty( Constants.DRIVER_ID ).equals( m_driverId ) )
                {
                    return i;
                }
                i++;
            }
            return DriverSelector.SELECT_NONE;
        }


    }
    
}
