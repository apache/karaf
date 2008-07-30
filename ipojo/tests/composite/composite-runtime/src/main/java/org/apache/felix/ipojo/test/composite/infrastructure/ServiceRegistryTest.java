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
package org.apache.felix.ipojo.test.composite.infrastructure;

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.context.ServiceRegistry;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.BarService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ServiceRegistryTest extends OSGiTestCase {
	
	private class svcListener implements ServiceListener {
		public int registration = 0;
		public int unregistration = 0;
		public int modification = 0;
		public void serviceChanged(ServiceEvent ev) {
			if(ev.getType() == ServiceEvent.REGISTERED) { registration++; }
			if(ev.getType() == ServiceEvent.UNREGISTERING) { unregistration++; }
			if(ev.getType() == ServiceEvent.MODIFIED) { modification++; }
		}
	}
	
	private class barProvider implements BarService {

		public boolean bar() { return true; }

		public Properties getProps() { return null; }

	}
	
	private class FakeComponent implements ComponentInstance {

		public ComponentTypeDescription getComponentDescription() {
			return null;
		}

		public BundleContext getContext() {
			return null;
		}

		public ComponentFactory getFactory() {
			return null;
		}

		public InstanceDescription getInstanceDescription() {
			return null;
		}

		public String getInstanceName() {
			return null;
		}

		public int getState() {
			return 0;
		}

		public boolean isStarted() {
			return false;
		}

		public void reconfigure(Dictionary dictionary) { }

		public void start() { }

		public void stop() { }
		
		public void dispose() { }

		public void addInstanceStateListener(InstanceStateListener arg0) { }

		public void removeInstanceStateListener(InstanceStateListener arg0) { }
		
	}
	
	public void testRegistrationAndListener() {
		ComponentInstance im = new FakeComponent();
		ComponentInstance im2 = new FakeComponent();
		ServiceRegistry registry = new ServiceRegistry(context);
		assertNotNull("Assert registry not null", registry);
		svcListener all = new svcListener();
		try {
			assertNull("Check that there is no available service", registry.getServiceReferences(null, null));
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
		registry.addServiceListener(all);
		
		ServiceRegistration reg1 = registry.registerService(im, BarService.class.getName(), new barProvider(), null);
		
		try {
			assertEquals("Check number of registred service", 1, registry.getServiceReferences(null, null).length);
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); 
		 }
	
	    ServiceRegistration reg2 = registry.registerService(im2, BarService.class.getName(), new barProvider(), null);
			
	   try {
			assertEquals("Check number of registred service", 2, registry.getServiceReferences(null, null).length);
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
	    
		assertEquals("Check the number of registration", 2, all.registration);
		
		Properties props = new Properties();
		props.put("foo", "bar");
		reg1.setProperties(props);
		assertEquals("Check the number of modification", 1, all.modification);
		
		reg1.unregister();
		assertEquals("Check the number of unregistration", 1, all.unregistration);
		
		reg2.setProperties(props);
		assertEquals("Check the number of modification", 2, all.modification);
		
		reg2.unregister();
		assertEquals("Check the number of unregistration", 2, all.unregistration);
		
		registry.removeServiceListener(all);
	}
	
	public void testRegistrationAndFilter() {
		ComponentInstance im = new FakeComponent();
		ComponentInstance im2 = new FakeComponent();
		ServiceRegistry registry = new ServiceRegistry(context);
		svcListener filtered = new svcListener();
		
		try {
			assertNull("Check that there is no available service", registry.getServiceReferences(null, null));
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
		  
		registry.addServiceListener(filtered, "(foo=bar)");
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration reg1 = registry.registerService(im, BarService.class.getName(), new barProvider(), props);
		
		try {
			assertEquals("Check number of registred service", 1, registry.getServiceReferences(null, null).length);
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
		  
	  ServiceRegistration reg2 = registry.registerService(im2, BarService.class.getName(), new barProvider(), null);
			
	   try {
			assertEquals("Check number of registred service", 2, registry.getServiceReferences(null, null).length);
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
			  
		assertEquals("Check the number of registration", 1, filtered.registration);
		
		reg2.setProperties(props);
		assertEquals("Check the number of modification", 1, filtered.modification);
		// Follow the OSGi semantics of filters
		
		reg1.unregister();
		reg2.unregister();
		assertEquals("Check the number of unregistration", 2, filtered.unregistration);
		registry.removeServiceListener(filtered);
	}
	
	public void testGetService() {
		ComponentInstance im = new FakeComponent();
		ComponentInstance im2 = new FakeComponent();
		ServiceRegistry registry = new ServiceRegistry(context);
		
		try {
			assertNull("Check that there is no available service", registry.getServiceReferences(null, null));
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration reg1 = registry.registerService(im, BarService.class.getName(), new barProvider(), props);
		
		ServiceReference ref = registry.getServiceReference(BarService.class.getName());
		assertNotNull("Check ref not null", ref);
		assertEquals("Test property", ref.getProperty("foo"), "bar");
		BarService bar = (BarService) registry.getService(im2, ref);
		assertTrue("Test invocation", bar.bar());
		
		reg1.unregister();
		ref = registry.getServiceReference(BarService.class.getName());
		assertNull("Check ref null", ref);
	}
	
	public void testGetFilteredService() {
		ComponentInstance im = new FakeComponent();
		ComponentInstance im2 = new FakeComponent();
		ServiceRegistry registry = new ServiceRegistry(context);
		
		try {
			assertNull("Check that there is no available service", registry.getServiceReferences(null, null));
		} catch (InvalidSyntaxException e) { fail("Cannot query the registry : " + e.getMessage()); }
		
		Properties props = new Properties();
		props.put("foo", "bar");
		ServiceRegistration reg1 = registry.registerService(im, BarService.class.getName(), new barProvider(), props);
		ServiceRegistration reg2 = registry.registerService(im2, BarService.class.getName(), new barProvider(), null);
		
		ServiceReference[] ref = null;
		try {
			ref = registry.getServiceReferences(BarService.class.getName(), "(foo=bar)");
		} catch (InvalidSyntaxException e) { fail("Registry query fail : " + e.getMessage()); }
		assertNotNull("Check ref not null", ref);
		assertEquals("Check ref count", ref.length, 1);
		assertEquals("Test property", ref[0].getProperty("foo"), "bar");
		BarService bar = (BarService) registry.getService(im2, ref[0]);
		assertTrue("Test invocation", bar.bar());
		
		ref = null;
		reg1.unregister();
		try {
			ref = registry.getServiceReferences(BarService.class.getName(), "(bar=foo)");
		} catch (InvalidSyntaxException e) { fail("Registry query fail : " + e.getMessage()); }
		assertNull("Check ref null", ref);
		
		reg2.unregister();
	}
	
	

}
