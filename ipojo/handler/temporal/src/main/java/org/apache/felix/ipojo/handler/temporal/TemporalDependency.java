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
import java.lang.reflect.Proxy;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.Nullable;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.handlers.dependency.NullableObject;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Temporal dependency. A temporal dependency waits (block) for the availability
 * of the service. If no provider arrives in the specified among of time, a
 * runtime exception is thrown.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TemporalDependency extends DependencyModel implements
        FieldInterceptor {

    /**
     * The timeout.
     */
    private long m_timeout;

    /**
     * The default implementation.
     */
    private String m_di;

    /**
     * The {@link Nullable} object or Default-Implementation instance if used.
     */
    private Object m_nullableObject;

    /**
     * The handler managing this dependency.
     */
    private PrimitiveHandler m_handler;

    /**
     * The timeout policy. Null injects null, {@link Nullable} injects a nullable object or
     * an array with a nullable object, Default-Implementation injects an object
     * created from the specified injected implementation or an array with it
     * Empty array inject an empty array (must be an aggregate dependency) No
     * policy (0) throw a runtime exception when the timeout occurs *
     */
    private int m_policy;

    /**
     * Constructor.
     * @param spec service specification
     * @param agg is the dependency aggregate ?
     * @param filter LDAP filter
     * @param context service context
     * @param timeout timeout
     * @param handler Handler managing this dependency
     * @param defaultImpl class used as default-implementation
     * @param policy onTimeout policy
     */
    public TemporalDependency(Class spec, boolean agg, Filter filter,
            BundleContext context, long timeout, int policy,
            String defaultImpl, TemporalHandler handler) {
        super(spec, agg, true, filter, null,
                DependencyModel.DYNAMIC_BINDING_POLICY, context, handler);
        m_di = defaultImpl;
        m_policy = policy;
        m_timeout = timeout;
        m_handler = handler;
    }

    /**
     * The dependency has been reconfigured.
     * @param arg0 new service references
     * @param arg1 old service references
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[],
     *      org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] arg0,
            ServiceReference[] arg1) {
        throw new UnsupportedOperationException(
                "Reconfiguration not yet supported");
    }

    /**
     * A provider arrives.
     * @param arg0 service reference of the new provider.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference arg0) {
        // Notify if a thread is waiting.
        synchronized (this) {
            notifyAll();
        }
    }

    /**
     * A provider leaves. Nothing to do.
     * @param arg0 leaving service references.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public synchronized void onServiceDeparture(ServiceReference arg0) {
    }

    /**
     * The code require a value of the monitored field. If providers are
     * available, the method return service object(s) immediately. Else, the
     * thread is blocked until an arrival. If no provider arrives during the
     * among of time specified, the method throws a Runtime Exception.
     * @param arg0 POJO instance asking for the service
     * @param arg1 field name
     * @param arg2 previous value
     * @return the object to inject.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public synchronized Object onGet(Object arg0, String arg1, Object arg2) {
        ServiceReference[] refs = getServiceReferences();
        if (refs != null) {
            // Immediate return.
            if (isAggregate()) {
                Object[] svc = (Object[]) Array.newInstance(getSpecification(),
                        refs.length);
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
                while (getServiceReference() == null && !exhausted) {
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
                return onTimeout();
            } else {
                ref = getServiceReference();
                if (isAggregate()) {
                    Object[] svc = (Object[]) Array.newInstance(
                            getSpecification(), 1);
                    svc[0] = getService(ref);
                    return svc;
                } else {
                    return getService(ref);
                }
            }
        }
    }

    /**
     * Start method. Initializes the nullable object.
     * @see org.apache.felix.ipojo.util.DependencyModel#start()
     */
    public void start() {
        super.start();
        switch (m_policy) {
            case TemporalHandler.NULL:
                m_nullableObject = null;
                break;
            case TemporalHandler.NULLABLE:
                // To load the proxy we use the POJO class loader. Indeed, this
                // classloader imports iPOJO (so can access to Nullable) and has
                // access to the service specification.
                try {
                    m_nullableObject = Proxy.newProxyInstance(m_handler
                            .getInstanceManager().getClazz().getClassLoader(),
                            new Class[] { getSpecification(), Nullable.class },
                            new NullableObject()); // NOPMD
                    if (isAggregate()) {
                        Object[] array = (Object[]) Array.newInstance(
                                getSpecification(), 1);
                        array[0] = m_nullableObject;
                        m_nullableObject = array;
                    }
                } catch (NoClassDefFoundError e) {
                    // A NoClassDefFoundError is thrown if the specification
                    // uses a
                    // class not accessible by the actual instance.
                    // It generally comes from a missing import.
                    throw new IllegalStateException(
                            "Cannot create the Nullable object, a referenced class cannot be loaded: "
                                    + e.getMessage());
                }

                break;
            case TemporalHandler.DEFAULT_IMPLEMENTATION:
                // Create the default-implementation object.
                try {
                    Class clazz = m_handler.getInstanceManager().getContext()
                            .getBundle().loadClass(m_di);
                    m_nullableObject = clazz.newInstance();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                }
                if (isAggregate()) {
                    Object[] array = (Object[]) Array.newInstance(
                            getSpecification(), 1);
                    array[0] = m_nullableObject;
                    m_nullableObject = array;
                }
                break;
            case TemporalHandler.EMPTY_ARRAY:
                m_nullableObject = Array.newInstance(getSpecification(), 0);
                break;
            default: // Cannot occurs
                break;
        }
    }

    /**
     * Stop method. Just releases the reference on the nullable object.
     * @see org.apache.felix.ipojo.util.DependencyModel#stop()
     */
    public void stop() {
        super.stop();
        m_nullableObject = null;
    }

    /**
     * The monitored field receives a value. Nothing to do.
     * @param arg0 POJO setting the value.
     * @param arg1 field name
     * @param arg2 received value
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void onSet(Object arg0, String arg1, Object arg2) {
    }

    /**
     * Implements the timeout policy according to the specified configuration.
     * @return the object to return when the timeout occurs.
     */
    private Object onTimeout() {
        switch (m_policy) {
            case TemporalHandler.NULL:
            case TemporalHandler.NULLABLE:
            case TemporalHandler.DEFAULT_IMPLEMENTATION:
            case TemporalHandler.EMPTY_ARRAY:
                return m_nullableObject;
            default:
                // Throws a runtime exception
                throw new RuntimeException("Service "
                        + getSpecification().getName()
                        + " unavailable : timeout");
        }
    }

}
