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
package org.apache.felix.ipojo.test.scenarios.service.dependency.statics;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;

public class SimpleDependencies extends OSGiTestCase {
	
	ComponentInstance instance1, instance2, instance3, instance4, instance5;
	ComponentInstance fooProvider;
	
	public void setUp() {
		try {
			Properties prov = new Properties();
			prov.put("name", "FooProvider");
			fooProvider = Utils.getFactoryByName(context, "FooProviderType-1").createComponentInstance(prov);
			fooProvider.stop();
		
			Properties i1 = new Properties();
			i1.put("name", "Simple");
			instance1 = Utils.getFactoryByName(context, "StaticSimpleCheckServiceProvider").createComponentInstance(i1);
		
			Properties i2 = new Properties();
			i2.put("name", "Void");
			instance2 = Utils.getFactoryByName(context, "StaticVoidCheckServiceProvider").createComponentInstance(i2);
		
			Properties i3 = new Properties();
			i3.put("name", "Object");
			instance3 = Utils.getFactoryByName(context, "StaticObjectCheckServiceProvider").createComponentInstance(i3);
		
			Properties i4 = new Properties();
			i4.put("name", "Ref");
			instance4 = Utils.getFactoryByName(context, "StaticRefCheckServiceProvider").createComponentInstance(i4);
			
			Properties i5 = new Properties();
            i5.put("name", "Both");
            instance5 = Utils.getFactoryByName(context, "StaticBothCheckServiceProvider").createComponentInstance(i5);
		} catch(Exception e) { 
		    e.printStackTrace();
		    fail(e.getMessage()); }
		
	}
	
	public void tearDown() {
		instance1.dispose();
		instance2.dispose();
		instance3.dispose();
		instance4.dispose();
		instance5.dispose();
		fooProvider.dispose();
		instance1 = null;
		instance2 = null;
		instance3 = null;
		instance4 = null;
		instance5 = null;
		fooProvider = null;
	}
	
	public void testSimple() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance1.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		assertTrue("check CheckService invocation", cs.check());
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.INVALID);
		
		cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
		assertNull("Check CheckService availability", cs_ref);
		
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
	}
	
	public void testVoid() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance2.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance2.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		Object o = context.getService(cs_ref);
		CheckService cs = (CheckService) o;
		Properties props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -1 ("+((Integer)props.get("voidB")).intValue()+")", ((Integer)props.get("voidB")).intValue(), 1);
		assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        
		fooProvider.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.INVALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance2.getInstanceName());
        assertNull("Check CheckService availability - 2", cs_ref);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);		
	}
	
	public void testObject() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance3.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance3.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 1);
		assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.INVALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance3.getInstanceName());
        assertNull("Check CheckService availability - 2", cs_ref);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);     
	}
	
	public void testRef() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance4.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance4.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
		assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 1);
		assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.INVALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance4.getInstanceName());
        assertNull("Check CheckService availability - 2", cs_ref);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);     
	}
	
	public void testBoth() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 1);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.INVALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance5.getInstanceName());
        assertNull("Check CheckService availability - 2", cs_ref);
        
        fooProvider.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);     
    }

}
