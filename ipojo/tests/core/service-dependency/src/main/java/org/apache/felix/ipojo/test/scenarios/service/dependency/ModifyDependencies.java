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
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;

public class ModifyDependencies extends OSGiTestCase {
    
    ComponentInstance instance2, instance3, instance4, instance5, instance7, instance8;
    ComponentInstance fooProvider;
    
    public void setUp() {
        try {
            Properties prov = new Properties();
            prov.put("instance.name","FooProvider");
            fooProvider = Utils.getFactoryByName(getContext(), "FooProviderType-Updatable").createComponentInstance(prov);
            fooProvider.stop();
        
            Properties i2 = new Properties();
            i2.put("instance.name","Void");
            instance2 = Utils.getFactoryByName(getContext(), "VoidModifyCheckServiceProvider").createComponentInstance(i2);
        
            Properties i3 = new Properties();
            i3.put("instance.name","Object");
            instance3 = Utils.getFactoryByName(getContext(), "ObjectModifyCheckServiceProvider").createComponentInstance(i3);
        
            Properties i4 = new Properties();
            i4.put("instance.name","Ref");
            instance4 = Utils.getFactoryByName(getContext(), "RefModifyCheckServiceProvider").createComponentInstance(i4);
            
            Properties i5 = new Properties();
            i5.put("instance.name","Both");
            instance5 = Utils.getFactoryByName(getContext(), "BothModifyCheckServiceProvider").createComponentInstance(i5);
                        
            Properties i7 = new Properties();
            i7.put("instance.name","Map");
            instance7 = Utils.getFactoryByName(getContext(), "MapModifyCheckServiceProvider").createComponentInstance(i7);
            
            Properties i8 = new Properties();
            i8.put("instance.name","Dictionary");
            instance8 = Utils.getFactoryByName(getContext(), "DictModifyCheckServiceProvider").createComponentInstance(i8);
        } catch(Exception e) { 
            e.printStackTrace();
            fail(e.getMessage()); }
        
    }
    
    public void tearDown() {
        instance2.dispose();
        instance3.dispose();
        instance4.dispose();
        instance5.dispose();
        instance7.dispose();
        instance8.dispose();
        fooProvider.dispose();
        instance2 = null;
        instance3 = null;
        instance4 = null;
        instance5 = null;
        instance7 = null;
        instance8 = null;
        fooProvider = null;
    }
    
