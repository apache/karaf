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

public class SimpleProperties extends OSGiTestCase {
	
	ComponentInstance fooProvider1;
	ComponentInstance fooProvider2;
	ComponentInstance fooProvider3;
	
	public void setUp() {
		String type = "CONFIG-FooProviderType-Conf";
		
		Properties p1 = new Properties();
		p1.put("instance.name","FooProvider-1");
		fooProvider1 = Utils.getComponentInstance(getContext(), type, p1);
		
		Properties p2 = new Properties();
		p2.put("instance.name","FooProvider-2");
		p2.put("int", new Integer(4));
		p2.put("boolean", new Boolean(false));
		p2.put("string", new String("bar"));
		p2.put("strAProp", new String[] {"bar", "foo"});
		p2.put("intAProp", new int[] {1, 2, 3});
		fooProvider2 = Utils.getComponentInstance(getContext(), type, p2);
		
		Properties p3 = new Properties();
        p3.put("instance.name","FooProvider-3");
        fooProvider3 = Utils.getComponentInstance(getContext(), "CONFIG-FooProviderType-ConfNoValue", p3);
	}
	
	public void tearDown() {
		fooProvider1.dispose();
		fooProvider2.dispose();
		fooProvider3.dispose();
		fooProvider1 = null;
		fooProvider2 = null;
		fooProvider3 = null;
	}
	
	public void testComponentTypeConfiguration() {
		ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider1.getInstanceName());
		assertNotNull("Check FooService availability", ref);
		FooService fs = (FooService) getContext().getService(ref);
		Properties toCheck = fs.fooProps();
		
		Integer intProp = (Integer) toCheck.get("intProp");
		Boolean boolProp = (Boolean) toCheck.get("boolProp");
		String strProp = (String) toCheck.get("strProp");
		String[] strAProp = (String[]) toCheck.get("strAProp");
		int[] intAProp = (int[]) toCheck.get("intAProp");
		
		assertEquals("Check intProp equality (1)", intProp, new Integer(2));
		assertEquals("Check longProp equality (1)", boolProp, new Boolean(false));
		assertEquals("Check strProp equality (1)", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity (1)", strAProp);
		String[] v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality (1) : " + strAProp[i] + " != " + v[i]); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[] {1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (1) : " + intAProp[i] + " != " + v2[i]); }
		}
		
		// change the field value
		assertTrue("Invoke the fs service", fs.foo());
		toCheck = fs.fooProps();
		
		
		//	Re-check the property (change)
		intProp = (Integer) toCheck.get("intProp");
		boolProp = (Boolean) toCheck.get("boolProp");
		strProp = (String) toCheck.get("strProp");
		strAProp = (String[]) toCheck.get("strAProp");
		intAProp = (int[]) toCheck.get("intAProp");
		
		assertEquals("Check intProp equality (2) ("+intProp+")", intProp, new Integer(3));
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
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality (2) : " + intAProp[i] + " != " + v2[i]); }
		}
		
		fs = null;
		getContext().ungetService(ref);
	}
	
	public void testInstanceConfiguration() {
		ServiceReference sr = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "FooProvider-2");
		assertNotNull("Check the availability of the FS service", sr);
		
		FooService fs = (FooService) getContext().getService(sr);
		Properties toCheck = fs.fooProps();
		
		// Check service properties
		Integer intProp = (Integer) toCheck.get("intProp");
		Boolean boolProp = (Boolean) toCheck.get("boolProp");
		String strProp = (String) toCheck.get("strProp");
		String[] strAProp = (String[]) toCheck.get("strAProp");
		int[] intAProp = (int[]) toCheck.get("intAProp");
		
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
		
		
		assertTrue("invoke fs", fs.foo());
		toCheck = fs.fooProps();
		
		// Re-check the property (change)
		intProp = (Integer) toCheck.get("intProp");
		boolProp = (Boolean) toCheck.get("boolProp");
		strProp = (String) toCheck.get("strProp");
		strAProp = (String[]) toCheck.get("strAProp");
		intAProp = (int[]) toCheck.get("intAProp");
		
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
		getContext().ungetService(sr);	
	}
	
	public void testNoValue() {
        ServiceReference sr = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "FooProvider-3");
        assertNotNull("Check the availability of the FS service", sr);
        
        FooService fs = (FooService) getContext().getService(sr);
        Properties toCheck = fs.fooProps();
        
        // Check service properties
        Integer intProp = (Integer) toCheck.get("intProp");
        Boolean boolProp = (Boolean) toCheck.get("boolProp");
        String strProp = (String) toCheck.get("strProp");
        String[] strAProp = (String[]) toCheck.get("strAProp");
        int[] intAProp = (int[]) toCheck.get("intAProp");
        
        assertEquals("Check intProp equality", intProp, new Integer(0));
        assertEquals("Check longProp equality", boolProp, new Boolean(false));
        assertEquals("Check strProp equality", strProp, null);
        assertNull("Check strAProp nullity", strAProp);
        assertNull("Check intAProp  nullity", intAProp);
       
        assertTrue("invoke fs", fs.foo());
        toCheck = fs.fooProps();
        
        // Re-check the property (change)
        intProp = (Integer) toCheck.get("intProp");
        boolProp = (Boolean) toCheck.get("boolProp");
        strProp = (String) toCheck.get("strProp");
        strAProp = (String[]) toCheck.get("strAProp");
        intAProp = (int[]) toCheck.get("intAProp");
        
        assertEquals("Check intProp equality", intProp, new Integer(3));
        assertEquals("Check longProp equality", boolProp, new Boolean(true));
        assertEquals("Check strProp equality", strProp, new String("bar"));
        assertNotNull("Check strAProp not nullity", strAProp);
        String[] v = new String[] {"foo", "bar", "baz"};
        for (int i = 0; i < strAProp.length; i++) {
            if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
        }
        assertNotNull("Check intAProp not nullity", intAProp);
        int[] v2 = new int[] {3, 2, 1};
        for (int i = 0; i < intAProp.length; i++) {
            if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
        }
        
        fs = null;
        getContext().ungetService(sr);   
    }

}
