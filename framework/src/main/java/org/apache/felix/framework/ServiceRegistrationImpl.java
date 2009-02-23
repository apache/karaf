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
package org.apache.felix.framework;

import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.*;

import org.apache.felix.framework.util.StringMap;
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IModule;
import org.apache.felix.moduleloader.IWire;
import org.osgi.framework.*;

class ServiceRegistrationImpl implements ServiceRegistration
{
    // Service registry.
    private ServiceRegistry m_registry = null;
    // Bundle implementing the service.
    private Bundle m_bundle = null;
    // Interfaces associated with the service object.
    private String[] m_classes = null;
    // Service Id associated with the service object.
    private Long m_serviceId = null;
    // Service object.
    private Object m_svcObj = null;
    // Service factory interface.
    private ServiceFactory m_factory = null;
    // Associated property dictionary.
    private volatile Map m_propMap = new StringMap(false);
    // Re-usable service reference.
    private ServiceReferenceImpl m_ref = null;
    // Flag indicating that we are unregistering.
    private boolean m_isUnregistering = false;

    public ServiceRegistrationImpl(
        ServiceRegistry registry, Bundle bundle,
        String[] classes, Long serviceId,
        Object svcObj, Dictionary dict)
    {
        m_registry = registry;
        m_bundle = bundle;
        m_classes = classes;
        m_serviceId = serviceId;
        m_svcObj = svcObj;
        m_factory = (m_svcObj instanceof ServiceFactory)
            ? (ServiceFactory) m_svcObj : null;

        initializeProperties(dict);

        // This reference is the "standard" reference for this
        // service and will always be returned by getReference().
        // Since all reference to this service are supposed to
        // be equal, we use the hashcode of this reference for
        // a references to this service in ServiceReference.
        m_ref = new ServiceReferenceImpl();
    }

    protected synchronized boolean isValid()
    {
        return (m_svcObj != null);
    }

    protected synchronized void invalidate()
    {
        m_svcObj = null;
    }

    public ServiceReference getReference()
    {
        // Make sure registration is valid.
        if (!isValid())
        {
            throw new IllegalStateException(
                "The service registration is no longer valid.");
        }
        return m_ref;
    }

    public void setProperties(Dictionary dict)
    {
        // Make sure registration is valid.
        if (!isValid())
        {
            throw new IllegalStateException(
                "The service registration is no longer valid.");
        }
        // Set the properties.
        initializeProperties(dict);
        // Tell registry about it.
        m_registry.servicePropertiesModified(this);
    }

    public void unregister()
    {
        synchronized (this)
        {
            if (!isValid() || m_isUnregistering)
            {
                throw new IllegalStateException("Service already unregistered.");
            }
            m_isUnregistering = true;
        }
        m_registry.unregisterService(m_bundle, this);
        synchronized (this)
        {
            m_svcObj = null;
            m_factory = null;
        }
    }

    //
    // Utility methods.
    //

    /**
     * This method determines if the class loader of the service object
     * has access to the specified class.
     * @param clazz the class to test for reachability.
     * @return <tt>true</tt> if the specified class is reachable from the
     *         service object's class loader, <tt>false</tt> otherwise.
    **/
    protected boolean isClassAccessible(Class clazz)
    {
        try
        {
            // Try to load from the service object or service factory class.
            Class sourceClass = (m_factory != null)
                ? m_factory.getClass() : m_svcObj.getClass();
            Class targetClass = Util.loadClassUsingClass(sourceClass, clazz.getName());
            return (targetClass == clazz);
        }
        catch (Exception ex)
        {
            // Ignore this and return false.
        }
        return false;
    }

    protected Object getProperty(String key)
    {
        return m_propMap.get(key);
    }

    protected String[] getPropertyKeys()
    {
        Set s = m_propMap.keySet();
        return (String[]) s.toArray(new String[s.size()]);
    }

    protected Bundle[] getUsingBundles()
    {
        return m_registry.getUsingBundles(m_ref);
    }

    /**
     * This method provides direct access to the associated service object;
     * it generally should not be used by anyone other than the service registry
     * itself.
     * @return The service object associated with the registration.
    **/
    Object getService()
    {
        return m_svcObj;
    }

