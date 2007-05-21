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
 * Export an service from the scope to the parent context.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ServiceExporter implements ServiceListener {

    /**
     * Destination context.
     */
    private BundleContext m_destination;

    /**
     * Origin context.
     */
    private ServiceContext m_origin;

    /**
     * Exported specification.
     */
    private String m_specification;

    /**
     * LDAP filter filtering internal provider.
     */
    private Filter m_filter;

    /**
     * String form of the LDAP filter.
     */
    private String m_filterStr;

    /**
     * Should be exported several providers.
     */
    private boolean m_aggregate = false;

    /**
     * Is this exports optional?
     */
    private boolean m_optional = false;

    /**
     * Reference of the handler.
     */
    private ImportExportHandler m_handler;

    /**
     * Is the exporter valid?
     */
    private boolean m_isValid;

    /**
     * Structure Reference, Registration, Service Object.
     */
    private class Record {
        /**
         * Internal Reference.
         */
        private ServiceReference m_ref;
        /**
         * External Registration.
         */
        private ServiceRegistration m_reg;
        /**
         * Exposed object.
         */
        private Object m_svcObject;
    }

    /**
     * List of managed records.
     */
    private List/*<Record>*/m_records = new ArrayList()/* <Record> */;

    /**
     * Constructor.
     * 
     * @param specification : exported service specification.
     * @param filter : LDAP filter
     * @param multiple : is the export an aggregate export?
     * @param optional : is the export optional?
     * @param from : internal service context
     * @param to : external bundle context
     * @param exp : handler
     */
    public ServiceExporter(String specification, String filter, boolean multiple, boolean optional, ServiceContext from, BundleContext to,
            ImportExportHandler exp) {
        this.m_destination = to;
        this.m_origin = from;
        this.m_handler = exp;
        try {
            this.m_filter = to.createFilter(filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
            return;
        }
        this.m_aggregate = multiple;
        this.m_specification = specification;
        this.m_optional = optional;
    }

    /**
     * Start method.
     * Start the provider traking and the publication.
     */
    public void start() {
        try {
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
     * Transform service reference property in a dictionary.
     * Service.PID and Factory.PID are injected too.
     * @param ref : the service reference.
     * @return the dictionary containing all property of the given service reference.
     */
    private Dictionary getProps(ServiceReference ref) {
        Properties prop = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            prop.put(keys[i], ref.getProperty(keys[i]));
        }

        prop.put(Constants.SERVICE_PID, m_handler.getManager().getInstanceName());
        prop.put("factory.pid", m_handler.getManager().getFactory().getName());

        return prop;
    }

    /**
     * Stop an exporter.
     * Remove the service listener
     * Unregister all exported services.
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
     * Check the exporter validity.
     * @return true if optional or 'valid'
     */
    public boolean isSatisfied() {
        return m_optional || m_records.size() > 0;
    }

    /**
     * Get the list of records using the given reference.
     * @param ref : the service reference
     * @return the list of records using the given reference, empty if no record used this reference
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
                // Test if the ref is always matching with the filter
                List l = getRecordsByRef(ev.getServiceReference());
                if (l.size() > 0) { // The ref is already contained => update
                    // the properties
                    for (int i = 0; i < l.size(); i++) { // Stop the implied
                        // record
                        Record rec = (Record) l.get(i);
                        if (rec.m_reg != null) {
                            rec.m_reg.setProperties(getProps(rec.m_ref));
                        }
                    }
                } else { // it is a new mathcing service => add it
                    arrivalManagement(ev.getServiceReference());
                }
            } else {
                List l = getRecordsByRef(ev.getServiceReference());
                if (l.size() > 0) { // The ref is already contained => the
                    // service does no more match
                    departureManagement(ev.getServiceReference());
                }
            }
        }
    }

    /**
     * Manage the arrival of a service.
     * @param ref : the new service reference.
     */
    private void arrivalManagement(ServiceReference ref) {
        // Check if the new service match
        if (m_filter.match(ref)) {
            // Add it to the record list
            Record rec = new Record();
            rec.m_ref = ref;
            m_records.add(rec);
            // Publishing ?
            if (m_records.size() == 1 || m_aggregate) { // If the service is the
                // first one, or if it
                // is a multiple imports
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
     * Manage the departure of a service.
     * @param ref : the new service reference.
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

        // Check the validity & if we need to reimport the service
        if (m_records.size() > 0) {
            // There is other available services
            if (!m_aggregate) { // Import the next one
                Record rec = (Record) m_records.get(0);
                if (rec.m_svcObject == null) { // It is the first service who
                    // disappears - create the next
                    // one
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

    
    protected String getSpecification() {
        return m_specification;
    }
    
    public String getFilter() {
        return m_filterStr;
    }
}
