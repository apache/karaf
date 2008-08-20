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
package org.apache.felix.ipojo.test.composite.exporter;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.composite.service.BazService;
import org.apache.felix.ipojo.test.composite.service.FooService;
import org.apache.felix.ipojo.test.composite.util.Utils;
import org.osgi.framework.ServiceReference;

public class MultipleExport extends OSGiTestCase {
	
	ComponentInstance export3;
	Factory fooProvider;
	ComponentInstance foo1 = null, foo2 = null;

	public void setUp() {
		fooProvider = Utils.getFactoryByName(context, "BazProviderType");
		assertNotNull("Check fooProvider availability", fooProvider);
		
		Properties p1 = new Properties();
		p1.put("instance.name","foo1");
		Properties p2 = new Properties();
		p2.put("instance.name","foo2");
		
		try {
			foo1 = fooProvider.createComponentInstance(p1);
			foo2 = fooProvider.createComponentInstance(p2);
		} catch(Exception e) {
			fail("Fail to create foos : " + e.getMessage());
		}
		
		foo1.stop();
		foo2.stop();
		
		Factory factory = Utils.getFactoryByName(context, "composite.export.3");
		Properties props = new Properties();
		props.put("instance.name","export");
		try {
			export3 = factory.createComponentInstance(props);
		} catch(Exception e) {
			fail("Fail to instantiate exporter " + e.getMessage());
		}
	}
	
	public void tearDown() {
		foo1.dispose();
		foo2.dispose();
		export3.dispose();
		foo1 = null;
		foo2 = null;
		export3 = null;
	}
	
	public void test1() {
		export3.start();
		
		// Check that no foo service are available
		assertEquals("Check no foo service", Utils.getServiceReferences(context, FooService.class.getName(), null).length, 0);
		
		// Test invalidity
		assertTrue("Check invalidity - 0", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 0", isFooServiceProvided());
		assertEquals("Check number of provides - 0", countFooServiceProvided(), 0);
		
		foo1.start();
		assertTrue("Check validity - 1", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 1", isFooServiceProvided());
		assertEquals("Check number of provides - 1", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 1", invoke(1));
		
		foo2.start();
		assertTrue("Check validity - 2", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 2", isFooServiceProvided());
		assertEquals("Check number of provides - 2", countFooServiceProvided(), 2);
		assertTrue("Check invocation - 2", invoke(2));
		
		foo1.stop();
		assertTrue("Check validity - 3", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 3", isFooServiceProvided());
		assertEquals("Check number of provides - 3 ("+countFooServiceProvided()+")", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 3", invoke(1));
		
		foo2.stop();
		assertTrue("Check invalidity - 4", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 4", isFooServiceProvided());
		assertEquals("Check number of provides - 4", countFooServiceProvided(), 0);
		
		foo2.start();
		assertTrue("Check validity - 5", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 5", isFooServiceProvided());
		assertEquals("Check number of provides - 5", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 5", invoke(1));
	}
	
	public void test2() {
		export3.start();
		
		// Test invalidity
		assertTrue("Check invalidity - 0", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 0", isFooServiceProvided());
		assertEquals("Check number of provides - 0", countFooServiceProvided(), 0);
		
		foo1.start();
		assertTrue("Check validity - 1", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 1", isFooServiceProvided());
		assertEquals("Check number of provides - 1", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 1", invoke());
		
		foo2.start();
		assertTrue("Check validity - 2", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 2", isFooServiceProvided());
		assertEquals("Check number of provides - 2", countFooServiceProvided(), 2);
		assertTrue("Check invocation - 2", invoke(2));
		
		foo2.stop();
		assertTrue("Check validity - 3", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 3", isFooServiceProvided());
		assertEquals("Check number of provides - 3", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 3", invoke(1));
		
		foo1.stop();
		assertTrue("Check invalidity - 4", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 4", isFooServiceProvided());
		assertEquals("Check number of provides - 4", countFooServiceProvided(), 0);
		
		foo1.start();
		assertTrue("Check validity - 5", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 5", isFooServiceProvided());
		assertEquals("Check number of provides - 5", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 5", invoke(1));
	}
	
	public void test3() {
		foo1.start();
		foo2.start();
		
		export3.start();
		assertTrue("Check validity - 1", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 1", isFooServiceProvided());
		assertEquals("Check number of provides - 1", countFooServiceProvided(), 2);
		assertTrue("Check invocation - 1", invoke(2));
		
		foo1.stop();
		assertTrue("Check validity - 2", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 2", isFooServiceProvided());
		assertEquals("Check number of provides - 2", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 2", invoke(1));
		
		foo2.stop();
		assertTrue("Check invalidity - 3", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 3", isFooServiceProvided());
		assertEquals("Check number of provides - 3", countFooServiceProvided(), 0);
		
		foo1.start();
		assertTrue("Check validity - 4", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 4", isFooServiceProvided());
		assertEquals("Check number of provides - 4", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 4", invoke(1));
	}
	
	public void test4() {
		foo1.start();
		foo2.start();
		
		export3.start();
		assertTrue("Check validity - 1", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 1", isFooServiceProvided());
		assertEquals("Check number of provides - 1", countFooServiceProvided(), 2);
		assertTrue("Check invocation - 1", invoke(2));
		
		foo2.stop();
		assertTrue("Check validity - 2", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 2", isFooServiceProvided());
		assertEquals("Check number of provides - 2", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 2", invoke(1));
		
		foo1.stop();
		assertTrue("Check invalidity - 3", export3.getState() == ComponentInstance.INVALID);
		assertFalse("Check providing - 3", isFooServiceProvided());
		assertEquals("Check number of provides - 3", countFooServiceProvided(), 0);
		
		foo2.start();
		assertTrue("Check validity - 4", export3.getState() == ComponentInstance.VALID);
		assertTrue("Check providing - 4", isFooServiceProvided());
		assertEquals("Check number of provides - 4", countFooServiceProvided(), 1);
		assertTrue("Check invocation - 4", invoke(1));
	}
	
	
	
	private boolean isFooServiceProvided() {
		ServiceReference ref = Utils.getServiceReferenceByName(context, BazService.class.getName(), export3.getInstanceName());
		return ref != null;
	}
	
	private int countFooServiceProvided() {
		ServiceReference[] refs = Utils.getServiceReferences(context, BazService.class.getName(), "(instance.name="+export3.getInstanceName()+")");
		return refs.length;
	}
	
	private boolean invoke() {
		ServiceReference ref = Utils.getServiceReferenceByName(context, BazService.class.getName(), export3.getInstanceName());
		if(ref == null) { return false; }
		BazService fs = (BazService) context.getService(ref);
		context.ungetService(ref);
		return fs.foo();
	}
	
	private boolean invoke(int nb) {
		ServiceReference[] refs = Utils.getServiceReferences(context, BazService.class.getName(), "(instance.name="+export3.getInstanceName()+")");
		if(refs == null) { return false; }
		if (nb > refs.length) { return false; }
		for(int i = 0; i < nb; i++) {
			BazService fs = (BazService) context.getService(refs[i]);
			context.ungetService(refs[i]);
			if(!fs.foo()) { return false; }
		}
		return true;
	}
	
	
	
	
	

}
