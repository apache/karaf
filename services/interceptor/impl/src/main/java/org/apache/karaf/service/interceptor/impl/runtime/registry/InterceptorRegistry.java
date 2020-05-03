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
package org.apache.karaf.service.interceptor.impl.runtime.registry;

import static java.util.stream.Collectors.toList;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.api.InterceptorBinding;
import org.apache.karaf.service.interceptor.impl.runtime.PropertiesManager;
import org.apache.karaf.service.interceptor.impl.runtime.hook.InterceptorInstance;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

public class InterceptorRegistry implements ServiceListener {
    private final Consumer<Class<?>> onAddition;
    private final Consumer<Class<?>> onRemoval;
    private final PropertiesManager propertiesManager;
    private final Map<Class<?>, InterceptorInstance<?>> interceptors = new ConcurrentHashMap<>();

    public InterceptorRegistry(final Consumer<Class<?>> onAddition,
                               final Consumer<Class<?>> onRemoval,
                               final PropertiesManager propertiesManager) {
        this.onAddition = onAddition;
        this.onRemoval = onRemoval;
        this.propertiesManager = propertiesManager;
    }

    public boolean areBindingsAvailable(final Stream<Class<?>> bindings) {
        return bindings.allMatch(binding -> binding != null && interceptors.containsKey(binding));
    }

    public List<InterceptorInstance<?>> getInterceptors(final List<Class<?>> bindings) {
        return bindings.stream().map(interceptors::get).distinct().collect(toList());
    }

    @Override
    public void serviceChanged(final ServiceEvent serviceEvent) {
        final Class<? extends Annotation> bindingClass = getInterceptorBinding(serviceEvent);
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED: {
                interceptors.put(bindingClass, new InterceptorInstance<>(
                        serviceEvent.getServiceReference(), bindingClass, propertiesManager));
                onAddition.accept(bindingClass);
                break;
            }
            case ServiceEvent.MODIFIED_ENDMATCH:
            case ServiceEvent.UNREGISTERING: {
                interceptors.remove(bindingClass);
                onRemoval.accept(bindingClass);
                break;
            }
            case ServiceEvent.MODIFIED:
            default:
        }
    }

    private Class<? extends Annotation> getInterceptorBinding(final ServiceEvent serviceEvent) {
        final List<Annotation> bindings = propertiesManager.unflattenStringValues(serviceEvent.getServiceReference().getProperty(Constants.OBJECTCLASS))
                .map(it -> {
                    try {
                        return serviceEvent.getServiceReference().getBundle().loadClass(it);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .flatMap(it -> Stream.of(it.getAnnotations()))
                .filter(it -> it.annotationType().isAnnotationPresent(InterceptorBinding.class))
                .distinct()
                .collect(toList());
        if (bindings.size() != 1) {
            throw new IllegalArgumentException("A single @InterceptorBinding on " + serviceEvent + " is required, found: " + bindings);
        }
        // todo: keep annotation instance to support binding values?
        return bindings.iterator().next().annotationType();
    }
}
