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
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.configuration.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class DynamicallyConfigurableProperties extends OSGiTestCase {

	ComponentInstance instance;
	
	public void setUp() {
		String type = "CONFIG-FooProviderType-3";
		
		Properties p1 = new Properties();
		p1.put("name", "instance");
		p1.put("foo", "foo");
		p1.put("bar", "2");
		p1.put("baz", "baz");
		instance = Utils.getComponentInstance(context, type, p1);
	}
	
	public void tearDown() {
		instance.dispose();
		instance = null;
	}
	
	public void testStatic() {
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		assertEquals("Check foo equality -1", fooP, "foo");
		assertEquals("Check bar equality -1", barP, new Integer(2));
		assertEquals("Check baz equality -1", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByName(context, ManagedServiceFactory.class.getName(), instance.getFactory().getName());
		assertNotNull("Check ManagedServiceFactory availability", msRef);
		
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("bar", new Integer(2));
		conf.put("foo", "foo");
		ManagedServiceFactory ms = (ManagedServiceFactory) context.getService(msRef);
		try {
			ms.updated(instance.getInstanceName(), conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		
		// Recheck props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
		fooP = (String) fooRef.getProperty("foo");
		barP = (Integer) fooRef.getProperty("bar");
		bazP = (String) fooRef.getProperty("baz");
		assertEquals("Check foo equality -2", fooP, "foo");
		assertEquals("Check bar equality -2", barP, new Integer(2));
		assertEquals("Check baz equality -2", bazP, "zab");
		context.ungetService(msRef);
	}
	
	public void testDynamic() {
    	ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
    	assertNotNull("Check FS availability", fooRef);
    	
    	String fooP = (String) fooRef.getProperty("foo");
    	Integer barP = (Integer) fooRef.getProperty("bar");
    	String bazP = (String) fooRef.getProperty("baz");
    	
    	assertEquals("Check foo equality", fooP, "foo");
    	assertEquals("Check bar equality", barP, new Integer(2));
    	assertEquals("Check baz equality", bazP, "baz");
    	
    	ServiceReference msRef = Utils.getServiceReferenceByName(context, ManagedServiceFactory.class.getName(), instance.getFactory().getName());
    	assertNotNull("Check ManagedServiceFactory availability", msRef);
    	
    	// Configuration of baz
    	Properties conf = new Properties();
    	conf.put("baz", "zab");
    	conf.put("foo", "oof");
    	conf.put("bar", new Integer(0));
    	ManagedServiceFactory ms = (ManagedServiceFactory) context.getService(msRef);
    	try {
    		ms.updated(instance.getInstanceName(), conf);
    	} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
    	
    	// Recheck props
    	fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
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

    public void testDynamicString() {
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		
		assertEquals("Check foo equality", fooP, "foo");
		assertEquals("Check bar equality", barP, new Integer(2));
		assertEquals("Check baz equality", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByName(context, ManagedServiceFactory.class.getName(), instance.getFactory().getName());
		assertNotNull("Check ManagedServiceFactory availability", msRef);
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("foo", "oof");
		conf.put("bar", "0");
		ManagedServiceFactory ms = (ManagedServiceFactory) context.getService(msRef);
		try {
			ms.updated(instance.getInstanceName(), conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		
		// Recheck props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
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
	
	public void testPropagation() {
		ServiceReference fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
		assertNotNull("Check FS availability", fooRef);
		
		String fooP = (String) fooRef.getProperty("foo");
		Integer barP = (Integer) fooRef.getProperty("bar");
		String bazP = (String) fooRef.getProperty("baz");
		
		assertEquals("Check foo equality", fooP, "foo");
		assertEquals("Check bar equality", barP, new Integer(2));
		assertEquals("Check baz equality", bazP, "baz");
		
		ServiceReference msRef = Utils.getServiceReferenceByName(context, ManagedServiceFactory.class.getName(), instance.getFactory().getName());
		assertNotNull("Check ManagedServiceFactory availability", msRef);
		
		// Configuration of baz
		Properties conf = new Properties();
		conf.put("baz", "zab");
		conf.put("foo", "foo");
		conf.put("bar", new Integer(2));
		conf.put("propagated1", "propagated");
		conf.put("propagated2", new Integer(1));
		ManagedServiceFactory ms = (ManagedServiceFactory) context.getService(msRef);
		try {
			ms.updated(instance.getInstanceName(), conf);
		} catch (ConfigurationException e) { fail("Configuration Exception : " + e); }
		
		// Recheck props
		fooRef = Utils.getServiceReferenceByName(context, FooService.class.getName(), instance.getInstanceName());
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
