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
package org.apache.felix.ipojo.test.scenarios.component;

import org.apache.felix.ipojo.test.scenarios.configuration.service.FooService;
import org.osgi.framework.ServiceReference;

public abstract class CheckProviderParentClass {
    
    int simpleU = 0;
    int objectU = 0;
    int refU = 0;
    int bothU = 0;
    
    
    public void bothUnbind(FooService o, ServiceReference sr) {
        if(sr != null && o != null && o instanceof FooService) { bothU++; }
    }
    
    public void refUnbind(ServiceReference sr) {
        if(sr != null) { refU++; }
    }
    
    public void objectUnbind(FooService o) {
        if(o != null && o instanceof FooService) { objectU++; }
        else {
            System.err.println("Unbind null : " + o);
        }
    }
    
    public void voidUnbind() {
        simpleU++;
    }

}
