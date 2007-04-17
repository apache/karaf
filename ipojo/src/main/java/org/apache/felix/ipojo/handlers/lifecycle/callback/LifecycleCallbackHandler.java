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
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;

/**
 * Lifecycle callback handler.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class LifecycleCallbackHandler extends Handler {

    /**
     * The list of the callback of the component.
     */
    private LifecycleCallback[] m_callbacks = new LifecycleCallback[0];

    /**
     * State of the instance manager (unresolved at the beginning).
     */
    private int m_state = InstanceManager.INVALID;

    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

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
     * @param cm : the instance manager
     * @param metadata : the component type metadata
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(InstanceManager cm, Element metadata, Dictionary configuration) {
        m_manager = cm;
        m_callbacks = new LifecycleCallback[0];

        if (metadata.containsAttribute("immediate") && metadata.getAttribute("immediate").equalsIgnoreCase("true")) {
            m_immediate = true;
        }

        Element[] hooksMetadata = metadata.getElements("callback");
        for (int i = 0; i < hooksMetadata.length; i++) {
            // Create an HookMetadata object
            String initialState = hooksMetadata[i].getAttribute("initial");
            String finalState = hooksMetadata[i].getAttribute("final");
            String method = hooksMetadata[i].getAttribute("method");
            boolean isStatic = false;
            if (hooksMetadata[i].containsAttribute("isStatic") && hooksMetadata[i].getAttribute("isStatic").equals("true")) {
                isStatic = true;
            }

            LifecycleCallback hk = new LifecycleCallback(this, initialState, finalState, method, isStatic);
            addCallback(hk);
        }
        if (m_callbacks.length > 0 || m_immediate) {
            m_manager.register(this);
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
     * Get the instance manager.
     * @return the instance manager
     */
    protected InstanceManager getInstanceManager() {
        return m_manager;
    }

    /**
     * When the state change call the associated callback.
     * 
     * @param state : the new isntance state.
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        // Manage immediate component
        if (m_state == ComponentInstance.INVALID && state == ComponentInstance.VALID && m_manager.getPojoObjects().length == 0) {
            m_manager.createPojoObject();
        }

        for (int i = 0; i < m_callbacks.length; i++) {
            if (m_callbacks[i].getInitialState() == m_state && m_callbacks[i].getFinalState() == state) {
                try {
                    m_callbacks[i].call();
                } catch (NoSuchMethodException e) {
                    m_manager.getFactory().getLogger().log(Logger.ERROR,
                            "[" + m_manager.getClassName() + "] The callback method " + m_callbacks[i].getMethod() + " is not found", e);
                } catch (IllegalAccessException e) {
                    m_manager.getFactory().getLogger().log(Logger.ERROR,
                            "[" + m_manager.getClassName() + "] The callback method " + m_callbacks[i].getMethod() + " is not accessible", e);
                } catch (InvocationTargetException e) {
                    m_manager.getFactory().getLogger().log(
                            Logger.ERROR,
                            "[" + m_manager.getClassName() + "] The callback method " + m_callbacks[i].getMethod() + " has throws an exception : "
                                    + e.getMessage() + " -> " + e.getCause());
                }
            }
        }
        // Update to internal state
        m_state = state;
    }

}
