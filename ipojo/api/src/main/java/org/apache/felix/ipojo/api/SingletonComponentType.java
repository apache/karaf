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

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;

/**
 * Allows defining a primitive component type that create an unique
 * instance when created. The factory is set to private by default.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SingletonComponentType extends PrimitiveComponentType {
    
    /**
     * The POJO object to inject through the
     * instance configuration.
     */
    private Object m_pojo;
    
    /**
     * Creates a SingletonComponentType.
     * This type is set to private by default.
     */
    public SingletonComponentType() {
        setPublic(false);
    }
    
    /**
     * Sets the pojo object used by the instance.
     * The object must be compatible with the 
     * implementation class. 
     * @param obj the object.
     * @return the current singleton component type.
     */
    public SingletonComponentType setObject(Object obj) {
        m_pojo = obj;
        return this;
    }
    
    /**
     * Starts the component type and creates the singleton
     * instance. This method has to be called in place of the
     * {@link PrimitiveComponentType#start()} and the
     * {@link PrimitiveComponentType#createInstance()}  methods.
     * @return the created component instance.
     * @throws ConfigurationException occurs if the type description is
     * incorrect
     * @throws MissingHandlerException occurs if a handler is not available 
     * @throws UnacceptableConfiguration  occurs if the configuration is not 
     * acceptable by the instance  
     * @see org.apache.felix.ipojo.api.ComponentType#start()
     * @see PrimitiveComponentType#createInstance()
     */
    public ComponentInstance create() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        start();
        if (m_pojo != null) {
            Dictionary dict = new Properties();
            dict.put("instance.object", m_pojo);
            return createInstance(dict);
        } else {
            return createInstance();
        }
    }
    
    /**
     * Starts the component type and creates the singleton
     * instance. This method has to be called in place of the
     * {@link PrimitiveComponentType#start()} and the
     * {@link PrimitiveComponentType#createInstance()}  methods.
     * @param conf the instance configuration
     * @return the created component instance
     * @throws ConfigurationException occurs if the type description is
     * incorrect
     * @throws MissingHandlerException occurs if a handler is not available 
     * @throws UnacceptableConfiguration  occurs if the configuration is not 
     * acceptable by the instance  
     * @see org.apache.felix.ipojo.api.ComponentType#start()
     * @see PrimitiveComponentType#createInstance()
     */
    public ComponentInstance create(Dictionary conf) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        start();
        if (m_pojo != null) {
            conf.put("instance.object", m_pojo);
        }
        return createInstance(conf);
    }
    
    /**
     * Starts the component type and creates the singleton
     * instance. This method has to be called in place of the
     * {@link PrimitiveComponentType#start()} and the
     * {@link PrimitiveComponentType#createInstance()}  methods.
     * @param name  the name of the instance to create. This parameter will
     * be used as the <code>instance.name</code> property.
     * @return the created component instance.
     * @throws ConfigurationException occurs if the type description is
     * incorrect
     * @throws MissingHandlerException occurs if a handler is not available 
     * @throws UnacceptableConfiguration  occurs if the configuration is not 
     * acceptable by the instance  
     * @see org.apache.felix.ipojo.api.ComponentType#start()
     * @see PrimitiveComponentType#createInstance()
     */
    public ComponentInstance create(String name) throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
        start();
        if (m_pojo != null) {
            Dictionary dict = new Properties();
            dict.put("instance.name", name);
            dict.put("instance.object", m_pojo);
            return createInstance(dict);
        } else {
            return createInstance(name);
        }
    }
    
   

}
