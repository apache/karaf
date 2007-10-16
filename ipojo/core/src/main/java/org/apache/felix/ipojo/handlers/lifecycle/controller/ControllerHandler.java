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
package org.apache.felix.ipojo.handlers.lifecycle.controller;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.util.Logger;

/**
 * Lifecycle Controller handler.
 * This handler allow a POJO  to vote for the instance state. By setting a boolean field to true or false, the handler state changed.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ControllerHandler extends PrimitiveHandler {
    
    /**
     * Actual handler (i.e. field value) state
     */
    private boolean m_state;

    /**
     * Configure method.
     * Look for the first 'controller' element.
     * @param metadata : metadata
     * @param configuration : configuration
     * @throws ConfigurationException : the field attribute is missing or does not exist in the class.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        Element[] lc = metadata.getElements("controller");
        String field = lc[0].getAttribute("field");   
        getInstanceManager().register(this, new FieldMetadata[] {new FieldMetadata(field, "boolean")}, null);
    }

    /**
     * Start method.
     * Nothing to do.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() { 
        m_state = true;
    }

    /**
     * Stop method.
     * Nothing to do. 
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() { }
    
    /**
     * GetterCallback.
     * Return the stored value.
     * @param field : field name.
     * @param o : value given by the previous handler.
     * @return : the handler state.
     */
    public Object getterCallback(String field, Object o) {
        return new Boolean(m_state);
    }
    
    /**
     * SetterCallback.
     * Store the new field value & invalidate / validate the handler is required.
     * @param field : field name.
     * @param o : new value.
     */
    public void setterCallback(String field, Object o) {
        if (o instanceof Boolean) {
            boolean nv = ((Boolean) o).booleanValue();
            if (nv != m_state) {
                m_state = nv;
                if (m_state) {
                    ((InstanceManager) getInstance()).setState(ComponentInstance.VALID);
                } else {
                    ((InstanceManager) getInstance()).setState(ComponentInstance.INVALID);
                }
            }
        } else {
            log(Logger.ERROR, "Boolean expected for the lifecycle controller");
            getInstanceManager().stop();
        }
    }
    
    /**
     * Initialize the component factory.
     * The controller field is checked to avoid configure check.
     * @param cd : component description
     * @param metadata : component type metadata
     * @throws ConfigurationException : occurs if the controller field is not in the POJO class or is not a boolean.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) throws ConfigurationException {
        String field = null;
        Element[] lc = metadata.getElements("controller");
        // Use only the first controller
        field = lc[0].getAttribute("field");
        if (field == null) {
            throw new ConfigurationException("Lifecycle controller : the controller element needs to have a field attribute");
        }
        
        ManipulationMetadata mm = new ManipulationMetadata(metadata);
        FieldMetadata fm = mm.getField(field);
        if (fm == null) {
            throw new ConfigurationException("Lifecycle controller : The field " + field + " does not exist in the class");
        }
        
        if (!fm.getFieldType().equalsIgnoreCase("boolean")) {
            throw new ConfigurationException("Lifecycle controller : The field " + field + " must be a boolean (" + fm.getFieldType() + " found)");
        }
    }

}
