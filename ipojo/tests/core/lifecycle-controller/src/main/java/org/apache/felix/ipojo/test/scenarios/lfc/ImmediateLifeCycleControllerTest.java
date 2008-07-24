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
package org.apache.felix.ipojo.test.scenarios.lfc;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.lfc.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.util.Utils;
import org.osgi.framework.ServiceReference;

public class ImmediateLifeCycleControllerTest extends OSGiTestCase {
    
    private ComponentInstance under;
    private Factory factory;
    
    public void setUp() {
        factory = Utils.getFactoryByName(context, "LFC-Test-Immediate");
    }
    
    public void testOne() {
        Properties props = new Properties();
        props.put("conf", "foo");
        props.put("name", "under");
        under = Utils.getComponentInstance(context, "LFC-Test-Immediate", props);
        
        // The conf is correct, the PS must be provided
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "under");
        assertNotNull("Check service availability -1", ref);
        CheckService cs = (CheckService) context.getService(ref);
        assertTrue("Check state 1", cs.check());
        context.ungetService(ref);
        cs = null;
        
        // Reconfigure the instance with a bad configuration
        props.put("conf", "bar"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch(Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }
        
        // The instance should now be invalid 
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "under");
        assertNull("Check service availability -2", ref);
        
        // Reconfigure the instance with a valid configuration
        props.put("conf", "foo"); // Bar is a bad conf
        try {
            factory.reconfigure(props);
        } catch(Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable (2) : " + props);
        }
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "under");
        assertNotNull("Check service availability -3", ref);
        cs = (CheckService) context.getService(ref);
        assertTrue("Check state 2", cs.check());
        context.ungetService(ref);
        cs = null;
        
        under.dispose();
    }
    
    public void testTwo() {        
        Properties props = new Properties();
        props.put("conf", "bar");
        props.put("name", "under");
        under = Utils.getComponentInstance(context, "LFC-Test-Immediate", props);    
        
        assertEquals("check under state", under.getState(), ComponentInstance.INVALID);
        
        // The conf is incorrect, the PS must not be provided
        ServiceReference ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "under");
        assertNull("Check service availability -1", ref);
        
        // Reconfigure the instance with a correct configuration
        props.put("conf", "foo");
        try {
            factory.reconfigure(props);
        } catch(Exception e) {
            fail("The reconfiguration is not unacceptable and seems unacceptable : " + props);
        }
        
        ref = Utils.getServiceReferenceByName(context, CheckService.class.getName(), "under");
        assertNotNull("Check service availability -2", ref);
        CheckService cs = (CheckService) context.getService(ref);
        assertTrue("Check state ", cs.check());
        context.ungetService(ref);
        cs = null;
        
        under.dispose();
    }
    
    

}
