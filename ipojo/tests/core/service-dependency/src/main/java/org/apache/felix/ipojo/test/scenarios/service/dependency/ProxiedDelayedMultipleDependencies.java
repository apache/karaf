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
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;

public class ProxiedDelayedMultipleDependencies extends OSGiTestCase {

    ComponentInstance instance1, instance2, instance3, instance4, instance5, instance6, instance7;
    ComponentInstance fooProvider1, fooProvider2;
    
    public void setUp() {
        try {
            Properties i1 = new Properties();
            i1.put("instance.name","Simple");
            instance1 = Utils.getFactoryByName(getContext(), "ProxiedSimpleMultipleCheckServiceProvider").createComponentInstance(i1);
            instance1.stop();
        
            Properties i2 = new Properties();
            i2.put("instance.name","Void");
            instance2 = Utils.getFactoryByName(getContext(), "ProxiedVoidMultipleCheckServiceProvider").createComponentInstance(i2);
            instance2.stop();
        
            Properties i3 = new Properties();
            i3.put("instance.name","Object");
            instance3 = Utils.getFactoryByName(getContext(), "ProxiedObjectMultipleCheckServiceProvider").createComponentInstance(i3);
            instance3.stop();
        
            Properties i4 = new Properties();
            i4.put("instance.name","Ref");
            instance4 = Utils.getFactoryByName(getContext(), "ProxiedRefMultipleCheckServiceProvider").createComponentInstance(i4);
            instance4.stop();
            
             Properties i5 = new Properties();
             i5.put("instance.name","Both");
             instance5 = Utils.getFactoryByName(getContext(), "ProxiedBothMultipleCheckServiceProvider").createComponentInstance(i5);
             instance5.stop();
             
             Properties i6 = new Properties();
             i6.put("instance.name","Map");
             instance6 = Utils.getFactoryByName(getContext(), "ProxiedMapMultipleCheckServiceProvider").createComponentInstance(i6);
             instance6.stop();
             
             Properties i7 = new Properties();
             i7.put("instance.name","Dict");
             instance7 = Utils.getFactoryByName(getContext(), "ProxiedDictMultipleCheckServiceProvider").createComponentInstance(i7);
             instance7.stop();
        
            Properties prov = new Properties();
            prov.put("instance.name","FooProvider1");
            fooProvider1 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov);
        
            Properties prov2 = new Properties();
            prov2.put("instance.name","FooProvider2");
            fooProvider2 = Utils.getFactoryByName(getContext(), "FooProviderType-1").createComponentInstance(prov2);
        } catch(Exception e) { fail(e.getMessage()); }
    }
    
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();
        instance3.dispose();
        instance4.dispose();
        instance5.dispose();
        instance6.dispose();
        instance7.dispose();
        fooProvider1.dispose();
        fooProvider2.dispose();
        instance1 = null;
        instance2 = null;
        instance3 = null;
        instance4 = null;
        instance5 = null;
        instance6 = null;
        instance7 = null;
        fooProvider1 = null;
        fooProvider2 = null;
    }
    
    public void testSimple() {
        instance1.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance1.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance1.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 2", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 2", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 2", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 2", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 2", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 2", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 2", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 2", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 2", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 2", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        
        instance1.stop();
    }
    
    public void testVoid() {
        instance2.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 2);
        assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 1", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 1", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 2);
        assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 1);
        assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation - 3", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation - 3", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);	
        instance2.stop();
    }
    
    public void testObject() {
        instance3.start();
        
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 5", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        instance3.stop();
    }
    
    public void testRef() {
        instance4.start();
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance4.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance4.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance4.stop();
        getContext().ungetService(cs_ref);
    }
    
    public void testBoth() {
        instance5.start();
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer)props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 1", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 0);
        
        assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer)props.get("bothB")).intValue(), 2);
        assertEquals("check both unbind callback invocation - 3", ((Integer)props.get("bothU")).intValue(), 1);
        assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 0);
        assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance5.stop();
        getContext().ungetService(cs_ref);
    }
    
    public void testMap() {
            instance6.start();
            ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance6.getInstanceName());
            assertNotNull("Check architecture availability", arch_ref);
            InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
            
            ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance6.getInstanceName());
            assertNotNull("Check CheckService availability", cs_ref);
            CheckService cs = (CheckService) getContext().getService(cs_ref);
            Properties props = cs.getProps();
            //Check properties
            assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
            assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
            assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
            assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
            assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
            assertEquals("check both bind callback invocation - 1", ((Integer)props.get("bothB")).intValue(), 0);
            assertEquals("check both unbind callback invocation - 1", ((Integer)props.get("bothU")).intValue(), 0);
            assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 2);
            assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 0);
            assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 0);
            assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 0);
            
            assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
            assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
            assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
            
            fooProvider1.stop();
            
            id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
            
            cs = (CheckService) getContext().getService(cs_ref);
            props = cs.getProps();
            //Check properties
            assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
            assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
            assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
            assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
            assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
            assertEquals("check both bind callback invocation - 3", ((Integer)props.get("bothB")).intValue(), 0);
            assertEquals("check both unbind callback invocation - 3", ((Integer)props.get("bothU")).intValue(), 0);
            assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 2);
            assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 1);
            assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 0);
            assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 0);
            assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
            assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
            assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
            
            fooProvider2.stop();
            
            id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
            
            id = null;
            cs = null;
            getContext().ungetService(arch_ref);
            instance6.stop();
            getContext().ungetService(cs_ref);
    }
    
    public void testDict() {
        instance7.start();
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance7.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance7.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 1", ((Boolean)props.get("result")).booleanValue()); // True, a provider is here
        assertEquals("check void bind invocation - 1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 1", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 2);
        assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 0);
        
        assertEquals("Check FS invocation (int) - 1", ((Integer)props.get("int")).intValue(), 2);
        assertEquals("Check FS invocation (long) - 1", ((Long)props.get("long")).longValue(), 2);
        assertEquals("Check FS invocation (double) - 1", ((Double)props.get("double")).doubleValue(), 2.0, 0);
        
        fooProvider1.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 3", id.getState() == ComponentInstance.VALID);
        
        cs = (CheckService) getContext().getService(cs_ref);
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation - 3", ((Boolean)props.get("result")).booleanValue()); // True, two providers are here
        assertEquals("check void bind invocation - 3", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation - 3", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation - 3", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation - 3", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check both bind callback invocation - 3", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation - 3", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation - 1", ((Integer)props.get("mapB")).intValue(), 0);
        assertEquals("check map unbind callback invocation - 1", ((Integer)props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation - 1", ((Integer)props.get("dictB")).intValue(), 2);
        assertEquals("check dict unbind callback invocation - 1", ((Integer)props.get("dictU")).intValue(), 1);
        assertEquals("Check FS invocation (int) - 3", ((Integer)props.get("int")).intValue(), 1);
        assertEquals("Check FS invocation (long) - 3", ((Long)props.get("long")).longValue(), 1);
        assertEquals("Check FS invocation (double) - 3", ((Double)props.get("double")).doubleValue(), 1.0, 0);
        
        fooProvider2.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 4", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        getContext().ungetService(arch_ref);
        instance7.stop();
        getContext().ungetService(cs_ref);
    }

    
}
