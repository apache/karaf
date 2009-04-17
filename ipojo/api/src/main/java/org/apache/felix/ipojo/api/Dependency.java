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
package org.apache.felix.ipojo.api;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.PrimitiveInstanceDescription;
import org.apache.felix.ipojo.handlers.dependency.DependencyDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows configuring a service dependencies.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Dependency implements HandlerConfiguration {
    
    /**
     * The dynamic binding policy.
     */
    public static final int DYNAMIC = org.apache.felix.ipojo.handlers.dependency.Dependency.DYNAMIC_BINDING_POLICY;
    
    /**
     * The static binding policy. 
     */
    public static final int STATIC = org.apache.felix.ipojo.handlers.dependency.Dependency.STATIC_BINDING_POLICY;
    
    /**
     * The dynamic-priority binding policy.
     */
    public static final int DYNAMIC_PRIORITY = org.apache.felix.ipojo.handlers.dependency.Dependency.DYNAMIC_PRIORITY_BINDING_POLICY;

    /**
     * The required specification.
     */
    private String m_specification;
    
    /**
     * The LDAP filter of the dependency. 
     */
    private String m_filter;
    
    /**
     * The field of the implementation class attached to
     * this dependency. 
     */
    private String m_field;
    
    /**
     * Is the dependency optional? 
     */
    private boolean m_optional;
    
    /**
     * Is the dependency aggregate? 
     */
    private boolean m_aggregate;
    
    /**
     * Bind method attached to the dependency. 
     */
    private String m_bind;
    
    /**
     * Unbind method attached to the dependency. 
     */
    private String m_unbind;
    
    /**
     * The dependency binding policy. 
     */
    private int m_policy;
    
    /**
     * The dependency comparator.
     * (used to compare service providers) 
     */
    private String m_comparator;
    
    /**
     * The dependency default-implementation. 
     */
    private String m_di;
    
    /**
     * The dependency specific provider. 
     */
    private String m_from;
    
    /**
     * The dependency id. 
     */
    private String m_id;
    
    /**
     * Does the dependency supports nullable? 
     */
    private boolean m_nullable = true;

    /**
     * Gets the dependency metadata.
     * @return the 'requires' element describing
     * the current dependency.
     */
    public Element getElement() {
        ensureValidity();
        
        Element dep = new Element("requires", "");
        if (m_specification != null) {
            dep.addAttribute(new Attribute("specification", m_specification));
        }
        if (m_filter != null) {
            dep.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_field != null) {
            dep.addAttribute(new Attribute("field", m_field));
        }
        if (m_bind != null) {
            Element cb = new Element("callback", "");
            cb.addAttribute(new Attribute("type", "bind"));
            cb.addAttribute(new Attribute("method", m_bind));
            dep.addElement(cb);
        }
        if (m_unbind != null) {
            Element cb = new Element("callback", "");
            cb.addAttribute(new Attribute("type", "unbind"));
            cb.addAttribute(new Attribute("method", m_unbind));
            dep.addElement(cb);
        }
        if (m_comparator != null) {
            dep.addAttribute(new Attribute("comparator", m_comparator));
        }
        if (m_di != null) {
            dep.addAttribute(new Attribute("default-implementation", m_di));
        }
        if (m_from != null) {
            dep.addAttribute(new Attribute("from", m_from));
        }
        if (m_id != null) {
            dep.addAttribute(new Attribute("id", m_id));
        }
        if (! m_nullable) {
            dep.addAttribute(new Attribute("nullable", "false"));
        }
        if (m_optional) {
            dep.addAttribute(new Attribute("optional", "true"));
        }
        if (m_aggregate) {
            dep.addAttribute(new Attribute("aggregate", "true"));
        }
        if (m_policy != -1) {
            if (m_policy == DYNAMIC) {
                dep.addAttribute(new Attribute("policy", "dynamic"));
            } else if (m_policy == STATIC) {
                dep.addAttribute(new Attribute("policy", "static"));
            } else if (m_policy == DYNAMIC_PRIORITY) {
                dep.addAttribute(new Attribute("policy", "dynamic-priority"));
            }
            // No other possibilities.
        }
        return dep;
    }
    
    /**
     * Sets the required service specification.
     * @param spec the specification
     * @return the current dependency object.
     */
    public Dependency setSpecification(String spec) {
        m_specification = spec;
        return this;
    }
    
    /**
     * Sets the dependency filter.
     * @param filter the LDAP filter
     * @return the current dependency object
     */
    public Dependency setFilter(String filter) {
        m_filter = filter;
        return this;
    }
    
    /**
     * Sets the field attached to the dependency.
     * @param field the implementation class field name.
     * @return the current dependency object
     */
    public Dependency setField(String field) {
        m_field = field;
        return this;
    }
    
    /**
     * Sets the dependency optionality.
     * @param opt <code>true</code> to set the 
     * dependency to optional.
     * @return the current dependency object.
     */
    public Dependency setOptional(boolean opt) {
        m_optional = opt;
        return this;
    }
    
    /**
     * Sets the dependency cardinality.
     * @param agg <code>true</code> to set the 
     * dependency to aggregate.
     * @return the current dependency object.
     */
    public Dependency setAggregate(boolean agg) {
        m_aggregate = agg;
        return this;
    }
    
    /**
     * Sets if the dependency supports nullable objects.
     * @param nullable <code>false</code> if the dependency does not
     * support the nullable object injection
     * @return the current dependency object.
     */
    public Dependency setNullable(boolean nullable) {
        m_nullable = nullable;
        return this;
    }
    
    /**
     * Sets the dependency bind method.
     * @param bind the bind method name
     * @return the current dependency object.
     */
    public Dependency setBindMethod(String bind) {
        m_bind = bind;
        return this;
    }
    
    /**
     * Sets the dependency unbind method.
     * @param unbind the unbind method
     * @return the current dependency object.
     */
    public Dependency setUnbindMethod(String unbind) {
        m_unbind = unbind;
        return this;
    }
    
    /**
     * Sets the dependency binding policy.
     * @param policy the binding policy
     * @return the current dependency object
     */
    public Dependency setBindingPolicy(int policy) {
        m_policy = policy;
        return this;
    }
    
    /**
     * Sets the dependency comparator.
     * @param cmp the comparator class name
     * @return the current dependency object
     */
    public Dependency setComparator(String cmp) {
        m_comparator = cmp;
        return this;
    }
    
    /**
     * Sets the dependency default-implementation.
     * @param di the default-implementation class name
     * @return the current dependency object
     */
    public Dependency setDefaultImplementation(String di) {
        m_di = di;
        return this;
    }
    
    /**
     * Sets the dependency 'from' attribute.
     * @param from the name of the service provider.
     * @return the current dependency object
     */
    public Dependency setFrom(String from) {
        m_from = from;
        return this;
    }
    
    /**
     * Sets the dependency id.
     * @param id the dependency id.
     * @return the current dependency object.
     */
    public Dependency setId(String id) {
        m_id = id;
        return this;
    }
    
    /**
     * Checks dependency configuration validity.
     */
    private void ensureValidity() {
        // At least a field or methods.
        if (m_field == null && m_bind == null && m_unbind == null) {
            throw new IllegalStateException("A dependency must have a field or bind/unbind methods");
        }
        // Check binding policy.
        if (m_policy != -1) {
            if (!(m_policy == DYNAMIC || m_policy == STATIC || m_policy == DYNAMIC_PRIORITY)) {
                throw new IllegalStateException("Unknow binding policy : " + m_policy);
            }
        }
    }
    
    /**
     * Gets the dependency description object attached to
     * this dependency.
     * @param instance the instance on which searching the dependency
     * @return the dependency description attached to this dependency or 
     * <code>null</code> if the dependency cannot be found.
     */
    public DependencyDescription getDependencyDescription(ComponentInstance instance) {
        PrimitiveInstanceDescription desc = (PrimitiveInstanceDescription) instance.getInstanceDescription();
        if (m_id != null) {
            return desc.getDependency(m_id);
        }
        if (m_specification != null) {
            return desc.getDependency(m_specification);
        }
        DependencyDescription[] deps = desc.getDependencies();
        if (deps.length == 1) {
            return deps[0];
        }
        // Cannot determine the dependency. 
        return null;
    }
    
    
}
