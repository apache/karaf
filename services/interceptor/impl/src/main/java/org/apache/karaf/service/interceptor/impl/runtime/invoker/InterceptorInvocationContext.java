/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.service.interceptor.impl.runtime.invoker;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.service.interceptor.api.InvocationContext;
import org.apache.karaf.service.interceptor.impl.runtime.Exceptions;
import org.apache.karaf.service.interceptor.impl.runtime.hook.InterceptorInstance;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class InterceptorInvocationContext<T> implements InvocationContext {
    private final ServiceReference<T> interceptedReference;
    private final Method method;
    private final List<InterceptorInstance<?>> interceptors;

    private T target;
    private Map<String, Object> contextData;
    private Object[] parameters;
    private int index;

    public InterceptorInvocationContext(final ServiceReference<T> reference,
                                        final List<InterceptorInstance<?>> interceptors,
                                        final Method method, final Object[] parameters) {
        this.interceptedReference = reference;
        this.method = method;
        this.parameters = parameters;
        this.interceptors = interceptors;
    }

    @Override
    public Object proceed() throws Exception {
        try {
            if (index < interceptors.size()) {
                final InterceptorInstance<?> interceptor = interceptors.get(index++);
                try {
                    return interceptor.intercept(this);
                } catch (final Exception e) {
                    index--;
                    throw e;
                }
            }
            try {
                return getMethod().invoke(getTarget(), getParameters());
            } catch (final InvocationTargetException ite) {
                return Exceptions.unwrap(ite);
            }
        } finally {
            if (target != null) { // todo: check scope and optimize it?
                interceptedReference.getBundle().getBundleContext().ungetService(interceptedReference);
            }
        }
    }

    @Override
    public T getTarget() {
        final BundleContext context = interceptedReference.getBundle().getBundleContext();
        target = context.getService(interceptedReference);
        if (target == null) {
            throw new IllegalStateException("service no more available (" + interceptedReference + ")");
        }
        return target;
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public Object[] getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(final Object[] parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, Object> getContextData() {
        if (contextData == null) {
            contextData = new HashMap<>();
        }
        return contextData;
    }
}
