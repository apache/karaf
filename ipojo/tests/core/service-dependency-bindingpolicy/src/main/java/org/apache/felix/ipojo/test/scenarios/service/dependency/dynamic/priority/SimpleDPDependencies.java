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
package org.apache.felix.ipojo.test.scenarios.service.dependency.dynamic.priority;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;

public class SimpleDPDependencies extends OSGiTestCase {
	
	ComponentInstance instance1, instance3;
	ComponentInstance fooProvider;
    ComponentInstance fooProvider2;
	
	public void setUp() {
		try {
			Properties prov = new Properties();
			prov.put("instance.name","FooProvider-1");
			prov.put("service.ranking", "1");
			fooProvider = Utils.getFactoryByName(context, "RankedFooProviderType").createComponentInstance(prov);
			fooProvider.stop();
			
			Properties prov2 = new Properties();
            prov2.put("instance.name","FooProvider-2");
            prov2.put("service.ranking", "0");
            fooProvider2 = Utils.getFactoryByName(context, "RankedFooProviderType").createComponentInstance(prov2);
            fooProvider2.stop();
		
			Properties i1 = new Properties();
			i1.put("instance.name","Simple");
			instance1 = Utils.getFactoryByName(context, "DPSimpleCheckServiceProvider").createComponentInstance(i1);
		
			Properties i3 = new Properties();
			i3.put("instance.name","Object");
			instance3 = Utils.getFactoryByName(context, "DPObjectCheckServiceProvider").createComponentInstance(i3);
			
		} catch(Exception e) { 
		    e.printStackTrace();
		    fail(e.getMessage()); }
		
	}
	
	public void tearDown() {
		instance1.dispose();
		instance3.dispose();
		fooProvider.dispose();
		fooProvider2.dispose();
		instance1 = null;
		instance3 = null;
		fooProvider = null;
		fooProvider2 = null;
	}
	
	public void testSimple() {
		ServiceReference arch_ref = Utils.getServiceReferenceByName(context, Architecture.class.getName(), instance1.getInstanceName());
		assertNotNull("Check architecture availability", arch_ref);
		InstanceDescription id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
		
		fooProvider.start();
		fooProvider2.start();
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
		assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
		
		ServiceReference cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
		assertNotNull("Check CheckService availability", cs_ref);
		CheckService cs = (CheckService) context.getService(cs_ref);
		// Check grade
		Integer grade = (Integer) cs.getProps().get("int");
		assertEquals("Check first grade", 1, grade.intValue());
		
		fooProvider.stop(); // Turn off the best provider.
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) context.getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check second grade", 0, grade.intValue());
        
		fooProvider.start(); // Turn on the best provider.
		
		id = ((Architecture) context.getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) context.getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check third grade", 1, grade.intValue());
        
		
        // Increase the second provider grade.
        ServiceReference fs_ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check foo service (2) reference", fs_ref);
        FooService fs = (FooService) context.getService(fs_ref);
        
        fs.foo(); // Increase the grade (now = 2)
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) context.getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check fourth grade", 2, grade.intValue());
        
        // Increase the other provider grade.
        fs_ref = Utils.getServiceReferenceByName(context, FooService.class.getName(), fooProvider.getInstanceName());
        assertNotNull("Check foo service (1) reference", fs_ref);
        fs = (FooService) context.getService(fs_ref);
        fs.foo(); //(grade = 3)
        
        cs_ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) context.getService(cs_ref);
        // Check grade
        grade = (Integer) cs.getProps().get("int");
        assertEquals("Check fifth grade", 3, grade.intValue());
        		
		id = null;
		cs = null;
		context.ungetService(arch_ref);
		context.ungetService(cs_ref);
		context.ungetService(fs_ref);
		fooProvider.stop();
		fooProvider2.stop();
	}
}
