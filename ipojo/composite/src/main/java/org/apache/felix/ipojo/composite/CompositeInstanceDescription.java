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
package org.apache.felix.ipojo.composite;

import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Composite Instance Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeInstanceDescription extends InstanceDescription {
    
    /**
     * Creates a Primitive Instance Description.
     * @param type the component type description
     * @param instance the instance description
     */
    public CompositeInstanceDescription(ComponentTypeDescription type, CompositeManager instance) {
        super(type, instance);
    }


    /**
     * Gets the list of contained instance in the describe instance.
     * This list contains only instances who exposed their architecture.
     * @return the list of contained instances.
     */
    public InstanceDescription[] getContainedInstances() {
        // Get instances description of internal instance
        ServiceContext internal = ((CompositeManager) m_instance).getServiceContext();
        try {
            ServiceReference[]refs = internal.getServiceReferences(Architecture.class.getName(), null);
            if (refs != null) {
                InstanceDescription[] descs = new InstanceDescription[refs.length];
                for (int i = 0; i < refs.length; i++) {
                    Architecture arch = (Architecture) internal.getService(refs[i]);
                    descs[i] = arch.getInstanceDescription();
                    internal.ungetService(refs[i]);
                }
                return descs;
            }
        } catch (InvalidSyntaxException e) {
            // Cannot happen
        }
        return new InstanceDescription[0];
    }

    
    /**
     * Gets the instance description.
     * Overridden to add created objects.
     * @return the instance description
     */
    public Element getDescription() {
        Element elem = super.getDescription();
        // Contained instance (exposing architecture) (empty if primitive)
        InstanceDescription[] descs = getContainedInstances();
        if (descs.length > 0) {
            Element inst = new Element("ContainedInstances", "");
            for (int i = 0; i < descs.length; i++) {
                inst.addElement(descs[i].getDescription());
            }
            elem.addElement(inst);
        }
        return elem;
    }

}
