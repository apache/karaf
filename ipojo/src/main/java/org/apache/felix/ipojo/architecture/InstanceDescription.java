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

/**
 * Instance Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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
    private ComponentDescription m_type;
    
    /**
     * COntained instance list.
     */
    private InstanceDescription[] m_containedInstances = new InstanceDescription[0];

    /**
     * Constructor.
     * @param name : the name of the component instance.
     * @param state : the state of the instance.
     * @param bundleId : bundle id owning this instance.
     * @param cd : the component type description of this instance.
     */
    public InstanceDescription(String name, int state, long bundleId, ComponentDescription cd) {
        m_name = name;
        m_state = state;
        m_createdObjects = new String[0];
        m_handlers = new HandlerDescription[0];
        m_containedInstances = new InstanceDescription[0];
        m_bundleId = bundleId;
        m_type = cd;
    }

    /**
     * @return the name of the component.
     */
    public String getName() { return m_name; }

    /**
     * @return the created instances
     */
    public String[] getCreatedObjects() { return m_createdObjects; }

    /**
     * Set the instances array.
     */
    public void setCreatedObjects(String[] objects) { m_createdObjects = objects; }

    /**
     * @return : the component type description of this instance.
     */
    public ComponentDescription getComponentDescription() { return m_type; }

    /**
     * @return the live handler list
     */
    public HandlerDescription[] getHandlers() { return m_handlers; }

    /**
     * Add an handler description to the list.
     * @param hd : the handler description to add
     */
    public void addHandler(HandlerDescription hd) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; (i < m_handlers.length); i++) {
            if (m_handlers[i] == hd) {
                return; //NOTHING TO DO, the description is already in the array
            }
        }
            // The component Description is not in the array, add it
        HandlerDescription[] newHd = new HandlerDescription[m_handlers.length + 1];
        System.arraycopy(m_handlers, 0, newHd, 0, m_handlers.length);
        newHd[m_handlers.length] = hd;
        m_handlers = newHd;
    }
    
    /**
     * Add an instance description to the contained instance list.
     * @param inst : the handler description to add
     */
    public void addInstance(InstanceDescription inst) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; (i < m_containedInstances.length); i++) {
            if (m_containedInstances[i] == inst) {
                return; //NOTHING TO DO, the description is already in the array
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
     * @param i : the state
     */
    public void setState(int i) { m_state = i; }

    /**
     * @return the state of the component.
     */
    public int getState() { return m_state; }

    /**
     * @return the bundle id owning the component implementation class.
     */
    public long getBundleId() { return m_bundleId; }
    
    /**
     * @return the list of contained instances.
     */
    public InstanceDescription[] getContainedInstances() { return m_containedInstances; }


}
