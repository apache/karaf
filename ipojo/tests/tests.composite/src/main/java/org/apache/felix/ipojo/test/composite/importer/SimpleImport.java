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

public class SimpleImport extends OSGiTestCase {
	
	ComponentInstance import1;
	Factory fooProvider;

	public void setUp() {
		fooProvider = Utils.getFactoryByName(context, "COMPO-FooProviderType-1");
		assertNotNull("Check fooProvider availability", fooProvider);
		
		Properties p = new Properties();
		p.put("name", "importer");
		Factory compFact = Utils.getFactoryByName(context, "composite.requires.1");
		try {
			import1 = compFact.createComponentInstance(p);
		} catch(Exception e) {
		    e.printStackTrace();
			fail("Cannot instantiate the component : " + e.getMessage());
		}
	}
	
	public void tearDown() {
		import1.dispose();
		import1 = null;
	}
	
	public void testSimple() {
		// No provider -> Invalid
		assertTrue("Test component invalidity - 0 ("+import1.getState()+")", import1.getState() == ComponentInstance.INVALID);
		
		ComponentInstance foo = null;
		Properties p = new Properties();
		p.put("name", "foo");
		try {
			foo = fooProvider.createComponentInstance(p);
		} catch(Exception e) {
			fail("Fail to instantiate the foo component " + e.getMessage());
		}
		
		ComponentInstance foo2 = null;
		Properties p2 = new Properties();
		p2.put("name", "foo2");
		try {
			foo2 = fooProvider.createComponentInstance(p2);
		} catch(Exception e) {
			fail("Fail to instantiate the foo2 component " + e.getMessage());
		}
		
		// The foo service is available => import1 must be valid
		assertTrue("Test component validity - 1", import1.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(import1);
		ServiceReference[] refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
		FooService fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// Stop the second provider
		foo2.dispose();
		assertTrue("Test component validity - 2", import1.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import1);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 2", refs);
		assertEquals("Test foo availability inside the composite - 2.1 ("+refs.length+")", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// stop the foo provider
		foo.stop();
		
		// No provider -> Invalid
		assertTrue("Test component invalidity - 2", import1.getState() == ComponentInstance.INVALID);
		
		foo.start();
		assertTrue("Test component validity - 3", import1.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import1);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 3", refs);
		assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		foo.dispose(); 
		// No provider -> Invalid
		assertTrue("Test component invalidity - 3", import1.getState() == ComponentInstance.INVALID);
	}
	
	public void testSimple2() {
		// No provider -> Invalid
		assertTrue("Test component invalidity", import1.getState() == ComponentInstance.INVALID);
		
		ComponentInstance foo1 = null;
		Properties p = new Properties();
		p.put("name", "foo");
		try {
			foo1 = fooProvider.createComponentInstance(p);
		} catch(Exception e) {
			fail("Fail to instantiate the foo component " + e.getMessage());
		}
		
		ComponentInstance foo2 = null;
		Properties p2 = new Properties();
		p2.put("name", "foo2");
		try {
			foo2 = fooProvider.createComponentInstance(p2);
		} catch(Exception e) {
			fail("Fail to instantiate the foo2 component " + e.getMessage());
		}
		
		// The foo service is available => import1 must be valid
		assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
		ServiceContext sc = Utils.getServiceContext(import1);
		ServiceReference[] refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 1", refs);
		assertEquals("Test foo availability inside the composite - 1.2", refs.length, 1);
		FooService fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// Stop the first provider
		foo1.stop();
		assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import1);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 2", refs);
		assertEquals("Test foo availability inside the composite - 2.1 ("+refs.length+")", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		// stop the second foo provider
		foo2.dispose();
		
		// No provider -> Invalid
		assertTrue("Test component invalidity - 2", import1.getState() == ComponentInstance.INVALID);
		
		foo1.start();
		assertTrue("Test component validity", import1.getState() == ComponentInstance.VALID);
		sc = Utils.getServiceContext(import1);
		refs = Utils.getServiceReferences(sc, FooService.class.getName(), null);
		assertNotNull("Test foo availability inside the composite - 3", refs);
		assertEquals("Test foo availability inside the composite - 3.1", refs.length, 1);
		fs = (FooService) sc.getService(refs[0]);
		assertTrue("Test foo invocation", fs.foo());
		sc.ungetService(refs[0]);
		
		foo1.dispose(); 
		// No provider -> Invalid
		assertTrue("Test component invalidity - 3", import1.getState() == ComponentInstance.INVALID);
	}	

}
