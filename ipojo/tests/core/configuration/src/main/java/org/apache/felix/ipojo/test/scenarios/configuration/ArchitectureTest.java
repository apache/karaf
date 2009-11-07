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
package org.apache.felix.ipojo.test.scenarios.configuration;

import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.test.scenarios.util.Utils;

public class ArchitectureTest extends OSGiTestCase {

    /**
     * Instance where the ManagedServicePID is provided by the component type. 
     */
    ComponentInstance instance1;
    /**
     * Instance where the ManagedServicePID is provided by the instance. 
     */
    ComponentInstance instance2;
    
    /**
     * Instance without configuration. 
     */
    ComponentInstance instance3;
    
    public void setUp() {
        String type = "CONFIG-FooProviderType-4";
        Properties p = new Properties();
        p.put("instance.name","instance");
        p.put("foo", "foo");
        p.put("bar", "2");
        p.put("baz", "baz");
        instance1 = Utils.getComponentInstance(getContext(), type, p);
        assertEquals("instance1 created", ComponentInstance.VALID,instance1.getState());
        
        type = "CONFIG-FooProviderType-3";
        Properties p1 = new Properties();
        p1.put("instance.name","instance-2");
        p1.put("foo", "foo");
        p1.put("bar", "2");
        p1.put("baz", "baz");
        p1.put("managed.service.pid", "instance");
        instance2 = Utils.getComponentInstance(getContext(), type, p1);
        
    }
    
    public void tearDown() {
        instance1.dispose();
        instance2.dispose();
        instance1 = null;
        instance2 = null;
    }
    
    public void testArchitectureForInstance1() {
        Architecture arch = (Architecture) Utils.getServiceObject(context, Architecture.class.getName(), "(architecture.instance=instance)");
        assertNotNull(arch);
        
        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"FooProvider-3\""));
        
        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);
        
        assertEquals(2, hd.getProperties().length);
        assertEquals("FooProvider-3", hd.getManagedServicePid());

    }
    
    public void testArchitectureForInstance2() {
        Architecture arch = (Architecture) Utils.getServiceObject(context, Architecture.class.getName(), "(architecture.instance=instance-2)");
        assertNotNull(arch);
        
        // Test on String representation.
        String desc = arch.getInstanceDescription().getDescription().toString();
        assertTrue(desc.contains("managed.service.pid=\"instance\""));
        
        // Test on handler description
        ConfigurationHandlerDescription hd = (ConfigurationHandlerDescription) arch.getInstanceDescription().getHandlerDescription("org.apache.felix.ipojo:properties");
        assertNotNull(hd);
        
        assertEquals(2, hd.getProperties().length);
        assertEquals("instance", hd.getManagedServicePid());

    }

}
