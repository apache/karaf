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



import org.apache.felix.das.DriverAttributes;
import org.apache.felix.das.DeviceManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Driver;
import org.osgi.service.device.DriverSelector;
import org.osgi.service.device.Match;


/**
 * The Device Manager delegates driver matching to the DriverMatcher.
 * This JUnit test tests the behavior of that DriverMatcher.
 * 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DriverMatcherTest
{

    private DriverMatcher m_matcherImpl;

    private int m_serviceId;

    @Mock
    private DeviceManager m_log;

    @Before
    public void setUp() throws Exception
    {

        m_serviceId = 0;

        MockitoAnnotations.initMocks(this);
    	
        m_matcherImpl = new DriverMatcher( m_log );

    }


    private String tstDriverId( Match match )
    {
        return ( String ) match.getDriver().getProperty( org.osgi.service.device.Constants.DRIVER_ID );
    }


    private DriverAttributes tstCreateDriverAttributes( String id, int match, int ranking ) throws Exception
    {

        Bundle bundle = Mockito.mock( Bundle.class );
        ServiceReference ref = Mockito.mock( ServiceReference.class );
        
        
        Mockito.when(ref.getBundle()).thenReturn(bundle);
        Mockito.when(bundle.getLocation()).thenReturn(DriverLoader.DRIVER_LOCATION_PREFIX + "-" + id);
        
        Mockito.when(ref.getProperty(Constants.SERVICE_ID))
        	.thenReturn(m_serviceId++);
        
        Mockito.when(ref.getProperty(org.osgi.service.device.Constants.DRIVER_ID))
        	.thenReturn(id);
        
        
        
        if ( ranking > 0 )
        {
        	Mockito.when( ref.getProperty( Constants.SERVICE_RANKING ) ).thenReturn( ranking );
        }
        else if ( ranking == 0 )
        {
        	Mockito.when( ref.getProperty( Constants.SERVICE_RANKING ) ).thenReturn( null );
        }
        else
        {
            //an invalid ranking object
        	Mockito.when( ref.getProperty( Constants.SERVICE_RANKING ) ).thenReturn( new Object() );
        }

        Driver driver = Mockito.mock( Driver.class );
        Mockito.when( driver.match( Mockito.isA( ServiceReference.class ) ) ).thenReturn( match );

        return new DriverAttributes( ref, driver );

    }


    private void add( String id, int match ) throws Exception
    {
        add( id, match, 0 );
    }


    private void add( String id, int match, int ranking ) throws Exception
    {
        m_matcherImpl.add( match, tstCreateDriverAttributes( id, match, ranking ) );
    }


    @Test
    public void GetBestMatchWithNoDriver() throws Exception
    {

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNull( match );

    }


    @Test
    public void GetBestMatchWithOneDriver() throws Exception
    {

        add( "org.apache.felix.driver-1.0", 1 );

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNotNull( match );
        Assert.assertEquals( "org.apache.felix.driver-1.0", tstDriverId( match ) );

    }

    
    @Test
    public void GetSelectBestMatchThrowsException() throws Exception
    {
    	
    	ServiceReference deviceRef = Mockito.mock(ServiceReference.class);
    	DriverSelector selector = Mockito.mock(DriverSelector.class);

    	Mockito.when(selector.select(Mockito.eq(deviceRef), Mockito.isA(Match[].class)))
    		.thenThrow(new IllegalArgumentException("test"));
    	
        Match match = m_matcherImpl.selectBestMatch(deviceRef, selector);
        Assert.assertNull( match );

    }

    @Test
    public void GetBestMatchWithMultipleDrivers() throws Exception
    {

        add( "org.apache.felix.driver.a-1.0", 1 );
        add( "org.apache.felix.driver.b-1.0", 1 );
        add( "org.apache.felix.driver.c-1.0", 10 );

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNotNull( match );
        Assert.assertEquals( "org.apache.felix.driver.c-1.0", tstDriverId( match ) );

    }


    @Test
    public void GetBestMatchWithInvalidRanking() throws Exception
    {

        add( "org.apache.felix.driver.a-1.0", 1, 0 );
        add( "org.apache.felix.driver.b-1.0", 1, -1 );

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNotNull( match );
        Assert.assertEquals( "org.apache.felix.driver.a-1.0", tstDriverId( match ) );

    }


    @Test
    public void GetBestMatchWithSameRanking() throws Exception
    {

        add( "org.apache.felix.driver.a-1.0", 1 );
        add( "org.apache.felix.driver.b-1.0", 1 );

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNotNull( match );
        Assert.assertEquals( "org.apache.felix.driver.a-1.0", tstDriverId( match ) );
        Assert.assertEquals( 1, match.getMatchValue() );
    }


    @Test
    public void GetBestMatchWithDifferentRanking() throws Exception
    {

        add( "org.apache.felix.driver.a-1.0", 1, 2 );
        add( "org.apache.felix.driver.b-1.0", 1 );

        Match match = m_matcherImpl.getBestMatch();

        Assert.assertNotNull( match );
        final String driverId = "org.apache.felix.driver.a-1.0";

        Assert.assertEquals( driverId, tstDriverId( match ) );
        Assert.assertEquals( 1, match.getMatchValue() );
    }


    @Test
    public void GetBestMatchWithDifferentMatchValue() throws Exception
    {

        add( "org.apache.felix.driver.a-1.0", 1 );
        add( "org.apache.felix.driver.b-1.0", 2 );
        add( "org.apache.felix.driver.c-1.0", 1 );

        Match match = m_matcherImpl.getBestMatch();
        Assert.assertNotNull( match );
        Assert.assertEquals( "org.apache.felix.driver.b-1.0", tstDriverId( match ) );

        Assert.assertEquals( 2, match.getMatchValue() );
    }


    @Test
    public void selectBestDriver() throws Exception
    {

        DriverSelector selector = Mockito.mock( DriverSelector.class );
        ServiceReference deviceRef = Mockito.mock( ServiceReference.class );

        add( "org.apache.felix.driver-1.0", 1 );
        add( "org.apache.felix.driver-1.1", 1 );
        add( "org.apache.felix.driver-1.2", 1 );
        add( "org.apache.felix.driver-1.3", 1 );
        add( "org.apache.felix.driver-1.4", 1 );
        add( "org.apache.felix.driver-1.5", 1 );
        
        
        
        //this is the actual driverselector implementation
        Mockito.when( selector.select( Mockito.isA(ServiceReference.class), Mockito.isA(Match[].class) ) )
        	.thenAnswer( new Answer<Integer>()
        {
        	
        	public Integer answer(InvocationOnMock invocation) throws Throwable
            {
                Match[] matches = ( Match[] ) invocation.getArguments()[1];
                int index = 0;
                for ( Match m : matches )
                {
                    if ( tstDriverId( m ).endsWith( "1.3" ) )
                    {
                        return index;
                    }
                    index++;
                }
                Assert.fail( "expected unreachable" );
                return null;
            }
        } );


        Match match = m_matcherImpl.selectBestMatch( deviceRef, selector );

        Assert.assertNotNull( "no match returned", match );
        String driverId = tstDriverId( match );

        Assert.assertEquals( "org.apache.felix.driver-1.3", driverId );
    }


    @Test
    public void selectFails() throws Exception
    {

        DriverSelector selector = Mockito.mock( DriverSelector.class );
        ServiceReference deviceRef = Mockito.mock( ServiceReference.class );

        Mockito.when( selector.select( Mockito.eq( deviceRef ), Mockito.isA(Match[].class) ) )
            .thenThrow( new RuntimeException( "test exception" ) );

        add( "org.apache.felix.driver-1.5", 1 );

        Match match = m_matcherImpl.selectBestMatch( deviceRef, selector );

        Assert.assertNull( match );

    }
    
    
    @Test
    public void VerifyMatchToString() throws Exception
    {

        DriverSelector selector = Mockito.mock( DriverSelector.class );
        ServiceReference deviceRef = Mockito.mock( ServiceReference.class );

        Mockito.when( selector.select( Mockito.eq( deviceRef ), Mockito.isA(Match[].class) ) )
            .thenReturn( 0 );

        add( "org.apache.felix.driver-1.5", 2 );

        Match match = m_matcherImpl.selectBestMatch( deviceRef, selector );

        Assert.assertNotNull( match );
        Assert.assertNotNull( match.toString() );

    }

}
