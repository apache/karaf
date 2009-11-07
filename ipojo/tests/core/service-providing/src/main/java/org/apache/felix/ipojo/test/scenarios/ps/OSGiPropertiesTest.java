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

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class OSGiPropertiesTest extends OSGiTestCase {
    
    public void testOSGiProperties() {
        IPOJOHelper helper = new IPOJOHelper(this);
        
        String factName = "PS-FooProviderType-1";
        String compName = "FooProvider-1";
        ServiceReference ref = null;
        
        // Check that no Foo Service are available
        ref = getServiceReference(FooService.class.getName());
        
        assertNull("FS already available", ref);
    
        // Get the factory to create a component instance
        Factory fact = helper.getFactory(factName);
        assertNotNull("Cannot find the factory FooProvider-1", fact);
        
        Dictionary conf = new Properties();
        conf.put(Constants.SERVICE_DESCRIPTION, "description");
        conf.put(Constants.SERVICE_RANKING, "10");
        conf.put(Constants.SERVICE_VENDOR, "ASF");
        conf.put(Constants.SERVICE_PID, "my.pid");
        
        helper.createComponentInstance(factName, compName, conf);
        
        // Get a FooService provider
        ref = getServiceReference(FooService.class.getName(), "(" + "instance.name" + "=" + compName + ")");

        assertNotNull("FS not available", ref);
        
        // Check properties
        assertEquals("description", ref.getProperty(Constants.SERVICE_DESCRIPTION));
        assertEquals(new Integer(10), ref.getProperty(Constants.SERVICE_RANKING));
        assertEquals("ASF", ref.getProperty(Constants.SERVICE_VENDOR));
        assertEquals("my.pid", ref.getProperty(Constants.SERVICE_PID));
        
        
        // Test foo invocation
        FooService fs = (FooService) getServiceObject(ref);
        assertTrue("FooService invocation failed", fs.foo());
        
        helper.dispose();

        
        // Check that there is no more FooService
        ref = getServiceReference(FooService.class.getName(), null);
        
        
        assertNull("FS available, but component instance stopped", ref);
        
    }

}
