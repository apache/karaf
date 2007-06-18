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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Manage a service instantiation. This service create componenet instance
 * providing the required service specification.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SvcInstance implements ServiceListener {

    /**
     * Required specification.
     */
    private String m_specification;

    /**
     * Configuration to push to the instance.
     */
    private Dictionary m_configuration;

    /**
     * Map of factory references => instance or NO_INSTANCE.
     */
    private HashMap /* ServiceReference */m_usedRef = new HashMap();

    /**
     * Does we instantiate several provider ?
     */
    private boolean m_isAggregate = false;

    /**
     * Is the service optional ?
     */
    private boolean m_isOptional = false;

    /**
     * Handler creating the service instance.
     */
    private ServiceInstantiatorHandler m_handler;

    /**
     * Service Context (internal scope).
     */
    private ServiceContext m_context;

    /**
     * True if the service instantiation is valid.
     */
    private boolean m_isValid = false;

    /**
     * String form of the factory filter.
     */
    private String m_filterStr;

    /**
     * Constructor.
     * 
     * @param h : the handler.
     * @param spec : required specification.
     * @param conf : instance configuration.
     * @param isAgg : is the svc instance an aggregate service ?
     * @param isOpt : is the svc instance optional ?
     * @param filt : LDAP filter
     */
    public SvcInstance(ServiceInstantiatorHandler h, String spec, Dictionary conf, boolean isAgg, boolean isOpt, String filt) {
        m_handler = h;
        m_context = h.getManager().getServiceContext();
        m_specification = spec;
        m_configuration = conf;
        m_isAggregate = isAgg;
        m_isOptional = isOpt;
        m_filterStr = filt;
    }

    /**
     * Start the service instance.
     * 
     * @param sc
     */
    public void start() {
        initFactoryList();
        // Register factory listener
        try {
            m_context.addServiceListener(this, m_filterStr);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace(); // Should not happens
        }

        // Init the instances
        if (m_usedRef.size() > 0) {
            Set keys = m_usedRef.keySet();
            Iterator it = keys.iterator();
            if (m_isAggregate) {
                while (it.hasNext()) {
                    ServiceReference ref = (ServiceReference) it.next();
                    createInstance(ref);
                }
            } else {
                ServiceReference ref = (ServiceReference) it.next();
                createInstance(ref);
            }
        }
        m_isValid = isSatisfied();
    }

    /**
     * Stop the service instance.
     */
    public void stop() {
        m_context.removeServiceListener(this);
        Set keys = m_usedRef.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            ServiceReference ref = (ServiceReference) it.next();
            Object o = m_usedRef.get(ref);
            if (o != null) {
                ((ComponentInstance) o).dispose();
            }
        }
        m_usedRef.clear();
        m_isValid = false;
    }

    /**
     * Check if an instance is already created.
     * @return true if at least one instance is created.
     */
    private boolean isAnInstanceCreated() {
        Set keys = m_usedRef.keySet();
        Iterator it = keys.iterator();
        ServiceReference ref = (ServiceReference) it.next();
        Object o = m_usedRef.get(ref);
        return o != null;
    }

    /**
     * Create an instance for the given reference.
     * @param ref : the service reference to used to create the instance.
     */
    private void createInstance(ServiceReference ref) {
        try {
            Factory factory = (Factory) m_context.getService(ref);
            
            //  Add an unique name if not specified.
            Properties p = new Properties();
            Enumeration kk = m_configuration.keys();
            while (kk.hasMoreElements()) {
                String k = (String) kk.nextElement();
                p.put(k, m_configuration.get(k));
            }
            ComponentInstance instance = factory.createComponentInstance(p);
            m_usedRef.put(ref, instance);
            m_context.ungetService(ref);
        } catch (Throwable e) {
            m_handler.getManager().getFactory().getLogger().log(Logger.ERROR,
                    "A matching factory (" + ref.getProperty("service.pid") + ") seems to refuse the given configuration : " + e.getMessage());
        }
    }

    /**
     * Create an instance for the next available factory.
     */
    private void createNextInstance() {
        Set keys = m_usedRef.keySet();
        Iterator it = keys.iterator();
        ServiceReference ref = (ServiceReference) it.next();
        try {
            Factory factory = (Factory) m_context.getService(ref);

            // Add an unique name if not specified.
            Properties p = new Properties();
            Enumeration kk = m_configuration.keys();
            while (kk.hasMoreElements()) {
                String k = (String) kk.nextElement();
                p.put(k, m_configuration.get(k));
            }
            
            ComponentInstance instance = factory.createComponentInstance(p);
            m_usedRef.put(ref, instance);
            m_context.ungetService(ref);
        } catch (UnacceptableConfiguration e) {
            m_handler.getManager().getFactory().getLogger().log(Logger.ERROR,
                    "A matching factory (" + ref.getProperty("service.pid") + ") seems to refuse the given configuration : " + e.getMessage());
        }
    }

    /**
     * Kill an instance (if exist).
     * If an instance if created by using the given reference, this reference is disposed.
     * @param ref : the leaving reference. 
     */
    private void stopInstance(ServiceReference ref) {
        Object o = m_usedRef.get(ref);
        if (o != null) {
            ((ComponentInstance) o).dispose();
        }
    }

    /**
     * Init the list of available factory.
     */
    public void initFactoryList() {
        // Init factory list
        try {
            ServiceReference[] refs = m_context.getServiceReferences(Factory.class.getName(), m_filterStr);
            if (refs == null) {
                return;
            }
            for (int i = 0; i < refs.length; i++) {
                ServiceReference ref = refs[i];
                Factory fact = (Factory) m_context.getService(ref);
                // Check provided spec & conf
                if (match(fact)) {
                    m_usedRef.put(ref, null);
                }
                fact = null;
                m_context.ungetService(ref);
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace(); // Should not happen
        }
    }

    /**
     * Check if the service isntance is statisfed.
     * @return true if the service instance if satisfied.
     */
    public boolean isSatisfied() {
        return m_isOptional || m_usedRef.size() > 0;
    }

    /**
     * Does the service instance match with the given factory.
     * 
     * @param fact : the factory to test.
     * @return true if the factory match, false otherwise.
     */
    private boolean match(Factory fact) {
        // Check if the factory can provide the spec
        for (int i = 0; i < fact.getComponentDescription().getprovidedServiceSpecification().length; i++) {
            if (fact.getComponentDescription().getprovidedServiceSpecification()[i].equals(m_specification)) {

                // Check that the factory needs every properties contained in
                // the configuration
                Enumeration e = m_configuration.keys();
                while (e.hasMoreElements()) {
                    String k = (String) e.nextElement();
                    if (!containsProperty(k, fact)) {
                        return false;
                    }
                }

                // Add an unique name if not specified.
                Properties p = new Properties();
                Enumeration keys = m_configuration.keys();
                while (keys.hasMoreElements()) {
                    String k = (String) keys.nextElement();
                    p.put(k, m_configuration.get(k));
                }

                // Check the acceptability.
                return fact.isAcceptable(p);
            }
        }
        return false;
    }

    /**
     * Does the factory support the given property ?
     * 
     * @param name : name of the property
     * @param factory : factory to test
     * @return true if the factory support this property
     */
    private boolean containsProperty(String name, Factory factory) {
        PropertyDescription[] props = factory.getComponentDescription().getProperties();
        for (int i = 0; i < props.length; i++) {
            if (props[i].getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        if (name.equalsIgnoreCase("name")) {
            return true;
        } // Skip the name property
        return false;
    }

    /**
     * Service Listener Implementation.
     * @param ev : the service event
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent ev) {
        if (ev.getType() == ServiceEvent.REGISTERED) {
            // Check the matching
            Factory fact = (Factory) m_context.getService(ev.getServiceReference());
            if (match(fact)) {
                m_usedRef.put(ev.getServiceReference(), null);
                if (m_isAggregate) { // Create an instance for the new
                    // factory
                    createInstance(ev.getServiceReference());
                    if (!m_isValid) {
                        m_isValid = true;
                        m_handler.validate();
                    }
                } else {
                    if (!isAnInstanceCreated()) {
                        createInstance(ev.getServiceReference());
                    }
                    if (!m_isValid) {
                        m_isValid = true;
                        m_handler.validate();
                    }
                }
            }
            fact = null;
            m_context.ungetService(ev.getServiceReference());
            return;
        }
        if (ev.getType() == ServiceEvent.UNREGISTERING) {
            // Remove the ref is contained
            Object o = m_usedRef.remove(ev.getServiceReference());
            if (o != null) {
                stopInstance(ev.getServiceReference());
                if (m_usedRef.size() > 0) {
                    if (!m_isAggregate) {
                        createNextInstance(); // Create an instance with
                        // another factory
                    }
                } else { // No more candidate
                    if (!m_isOptional) {
                        m_isValid = false;
                        m_handler.invalidate();
                    }
                }
            }
        }
    }

    /**
     * Get the required specification.
     * @return the required specification.
     */
    public String getSpecification() {
        return m_specification;
    }

    /**
     * Get the map of used references [ref, component instance].
     * @return the map of used references.
     */
    protected HashMap getUsedReferences() {
        return m_usedRef;
    }

}
