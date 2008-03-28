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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.context.ServiceRegistry;
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * CompositeServiceContext Class. This class provides an implementation of the
 * service context for composite.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeServiceContext implements ServiceContext, TrackerCustomizer {

    /**
     * Structure storing the reference, the factory and the registration.
     */
    private class Record {
        /**
         * Reference of the represented factory from the external context.
         */
        private ServiceReference m_ref;
        /**
         * Registration of the factory in the internal context.
         */
        private ServiceRegistration m_reg;
        /**
         * Represented Factory. 
         */
        private FactoryProxy m_fact;
    }

    /**
     * List of imported factories.
     */
    private List m_factories = new ArrayList();
    /**
     * Internal service registry.
     */
    private ServiceRegistry m_registry;

    /**
     * Component Instance who creates this registry.
     */
    private ComponentInstance m_instance;
    
    /**
     * Global service context.
     */
    private BundleContext m_global;
    
    /**
     * Tracker tracking Factories to import.
     */
    private Tracker m_tracker;

    /**
     * Constructor. This constructor instantiate a service registry with the
     * given bundle context.
     * 
     * @param context : the bundle context
     */
    public CompositeServiceContext(BundleContext context) {
        m_registry = new ServiceRegistry(context);
        if (context instanceof IPojoContext) {
            m_global = ((IPojoContext) context).getGlobalContext();
        } else {
            m_global = context; // the parent context is the global context
        }
    }

    /**
     * Constructor.
     * 
     * @param context : the bundle context
     * @param instance : the component instance owning this context
     */
    public CompositeServiceContext(BundleContext context, ComponentInstance instance) {
        this(context);
        m_instance = instance;
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
     * @param arg0 : the service listener object to add
     * @param arg1 : the LDAP filter for this listener
     * @throws InvalidSyntaxException : occurs if the LDAP filter is malformed
     * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener,
     * java.lang.String)
     */
    public void addServiceListener(ServiceListener arg0, String arg1) throws InvalidSyntaxException {
        m_registry.addServiceListener(arg0, arg1);
    }

    /**
     * Get all service references.
     * @param arg0 : The required service interface.
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
     * @param arg0 : the required interface name
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
        rec.m_fact = new FactoryProxy((Factory) m_tracker.getService(ref), this);
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
                if (rec.m_reg != null) {
                    rec.m_reg.unregister();
                    rec.m_fact = null;
                }
                m_tracker.ungetService(rec.m_ref);
                m_factories.remove(rec);
                return;
            }
        }
    }

    /**
     * Start the registry management.
     */
    public void start() {
        m_tracker = new Tracker(m_global, Factory.class.getName(), this);
        m_tracker.open();
    }

    /**
     * Stop the registry management.
     */
    public synchronized void stop() {
        m_tracker.close();
        m_registry.reset();
        for (int i = 0; i < m_factories.size(); i++) {
            Record rec = (Record) m_factories.get(i);
            removeFactory(rec.m_ref);
        }
        m_tracker = null;
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

    /**
     * Add a bundle listener.
     * Delegate on the global bundle context.
     * @param arg0 : bundle listener to add
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener(BundleListener arg0) {
        m_global.addBundleListener(arg0);
    }

    /**
     * Add a framework listener.
     * Delegate on the global bundle context.
     * @param arg0 : framework listener to add.
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener(FrameworkListener arg0) {
        m_global.addFrameworkListener(arg0);
    }

    /**
     * Create a LDAP filter.
     * @param arg0 : String-form of the filter
     * @return the created filter object
     * @throws InvalidSyntaxException : if the given argument is not a valid against the LDAP grammar.
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter(String arg0) throws InvalidSyntaxException {
        return m_global.createFilter(arg0);
    }

    /**
     * Get the current bundle.
     * @return the current bundle
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle() {
        return m_global.getBundle();
    }

    /**
     * Get the bundle object with the given id.
     * @param bundleId : bundle id
     * @return the bundle object
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle(long bundleId) {
        return m_global.getBundle(bundleId);
    }

    /**
     * Get installed bundles.
     * @return the list of installed bundles
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles() {
        return m_global.getBundles();
    }


    /**
     * Get a data file.
     * @param filename : File name.
     * @return the File object
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile(String filename) {
        return m_global.getDataFile(filename);
    }

    /**
     * Get a property value.
     * @param key : key of the asked property
     * @return the property value (object) or null if no property are associated with the given key
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return m_global.getProperty(key);
    }

    /**
     * Install a bundle.
     * @param location : URL of the bundle to install
     * @return the installed bundle
     * @throws BundleException : if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle(String location) throws BundleException {
        return m_global.installBundle(location);
    }

    /**
     * Install a bundle.
     * @param location : URL of the bundle to install
     * @param input : 
     * @return the installed bundle
     * @throws BundleException : if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
     */
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return m_global.installBundle(location, input);
    }

    /**
     * Remove a bundle listener.
     * @param listener : the listener to remove
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener(BundleListener listener) {
        m_global.removeBundleListener(listener);
    }

    /**
     * Remove a framework listener.
     * @param listener : the listener to remove
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener(FrameworkListener listener) {
        m_global.removeFrameworkListener(listener);
    }

    /**
     * A new factory is detected.
     * @param reference : service reference
     * @return true if not already imported.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference reference) {
        if (!containsRef(reference)) {
            return true;
        }
        return false;
    }
    
    /**
     * A matching reference has been added. The import factory can now be imported.
     * @param reference : the added reference.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
     */
    public void addedService(ServiceReference reference) {
        importFactory(reference);
    }

    /**
     * An imported factory is modified.
     * @param reference : modified reference
     * @param service : factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference reference, Object service) {
        for (int i = 0; i < m_factories.size(); i++) {
            Record rec = (Record) m_factories.get(i);
            if (rec.m_ref == reference) {
                Dictionary dict = new Properties();
                for (int j = 0; j < reference.getPropertyKeys().length; j++) {
                    dict.put(reference.getPropertyKeys()[j], reference.getProperty(reference.getPropertyKeys()[j]));
                }
                rec.m_reg.setProperties(dict);
                return;
            }
        }
    }

    /**
     * An imported factory disappears.
     * @param reference : reference
     * @param service : factory object.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference reference, Object service) {
        if (containsRef(reference)) {
            removeFactory(reference);
        }
        
    }
}
