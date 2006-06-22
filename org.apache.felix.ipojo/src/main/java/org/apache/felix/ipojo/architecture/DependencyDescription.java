/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.architecture;

import java.util.HashMap;

import org.osgi.framework.ServiceReference;

/**
 * Dependency Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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
     * State (VALID | INVALID).
     */
    private int m_state;

    /**
     * Filter.
     */
    private String m_filter;
    /**
     * Hashmap [Instance reference, service reference] of the used service.
     */
    private HashMap m_usedServices = new HashMap();


    /**
     * The list of service reference.
     */
    private ServiceReference[] m_serviceReferences;

    /**
     * Parent of the dependency either de ProvidedServiceDescription or a ComponentDescription.
     */
    private Object m_parent;


    /**
     * @return true if the dependency is a multiple dependency.
     */
    public boolean isMultiple() {
        return m_multiple;
    }

    /**
     * @return true if the dependency is an optional dependency.
     */
    public boolean isOptional() {
        return m_optional;
    }

    /**
     * @return the filter.
     */
    public String getFilter() {
        return m_filter;
    }

    /**
     * @return the needed interface.
     */
    public String getInterface() {
        return m_interface;
    }

    /**
     * @return the state of the dependency.
     */
    public int getState() {
        return m_state;
    }

    /**
     * Constructor.
     * @param itf : the needed itf
     * @param multiple : is the dependency a multiple dependency ?
     * @param optional : is the depdendency optional ?
     * @param filter : the filter
     * @param state : the state
     * @param parent : the description of the parent (either a ProvidedServiceDescription, either a ComponentDescription)
     */
    public DependencyDescription(String itf, boolean multiple, boolean optional, String filter, int state, Object parent) {
        super();
        m_interface = itf;
        m_multiple = multiple;
        m_optional = optional;
        m_filter = filter;
        m_state = state;
        m_serviceReferences = new ServiceReference[0];
        m_parent = parent;
    }

    /**
     * @return the array of service reference (only if the cardinality could be n).
     */
    public ServiceReference[] getServiceReferences() {
        return m_serviceReferences;
    }

    /**
     * @return the ServiceReference (only if the cardinality could be 1).
     */
    public ServiceReference getServiceReference() {
        return m_serviceReferences[0];
    }

    /**
     * Set the service reference array.
     * @param sr : the array of service reference
     */
    public void setServiceReferences(ServiceReference[] sr) {
        m_serviceReferences = sr;
    }

    /**
     * @return the parent of the dependency
     */
    public Object getParent() {
        return m_parent;
    }

    /**
     * @return the hashmap [object reference, service reference] containing the used services
     */
    public HashMap getUsedServices() { return m_usedServices; }

    /**
     * Set the usedServices.
     * @param hm : the new usedService
     */
    public void setUsedServices(HashMap hm) {
    	m_usedServices = hm;
    }



}
