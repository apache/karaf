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
package org.apache.felix.ipojo.composite.instance;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.composite.instance.InstanceHandler.ManagedConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Description of the Instance Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceHandlerDescription extends HandlerDescription {

  
    /**
     * List of managed instances.
     */
    private ManagedConfiguration[] m_configurations; 

    /**
     * Constructor.
     * 
     * @param handler : handler
     * @param insts : list of component instances
     */
    public InstanceHandlerDescription(CompositeHandler handler, ManagedConfiguration[] insts) {
        super(handler);
        m_configurations = insts;
    }

    /**
     * Build handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element instances = super.getHandlerInfo();
        for (int i = 0; i < m_configurations.length; i++) {
            ManagedConfiguration inst = m_configurations[i];
            Element instance = new Element("Instance", "");
            if (inst.getInstance() == null) { 
                instance.addAttribute(new Attribute("Factory", inst.getConfiguration().get("component").toString()));
                instance.addAttribute(new Attribute("State", "Not Available"));
            } else {
                instance.addAttribute(new Attribute("Factory", inst.getFactory()));
                instance.addAttribute(new Attribute("Name", inst.getInstance().getInstanceName()));
                String state = null;
                switch(inst.getInstance().getState()) {
                    case ComponentInstance.DISPOSED : 
                        state = "disposed"; break;
                    case ComponentInstance.STOPPED : 
                        state = "stopped"; break;
                    case ComponentInstance.VALID : 
                        state = "valid"; break;
                    case ComponentInstance.INVALID : 
                        state = "invalid"; break;
                    default :
                        break;
                }
                instance.addAttribute(new Attribute("State", state));
                // The instance description is already contained inside parent instance description.
                //instance.addElement(inst.getInstance().getInstanceDescription().getDescription());
            }
            instances.addElement(instance);
        }
        return instances;
    }

}
