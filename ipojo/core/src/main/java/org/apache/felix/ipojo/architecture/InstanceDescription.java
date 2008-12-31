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
package org.apache.felix.ipojo.architecture;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Instance Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceDescription implements InstanceStateListener {
   
    /**
     * The list of handlers plugged on the component instance.
     */
    protected HandlerDescription[] m_handlers = new HandlerDescription[0];
    
    /**
     * The Underlying component instance.
     */
    protected ComponentInstance m_instance;

    /**
     * Component Type of the instance.
     */
    protected ComponentTypeDescription m_type;

    /**
     * Creates the instance description.
     * @param ci  the state of the instance.
     * @param desc  the component type description of this instance.
     */
    public InstanceDescription(ComponentTypeDescription desc, ComponentInstance ci) {
        m_handlers = new HandlerDescription[0];
        m_type = desc;
        m_instance = ci;
        m_instance.addInstanceStateListener(this);
    }

    /**
     * Gets the instance name.
     * @return the name of the instance.
     */
    public String getName() {
        return m_instance.getInstanceName();
    }

    /**
     * Gets the component type description of the described instance.
     * @return : the component type description of this instance.
     */
    public ComponentTypeDescription getComponentDescription() {
        return m_type;
    }

    /**
     * Gets the plugged handler list.
     * @return the live handler list
     */
    public HandlerDescription[] getHandlers() {
        return m_handlers;
    }

    /**
     * Adds an handler description to the list.
     * @param desc : the handler description to add
     */
    public void addHandler(HandlerDescription desc) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i] == desc) {
                return; // NOTHING TO DO, the description is already in the
                        // array
            }
        }
        // The component Description is not in the array, add it
        HandlerDescription[] newHd = new HandlerDescription[m_handlers.length + 1];
        System.arraycopy(m_handlers, 0, newHd, 0, m_handlers.length);
        newHd[m_handlers.length] = desc;
        m_handlers = newHd;
    }
    
    /**
     * Gets a handler description by specifying the handler qualified name.
     * @param handler the handler name
     * @return the handler description or <code>null</code> if not found
     */
    public HandlerDescription getHandlerDescription(String handler) {
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getHandlerName().equals(handler)) {
                return m_handlers[i];
            }
        }
        return null;
    }

    /**
     * Gets the state of the described instance.
     * @return the state of the instance.
     */
    public int getState() {
        waitForStability();
        return m_instance.getState();
    }

    /**
     * Gets the bundle id of the bundle containing the component type of the instance.
     * @return the bundle id owning the component implementation class.
     */
    public long getBundleId() {
        return m_instance.getFactory().getBundleContext().getBundle().getBundleId();
    }

    /**
     * Gets the instance description.
     * @return the instance description
     */
    public Element getDescription() {
        Element instance = new Element("Instance", "");
        instance.addAttribute(new Attribute("name", getName())); // Name
        
        int state = getState();
        if (state == ComponentInstance.STOPPED) {
            instance.addAttribute(new Attribute("state", "stopped"));
        }
        if (state == ComponentInstance.VALID) {
            instance.addAttribute(new Attribute("state", "valid"));
        }
        if (state == ComponentInstance.INVALID) {
            instance.addAttribute(new Attribute("state", "invalid"));
        }
        if (state == ComponentInstance.DISPOSED) {
            instance.addAttribute(new Attribute("state", "disposed"));
        }
        // Bundle
        instance.addAttribute(new Attribute("bundle", Long.toString(getBundleId())));

        // Component Type
        instance.addAttribute(new Attribute("component.type", m_type.getName()));

        // Handlers
        for (int i = 0; i < m_handlers.length; i++) {
            instance.addElement(m_handlers[i].getHandlerInfo());
        }

        return instance;

    }

    /**
     * Waits for state stability before returning results.
     */
    private synchronized void waitForStability() {
        while (m_instance.getState() == -2) { // Transition
            try {
                wait();
            } catch (InterruptedException e) {
               // We're interrupted, will re-check the condition.
            }
        }
        
    }

    /**
     * The underlying instance state changes.
     * @param instance the instance
     * @param newState the new state
     * @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public synchronized void stateChanged(ComponentInstance instance, int newState) {
        notifyAll(); // if we was in a transition, the transition is now done.
    }

}
