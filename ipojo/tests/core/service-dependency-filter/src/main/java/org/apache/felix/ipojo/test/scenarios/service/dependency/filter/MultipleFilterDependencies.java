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
package org.apache.felix.ipojo.test.scenarios.service.dependency.filter;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;

public class MultipleFilterDependencies extends OSGiTestCase {
    
    ComponentInstance instance1, instance2, instance3;
    ComponentInstance fooProvider1, fooProvider2;
    
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name","FooProvider1");
            fooProvider1 = Utils.getFactoryByName(getContext(), "SimpleFilterCheckServiceProvider").createComponentInstance(prov);
            fooProvider1.stop();
            
            prov = new Properties();
            prov.put("instance.name","FooProvider2");
            fooProvider2 = Utils.getFactoryByName(getContext(), "SimpleFilterCheckServiceProvider").createComponentInstance(prov);
            fooProvider2.stop();
        
            Properties i1 = new Properties();
            i1.put("instance.name","Subscriber1");
            instance1 = Utils.getFactoryByName(getContext(), "MultipleFilterCheckServiceSubscriber").createComponentInstance(i1);
            
            Properties i2 = new Properties();
            i2.put("instance.name","Subscriber2");
            Properties ii2 = new Properties();
            ii2.put("id2", "(toto=A)");
            i2.put("requires.filters", ii2);
            instance2 = Utils.getFactoryByName(getContext(), "MultipleFilterCheckServiceSubscriber2").createComponentInstance(i2);
            
            Properties i3 = new Properties();
            i3.put("instance.name","Subscriber3");
            Properties ii3 = new Properties();
            ii3.put("id1", "(toto=A)");
            i3.put("requires.filters", ii3);
            instance3 = Utils.getFactoryByName(getContext(), "MultipleFilterCheckServiceSubscriber").createComponentInstance(i3);
        
        } catch(Exception e) { 
            e.printStackTrace();
            fail(e.getMessage()); }
        
    }
    
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();
        instance3.dispose();
        fooProvider1.dispose();
        fooProvider2.dispose();
        instance1 = null;
        instance2 = null;
        instance3 = null;
        fooProvider1 = null;
        fooProvider2 = null;
    }
    
    public void testMultipleNotMatch() {
        instance1.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance1.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);

        
        fooProvider1.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 3", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        assertTrue("Check service Binding - 3", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        
        fooProvider2.start();
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        CheckService cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 6", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service Binding - 6", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 6", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 7", id.getState() == ComponentInstance.INVALID);
        
        
        fooProvider2.start();
        cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 8", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service Binding - 8", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 8", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 9", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        cs2 = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);		
    }
    
    public void testMultipleMatch() {
        
        fooProvider1.start();
        fooProvider2.start();
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        CheckService cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        
        instance1.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance1.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 1", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 1", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 2", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 2", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 3", id.getState() == ComponentInstance.INVALID);
        
        fooProvider2.start();
        cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        id = null;
        cs = null;
        cs2 = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);      
    }
    
    public void testMultipleNotMatchInstance() {
        instance3.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        
        fooProvider1.start();
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        
        cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 1", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 1", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.start();
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        CheckService cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        
        // change the value of the property toto
        cs2.check();
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 6", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service Binding - 6", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 6", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 7", id.getState() == ComponentInstance.INVALID);
        
        fooProvider2.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 8", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 8", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 8", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 9", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        cs2 = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);
    }
    
    public void testMultipleMatchInstance() {
        
        fooProvider1.start();
        fooProvider2.start();
        instance3.start();
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 1", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 1", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 2", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 2", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 3", id.getState() == ComponentInstance.INVALID);
       
        
        fooProvider2.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        id = null;
        cs = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);        
    }
    
    public void testMultipleNotMatchInstanceWithoutFilter() {
        instance2.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        
        fooProvider1.start();
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        
        cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 1", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 1", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.start();
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        CheckService cs2 = (CheckService) getContext().getService(cs_ref2);
        // change the value of the property toto
        cs2.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        
        // change the value of the property toto
        cs2.check();
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 6", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service Binding - 6", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 6", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 7", id.getState() == ComponentInstance.INVALID);
        
        fooProvider2.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 8", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 8", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 8", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 9", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        cs2 = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);
    }
    
    public void testMultipleMatchInstanceWithoutFilter() {
        fooProvider1.start();
        fooProvider2.start();
        instance2.start();
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        
        ServiceReference cs_ref2 = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), fooProvider2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref2);
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        CheckService cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 1", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 1", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        // change the value of the property toto
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 2", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 2", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 3", id.getState() == ComponentInstance.INVALID);
       
        
        fooProvider2.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.VALID);
        cs_instance_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_instance_ref);
        cs_instance = (CheckService) getContext().getService(cs_instance_ref);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 4", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(1)));
        assertTrue("Check Array size - 4", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(1)));
        
        cs.check();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.VALID);
        assertTrue("Check service invocation", cs_instance.check());
        assertTrue("Check service Binding - 5", ((Integer)cs_instance.getProps().get("Bind")).equals(new Integer(2)));
        assertTrue("Check Array size - 5", ((Integer)cs_instance.getProps().get("Size")).equals(new Integer(2)));
        
        id = null;
        cs = null;
        cs_instance = null;
        getContext().ungetService(cs_instance_ref);
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(cs_ref2);        
    }

}
