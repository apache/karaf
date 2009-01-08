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
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandler;
import org.apache.felix.ipojo.handlers.configuration.ConfigurationHandlerDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandlerDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceDescription;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandlerDescription;
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
     * Gets the instance service dependencies.
     * @return the set of dependency description or an empty array if
     * no dependencies.
     */
    public DependencyDescription[] getDependencies() {
        Handler handler =  ((InstanceManager) m_instance).getHandler("org.apache.felix.ipojo:requires");
        if (handler == null) {
            return new DependencyDescription[0];
        } else {
            return ((DependencyHandlerDescription) ((DependencyHandler) handler)
                    .getDescription()).getDependencies();
        }
    }
    
    /**
     * Gets the instance service dependency matching with the given service specification or id.
     * @param specification the service specification of the looked specification.
     * @return the dependency description matching with the given service specification or id.
     * <code>null</code> is not found.
     * no dependencies.
     */
    public DependencyDescription getDependency(String specification) {
        DependencyDescription[] deps =  getDependencies();
        if (deps == null) {
            return null;
        } else {
            for (int i = 0; i < deps.length; i++) {
                if (specification.equals(deps[i].getId())
                        || specification.equals(deps[i].getSpecification())) {
                    return deps[i];
                }
                        
            }
        }
        return null;
    }
    
    /**
     * Gets the instance provided service matching with the given service specification.
     * @param specification the provided specification of the looked provided service.
     * @return the provided service description matching with the given service specification.
     * <code>null</code> is not found.
     */
    public ProvidedServiceDescription getProvidedService(String specification) {
        ProvidedServiceDescription[] pss =  getProvidedServices();
        if (pss == null) {
            return null;
        } else {
            for (int i = 0; i < pss.length; i++) {
                String[] str = pss[i].getServiceSpecifications();
                for  (int j = 0; j < str.length; j++) {
                    if (specification.equals(str[j])) {
                        return pss[i];
                    }
                }        
            }
        }
        return null;
    }
    
    /**
     * Gets the instance provided service.
     * @return the set of provided service description or an empty array if
     * no provided services.
     */
    public ProvidedServiceDescription[] getProvidedServices() {
        Handler handler =  ((InstanceManager) m_instance).getHandler("org.apache.felix.ipojo:provides");
        if (handler == null) {
            return new ProvidedServiceDescription[0];
        } else {
            return ((ProvidedServiceHandlerDescription) ((ProvidedServiceHandler) handler)
                    .getDescription()).getProvidedServices();
        }
    }
    
    /**
     * Gets the instance properties.
     * @return the set of property descriptions or an empty array if
     * no properties.
     */
    public PropertyDescription[] getProperties() {
        Handler handler =  ((InstanceManager) m_instance).getHandler("org.apache.felix.ipojo:properties");
        if (handler == null) {
            return new PropertyDescription[0];
        } else {
            return ((ConfigurationHandlerDescription) ((ConfigurationHandler) handler)
                    .getDescription()).getProperties();
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
