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

import org.apache.felix.ipojo.api.HandlerConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;

/**
 * Allows defining an imported service. A service import is the 
 * publication of service from the parent composite
 * or global registry inside the composite. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ImportedService implements HandlerConfiguration {
    
    /**
     * Scoping policy: composite.
     */
    public static final String COMPOSITE_SCOPE = "composite";
    
    /**
     * Scoping policy: global.
     */
    public static final String GLOBAL_SCOPE = "global";
    
    /**
     * Scoping policy: composite+global.
     */
    public static final String COMPOSITE_AND_GLOBAL_SCOPE = "composite+global";
    
    
    /**
     * The required specification.
     */
    private String m_specification;
    
    /**
     * The LDAP filter of the dependency. 
     */
    private String m_filter;
    
    /**
     * Is the dependency optional? 
     */
    private boolean m_optional;
    
    /**
     * Is the dependency aggregate? 
     */
    private boolean m_aggregate;
  
    /**
     * The dependency binding policy. 
     * Default: Dynamic policy.
     */
    private int m_policy = DependencyModel.DYNAMIC_BINDING_POLICY;
    
    /**
     * The dependency comparator.
     * (used to compare service providers) 
     */
    private String m_comparator;
      
     /**
     * The dependency id. 
     */
    private String m_id;
    
    /**
     * Dependency scope.
     */
    private String m_scope = COMPOSITE_SCOPE;
   
    /**
     * Gets the dependency metadata.
     * @return the 'requires' element describing
     * the current dependency.
     */
    public Element getElement() {
        ensureValidity();
        
        Element dep = new Element("subservice", "");
        dep.addAttribute(new Attribute("action", "import"));
       
        dep.addAttribute(new Attribute("specification", m_specification));
        dep.addAttribute(new Attribute("scope", m_scope));

        
        if (m_filter != null) {
            dep.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_comparator != null) {
            dep.addAttribute(new Attribute("comparator", m_comparator));
        }
  
        if (m_id != null) {
            dep.addAttribute(new Attribute("id", m_id));
        }
       
        if (m_optional) {
            dep.addAttribute(new Attribute("optional", "true"));
        }
        if (m_aggregate) {
            dep.addAttribute(new Attribute("aggregate", "true"));
        }

        if (m_policy == DependencyModel.DYNAMIC_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "dynamic"));
        } else if (m_policy == DependencyModel.STATIC_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "static"));
        } else if (m_policy == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "dynamic-priority"));
        }
        
        return dep;
    }
    
    /**
     * Sets the required service specification.
     * @param spec the specification
     * @return the current imported sub-service.
     */
    public ImportedService setSpecification(String spec) {
        m_specification = spec;
        return this;
    }
    
    /**
     * Sets the dependency filter.
     * @param filter the LDAP filter
     * @return the current imported sub-service
     */
    public ImportedService setFilter(String filter) {
        m_filter = filter;
        return this;
    }
    
    
    /**
     * Sets the dependency optionality.
     * @param opt <code>true</code> to set the 
     * dependency to optional.
     * @return the current imported sub-service.
     */
    public ImportedService setOptional(boolean opt) {
        m_optional = opt;
        return this;
    }
    
    /**
     * Sets the dependency cardinality.
     * @param agg <code>true</code> to set the 
     * dependency to aggregate.
     * @return the current imported sub-service.
     */
    public ImportedService setAggregate(boolean agg) {
        m_aggregate = agg;
        return this;
    }
    
    
    /**
     * Sets the dependency binding policy.
     * @param policy the binding policy
     * @return the current imported sub-service
     */
    public ImportedService setBindingPolicy(int policy) {
        m_policy = policy;
        return this;
    }
    
    /**
     * Sets the dependency comparator.
     * @param cmp the comparator class name
     * @return the current imported sub-service
     */
    public ImportedService setComparator(String cmp) {
        m_comparator = cmp;
        return this;
    }
    
    
    /**
     * Sets the dependency id.
     * @param id the dependency id.
     * @return the current imported sub-service.
     */
    public ImportedService setId(String id) {
        m_id = id;
        return this;
    }
    
    /**
     * Sets the dependency scope.
     * @param scope the dependency scope (global, composite or
     * composite+global).
     * @return the current imported sub-service.
     */
    public ImportedService setScope(String scope) {
        m_scope = scope;
        return this;
    }
    
    /**
     * Checks dependency configuration validity.
     */
    private void ensureValidity() {
        // Check specification
        if (m_specification == null) {
            throw new IllegalStateException("The specification of the imported service must be set");
        }

        // Check binding policy.
        if (!(m_policy == DependencyModel.DYNAMIC_BINDING_POLICY || m_policy == DependencyModel.STATIC_BINDING_POLICY || m_policy == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY)) {
            throw new IllegalStateException("Unknown binding policy : " + m_policy);
        }
    }

}
