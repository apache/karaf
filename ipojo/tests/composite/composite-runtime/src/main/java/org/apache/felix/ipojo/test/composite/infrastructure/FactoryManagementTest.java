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
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.FactoryStateListener;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.test.composite.service.CheckService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class FactoryManagementTest extends OSGiTestCase {
	
	private FakeFactory fake1 = new FakeFactory("fake");
	private FakeFactory fake2 = new FakeFactory("fake2");
	
	private Factory emptyFactory;
	private ComponentInstance empty;
	
	private class FakeFactory implements Factory {
		
		private String m_name;
		public FakeFactory(String name) { m_name = name; } 

		public ComponentInstance createComponentInstance(Dictionary arg0) throws UnacceptableConfiguration { return null; }
		public ComponentInstance createComponentInstance(Dictionary arg0, ServiceContext arg1) throws UnacceptableConfiguration { return null; }
		public Element getDescription() { return null; }
		public String getName() { return m_name; }
		public boolean isAcceptable(Dictionary arg0) { return false; }
		public void reconfigure(Dictionary arg0) throws UnacceptableConfiguration {	}
        public void addFactoryStateListener(FactoryStateListener arg0) { }
        public List getMissingHandlers() { return null;  }
        public List getRequiredHandlers() { return null;  }
        public void removeFactoryStateListener(FactoryStateListener arg0) { }
        public ComponentTypeDescription getComponentDescription() { return null; }
        public String getClassName() { return ""; }
        public int getState() { return Factory.VALID; }
        public BundleContext getBundleContext() { return context; }

	}
	
	public void setUp() {
		emptyFactory = Utils.getFactoryByName(context, "composite.empty");
		Properties props = new Properties();
		props.put("name", "empty-1");
		try {
			empty = emptyFactory.createComponentInstance(props);
		} catch (Exception e) { fail("Cannot create empty instance " + e.getMessage()); }
	}
	
	public void tearDown() {
		empty.dispose();
		empty = null;
	}
	
	public void testOneLevelExposition() {
		ServiceReference[] parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		ServiceContext sc = Utils.getServiceContext(empty);
		ServiceReference[] internalFactoryReferences = Utils.getServiceReferences(sc, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories", parentsFactoryReferences.length, internalFactoryReferences.length);
		
		for(int i = 0; i < parentsFactoryReferences.length; i++) {
			Factory factory = (Factory) context.getService(parentsFactoryReferences[i]);
			assertTrue("Check the avaibility of " + factory.getName(), isExposed(factory, internalFactoryReferences, sc));
		}
	}
	
	public void testTwoLevelExposition() {
		ServiceReference[] parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		ServiceContext sc1 = Utils.getServiceContext(empty);
		ServiceReference[] Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("name", "empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch (Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		ServiceReference[] Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		for(int i = 0; i < Level1FactoryReferences.length; i++) {
			Factory factory = (Factory) context.getService(parentsFactoryReferences[i]);
			assertTrue("Check the avaibility of " + factory.getName(), isExposed(factory, Level2FactoryReferences, sc2));
		}
		
		empty2.dispose();
	}
	
	public void testDynamism() {
		ServiceReference[] parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		ServiceContext sc1 = Utils.getServiceContext(empty);
		ServiceReference[] Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("name", "empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch (Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		ServiceReference[] Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		for(int i = 0; i < Level1FactoryReferences.length; i++) {
			Factory factory = (Factory) context.getService(parentsFactoryReferences[i]);
			assertTrue("Check the avaibility of " + factory.getName(), isExposed(factory, Level2FactoryReferences, sc2));
		}
		
		// Publish fake1
		ServiceRegistration reg1 = context.registerService(Factory.class.getName(), fake1, null);
		
		parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		sc1 = Utils.getServiceContext(empty);
		Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		sc2 = Utils.getServiceContext(empty2);
		Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1.1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		// 	Publish fake2
		ServiceRegistration reg2 = context.registerService(Factory.class.getName(), fake2, null);
		
		parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		sc1 = Utils.getServiceContext(empty);
		Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		sc2 = Utils.getServiceContext(empty2);
		Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1.1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		reg1.unregister();
		
		parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		sc1 = Utils.getServiceContext(empty);
		Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		sc2 = Utils.getServiceContext(empty2);
		Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1.1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		reg2.unregister();
		
		parentsFactoryReferences = Utils.getServiceReferences(context, Factory.class.getName(), null);
		sc1 = Utils.getServiceContext(empty);
		Level1FactoryReferences = Utils.getServiceReferences(sc1, Factory.class.getName(), null);
		sc2 = Utils.getServiceContext(empty2);
		Level2FactoryReferences = Utils.getServiceReferences(sc2, Factory.class.getName(), null);
		
		assertEquals("Check the number of available factories - 1.1", parentsFactoryReferences.length, Level1FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.2", parentsFactoryReferences.length, Level2FactoryReferences.length);
		assertEquals("Check the number of available factories - 1.3", Level1FactoryReferences.length, Level2FactoryReferences.length);
		
		empty2.dispose();
	}
	
	public void testInvocation() {
		ServiceContext sc1 = Utils.getServiceContext(empty);		
		Factory fact = Utils.getFactoryByName(sc1, "composite.empty");
		Properties p = new Properties();
		p.put("name", "empty2");
		ComponentInstance empty2 = null;
		try {
			empty2 = fact.createComponentInstance(p);
		} catch (Exception e) {
			fail("Cannot instantiate empty2 instance : " + e.getMessage());
		}
		
		ServiceContext sc2 = Utils.getServiceContext(empty2);
		
		Factory fact1 = Utils.getFactoryByName(sc2, "COMPO-SimpleCheckServiceProvider");
		Properties props = new Properties();
		props.put("name", "client");
		ComponentInstance client = null;
		try {
			client = fact1.createComponentInstance(props);
		} catch (Exception e) { e.printStackTrace(); fail("Cannot instantiate the client : " + e.getMessage()); }
		
		Factory fact2 = Utils.getFactoryByName(sc2, "COMPO-FooProviderType-1");
		Properties props2 = new Properties();
		props2.put("name", "provider");
		ComponentInstance provider = null;
		try {
			provider = fact2.createComponentInstance(props2);
		} catch (Exception e) {
			fail("Cannot instantiate the provider : " + e.getMessage());
		}
		
		ServiceReference ref = sc2.getServiceReference(CheckService.class.getName());		
		assertNotNull("Check ref existency", ref);
		CheckService check = (CheckService) sc2.getService(ref);
		
		assertTrue("Check invocation", check.check());
		client.dispose();
		provider.dispose();
		empty2.dispose();
	}
	
	
	
	
	private boolean isExposed(Factory fact, ServiceReference[] refs, ServiceContext sc) {
		for(int i = 0; i < refs.length; i++) {
			Factory f = (Factory) sc.getService(refs[i]);
			if(fact.getName().equals(f.getName())) {
				sc.ungetService(refs[i]);
				return true; 
			}
			sc.ungetService(refs[i]);
		}
		return false;
	}

}
