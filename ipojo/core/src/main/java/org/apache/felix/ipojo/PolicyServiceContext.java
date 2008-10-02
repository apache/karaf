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
 * The policy service context is a service context aiming to resolve service dependencies
 * inside different service context according to a policy.
 * So, the policy service context behavior follows one of the three following policy:
 * <li> Local : services are only resolved in the local service context.</li>
 * <li> Global : services are only resolved in the global context (hte OSGi one)</li>
 * <li> Local and Global : services are resolved inside the local context and inside
 * the global context</li>
 *    
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PolicyServiceContext implements ServiceContext {
    
    /**
     * Resolving policy, resolves services only in the composite
     * context (local).
     * This policy is the default one for services inherited from
     * service-level dependencies.
     */
    public static final int LOCAL = 0;
    
    /**
     * Resolving policy, resolves services only in the composite
     * (local) and in the global context.
     * This policy is the default one for implementation dependency.
     */
    public static final int LOCAL_AND_GLOBAL = 1;
    
    /**
     * Resolving policy, resolves services inside the global context
     * only.
     */
    public static final int GLOBAL = 2;
    
    /**
     * The global service registry.
     * Targets the OSGi service registry.
     */
    public BundleContext m_global;
    
    /**
     * The local (Composite) Service Registry.
     */
    public ServiceContext m_local;
    
    /**
     * The resolving policy to use to resolve
     * dependencies.
     */
    private int m_policy = LOCAL_AND_GLOBAL;
    
    
    /**
     * Creates a PolicyServiceContext.
     * If the local context is null, sets the policy to
     * {@link PolicyServiceContext#GLOBAL}, else use the 
     * given policy.
     * @param global the global bundle context
     * @param local the parent (local) service context
     * @param policy the resolution policy
     */
    public PolicyServiceContext(BundleContext global, ServiceContext local, int policy) {
        m_global = global;
        m_local = local;
        if (m_local == null) {
            m_policy = GLOBAL;
        } else {
            m_policy = policy;
        }
    }

    /**
     * Adds a service listener according to the policy.
     * Be aware, that the listener can be registered both in the local and in the global context
     * if the {@link PolicyServiceContext#LOCAL_AND_GLOBAL} is used.
     * @param listener the listener to add
     * @param filter the LDAP filter
     * @throws InvalidSyntaxException if the filter is malformed.
     * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    public void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException {
        if (m_policy == LOCAL || m_policy == LOCAL_AND_GLOBAL) {
            m_local.addServiceListener(listener, filter);
        }
        if (m_policy == GLOBAL || m_policy == LOCAL_AND_GLOBAL) {
            m_global.addServiceListener(listener, filter);
        }

    }

    /**
     * Adds a service listener according to the policy.
     * Be aware, that the listener can be registered both in the local and in the global context
     * if the {@link PolicyServiceContext#LOCAL_AND_GLOBAL} is used.
     * @param listener the listener to add
     * @see org.apache.felix.ipojo.ServiceContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    public void addServiceListener(ServiceListener listener) {
        if (m_policy == LOCAL || m_policy == LOCAL_AND_GLOBAL) {
            m_local.addServiceListener(listener);
        }
        if (m_policy == GLOBAL || m_policy == LOCAL_AND_GLOBAL) {
            m_global.addServiceListener(listener);
        }
    }

    /**
     * Gets all service references.
     * These references are found inside the local registry, global registry or both according to the policy.
     * The returned array can contain service references from both context.
     * @param clazz the required service specification.
     * @param filter the LDAP filter
     * @return the array of service reference, <code>null</code> if no service available
     * @throws InvalidSyntaxException if the LDAP filter is malformed 
     * @see org.apache.felix.ipojo.ServiceContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    public ServiceReference[] getAllServiceReferences(String clazz,
            String filter) throws InvalidSyntaxException {
        switch (m_policy) {
            case LOCAL:
                return m_local.getAllServiceReferences(clazz, filter);
            case GLOBAL:
                return m_global.getAllServiceReferences(clazz, filter);
            case LOCAL_AND_GLOBAL:
                ServiceReference[] refLocal = m_local.getAllServiceReferences(clazz, filter);
                ServiceReference[] refGlobal = m_global.getAllServiceReferences(clazz, filter);
                return computeServiceReferencesFromBoth(refLocal, refGlobal);
            default:
                return null;
        }
    }

    /**
     * Gets the service object for the given references.
     * The service is get inside the context according to the policy.
     * @param ref the service reference
     * @return the service object
     * @see org.apache.felix.ipojo.ServiceContext#getService(org.osgi.framework.ServiceReference)
     */
    public Object getService(ServiceReference ref) {
        switch(m_policy) { // NOPMD No break needed as we return in each branch.
            case LOCAL:
                // The reference comes from the local scope
                return m_local.getService(ref);
            case GLOBAL:
                // The reference comes from the global registry
                return m_global.getService(ref);
            case LOCAL_AND_GLOBAL:
                if (ref instanceof org.apache.felix.ipojo.context.ServiceReferenceImpl) {
                    return m_local.getService(ref);
                } else {
                    return m_global.getService(ref);
                }
            default : 
                return null;
        }
    }

    /**
     * Gets a service reference for the required service specification.
     * The service is looked inside the context according to the policy.
     * @param clazz the required service specification
     * @return a service reference or <code>null</code> if no matching service available
     * @see org.apache.felix.ipojo.ServiceContext#getServiceReference(java.lang.String)
     */
    public ServiceReference getServiceReference(String clazz) {
        switch (m_policy) { // NOPMD No break needed as we return in each branch.
            case LOCAL:
                return m_local.getServiceReference(clazz);
            case GLOBAL:
                return m_global.getServiceReference(clazz);
            case LOCAL_AND_GLOBAL:
                ServiceReference refLocal = m_local.getServiceReference(clazz);
                if (refLocal == null) {
                    return m_global.getServiceReference(clazz);
                } else {
                    return refLocal;
                }
            default:
                return null;
        }
    }

    /**
     * Get a service reference for the required service specification.
     * The services are looked inside the context according to the policy.
     * @param clazz the required service specification
     * @param filter the LDAP filter
     * @return a service reference array or <code>null</code> if not consistent service available
     * @throws InvalidSyntaxException if the LDAP filter is malformed 
     * @see org.apache.felix.ipojo.ServiceContext#getServiceReference(java.lang.String)
     */
    public ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException {
        switch (m_policy) {
            case LOCAL:
                return m_local.getServiceReferences(clazz, filter);
            case GLOBAL:
                return m_global.getServiceReferences(clazz, filter);
            case LOCAL_AND_GLOBAL:
                ServiceReference[] refLocal = m_local.getServiceReferences(clazz, filter);
                ServiceReference[] refGlobal = m_global.getServiceReferences(clazz, filter);
                return computeServiceReferencesFromBoth(refLocal, refGlobal);
            default:
                return null;
        }

    }
    
    /**
     * Computes the service reference array from the two given set of service references
     * according to the policy.
     * @param refLocal the set of local references
     * @param refGlobal the set of global references
     * @return the set of service references
     */
    private ServiceReference[] computeServiceReferencesFromBoth(ServiceReference[] refLocal, ServiceReference[] refGlobal) {
        if (refLocal == null) {
            return refGlobal; // If refGlobal is null, return null, else return refGlobal
        } else if (refGlobal == null) { // refLocal != null && refGlobal == null
            return refLocal;
        } else { // Both refGlobal and refLocal are not null
            ServiceReference[] refs = new ServiceReference[refLocal.length + refGlobal.length];
            System.arraycopy(refLocal, 0, refs, 0, refLocal.length);
            System.arraycopy(refGlobal, 0, refs, refLocal.length, refGlobal.length);
            return refs;
        }        
    }

    /**
     * This method is not supported.
     * This context can only be used to resolve service dependencies.
     * @param clazzes the specifications
     * @param service the service object
     * @param properties the service properties
     * @return the service registration object
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties) {
        throw new UnsupportedOperationException("PolicyServiceContext can only be used for service dependency and not to provide services");
    }

    /**
     * This method is not supported.
     * This context can only be used to resolve service dependencies.
     * @param clazz the specification
     * @param service the service object
     * @param properties the service properties to publish
     * @return the service registration object
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    public ServiceRegistration registerService(String clazz, Object service, Dictionary properties) {
        throw new UnsupportedOperationException("PolicyServiceContext can only be used for service dependency and not to provide services");
    }

    /**
     * Removes a service listener.
     * @param listener the service listener to remove
     * @see org.apache.felix.ipojo.ServiceContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    public void removeServiceListener(ServiceListener listener) {
        if (m_policy == LOCAL || m_policy == LOCAL_AND_GLOBAL) {
            m_local.removeServiceListener(listener);
        }
        if (m_policy == GLOBAL || m_policy == LOCAL_AND_GLOBAL) {
            m_global.removeServiceListener(listener);
        }
    }

    /**
     * Ungets the service reference.
     * @param reference the service reference to unget.
     * @return <code>true</code> if the service release if the reference is no more used.
     * @see org.apache.felix.ipojo.ServiceContext#ungetService(org.osgi.framework.ServiceReference)
     */
    public boolean ungetService(ServiceReference reference) {
        if (reference instanceof org.apache.felix.ipojo.context.ServiceReferenceImpl) {
            return m_local.ungetService(reference);
        } else {
            return m_global.ungetService(reference);
        }
    }

    /**
     * Adds a bundle listener.
     * Delegate on the global bundle context.
     * @param arg0 : bundle listener to add
     * @see org.osgi.framework.BundleContext#addBundleListener(org.osgi.framework.BundleListener)
     */
    public void addBundleListener(BundleListener arg0) {
        m_global.addBundleListener(arg0);
    }

    /**
     * Adds a framework listener.
     * Delegates on the global bundle context.
     * @param arg0 : framework listener to add.
     * @see org.osgi.framework.BundleContext#addFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void addFrameworkListener(FrameworkListener arg0) {
        m_global.addFrameworkListener(arg0);
    }

    /**
     * Creates a LDAP filter.
     * @param arg0 the String-form of the filter
     * @return the created filter object
     * @throws InvalidSyntaxException if the given argument is not a valid against the LDAP grammar.
     * @see org.osgi.framework.BundleContext#createFilter(java.lang.String)
     */
    public Filter createFilter(String arg0) throws InvalidSyntaxException {
        return m_global.createFilter(arg0);
    }

    /**
     * Gets the current bundle.
     * @return the current bundle
     * @see org.osgi.framework.BundleContext#getBundle()
     */
    public Bundle getBundle() {
        return m_global.getBundle();
    }

    /**
     * Gets the bundle object with the given id.
     * @param bundleId the bundle id
     * @return the bundle object
     * @see org.osgi.framework.BundleContext#getBundle(long)
     */
    public Bundle getBundle(long bundleId) {
        return m_global.getBundle(bundleId);
    }

    /**
     * Gets installed bundles.
     * @return the list of installed bundles
     * @see org.osgi.framework.BundleContext#getBundles()
     */
    public Bundle[] getBundles() {
        return m_global.getBundles();
    }


    /**
     * Gets a data file.
     * @param filename the File name.
     * @return the File object
     * @see org.osgi.framework.BundleContext#getDataFile(java.lang.String)
     */
    public File getDataFile(String filename) {
        return m_global.getDataFile(filename);
    }

    /**
     * Gets a property value.
     * @param key the key of the asked property
     * @return the property value (object) or <code>null</code> if no property
     * are associated with the given key
     * @see org.osgi.framework.BundleContext#getProperty(java.lang.String)
     */
    public String getProperty(String key) {
        return m_global.getProperty(key);
    }

    /**
     * Installs a bundle.
     * @param location the URL of the bundle to install
     * @return the installed bundle
     * @throws BundleException if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String)
     */
    public Bundle installBundle(String location) throws BundleException {
        return m_global.installBundle(location);
    }

    /**
     * Installs a bundle.
     * @param location the URL of the bundle to install
     * @param input the input stream to load the bundle content
     * @return the installed bundle
     * @throws BundleException if the bundle cannot be installed correctly
     * @see org.osgi.framework.BundleContext#installBundle(java.lang.String, java.io.InputStream)
     */
    public Bundle installBundle(String location, InputStream input) throws BundleException {
        return m_global.installBundle(location, input);
    }

    /**
     * Removes the bundle listener.
     * @param listener the listener to remove
     * @see org.osgi.framework.BundleContext#removeBundleListener(org.osgi.framework.BundleListener)
     */
    public void removeBundleListener(BundleListener listener) {
        m_global.removeBundleListener(listener);
    }

    /**
     * Removes a framework listener.
     * @param listener the listener to remove
     * @see org.osgi.framework.BundleContext#removeFrameworkListener(org.osgi.framework.FrameworkListener)
     */
    public void removeFrameworkListener(FrameworkListener listener) {
        m_global.removeFrameworkListener(listener);
    }

}
