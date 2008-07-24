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
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.ps.service.ChildInterface;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.apache.felix.ipojo.test.scenarios.ps.service.ParentInterface1;
import org.apache.felix.ipojo.test.scenarios.ps.service.ParentInterface2;
import org.apache.felix.ipojo.test.scenarios.ps.service.ParentParentInterface;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class InheritedTest extends OSGiTestCase {
    
    private Factory pi1, pi11, pi12, pi2, pi21, pi3;
    
    public void setUp() {
        pi1 = Utils.getFactoryByName(context, "PS-PI1");
        pi11 = Utils.getFactoryByName(context, "PS-PI1-1");
        pi12 = Utils.getFactoryByName(context, "PS-PI1-2");
        
        pi2 = Utils.getFactoryByName(context, "PS-PI2");
        pi21 = Utils.getFactoryByName(context, "PS-PI2-1");
        
        pi3 = Utils.getFactoryByName(context, "PS-PI3");
    }
    
    private boolean contains(String[] arr, String txt) {
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].equals(txt)) {
                return true;
            }
        }
        return false;
    }
    
    public void testPI1Factory() {
        String[] specs = pi1.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 4);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }
    
    public void testPI11Factory() {
        String[] specs = pi11.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 1);
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }
    
    public void testPI12Factory() {
        String[] specs = pi12.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 2);
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }
    
    public void testPI2Factory() {
        String[] specs = pi2.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count ("+specs.length+")", specs.length, 4);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }
    
    public void testPI21Factory() {
        String[] specs = pi21.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 1);
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
    }
    
    public void testPI3Factory() {
        String[] specs = pi3.getComponentDescription().getprovidedServiceSpecification();
        assertEquals("Check provides count", specs.length, 5);
        assertTrue("Check Child", contains(specs, ChildInterface.class.getName()));
        assertTrue("Check Parent1", contains(specs, ParentInterface1.class.getName()));
        assertTrue("Check Parent2", contains(specs, ParentInterface2.class.getName()));
        assertTrue("Check ParentParent", contains(specs, ParentParentInterface.class.getName()));
        assertTrue("Check FS", contains(specs, FooService.class.getName()));
    }
    
    public void testIP1() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi1.getName(), "ci");
        
        ServiceReference ref1 = Utils.getServiceReferenceByName(context, ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);
        
        ServiceReference ref2 = Utils.getServiceReferenceByName(context, ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);
        
        ServiceReference ref3 = Utils.getServiceReferenceByName(context, ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ci.dispose();
    }
    
    public void testIP11() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi11.getName(), "ci");
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ci.dispose();
    }
    
    public void testIP12() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi12.getName(), "ci");
        
        ServiceReference ref3 = Utils.getServiceReferenceByName(context, ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ci.dispose();
    }
    
    public void testIP2() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi2.getName(), "ci");
        
        ServiceReference ref1 = Utils.getServiceReferenceByName(context, ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);
        
        ServiceReference ref2 = Utils.getServiceReferenceByName(context, ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);
        
        ServiceReference ref3 = Utils.getServiceReferenceByName(context, ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ci.dispose();
    }
    
    public void testIP21() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi21.getName(), "ci");
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ci.dispose();
    }
    
    public void testIP3() {
        ComponentInstance ci = Utils.getComponentInstanceByName(context, pi3.getName(), "ci");
        
        ServiceReference ref1 = Utils.getServiceReferenceByName(context, ChildInterface.class.getName(), "ci");
        assertNotNull("Check Child", ref1);
        
        ServiceReference ref2 = Utils.getServiceReferenceByName(context, ParentInterface1.class.getName(), "ci");
        assertNotNull("Check Parent1", ref2);
        
        ServiceReference ref3 = Utils.getServiceReferenceByName(context, ParentInterface2.class.getName(), "ci");
        assertNotNull("Check Parent2", ref3);
        
        ServiceReference ref4 = Utils.getServiceReferenceByName(context, ParentParentInterface.class.getName(), "ci");
        assertNotNull("Check PP", ref4);
        
        ServiceReference ref5 = Utils.getServiceReferenceByName(context, FooService.class.getName(), "ci");
        assertNotNull("Check FS", ref5);
        
        ci.dispose();
    }
    
    
    

}
