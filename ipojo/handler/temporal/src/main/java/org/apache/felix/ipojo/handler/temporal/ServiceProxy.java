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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.osgi.framework.ServiceReference;

/**
 * Wrapper in front of a temporal service dependencies.
 * This proxy can be used by collaborators. Service lookup 
 * and on timeout policies are executed when a method is invoked.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceProxy implements InvocationHandler {
    
    /**
     * The wrapped temporal dependency. 
     */
    private TemporalDependency m_dependency;
    
    /**
     * Creates a Service Proxy.
     * @param dep the wrapped temporal dependency
     */
    public ServiceProxy(TemporalDependency dep) {
        m_dependency = dep;
    }

    /**
     * Intercept a method invocation.
     * This method looks for a service provider, wait for timeout and
     * depending on the lookup result either call the method
     * on a service object or executed the on timeout policy.
     * In this latter case, this methods can throw a {@link RuntimeException},
     * throws a {@link NullPointerException} (null policy) or invokes the 
     * method on a nullable/default-implementation object.
     * @param proxy the proxy on which the method is invoked
     * @param method the invoked method
     * @param args the arguments
     * @return the invocation result.
     * @throws Exception an exception occurs either during the invocation or 
     * is the result to the on timeout policy.
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(Object proxy, Method method, Object[] args)
        throws Exception {
        ServiceReference ref = m_dependency.getServiceReference();
        if (ref != null) {
            // Immediate return.
            return invokeOperation(ref, method, args); // Invoke the method on the service object.
        } else {
            // Begin to wait ...
            long enter = System.currentTimeMillis();
            boolean exhausted = false;
            synchronized (this) {
                while (m_dependency.getServiceReference() == null && !exhausted) {
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        // We was interrupted ....
                    } finally {
                        long end = System.currentTimeMillis();
                        exhausted = (end - enter) > m_dependency.getTimeout();
                    }
                }
            }
            // Check
            if (exhausted) {
                Object oto = m_dependency.onTimeout(); // Throws the RuntimeException
                if (oto == null) { // If null, return null
                    throw new NullPointerException("No service object available"); // throws an NPE in this case.
                } else {
                    // invoke the method on the returned object.
                    return invokeOperation(oto, method, args);
                }
            } else {
                ref = m_dependency.getServiceReference();
                return invokeOperation(ref, method, args); // Invoke the method on the service object.
            }
        }
    }

    /**
     * Helper method invoking the given method, with the given
     * argument on the given object.
     * @param svc the service object
     * @param method the method object
     * @param args the arguments
     * @return the invocation result
     * @throws Exception occurs when an issue happen during method invocation.
     */
    private Object invokeOperation(Object svc, Method method, Object[] args) 
        throws Exception {
        return method.invoke(svc, args);
    }
    
    /**
     * Helper method invoking the given method, with the given
     * argument on the given service reference. This methods gets 
     * the service object and then invokes the method.
     * @param ref the service reference
     * @param method the method object
     * @param args the arguments
     * @return the invocation result
     * @throws Exception occurs when an issue happen during method invocation.
     */
    private Object invokeOperation(ServiceReference ref, Method method, Object[] args) 
        throws Exception {
        Object svc = m_dependency.getService(ref);
        return method.invoke(svc, args);
    }

}
