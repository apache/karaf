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
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * The iPOJO Context is a BundleContext implementation allowing the separation
 * between Bundle context and Service (Bundle) Context.
 * This is used inside composition to differentiate the classloading context (i.e.
 * Bundle) and the service registry access.
 * This class delegates calls to the good internal context (either the BundleContext
 * or the ServiceContext) according to the method. If the instance does not have a valid
 * service context, the bundle context is always used. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class IPojoContext implements BundleContext, ServiceContext {

    /**
     * The bundleContext used to access bundle methods.
     */
    private BundleContext m_bundleContext;

    /**
     * The service context used to access to the service registry.
     */
    private ServiceContext m_serviceContext;

    /**
     * Creates an iPOJO Context.
     * No service context is specified.
     * This constructor is used when the
     * instance lives in the global context.
     * @param context the bundle context
     */
    public IPojoContext(BundleContext context) {
        m_bundleContext = context;
    }

    /**
     * Creates an iPOJO Context.
     * A service context is used to refer to the
     * service registry. The service context will be 
     * used for all service accesses.
     * @param bundleContext the bundle context
     * @param serviceContext the service context
     */
    public IPojoContext(BundleContext bundleContext, ServiceContext serviceContext) {
        m_bundleContext = bundleContext;
        m_serviceContext = serviceContext;
    }

    /**
     * Adds a bundle listener.
     * @param listener the listener to add
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener(BundleListener listener) {
        m_bundleContext.addBundleListener(listener);
    }

    /**
     * Adds a framework listener.
     * @param listener the listener object to add
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener(FrameworkListener listener) {
        m_bundleContext.addFrameworkListener(listener);
    }

    /**
     * Adds a service listener.
     * This methods registers the listener on the service context
     * if it specified. Otherwise, if the internal dispatcher is enabled,
     * it registers the listener inside the internal dispatcher (if
     * the filter match against the iPOJO Filter format 
     * {@link IPojoContext#match(String)}). Finally, if the internal 
     * dispatcher is disabled, it uses the "regular" bundle context.
     * @param listener the service listener to add.
     * @param filter the LDAP filter
     * @throws InvalidSyntaxException if LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        if (m_serviceContext == null) {
            EventDispatcher dispatcher = EventDispatcher.getDispatcher();
            if (dispatcher != null) { // getDispatcher returns null if not enable.
                String itf = match(filter);
                if (itf != null) {
                    dispatcher.addListener(itf, listener);
                } else {
                    m_bundleContext.addServiceListener(listener, filter);
                }
            } else {
                m_bundleContext.addServiceListener(listener, filter);
            }
        } else {
            m_serviceContext.addServiceListener(listener, filter);
        }
    }

    /**
     * Add a service listener.
     * This methods registers the listener on the service context
     * if it specified. Otherwise, it uses the "regular" bundle context.
     * @param listener the service listener to add.
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
     * This method checks if the filter matches with the iPOJO
     * filter format: <code>(OBJECTCLASS=$ITF)</code>. It tries
     * to extract the required interface (<code>$ITF</code>).
     * @param filter the filter to analyze
     * @return the required interface or <code>null</code>
     * if the filter doesn't match with the iPOJO format.
     */
    private String match(String filter) {
        if (filter != null && filter.startsWith("(" + Constants.OBJECTCLASS + "=") // check the beginning (OBJECTCLASS
            && filter.lastIndexOf(')') == filter.indexOf(')')) { // check that there is only one )
            return filter.substring(("(" + Constants.OBJECTCLASS + "=").length(), filter.length() - 1);
        }
        return null;
    }
    
    

    /**
     * Creates a filter objects.
     * This method always uses the bundle context.
     * @param filter the string form of the LDAP filter to create
     * @return the filter object.
     * @throws InvalidSyntaxException if the given filter is malformed
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter(String filter) throws InvalidSyntaxException {
        return m_bundleContext.createFilter(filter);
    }

    /**
     * Gets the service references matching with the given query.
     * Uses the service context if specified, used the bundle context
     * otherwise.
     * @param clazz the required interface
     * @param filter the LDAP filter
     * @return the array of available service references
     * @throws InvalidSyntaxException if the LDAP filter is malformed
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
     * Gets the current bundle object.
     * @return the bundle declaring the component type of the instance
     * using the current IPojoContext.
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle() {
        return m_bundleContext.getBundle();
    }

    /**
     * Gets the bundle object with the given id.
     * @param bundleId the bundle id
     * @return the bundle object
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle(long bundleId) {
        return m_bundleContext.getBundle(bundleId);
    }

    /**
     * Gets installed bundles.
     * @return the list of installed bundles
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles() {
        return m_bundleContext.getBundles();
    }

    /**
     * Gets a data file.
     * @param filename the file name.
     * @return the File object
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile(String filename) {
        return m_bundleContext.getDataFile(filename);
    }

    /**
     * Gets a property value.
     * @param key the key of the asked property
     * @return the property value (object) or <code>null</code> if no
     * property are associated with the given key
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return m_bundleContext.getProperty(key);
    }

    /**
     * Gets a service object.
     * The given service reference must come from the same context than
     * where the service is get.
     * This method uses the service context if specified, the bundle
     * context otherwise.
     * @param reference the required service reference 
     * @return the service object or <code>null</code> if the service reference 
     * is no more valid or if the service object is not accessible.
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
     * Gets a service reference for the given interface.
     * This method uses the service context if specified, the bundle
     * context otherwise.
     * @param clazz the required interface name
     * @return a service reference on a available provider or <code>null</code>
     * if no providers available
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
     * Gets service reference list for the given query.
     * This method uses the service context if specified, the bundle
     * context otherwise.
     * @param clazz the name of the required service interface
     * @param filter the LDAP filter to apply on service provider
     * @return the array of consistent service reference or <code>null</code>
     * if no available providers
     * @throws InvalidSyntaxException if the LDAP filter is malformed
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
     * Installs a bundle.
     * @param location the URL of the bundle to install
     * @return the installed bundle
     * @throws BundleException if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle(String location) throws BundleException {
        return m_bundleContext.installBundle(location);
    }

    /**
     * Installs a bundle.
     * @param location the URL of the bundle to install
     * @param input the input stream to load the bundle.
     * @return the installed bundle
     * @throws BundleException  if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
     */
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return m_bundleContext.installBundle(location, input);
    }

    /**
     * Registers a service.
     * This method uses the service context if specified (and so, registers
     * the service in this service registry), the bundle context otherwise (the
     * service will be available to every global instances).
     * @param clazzes the interfaces provided by the service.
     * @param service the service object.
     * @param properties the service properties to publish
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
     * Registers a service.
     * This method uses the service context if specified (and so, registers
     * the service in this service registry), the bundle context otherwise (the
     * service will be available to every global instances).
     * @param clazz the interface provided by the service.
     * @param service the the service object.
     * @param properties the service properties to publish.
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
     * Removes a bundle listener.
     * @param listener the listener to remove
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener(BundleListener listener) {
        m_bundleContext.removeBundleListener(listener);
    }

    /**
     * Removes a framework listener.
     * @param listener the listener to remove
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener(FrameworkListener listener) {
        m_bundleContext.removeFrameworkListener(listener);
    }

    /**
     * Removes a service listener.
     * Removes the service listener from where it was registered so either in
     * the global context, or in the service context or in the internal dispatcher.
     * @param listener the service listener to remove
     * @see org.apache.felix.ipojo.ServiceContext#removeServiceListener(org.osgi.framework.ServiceListener)
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener) {
        if (m_serviceContext == null) {
            EventDispatcher dispatcher = EventDispatcher.getDispatcher();
            if (dispatcher == null || ! dispatcher.removeListener(listener)) {
                m_bundleContext.removeServiceListener(listener);
            }
        } else {
            m_serviceContext.removeServiceListener(listener);
        }
    }

    /**
     * Ungets the service reference.
     * This method uses the service context if specified, 
     * the bundle context otherwise.
     * @param reference the reference to unget
     * @return <code>true</code> if you are the last user of the reference
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
     * Gets the global context, i.e. the bundle context of the factory.
     * @return the global bundle context.
     */
    public BundleContext getGlobalContext() {
        return m_bundleContext;
    }

    /**
     * Gets the service context, i.e. the composite context.
     * Returns <code>null</code> if the instance does not live
     * inside a composite.
     * @return the service context or <code>null</code>.
     */
    public ServiceContext getServiceContext() {
        return m_serviceContext;
    }

}
