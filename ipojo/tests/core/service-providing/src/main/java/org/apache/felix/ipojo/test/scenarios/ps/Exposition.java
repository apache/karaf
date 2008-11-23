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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.BarService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class Exposition extends OSGiTestCase {
	
	private ComponentInstance fooProviderSimple;
	private ComponentInstance fooProviderItf;
	private ComponentInstance fooBarProvider;
	private ComponentInstance fooBarProvider2;
	private ComponentInstance fooBarProvider3;
	
	private IPOJOHelper helper;
	
	public void setUp(){ 
	    helper = new IPOJOHelper(this);
		fooProviderSimple = helper.createComponentInstance("PS-FooProviderType-1", "fooProviderSimple");
		
		fooProviderItf = helper.createComponentInstance("PS-FooProviderType-itf", "fooProviderItf");
		
		fooBarProvider = helper.createComponentInstance("PS-FooBarProviderType-1", "fooProviderItfs");
		
		fooBarProvider2 = helper.createComponentInstance("PS-FooBarProviderType-2", "fooProviderItfs2");
		
		fooBarProvider3 = helper.createComponentInstance("PS-FooBarProviderType-3", "fooProviderItfs3");
		
		assertNotNull("Check the instance creation of fooProviderSimple", fooProviderSimple);
		assertNotNull("Check the instance creation of fooProviderItf", fooProviderItf);
		assertNotNull("Check the instance creation of fooProviderItfs", fooBarProvider);
		assertNotNull("Check the instance creation of fooProviderItfs2", fooBarProvider2);
		assertNotNull("Check the instance creation of fooProviderItfs3", fooBarProvider3);
		
	}
	
	public void tearDown() {
	    helper.dispose();	
	}
	
	public void testSimpleExposition() {
		ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), fooProviderSimple.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooProviderSimple.getInstanceName(), ref);
		FooService fs = (FooService) getServiceObject(ref);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		fooProviderSimple.stop();
		ref = helper.getServiceReferenceByName(FooService.class.getName(), fooProviderSimple.getInstanceName());
		assertNull("Check the absence of the FS from "+fooProviderSimple.getInstanceName(), ref);
		
	}
	
	public void testItfExposition() {
		ServiceReference ref = helper.getServiceReferenceByName(FooService.class.getName(), fooProviderItf.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooProviderItf.getInstanceName(), ref);
		FooService fs = (FooService) getServiceObject(ref);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		fooProviderItf.stop();
		
		ref = helper.getServiceReferenceByName(FooService.class.getName(), fooProviderItf.getInstanceName());
		assertNull("Check the absence of the FS from "+fooProviderItf.getInstanceName(), ref);
	}
	
	public void testItfsExposition() {
		ServiceReference refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		ServiceReference refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider.getInstanceName(), refBar);
		
		assertSame("Check service reference equality", refFoo, refBar);
		
		FooService fs = (FooService) getServiceObject(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		
		BarService bs = (BarService) getServiceObject(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		
		fooBarProvider.stop();
		
		refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider.getInstanceName());
		refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	public void testItfsExposition2() {
		ServiceReference refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider2.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider2.getInstanceName(), refFoo);
		ServiceReference refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider2.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider2.getInstanceName(), refBar);
		
		assertSame("Check service reference equality", refFoo, refBar);
		
		FooService fs = (FooService) getServiceObject(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		
		BarService bs = (BarService) getServiceObject(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		
		fooBarProvider2.stop();
		
		refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider2.getInstanceName());
		refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider2.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	public void testItfsExposition3() {
		ServiceReference refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider3.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider3.getInstanceName(), refFoo);
		ServiceReference refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider3.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider3.getInstanceName(), refBar);
		
		assertNotSame("Check service reference inequality", refFoo, refBar);
		
		FooService fs = (FooService) getServiceObject(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		
		BarService bs = (BarService) getServiceObject(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		
		// Check properties
		String baz1 = (String) refFoo.getProperty("baz");
		String baz2 = (String) refBar.getProperty("baz");
		
		assertEquals("Check Baz Property 1", baz1, "foo");
		assertEquals("Check Baz Property 2", baz2, "bar");
		
		fooBarProvider3.stop();
		
		refFoo = helper.getServiceReferenceByName(FooService.class.getName(), fooBarProvider3.getInstanceName());
		refBar = helper.getServiceReferenceByName(BarService.class.getName(), fooBarProvider3.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	

}
