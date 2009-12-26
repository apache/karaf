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
package org.apache.felix.ipojo.handlers.dependency;

import java.util.Iterator;
import java.util.List;

import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Dependency Handler Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyHandlerDescription extends HandlerDescription {

    /**
     * Dependencies managed by the dependency handler.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];
    
    // TODO Define the DependencyStateListener Interface (in ipojo utils)
    
    // TODO Add the list of listener.
    
    // TODO Add register listener method.
    
    // TODO Add unregister listener method.
    
    // TODO Implement the validate method.
    
    // TODO Implement the invalidate method.
    
    // TODO Implement the onServiceArrival method.
    
    // TODO Implement the onServiceDeparture method.
    
    // TODO Implement the onServiceBound method.
    
    // TODO Implement the onServiceUnbound method.

    /**
     * Creates the Dependency Handler description.
     * @param handler the Dependency Handler.
     * @param deps the Dependencies
     */
    public DependencyHandlerDescription(DependencyHandler handler, Dependency[] deps) {
        super(handler);
        m_dependencies = new DependencyDescription[deps.length];
        for (int i = 0; i < m_dependencies.length; i++) {
            m_dependencies[i] = new DependencyDescription(deps[i]);
            //TODO Register callback there on the dependency model.
        }
    }

    /**
     * Get dependencies description.
     * @return the dependencies list.
     */
    public DependencyDescription[] getDependencies() {
        return m_dependencies;
    }

    /**
     * Builds the Dependency Handler description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element deps = super.getHandlerInfo();
        for (int i = 0; i < m_dependencies.length; i++) {
            String state = "resolved";
            if (m_dependencies[i].getState() == DependencyModel.UNRESOLVED) {
                state = "unresolved";
            }
            if (m_dependencies[i].getState() == DependencyModel.BROKEN) {
                state = "broken";
            }
            Element dep = new Element("Requires", "");
            dep.addAttribute(new Attribute("Specification", m_dependencies[i].getInterface()));
            dep.addAttribute(new Attribute("Id", m_dependencies[i].getId()));
            
            if (m_dependencies[i].getFilter() != null) {
                dep.addAttribute(new Attribute("Filter", m_dependencies[i].getFilter()));
            }
            
            if (m_dependencies[i].isOptional()) {
                dep.addAttribute(new Attribute("Optional", "true"));
                if (m_dependencies[i].supportsNullable()) {
                    dep.addAttribute(new Attribute("Nullable", "true"));    
                }
                if (m_dependencies[i].getDefaultImplementation() != null) {
                    dep.addAttribute(new Attribute("Default-Implementation", m_dependencies[i].getDefaultImplementation()));
                }
            } else {
                dep.addAttribute(new Attribute("Optional", "false"));
            }

            if (m_dependencies[i].isMultiple()) {
                dep.addAttribute(new Attribute("Aggregate", "true"));
            } else {
                dep.addAttribute(new Attribute("Aggregate", "false"));
            }
            
            if (m_dependencies[i].isProxy()) {
                dep.addAttribute(new Attribute("Proxy", "true"));
            } else {
                dep.addAttribute(new Attribute("Proxy", "false"));
            }
            
            String policy = "dynamic";
            if (m_dependencies[i].getPolicy() == DependencyModel.STATIC_BINDING_POLICY) {
                policy = "static";
            } else if (m_dependencies[i].getPolicy() == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY) {
                policy = "dynamic-priority";
            }
            dep.addAttribute(new Attribute("Binding-Policy", policy));
            
            if (m_dependencies[i].getComparator() != null) {
                dep.addAttribute(new Attribute("Comparator", m_dependencies[i].getComparator()));
            }
            
            dep.addAttribute(new Attribute("State", state));
            List set = m_dependencies[i].getUsedServices();
            if (set != null) {
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Element use = new Element("Uses", "");
                    ServiceReference ref = (ServiceReference) iterator.next();
                    use.addAttribute(new Attribute("service.id", ref.getProperty(Constants.SERVICE_ID).toString()));                
                    String instance = (String) ref.getProperty("instance.name");
                    if (instance != null) {
                        use.addAttribute(new Attribute("instance.name", instance));
                    }
                    dep.addElement(use);
                }
            }
            
            deps.addElement(dep);
        }
        return deps;
    }

}
