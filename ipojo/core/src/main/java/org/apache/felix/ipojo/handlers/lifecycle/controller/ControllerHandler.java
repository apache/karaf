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
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;

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
        Element[] controller = metadata.getElements("controller");
        String field = controller[0].getAttribute("field");   
        getInstanceManager().register(new FieldMetadata(field, "boolean"), this);
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
    public void stop() { 
        // Nothing to do.
    }
    
    /**
     * GetterCallback.
     * @param pojo : the pojo object on which the field is accessed
     * Return the stored value.
     * @param field : field name.
     * @param value : value given by the previous handler.
     * @return : the handler state.
     */
    public Object onGet(Object pojo, String field, Object value) {
        if (m_state) { 
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }
    
    /**
     * SetterCallback.
     * @param pojo : the pojo object on which the field is accessed
     * Store the new field value & invalidate / validate the handler is required.
     * @param field : field name.
     * @param value : new value.
     */
    public void onSet(Object pojo, String field, Object value) {
        if (value instanceof Boolean) {
            boolean newValue = ((Boolean) value).booleanValue();
            if (newValue != m_state) {
                m_state = newValue;
                if (m_state) {
                    ((InstanceManager) getHandlerManager()).setState(ComponentInstance.VALID);
                } else {
                    ((InstanceManager) getHandlerManager()).setState(ComponentInstance.INVALID);
                }
            }
        } else {
            error("Boolean expected for the lifecycle controller");
            getInstanceManager().stop();
        }
    }
    
    /**
     * Initialize the component factory.
     * The controller field is checked to avoid configure check.
     * @param desc : component description
     * @param metadata : component type metadata
     * @throws ConfigurationException : occurs if the controller field is not in the POJO class or is not a boolean.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription desc, Element metadata) throws ConfigurationException {
        String field = null;
        Element[] controller = metadata.getElements("controller");
        // Use only the first controller
        field = controller[0].getAttribute("field");
        if (field == null) {
            throw new ConfigurationException("Lifecycle controller : the controller element needs to contain a field attribute");
        }
        
        PojoMetadata method = getFactory().getPojoMetadata();
        FieldMetadata fieldMetadata = method.getField(field);
        if (fieldMetadata == null) {
            throw new ConfigurationException("Lifecycle controller : The field " + field + " does not exist in the implementation class");
        }
        
        if (!fieldMetadata.getFieldType().equalsIgnoreCase("boolean")) {
            throw new ConfigurationException("Lifecycle controller : The field " + field + " must be a boolean (" + fieldMetadata.getFieldType() + " found)");
        }
    }

}
