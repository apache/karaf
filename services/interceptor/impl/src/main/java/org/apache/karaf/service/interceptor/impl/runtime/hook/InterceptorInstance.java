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
package org.apache.karaf.service.interceptor.impl.runtime.hook;

import static java.util.stream.Collectors.toList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.api.AroundInvoke;
import org.apache.karaf.service.interceptor.api.InvocationContext;
import org.apache.karaf.service.interceptor.impl.runtime.Exceptions;
import org.apache.karaf.service.interceptor.impl.runtime.PropertiesManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class InterceptorInstance<T> {
    private final ServiceReference<T> reference;
    private final BundleContext context;
    private final Method method;
    private final Class<?> binding;

    public InterceptorInstance(final ServiceReference<T> reference, final Class<?> binding, final PropertiesManager propertiesManager) {
        this.reference = reference;
        this.context = reference.getBundle().getBundleContext();
        this.method = propertiesManager.unflattenStringValues(reference.getProperty(Constants.OBJECTCLASS))
            .map(this::findAroundInvoke)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
        this.binding = binding;
    }

    public Class<?> getBinding() {
        return binding;
    }

    public Object intercept(final InvocationContext invocationContext) throws Exception {
        final T service = context.getService(reference);
        if (service == null) {
            throw new IllegalStateException("'" + reference + "' no more available");
        }
        try {
            return method == null ? invocationContext.proceed() : method.invoke(service, invocationContext);
        } catch (final InvocationTargetException ite) {
            return Exceptions.unwrap(ite);
        } finally {
            context.ungetService(reference);
        }
    }

    private Method findAroundInvoke(final String clazz) {
        try {
            final List<Method> interceptingMethods = Stream.of(context.getBundle().loadClass(clazz))
                    .flatMap(c -> Stream.of(c.getMethods()))
                    .filter(m -> m.isAnnotationPresent(AroundInvoke.class))
                    .collect(toList());
            switch (interceptingMethods.size()) {
                case 0: // we can add @AroundConstruct later so let's already tolerate that
                    return null;
                case 1:
                    return interceptingMethods.iterator().next();
                default:
                    throw new IllegalArgumentException("'" + clazz + "' must have a single @AroundInvoke method, found " + interceptingMethods);
            }
        } catch (final ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }
}

