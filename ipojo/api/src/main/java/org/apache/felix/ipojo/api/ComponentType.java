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
package org.apache.felix.ipojo.api;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;


/**
 * The component type class allows specifying a new component type
 * and its attached factory. It also allows creating instances form 
 * the specified component type. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class ComponentType {
        
    /**
     * The list of instances created from the
     * current component type.
     */
    private List m_instances = new ArrayList();
   
    
    /**
     * Gets the factory attached to the current
     * component type.
     * @return the factory
     */
    public abstract Factory getFactory();
    
    /**
     * Starts the factory attached to this 
     * component type. Once started a factory 
     * and its attached component type
     * cannot be modified.
     */
    public abstract void start();
    
    /**
     * Stops the factory attached to this 
     * component type.
     */
    public abstract void stop();

    
    /**
     * Creates a component instance from the current type 
     * with an empty configuration.
     * @return the component instance object.
     * @throws UnacceptableConfiguration the configuration is not acceptable 
     * @throws MissingHandlerException the factory in invalid
     * @throws ConfigurationException the instance configuration failed
     */
    public ComponentInstance createInstance() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = ensureAndGetFactory().createComponentInstance(null); 
        m_instances.add(ci);
        return ci;
    }
    
    /**
     * Creates a component instance from the current type 
     * with the given name.
     * @param name the instance name
     * @return the component instance object.
     * @throws UnacceptableConfiguration the configuration is not acceptable 
     * @throws MissingHandlerException the factory in invalid
     * @throws ConfigurationException the instance configuration failed
     */
    public ComponentInstance createInstance(String name) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        Dictionary dict = null;
        if (name != null) {
            dict = new Properties();
            dict.put("instance.name", name);
        } 
        ComponentInstance ci = ensureAndGetFactory().createComponentInstance(dict); 
        m_instances.add(ci);
        return ci;
    }
    
    /**
     * Creates a component instance from the current type 
     * with the given configuration.
     * @param conf the configuration
     * @return the component instance object.
     * @throws UnacceptableConfiguration the configuration is not acceptable 
     * @throws MissingHandlerException the factory in invalid
     * @throws ConfigurationException the instance configuration failed
     */
    public ComponentInstance createInstance(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        ComponentInstance ci = ensureAndGetFactory().createComponentInstance(conf);
        m_instances.add(ci);
        return ci;
    }
    
    /**
     * Disposes the given name. The instance must be created from this
     * component type.
     * @param ci the component instance to delete
     * @return <code>true</code> if the instance was
     * successfully disposed.
     */
    public boolean disposeInstance(ComponentInstance ci) {
        if (m_instances.remove(ci)) {
            ci.dispose();
            return true;
        } else {
            System.err.println("The instance was not created from this component type");
            return false;
        }
    }
    
    /**
     * Gets the component instance created from this component type.
     * with the given name.
     * @param name the instance name.
     * @return the component instance with the given name and created
     * from the current component type factory. <code>null</code> if
     * the instance cannot be found.
     */
    public ComponentInstance getInstanceByName(String name) {
        for (int i = 0; i < m_instances.size(); i++) {
            ComponentInstance ci = (ComponentInstance) m_instances.get(i);
            if (ci.getInstanceName().equals(name)) {
                return ci;
            }
        }
        return null;
    }
    
    /**
     * Disposes the instance created with this component type which 
     * has the given name.
     * @param name the name of the instance to delete.
     * @return <code>true</code> is the instance is successfully disposed.
     */
    public boolean disposeInstance(String name) {
        ComponentInstance ci = getInstanceByName(name);
        if (ci == null) {
            System.err.println("The instance was not found in this component type");
            return false;
        } else {
            return disposeInstance(ci);
        }
    }
    
    /**
     * Returns the attached factory.
     * Before returning the factory, the consistency of the
     * factory is checked.
     * @return the attached factory to the current component type
     */
    private Factory ensureAndGetFactory() {
        ensureFactory();
        return getFactory();
    }
    
    /**
     * Checks if the factory is already created.
     */
    private void ensureFactory() {
        if (getFactory() == null) {
            throw new IllegalStateException("The factory associated with the component type is not created");
        } else {
            if (getFactory().getState() == Factory.INVALID) {
                throw new IllegalStateException("The factory associated with the component type is invalid (not started or missing handlers)");
            }
        }
    }


}
