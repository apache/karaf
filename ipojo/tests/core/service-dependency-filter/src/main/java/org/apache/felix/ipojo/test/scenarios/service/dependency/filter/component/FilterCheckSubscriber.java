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
package org.apache.felix.ipojo.test.scenarios.service.dependency.filter.component;

import java.util.Properties;

import org.apache.felix.ipojo.Nullable;

import org.apache.felix.ipojo.test.scenarios.service.dependency.service.CheckService;
import org.apache.felix.ipojo.test.scenarios.service.dependency.service.FooService;

public class FilterCheckSubscriber implements CheckService {
    
    private FooService m_foo;
    
    private int bound;
    
    public FilterCheckSubscriber(){
    }
    
    public boolean check() {
        m_foo.foo();
        return true;
    }

    public Properties getProps() {
        Properties props = new Properties();
        props.put("Bind", new Integer(bound));
        props.put("Nullable", new Boolean(m_foo instanceof Nullable));
        return props;
    }
    
    private void Bind() {
        bound++;
    }
    private void Unbind() {
        bound--;
    }

}
