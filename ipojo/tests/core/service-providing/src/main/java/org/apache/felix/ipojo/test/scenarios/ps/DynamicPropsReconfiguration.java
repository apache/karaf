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
package org.apache.felix.ipojo.test.scenarios.ps;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

public class DynamicPropsReconfiguration extends OSGiTestCase {
	ComponentInstance fooProvider3, fooProvider4;
	
	IPOJOHelper helper;

    public void setUp() {
        helper = new IPOJOHelper(this);		
		String type2 = "PS-FooProviderType-Dyn2";
		Properties p3 = new Properties();
		p3.put("instance.name","FooProvider-3");
		p3.put("int", new Integer(0));
		p3.put("boolean", new Boolean(true));
		p3.put("string", new String(""));
		p3.put("strAProp", new String[0]);
		p3.put("intAProp", new int[0]);
		fooProvider3 = helper.createComponentInstance(type2, p3);
		
        fooProvider4 = helper.createComponentInstance(type2, "FooProvider-4");
	}
	
	public void tearDown() {
		helper.dispose();
	}
	
	public void testFactoryReconf() {
    	ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	Integer intProp = (Integer) sr.getProperty("int");
    	Boolean boolProp = (Boolean) sr.getProperty("boolean");
    	String strProp = (String) sr.getProperty("string");
    	String[] strAProp = (String[]) sr.getProperty("strAProp");
    	int[] intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(0));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String(""));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	String[] v = new String[0];
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	int[] v2 = new int[0];
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}
    	
    	// Reconfiguration
    	ServiceReference fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
    	Factory fact = (Factory) getServiceObject(fact_ref);
    	Properties p3 = new Properties();
    	p3.put("instance.name","FooProvider-3");
    	p3.put("int", new Integer(1));
    	p3.put("boolean", new Boolean(true));
    	p3.put("string", new String("foo"));
    	p3.put("strAProp", new String[] {"foo", "bar", "baz"});
    	p3.put("intAProp", new int[] { 1, 2, 3});
    	try {
    		fact.reconfigure(p3);
    	} catch(Exception e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	// Invoke
    	FooService fs = (FooService) getServiceObject(sr);
    	assertTrue("invoke fs", fs.foo());
    	
    	// Re-check the property (change)
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(2));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNull("Check intAProp hidding (no value)", intAProp);
    	
    	//	Reconfiguration
    	fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
    	fact = (Factory) getServiceObject(fact_ref);
    	p3 = new Properties();
    	p3.put("instance.name","FooProvider-3");
    	p3.put("int", new Integer(1));
    	p3.put("boolean", new Boolean(true));
    	p3.put("string", new String("foo"));
    	p3.put("strAProp", new String[] {"foo", "bar", "baz"});
    	p3.put("intAProp", new int[] { 1, 2, 3});
    	try {
    		fact.reconfigure(p3);
    	} catch(Exception e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	fact = null;
    	fs = null;
    }

    public void testFactoryReconfString() {
		ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Boolean boolProp = (Boolean) sr.getProperty("boolean");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(0));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String(""));
		assertNotNull("Check strAProp not nullity", strAProp);
		String[] v = new String[0];
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[0];
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}
		
		// Reconfiguration
		ServiceReference fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
		Factory fact = (Factory) getServiceObject(fact_ref);
		Properties p3 = new Properties();
		p3.put("instance.name","FooProvider-3");
		p3.put("int", "1");
		p3.put("boolean", "true");
		p3.put("string", "foo");
		p3.put("strAProp", "{foo, bar, baz}");
		p3.put("intAProp", "{1, 2, 3}");
		try {
			fact.reconfigure(p3);
		} catch(Exception e) {
			fail("Unable to reconfigure the instance with : " + p3);
		}
		
		sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(1));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		v2 = new int[] { 1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}	
		
		// Invoke
		FooService fs = (FooService) getServiceObject(sr);
		assertTrue("invoke fs", fs.foo());
		
		// Re-check the property (change)
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(2));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNull("Check intAProp hidding (no value)", intAProp);
		
		//	Reconfiguration
		fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
		fact = (Factory) getServiceObject(fact_ref);
		p3 = new Properties();
		p3.put("instance.name","FooProvider-3");
		p3.put("int", "1");
		p3.put("boolean", "true");
		p3.put("string", "foo");
		p3.put("strAProp", "{foo, bar, baz}");
		p3.put("intAProp", "{ 1, 2, 3}");
		try {
			fact.reconfigure(p3);
		} catch(Exception e) {
			fail("Unable to reconfigure the instance with : " + p3);
		}
		
		sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(1));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		v2 = new int[] { 1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}	
		
		fact = null;
		fs = null;
	}
	
	public void testMSFReconf() {
		ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Boolean boolProp = (Boolean) sr.getProperty("boolean");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(0));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String(""));
		assertNotNull("Check strAProp not nullity", strAProp);
		String[] v = new String[0];
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[0];
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}
		
		// Reconfiguration
		ServiceReference fact_ref = helper.getServiceReferenceByName(ManagedServiceFactory.class.getName() , "PS-FooProviderType-Dyn2");
		ManagedServiceFactory fact = (ManagedServiceFactory) getServiceObject(fact_ref);
		Properties p3 = new Properties();
		p3.put("int", new Integer(1));
		p3.put("boolean", new Boolean(true));
		p3.put("string", new String("foo"));
		p3.put("strAProp", new String[] {"foo", "bar", "baz"});
		p3.put("intAProp", new int[] { 1, 2, 3});
		try {
			fact.updated("FooProvider-3", p3);
		} catch (ConfigurationException e) {
			fail("Unable to reconfigure the instance with : " + p3);
		}
		
		sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(1));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		v2 = new int[] { 1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}	
		
		// Invoke
		FooService fs = (FooService) getServiceObject(sr);
		assertTrue("invoke fs", fs.foo());
		
		// Re-check the property (change)
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(2));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNull("Check intAProp hidding (no value)", intAProp);
		
		//	Reconfiguration
		fact_ref = helper.getServiceReferenceByName(ManagedServiceFactory.class.getName() , "PS-FooProviderType-Dyn2");
		fact = (ManagedServiceFactory) getServiceObject(fact_ref);
		p3 = new Properties();
		p3.put("int", new Integer(1));
		p3.put("boolean", new Boolean(true));
		p3.put("string", new String("foo"));
		p3.put("strAProp", new String[] {"foo", "bar", "baz"});
		p3.put("intAProp", new int[] { 1, 2, 3});
		try {
			fact.updated("FooProvider-3", p3);
		} catch (ConfigurationException e) {
			fail("Unable to reconfigure the instance with : " + p3);
		}
		
		sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(1));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		v2 = new int[] { 1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}	
		
		fact = null;
		fs = null;
	}

    public void testMSFReconfString() {
    	ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	Integer intProp = (Integer) sr.getProperty("int");
    	Boolean boolProp = (Boolean) sr.getProperty("boolean");
    	String strProp = (String) sr.getProperty("string");
    	String[] strAProp = (String[]) sr.getProperty("strAProp");
    	int[] intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(0));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String(""));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	String[] v = new String[0];
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	int[] v2 = new int[0];
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}
    	
    	// Reconfiguration
    	ServiceReference fact_ref = helper.getServiceReferenceByName(ManagedServiceFactory.class.getName() , "PS-FooProviderType-Dyn2");
    	ManagedServiceFactory fact = (ManagedServiceFactory) getServiceObject(fact_ref);
    	Properties p3 = new Properties();
    	p3.put("int", "1");
    	p3.put("boolean", "true");
    	p3.put("string", "foo");
    	p3.put("strAProp", "{foo, bar, baz}");
    	p3.put("intAProp", "{ 1, 2, 3}");
    	try {
    		fact.updated("FooProvider-3", p3);
    	} catch (ConfigurationException e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	// Invoke
    	FooService fs = (FooService) getServiceObject(sr);
    	assertTrue("invoke fs", fs.foo());
    	
    	// Re-check the property (change)
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(2));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNull("Check intAProp hidding (no value)", intAProp);
    	
    	//	Reconfiguration
    	fact_ref = helper.getServiceReferenceByName(ManagedServiceFactory.class.getName() , "PS-FooProviderType-Dyn2");
    	fact = (ManagedServiceFactory) getServiceObject(fact_ref);
    	p3 = new Properties();
    	p3.put("int", "1");
        p3.put("boolean", "true");
        p3.put("string", "foo");
        p3.put("strAProp", "{foo, bar, baz}");
        p3.put("intAProp", "{ 1, 2, 3}");
    	try {
    		fact.updated("FooProvider-3", p3);
    	} catch (ConfigurationException e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < strAProp.length; i++) {
    		if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	fact = null;
    	fs = null;
    }

    public void testFactoryReconfNoValue() {
    	ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-4");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	Integer intProp = (Integer) sr.getProperty("int");
    	Object boolProp = sr.getProperty("boolean");
    	Object strProp = sr.getProperty("string");
    	Object strAProp = sr.getProperty("strAProp");
    	int[] intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(4));
    	assertEquals("Check longProp equality", boolProp, null);
    	assertEquals("Check strProp equality", strProp, null);
    	assertNull("Check strAProp nullity", strAProp);
    	
    	assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[] {1, 2, 3};
        for (int i = 0; i < intAProp.length; i++) {
            if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
        }
    	
    	// Reconfiguration
    	ServiceReference fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
    	Factory fact = (Factory) getServiceObject(fact_ref);
    	Properties p3 = new Properties();
    	p3.put("instance.name","FooProvider-4");
    	p3.put("int", new Integer(1));
    	p3.put("boolean", new Boolean(true));
    	p3.put("string", new String("foo"));
    	p3.put("strAProp", new String[] {"foo", "bar", "baz"});
    	p3.put("intAProp", new int[] { 1, 2, 3});
    	try {
    		fact.reconfigure(p3);
    	} catch(Exception e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-4");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	String[] v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < ((String[]) strAProp).length; i++) {
    		if(! ((String[])strAProp)[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	// Invoke
    	FooService fs = (FooService) getServiceObject(sr);
    	assertTrue("invoke fs", fs.foo());
    	
    	// Re-check the property (change)
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(2));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar"};
    	for (int i = 0; i < ((String[]) strAProp).length; i++) {
    		if(!((String[]) strAProp)[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNull("Check intAProp hidding (no value)", intAProp);
    	
    	//	Reconfiguration
    	fact_ref = helper.getServiceReferenceByName(Factory.class.getName() , "PS-FooProviderType-Dyn2");
    	fact = (Factory) getServiceObject(fact_ref);
    	p3 = new Properties();
    	p3.put("instance.name","FooProvider-3");
    	p3.put("int", new Integer(1));
    	p3.put("boolean", new Boolean(true));
    	p3.put("string", new String("foo"));
    	p3.put("strAProp", new String[] {"foo", "bar", "baz"});
    	p3.put("intAProp", new int[] { 1, 2, 3});
    	try {
    		fact.reconfigure(p3);
    	} catch(Exception e) {
    		fail("Unable to reconfigure the instance with : " + p3);
    	}
    	
    	sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-3");
    	assertNotNull("Check the availability of the FS service", sr);
    	
    	// Check service properties
    	intProp = (Integer) sr.getProperty("int");
    	boolProp = (Boolean) sr.getProperty("boolean");
    	strProp = (String) sr.getProperty("string");
    	strAProp = (String[]) sr.getProperty("strAProp");
    	intAProp = (int[]) sr.getProperty("intAProp");
    	
    	assertEquals("Check intProp equality", intProp, new Integer(1));
    	assertEquals("Check longProp equality", boolProp, new Boolean(true));
    	assertEquals("Check strProp equality", strProp, new String("foo"));
    	assertNotNull("Check strAProp not nullity", strAProp);
    	v = new String[] {"foo", "bar", "baz"};
    	for (int i = 0; i < ((String[])strAProp).length; i++) {
    		if(!((String[]) strAProp)[i].equals(v[i])) { fail("Check the strAProp Equality"); }
    	}
    	assertNotNull("Check intAProp not nullity", intAProp);
    	v2 = new int[] { 1, 2, 3};
    	for (int i = 0; i < intAProp.length; i++) {
    		if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
    	}	
    	
    	fact = null;
    	fs = null;
    }

}
