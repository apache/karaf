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
package org.apache.felix.ipojo.composite.service.importer;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Import a service form the parent to the internal service registry.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImporter implements TrackerCustomizer {

    /**
     * Destination context.
     */
    private ServiceContext m_destination;

    /**
     * Context where service need to be found. 
     */
    private ServiceContext m_origin;

    /**
     * Imported Specification.
     */
    private String m_specification;

    /**
     * LDAP filter filtering external providers.
     */
    private Filter m_filter;

    /**
     * String form of the LDAP filter.
     */
    private String m_filterStr;

    /**
     * Should we importer several providers?
     */
    private boolean m_aggregate = false;

    /**
     * Is the import optional?
     */
    private boolean m_optional = false;

    /**
     * Is the importer valid?
     */
    private boolean m_isValid;
    
    /**
     * Resolving policy.
     */
    private int m_policy;
    
    /**
     * TRacker tracking imported service.
     */
    private Tracker m_tracker;

    /**
     * Reference on the handler.
     */
    private ImportHandler m_handler;

    private class Record {
        /**
         * External Reference.
         */
        private ServiceReference m_ref;
        /**
         * Internal Registration.
         */
        private ServiceRegistration m_reg;
        /**
         * Exposed Object.
         */
        private Object m_svcObject;
        
        /**
         * Test object equality.
         * @param o : object to confront against the current object.
         * @return true if the two objects are equals (same service reference).
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object o) {
            if (o instanceof Record) {
                Record rec = (Record) o;
                return rec.m_ref == m_ref;
            }
            return false;
        }
    }

    /**
     * List of managed records.
     */
    private List/*<Record>*/m_records = new ArrayList()/* <Record> */;

    /**
     * Requirement Id.
     */
    private String m_id;

    /**
     * Is this requirement attached to a service-level requirement.
     */
    private boolean m_isServiceLevelRequirement;

    /**
     * Constructor.
     * 
     * @param specification : targeted specification
     * @param filter : LDAP filter
     * @param multiple : should the importer imports several services ?
     * @param optional : is the import optional ?
     * @param from : parent context
     * @param to : internal context
     * @param policy : resolving policy
     * @param id : requirement id (may be null)
     * @param in : handler
     */
    public ServiceImporter(String specification, String filter, boolean multiple, boolean optional, BundleContext from, ServiceContext to, int policy, String id,
            ImportHandler in) {
        this.m_destination = to;
        try {
            this.m_filter = from.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
            return;
        }
        this.m_aggregate = multiple;
        this.m_specification = specification;
        this.m_optional = optional;
        this.m_handler = in;
        
        if (m_id == null) {
            m_id = m_specification;
        } else {
            m_id = id;
        }
        
        if (policy == -1) {
            m_policy = PolicyServiceContext.LOCAL_AND_GLOBAL;  
        } else {
            m_policy = policy;
        }
    }

    /**
     * Start method to begin the import.
     */
    public void start() {
        m_origin = new PolicyServiceContext(m_handler.getCompositeManager().getGlobalContext(), m_handler.getCompositeManager().getParentServiceContext(), m_policy);
        m_tracker = new Tracker(m_origin, m_filter, this);
        m_tracker.open();
        m_isValid = isSatisfied();
    }

    /**
     * Get the properties for the exposed service from the given reference.
     * 
     * @param ref : the reference.
     * @return the property dictionary
     */
    private Dictionary getProps(ServiceReference ref) {
        Properties prop = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            prop.put(keys[i], ref.getProperty(keys[i]));
        }
        return prop;
    }

    /**
     * Stop the management of the import.
     */
    public void stop() {

        m_tracker.close();

        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            rec.m_svcObject = null;
            if (rec.m_reg != null) {
                rec.m_reg.unregister();
                m_tracker.ungetService(rec.m_ref);
                rec.m_ref = null;
            }
        }
        
        m_tracker = null;
        m_records.clear();

    }

    /**
     * Check if the import is satisfied.
     * @return true if the import is optional or at least one provider is imported
     */
    public boolean isSatisfied() {
        return m_optional || m_records.size() > 0;
    }

    /**
     * Get the record list using the given reference.
     * 
     * @param ref : the reference
     * @return the list containing all record using the given reference
     */
    private List/* <Record> */getRecordsByRef(ServiceReference ref) {
        List l = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            if (rec.m_ref == ref) {
                l.add(rec);
            }
        }
        return l;
    }

    public String getSpecification() {
        return m_specification;
    }

    /**
     * Build the list of imported service provider.
     * @return the list of all imported services.
     */
    protected List getProviders() {
        List l = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            l.add((((Record) m_records.get(i)).m_ref).getProperty("instance.name"));
        }
        return l;

    }

    public String getFilter() {
        return m_filterStr;
    }
    
    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT. 
     * @param b
     */
    public void setServiceLevelDependency() {
        m_isServiceLevelRequirement = true;
        m_policy = PolicyServiceContext.LOCAL;
    }

    public String getId() {
        return m_id;
    }
    
    public boolean isServiceLevelRequirement() {
        return m_isServiceLevelRequirement;
    }
    
    public boolean isAggregate() {
        return m_aggregate;
    }
    
    public boolean isOptional() {
        return m_optional;
    }

    /**
     * A new service is detected.
     * @param reference : service reference
     * @return true if not already imported.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference reference) {
        // Else add it to the record list
        Record rec = new Record();
        rec.m_ref = reference;
        if (m_records.contains(rec)) {
            return false;
        }
        
        m_records.add(rec);
        // Publishing ?
        if (m_records.size() == 1 || m_aggregate) { // If the service is the first one, or if it is a multiple imports
            rec.m_svcObject = m_tracker.getService(rec.m_ref);
            rec.m_reg = m_destination.registerService(m_specification, rec.m_svcObject, getProps(rec.m_ref));
        }
        // Compute the new state
        if (!m_isValid && isSatisfied()) {
            m_isValid = true;
            m_handler.validating(this);
        }
        return true;
    }

    /**
     * An imported service was modified.
     * @param reference : service reference
     * @param service : service object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) {
        List l = getRecordsByRef(reference);
        for (int i = 0; i < l.size(); i++) { // Stop the implied record
            Record rec = (Record) l.get(i);
            if (rec.m_reg != null) {
                rec.m_reg.setProperties(getProps(rec.m_ref));
            }
        }
    }

    /**
     * An imported service disappears.
     *@param reference : service reference
     * @param service : service object (if already get)
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
        List l = getRecordsByRef(reference);
        for (int i = 0; i < l.size(); i++) { // Stop the implied record
            Record rec = (Record) l.get(i);
            if (rec.m_reg != null) {
                rec.m_svcObject = null;
                rec.m_reg.unregister();
                rec.m_reg = null;
                m_tracker.ungetService(rec.m_ref);
            }
        }
        m_records.removeAll(l);

        // Check the validity & if we need to re-import the service
        if (m_records.size() > 0) {
            // There is other available services
            if (!m_aggregate) { // Import the next one
                Record rec = (Record) m_records.get(0);
                if (rec.m_svcObject == null) { // It is the first service which disappears - create the next one
                    rec.m_svcObject = m_tracker.getService(rec.m_ref);
                    rec.m_reg = m_destination.registerService(m_specification, rec.m_svcObject, getProps(rec.m_ref));
                }
            }
        } else {
            if (!m_optional) {
                m_isValid = false;
                m_handler.invalidating(this);
            }
        }
    }

}
