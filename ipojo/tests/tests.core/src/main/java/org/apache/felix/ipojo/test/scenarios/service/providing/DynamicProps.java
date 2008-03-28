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
package org.apache.felix.ipojo.test.scenarios.service.providing;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class DynamicProps extends OSGiTestCase {
	
	ComponentInstance fooProvider1;
	ComponentInstance fooProvider2;
	ComponentInstance fooProvider3;

	public void setUp() {
		String type = "FooProviderType-Dyn";
		
		Properties p1 = new Properties();
		p1.put("name", "FooProvider-1");
		fooProvider1 = Utils.getComponentInstance(context, type, p1);
		
		Properties p2 = new Properties();
		p2.put("name", "FooProvider-2");
		p2.put("int", new Integer(4));
		p2.put("boolean", new Boolean(false));
		p2.put("string", new String("bar"));
		p2.put("strAProp", new String[] {"bar", "foo"});
		p2.put("intAProp", new int[] {1, 2, 3});
		fooProvider2 = Utils.getComponentInstance(context, type, p2);
		
		String type2 = "FooProviderType-Dyn2";
		Properties p3 = new Properties();
		p3.put("name", "FooProvider-3");
		p3.put("int", new Integer(0));
		p3.put("boolean", new Boolean(true));
		p3.put("string", new String(""));
		p3.put("strAProp", new String[0]);
		p3.put("intAProp", new int[0]);
		fooProvider3 = Utils.getComponentInstance(context, type2, p3);
		
	}
	
	public void tearDown() {
		fooProvider1.dispose();
		fooProvider1 = null;
		fooProvider2.dispose();
		fooProvider2 = null;
		fooProvider3.dispose();
		fooProvider3 = null;
	}
	
	public void testProperties1() {
		ServiceReference sr = Utils.getServiceReferenceByName(context, FooService.class.getName(), "FooProvider-1");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Boolean boolProp = (Boolean) sr.getProperty("boolean");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality (1)", intProp, new Integer(2));
		assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
		assertEquals("Check strProp equality (1)", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity (1)", strAProp);
		String[] v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality (1)"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[] {1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (1)"); }
		}
		
		// Invoke
		FooService fs = (FooService) context.getService(sr);
		assertTrue("invoke fs", fs.foo());
		
		// Re-check the property (change)
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality (2)", intProp, new Integer(3));
		assertEquals("Check longProp equality (2)", boolProp, new Boolean(true));
		assertEquals("Check strProp equality (2)", strProp, new String("bar"));
		assertNotNull("Check strAProp not nullity (2)", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality (2)"); }
		}
		assertNotNull("Check intAProp not nullity (2)", intAProp);
		v2 = new int[] {3, 2, 1};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (2)"); }
		}
		
		fs = null;
		context.ungetService(sr);
	}
	
	public void testProperties2() {
		ServiceReference sr = Utils.getServiceReferenceByName(context, FooService.class.getName(), "FooProvider-2");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Boolean boolProp = (Boolean) sr.getProperty("boolean");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(4));
		assertEquals("Check longProp equality", boolProp, new Boolean(false));
		assertEquals("Check strProp equality", strProp, new String("bar"));
		assertNotNull("Check strAProp not nullity", strAProp);
		String[] v = new String[] {"bar", "foo"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[] {1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}
		
		// Invoke
		FooService fs = (FooService) context.getService(sr);
		assertTrue("invoke fs", fs.foo());
		
		// Re-check the property (change)
		intProp = (Integer) sr.getProperty("int");
		boolProp = (Boolean) sr.getProperty("boolean");
		strProp = (String) sr.getProperty("string");
		strAProp = (String[]) sr.getProperty("strAProp");
		intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(3));
		assertEquals("Check longProp equality", boolProp, new Boolean(true));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		v = new String[] {"foo", "bar", "baz"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		v2 = new int[] {3, 2, 1};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}
		
		fs = null;
		context.ungetService(sr);	
	}
	
	public void testProperties3() {
		ServiceReference sr = Utils.getServiceReferenceByName(context, FooService.class.getName(), "FooProvider-3");
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
		
		// Invoke
		FooService fs = (FooService) context.getService(sr);
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
		
		fs = null;
		context.ungetService(sr);	

	}

}
