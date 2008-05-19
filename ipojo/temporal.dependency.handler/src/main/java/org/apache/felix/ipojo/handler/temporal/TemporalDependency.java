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
package org.apache.felix.ipojo.handler.temporal;

import java.lang.reflect.Array;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
* Temporal dependency.
* A temporal dependency waits (block) for the availability of the service.
* If no provider arrives in the specified among of time, a runtime exception is thrown.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class TemporalDependency extends DependencyModel implements FieldInterceptor {
    
    /**
     * Timeout.
     */
    private long m_timeout;

    /**
     * Constructor.
     * @param spec : service specification
     * @param agg : is the dependency aggregate ?
     * @param filter : LDAP filter
     * @param context : service context
     * @param timeout : timeout
     * @param handler : Handler managing this dependency
     */
    public TemporalDependency(Class spec, boolean agg, Filter filter, BundleContext context, long timeout, TemporalHandler handler) {
        super(spec, agg, true, filter, null, DependencyModel.DYNAMIC_BINDING_POLICY, context, handler);
        this.m_timeout = timeout;
    }

    /**
     * The dependency has been reconfigured.
     * @param arg0 : new service references
     * @param arg1 : old service references
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] arg0, ServiceReference[] arg1) { 
        throw new UnsupportedOperationException("Reconfiguration not yet supported");
    }

    /**
     * A provider arrives.
     * @param arg0 : service reference of the new provider.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference arg0) {
        // Notify if a thread is waiting.
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * A provider leaves.
     * Nothing to do.
     * @param arg0 : leaving service references.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public synchronized void onServiceDeparture(ServiceReference arg0) { }
    
    /**
     * The code require a value of the monitored field.
     * If providers are available, the method return service object(s) immediately. 
     * Else, the thread is blocked until an arrival. If no provider arrives during 
     * the among of time specified, the method throws a Runtime Exception.
     * @param arg0 : POJO instance asking for  the service
     * @param arg1 : field name
     * @param arg2 : previous value
     * @return the object to inject.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public synchronized Object onGet(Object arg0, String arg1, Object arg2) {
        ServiceReference[] refs = getServiceReferences();
        if (refs != null) {
            // Immediate return.
            if (isAggregate()) {
                Object[] svc = (Object[]) Array.newInstance(getSpecification(), refs.length);
                for (int i = 0; i < svc.length; i++) {
                    svc[i] = getService(refs[i]);
                }
                return svc;
            } else {
                return getService(refs[0]);
            }
        } else {
            // Begin to wait ...
            long enter = System.currentTimeMillis();
            boolean exhausted = false;
            ServiceReference ref = null;
            synchronized (this) {
                while (getServiceReference() == null && ! exhausted) {
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        // We was interrupted ....
                    } finally {
                        long end = System.currentTimeMillis(); 
                        exhausted = (end - enter) > m_timeout;
                    }
                }
            }
            // Check 
            if (exhausted) {
                // Timeout, throw an exception
                throw new RuntimeException("Service " + getSpecification().getName() + " unavailable : timeout");
            } else {
                ref = getServiceReference();
                if (isAggregate()) {
                    Object[] svc = (Object[]) Array.newInstance(getSpecification(), 1);
                    svc[0] = ref;
                    return svc[0];
                } else {
                    return getService(ref);
                }
            }
        }
    }

    /**
     * The monitored field receives a value.
     * Nothing to do.
     * @param arg0 : POJO setting the value.
     * @param arg1 : field name
     * @param arg2 : received value
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void onSet(Object arg0, String arg1, Object arg2) { }

}
