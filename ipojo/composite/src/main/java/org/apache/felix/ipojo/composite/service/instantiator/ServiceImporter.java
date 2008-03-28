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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Import a service form the parent to the internal service registry.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceImporter extends DependencyModel {

    /**
     * Reference on the handler.
     */
    private ServiceDependencyHandler m_handler;

    private final class Record {
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
         * Constructor.
         * @param ref : service reference.
         */
        protected Record(ServiceReference ref) {
            m_ref = ref;
        }

        /**
         * Register the current import.
         */
        private void register() {
            if (m_reg != null) {
                m_reg.unregister();
            }
            m_svcObject = getService(m_ref);
            m_reg = m_handler.getCompositeManager().getServiceContext().registerService(getSpecification().getName(), m_svcObject, getProps(m_ref));
        }

        /**
         * Update the current import.
         */
        private void update() {
            if (m_reg != null) {
                m_reg.setProperties(getProps(m_ref));
            }
        }

        /**
         * Unregister and release the current import.
         */
        private void dispose() {
            if (m_reg != null) {
                m_reg.unregister();
                m_svcObject = null;
                m_reg = null;
            }
            m_ref = null;
        }

        /**
         * Test object equality.
         * @param object : object to confront against the current object.
         * @return true if the two objects are equals (same service reference).
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object object) {
            if (object instanceof Record) {
                Record rec = (Record) object;
                return rec.m_ref == m_ref;
            }
            return false;
        }
        
        /**
         * Hash code method.
         * @return the hash code by calling the parent method.
         */
        public int hashCode() {
            return super.hashCode();
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
    private boolean m_specLevelReq;

    /**
     * Is the set of used provider frozen ?
     */
    private boolean m_isFrozen;

    /**
     * Constructor.
     * 
     * @param specification : targeted specification
     * @param filter : LDAP filter
     * @param multiple : should the importer imports several services ?
     * @param optional : is the import optional ?
     * @param cmp : comparator to use for the tracking 
     * @param policy : resolving policy
     * @param context : bundle context to use for the tracking (can be a servie context)
     * @param identitity : requirement id (may be null)
     * @param handler : handler
     */
    public ServiceImporter(Class specification, Filter filter, boolean multiple, boolean optional, Comparator cmp, int policy, BundleContext context, String identitity
            , ServiceDependencyHandler handler) {
        super(specification, multiple, optional, filter, cmp, policy, context, handler);

        this.m_handler = handler;

        if (m_id == null) {
            m_id = super.getSpecification().getName();
        } else {
            m_id = identitity;
        }

    }

    /**
     * Get the properties for the exposed service from the given reference.
     * 
     * @param ref : the reference.
     * @return the property dictionary
     */
    private static Dictionary getProps(ServiceReference ref) {
        Properties prop = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            prop.put(keys[i], ref.getProperty(keys[i]));
        }
        return prop;
    }

    /**
     * Freeze the set of used provider.
     * This method allow to fix the set of provider when the static binding policy is used.
     */
    public void freeze() {
        m_isFrozen = true;
    }

    public boolean isFrozen() {
        return m_isFrozen;
    }

    /**
     * Stop the management of the import.
     */
    public void stop() {

        super.stop();

        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            rec.dispose();
        }

        m_records.clear();

    }

    /**
     * Get the record list using the given reference.
     * 
     * @param ref : the reference
     * @return the list containing all record using the given reference
     */
    private List/* <Record> */getRecordsByRef(ServiceReference ref) {
        List list = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            Record rec = (Record) m_records.get(i);
            if (rec.m_ref == ref) {
                list.add(rec);
            }
        }
        return list;
    }

    /**
     * Build the list of imported service provider.
     * @return the list of all imported services.
     */
    public List getProviders() {
        List list = new ArrayList();
        for (int i = 0; i < m_records.size(); i++) {
            list.add((((Record) m_records.get(i)).m_ref).getProperty("instance.name"));
        }
        return list;
    }

    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT. 
     * @param b
     */
    public void setServiceLevelDependency() {
        m_specLevelReq = true;
        PolicyServiceContext context = new PolicyServiceContext(m_handler.getCompositeManager().getGlobalContext(), m_handler.getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL);
        setBundleContext(context);
    }

    public String getId() {
        return m_id;
    }

    public boolean isServiceLevelRequirement() {
        return m_specLevelReq;
    }

    /**
     * On Dependency Reconfiguration notification method.
     * @param departs : leaving service references.
     * @param arrivals : new injected service references.
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        for (int i = 0; departs != null && i < departs.length; i++) {
            onServiceDeparture(departs[i]);
        }
        
        for (int i = 0; arrivals != null && i < arrivals.length; i++) {
            onServiceArrival(arrivals[i]);
        }
    }

    /**
     * A new service is injected by the tracker.
     * This method create a 'Record' and register it.
     * @param ref : new service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference ref) {
        Record rec = new Record(ref);
        m_records.add(rec);
        // Always register the reference, as the method is call only when needed. 
        rec.register();
    }

    /**
     * A used service disappears.
     * This method find the implicated 'Record', dispose it and remove it from the list.
     * @param ref : leaving service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public void onServiceDeparture(ServiceReference ref) {
        List list = getRecordsByRef(ref);
        for (int i = 0; i < list.size(); i++) { // Stop the implied record
            Record rec = (Record) list.get(i);
            rec.dispose();
        }
        m_records.removeAll(list);
    }

    /**
     * A used service is modified.
     * @param ref : modified service reference.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceModification(org.osgi.framework.ServiceReference)
     */
    public void onServiceModification(ServiceReference ref) {
        List list = getRecordsByRef(ref);
        for (int i = 0; i < list.size(); i++) { // Stop the implied record
            Record rec = (Record) list.get(i);
            rec.update();
        }
    }

}
