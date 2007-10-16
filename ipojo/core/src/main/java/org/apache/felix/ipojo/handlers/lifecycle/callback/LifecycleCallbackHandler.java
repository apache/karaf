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
package org.apache.felix.ipojo.handlers.lifecycle.callback;

import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;

/**
 * Lifecycle callback handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LifecycleCallbackHandler extends PrimitiveHandler {

    /**
     * The list of the callback of the component.
     */
    private LifecycleCallback[] m_callbacks = new LifecycleCallback[0];

    /**
     * State of the instance manager (unresolved at the beginning).
     */
    private int m_state = InstanceManager.INVALID;
    /**
     * Does a POJO object be created at starting.
     */
    private boolean m_immediate = false;

    /**
     * Add the given callback to the callback list.
     * 
     * @param hk : the element to add
     */
    private void addCallback(LifecycleCallback hk) {
        for (int i = 0; (m_callbacks != null) && (i < m_callbacks.length); i++) {
            if (m_callbacks[i] == hk) {
                return;
            }
        }

        if (m_callbacks.length > 0) {
            LifecycleCallback[] newHk = new LifecycleCallback[m_callbacks.length + 1];
            System.arraycopy(m_callbacks, 0, newHk, 0, m_callbacks.length);
            newHk[m_callbacks.length] = hk;
            m_callbacks = newHk;
        } else {
            m_callbacks = new LifecycleCallback[] { hk };
        }

    }

    /**
     * Configure the handler.
     * @param metadata : the component type metadata
     * @param configuration : the instance configuration
     * @throws ConfigurationException : one callback metadata is not correct (either the transition or the method are not correct).
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        m_callbacks = new LifecycleCallback[0];

        String imm = metadata.getAttribute("immediate");
        m_immediate = imm != null && imm.equalsIgnoreCase("true");
        
        ManipulationMetadata mm = new ManipulationMetadata(metadata);

        Element[] hooksMetadata = metadata.getElements("callback");
        for (int i = 0; i < hooksMetadata.length; i++) {
            String method = hooksMetadata[i].getAttribute("method");
            if (method == null) {
                throw new ConfigurationException("Lifecycle callback : A callback needs to contains a method attribute");
            }
            
            MethodMetadata met = mm.getMethod(method, new String[0]);
            
            int transition = -1;
            String trans = hooksMetadata[i].getAttribute("transition");
            if (trans != null) {
                if (trans.equalsIgnoreCase("validate")) {
                    transition = LifecycleCallback.VALIDATE;
                } else if (trans.equalsIgnoreCase("invalidate")) {
                    transition = LifecycleCallback.INVALIDATE; 
                } else {
                    throw new ConfigurationException("Lifecycle callback : Unknown or malformed transition : " + transition);
                }
            } else {
                throw new ConfigurationException("Lifecycle callback : Unknown or malformed transition");
            }
            
            LifecycleCallback hk = null;
            if (met != null) { 
                hk = new LifecycleCallback(this, transition, met);
            } else {
                hk = new LifecycleCallback(this, transition, method);
            }
            addCallback(hk);
        }
    }

    /**
     * Start the handler. 
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
    } // Do nothing during the start

    /**
     * Stop the handler.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        m_state = InstanceManager.INVALID;
    }

    /**
     * When the state change call the associated callback.
     * 
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        int transition = -1;
        if (m_state == ComponentInstance.INVALID && state == ComponentInstance.VALID) {
            transition = LifecycleCallback.VALIDATE;
        }
        if (m_state == ComponentInstance.VALID && state == ComponentInstance.INVALID) {
            transition = LifecycleCallback.INVALIDATE;
        }
        
        // Manage immediate component
        if (m_immediate && transition == LifecycleCallback.VALIDATE && getInstanceManager().getPojoObjects().length == 0) {
            getInstanceManager().getPojoObject();
        }

        for (int i = 0; i < m_callbacks.length; i++) {
            if (m_callbacks[i].getTransition() == transition) {
                try {
                    m_callbacks[i].call();
                } catch (NoSuchMethodException e) {
                    log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The callback method " + m_callbacks[i].getMethod() + " is not found", e);
                    getInstanceManager().stop();
                } catch (IllegalAccessException e) {
                    log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The callback method " + m_callbacks[i].getMethod() + " is not accessible", e);
                    getInstanceManager().stop();
                } catch (InvocationTargetException e) {
                    log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The callback method " + m_callbacks[i].getMethod() + " has throws an exception : " + e.getMessage());
                    getInstanceManager().stop();
                }
            }
        }
        // Update to internal state
        m_state = state;
    }
}
