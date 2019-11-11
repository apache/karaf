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
package org.apache.karaf.service.interceptor.impl.runtime;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.impl.runtime.proxy.ProxyFactory;
import org.apache.karaf.service.interceptor.impl.runtime.registry.InterceptedServiceRegistry;
import org.apache.karaf.service.interceptor.impl.runtime.registry.InterceptorRegistry;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class ProxiesManager {
    private final ProxyFactory proxyFactory;
    private final PropertiesManager propertiesManager;
    private final InterceptorRegistry interceptors;
    private final InterceptedServiceRegistry services;

    private final Map<ServiceReference<?>, ServiceRegistration<?>> registrationPerReference = new ConcurrentHashMap<>();
    private final Map<ServiceReference<?>, List<Class<?>>> bindingPerReference = new ConcurrentHashMap<>();
    private final Map<Class<?>, Collection<ServiceReference<?>>> referencesPerBinding = new ConcurrentHashMap<>();

    public ProxiesManager(final InterceptorRegistry interceptorRegistry,
                          final InterceptedServiceRegistry services,
                          final ProxyFactory proxyFactory,
                          final PropertiesManager propertiesManager) {
        this.interceptors = interceptorRegistry;
        this.services = services;
        this.proxyFactory = proxyFactory;
        this.propertiesManager = propertiesManager;
    }

    // check out all services not yet proxied which can now be proxied and register the proxy
    public void onInterceptorAddition(final Class<?> bindingClass) {
        ofNullable(referencesPerBinding.get(bindingClass))
                .ifPresent(references -> references.stream()
                        .filter(ref -> !registrationPerReference.containsKey(ref)) // already proxied so skip
                        .filter(ref -> ofNullable(bindingPerReference.get(ref))
                                .map(b -> interceptors.areBindingsAvailable(b.stream()))
                                .orElse(false))
                        .forEach(ref -> registrationPerReference.put(ref, registerProxy(ref))));
    }

    // remove registered proxies since one of the interceptor is no more available
    public void onInterceptorRemoval(final Class<?> bindingClass) {
        ofNullable(referencesPerBinding.get(bindingClass))
                .ifPresent(references -> references.stream()
                        .filter(registrationPerReference::containsKey)
                        .forEach(ref -> ofNullable(registrationPerReference.remove(ref))
                                .ifPresent(ServiceRegistration::unregister)));
    }

    public <T> void onInterceptedInstanceAddition(final ServiceReference<T> ref) {
        final List<Class<?>> bindings = toBindings(ref).collect(toList());
        bindings.forEach(binding -> referencesPerBinding.computeIfAbsent(binding, k -> new CopyOnWriteArraySet<>()).add(ref));
        bindingPerReference.put(ref, bindings);
        if (interceptors.areBindingsAvailable(bindings.stream())) {
            registrationPerReference.put(ref, registerProxy(ref));
        }
    }

    public <T> void onInterceptedInstanceRemoval(final ServiceReference<T> ref) {
        toBindings(ref).filter(referencesPerBinding::containsKey).forEach(binding -> {
            final Collection<ServiceReference<?>> refs = referencesPerBinding.get(binding);
            refs.remove(ref);
            if (refs.isEmpty()) {
                referencesPerBinding.remove(binding);
            }
        });
        bindingPerReference.remove(ref);
        ofNullable(registrationPerReference.remove(ref))
                .ifPresent(ServiceRegistration::unregister);
    }

    private <T> Stream<? extends Class<?>> toBindings(final ServiceReference<T> ref) {
        return services.getBindings(ref);
    }

    private <T> ServiceRegistration<?> registerProxy(final ServiceReference<T> ref) {
        final BundleContext context = ref.getBundle().getBundleContext();
        final Object classProperty = ref.getProperty(Constants.OBJECTCLASS);
        final List<Class<?>> classes = Stream.of(classProperty)
                .flatMap(propertiesManager::unflattenStringValues)
                .map(it -> {
                    try {
                        return context.getBundle().loadClass(it);
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(toList());

        // drop interceptors property to let it be forwarded
        final Hashtable<String, Object> properties = propertiesManager.collectProperties(ref);
        final T proxy = proxyFactory.create(
                ref, classes,
                interceptors.getInterceptors(bindingPerReference.get(ref)),
                services.getInterceptorsPerMethod(ref));
        return context.registerService(classes.stream().map(Class::getName).toArray(String[]::new), proxy, properties);
    }

    public void stop() {
        registrationPerReference.values().forEach(ServiceRegistration::unregister);
        bindingPerReference.clear();
        referencesPerBinding.clear();
        registrationPerReference.clear();
    }
}
