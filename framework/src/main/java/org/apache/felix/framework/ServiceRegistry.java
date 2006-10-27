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

import java.util.*;

import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.*;

public class ServiceRegistry
{
    private Logger m_logger = null;
    private long m_currentServiceId = 1L;
    // Maps bundle to an array of service registrations.
    private Map m_serviceRegsMap = new HashMap();
    // Maps bundle to an array of usage counts.
    private Map m_inUseMap = new HashMap();

    private ServiceListener m_serviceListener = null;

    public ServiceRegistry(Logger logger)
    {
        m_logger = logger;
    }

    public synchronized ServiceReference[] getRegisteredServices(Bundle bundle)
    {
        ServiceRegistration[] regs = (ServiceRegistration[]) m_serviceRegsMap.get(bundle);
        if (regs != null)
        {
            ServiceReference[] refs = new ServiceReference[regs.length];
            for (int i = 0; i < refs.length; i++)
            {
                refs[i] = regs[i].getReference();
            }
            return refs;
        }
        return null;
    }

    public ServiceRegistration registerService(
        Bundle bundle, String[] classNames, Object svcObj, Dictionary dict)
    {
        ServiceRegistration reg = null;

        synchronized (this)
        {
            // Create the service registration.
            reg = new ServiceRegistrationImpl(
                this, bundle, classNames, new Long(m_currentServiceId++), svcObj, dict);
            // Get the bundles current registered services.
            ServiceRegistration[] regs = (ServiceRegistration[]) m_serviceRegsMap.get(bundle);
            m_serviceRegsMap.put(bundle, addServiceRegistration(regs, reg));
        }
        fireServiceChanged(new ServiceEvent(ServiceEvent.REGISTERED, reg.getReference()));
        return reg;
    }

    public void unregisterService(Bundle bundle, ServiceRegistration reg)
    {
        // First remove the registered service.
        synchronized (this)
        {
            ServiceRegistration[] regs = (ServiceRegistration[]) m_serviceRegsMap.get(bundle);
            m_serviceRegsMap.put(bundle, removeServiceRegistration(regs, reg));
        }

        // Fire the service event which gives all client bundles the
        // opportunity to unget their service object.
        fireServiceChanged(new ServiceEvent(ServiceEvent.UNREGISTERING, reg.getReference()));

        // Now forcibly unget the service object for all stubborn clients.
        synchronized (this)
        {
            Bundle[] clients = getUsingBundles(reg.getReference());
            for (int i = 0; (clients != null) && (i < clients.length); i++)
            {
                while (ungetService(clients[i], reg.getReference()))
                    ; // Keep removing until it is no longer possible
            }
        }
    }

    /**
     * This method retrieves all services registrations for the specified
     * bundle and invokes <tt>ServiceRegistration.unregister()</tt> on each
     * one. This method is only called be the framework to clean up after
     * a stopped bundle.
     * @param bundle the bundle whose services should be unregistered.
    **/
    public void unregisterServices(Bundle bundle)
    {
        // Simply remove all service registrations for the bundle.
        ServiceRegistration[] regs = null;
        synchronized (this)
        {
            regs = (ServiceRegistration[]) m_serviceRegsMap.get(bundle);
        }

        // Unregister each service.
        for (int i = 0; (regs != null) && (i < regs.length); i++)
        {
            regs[i].unregister();
        }

        // Now remove the bundle itself.
        m_serviceRegsMap.remove(bundle);
    }

    public synchronized List getServiceReferences(String className, Filter filter)
        throws InvalidSyntaxException
    {
        // Create a filtered list of service references.
        List list = new ArrayList();

        // Iterator over all service registrations.
        for (Iterator i = m_serviceRegsMap.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) i.next();
            ServiceRegistration[] regs = (ServiceRegistration[]) entry.getValue();

            for (int regIdx = 0;
                (regs != null) && (regIdx < regs.length);
                regIdx++)
            {
                // Determine if the registered services matches
                // the search criteria.
                boolean matched = false;

                // If className is null, then look at filter only.
                if ((className == null) &&
                    ((filter == null) || filter.match(regs[regIdx].getReference())))
                {
                    matched = true;
                }
                // If className is not null, then first match the
                // objectClass property before looking at the
                // filter.
                else if (className != null)
                {
                    String[] objectClass = (String[])
                        ((ServiceRegistrationImpl) regs[regIdx]).getProperty(FelixConstants.OBJECTCLASS);
                    for (int classIdx = 0;
                        classIdx < objectClass.length;
                        classIdx++)
                    {
                        if (objectClass[classIdx].equals(className) &&
                            ((filter == null) || filter.match(regs[regIdx].getReference())))
                        {
                            matched = true;
                            break;
                        }
                    }
                }

                // Add reference if it was a match.
                if (matched)
                {
                    list.add(regs[regIdx].getReference());
                }
            }
        }

