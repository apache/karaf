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

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.junit4osgi.helpers.IPOJOHelper;
import org.apache.felix.ipojo.test.scenarios.component.inherited.ProcessParentImplementation;
import org.apache.felix.ipojo.test.scenarios.ps.service.FooService;
import org.osgi.framework.ServiceReference;

public class ClassTest extends OSGiTestCase {
    
    private Factory pi4, pi5, pi6, pi7;
    
    private IPOJOHelper helper;
    
    public void setUp() {
        helper = new IPOJOHelper(this);
        pi4 = helper.getFactory("PS-PI4");
        pi5 = helper.getFactory("PS-PI5");
        pi6 = helper.getFactory("PS-PI6");
        pi7 = helper.getFactory("PS-PI7");
    }
    
    public void tearDown() {
        helper.dispose();
    }
    
    
    public void testIP4() {
       helper.createComponentInstance( pi4.getName(), "ci");
        
       ServiceReference ref1 = helper.getServiceReferenceByName("org.apache.felix.ipojo.test.scenarios.component.inherited.ProcessParentImplementation", "ci");
       assertNotNull("Check itself", ref1);
        
       ProcessParentImplementation itself = (ProcessParentImplementation) getServiceObject(ref1);
       
        itself.processChild();
    }
    
    public void testIP5() {
        helper.createComponentInstance( pi5.getName(), "ci");
        
        ServiceReference ref1 = helper.getServiceReferenceByName("org.apache.felix.ipojo.test.scenarios.component.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent", ref1);
         
        ProcessParentImplementation itself = (ProcessParentImplementation) getServiceObject(ref1);
        
         itself.processChild();
        
    }
    
    public void testIP6() {
        helper.createComponentInstance( pi6.getName(), "ci");
        
        ServiceReference ref1 = helper.getServiceReferenceByName("org.apache.felix.ipojo.test.scenarios.component.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent-parent", ref1);
         
        ProcessParentImplementation itself = (ProcessParentImplementation) getServiceObject(ref1);
        
         itself.processChild();
    }
    
    public void testIP7() {
       helper.createComponentInstance( pi7.getName(), "ci");
        
        ServiceReference ref1 = helper.getServiceReferenceByName("org.apache.felix.ipojo.test.scenarios.component.inherited.ProcessParentImplementation", "ci");
        assertNotNull("Check parent-parent", ref1);
         
        ProcessParentImplementation itself = (ProcessParentImplementation) getServiceObject(ref1);
        
         itself.processChild();
         
         ServiceReference ref5 = helper.getServiceReferenceByName( FooService.class.getName(), "ci");
         assertNotNull("Check FS", ref5);
    }
}
