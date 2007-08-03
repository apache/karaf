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
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Import a service form the parent to the internal service registry.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImporter implements ServiceListener {

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
     * Reference on the handler.
     */
    private ImportExportHandler m_handler;

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
            ImportExportHandler in) {
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
        try {
            m_origin = new PolicyServiceContext(m_handler.getManager().getGlobalContext(), m_handler.getManager().getParentServiceContext(), m_policy);
            ServiceReference[] refs = m_origin.getServiceReferences(m_specification, null);
            if (refs != null) {
                for (int i = 0; i < refs.length; i++) {
                    if (m_filter.match(refs[i])) {
                        Record rec = new Record();
                        rec.m_ref = refs[i];
                        m_records.add(rec);
                    }
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

        // Publish available services
        if (m_records.size() > 0) {
            if (m_aggregate) {
                for (int i = 0; i < m_records.size(); i++) {
                    Record rec = (Record) m_records.get(i);
                    rec.m_svcObject = m_origin.getService(rec.m_ref);
                    rec.m_reg = m_destination.registerService(m_specification, rec.m_svcObject, getProps(rec.m_ref));
                }
            } else {
                Record rec = (Record) m_records.get(0);
                rec.m_svcObject = m_origin.getService(rec.m_ref);
                rec.m_reg = m_destination.registerService(m_specification, rec.m_svcObject, getProps(rec.m_ref));
            }
        }

        // Register service listener
        try {
            m_origin.addServiceListener(this, "(" + Constants.OBJECTCLASS + "=" + m_specification + ")");
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

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

        m_origin.removeServiceListener(this);

        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            rec.m_svcObject = null;
            if (rec.m_reg != null) {
                rec.m_reg.unregister();
                m_origin.ungetService(rec.m_ref);
                rec.m_ref = null;
            }
        }

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

    /**
     * Service Listener Implementation.
     * @param ev : the service event
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent ev) {
        if (ev.getType() == ServiceEvent.REGISTERED) {
            arrivalManagement(ev.getServiceReference());
        }
        if (ev.getType() == ServiceEvent.UNREGISTERING) {
            departureManagement(ev.getServiceReference());
        }

        if (ev.getType() == ServiceEvent.MODIFIED) {
            if (m_filter.match(ev.getServiceReference())) {
                // Test if the reference is always matching with the filter
                List l = getRecordsByRef(ev.getServiceReference());
                if (l.size() > 0) { // The reference is already contained => update the properties
                    for (int i = 0; i < l.size(); i++) { // Stop the implied record
                        Record rec = (Record) l.get(i);
                        if (rec.m_reg != null) {
                            rec.m_reg.setProperties(getProps(rec.m_ref));
                        }
                    }
                } else { // it is a new matching service => add it
                    arrivalManagement(ev.getServiceReference());
                }
            } else {
                List l = getRecordsByRef(ev.getServiceReference());
                if (l.size() > 0) { // The reference is already contained => the service does no more match
                    departureManagement(ev.getServiceReference());
                }
            }
        }
    }

    /**
     * Manage the arrival of a consistent service.
     * @param ref : the arrival service reference
     */
    private void arrivalManagement(ServiceReference ref) {
        // Check if the new service match
        if (m_filter.match(ref)) {
            // Add it to the record list
            Record rec = new Record();
            rec.m_ref = ref;
            m_records.add(rec);
            // Publishing ?
            if (m_records.size() == 1 || m_aggregate) { // If the service is the first one, or if it is a multiple imports
                rec.m_svcObject = m_origin.getService(rec.m_ref);
                rec.m_reg = m_destination.registerService(m_specification, rec.m_svcObject, getProps(rec.m_ref));
            }
            // Compute the new state
            if (!m_isValid && isSatisfied()) {
                m_isValid = true;
                m_handler.validating(this);
            }
        }
    }

    /**
     * Manage the departure of a used reference.
     * 
     * @param ref : the leaving reference
     */
    private void departureManagement(ServiceReference ref) {
        List l = getRecordsByRef(ref);
        for (int i = 0; i < l.size(); i++) { // Stop the implied record
            Record rec = (Record) l.get(i);
            if (rec.m_reg != null) {
                rec.m_svcObject = null;
                rec.m_reg.unregister();
                rec.m_reg = null;
                m_origin.ungetService(rec.m_ref);
            }
        }
        m_records.removeAll(l);

        // Check the validity & if we need to re-import the service
        if (m_records.size() > 0) {
            // There is other available services
            if (!m_aggregate) { // Import the next one
                Record rec = (Record) m_records.get(0);
                if (rec.m_svcObject == null) { // It is the first service which disappears - create the next one
                    rec.m_svcObject = m_origin.getService(rec.m_ref);
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
            l.add((((Record) m_records.get(i)).m_ref).getProperty(Constants.SERVICE_PID));
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

}
