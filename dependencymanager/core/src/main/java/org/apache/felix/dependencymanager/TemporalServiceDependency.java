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
package org.apache.felix.dependencymanager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
* A Temporal Service dependency that can block the caller thread between service updates. Only
* useful for required stateless dependencies that can be replaced transparently.
* A Dynamic Proxy is used to wrap the actual service dependency. When the dependency goes 
* away, an attempt is made to replace it with another one which satisfies the service dependency 
* criteria. If no service replacement is available, then any method invocation (through the 
* dynamic proxy) will block during a configurable timeout. On timeout, an unchecked ServiceUnavailable 
* exception is raised (but the service is not deactivated).<p>
* 
* When an OSGi update takes place, we use the following locking strategy: A Read/Write lock is used
* to synchronize the updating thread with respect to the other threads which invoke the backed service. 
* The Updating thread uses an exclusive Write lock and service invokers uses a Read lock. This model
* allows multiple threads to invoke the backed service concurrently, but the updating thread won't
* update the dependency if it's currently in use.<p>
* 
* <b>This class only supports required dependencies, and temporal dependencies must be accessed outside
* the Activator (OSGi) thread, because method invocations may block the caller thread when dependencies
* are not satisfied.
* </b>
*
* <p> Sample Code:<p>
* <blockquote>
* 
* <pre>
* import org.apache.felix.dependencymanager.*;
* 
* public class Activator extends DependencyActivatorBase {
*   public void init(BundleContext ctx, DependencyManager dm) throws Exception {
*     dm.add(createService()
*            .setImplementation(MyServer.class)
*        .add(createTemporalServiceDependency()
*            .setService(MyDependency.class)
*            .setTimeout(15000)));
*   }
* 
*   public void destroy(BundleContext ctx, DependencyManager dm) throws Exception {
*   }
* }
* 
* class MyServer implements Runnable {
*   MyDependency _dependency; // Auto-Injected by reflection.
*   void start() {
*     (new Thread(this)).start();
*   }
*   
*   public void run() {
*     try {
*       _dependency.doWork();
*     } catch (ServiceUnavailableException e) {
*       t.printStackTrace();
*     }
*   }   
* </pre>
* 
* </blockquote>
*/
public class TemporalServiceDependency extends ServiceDependency implements InvocationHandler {
    // Max millis to wait for service availability.
    private long m_timeout = 30000;

    /**
     * Creates a new Temporal Service Dependency.
     * 
     * @param context The bundle context of the bundle which is instantiating this dependency object
     * @param logger the logger our Internal logger for logging events.
     * @see DependencyActivatorBase#createTemporalServiceDependency()
     */
    public TemporalServiceDependency(BundleContext context, Logger logger) {
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
        if (makeAvailable()) {
            // So far, our dependency was not satisfied: wrap the service behind our proxy.
            m_serviceInstance = Proxy.newProxyInstance(m_trackedServiceName.getClassLoader(), new Class[] { m_trackedServiceName }, this);
            m_service.dependencyAvailable(this); // will invoke "added" callbacks, if any.
        }
        else {
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
        method.setAccessible(true);
        return method.invoke(service, args);
    }
}
