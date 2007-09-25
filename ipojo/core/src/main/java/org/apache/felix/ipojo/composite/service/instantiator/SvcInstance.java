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
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Manage a service instantiation. This service create component instance
 * providing the required service specification.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SvcInstance implements TrackerCustomizer {

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
    private Map /* ServiceReference */m_usedRef = new HashMap();

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
     * Tracker used to track required factory.
     */
    private Tracker m_tracker;

    /**
     * Constructor.
     * @param h : the handler.
     * @param spec : required specification.
     * @param conf : instance configuration.
     * @param isAgg : is the service instance an aggregate service ?
     * @param isOpt : is the service instance optional ?
     * @param filt : LDAP filter
     */
    public SvcInstance(ServiceInstantiatorHandler h, String spec, Dictionary conf, boolean isAgg, boolean isOpt, String filt) {
        m_handler = h;
        m_context = h.getCompositeManager().getServiceContext();
        m_specification = spec;
        m_configuration = conf;
        m_isAggregate = isAgg;
        m_isOptional = isOpt;
        try {
            m_tracker  = new Tracker(m_context, h.getCompositeManager().getContext().createFilter(filt), this);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the service instance.
     * @param sc
     */
    public void start() {
        m_tracker.open();
        m_isValid = isSatisfied();
    }

    /**
     * Stop the service instance.
     */
    public void stop() {
        m_tracker.close();
        
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
        m_tracker = null;
        m_isValid = false;
    }

    /**
     * Check if an instance is already created.
     * @return true if at least one instance is created.
     */
    private boolean isAnInstanceCreated() {
        Set keys = m_usedRef.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            if (m_usedRef.get(it.next()) != null)  {
                return true;
            }
        }
        return false;
    }

    /**
     * Create an instance for the given reference.
     * The instance is not added inside the map.
     * @param factory : the factory from which we need to create the instance.
     * @return the created component instance.
     */
    private ComponentInstance createInstance(Factory factory) {
        // Add an unique name if not specified.
        Properties p = new Properties();
        Enumeration kk = m_configuration.keys();
        while (kk.hasMoreElements()) {
            String k = (String) kk.nextElement();
            p.put(k, m_configuration.get(k));
        }
        ComponentInstance instance = null;
        try {
            instance = factory.createComponentInstance(p);
        } catch (UnacceptableConfiguration e) {
            e.printStackTrace();
        } catch (MissingHandlerException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        return instance;
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
            m_handler.log(Logger.ERROR, "A matching factory (" + ref.getProperty("instance.name") + ") seems to refuse the given configuration : " + e.getMessage());
        } catch (MissingHandlerException e) {
            m_handler.log(Logger.ERROR, "A matching factory (" + ref.getProperty("instance.name") + ") seems to refuse the given configuration : " + e.getMessage());
        } catch (ConfigurationException e) {
            m_handler.log(Logger.ERROR, "A matching factory (" + ref.getProperty("instance.name") + ") seems to refuse the given configuration : " + e.getMessage());
        }
    }



    /**
     * Check if the service instance is satisfied.
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
        // Check if the factory can provide the specification
        Element[] provides = fact.getDescription().getElements("provides");
        for (int i = 0; i < provides.length; i++) {
            if (provides[i].getAttribute("specification").equals(m_specification)) {

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
        Element[] props = factory.getDescription().getElements("property");
        for (int i = 0; i < props.length; i++) {
            if (props[i].getAttribute("name").equalsIgnoreCase(name)) {
                return true;
            }
        }
        if (name.equalsIgnoreCase("name")) {
            return true;
        } // Skip the name property
        return false;
    }

    /**
     * Get the required specification.
     * @return the required specification.
     */
    public String getSpecification() {
        return m_specification;
    }
    
    public boolean isAggregate() {
        return m_isAggregate;
    }
    
    public boolean isOptional() {
        return m_isOptional;
    }

    /**
     * Get the map of used references [reference, component instance].
     * @return the map of used references.
     */
    protected Map getUsedReferences() {
        return m_usedRef;
    }

    /**
     * A factory potentially matching with the managed instance appears.
     * @param reference : service reference
     * @return : true if the factory match
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference reference) {
        Factory fact = (Factory) m_tracker.getService(reference);
        if (match(fact)) {
            if (m_isAggregate) { // Create an instance for the new factory
                m_usedRef.put(reference, createInstance(fact));
                if (!m_isValid) {
                    m_isValid = true;
                    m_handler.validate();
                }
            } else {
                if (!isAnInstanceCreated()) {
                    m_usedRef.put(reference, createInstance(fact));
                } else {
                    m_usedRef.put(reference, null); // Store the reference
                }
                if (!m_isValid) {
                    m_isValid = true;
                    m_handler.validate();
                }
            }
            m_tracker.ungetService(reference);
            return true;
        } else {
            m_tracker.ungetService(reference);
            return false;
        }
        
    }

    /**
     * A used factory was modified.
     * @param reference : service reference
     * @param service : object if already get
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) { } 
        
    /**
     * A used factory disappears.
     * @param reference : service reference
     * @param service : object if already get
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
     // Remove the reference is contained
        Object o = m_usedRef.remove(reference);
        if (o != null) {
            ((ComponentInstance) o).dispose();
            if (m_usedRef.size() > 0) {
                if (!m_isAggregate) {
                    createNextInstance(); // Create an instance with another factory
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
