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
package org.apache.felix.ipojo.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.ServiceContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * CompositeServiceContext Class. This class provides an implementation of the
 * service context for composite.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeServiceContext implements ServiceContext, ServiceListener {

    /**
     * Structure stoing the referense, the factory and the registration.
     */
    private class Record {
        /**
         * Reference of the proxed factory from the external context.
         */
        private ServiceReference m_ref;
        /**
         * Registration of the factory in the internal context.
         */
        private ServiceRegistration m_reg;
        /**
         * Proxed Factory. 
         */
        private FactoryProxy m_fact;
    }

    /**
     * List of imported factories.
     */
    private List m_factories = new ArrayList();

    /**
     * External context.
     */
    private BundleContext m_parent;

    /**
     * Internal service registry.
     */
    private ServiceRegistry m_registry;

    /**
     * Component Instance who creates this registry.
     */
    private ComponentInstance m_instance;

    /**
     * Constructor. This constructor instantiate a service registry with the
     * given bundle context.
     * 
     * @param bc : the bundle context
     */
    public CompositeServiceContext(BundleContext bc) {
        m_registry = new ServiceRegistry(bc);
        m_parent = bc;
    }

    /**
     * Constructor.
     * 
     * @param bc : the bundle context
     * @param ci : the component instance owning this context
     */
    public CompositeServiceContext(BundleContext bc, ComponentInstance ci) {
        this(bc);
        m_parent = bc;
        m_instance = ci;
    }

    /**
     * Add a service listener.
     * @param arg0 : The service listener to add
     * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    public void addServiceListener(ServiceListener arg0) {
        m_registry.addServiceListener(arg0);
    }

    /**
     * Add a filtered service listener.
     * @param arg0 : the service listner object to add
     * @param arg1 : the LDAP filter for this listener
     * @throws InvalidSyntaxException : throwed if the LDAP filter is malformed
     * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener,
     * java.lang.String)
     */
    public void addServiceListener(ServiceListener arg0, String arg1) throws InvalidSyntaxException {
        m_registry.addServiceListener(arg0, arg1);
    }

    /**
     * Get all service references.
     * @param arg0 : The requried service interface.
     * @param arg1 : LDAP filter
     * @return the list of all service reference matching with the query
     * @throws InvalidSyntaxException : occurs when the given filter is malformed
     * @see org.apache.felix.ipojo.ServiceContext#getAllServiceReferences(java.lang.String,
     * java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences(String arg0, String arg1) throws InvalidSyntaxException {
        return m_registry.getAllServiceReferences(arg0, arg1);
    }

    /**
     * Get a service object for the given service reference.
     * @param arg0 : the service reference
     * @return the service object or null if the reference is no more valid or if the object is not accessible
     * @see org.apache.felix.ipojo.ServiceContext#getService(org.osgi.framework.ServiceReference)
     */
    public Object getService(ServiceReference arg0) {
        return m_registry.getService(m_instance, arg0);
    }

    
    /**
     * Get a service reference for the required interface.
     * @param arg0 : the requried interface name
     * @return the service reference or null if no available provider
     * @see org.apache.felix.ipojo.ServiceContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference(String arg0) {
        return m_registry.getServiceReference(arg0);
    }

    /**
     * Get all accessible service reference for the given query.
     * @param clazz : required interface
     * @param filter : LDAP filter
     * @return the list (array) of service reference matching with the query.
     * @throws InvalidSyntaxException : occurs when the LDAP filter is malformed
     * @see org.apache.felix.ipojo.ServiceContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        return m_registry.getServiceReferences(clazz, filter);
    }


    /**
     * Register a service inside the composite context.
     * @param arg0 : list of interfaces to register.
     * @param arg1 : service object
     * @param arg2 : properties list
     * @return the service registration
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String[] arg0, Object arg1, Dictionary arg2) {
        return m_registry.registerService(m_instance, arg0, arg1, arg2);
    }

    /**
     * Register a service inside the composite context.
     * @param arg0 : interface to register.
     * @param arg1 : service object
     * @param arg2 : properties list
     * @return the service registration
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String arg0, Object arg1, Dictionary arg2) {
        return m_registry.registerService(m_instance, arg0, arg1, arg2);
    }

    /**
     * Remove a service listener.
     * @param arg0 : the service listener to remove
     * @see org.apache.felix.ipojo.ServiceContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener(ServiceListener arg0) {
        m_registry.removeServiceListener(arg0);
    }

    /**
     * Unget a service.
     * @param arg0 the service reference to unget
     * @return true
     * @see org.apache.felix.ipojo.ServiceContext#ungetService(org.osgi.framework.ServiceReference)
     */
    public boolean ungetService(ServiceReference arg0) {
        return m_registry.ungetService(m_instance, arg0);
    }

    /**
     * Initiate the factory list.
     */
    private void importFactories() {
        try {
            ServiceReference[] refs = m_parent.getServiceReferences(Factory.class.getName(), null);
            if (refs != null) {
                for (int i = 0; i < refs.length; i++) {
                    importFactory(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace(); // Should not happen
        }
    }

    /**
     * Import a factory form the parent to the internal registry.
     * 
     * @param ref : the reference of the factory to import.
     */
    private void importFactory(ServiceReference ref) {
        Record rec = new Record();
        m_factories.add(rec);
        Dictionary dict = new Properties();
        for (int j = 0; j < ref.getPropertyKeys().length; j++) {
            dict.put(ref.getPropertyKeys()[j], ref.getProperty(ref.getPropertyKeys()[j]));
        }
        rec.m_fact = new FactoryProxy((Factory) m_parent.getService(ref), this);
        rec.m_reg = registerService(Factory.class.getName(), rec.m_fact, dict);
        rec.m_ref = ref;
    }

    /**
     * Remove a factory of the available factory list.
     * 
     * @param ref : the reference on the factory to remove.
     */
    private void removeFactory(ServiceReference ref) {
        for (int i = 0; i < m_factories.size(); i++) {
            Record rec = (Record) m_factories.get(i);
            if (rec.m_ref == ref) {
                rec.m_reg.unregister();
                rec.m_fact = null;
                m_parent.ungetService(rec.m_ref);
                m_factories.remove(rec);
                return;
            }
        }
    }

    /**
     * Start the registry management.
     */
    public void start() {
        importFactories();
        try {
            m_parent.addServiceListener(this, "(" + Constants.OBJECTCLASS + "=" + Factory.class.getName() + ")");
        } catch (InvalidSyntaxException e) {
            // Should not happen
            e.printStackTrace();
        }
    }

    /**
     * Stop the registry management.
     */
    public synchronized void stop() {
        m_parent.removeServiceListener(this);
        m_registry.reset();
        for (int i = 0; i < m_factories.size(); i++) {
            Record rec = (Record) m_factories.get(i);
            removeFactory(rec.m_ref);
        }
    }

    /**
     * Service Listener implementation.
     * @param event : the service event
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     */
    public void serviceChanged(ServiceEvent event) {
        if (event.getType() == ServiceEvent.REGISTERED) {
            if (!containsRef(event.getServiceReference())) {
                importFactory(event.getServiceReference());
            }
            return;
        }
        if (event.getType() == ServiceEvent.UNREGISTERING) {
            if (containsRef(event.getServiceReference())) {
                removeFactory(event.getServiceReference());
            }
        }
    }

    /**
     * Check if the factory list contain the given reference.
     * 
     * @param ref : the reference to find.
     * @return true if the list contains the given reference.
     */
    private boolean containsRef(ServiceReference ref) {
        for (int i = 0; i < m_factories.size(); i++) {
            Record rec = (Record) m_factories.get(i);
            if (rec.m_ref == ref) {
                return true;
            }
        }
        return false;
    }
}