    protected Object getService(Bundle acqBundle)
    {
        // If the service object is a service factory, then
        // let it create the service object.
        if (m_factory != null)
        {
            try
            {
                if (System.getSecurityManager() != null)
                {
                    return AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(acqBundle, null));
                }
                else
                {
                    return getFactoryUnchecked(acqBundle);
                }
            }
            catch (Exception ex)
            {
                m_registry.getLogger().log(
                    Logger.LOG_ERROR,
                    "ServiceRegistrationImpl: Error getting service.", ex);
                return null;
            }
        }
        else
        {
            return m_svcObj;
        }
    }

    protected void ungetService(Bundle relBundle, Object svcObj)
    {
        // If the service object is a service factory, then
        // let it release the service object.
        if (m_factory != null)
        {
            try
            {
                if (System.getSecurityManager() != null)
                {
                    AccessController.doPrivileged(
                        new ServiceFactoryPrivileged(relBundle, svcObj));
                }
                else
                {
                    ungetFactoryUnchecked(relBundle, svcObj);
                }
            }
            catch (Exception ex)
            {
                m_registry.getLogger().log(
                    Logger.LOG_ERROR, "ServiceRegistrationImpl: Error ungetting service.", ex);
            }
        }
    }

    private void initializeProperties(Dictionary dict)
    {
        // Create a case-insensitive map for the properties.
        Map props = new StringMap(false);

        if (dict != null)
        {
            // Make sure there are no duplicate keys.
            Enumeration keys = dict.keys();
            while (keys.hasMoreElements())
            {
                Object key = keys.nextElement();
                if (props.get(key) == null)
                {
                    props.put(key, dict.get(key));
                }
                else
                {
                    throw new IllegalArgumentException("Duplicate service property: " + key);
                }
            }
        }

        // Add the framework assigned properties.
        props.put(Constants.OBJECTCLASS, m_classes);
        props.put(Constants.SERVICE_ID, m_serviceId);

        // Update the service property map.
        m_propMap = props;
    }

    private Object getFactoryUnchecked(Bundle bundle)
    {
        Object svcObj = m_factory.getService(bundle, this);
        if (svcObj != null)
        {
            for (int i = 0; i < m_classes.length; i++)
            {
                Class clazz = Util.loadClassUsingClass(svcObj.getClass(), m_classes[i]);
                if ((clazz == null) || !clazz.isAssignableFrom(svcObj.getClass()))
                {
                    return null;
                }
            }
        }
        return svcObj;
    }

    private void ungetFactoryUnchecked(Bundle bundle, Object svcObj)
    {
        m_factory.ungetService(bundle, this, svcObj);
    }

    /**
     * This simple class is used to ensure that when a service factory
     * is called, that no other classes on the call stack interferes
     * with the permissions of the factory itself.
    **/
    private class ServiceFactoryPrivileged implements PrivilegedExceptionAction
    {
        private Bundle m_bundle = null;
        private Object m_svcObj = null;

        public ServiceFactoryPrivileged(Bundle bundle, Object svcObj)
        {
            m_bundle = bundle;
            m_svcObj = svcObj;
        }

        public Object run() throws Exception
        {
            if (m_svcObj == null)
            {
                return getFactoryUnchecked(m_bundle);
            }
            else
            {
                ungetFactoryUnchecked(m_bundle, m_svcObj);
            }
            return null;
        }
    }

    class ServiceReferenceImpl implements ServiceReference
    {
        private ServiceReferenceImpl() {}

        ServiceRegistrationImpl getServiceRegistration()
        {
            return ServiceRegistrationImpl.this;
        }

        public Object getProperty(String s)
        {
            return ServiceRegistrationImpl.this.getProperty(s);
        }

        public String[] getPropertyKeys()
        {
            return ServiceRegistrationImpl.this.getPropertyKeys();
        }

        public Bundle getBundle()
        {
            // The spec says that this should return null if
            // the service is unregistered.
            return (isValid()) ? m_bundle : null;
        }

        public Bundle[] getUsingBundles()
        {
            return ServiceRegistrationImpl.this.getUsingBundles();
        }

        public String toString()
        {
            String[] ocs = (String[]) getProperty("objectClass");
            String oc = "[";
            for(int i = 0; i < ocs.length; i++)
            {
                oc = oc + ocs[i];
                if (i < ocs.length - 1)
                    oc = oc + ", ";
            }
            oc = oc + "]";
            return oc;
        }

        public boolean isAssignableTo(Bundle requester, String className)
        {
            // Always return true if the requester is the same as the provider.
            if (requester == m_bundle)
            {
                return true;
            }

            // Boolean flag.
            boolean allow = true;
            // Get the package.
            String pkgName =
                Util.getClassPackage(className);
            IModule requesterModule = ((BundleImpl) requester).getCurrentModule();
            // Get package wiring from service requester.
            IWire requesterWire = Util.getWire(requesterModule, pkgName);

            // There are three situations that may occur here:
            //   1. The requester does not have a wire for the package.
            //   2. The provider does not have a wire for the package.
            //   3. Both have a wire for the package.
            // For case 1, we do not filter the service reference since we
            // assume that the bundle is using reflection or that it won't
            // use that class at all since it does not import it. For
            // case 2, we have to try to load the class from the class
            // loader of the service object and then compare the class
            // loaders to determine if we should filter the service
            // refernce. In case 3, we simply compare the exporting
            // modules from the package wiring to determine if we need
            // to filter the service reference.

            // Case 1: Always include service reference.
            if (requesterWire == null)
            {
                return allow;
            }

            // Get package wiring from service provider.
            IModule providerModule = ((BundleImpl) m_bundle).getCurrentModule();
            IWire providerWire = Util.getWire(providerModule, pkgName);

            // Case 2: Only include service reference if the service
            // object uses the same class as the requester.
            if (providerWire == null)
            {
                // If the provider is not the exporter of the requester's package,
                // then try to use the service registration to see if the requester's
                // class is accessible.
                if (!((BundleImpl) m_bundle).hasModule(requesterWire.getExporter()))
                {
                    try
                    {
                        // Load the class from the requesting bundle.
                        Class requestClass = requesterModule.getClassByDelegation(className);
                        // Get the service registration and ask it to check
                        // if the service object is assignable to the requesting
                        // bundle's class.
                        allow = getServiceRegistration().isClassAccessible(requestClass);
                    }
                    catch (Exception ex)
                    {
                        // This should not happen, filter to be safe.
                        allow = false;
                    }
                }
                else
                {
                    // O.k. the provider is the exporter of the requester's package, now check
                    // if the requester is wired to the latest version of the provider, if so
                    // then allow else don't (the provider has been updated but not refreshed).
                    allow = providerModule == requesterWire.getExporter();
                }
            }
            // Case 3: Include service reference if the wires have the
            // same source module.
            else
            {
                allow = providerWire.getExporter().equals(requesterWire.getExporter());
            }

            return allow;
        }

        public int compareTo(Object reference)
        {
            ServiceReference other = (ServiceReference) reference;

            Long id = (Long) getProperty(Constants.SERVICE_ID);
            Long otherId = (Long) other.getProperty(Constants.SERVICE_ID);

            if (id.equals(otherId))
            {
                return 0; // same service
            }

            Object rankObj = getProperty(Constants.SERVICE_RANKING);
            Object otherRankObj = other.getProperty(Constants.SERVICE_RANKING);

            // If no rank, then spec says it defaults to zero.
            rankObj = (rankObj == null) ? new Integer(0) : rankObj;
            otherRankObj = (otherRankObj == null) ? new Integer(0) : otherRankObj;

            // If rank is not Integer, then spec says it defaults to zero.
            Integer rank = (rankObj instanceof Integer)
                ? (Integer) rankObj : new Integer(0);
            Integer otherRank = (otherRankObj instanceof Integer)
                ? (Integer) otherRankObj : new Integer(0);

            // Sort by rank in ascending order.
            if (rank.compareTo(otherRank) < 0)
            {
                return -1; // lower rank
            }
            else if (rank.compareTo(otherRank) > 0)
            {
                return 1; // higher rank
            }

            // If ranks are equal, then sort by service id in descending order.
            return (id.compareTo(otherId) < 0) ? 1 : -1;
        }
    }
}