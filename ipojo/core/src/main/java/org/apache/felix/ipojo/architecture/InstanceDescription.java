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
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Instance Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceDescription {

    /**
     * The name of the component (instance).
     */
    private String m_name;

    /**
     * Handlers of the component instance.
     */
    private HandlerDescription[] m_handlers = new HandlerDescription[0];

    /**
     * Created Instances of the components.
     */
    private String[] m_createdObjects = new String[0];

    /**
     * State of the component (VALID / UNRESOLVED).
     */
    private int m_state;

    /**
     * BundleId who create the instance.
     */
    private long m_bundleId;

    /**
     * Component Type of the instance.
     */
    private ComponentTypeDescription m_type;

    /**
     * COntained instance list.
     */
    private InstanceDescription[] m_containedInstances = new InstanceDescription[0];

    /**
     * Constructor.
     * 
     * @param name : the name of the component instance.
     * @param state : the state of the instance.
     * @param bundleId : bundle id owning this instance.
     * @param desc : the component type description of this instance.
     */
    public InstanceDescription(String name, int state, long bundleId, ComponentTypeDescription desc) {
        m_name = name;
        m_state = state;
        m_createdObjects = new String[0];
        m_handlers = new HandlerDescription[0];
        m_containedInstances = new InstanceDescription[0];
        m_bundleId = bundleId;
        m_type = desc;
    }

    /**
     * Get the instance name.
     * @return the name of the instance.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the list of object created by the described instance.
     * @return the created instances
     */
    public String[] getCreatedObjects() {
        return m_createdObjects;
    }

    /**
     * Set the array of objects created by the described instance.
     * @param objects : the list of create objects.
     */
    public void setCreatedObjects(String[] objects) {
        m_createdObjects = objects;
    }

    /**
     * Get the component type description of the described instance.
     * @return : the component type description of this instance.
     */
    public ComponentTypeDescription getComponentDescription() {
        return m_type;
    }

    /**
     * Get the plugged handler list.
     * @return the live handler list
     */
    public HandlerDescription[] getHandlers() {
        return m_handlers;
    }

    /**
     * Add an handler description to the list.
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
     * Add an instance description to the contained instance list.
     * 
     * @param inst : the handler description to add
     */
    public void addInstance(InstanceDescription inst) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; i < m_containedInstances.length; i++) {
            if (m_containedInstances[i].getName().equals(inst.getName())) {
                return; // NOTHING TO DO, the description is already in the array
            }
        }
        // The component Description is not in the array, add it
        InstanceDescription[] newCi = new InstanceDescription[m_containedInstances.length + 1];
        System.arraycopy(m_containedInstances, 0, newCi, 0, m_containedInstances.length);
        newCi[m_containedInstances.length] = inst;
        m_containedInstances = newCi;
    }

    /**
     * Set the state of the component.
     * 
     * @param state : the state
     */
    public void setState(int state) {
        m_state = state;
    }

    /**
     * Get the state of the described instance.
     * @return the state of the instance.
     */
    public int getState() {
        return m_state;
    }

    /**
     * Get the bundle id of the bundle containing the described instance.
     * @return the bundle id owning the component implementation class.
     */
    public long getBundleId() {
        return m_bundleId;
    }

    /**
     * Get the list of contained instance in the describe instance.
     * This list contains only instances who exposed their architecture.
     * @return the list of contained instances.
     */
    public InstanceDescription[] getContainedInstances() {
        return m_containedInstances;
    }

    /**
     * Get the instance description.
     * @return the instance description
     */
    public Element getDescription() {
        Element instance = new Element("Instance", "");
        instance.addAttribute(new Attribute("name", getName())); // Name
        // State
        if (m_state == ComponentInstance.STOPPED) {
            instance.addAttribute(new Attribute("state", "stopped"));
        }
        if (m_state == ComponentInstance.VALID) {
            instance.addAttribute(new Attribute("state", "valid"));
        }
        if (m_state == ComponentInstance.INVALID) {
            instance.addAttribute(new Attribute("state", "invalid"));
        }
        if (m_state == ComponentInstance.DISPOSED) {
            instance.addAttribute(new Attribute("state", "disposed"));
        }
        // Bundle
        instance.addAttribute(new Attribute("bundle", Long.toString(m_bundleId)));

        // Component Type
        instance.addAttribute(new Attribute("component.type", m_type.getName()));

        // Handlers
        for (int i = 0; i < m_handlers.length; i++) {
            instance.addElement(m_handlers[i].getHandlerInfo());
        }
        // Created Object (empty is composite)
        for (int i = 0; i < m_createdObjects.length; i++) {
            Element obj = new Element("Object", "");
            obj.addAttribute(new Attribute("name", ((Object) m_createdObjects[i]).toString()));
            instance.addElement(obj);
        }
        // Contained instance (exposing architecture) (empty if primitive)
        if (m_containedInstances.length > 0) {
            Element inst = new Element("ContainedInstances", "");
            for (int i = 0; i < m_containedInstances.length; i++) {
                inst.addElement(m_containedInstances[i].getDescription());
                instance.addElement(inst);
            }
        }
        return instance;

    }

}
