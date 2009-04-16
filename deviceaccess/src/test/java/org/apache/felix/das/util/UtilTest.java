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


import java.util.Properties;

import junit.framework.Assert;

import org.apache.felix.das.Utils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.device.Constants;
import org.osgi.service.device.Device;
import org.osgi.service.log.LogService;


/**
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 *
 */
public class UtilTest
{

    private DeviceAnalyzer m_devA;

    @Mock
    private LogService m_log;

    @Mock
    private BundleContext m_context;


    @Before
    public void before() throws Exception
    {

    	MockitoAnnotations.initMocks(this);
    	
        m_devA = new DeviceAnalyzer( m_context );
        Utils.inject( m_devA, LogService.class, m_log );
        
        
        String f1 = "(objectClass=org.osgi.service.device.Device)";
        Filter deviceFilter = FrameworkUtil.createFilter(f1);
        Mockito.when(m_context.createFilter(Mockito.eq(f1))).thenReturn(deviceFilter);
        
        String f2 = "(DEVICE_CATEGORY=*)";
        Filter driverFilter = FrameworkUtil.createFilter(f2);
        Mockito.when(m_context.createFilter(f2)).thenReturn(driverFilter);
        

        Utils.invoke( m_devA, "start" );
    }

    
    
    private ServiceReference createReference(final Properties p) 
    {
        ServiceReference ref = Mockito.mock( ServiceReference.class );

        Mockito.when(ref.getProperty(Mockito.anyString())).thenAnswer(new Answer<Object>() {
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		return p.get(invocation.getArguments()[0].toString());
        	}
        });
        
        Mockito.when(ref.getPropertyKeys()).thenAnswer(new Answer<Object>() {
        	public Object answer(InvocationOnMock invocation) throws Throwable {
        		return p.keySet().toArray(new String[0]);
        	}
        });
        
        
        return ref;
    }

    @Test
    public void ShowDeviceIfThereIsAnInvalidCategory() throws Exception
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Object.class.getName()} );
        p.put(Constants.DEVICE_CATEGORY, "dummy");

        ServiceReference ref = createReference(p);
        
        m_devA.deviceAdded( ref );
        
        Mockito.verify(m_log).log(Mockito.eq(LogService.LOG_ERROR), Mockito.anyString());

    }


    @Test
    public void ShowDeviceIfThereIsNoCategory() throws Exception
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Object.class.getName()} );

        ServiceReference ref = createReference(p);
        
        m_devA.deviceAdded( ref );
        
        Mockito.verify(m_log).log( Mockito.eq( LogService.LOG_ERROR ), Mockito.isA( String.class ) );
    }

    @Test 
    public void VerifyValidIsDeviceInstanceValidationIfDevice() throws InvalidSyntaxException 
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Device.class.getName()} );
        
    	ServiceReference ref = createReference(p);
    
    	Assert.assertTrue( "Incorrectly determined as no device", Util.isDeviceInstance(ref) );
    }

    @Test 
    public void VerifyValidIsDeviceInstanceValidationThrowsException() throws InvalidSyntaxException 
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Device.class.getName()} );
        
    	ServiceReference ref = createReference(p);
    
    	Assert.assertTrue( "Incorrectly determined as no device", Util.isDeviceInstance(ref) );
    }

    @Test 
    public void VerifyValidFilterStringCreation() throws InvalidSyntaxException {
    	
    	Object[] data = new Object[]{"a","b","c","d"};
    	String str = Util.createFilterString("(|(%s=%s)(%s=%s))", data);
    	
    	Assert.assertEquals("filter string mismatch","(|(a=b)(c=d))", str);
    }

    @Test 
    public void VerifyValidFilterCreation() throws InvalidSyntaxException {
    	
    	Object[] data = new Object[]{Constants.DEVICE_CATEGORY, "dummy"};
    	Filter filter = Util.createFilter("(%s=%s)", data);
    	
    	
    	Properties matching = new Properties();
    	matching.put(Constants.DEVICE_CATEGORY, new String[]{"dummy", "nonsense"});
    	Assert.assertTrue("matching filter does not match", filter.match(matching));
    	
    	Properties notmatching = new Properties();
    	notmatching.put(Constants.DEVICE_CATEGORY, new String[]{"lummy", "nonsense"});
    	Assert.assertFalse("notmatching filter does match", filter.match(notmatching));

    	
    }
    
 
    
    
    
    @Test
    public void ShowDeviceIfThereIsAnEmptyCategory() throws Exception
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Object.class.getName()} );
        p.put( Constants.DEVICE_CATEGORY, new String[0] );

        
        ServiceReference ref = createReference(p);

        m_devA.deviceAdded( ref );

        Mockito.verify(m_log).log( Mockito.eq( LogService.LOG_ERROR ), Mockito.isA( String.class ) );

    }


    @Test
    public void NoShowDeviceIfThereIsAValidCategory() throws Exception
    {

        Properties p = new Properties();
        p.put( org.osgi.framework.Constants.OBJECTCLASS, new String[]{Device.class.getName()} );
        p.put( Constants.DEVICE_CATEGORY, new String[]{"dummy"} );

        ServiceReference ref = createReference(p);

        m_devA.deviceAdded( ref );
    }
}
