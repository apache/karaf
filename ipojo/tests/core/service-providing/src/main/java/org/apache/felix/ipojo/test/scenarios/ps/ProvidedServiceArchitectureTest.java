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
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.ps.service.BarService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class ProvidedServiceArchitectureTest extends OSGiTestCase {
	
	
	public void testExposition() {
		String factName = "PS-FooProviderType-1";
		String compName = "FooProvider-1";
		
		// Get the factory to create a component instance
		Factory fact = Utils.getFactoryByName(context, factName);
		assertNotNull("Cannot find the factory FooProvider-1", fact);
		
		Properties props = new Properties();
		props.put("instance.name",compName);
		ComponentInstance ci = null;
		try {
			ci = fact.createComponentInstance(props);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), "FooProvider-1");
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) context.getService(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 2);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
		for(int i = 0; i < handlers.length; i++) {
			if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
				pshd = (ProvidedServiceHandlerDescription) handlers[i];
			}
		}
		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecification().length, 1);
		assertEquals("Check Provided Service Specs - 2", ps[0].getServiceSpecification()[0], FooService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
		Properties prop = ps[0].getProperties();
		assertNotNull("Check Props", prop);
		assertEquals("Check service properties number", prop.size(), 2);
		assertEquals("Check instance.name property", prop.getProperty("instance.name"), compName);
		assertEquals("Check factory.name property", prop.getProperty("factory.name"), factName);
		
		ci.dispose();
	}
	
	public void testProps() {
		String factName = "PS-FooProviderType-3";
		String compName = "FooProvider";
		
		// Get the factory to create a component instance
		Factory fact = Utils.getFactoryByName(context, factName);
		assertNotNull("Cannot find the factory FooProvider", fact);
		
		Properties props = new Properties();
		props.put("instance.name",compName);
		props.put("foo", "foo");
		props.put("bar", "2");
		props.put("baz", "baz");
		ComponentInstance ci = null;
		try {
			ci = fact.createComponentInstance(props);
		} catch (Exception e) { fail(e.getMessage()); }

		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), compName);
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) context.getService(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 3);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
		for(int i = 0; i < handlers.length; i++) {
			if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
				pshd = (ProvidedServiceHandlerDescription) handlers[i];
			}
		}
		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecification().length, 1);
		assertEquals("Check Provided Service Specs - 2", ps[0].getServiceSpecification()[0], FooService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
	
		Properties prop = ps[0].getProperties();
		assertNotNull("Check Props", prop);
		assertEquals("Check service properties number (#" + prop + "?=5)" , prop.size(), 5);
		assertEquals("Check instance.name property", prop.getProperty("instance.name"), compName);
		assertEquals("Check factory.name property", prop.getProperty("factory.name"), factName);
		assertEquals("Check foo property", prop.getProperty("foo"), "foo");
		assertEquals("Check bar property", prop.getProperty("bar"), "2");
		assertEquals("Check baz property", prop.getProperty("baz"), "baz");
		
		ci.dispose();
	}
	
	public void testDoubleProviding() {
		String factName = "PS-FooBarProviderType-1";
		String compName = "FooProvider";
		
		// Get the factory to create a component instance
		Factory fact = Utils.getFactoryByName(context, factName);
		assertNotNull("Cannot find the factory FooProvider", fact);
		
		Properties props = new Properties();
		props.put("instance.name",compName);
		ComponentInstance ci = null;
		try {
			ci = fact.createComponentInstance(props);
		} catch (Exception e) {
			fail(e.getMessage());
		}

		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), compName);
		assertNotNull("Architecture not available", arch_ref);

		Architecture arch = (Architecture) context.getService(arch_ref);
		InstanceDescription id = arch.getInstanceDescription();
		
		assertEquals("Check component instance name (" + id.getName() + ")", id.getName(), compName);
		assertEquals("Check component type implementation class", id.getComponentDescription().getClassName(), "org.apache.felix.ipojo.test.scenarios.component.FooBarProviderType1");
		
		HandlerDescription[] handlers = id.getHandlers();
		assertEquals("Number of handlers", handlers.length, 2);
		
		//Look for the ProvidedService Handler
		ProvidedServiceHandlerDescription pshd = null;
		for(int i = 0; i < handlers.length; i++) {
			if(handlers[i].getHandlerName().equals("org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler")) {
				pshd = (ProvidedServiceHandlerDescription) handlers[i];
			}
		}
		
		assertNotNull("Check ProvidedServiceHandlerDescription", pshd);
		ProvidedServiceDescription[] ps = pshd.getProvidedServices();
		
		assertEquals("Check ProvidedService number", ps.length, 1);
		assertEquals("Check Provided Service Specs - 1", ps[0].getServiceSpecification().length, 2);
		assertContains("Check provided service specs - 2", ps[0].getServiceSpecification(), FooService.class.getName());;
		assertContains("Check provided service specs - 2", ps[0].getServiceSpecification(), BarService.class.getName());
		assertEquals("Check Provided Service availability", ps[0].getState(), ProvidedServiceDescription.REGISTERED);
		
		ci.dispose();
	}
	
}
