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
package org.apache.felix.ipojo.test.composite.importer;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.FooService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.ServiceReference;

public class DelayedMultipleImport extends OSGiTestCase {
	
	ComponentInstance import2;
	Factory fooProvider;
	ComponentInstance foo1, foo2;

	public void setUp() {
		
		Properties p = new Properties();
		p.put("instance.name","importer");
		Factory compFact = Utils.getFactoryByName(getContext(), "composite.requires.2");
		try {
			import2 = compFact.createComponentInstance(p);
		} catch(Exception e) {
			fail("Cannot instantiate the component : " + e.getMessage());
		}
		
		import2.stop();
		
		fooProvider = Utils.getFactoryByName(getContext(), "COMPO-FooProviderType-1");
		assertNotNull("Check fooProvider availability", fooProvider);
		
		Properties p1 = new Properties();
		p1.put("instance.name","foo1");
		Properties p2 = new Properties();
		p2.put("instance.name","foo2");
		try {
			foo1 = fooProvider.createComponentInstance(p1);
			foo2 = fooProvider.createComponentInstance(p2);
		} catch(Exception e) {
			fail("Cannot instantiate foo providers : " + e.getMessage());
		}
	}
	
	public void tearDown() {
		foo1.dispose();
		foo2.dispose();
		import2.dispose();
		foo1 = null;
		foo2 = null;
		import2 = null;
	}
	
	public void testSimple() {
		import2.start(); 
		//Two providers
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(import2);
		ServiceReference[] refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 2);
		FooService fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		fs = (FooService) sc.getService(refs[1]);
		assertTrue("Test foo invocation (2)", fs.foo());
		sc.ungetService(refs[1]);
		
		foo1.stop();
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import2);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// Stop the second provider
		foo2.stop();		
		assertTrue("Test component invalidity - 2", import2.getState() == ComponentInstance.INVALID);
		
		foo2.start();
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import2);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 3", refs);
		assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
	}
	
	public void testSimple2() {
		import2.start(); 
		//Two providers
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(import2);
		ServiceReference[] refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 2);
		FooService fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		fs = (FooService) sc.getService(refs[1]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[1]);
		
		foo2.stop();
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import2);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// Stop the second provider
		foo1.stop();		
		assertTrue("Test component invalidity - 2", import2.getState() == ComponentInstance.INVALID);
		
		foo1.start();
		assertTrue("Test component validity", import2.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import2);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 3", refs);
		assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
	}
	
	

}
