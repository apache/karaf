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
package org.apache.felix.ipojo.test.scenarios.lifecycle.callback;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.lifecycle.callback.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class CallbackTestCase extends OSGiTestCase {
    
    ComponentInstance instance; // Instance under test
    ComponentInstance fooProvider;

    public void setUp() {
        Properties p2 = new Properties();
        p2.put("instance.name","fooProvider");
        fooProvider = Utils.getComponentInstance(getContext(), "LFCB-FooProviderType-1", p2);
        fooProvider.stop();
        
        Properties p1 = new Properties();
        p1.put("instance.name","callback");
        instance = Utils.getComponentInstance(getContext(), "LFCB-CallbackCheckService", p1);
        
    }
    
    public void tearDown() {
        instance.dispose();
        fooProvider.dispose();
        instance= null;
        fooProvider = null;
    }
    
    public void testCallback() {
        // Check instance is invalid
        ServiceReference arch_ref = Utils.getServiceReferenceByName(getContext(), Architecture.class.getName(), instance.getInstanceName());
        assertNotNull("Check architecture availability", arch_ref);
        PrimitiveInstanceDescription id_dep = (PrimitiveInstanceDescription) ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 1", id_dep.getState() == ComponentInstance.INVALID);
        assertEquals("Check pojo count - 1", id_dep.getCreatedObjects().length, 0);
        
        // Start fooprovider
        fooProvider.start();
        
        // Check instance validity
        //id_dep = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 1", id_dep.getState() == ComponentInstance.VALID);
        
        // Check service providing
        ServiceReference cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        CheckService cs = (CheckService) getContext().getService(cs_ref);
        assertTrue("check CheckService invocation", cs.check());
        
        // Check int property
        Integer index = (Integer) (cs.getProps().get("int"));
        assertEquals("Check int property - 1", index.intValue(), 1);
        
        assertEquals("Check pojo count - 2", id_dep.getCreatedObjects().length, 1);
        
        fooProvider.stop();
        
        //id_dep = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance invalidity - 2", id_dep.getState() == ComponentInstance.INVALID);
        
        assertEquals("Check pojo count - 3", id_dep.getCreatedObjects().length, 1);
        
        fooProvider.start();
        
        // Check instance validity
        //id_dep = ((Architecture) getContext().getService(arch_ref)).getInstanceDescription();
        assertTrue("Check instance validity - 2", id_dep.getState() == ComponentInstance.VALID);
        
        // Check service providing
        cs_ref = Utils.getServiceReferenceByName(getContext(), CheckService.class.getName(), instance.getInstanceName());
        assertNotNull("Check CheckService availability", cs_ref);
        cs = (CheckService) getContext().getService(cs_ref);
        assertTrue("check CheckService invocation", cs.check());
        
        // Check int property
        index = (Integer) (cs.getProps().get("int"));
        assertEquals("Check int property - 2 ("+index.intValue()+")", index.intValue(), 3);
        
        assertEquals("Check pojo count - 4 ", id_dep.getCreatedObjects().length, 1);
        
        // Clean up
        getContext().ungetService(arch_ref);
        getContext().ungetService(cs_ref);
        cs = null;
        id_dep = null;
    }
        

}
