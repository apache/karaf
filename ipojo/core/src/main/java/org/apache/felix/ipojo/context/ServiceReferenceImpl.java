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
package org.apache.felix.ipojo.context;

import java.util.Dictionary;

import org.apache.felix.ipojo.ComponentInstance;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * Internal service reference implementation. This class is used for in the
 * composition.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceReferenceImpl implements ServiceReference {

    /**
     * Service Registration attached to the service reference.
     */
    private ServiceRegistrationImpl m_registration = null;
    
    /**
     * Component Instance.
     */
    private ComponentInstance m_cm;

    /**
     * Constructor.
     * 
     * @param instance : component instance publishing the service.
     * @param ref : registration attached to this service reference.
     */
    public ServiceReferenceImpl(ComponentInstance instance, ServiceRegistrationImpl ref) {
        m_registration = ref;
        m_cm = instance;
    }

    /**
     * Not supported in composite.
     * @return null
     * @see org.osgi.framework.ServiceReference#getBundle()
     */
    public Bundle getBundle() {
        return m_cm.getContext().getBundle();
    }

    /**
     * Get the service registration for this reference.
     * @return the service registration for this service reference.
     */
    public ServiceRegistrationImpl getServiceRegistration() {
        return m_registration;
    }


    /**
     * Get a property value.
     * @param name : the key of the required property.
     * @return the property value or null if no property for the given key.
     * @see org.osgi.framework.ServiceReference#getProperty(java.lang.String)
     */
    public Object getProperty(String name) {
        return m_registration.getProperty(name);
    }

    /**
     * Get the String arrays of service property keys.
     * @return : the list of property keys.
     * @see org.osgi.framework.ServiceReference#getPropertyKeys()
     */
    public String[] getPropertyKeys() {
        return m_registration.getPropertyKeys();
    }
    
    public Dictionary getProperties() {
        return m_registration.getProperties();
    }


    /**
     * Unsupported Operation inside composite.
     * @return bundles using this reference.
     * @see org.osgi.framework.ServiceReference#getUsingBundles()
     */
    public Bundle[] getUsingBundles() {
        throw new UnsupportedOperationException("getUsingBundles is not supported in service context");
    }

    /**
     * Check if the current service reference is assignable to the given bundle.
     * @param arg0 : the bundle to check
     * @param arg1 : the class name to check.
     * @return true in the case of composite
     * @see org.osgi.framework.ServiceReference#isAssignableTo(org.osgi.framework.Bundle, java.lang.String)
     */
    public boolean isAssignableTo(Bundle arg0, String arg1) {
        return true;
    }

    /**
     * Service Reference compare method.
     * @param reference the service reference
     * @return this methods is not yet supported, and throws an
     *         {@link UnsupportedOperationException}.
     * @see org.osgi.framework.ServiceReference#compareTo(java.lang.Object)
     */
    public int compareTo(Object reference) {

        ServiceReference other = (ServiceReference) reference;

        Long id = (Long) getProperty(Constants.SERVICE_ID);
        Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

        if (id.equals(otherId)) {
            return 0; // same service
        }

        Integer rank = (Integer) getProperty(Constants.SERVICE_RANKING);
        Integer otherRank = (Integer) other
                .getProperty(Constants.SERVICE_RANKING);

        // If no rank, then spec says it defaults to zero.
        rank = (rank == null) ? new Integer(0) : rank;
        otherRank = (otherRank == null) ? new Integer(0) : otherRank;

        // Sort by rank in ascending order.
        if (rank.compareTo(otherRank) < 0) {
            return -1; // lower rank
        } else if (rank.compareTo(otherRank) > 0) {
            return 1; // higher rank
        }

        // If ranks are equal, then sort by service id in descending order.
        return (id.compareTo(otherId) < 0) ? 1 : -1;
    }

}
