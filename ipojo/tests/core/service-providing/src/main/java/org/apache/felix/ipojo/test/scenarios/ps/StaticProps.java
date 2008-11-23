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

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class StaticProps extends OSGiTestCase {

	IPOJOHelper helper;
	
	public void setUp() {
	    helper = new IPOJOHelper(this);
		String type = "PS-FooProviderType-2";

		helper.createComponentInstance(type, "FooProvider-1");
		
		Properties p2 = new Properties();
		p2.put("instance.name","FooProvider-2");
		p2.put("int", new Integer(4));
		p2.put("long", new Long(42));
		p2.put("string", new String("bar"));
		p2.put("strAProp", new String[] {"bar", "foo"});
		p2.put("intAProp", new int[] {1, 2, 3});
		helper.createComponentInstance(type, p2);
		
	}
	
	public void tearDown() {
	    helper.dispose();
	}
	
	public void testProperties1() {
		ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-1");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Long longProp = (Long) sr.getProperty("long");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(2));
		assertEquals("Check longProp equality", longProp, new Long(40));
		assertEquals("Check strProp equality", strProp, new String("foo"));
		assertNotNull("Check strAProp not nullity", strAProp);
		String[] v = new String[] {"foo", "bar"};
		for (int i = 0; i < strAProp.length; i++) {
			if(!strAProp[i].equals(v[i])) { fail("Check the strAProp Equality"); }
		}
		assertNotNull("Check intAProp not nullity", intAProp);
		int[] v2 = new int[] {1, 2, 3};
		for (int i = 0; i < intAProp.length; i++) {
			if(intAProp[i] != v2[i]) { fail("Check the intAProp Equality"); }
		}
		       
	}
	
	public void testProperties2() {
		ServiceReference sr = helper.getServiceReferenceByName(FooService.class.getName(), "FooProvider-2");
		assertNotNull("Check the availability of the FS service", sr);
		
		// Check service properties
		Integer intProp = (Integer) sr.getProperty("int");
		Long longProp = (Long) sr.getProperty("long");
		String strProp = (String) sr.getProperty("string");
		String[] strAProp = (String[]) sr.getProperty("strAProp");
		int[] intAProp = (int[]) sr.getProperty("intAProp");
		
		assertEquals("Check intProp equality", intProp, new Integer(4));
		assertEquals("Check longProp equality", longProp, new Long(42));
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
	}
}
