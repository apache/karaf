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

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IWire;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

class ServiceReferenceImpl implements ServiceReference
{
    private ServiceRegistrationImpl m_registration = null;
    private Bundle m_bundle = null;

    public ServiceReferenceImpl(ServiceRegistrationImpl reg, Bundle bundle)
    {
        m_registration = reg;
        m_bundle = bundle;
    }
    
    protected ServiceRegistrationImpl getServiceRegistration()
    {
        return m_registration;
    }

    public Object getProperty(String s)
    {
        return m_registration.getProperty(s);
    }

    public String[] getPropertyKeys()
    {
        return m_registration.getPropertyKeys();
    }

    public Bundle getBundle()
    {
        // The spec says that this should return null if
        // the service is unregistered.
        return (m_registration.isValid()) ? m_bundle : null;
    }

    public Bundle[] getUsingBundles()
    {
        return m_registration.getUsingBundles();
    }

    public boolean equals(Object obj)
    {
        try
        {
            ServiceReferenceImpl ref = (ServiceReferenceImpl) obj;
            return ref.m_registration == m_registration;
        }
        catch (ClassCastException ex)
        {
            // Ignore and return false.
        }
        catch (NullPointerException ex)
        {
            // Ignore and return false.
        }

        return false;
    }

    public int hashCode()
    {
        if (m_registration.getReference() != null)
        {
            if (m_registration.getReference() != this)
            {
                return m_registration.getReference().hashCode();
            }
            return super.hashCode();
        }
        return 0;
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
        // Get package wiring from service requester.
        IWire requesterWire = Util.getWire(
            ((BundleImpl) requester).getInfo().getCurrentModule(), pkgName);
        // Get package wiring from service provider.
        IWire providerWire = Util.getWire(
            ((BundleImpl) m_bundle).getInfo().getCurrentModule(), pkgName);

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
            // This is an intentional no-op.
        }
        // Case 2: Only include service reference if the service
        // object uses the same class as the requester.
        else if (providerWire == null)
        {
            try
            {
                // Load the class from the requesting bundle.
                Class requestClass =
                    ((BundleImpl) requester).getInfo().getCurrentModule().getClass(className);
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
        // Case 3: Include service reference if the wires have the
        // same source module.
        else
        {
            allow = providerWire.getExporter().equals(requesterWire.getExporter());
        }

        return allow;
    }
}