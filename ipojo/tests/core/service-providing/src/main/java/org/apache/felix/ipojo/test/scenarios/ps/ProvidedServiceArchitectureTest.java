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

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.BarService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class ProvidedServiceArchitectureTest extends OSGiTestCase {
	
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    public void tearDown() {
        helper.dispose();
    }
	
	public void testExposition() {
		String factName = "PS-FooProviderType-1";
		String compName = "FooProvider-1";
		
		// Get the factory to create a component instance
		Factory fact = helper.getFactory( factName);
		assertNotNull("Cannot find the factory FooProvider-1", fact);

		helper.createComponentInstance(factName, compName);

		ServiceReference arch_ref = helper.getServiceReferenceByName(Architecture.class.getName(), compName);		
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) getServiceObject(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 2);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
        pshd = (ProvidedServiceHandlerDescription) id.getHandlerDescription("org.apache.felix.ipojo:provides");

//		for(int i = 0; i < handlers.length; i++) {
//			if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
//				pshd = (ProvidedServiceHandlerDescription) handlers[i];
//			}
//		}
//		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecifications().length, 1);
		assertEquals("Check Provided Service Specs - 2", ps[0].getServiceSpecifications()[0], FooService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
		Properties prop = ps[0].getProperties();
		assertNotNull("Check Props", prop);
		assertEquals("Check service properties number", prop.size(), 2);
		assertEquals("Check instance.name property", prop.getProperty("instance.name"), compName);
		assertEquals("Check factory.name property", prop.getProperty("factory.name"), factName);
	}
	
	public void testProps() {
		String factName = "PS-FooProviderType-3";
		String compName = "FooProvider";
		
		// Get the factory to create a component instance
		Factory fact = helper.getFactory( factName);
		assertNotNull("Cannot find the factory FooProvider", fact);
		
		Properties props = new Properties();
		props.put("instance.name",compName);
		props.put("foo", "foo");
		props.put("bar", "2");
		props.put("baz", "baz");
		helper.createComponentInstance(factName, props);

		ServiceReference arch_ref = helper.getServiceReferenceByName(Architecture.class.getName(), compName);
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) getServiceObject(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 3);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
		pshd = (ProvidedServiceHandlerDescription) id.getHandlerDescription("org.apache.felix.ipojo:provides");

		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecifications().length, 1);
		assertEquals("Check Provided Service Specs - 2", ps[0].getServiceSpecifications()[0], FooService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
	
		Properties prop = ps[0].getProperties();
		assertNotNull("Check Props", prop);
		assertEquals("Check service properties number (#" + prop + "?=5)" , prop.size(), 5);
		assertEquals("Check instance.name property", prop.getProperty("instance.name"), compName);
		assertEquals("Check factory.name property", prop.getProperty("factory.name"), factName);
		assertEquals("Check foo property", prop.get("foo"), "foo");
		assertEquals("Check bar property", prop.get("bar"), new Integer(2));
		assertEquals("Check baz property", prop.get("baz"), "baz");
		
	}
	
	public void testDoubleProviding() {
		String factName = "PS-FooBarProviderType-1";
		String compName = "FooProvider";
		
		// Get the factory to create a component instance
		Factory fact = helper.getFactory( factName);
		assertNotNull("Cannot find the factory FooProvider", fact);
		
		helper.createComponentInstance(factName, compName);

		ServiceReference arch_ref = helper.getServiceReferenceByName(Architecture.class.getName(), compName);
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) getServiceObject(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooBarProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 2);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
        pshd = (ProvidedServiceHandlerDescription) id.getHandlerDescription("org.apache.felix.ipojo:provides");

//		for(int i = 0; i < handlers.length; i++) {
//			if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
//				pshd = (ProvidedServiceHandlerDescription) handlers[i];
//			}
//		}
		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecifications().length, 2);
		assertContains("Check provided service specs - 2", ps[0].getServiceSpecifications(), FooService.class.getName());;
		assertContains("Check provided service specs - 2", ps[0].getServiceSpecifications(), BarService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
		
	}

    public void testPropsNoValue() {
    	String factName = "PS-FooProviderType-3";
    	String compName = "FooProvider";
    	
    	// Get the factory to create a component instance
    	Factory fact = helper.getFactory( factName);
    	assertNotNull("Cannot find the factory FooProvider", fact);
    		
        helper.createComponentInstance(factName, compName);
    
    	ServiceReference arch_ref = helper.getServiceReferenceByName(Architecture.class.getName(), compName);
    	assertNotNull("Architecture not available", arch_ref);
    
    	Architecture arch = (Architecture) getServiceObject(arch_ref);
    	InstanceDescription id = arch.getInstanceDescription();
    	
    	assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
    	assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
    	
    	HandlerDescription[] handlers = id.getHandlers();
    	assertEquals("Number of handlers", handlers.length, 3);
    	
    	//Look for the ProvidedService Handler
    	ProvidedServiceHandlerDescription pshd = null;
        pshd = (ProvidedServiceHandlerDescription) id.getHandlerDescription("org.apache.felix.ipojo:provides");

//    	for(int i = 0; i < handlers.length; i++) {
//    		if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
//    			pshd = (ProvidedServiceHandlerDescription) handlers[i];
//    		}
//    	}
    	
    	assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
    	ProvidedServiceDescription[] ps = pshd.getProvidedServices();
    	
    	assertEquals("Check ProvidedService number", ps.length, 1);
    	assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecifications().length, 1);
    	assertEquals("Check Provided Service Specs - 2", ps[0].getServiceSpecifications()[0], FooService.class.getName());
    	assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
    
    	Properties prop = ps[0].getProperties();
    	assertNotNull("Check Props", prop);
    	assertEquals("Check service properties number (#" + prop + "?=5)" , prop.size(), 2);
    	assertEquals("Check instance.name property", prop.getProperty("instance.name"), compName);
    	assertEquals("Check factory.name property", prop.getProperty("factory.name"), factName);

    }
	
}
