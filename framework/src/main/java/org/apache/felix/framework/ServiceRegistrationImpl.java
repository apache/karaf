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
    private Map m_propMap =  new StringMap(false);
    // Re-usable service reference.
    private ServiceReferenceImpl m_ref = null;

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
        m_ref = new ServiceReferenceImpl(this, m_bundle);
    }

    protected boolean isValid()
    {
        return (m_svcObj != null);
    }

    public ServiceReference getReference()
    {
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
        if (m_svcObj != null)
        {
            m_registry.unregisterService(m_bundle, this);
            m_svcObj = null;
            m_factory = null;
        }
        else
        {
            throw new IllegalStateException("Service already unregistered.");
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

    private transient ArrayList m_list = new ArrayList();

    protected String[] getPropertyKeys()
    {
        synchronized (m_propMap)
        {
            m_list.clear();
            Iterator i = m_propMap.entrySet().iterator();
            while (i.hasNext())
            {
                Map.Entry entry = (Map.Entry) i.next();
                m_list.add(entry.getKey());
            }
            return (String[]) m_list.toArray(new String[m_list.size()]);
        }
    }

    protected Bundle[] getUsingBundles()
    {
        return m_registry.getUsingBundles(m_ref);
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
                    Logger.LOG_ERROR, "ServiceRegistrationImpl: Error getting service.", ex);
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
        // let is release the service object.
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
        synchronized (m_propMap)
        {
            m_propMap.clear();
    
            if (dict != null)
            {
                Enumeration keys = dict.keys();
                while (keys.hasMoreElements())
                {
                    Object key = keys.nextElement();
                    m_propMap.put(key, dict.get(key));
                }
            }
    
            // Add the framework assigned properties.
            m_propMap.put(Constants.OBJECTCLASS, m_classes);
            m_propMap.put(Constants.SERVICE_ID, m_serviceId);
        }
    }

    private Object getFactoryUnchecked(Bundle bundle)
    {
        return m_factory.getService(bundle, this);
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
}