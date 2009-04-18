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
package org.apache.felix.ipojo.api.composite;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.api.HandlerConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows defining a provided service. A provided service is a service
 * 'implemented' by the composite. This implementations relies (by 
 * delegation) on contained instances and services. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedService implements HandlerConfiguration {
    /**
     * Delegation policy: all.
     */
    public static final String ALL_POLICY = "all";
    
    /**
     * Delegation policy: one. 
     */
    public static final String ONE_POLICY = "one";
    
    /**
     * The provided specification.
     */
    private String m_specification;
    
    /**
     * List of delegation.
     * List of Element ({delegation $method $policy}) 
     */
    private List m_delegation = new ArrayList();


    /**
     * Gets the provided element.
     * @return the 'provides' element describing
     * the current provided service.
     */
    public Element getElement() {
        ensureValidity();
        
        Element dep = new Element("provides", "");
        dep.addAttribute(new Attribute("action", "implement"));
       
        dep.addAttribute(new Attribute("specification", m_specification));

        for (int i = 0; i < m_delegation.size(); i++) {
            dep.addElement((Element) m_delegation.get(i));
        }
        
        return dep;
    }
    
    /**
     * Sets the provided service specification.
     * @param spec the specification
     * @return the current provided service.
     */
    public ProvidedService setSpecification(String spec) {
        m_specification = spec;
        return this;
    }
    
    /**
     * Sets the delegation policy of the given method.
     * @param method  the method name
     * @param policy the delegation policy
     * @return the current exported service.
     */
    public ProvidedService setDelegation(String method, String policy) {
        Element element = new Element("delegation", "");
        element.addAttribute(new Attribute("method", method));
        element.addAttribute(new Attribute("policy", policy));
        m_delegation.add(element);
        return this;
    }

    
    /**
     * Checks provided service configuration validity.
     */
    private void ensureValidity() {
        // Check specification
        if (m_specification == null) {
            throw new IllegalStateException("The specification of the implemented service must be set");
        }
    }

}
