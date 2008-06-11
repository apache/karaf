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

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.ServiceReference;

/**
 * Dependency Description.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyDescription {

    /**
     * Needed Service Interface.
     */
    private String m_interface;

    /**
     * Multiple ?
     */
    private boolean m_multiple;

    /**
     * Optional ?
     */
    private boolean m_optional;
    
    /**
     * Binding policy.
     */
    private int m_bindingPolicy;
    
    /**
     * Does the dependency use a nullable object?
     */
    private boolean m_useNullable;
    
    /**
     * Does the dependency use a default-implementation? 
     */
    private String m_defaultImplementation;

    /**
     * State (VALID | INVALID).
     */
    private int m_state;

    /**
     * Filter.
     */
    private String m_filter;
    
    /**
     * Is the provider set frozen ?
     */
    private boolean m_isFrozen;
    
    /**
     * Comparator used by the dependency.
     * Null means OSGi default comparator.
     */
    private String m_comparator;

    /**
     * Set[service reference] of the used service.
     */
    private List m_usedServices = new ArrayList();

    /**
     * The list of service reference.
     */
    private List m_serviceReferences;

    /**
     * Constructor.
     * @param itf : the needed interface
     * @param multiple : is the dependency a multiple dependency ?
     * @param optional : is the dependency optional ?
     * @param filter : the filter
     * @param policy : binding policy
     * @param nullable : does the dependency support nullable object
     * @param defaultImpl : does the dependency use a default implementation
     * @param comparator : does the dependency use a special comparator
     * @param frozen : is the provider set frozen
     * @param state : the state
     */
    public DependencyDescription(String itf, boolean multiple, boolean optional, String filter, int policy, boolean nullable, String defaultImpl, String comparator, boolean frozen, int state) {
        super();
        m_interface = itf;
        m_multiple = multiple;
        m_optional = optional;
        m_filter = filter;
        m_state = state;
        m_bindingPolicy = policy;
        m_useNullable = nullable;
        m_defaultImplementation = defaultImpl;
        m_comparator = comparator;
        m_isFrozen = frozen;
    }

    public boolean isMultiple() { return m_multiple; }

    public boolean isOptional() { return m_optional; }

    public String getFilter() { return m_filter; }

    public String getInterface() { return m_interface; }

    public int getState() { return m_state; }
    
    /**
     * Gets true if the dependency uses Nullable objects.
     * @return true if the dependency is optional and supports nullable object.
     */
    public boolean supportsNullable() { return m_useNullable; }
    
    public String getDefaultImplementation() { return m_defaultImplementation; }
    
    public int getPolicy() { return m_bindingPolicy; }
    
    public String getComparator() { return m_comparator; }
    
    public boolean isFrozen() { return m_isFrozen; }

    /**
     * Get the service reference list.
     * @return the array of service reference (only if the cardinality could be n).
     */
    public List getServiceReferences() { return m_serviceReferences; }

    /**
     * Get the service reference if only 1 used.
     * @return the ServiceReference (only if the cardinality could be 1).
     */
    public ServiceReference getServiceReference() { return (ServiceReference) m_serviceReferences.get(0); }

    /**
     * Set the service reference array.
     * @param refs : the list of service reference
     */
    public void setServiceReferences(List refs) { m_serviceReferences = refs; }

    /**
     * Get the used service set.
     * @return the list [service reference] containing the used services
     */
    public List getUsedServices() { return m_usedServices; }

    /**
     * Set the usedServices.
     * @param usages : the list of used service reference.
     */
    public void setUsedServices(List usages) {
        m_usedServices = usages;
    }



}
