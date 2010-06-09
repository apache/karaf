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
package org.apache.felix.ipojo.arch.gogo;

import java.io.PrintStream;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.ServiceProperty;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.service.command.Descriptor;
/**
 * iPOJO Arch command giving information about the current
 * system architecture. This is a Gogo command.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
@Component(public_factory = false, immediate = true)
@Instantiate
@Provides(specifications = Arch.class)
public class Arch {
    
    /**
     * Defines the command scope (ipojo).
     */
    @ServiceProperty(name = "osgi.command.scope", value = "ipojo")
    String m_scope;
    
    /**
     * Defines the functions (commands). 
     */
    @ServiceProperty(name = "osgi.command.function", value = "{}")
    String[] m_function = new String[] {
        "instances",
        "instance",
        "factory",
        "factories",
        "handlers"
    };
    
    /**
     * Instance architecture services.
     */
    @Requires(optional = true)
    private Architecture[] m_archs;
    
    /**
     * Factory services.
     */
    @Requires(optional = true)
    private Factory[] m_factories;
    
    /**
     * Handler Factory services.
     */
    @Requires(optional = true)
    private HandlerFactory[] m_handlers;
    
    /**
     * Displays iPOJO instances.
     */
    @Descriptor("Display iPOJO instances")
    public void instances() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription instance = m_archs[i].getInstanceDescription();
            if (instance.getState() == ComponentInstance.VALID) {
                buffer.append("Instance " + instance.getName() + " -> valid \n");
            }
            if (instance.getState() == ComponentInstance.INVALID) {
                buffer.append("Instance " + instance.getName() + " -> invalid \n");
            }
            if (instance.getState() == ComponentInstance.STOPPED) {
                buffer.append("Instance " + instance.getName() + " -> stopped \n");
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No instances \n");
        }
        
        System.out.println(buffer.toString());   
    }
    
    /**
     * Displays the architecture of a specific instance.
     * @param instance the instance name
     */
    @Descriptor("Display the architecture of a specific instance")
    public void instance(@Descriptor("target instance name") String instance) {
        for (int i = 0; i < m_archs.length; i++) {
            InstanceDescription id = m_archs[i].getInstanceDescription();
            if (id.getName().equalsIgnoreCase(instance)) {
                System.out.println(id.getDescription());
                return;
            }
        }
        System.err.println("Instance " + instance + " not found");
    }
    
    /**
     * Displays the information about a specific factory.
     * Note that factory name are not unique, so all matching
     * factories are displayed.
     * @param factory the factory name
     */
    @Descriptor("Display the information about a specific factory")
    public void factory(@Descriptor("target factory") String factory) {
        boolean found = false;
        PrintStream out = System.out;
        
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getName().equalsIgnoreCase(factory)) {
                // Skip a line if already found (factory name not necessary unique)
                if (found) {
                    out.println();
                }
                out.println(m_factories[i].getDescription());
                found = true;
            }
        }
        
        if (! found) {
            System.err.println("Factory " + factory + " not found");
        }
    }
    
    /**
     * Displays the list of public iPOJO factories.
     */
    @Descriptor("Display iPOJO factories")
    public void factories() {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < m_factories.length; i++) {
            if (m_factories[i].getMissingHandlers().size() == 0) {
                buffer.append("Factory " + m_factories[i].getName() + " (VALID) \n");
            } else {
                buffer.append("Factory " + m_factories[i].getName() + " (INVALID : " + m_factories[i].getMissingHandlers() + ") \n");
            }
        }
        
        if (buffer.length() == 0) {
            buffer.append("No factories \n");
        }
        
        System.out.println(buffer.toString());
    }
    
    /**
     * Displays the list of available handlers.
     */
    @Descriptor("Display iPOJO handlers")
    public void handlers() {
        PrintStream out = System.out;
        for (int i = 0; i < m_handlers.length; i++) {
            String name = m_handlers[i].getHandlerName();
            if ("composite".equals(m_handlers[i].getType())) {
                name = name + " [composite]";
            }
            if (m_handlers[i].getMissingHandlers().size() == 0) {
                out.println("Handler " + name + " (VALID)");
            } else {
                out.println("Handler " + name + " (INVALID : " + m_handlers[i].getMissingHandlers() + ")");
            }
        }
    }

}
