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
package org.apache.felix.ipojo.test.scenarios.service.dependency;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class MethodDelayedMultipleDependencies extends OSGiTestCase {

	ComponentInstance instance3, instance4, instance5;
	ComponentInstance fooProvider1, fooProvider2;
	
	public void setUp() {
		try {
		
			Properties i3 = new Properties();
			i3.put("name", "Object");
			instance3 = Utils.getFactoryByName(context, "MObjectMultipleCheckServiceProvider").createComponentInstance(i3);
			instance3.stop();
		
			Properties i4 = new Properties();
			i4.put("name", "Ref");
			instance4 = Utils.getFactoryByName(context, "MRefMultipleCheckServiceProvider").createComponentInstance(i4);
			instance4.stop();
			
			Properties i5 = new Properties();
            i5.put("name", "Both");
            instance5 = Utils.getFactoryByName(context, "MBothMultipleCheckServiceProvider").createComponentInstance(i5);
            instance5.stop();
		
			Properties prov = new Properties();
			prov.put("name", "FooProvider1");
			fooProvider1 = Utils.getFactoryByName(context, "FooProviderType-1").createComponentInstance(prov);
		
			Properties prov2 = new Properties();
			prov2.put("name", "FooProvider2");
			fooProvider2 = Utils.getFactoryByName(context, "FooProviderType-1").createComponentInstance(prov2);
		} catch(Exception e) { fail(e.getMessage()); }
	}
	
	public void tearDown() {
		instance3.dispose();
		instance4.dispose();
		instance5.dispose();
		fooProvider1.dispose();
		fooProvider2.dispose();
		instance3 = null;
		instance4 = null;
		instance5 = null;
		fooProvider1 = null;
		fooProvider2 = null;
	}
	
	public void testObject() {
		instance3.start();
		
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance3.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance3.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
		assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 2);
		assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
		assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
		assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0);
		
		fooProvider1.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
		assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 2);
		assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 1);
		assertEquals("check ref bind callback invocation - 3", ((Integer)props.get("refB")).intValue(), 0);
		assertEquals("check ref unbind callback invocation - 3", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
		assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
		assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0);
		
		fooProvider2.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.INVALID);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);
		instance3.stop();
	}
	
	public void testRef() {
		instance4.start();
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance4.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance4.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		Properties props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
		assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 2);
		assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
		assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
		assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
		assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0);
		
		fooProvider1.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
		
		cs = (CheckService) context.getService(cs_ref);
		props = cs.getProps();
		//Check properties
		assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
		assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
		assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
		assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
		assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
		assertEquals("check ref bind callback invocation - 3", ((Integer)props.get("refB")).intValue(), 2);
		assertEquals("check ref unbind callback invocation - 3", ((Integer)props.get("refU")).intValue(), 1);
		assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
		assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
		assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0);
		
		fooProvider2.stop();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		instance4.stop();
		context.ungetService(cs_ref);
	}
	
	public void testBoth() {
        instance5.start();
        ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) context.getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer)props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 1", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0);
        
        fooProvider1.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) context.getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer)props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 3", ((Integer)props.get("bothU")).intValue(), 1);
        assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0);
        
        fooProvider2.stop();
        
        id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        context.ungetService(arch_ref);
        instance5.stop();
        context.ungetService(cs_ref);
    }

	
}
