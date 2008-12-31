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
package org.apache.felix.ipojo;

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Primitive Instance Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PrimitiveInstanceDescription extends InstanceDescription {
    
    /**
     * Creates a Primitive Instance Description.
     * @param type the component type description
     * @param instance the instance description
     */
    public PrimitiveInstanceDescription(ComponentTypeDescription type, InstanceManager instance) {
        super(type, instance);
    }

    /**
     * Gets the list of object created by the described instance.
     * @return the created objects.
     */
    public String[] getCreatedObjects() {
        // TODO should get a copy
        Object [] objs = ((InstanceManager) m_instance).getPojoObjects();
        if (objs != null) {
            String[] result = new String[objs.length];
            for (int i = 0; i < objs.length; i++) {
                result[i] = objs[i].toString();
            }
            return result;
        } else {
            return new String[0];
        }
    }
    
    /**
     * Gets the instance description.
     * Overridden to add created objects.
     * @return the instance description
     */
    public Element getDescription() {
        Element elem = super.getDescription();
        // Created Object (empty is composite)
        String[] objs = getCreatedObjects();
        for (int i = 0; i < objs.length; i++) {
            Element obj = new Element("Object", "");
            obj.addAttribute(new Attribute("name", ((Object) objs[i]).toString()));
            elem.addElement(obj);
        }
        return elem;
    }
    
    

}
