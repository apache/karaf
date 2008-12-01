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
package org.apache.felix.ipojo.test.composite.instance;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.FooService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.ServiceReference;

public class SimpleInstance extends OSGiTestCase {

	private ComponentFactory fooFactory1, fooFactory2;
    private ComponentFactory compoFactory;
	private ComponentInstance empty;
	
	
	public void setUp() {
        fooFactory1 = (ComponentFactory) Utils.getFactoryByName(getContext(), "COMPO-FooProviderType-1");
        fooFactory2 = (ComponentFactory) Utils.getFactoryByName(getContext(), "COMPO-FooProviderType-Dyn2");
        compoFactory = (ComponentFactory) Utils.getFactoryByName(getContext(), "composite.inst.1");
		Factory fact = Utils.getFactoryByName(getContext(), "composite.empty");
		Properties props = new Properties();
		props.put("instance.name","empty-X");
		try {
			empty = fact.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot create the empty composite : " + e.getMessage());
		}
	}
	
	public void tearDown() {
		empty.dispose();
		empty = null;
	}
	
	public void testCreation() {
		Properties props = new Properties();
		props.put("instance.name","under-A");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props);
		} catch(Exception e) {
		    e.printStackTrace();
			fail("Cannot instantiate under from " + compoFactory.getName() + " -> " + e.getMessage());
		}      
		
		assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
		under.dispose();
	}
	
	public void testServiceAvailability() {
		Properties props = new Properties();
		props.put("instance.name","under");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}	
		assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(under);
       
		assertNotNull("Check service availability", sc.getServiceReference(FooService.class.getName()));
        assertEquals("Check service provider", Utils.getServiceReferences(sc, FooService.class.getName(), null).length, 2);
		
		under.dispose();
	}
	
	public void testCreationLevel2() {
		ServiceContext sc = Utils.getServiceContext(empty);
		Properties props = new Properties();
		props.put("instance.name","under");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props, sc);
		} catch(Exception e) {
		    e.printStackTrace();
			fail("Cannot instantiate under : " + e.getMessage());
		}
		assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
		under.dispose();
	}
	
	public void testServiceAvailabilityLevel2() {
		ServiceContext sc = Utils.getServiceContext(empty);
		Properties props = new Properties();
		props.put("instance.name","under-X");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props, sc);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}	
		assertTrue("Check instance validity", under.getState() == ComponentInstance.VALID);
		ServiceContext sc2 = Utils.getServiceContext(under);
        
        assertNotNull("Check service availability", sc2.getServiceReference(FooService.class.getName()));
        assertEquals("Check service providers", Utils.getServiceReferences(sc2, FooService.class.getName(), null).length, 2);
		
		under.dispose();
	}
	
	public void testFactoryManagement() {
		Properties props = new Properties();
		props.put("instance.name","under");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		assertTrue("Check instance validity - 1", under.getState() == ComponentInstance.VALID);
		
		fooFactory1.stop();
		assertTrue("Check instance invalidity - 2", under.getState() == ComponentInstance.INVALID);
        
        fooFactory1.start();
		assertTrue("Check instance validity - 3", under.getState() == ComponentInstance.VALID);
        
		fooFactory2.stop();
        assertTrue("Check instance invalidity", under.getState() == ComponentInstance.INVALID);
		
		fooFactory2.start();
		assertTrue("Check instance validity - 4", under.getState() == ComponentInstance.VALID);
		
		under.dispose();
		fooFactory1.start();
		fooFactory2.start();
	}
	
	public void testFactoryManagementLevel2() {
		ServiceContext sc = Utils.getServiceContext(empty);
		Properties props = new Properties();
		props.put("instance.name","under");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props, sc);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		assertTrue("Check instance validity - 1", under.getState() == ComponentInstance.VALID);
		
		assertTrue("Check instance validity - 1", under.getState() == ComponentInstance.VALID);
        
        fooFactory1.stop();
        assertTrue("Check instance invalidity - 2", under.getState() == ComponentInstance.INVALID);
        
        fooFactory1.start();
        assertTrue("Check instance validity - 3", under.getState() == ComponentInstance.VALID);
        
        fooFactory2.stop();
        assertTrue("Check instance invalidity", under.getState() == ComponentInstance.INVALID);
        
        fooFactory2.start();
        assertTrue("Check instance validity - 4", under.getState() == ComponentInstance.VALID);
        
        under.dispose();
        fooFactory1.start();
        fooFactory2.start();
	}
	
	public void atestArchitecture() { // TODO fix and reactivate the method.
		Properties props = new Properties();
		props.put("instance.name","under");
		ComponentInstance under = null;
		try {
			under = compoFactory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Cannot instantiate under : " + e.getMessage());
		}
		ServiceReference ref = Utils.getServiceReference(getContext(), Architecture.class.getName(), "(architecture.instance=under)");
		assertNotNull("Check architecture availability", ref);
		Architecture arch = (Architecture) getContext().getService(ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		InstanceDescription[] contained = id.getContainedInstances();
		assertEquals("Check contained instances count (" + contained.length + ")", contained.length, 1);
		assertEquals("Check that no object are created" , id.getCreatedObjects().length, 0);
		assertEquals("Check instance name" , id.getName(), "under");
		assertEquals("Check component type name" , id.getComponentDescription().getName(), "composite.bar.1");
		
		ComponentFactory fact1 = (ComponentFactory) Utils.getFactoryByName(getContext(), "COMPO-FooBarProviderType-1");
		ComponentFactory fact2 = (ComponentFactory) Utils.getFactoryByName(getContext(), "COMPO-FooBarProviderType-2");
		ComponentFactory fact3 = (ComponentFactory) Utils.getFactoryByName(getContext(), "COMPO-FooBarProviderType-3");
		
		fact1.stop();
		assertTrue("Check instance validity - 2", under.getState() == ComponentInstance.VALID);
		ref = Utils.getServiceReference(getContext(), Architecture.class.getName(), "(architecture.instance=under)");
		assertNotNull("Check architecture availability", ref);
		arch = (Architecture) getContext().getService(ref);
		id = arch.getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		contained = id.getContainedInstances();
		assertEquals("Check contained instances count", contained.length, 1);
		assertEquals("Check that no object are created" , id.getCreatedObjects().length, 0);
		assertEquals("Check instance name" , id.getName(), "under");
		assertEquals("Check component type name" , id.getComponentDescription().getName(), "composite.bar.1");
		
		fact2.stop();
		assertTrue("Check instance validity - 3", under.getState() == ComponentInstance.VALID);
		ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "under");
		assertNotNull("Check architecture availability", ref);
		arch = (Architecture) getContext().getService(ref);
		id = arch.getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		contained = id.getContainedInstances();
		assertEquals("Check contained instances count", contained.length, 1);
		assertEquals("Check that no object are created" , id.getCreatedObjects().length, 0);
		assertEquals("Check instance name" , id.getName(), "under");
		assertEquals("Check component type name" , id.getComponentDescription().getName(), "composite.bar.1");

		fact3.stop();
		assertTrue("Check instance invalidity", under.getState() == ComponentInstance.INVALID);
		ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "under");
		assertNotNull("Check architecture availability", ref);
		arch = (Architecture) getContext().getService(ref);
		id = arch.getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.INVALID);
		contained = id.getContainedInstances();
		assertEquals("Check contained instances count", contained.length, 0);
		assertEquals("Check that no object are created" , id.getCreatedObjects().length, 0);
		assertEquals("Check instance name" , id.getName(), "under");
		assertEquals("Check component type name" , id.getComponentDescription().getName(), "composite.bar.1");

		fact1.start();
		assertTrue("Check instance validity - 4", under.getState() == ComponentInstance.VALID);
		ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), "under");
		assertNotNull("Check architecture availability", ref);
		arch = (Architecture) getContext().getService(ref);
		id = arch.getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		contained = id.getContainedInstances();
		assertEquals("Check contained instances count", contained.length, 1);
		assertEquals("Check that no object are created" , id.getCreatedObjects().length, 0);
		assertEquals("Check instance name" , id.getName(), "under");
		assertEquals("Check component type name" , id.getComponentDescription().getName(), "composite.bar.1");

		under.dispose();
		fact2.start();
		fact3.start();
	}

}
