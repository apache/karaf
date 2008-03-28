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
package org.apache.felix.ipojo;

import java.io.File;
import java.io.InputStream;
import java.util.Dictionary;

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
 * The iPOJO Context is a BundleContext implementation allowing the separation
 * between Bundle context and Service (Bundle) Context.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IPojoContext implements BundleContext, ServiceContext {

    /**
     * BundleContext used to access bundle method.
     */
    private BundleContext m_bundleContext;

    /**
     * Service Context used to access service interaction.
     */
    private ServiceContext m_serviceContext;

    /**
     * Constructor. Used when the service context = the bundle context
     * 
     * @param context : bundle context
     */
    public IPojoContext(BundleContext context) {
        m_bundleContext = context;
    }

    /**
     * Constructor. Used when the service context and the bundle context are
     * different
     * 
     * @param bundleContext : bundle context
     * @param serviceContext : service context
     */
    public IPojoContext(BundleContext bundleContext, ServiceContext serviceContext) {
        m_bundleContext = bundleContext;
        m_serviceContext = serviceContext;
    }

    /**
     * Add a bundle listener.
     * @param listener : the listener to add
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener(BundleListener listener) {
        m_bundleContext.addBundleListener(listener);
    }

    /**
     * Add a framework listener.
     * @param listener : the listener object to add
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener(FrameworkListener listener) {
        m_bundleContext.addFrameworkListener(listener);
    }

    /**
     * Add a service listener.
     * @param listener : the service listener to add.
     * @param filter : the LDAP filter
     * @throws InvalidSyntaxException : occurs when the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        if (m_serviceContext == null) {
            m_bundleContext.addServiceListener(listener, filter);
        } else {
            m_serviceContext.addServiceListener(listener, filter);
        }
    }

    /**
     * Add a service listener.
     * @param listener : the service listener to add.
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    public void addServiceListener(ServiceListener listener) {
        if (m_serviceContext == null) {
            m_bundleContext.addServiceListener(listener);
        } else {
            m_serviceContext.addServiceListener(listener);
        }
    }

    /**
     * Create a Filter object.
     * @param filter : the string form of the LDAP filter to create
     * @return the Filter object.
     * @throws InvalidSyntaxException : occurs when the given filter is malformed
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return m_bundleContext.createFilter(filter);
    }

    /**
     * Get the service references matching with the given query.
     * @param clazz : Required interface
     * @param filter : LDAP filter
     * @return the array of available service reference
     * @throws InvalidSyntaxException : occurs if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        if (m_serviceContext == null) {
            return m_bundleContext.getAllServiceReferences(clazz, filter);
        } else {
            return m_serviceContext.getAllServiceReferences(clazz, filter);
        }
    }

    /**
     * Get the current bundle.
     * @return the current bundle
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle() {
        return m_bundleContext.getBundle();
    }

    /**
     * Get the bundle object with the given id.
     * @param bundleId : bundle id
     * @return the bundle object
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle(long bundleId) {
        return m_bundleContext.getBundle(bundleId);
    }

    /**
     * Get installed bundles.
     * @return the list of installed bundles
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles() {
        return m_bundleContext.getBundles();
    }

    /**
     * Get a data file.
     * @param filename : File name.
     * @return the File object
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile(String filename) {
        return m_bundleContext.getDataFile(filename);
    }

    /**
     * Get a property value.
     * @param key : key of the asked property
     * @return the property value (object) or null if no property are associated with the given key
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return m_bundleContext.getProperty(key);
    }

    /**
     * Get a service object.
     * @param reference : the required service reference 
     * @return the service object or null if the service reference is no more valid or if the service object is not accessible
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    public Object getService(ServiceReference reference) {
        if (m_serviceContext == null) {
            return m_bundleContext.getService(reference);
        } else {
            return m_serviceContext.getService(reference);
        }
    }

    /**
     * Get a service reference for the given interface.
     * @param clazz : the required interface name
     * @return a service reference on a available provider or null if no provider available
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference(String clazz) {
        if (m_serviceContext == null) {
            return m_bundleContext.getServiceReference(clazz);
        } else {
            return m_serviceContext.getServiceReference(clazz);
        }
    }

    /**
     * Get service reference list for the given query.
     * @param clazz : the name of the required service interface
     * @param filter : LDAP filter to apply on service provider
     * @return : the array of consistent service reference or null if no available provider
     * @throws InvalidSyntaxException : occurs if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        if (m_serviceContext == null) {
            return m_bundleContext.getServiceReferences(clazz, filter);
        } else {
            return m_serviceContext.getServiceReferences(clazz, filter);
        }
    }

    /**
     * Install a bundle.
     * @param location : URL of the bundle to install
     * @return the installed bundle
     * @throws BundleException : if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle(String location) throws BundleException {
        return m_bundleContext.installBundle(location);
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
        return m_bundleContext.installBundle(location, input);
    }

    /**
     * Register a service.
     * @param clazzes : interfaces provided by the service.
     * @param service : the service object.
     * @param properties : service properties.
     * @return the service registration for this service publication.
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        if (m_serviceContext == null) {
            return m_bundleContext.registerService(clazzes, service, properties);
        } else {
            return m_serviceContext.registerService(clazzes, service, properties);
        }
    }

    /**
     * Register a service.
     * @param clazz : interface provided by the service.
     * @param service : the service object.
     * @param properties : service properties.
     * @return the service registration for this service publication.
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
        if (m_serviceContext == null) {
            return m_bundleContext.registerService(clazz, service, properties);
        } else {
            return m_serviceContext.registerService(clazz, service, properties);
        }
    }

    /**
     * Remove a bundle listener.
     * @param listener : the listener to remove
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener(BundleListener listener) {
        m_bundleContext.removeBundleListener(listener);
    }

    /**
     * Remove a framework listener.
     * @param listener : the listener to remove
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener(FrameworkListener listener) {
        m_bundleContext.removeFrameworkListener(listener);
    }

    /**
     * Remove a service listener.
     * @param listener : the service listener to remove
     * @see org.apache.felix.ipojo.ServiceContext#removeServiceListener(org.osgi.framework.ServiceListener)
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener) {
        if (m_serviceContext == null) {
            m_bundleContext.removeServiceListener(listener);
        } else {
            m_serviceContext.removeServiceListener(listener);
        }
    }

    /**
     * Unget the service reference.
     * @param reference : the reference to unget
     * @return true if you are the last user of the reference
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    public boolean ungetService(ServiceReference reference) {
        if (m_serviceContext == null) {
            return m_bundleContext.ungetService(reference);
        } else {
            return m_serviceContext.ungetService(reference);
        }
    }

    /**
     * Get the global context, i.e. the bundle context of the factory.
     * @return the global bundle context.
     */
    public BundleContext getGlobalContext() {
        return m_bundleContext;
    }

    /**
     * Get the service context, i.e. the composite context.
     * @return the service context.
     */
    public ServiceContext getServiceContext() {
        return m_serviceContext;
    }

}
