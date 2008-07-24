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
package org.apache.felix.ipojo.test.scenarios.configuration;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.configuration.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

public class ManagedServiceConfigurableProperties extends OSGiTestCase {

	/**
	 * Instance where the ManagedServicePID is provided by the component type. 
	 */
	ComponentInstance instance1;
	/**
     * Instance where the ManagedServicePID is provided by the instance. 
     */
	ComponentInstance instance2;
	
	public void setUp() {
	    String type = "CONFIG-FooProviderType-4";
        Properties p = new Properties();
        p.put("name", "instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = Utils.getComponentInstance(context, type, p);
        assertEquals("instance1 created", ComponentInstance.VALID,instance1.getState());
        
		type = "CONFIG-FooProviderType-3";
		Properties p1 = new Properties();
		p1.put("name", "instance-2");
		p1.put("foo", "foo");
		p1.put("bar", "2");
		p1.put("baz", "baz");
		p1.put("managed.service.pid", "instance");
		instance2 = Utils.getComponentInstance(context, type, p1);
	}
	
	public void tearDown() {
		instance1.dispose();
		instance2.dispose();
		instance1 = null;
		instance2 = null;
	}
	
	public void testStaticInstance1() {
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		assertEquals("Check foo equality -1", fooP, "foo");
		assertEquals("Check bar equality -1", barP, new Integer(2));
		assertEquals("Check baz equality -1", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "FooProvider-3");
		assertNotNull("Check ManagedServiceFactory availability", msRef);
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("bar", new Integer(2));
		conf.put("foo", "foo");
		ManagedService ms = (ManagedService) context.getService(msRef);
		try {
			ms.updated(conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		
		// Re-check props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		fooP = (String) fooRef.getProperty("foo");
		barP = (Integer) fooRef.getProperty("bar");
		bazP = (String) fooRef.getProperty("baz");
		assertEquals("Check foo equality -2", fooP, "foo");
		assertEquals("Check bar equality -2", barP, new Integer(2));
		assertEquals("Check baz equality -2", bazP, "zab");
		context.ungetService(msRef);
	}
	
	public void testStaticInstance2() {
        ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -1", fooP, "foo");
        assertEquals("Check bar equality -1", barP, new Integer(2));
        assertEquals("Check baz equality -1", bazP, "baz");
        
        ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);
        
        
        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("bar", new Integer(2));
        conf.put("foo", "foo");
        ManagedService ms = (ManagedService) context.getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
        
        // Recheck props
        fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertEquals("Check foo equality -2", fooP, "foo");
        assertEquals("Check bar equality -2", barP, new Integer(2));
        assertEquals("Check baz equality -2", bazP, "zab");
        context.ungetService(fooRef);
        context.ungetService(msRef);
    }
	
	public void testDynamicInstance1() {
    	ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
    	assertNotNull("Check FS availability", fooRef);
    	
    	String fooP = (String) fooRef.getProperty("foo");
    	Integer barP = (Integer) fooRef.getProperty("bar");
    	String bazP = (String) fooRef.getProperty("baz");
    	
    	assertEquals("Check foo equality", fooP, "foo");
    	assertEquals("Check bar equality", barP, new Integer(2));
    	assertEquals("Check baz equality", bazP, "baz");
    	
    	ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "FooProvider-3");
    	assertNotNull("Check ManagedServiceFactory availability", msRef);
    	
    	// Configuration of baz
    	Properties conf = new Properties();
    	conf.put("baz", "zab");
    	conf.put("foo", "oof");
    	conf.put("bar", new Integer(0));
    	ManagedService ms = (ManagedService) context.getService(msRef);
    	try {
    		ms.updated(conf);
    	} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
    	
    	// Re-check props
    	fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
    	fooP = (String) fooRef.getProperty("foo");
    	barP = (Integer) fooRef.getProperty("bar");
    	bazP = (String) fooRef.getProperty("baz");
    	
    	assertEquals("Check foo equality", fooP, "oof");
    	assertEquals("Check bar equality", barP, new Integer(0));
    	assertEquals("Check baz equality", bazP, "zab");
    	
    	// Check field value
    	FooService fs = (FooService) context.getService(fooRef);
    	Properties p = fs.fooProps();
    	fooP = (String) p.get("foo");
    	barP = (Integer) p.get("bar");
    	
    	assertEquals("Check foo field equality", fooP, "oof");
    	assertEquals("Check bar field equality", barP, new Integer(0));
    	
    	context.ungetService(fooRef);
    	context.ungetService(msRef);
    }
	
	public void testDynamicInstance2() {
        ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        
        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");
        
        ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedServiceFactory availability", msRef);
        
        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", new Integer(0));
        ManagedService ms = (ManagedService) context.getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
        
        // Recheck props
        fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        
        assertEquals("Check foo equality", fooP, "oof");
        assertEquals("Check bar equality", barP, new Integer(0));
        assertEquals("Check baz equality", bazP, "zab");
        
        // Check field value
        FooService fs = (FooService) context.getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");
        
        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));
        
