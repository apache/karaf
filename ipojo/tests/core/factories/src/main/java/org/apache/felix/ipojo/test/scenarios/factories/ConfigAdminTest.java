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
package org.apache.felix.ipojo.test.scenarios.factories;

import java.util.Properties;

import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.factories.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;

/**
 * Check configuration admin reconfiguration.
 */
public class ConfigAdminTest extends OSGiTestCase {
	
	private ManagedServiceFactory getFactoryByName(String pid) {
		ServiceReference[] refs;
		try {
			refs = getContext().getServiceReferences(ManagedServiceFactory.class.getName(), "(service.pid="+pid+")");
			if(refs == null) { return null; }
			return ((org.osgi.service.cm.ManagedServiceFactory) getContext().getService(refs[0]));
		} catch (InvalidSyntaxException e) {
			System.err.println("Cannot get the factory " + pid + " : " + e.getMessage());
			return null;
		}
	}
	
	/**
	 * Check creation.
	 */
	public void testCreation() {
		ManagedServiceFactory f = getFactoryByName("Factories-FooProviderType-2");
		
		Properties  p = new Properties();
		p.put("int", new Integer(3));
		p.put("long", new Long(42));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		try {
			f.updated("ok2", p);
			ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "ok2");
			assertNotNull("Check instance creation", ref);
			f.deleted("ok2");
			ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "ok2");
			assertNull("Check instance deletion", ref);
		} catch (ConfigurationException e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}
	
	/**
	 * Check creation (push String).
	 */
	public void testCreationString() {
        ManagedServiceFactory f = getFactoryByName("Factories-FooProviderType-2");
        
        Properties  p = new Properties();
        p.put("int", "3");
        p.put("long", "42");
        p.put("string", "absdir");
        p.put("strAProp", "{a}");
        p.put("intAProp", "{1,2}");
        
        try {
            f.updated("ok2", p);
            ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "ok2");
            assertNotNull("Check instance creation", ref);
            f.deleted("ok2");
            ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "ok2");
            assertNull("Check instance deletion", ref);
        } catch (ConfigurationException e) {
            fail("An acceptable configuration is rejected : " + e.getMessage());
        }
    }
	
	/**
	 * Check update and delete.
	 */
	public void testUpdate() {
		ManagedServiceFactory f = getFactoryByName("Factories-FooProviderType-2");
		
		Properties  p = new Properties();
		p.put("int", new Integer(3));
		p.put("long", new Long(42));
		p.put("string", "absdir");
		p.put("strAProp", new String[] {"a"});
		p.put("intAProp", new int[] {1,2});
		
		try {
			f.updated("okkkk", p);
			ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
			assertNotNull("Check instance creation", ref);
			p.put("int", new Integer("4"));
			f.updated("okkkk", p);
			ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
			Integer test = (Integer) ref.getProperty("int");
			assertEquals("Check instance modification", 4, test.intValue());
			f.deleted("okkkk");
			ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
			assertNull("Check instance deletion", ref);
		} catch (ConfigurationException e) {
			fail("An acceptable configuration is rejected : " + e.getMessage());
		}
	}

    /**
     * Check update and delete.
     * (Push String).
     */
    public void testUpdateString() {
    	ManagedServiceFactory f = getFactoryByName("Factories-FooProviderType-2");
    	
    	Properties  p = new Properties();
    	p.put("int", "3");
    	p.put("long", "42");
    	p.put("string", "absdir");
    	p.put("strAProp", "{a}");
    	p.put("intAProp", "{1,2}");
    	
    	try {
    		f.updated("okkkk", p);
    		ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
    		assertNotNull("Check instance creation", ref);
    		p.put("int", new Integer("4"));
    		f.updated("okkkk", p);
    		ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
    		Integer test = (Integer) ref.getProperty("int");
    		assertEquals("Check instance modification", 4, test.intValue());
    		f.deleted("okkkk");
    		ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), "okkkk");
    		assertNull("Check instance deletion", ref);
    	} catch (ConfigurationException e) {
    		fail("An acceptable configuration is rejected : " + e.getMessage());
    	}
    }
	
}
