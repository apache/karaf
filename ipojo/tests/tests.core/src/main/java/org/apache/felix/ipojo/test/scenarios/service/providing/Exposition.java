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
package org.apache.felix.ipojo.test.scenarios.service.providing;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.BarService;
import org.apache.felix.ipojo.test.scenarios.service.FooService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class Exposition extends OSGiTestCase {
	
	private ComponentInstance fooProviderSimple;
	private ComponentInstance fooProviderItf;
	private ComponentInstance fooBarProvider;
	private ComponentInstance fooBarProvider2;
	private ComponentInstance fooBarProvider3;
	
	public void setUp(){ 
		Properties p1 = new Properties();
		p1.put("name", "fooProviderSimple");
		fooProviderSimple = Utils.getComponentInstance(context, "FooProviderType-1", p1);
		
		Properties p2 = new Properties();
		p2.put("name", "fooProviderItf");
		fooProviderItf = Utils.getComponentInstance(context, "FooProviderType-itf", p2);
		
		Properties p3 = new Properties();
		p3.put("name", "fooProviderItfs");
		fooBarProvider = Utils.getComponentInstance(context, "FooBarProviderType-1", p3);
		
		Properties p4 = new Properties();
		p4.put("name", "fooProviderItfs2");
		fooBarProvider2 = Utils.getComponentInstance(context, "FooBarProviderType-2", p4);
		
		Properties p5 = new Properties();
		p5.put("name", "fooProviderItfs3");
		fooBarProvider3 = Utils.getComponentInstance(context, "FooBarProviderType-3", p5);
		
		assertNotNull("Check the instance creation of fooProviderSimple", fooProviderSimple);
		assertNotNull("Check the instance creation of fooProviderItf", fooProviderItf);
		assertNotNull("Check the instance creation of fooProviderItfs", fooBarProvider);
		assertNotNull("Check the instance creation of fooProviderItfs2", fooBarProvider2);
		assertNotNull("Check the instance creation of fooProviderItfs3", fooBarProvider3);
		
	}
	
	public void tearDown() {
		fooProviderSimple.dispose();
		fooProviderItf.dispose();
		fooBarProvider.dispose();
		fooBarProvider2.dispose();
		fooBarProvider3.dispose();
		fooProviderSimple = null;
		fooProviderItf = null;
		fooBarProvider = null;
		fooBarProvider2 = null;
		fooBarProvider3 = null;		
	}
	
	public void testSimpleExposition() {
		ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProviderSimple.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooProviderSimple.getInstanceName(), ref);
		FooService fs = (FooService) context.getService(ref);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		context.ungetService(ref);
		fooProviderSimple.stop();
		ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProviderSimple.getInstanceName());
		assertNull("Check the absence of the FS from "+fooProviderSimple.getInstanceName(), ref);
		
	}
	
	public void testItfExposition() {
		ServiceReference ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProviderItf.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooProviderItf.getInstanceName(), ref);
		FooService fs = (FooService) context.getService(ref);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		context.ungetService(ref);
		fooProviderItf.stop();
		
		ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProviderItf.getInstanceName());
		assertNull("Check the absence of the FS from "+fooProviderItf.getInstanceName(), ref);
	}
	
	public void testItfsExposition() {
		ServiceReference refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		ServiceReference refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider.getInstanceName(), refBar);
		
		assertSame("Check service reference equality", refFoo, refBar);
		
		FooService fs = (FooService) context.getService(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		context.ungetService(refFoo);
		
		BarService bs = (BarService) context.getService(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		context.ungetService(refBar);
		
		fooBarProvider.stop();
		
		refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider.getInstanceName());
		refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	public void testItfsExposition2() {
		ServiceReference refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider2.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider2.getInstanceName(), refFoo);
		ServiceReference refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider2.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider2.getInstanceName(), refBar);
		
		assertSame("Check service reference equality", refFoo, refBar);
		
		FooService fs = (FooService) context.getService(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		context.ungetService(refFoo);
		
		BarService bs = (BarService) context.getService(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		context.ungetService(refBar);
		
		fooBarProvider2.stop();
		
		refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider2.getInstanceName());
		refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider2.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	public void testItfsExposition3() {
		ServiceReference refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider3.getInstanceName());
		assertNotNull("Check the availability of the FS from "+fooBarProvider3.getInstanceName(), refFoo);
		ServiceReference refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider3.getInstanceName());
		assertNotNull("Check the availability of the BS from "+fooBarProvider3.getInstanceName(), refBar);
		
		assertNotSame("Check service reference inequality", refFoo, refBar);
		
		FooService fs = (FooService) context.getService(refFoo);
		assertTrue("Check fs invocation", fs.foo());
		fs = null;
		context.ungetService(refFoo);
		
		BarService bs = (BarService) context.getService(refBar);
		assertTrue("Check bs invocation", bs.bar());
		bs = null;
		context.ungetService(refBar);
		
		// Check properties
		String baz1 = (String) refFoo.getProperty("baz");
		String baz2 = (String) refBar.getProperty("baz");
		
		assertEquals("Check Baz Property 1", baz1, "foo");
		assertEquals("Check Baz Property 2", baz2, "bar");
		
		fooBarProvider3.stop();
		
		refFoo = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooBarProvider3.getInstanceName());
		refBar = Utils.getServiceReferenceByName(context, BarService.class.getName(), fooBarProvider3.getInstanceName());
		assertNull("Check the absence of the FS from "+fooBarProvider.getInstanceName(), refFoo);
		assertNull("Check the absence of the BS from "+fooBarProvider.getInstanceName(), refBar);
	}
	
	

}
