/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.impl.dependencies;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.dependencies.TemporalServiceDependency;
import org.apache.felix.dm.impl.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
* Temporal Service dependency implementation.
* (see javadoc in {@link TemporalServiceDependency}).
*/
public class TemporalServiceDependencyImpl extends ServiceDependencyImpl implements TemporalServiceDependency, InvocationHandler {
    // Max millis to wait for service availability.
    private long m_timeout = 30000;

    /**
     * Creates a new Temporal Service Dependency.
     * 
     * @param context The bundle context of the bundle which is instantiating this dependency object
     * @param logger the logger our Internal logger for logging events.
     * @see DependencyActivatorBase#createTemporalServiceDependency()
     */
    public TemporalServiceDependencyImpl(BundleContext context, Logger logger) {
        super(context, logger);
        super.setRequired(true);
    }

    /**
     * Sets the timeout for this temporal dependency. Specifying a timeout value of zero means that there is no timeout period,
     * and an invocation on a missing service will fail immediately.
     * 
     * @param timeout the dependency timeout value greater or equals to 0
     * @throws IllegalArgumentException if the timeout is negative
     * @return this temporal dependency
     */
    public TemporalServiceDependency setTimeout(long timeout) {
        if (timeout < 0) {
            throw new IllegalArgumentException("Invalid timeout value: " + timeout);
        }
        m_timeout = timeout;
        return this;
    }

    /**
     * The ServiceTracker calls us here in order to inform about a service arrival.
     */
    public synchronized void addedService(ServiceReference ref, Object service) {
        boolean makeAvailable = makeAvailable();
        if (makeAvailable) {
            m_serviceInstance = Proxy.newProxyInstance(m_trackedServiceName.getClassLoader(), new Class[] { m_trackedServiceName }, this);
        }
        Object[] services = m_services.toArray();
        for (int i = 0; i < services.length; i++) {
            DependencyService ds = (DependencyService) services[i];
            if (makeAvailable) {
                ds.dependencyAvailable(this);
            }
        }
        if (!makeAvailable) {
            notifyAll();
        }
    }

    /**
     * The ServiceTracker calls us here when a tracked service properties are modified.
     */
    public void modifiedService(ServiceReference ref, Object service) {
        // We don't care.
    }

    /**
     * The ServiceTracker calls us here when a tracked service is lost.
     */
    public synchronized void removedService(ServiceReference ref, Object service) {
        // Unget what we got in addingService (see ServiceTracker 701.4.1)
        m_context.ungetService(ref);
    }

    /**
     * @returns our dependency instance. Unlike in ServiceDependency, we always returns our proxy.
     */
    public synchronized Object getService() {
        return m_serviceInstance;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object service = m_tracker.getService();
        if (service == null) {
            synchronized (this) {
                long start = System.currentTimeMillis();
                long waitTime = m_timeout;
                while (service == null) {
                    if (waitTime <= 0) {
                        throw new IllegalStateException("Service unavailable: " + m_trackedServiceName.getName());
                    }
                    try {
                        wait(waitTime);
                    }
                    catch (InterruptedException e) {
                        throw new IllegalStateException("Service unavailable: " + m_trackedServiceName.getName());
                    }
                    waitTime = m_timeout - (System.currentTimeMillis() - start);
                    service = m_tracker.getService();
                }
            }
        }
        try {
            return method.invoke(service, args);
        }
        catch (IllegalAccessException iae) {
            method.setAccessible(true);
            return method.invoke(service, args);
        }
    }
}