   public void testVoid() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance2.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance2.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        Object o = getContext().getService(cs_ref);
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
        assertEquals("check modify -1", ((Integer)props.get("modified")).intValue(), 1); // Already called inside the method
        
        
        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) getContext().getService(ref);
        
        fs.foo(); // Update
        
        props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1.1", ((Boolean)props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1.1 ("+((Integer)props.get("voidB")).intValue()+")", ((Integer)props.get("voidB")).intValue(), 1);
        assertEquals("check void unbind callback invocation -1.1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1.1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1.1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1.1", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1.1", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1.1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1.1", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3); // 1 (first foo) + 1 (our foo) + 1 (check foo)        
        fooProvider.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);

    }
    
    public void testObject() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance3.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance3.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("check modify -1 (" + ((Integer)props.get("modified")).intValue() + ")", ((Integer)props.get("modified")).intValue(), 1);

        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) getContext().getService(ref);
        
        fs.foo(); // Update
        
        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3);
        
        
        fooProvider.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref);	
    }
    
    public void testRef() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance4.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance4.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("check modify -1 (" + ((Integer)props.get("modified")).intValue() + ")", ((Integer)props.get("modified")).intValue(), 1);

        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) getContext().getService(ref);
        
        fs.foo(); // Update
        
        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3);
        
        fooProvider.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref); 
    }
    
    public void testBoth() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance5.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance5.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
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
        assertEquals("check modify -1 (" + ((Integer)props.get("modified")).intValue() + ")", ((Integer)props.get("modified")).intValue(), 1);

        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) getContext().getService(ref);
        
        fs.foo(); // Update
        
        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3);
        
        fooProvider.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref); 
    }

    
    public void testMap() {
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance7.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
        
        fooProvider.start();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
        
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance7.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        Properties props = cs.getProps();
        //Check properties
        assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
        assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
        assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
        assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
        assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
        assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
        assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
        assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
        assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
        assertEquals("check map bind callback invocation -1", ((Integer)props.get("mapB")).intValue(), 1);
        assertEquals("check map unbind callback invocation -1", ((Integer)props.get("mapU")).intValue(), 0);
        assertEquals("check dict bind callback invocation -1", ((Integer)props.get("dictB")).intValue(), 0);
        assertEquals("check dict unbind callback invocation -1", ((Integer)props.get("dictU")).intValue(), 0);
        assertEquals("check modify -1 (" + ((Integer)props.get("modified")).intValue() + ")", ((Integer)props.get("modified")).intValue(), 1);

        ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
        FooService fs = (FooService) getContext().getService(ref);
        
        fs.foo(); // Update
        
        props = cs.getProps();
        //Check properties
        assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3);
        
        fooProvider.stop();
        
        id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
        
        id = null;
        cs = null;
        fs = null;
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        getContext().ungetService(ref); 
    }
    
       public void testDict() {
            ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance8.getInstanceName());
            assertNotNull("Check architecture availability", arch_ref);
            InstanceDescription id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance invalidity - 1", id.getState() == ComponentInstance.INVALID);
            
            fooProvider.start();
            
            id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance validity", id.getState() == ComponentInstance.VALID);
            
            ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance8.getInstanceName());
            assertNotNull("Check CheckService availability", cs_ref);
            CheckService cs = (CheckService) getContext().getService(cs_ref);
            Properties props = cs.getProps();
            //Check properties
            assertTrue("check CheckService invocation -1", ((Boolean)props.get("result")).booleanValue());
            assertEquals("check void bind invocation -1", ((Integer)props.get("voidB")).intValue(), 0);
            assertEquals("check void unbind callback invocation -1", ((Integer)props.get("voidU")).intValue(), 0);
            assertEquals("check object bind callback invocation -1", ((Integer)props.get("objectB")).intValue(), 0);
            assertEquals("check object unbind callback invocation -1", ((Integer)props.get("objectU")).intValue(), 0);
            assertEquals("check ref bind callback invocation -1", ((Integer)props.get("refB")).intValue(), 0);
            assertEquals("check ref unbind callback invocation -1", ((Integer)props.get("refU")).intValue(), 0);
            assertEquals("check both bind callback invocation -1", ((Integer)props.get("bothB")).intValue(), 0);
            assertEquals("check both unbind callback invocation -1", ((Integer)props.get("bothU")).intValue(), 0);
            assertEquals("check map bind callback invocation -1", ((Integer)props.get("mapB")).intValue(), 0);
            assertEquals("check map unbind callback invocation -1", ((Integer)props.get("mapU")).intValue(), 0);
            assertEquals("check dict bind callback invocation -1", ((Integer)props.get("dictB")).intValue(), 1);
            assertEquals("check dict unbind callback invocation -1", ((Integer)props.get("dictU")).intValue(), 0);
            assertEquals("check modify -1 (" + ((Integer)props.get("modified")).intValue() + ")", ((Integer)props.get("modified")).intValue(), 1);

            ServiceReference ref = Utils.getServiceReferenceByName(getContext(), FooService.class.getName(), fooProvider.getInstanceName());
            FooService fs = (FooService) getContext().getService(ref);
            
            fs.foo(); // Update
            
            props = cs.getProps();
            //Check properties
            assertEquals("check modify -1.1", ((Integer)props.get("modified")).intValue(), 3);
            
            fooProvider.stop();
            
            id = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
            assertTrue("Check instance invalidity - 2", id.getState() == ComponentInstance.INVALID);
            
            id = null;
            cs = null;
            fs = null;
            getContext().ungetService(arch_ref);
            getContext().ungetService(cs_ref);
            getContext().ungetService(ref); 
        }

}
