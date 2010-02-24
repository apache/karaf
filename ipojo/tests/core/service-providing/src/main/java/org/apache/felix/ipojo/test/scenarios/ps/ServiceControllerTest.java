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
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;

public class ServiceControllerTest extends OSGiTestCase {
    
    IPOJOHelper helper;

    public void setUp() {
        helper = new IPOJOHelper(this);
    }
    
    
    public void tearDown() {
        helper.dispose();
    }
    
    
    public void testComponentWithAController() {
        ComponentInstance ci = helper.createComponentInstance("PS-Controller-1-default");
        // Controller set to true.
        waitForService(FooService.class.getName(), null, 5000);
        waitForService(CheckService.class.getName(), null, 5000);
        
        CheckService check = (CheckService) getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);
        
        assertFalse(check.check());
        
        // FooService should not be there anymore
        assertNull(getServiceReference(FooService.class.getName()));
        
        assertTrue(check.check());
        
        assertNotNull(getServiceReference(FooService.class.getName()));
        
        ci.dispose();
    }
    
    public void testComponentWithAControllerSetToFalse() {
        ComponentInstance ci = helper.createComponentInstance("PS-Controller-1-false");
        // Controller set to false.
        waitForService(CheckService.class.getName(), null, 5000);
        assertNull(getServiceReference(FooService.class.getName()));

        CheckService check = (CheckService) getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);
        
        assertTrue(check.check());
        assertNotNull(getServiceReference(FooService.class.getName()));

        assertFalse(check.check());
        // FooService should not be there anymore
        assertNull(getServiceReference(FooService.class.getName()));
        
        ci.dispose();
    }
    
    public void testComponentWithTwoControllersSetToTrue() {
        ComponentInstance ci = helper.createComponentInstance("PS-Controller-2-truetrue");

        waitForService(CheckService.class.getName(), null, 5000);
        waitForService(FooService.class.getName(), null, 5000);

        CheckService check = (CheckService) getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);
        
        check.check();
       
        assertNull(getServiceReference(CheckService.class.getName()));
        assertNotNull(getServiceReference(FooService.class.getName()));

        FooService fs = (FooService) getServiceObject(FooService.class.getName(), null);
        fs.foo();
        
        assertNull(getServiceReference(CheckService.class.getName()));
        assertNull(getServiceReference(FooService.class.getName()));
        
        ci.dispose();
    }
    
    public void testComponentWithTwoControllersSetToTrueAndFalse() {
        ComponentInstance ci = helper.createComponentInstance("PS-Controller-2-truefalse");

        waitForService(CheckService.class.getName(), null, 5000);
        
        assertFalse(isServiceAvailable(FooService.class.getName()));

        CheckService check = (CheckService) getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);
        
        check.getProps();
        
        assertFalse(isServiceAvailable(CheckService.class.getName()));
        assertTrue(isServiceAvailable(FooService.class.getName()));
       
        FooService fs = (FooService) getServiceObject(FooService.class.getName(), null);
        fs.fooProps();
        
        assertTrue(isServiceAvailable(CheckService.class.getName()));
        assertTrue(isServiceAvailable(FooService.class.getName()));
        
        ci.dispose();
    }
    
    public void testArchitecture() {
        ComponentInstance ci = helper.createComponentInstance("PS-Controller-1-default");
        // Controller set to true.
        waitForService(FooService.class.getName(), null, 5000);
        waitForService(CheckService.class.getName(), null, 5000);
        
        ProvidedServiceHandlerDescription pshd = null;
        pshd = (ProvidedServiceHandlerDescription) ci.getInstanceDescription()
            .getHandlerDescription("org.apache.felix.ipojo:provides");
        
        ProvidedServiceDescription ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("true", ps.getController());
        
        CheckService check = (CheckService) getServiceObject(CheckService.class.getName(), null);
        assertNotNull(check);
        
        assertFalse(check.check());
        
        ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("false", ps.getController());
        
        assertTrue(check.check());
        
        ps = getPS(FooService.class.getName(), pshd.getProvidedServices());
        assertEquals("true", ps.getController());
        
    }
    
    private  ProvidedServiceDescription getPS(String itf, ProvidedServiceDescription[] svc) {
        for (int i = 0; i < svc.length; i++) {
            if (svc[i].getServiceSpecifications()[0].equals(itf)) {
                return svc[i];
            }
        }
        
        fail("Service : " + itf + " not found");
        return null;
    }
}
