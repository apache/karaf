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

package org.apache.felix.ipojo.handlers.providedservice.strategy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.IPOJOServiceFactory;

/**
 * This proxy class is here to wrap an iPOJO ServiceFactory.
 * If the consumer of this service do not call the getService or ungetService
 * methods, it will get an Exception with an explicit error message telling
 * him that this service is only usable through iPOJO.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ErrorPrintingServiceFactoryProxy implements InvocationHandler {

    /**
     * getService(ComponentInstance) method.
     */
    private static Method GET_METHOD;

    /**
     * ungetService(ComponentInstance, Object) method.
     */
    private static Method UNGET_METHOD;

    /**
     * Wrapped factory.
     */
    private final IPOJOServiceFactory m_factory;

    static {
        // Static initialization of theses constants.
        try {
            GET_METHOD = IPOJOServiceFactory.class.getMethod("getService",
                                                             new Class[]{ComponentInstance.class});
            UNGET_METHOD = IPOJOServiceFactory.class.getMethod("ungetService",
                                                               new Class[]{ComponentInstance.class, Object.class});
        } catch (Exception e) {
            // Should never happen
        }
    }

    /**
     * Wraps a ServiceFactory in an InvocationHandler that will delegate only
     * get/ungetService methods to the factory. All other methods will be
     * rejected with a meaningful error message.
     *
     * @param factory delegating iPOJO ServiceFactory
     */
    public ErrorPrintingServiceFactoryProxy(final IPOJOServiceFactory factory) {
        this.m_factory = factory;
    }

    /**
     * 'Invoke' methods called when a method is called on the proxy.
     * @param proxy the proxy
     * @param method the method
     * @param args the arguments
     * @return the result
     * @throws Exception if something wrong happens
     * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public Object invoke(final Object proxy,
                         final Method method,
                         final Object[] args) throws Exception {

        // Handle get/unget operations
        if (GET_METHOD.equals(method)) {
            return m_factory.getService((ComponentInstance) args[0]);
        }
        if (UNGET_METHOD.equals(method)) {
            m_factory.ungetService((ComponentInstance) args[0], args[1]);
            return null;
        }

        // All other methods are rejected
        throw new UnsupportedOperationException("This service requires an advanced creation policy. "
                        + "Before calling the service, call the IPOJOServiceFactory.getService(ComponentInstance) "
                        + "method to get the service object. ");

    }

}