        return list;
    }

    public synchronized ServiceReference[] getServicesInUse(Bundle bundle)
    {
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);
        if (usages != null)
        {
            ServiceReference[] refs = new ServiceReference[usages.length];
            for (int i = 0; i < refs.length; i++)
            {
                refs[i] = usages[i].m_ref;
            }
            return refs;
        }
        return null;
    }

    public synchronized Object getService(Bundle bundle, ServiceReference ref)
    {
        // Get usage counts for specified bundle.
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);

        // Make sure the service registration is still valid.
        if (!((ServiceReferenceImpl) ref).getServiceRegistration().isValid())
        {
            // If the service registration is not valid, then this means
            // that the service provider unregistered the service. The spec
            // says that calls to get an unregistered service should always
            // return null (assumption: even if it is currently cached
            // by the bundle). So in this case, flush the service reference
            // from the cache and return null.
            m_inUseMap.put(bundle, removeUsageCount(usages, ref));

            // It is not necessary to unget the service object from
            // the providing bundle, since the associated service is
            // unregistered and hence not in the list of registered services
            // of the providing bundle. This is precisely why the service
            // registration was not found above in the first place.
            return null;
        }

        // Get the service registration.
        ServiceRegistrationImpl reg = ((ServiceReferenceImpl) ref).getServiceRegistration();

        // Get the usage count, if any.
        UsageCount usage = getUsageCount(usages, ref);
       
        // If the service object is cached, then increase the usage
        // count and return the cached service object.
        Object svcObj = null;
        if (usage != null)
        {
            usage.m_count++;
            svcObj = usage.m_svcObj;
        }
        else
        {
            // Get service object from service registration.
            svcObj = reg.getService(bundle);

            // Cache the service object.
            if (svcObj != null)
            {
                m_inUseMap.put(bundle, addUsageCount(usages, ref, svcObj));
            }
        }

        return svcObj;
    }

    public synchronized boolean ungetService(Bundle bundle, ServiceReference ref)
    {
        // Get usage count.
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);
        UsageCount usage = getUsageCount(usages, ref);

        // If no usage count, then return.
        if (usage == null)
        {
            return false;
        }

        // Make sure the service registration is still valid.
        if (!((ServiceReferenceImpl) ref).getServiceRegistration().isValid())
        {
            // If the service registration is not valid, then this means
            // that the service provider unregistered the service. The spec
            // says that calls to get an unregistered service should always
            // return null (assumption: even if it is currently cached
            // by the bundle). So in this case, flush the service reference
            // from the cache and return null.
            m_inUseMap.put(bundle, removeUsageCount(usages, ref));
            return false;
        }

        // Decrement usage count.
        usage.m_count--;

        // Remove reference when usage count goes to zero
        // and unget the service object from the exporting
        // bundle.
        if (usage.m_count == 0)
        {
            // Remove reference from usages array.
            usages = removeUsageCount(usages, ref);
            // If there are no more usages in the array, then remove
            // the bundle from the inUseMap to allow for garbage collection.
            if (usages.length == 0)
            {
                m_inUseMap.remove(bundle);
            }
            else
            {
                m_inUseMap.put(bundle, usages);
            }
            ServiceRegistrationImpl reg =
                ((ServiceReferenceImpl) ref).getServiceRegistration();
            reg.ungetService(bundle, usage.m_svcObj);
            usage.m_svcObj = null;
        }

        // Return true if the usage count is greater than zero.
        return (usage.m_count > 0);
    }


    /**
     * This is a utility method to release all services being
     * used by the specified bundle.
     * @param bundle the bundle whose services are to be released.
    **/
    public synchronized void ungetServices(Bundle bundle)
    {
        UsageCount[] usages = (UsageCount[]) m_inUseMap.get(bundle);
        if (usages == null)
        {
            return;
        }

        // Remove each service object from the
        // service cache.
        for (int i = 0; i < usages.length; i++)
        {
            // Keep ungetting until all usage count is zero.
            while (ungetService(bundle, usages[i].m_ref))
            {
                // Empty loop body.
            }
        }
    }

    public synchronized Bundle[] getUsingBundles(ServiceReference ref)
    {
        Bundle[] bundles = null;
        for (Iterator iter = m_inUseMap.entrySet().iterator(); iter.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) iter.next();
            Bundle bundle = (Bundle) entry.getKey();
            UsageCount[] usages = (UsageCount[]) entry.getValue();
            for (int useIdx = 0; useIdx < usages.length; useIdx++)
            {
                if (usages[useIdx].m_ref.equals(ref))
                {
                    // Add the bundle to the array to be returned.
                    if (bundles == null)
                    {
                        bundles = new Bundle[] { bundle };
                    }
                    else
                    {
                        Bundle[] nbs = new Bundle[bundles.length + 1];
                        System.arraycopy(bundles, 0, nbs, 0, bundles.length);
                        nbs[bundles.length] = bundle;
                        bundles = nbs;
                    }
                }
            }
        }
        return bundles;
    }

    public void servicePropertiesModified(ServiceRegistration reg)
    {
        fireServiceChanged(new ServiceEvent(ServiceEvent.MODIFIED, reg.getReference()));
    }

    public Logger getLogger()
    {
        return m_logger;
    }

    private static ServiceRegistration[] addServiceRegistration(
        ServiceRegistration[] regs, ServiceRegistration reg)
    {
        if (regs == null)
        {
            regs = new ServiceRegistration[] { reg };
        }
        else
        {
            ServiceRegistration[] newRegs = new ServiceRegistration[regs.length + 1];
            System.arraycopy(regs, 0, newRegs, 0, regs.length);
            newRegs[regs.length] = reg;
            regs = newRegs;
        }
        return regs;
    }

    private static ServiceRegistration[] removeServiceRegistration(
        ServiceRegistration[] regs, ServiceRegistration reg)
    {
        for (int i = 0; (regs != null) && (i < regs.length); i++)
        {
            if (regs[i].equals(reg))
            {
                // If this is the only usage, then point to empty list.
                if ((regs.length - 1) == 0)
                {
                    regs = new ServiceRegistration[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    ServiceRegistration[] newRegs = new ServiceRegistration[regs.length - 1];
                    System.arraycopy(regs, 0, newRegs, 0, i);
                    if (i < newRegs.length)
                    {
                        System.arraycopy(
                            regs, i + 1, newRegs, i, newRegs.length - i);
                    }
                    regs = newRegs;
                }
            }
        }      
        return regs;
    }

    public synchronized void addServiceListener(ServiceListener l)
    {
        m_serviceListener = ServiceListenerMulticaster.add(m_serviceListener, l);
    }

    public synchronized void removeServiceListener(ServiceListener l)
    {
        m_serviceListener = ServiceListenerMulticaster.remove(m_serviceListener, l);
    }

    protected void fireServiceChanged(ServiceEvent event)
    {
        // Grab a copy of the listener list.
        ServiceListener listener = m_serviceListener;
        // If not null, then dispatch event.
        if (listener != null)
        {
            m_serviceListener.serviceChanged(event);
        }
    }

    private static class ServiceListenerMulticaster implements ServiceListener
    {
        protected ServiceListener m_a = null, m_b = null;

        protected ServiceListenerMulticaster(ServiceListener a, ServiceListener b)    
        {        
            m_a = a;        
            m_b = b;    
        }    
    
        public void serviceChanged(ServiceEvent e)    
        {
            m_a.serviceChanged(e);
            m_b.serviceChanged(e);
        }
    
        public static ServiceListener add(ServiceListener a, ServiceListener b)
        {
            if (a == null)
            {
                return b;
            }
            else if (b == null)
            {
                return a;
            }
            else
            {
                return new ServiceListenerMulticaster(a, b);
            }
        }
    
        public static ServiceListener remove(ServiceListener a, ServiceListener b)
        {
            if ((a == null) || (a == b))
            {
                return null;
            }
            else if (a instanceof ServiceListenerMulticaster)
            {
                return add(
                    remove(((ServiceListenerMulticaster) a).m_a, b),
                    remove(((ServiceListenerMulticaster) a).m_b, b));
            }
            else
            {
                return a;
            }
        }
    }

    private static UsageCount getUsageCount(UsageCount[] usages, ServiceReference ref)
    {
        for (int i = 0; (usages != null) && (i < usages.length); i++)
        {
            if (usages[i].m_ref.equals(ref))
            {
                return usages[i];
            }
        }
        return null;
    }

    private static UsageCount[] addUsageCount(UsageCount[] usages, ServiceReference ref, Object svcObj)
    {
        UsageCount usage = new UsageCount();
        usage.m_ref = ref;
        usage.m_svcObj = svcObj;
        usage.m_count++;

        if (usages == null)
        {
            usages = new UsageCount[] { usage };
        }
        else
        {
            UsageCount[] newUsages = new UsageCount[usages.length + 1];
            System.arraycopy(usages, 0, newUsages, 0, usages.length);
            newUsages[usages.length] = usage;
            usages = newUsages;
        }
        return usages;
    }

    private static UsageCount[] removeUsageCount(UsageCount[] usages, ServiceReference ref)
    {
        for (int i = 0; (usages != null) && (i < usages.length); i++)
        {
            if (usages[i].m_ref.equals(ref))
            {
                // If this is the only usage, then point to empty list.
                if ((usages.length - 1) == 0)
                {
                    usages = new UsageCount[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    UsageCount[] newUsages= new UsageCount[usages.length - 1];
                    System.arraycopy(usages, 0, newUsages, 0, i);
                    if (i < newUsages.length)
                    {
                        System.arraycopy(
                            usages, i + 1, newUsages, i, newUsages.length - i);
                    }
                    usages = newUsages;
                }
            }
        }
        
        return usages;
    }

    private static class UsageCount
    {
        public int m_count = 0;
        public ServiceReference m_ref = null;
        public Object m_svcObj = null;
    }
}