        context.ungetService(fooRef);
        context.ungetService(msRef);
    }

    public void testDynamicStringInstance1() {
        assertEquals("Check instance1 state", ComponentInstance.VALID,instance1.getState());
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		
		assertEquals("Check foo equality - 1", fooP, "foo");
		assertEquals("Check bar equality - 1", barP, new Integer(2));
		assertEquals("Check baz equality - 1", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "FooProvider-3");
		assertNotNull("Check ManagedService availability", msRef);
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("foo", "oof");
		conf.put("bar", "0");
        assertEquals("Check instance1 state (2)", ComponentInstance.VALID,instance1.getState());
		ManagedService ms = (ManagedService) context.getService(msRef);
		
		PrimitiveHandler ph = (PrimitiveHandler) ms;
		assertSame("Check the correct instance", ph.getInstanceManager(), instance1);
		
		try {
			ms.updated(conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		assertEquals("Check instance1 state (3)", ComponentInstance.VALID,instance1.getState());
		
		// Recheck props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		fooP = (String) fooRef.getProperty("foo");
		barP = (Integer) fooRef.getProperty("bar");
		bazP = (String) fooRef.getProperty("baz");
		
		assertEquals("Check foo equality - 2", fooP, "oof");
		assertEquals("Check bar equality - 2", barP, new Integer(0));
		assertEquals("Check baz equality - 2", bazP, "zab");
		
		// Check field value
		FooService fs = (FooService) context.getService(fooRef);
		Properties p = fs.fooProps();
		fooP = (String) p.get("foo");
		barP = (Integer) p.get("bar");
		
		assertEquals("Check foo field equality", fooP, "oof");
		assertEquals("Check bar field equality", barP, new Integer(0));
		
		context.ungetService(fooRef);
		context.ungetService(msRef);
	}
    
    public void testDynamicStringInstance2() {
        ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        
        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");
        
        ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);
        
        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "oof");
        conf.put("bar", "0");
        ManagedService ms = (ManagedService) context.getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
        
        // Recheck props
        fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        
        assertEquals("Check foo equality", fooP, "oof");
        assertEquals("Check bar equality", barP, new Integer(0));
        assertEquals("Check baz equality", bazP, "zab");
        
        // Check field value
        FooService fs = (FooService) context.getService(fooRef);
        Properties p = fs.fooProps();
        fooP = (String) p.get("foo");
        barP = (Integer) p.get("bar");
        
        assertEquals("Check foo field equality", fooP, "oof");
        assertEquals("Check bar field equality", barP, new Integer(0));
        
        context.ungetService(fooRef);
        context.ungetService(msRef);
    }
	
	public void testPropagationInstance1() {
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		
		assertEquals("Check foo equality", fooP, "foo");
		assertEquals("Check bar equality", barP, new Integer(2));
		assertEquals("Check baz equality", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "FooProvider-3");
		assertNotNull("Check ManagedService availability", msRef);
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("foo", "foo");
		conf.put("bar", new Integer(2));
		conf.put("propagated1", "propagated");
		conf.put("propagated2", new Integer(1));
		ManagedService ms = (ManagedService) context.getService(msRef);
		try {
			ms.updated(conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		
		// Recheck props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance1.getInstanceName());
		fooP = (String) fooRef.getProperty("foo");
		barP = (Integer) fooRef.getProperty("bar");
		bazP = (String) fooRef.getProperty("baz");
		assertNotNull("Check the propagated1 existency", fooRef.getProperty("propagated1"));
		String prop1 = (String) fooRef.getProperty("propagated1");
		assertNotNull("Check the propagated2 existency", fooRef.getProperty("propagated2"));
		Integer prop2 = (Integer) fooRef.getProperty("propagated2");
		
		assertEquals("Check foo equality", fooP, "foo");
		assertEquals("Check bar equality", barP, new Integer(2));
		assertEquals("Check baz equality", bazP, "zab");
		assertEquals("Check propagated1 equality", prop1, "propagated");
		assertEquals("Check propagated2 equality", prop2, new Integer(1));
		
		context.ungetService(msRef);
	}
	
	public void testPropagationInstance2() {
        ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check FS availability", fooRef);
        
        String fooP = (String) fooRef.getProperty("foo");
        Integer barP = (Integer) fooRef.getProperty("bar");
        String bazP = (String) fooRef.getProperty("baz");
        
        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "baz");
        
        ServiceReference msRef = Utils.getServiceReferenceByPID(context, ManagedService.class.getName(), "instance");
        assertNotNull("Check ManagedService availability", msRef);
        
        // Configuration of baz
        Properties conf = new Properties();
        conf.put("baz", "zab");
        conf.put("foo", "foo");
        conf.put("bar", new Integer(2));
        conf.put("propagated1", "propagated");
        conf.put("propagated2", new Integer(1));
        ManagedService ms = (ManagedService) context.getService(msRef);
        try {
            ms.updated(conf);
        } catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
        
        // Recheck props
        fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance2.getInstanceName());
        fooP = (String) fooRef.getProperty("foo");
        barP = (Integer) fooRef.getProperty("bar");
        bazP = (String) fooRef.getProperty("baz");
        assertNotNull("Check the propagated1 existency", fooRef.getProperty("propagated1"));
        String prop1 = (String) fooRef.getProperty("propagated1");
        assertNotNull("Check the propagated2 existency", fooRef.getProperty("propagated2"));
        Integer prop2 = (Integer) fooRef.getProperty("propagated2");
        
        assertEquals("Check foo equality", fooP, "foo");
        assertEquals("Check bar equality", barP, new Integer(2));
        assertEquals("Check baz equality", bazP, "zab");
        assertEquals("Check propagated1 equality", prop1, "propagated");
        assertEquals("Check propagated2 equality", prop2, new Integer(1));
        
        context.ungetService(msRef);
    }

}
