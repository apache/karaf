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

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.util.Logger;

/**
 * Lifecycle Controller handler.
 * This handler allow a POJO  to vote for the instance state. By setting a boolean field to true or false, the handler state changed.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ControllerHandler extends Handler {
    
    /**
     * Instance Manager.
     */
    private InstanceManager m_manager;
    
    /**
     * Actual handler (i.e. field value) state
     */
    private boolean m_state;

    /**
     * Configure method.
     * Look for the first 'controller' element.
     * @param im : instance manager
     * @param metadata : metadata
     * @param configuration : configuration
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(InstanceManager im, Element metadata, Dictionary configuration) {
        m_manager = im;
        String field = null;
        Element[] lc = metadata.getElements("controller");
        if (lc.length > 0) {
            // Use only the first controller
            if (lc[0].containsAttribute("field")) {
                field = lc[0].getAttribute("field");
            } else {
                m_manager.getFactory().getLogger().log(Logger.ERROR, "A lifecycle controler needs to contain a field attribute");
                return;
            }
        } else {
            return;
        }
        
        ManipulationMetadata mm = new ManipulationMetadata(metadata);
        FieldMetadata fm = mm.getField(field);
        if (fm == null) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "The field " + field + " does not exist in the class");
            return;
        }
        
        if (!fm.getFieldType().equalsIgnoreCase("boolean")) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "The field " + field + " must be a boolean (" + fm.getFieldType() + " found)");
            return;
        }
        
        im.register(this, new FieldMetadata[] {fm}, null);
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
     * Return the field value.
     * @return the field value (i.e. the handler state)
     * @see org.apache.felix.ipojo.Handler#isValid()
     */
    public boolean isValid() {
        return m_state;
    }
    
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
                m_manager.checkInstanceState();
            }
        } else {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "Boolean expected");
        }
    }

}
