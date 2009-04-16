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
package org.apache.felix.das.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.das.DeviceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.device.DriverLocator;


/**
 * The Device Manager delegates driver loading to the DriverLoader.
 * This JUnit test tests the behavior of that DriverMatcher.
 * 
 * Tests all kinds of driver loading flows.
 * all flows pertaining driver loading are grouped in the DriverLoader.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class DriverLoaderTest
{


    private DriverLoader m_loader;

    @Mock
    private BundleContext m_context;

    @Mock
    private DeviceManager m_log;
    
    @Before
    public void setUp() throws Exception
    {
    	
    	MockitoAnnotations.initMocks(this);
        m_loader = new DriverLoader( m_log, m_context );
    }


    private DriverLocator tstExpectDriverIdsFor(String[] ids) {
    	
        DriverLocator dl = Mockito.mock(DriverLocator.class );
        Mockito.when( dl.findDrivers( Mockito.isA(Dictionary.class) ) ).thenReturn( ids );
        return dl;
    }
    
    /**
     * test whether the driver loader can handle a situation where
     * there are no DriverLocators.
     * 
     */
    @Test
    public void findDriversNoDriverLocators()
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();


        List<String> driverIds = m_loader.findDrivers( locators, new Properties() );
        Assert.assertTrue( "should be an empty list", driverIds.size() == 0 );
        
    }


    /**
     * in this test there is a driver locator. the driver locator is instructed to 
     * even return some driver ids.
     * this test tests whether these driver ids are really returned. 
     */
    @Test
    public void findDriversWithDriverLocator()
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl = tstExpectDriverIdsFor( 
            new String[] { "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1" } );

        locators.add( dl );

        Properties dict = new Properties();
        List<String> driverIds = m_loader.findDrivers( locators, dict );

        Assert.assertEquals( "should not be an empty list", 2, driverIds.size());

    }


    /**
     * in this test there are several driver locators, some of which return
     * driver Ids, some don't. we expect an accurate number of driver ids being returned
     * from the driverloader.
     */
    @Test
    public void findDriversWithDriverLocators()
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl1 = tstExpectDriverIdsFor( 
            new String[]{ "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1" } );
        locators.add( dl1 );
        
        DriverLocator dl2 = tstExpectDriverIdsFor( 
            new String[]{ "org.apache.felix.driver-1.2", "org.apache.felix.driver-1.3" } );
        locators.add( dl2 );
        
        DriverLocator dl3 = tstExpectDriverIdsFor( null );
        locators.add( dl3 );

        
        Properties dict = new Properties();
        List<String> driverIds = m_loader.findDrivers( locators, dict );

        Assert.assertEquals( "should not be an empty list", 4, driverIds.size() );

    }


    @Test
    public void findDriversWithDriverLocatorFails()
    {

        Properties dict = new Properties();
        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl = Mockito.mock( DriverLocator.class, "dl" );
        locators.add( dl );

        Mockito.when( dl.findDrivers( Mockito.eq( dict ) ) ).thenThrow( new RuntimeException( "test exception" ) );

        List<String> driverIds = m_loader.findDrivers( locators, dict );

        Assert.assertTrue( "should be an empty list", driverIds.size() == 0 );

    }


    @Test
    public void loadDrivers() throws IOException, BundleException
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl = Mockito.mock( DriverLocator.class, "dl" );
        locators.add( dl );

        String[] driverIds = new String[]
            { "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1", };

        for ( String string : driverIds )
        {
            Mockito.when( dl.loadDriver( Mockito.eq( string ) ) ).thenReturn( null );
            Bundle bundle = Mockito.mock( Bundle.class );
            
            Mockito.when( m_context.installBundle( "_DD_" + string, null ) ).thenReturn( bundle );
            bundle.start();
            
            ServiceReference ref = Mockito.mock( ServiceReference.class );
            Mockito.when( ref.getProperty( Constants.DRIVER_ID ) ).thenReturn( string );
            Mockito.when( bundle.getRegisteredServices() ).thenReturn( new ServiceReference[]
                { ref } );
        }

        List<ServiceReference> refs = m_loader.loadDrivers( locators, driverIds );

        Assert.assertEquals( "", 2, refs.size() );
        for ( ServiceReference serviceReference : refs )
        {
            String driverId = "" + serviceReference.getProperty( Constants.DRIVER_ID );
            if ( !driverId.equals( driverIds[0] ) && !driverId.equals( driverIds[1] ) )
            {
                Assert.fail( "unexpected driverId" );
            }
        }

    }


    @Test
    public void loadDrivers_LoadFails() throws IOException, BundleException
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl = Mockito.mock( DriverLocator.class, "dl" );
        locators.add( dl );

        String[] driverIds = new String[]
            { "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1", };

        for ( String string : driverIds )
        {
            Mockito.when( dl.loadDriver( string ) ).thenThrow( new IOException( "test exception" ) );
        }

        List<ServiceReference> refs = m_loader.loadDrivers( locators, driverIds );

        Assert.assertEquals( "", 0, refs.size() );

    }


    @Test
    public void loadDrivers_InstallFails() throws IOException, BundleException
    {

        List<DriverLocator> locators = new ArrayList<DriverLocator>();

        DriverLocator dl = Mockito.mock( DriverLocator.class, "dl" );
        locators.add( dl );

        String[] driverIds = new String[]
            { "org.apache.felix.driver-1.0", "org.apache.felix.driver-1.1", };

        for ( String string : driverIds )
        {
        	Mockito.when( dl.loadDriver( string ) ).thenReturn( null );
        	Mockito.when( m_context.installBundle( DriverLoader.DRIVER_LOCATION_PREFIX + string, null ) )
        		.thenThrow(new BundleException( "test exception" ) );
        }

        List<ServiceReference> refs = m_loader.loadDrivers( locators, driverIds );

        Assert.assertEquals( "", 0, refs.size() );
    }

}
