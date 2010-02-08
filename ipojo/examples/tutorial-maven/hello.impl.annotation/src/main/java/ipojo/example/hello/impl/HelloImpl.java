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
package ipojo.example.hello.impl;

import ipojo.example.hello.Hello;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.ServiceProperty;

/**
 * Component implementing the Hello service.
 * This class used annotations to describe the component type. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component
@Provides
public class HelloImpl implements Hello {
    
    
    @ServiceProperty
    public String boo = "boo";
    
    @ServiceProperty
    public String bla = "bla";

    
    /**
     * Returns an 'Hello' message.
     * @param name : name
     * @return Hello message
     * @see ipojo.example.hello.Hello#sayHello(java.lang.String)
     */
    public String sayHello(String name) { return "hello " + name + " @";  }
}
