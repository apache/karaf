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

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.CheckService;
import org.apache.felix.ipojo.test.composite.service.FooService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class ServiceRangeTest extends OSGiTestCase {
	
	private Factory emptyFactory;
	private ComponentInstance empty;

	
	public void setUp() {
		emptyFactory = Utils.getFactoryByName(getContext(), "composite.empty");
		Properties props = new Properties();
		props.put("instance.name","empty-1");
		try {
			empty = emptyFactory.createComponentInstance(props);
		} catch(Exception e) { fail("Cannot create empty instance " + e.getMessage()); }
	}
	
	public void tearDown() {
		empty.dispose();
		empty = null;
	}
	
	public void testLevelOne1() {
		ServiceContext sc2 = Utils.getServiceContext(empty);
		
		Factory fact1 = Utils.getFactoryByName(sc2, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc2, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		// Check visibility 
		assertNotNull("Check foo service visible inside the composite", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the context", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		provider.dispose();
		client.dispose();
		 
		assertNull("Check foo service invisible inside the composite", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible from the context", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
	}
	
	public void testLevelOne2() {
		ServiceContext sc2 = Utils.getServiceContext(empty);
		
		Factory fact1 = Utils.getFactoryByName(sc2, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props, sc2);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc2, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2, sc2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		// Check visibility 
		assertNotNull("Check foo service visible inside the composite", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the context", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
	}
	
	public void testLevelOne3() {
		ServiceContext sc2 = Utils.getServiceContext(empty);
		
		Factory fact1 = Utils.getFactoryByName(getContext(), "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props, sc2);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(getContext(), "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2, sc2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		// Check visibility 
		assertNotNull("Check foo service visible inside the composite", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the context", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
	}
	
	public void testLevelTwo1() {
		ServiceContext sc1 = Utils.getServiceContext(empty);		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("instance.name","empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch(Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		
		Factory fact1 = Utils.getFactoryByName(sc2, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc2, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		//	Check visibility 
		assertNotNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		empty2.dispose();
	}
	
	public void testLevelTwo2() {
		ServiceContext sc1 = Utils.getServiceContext(empty);		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("instance.name","empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch(Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		
		Factory fact1 = Utils.getFactoryByName(sc1, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props, sc2);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc1, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2, sc2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		//	Check visibility 
		assertNotNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		empty2.dispose();
	}
	
	public void testLevelTwo3() {
		ServiceContext sc1 = Utils.getServiceContext(empty);		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("instance.name","empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch(Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		
		Factory fact1 = Utils.getFactoryByName(getContext(), "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props, sc2);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(getContext(), "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2, sc2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		//	Check visibility 
		assertNotNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		empty2.dispose();
	}
	
	public void testLevelTwo4() {
		ServiceContext sc1 = Utils.getServiceContext(empty);		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("instance.name","empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch(Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		
		Factory fact1 = Utils.getFactoryByName(sc2, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("instance.name","client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props, sc2);
		} catch(Exception e) { fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc2, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("instance.name","provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2, sc2);
		} catch(Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		
		sc2.ungetService(ref);
		
		//	Check visibility 
		assertNotNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNotNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		// Check invisibilty
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		
		client.dispose();
		provider.dispose();
		 
		assertNull("Check foo service visible inside the composite 2", sc2.getServiceReference(FooService.class.getName()));
		assertNull("Check check service visible inside the composite 2", sc2.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the composite 1", sc1.getServiceReference(FooService.class.getName()));
		assertNull("Check check service invisible inside the composite 1", sc1.getServiceReference(CheckService.class.getName()));
		assertNull("Check foo service invisible inside the global", getContext().getServiceReference(FooService.class.getName()));
		try {
			assertNull("Check check service invisible inside the context", getContext().getServiceReferences(CheckService.class.getName(), "(instance.name=client)"));
		} catch (InvalidSyntaxException e) {
			fail("Invalid filter : " + e);
		}
		empty2.dispose();
	}
	
	
}
