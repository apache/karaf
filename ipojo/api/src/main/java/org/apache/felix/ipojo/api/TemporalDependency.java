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

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows configuring a service dependencies.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TemporalDependency implements HandlerConfiguration {
    
    /**
     * OnTimeout policy: nullable object.
     */
    public static final String NULLABLE = "nullable";
    
    /**
     * OnTimeout policy: empty array or collection.
     */
    public static final String EMPTY = "empty";
    
    /**
     * OnTimeout policy: inject null.
     */
    public static final String NULL = "null";


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
     * OnTimeout policy.
     */
    private String m_onTimeout;
    
    /**
     * Timeout. 
     */
    private String m_timeout;
    
    /**
     * Proxy. 
     */
    private boolean m_proxy = false;

    /**
     * Gets the dependency metadata.
     * @return the 'requires' element describing
     * the current dependency.
     */
    public Element getElement() {
        ensureValidity();
        
        Element dep = new Element("requires", "org.apache.felix.ipojo.handler.temporal");
        if (m_specification != null) {
            dep.addAttribute(new Attribute("specification", m_specification));
        }
        if (m_filter != null) {
            dep.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_field != null) {
            dep.addAttribute(new Attribute("field", m_field));
        }
        if (m_onTimeout != null) {
            dep.addAttribute(new Attribute("omTimeout", m_onTimeout));
        }
        if (m_timeout != null) {
            dep.addAttribute(new Attribute("timeout", m_timeout));
        }
        if (m_proxy) {
            dep.addAttribute(new Attribute("proxy", "true"));
        }
        
        return dep;
    }
    
    /**
     * Sets the required service specification.
     * @param spec the specification
     * @return the current dependency object.
     */
    public TemporalDependency setSpecification(String spec) {
        m_specification = spec;
        return this;
    }
    
    /**
     * Sets the dependency filter.
     * @param filter the LDAP filter
     * @return the current dependency object
     */
    public TemporalDependency setFilter(String filter) {
        m_filter = filter;
        return this;
    }
    
    /**
     * Sets the field attached to the dependency.
     * @param field the implementation class field name.
     * @return the current dependency object
     */
    public TemporalDependency setField(String field) {
        m_field = field;
        return this;
    }
    
    /**
     * Sets if the dependency is injected as a proxy.
     * @param proxy <code>true</code> to inject proxies.
     * @return the current dependency object.
     */
    public TemporalDependency setProxy(boolean proxy) {
        m_proxy = proxy;
        return this;
    }
    
    /**
     * Sets the dependency timeout.
     * @param time the dependency timeout in ms
     * 'infinite' for infinite.
     * @return the current dependency object
     */
    public TemporalDependency setTimeout(String time) {
        m_timeout = time;
        return this;
    }
    
    /**
     * Sets the dependency timeout.
     * @param time the dependency timeout in ms
     * @return the current dependency object
     */
    public TemporalDependency setTimeout(long time) {
        m_timeout = new Long(time).toString();
        return this;
    }
    
    /**
     * Sets the dependency ontimeout policy.
     * Supports null, nullable, empty, and default-implementation.
     * In this latter case, you must specify the qualified class name
     * of the default-implementation (instead of default-implementation).
     * Default: no action (i.e throws a runtime exception)
     * @param tip the ontimeout policy
     * @return the current dependency object
     */
    public TemporalDependency setOnTimeoutPolicy(String tip) {
        m_onTimeout = tip;
        return this;
    }
    
   
    /**
     * Checks dependency configuration validity.
     */
    private void ensureValidity() {
        // At least a field or methods.
        if (m_field == null) {
            throw new IllegalStateException("A temporal dependency must have a field");
        }
    }
    
    
}
