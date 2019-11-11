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

import static java.util.Optional.ofNullable;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.api.InterceptorBinding;
import org.apache.karaf.service.interceptor.impl.runtime.PropertiesManager;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

public class InterceptedServiceRegistry implements ServiceListener {
    private final PropertiesManager propertiesManager;
    private final Consumer<ServiceReference<?>> onServiceAddition;
    private final Consumer<ServiceReference<?>> onServiceRemoval;
    private final Map<ServiceReference<?>, RegistrationState> references = new ConcurrentHashMap<>();

    public InterceptedServiceRegistry(final Consumer<ServiceReference<?>> onServiceAddition,
                                      final Consumer<ServiceReference<?>> onServiceRemoval,
                                      final PropertiesManager propertiesManager) {
        this.onServiceAddition = onServiceAddition;
        this.onServiceRemoval = onServiceRemoval;
        this.propertiesManager = propertiesManager;
    }

    @Override
    public void serviceChanged(final ServiceEvent serviceEvent) {
        final ServiceReference<?> ref = serviceEvent.getServiceReference();
        switch (serviceEvent.getType()) {
            case ServiceEvent.REGISTERED:
                doRegister(ref);
                break;
            case ServiceEvent.MODIFIED_ENDMATCH:
            case ServiceEvent.UNREGISTERING:
                doRemove(ref);
                break;
            case ServiceEvent.MODIFIED:
                ofNullable(references.get(ref))
                        .filter(reg -> didChange(ref, reg))
                        .ifPresent(reg -> {
                            doRemove(ref);
                            doRegister(ref);
                        });
            default:
        }
    }

    private boolean didChange(final ServiceReference<?> ref, final RegistrationState reg) {
        return !reg.registrationProperties.equals(propertiesManager.collectProperties(ref)) ||
                !reg.bindingsPerMethod.equals(computeBindings(ref));
    }

    private void doRegister(final ServiceReference<?> ref) {
        references.put(ref, new RegistrationState(propertiesManager.collectProperties(ref), computeBindings(ref)));
        onServiceAddition.accept(ref);
    }

    private void doRemove(final ServiceReference<?> ref) {
        onServiceRemoval.accept(ref);
        references.remove(ref);
    }

    private Map<Method, List<Class<?>>> computeBindings(final ServiceReference<?> ref) {
        final List<Class<?>> types = propertiesManager.unflattenStringValues(ref.getProperty(Constants.OBJECTCLASS))
                .map(it -> {
                    try {
                        return ref.getBundle().loadClass(it);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .distinct()
                .collect(toList());
        final Collection<Annotation> globalInterceptors = types.stream()
                .flatMap(type -> Stream.of(type.getAnnotations()))
                .filter(methodAnnotation -> methodAnnotation.annotationType().isAnnotationPresent(InterceptorBinding.class))
                .distinct()
                .collect(toList());
        return types.stream()
                .flatMap(type -> Stream.of(type.getMethods()))
                .collect(toMap(identity(), m -> Stream.concat(
                        globalInterceptors.stream(),
                        Stream.of(m.getAnnotations()))
                        .filter(methodAnnotation -> methodAnnotation.annotationType().isAnnotationPresent(InterceptorBinding.class))
                        .distinct()
                        .map(Annotation::annotationType) // todo: keep Annotation with values
                        .collect(toList())));
    }

    public <T> Stream<Class<?>> getBindings(final ServiceReference<T> ref) {
        return ofNullable(references.get(ref))
                .map(reg -> reg.bindingsPerMethod.values().stream().flatMap(Collection::stream).distinct())
                .orElseGet(Stream::empty);
    }

    public <T> Map<Method, List<Class<?>>> getInterceptorsPerMethod(final ServiceReference<T> ref) {
        return ofNullable(references.get(ref))
                .map(reg -> reg.bindingsPerMethod)
                .orElseGet(Collections::emptyMap);
    }

    private static class RegistrationState {
        private final Hashtable<String, Object> registrationProperties;
        private final Map<Method, List<Class<?>>> bindingsPerMethod;

        private RegistrationState(final Hashtable<String, Object> registrationProperties,
                                  final Map<Method, List<Class<?>>> bindingsPerMethod) {
            this.registrationProperties = registrationProperties;
            this.bindingsPerMethod = bindingsPerMethod;
        }
    }
}
