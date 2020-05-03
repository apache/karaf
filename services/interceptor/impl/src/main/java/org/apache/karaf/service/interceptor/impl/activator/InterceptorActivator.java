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
package org.apache.karaf.service.interceptor.impl.activator;

import static java.util.Optional.ofNullable;
import static org.apache.karaf.service.interceptor.impl.runtime.ComponentProperties.INTERCEPTORS_PROPERTY;
import static org.apache.karaf.service.interceptor.impl.runtime.ComponentProperties.INTERCEPTOR_PROPERTY;

import java.util.Hashtable;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.impl.runtime.PropertiesManager;
import org.apache.karaf.service.interceptor.impl.runtime.ProxiesManager;
import org.apache.karaf.service.interceptor.impl.runtime.hook.InterceptedInstancesHooks;
import org.apache.karaf.service.interceptor.impl.runtime.proxy.ProxyFactory;
import org.apache.karaf.service.interceptor.impl.runtime.registry.InterceptedServiceRegistry;
import org.apache.karaf.service.interceptor.impl.runtime.registry.InterceptorRegistry;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;

public class InterceptorActivator implements BundleActivator {
    private InterceptorRegistry interceptorRegistry;
    private InterceptedServiceRegistry interceptedServiceRegistry;
    private ProxiesManager proxiesManager;

    private ServiceRegistration<?> hooksRegistration;

    @Override
    public void start(final BundleContext context) throws InvalidSyntaxException {
        final PropertiesManager propertiesManager = new PropertiesManager();
        // todo: decouple these three services with a bus? here we use the activator to keep it simple
        interceptedServiceRegistry = new InterceptedServiceRegistry(this::onServiceAddition, this::onServiceRemoval, propertiesManager);
        interceptorRegistry = new InterceptorRegistry(this::onInterceptorAddition, this::onInterceptorRemoval, propertiesManager);
        proxiesManager = new ProxiesManager(interceptorRegistry, interceptedServiceRegistry, new ProxyFactory(), propertiesManager);

        // listen for interceptors and intercepted instances to be able to react on (un)registrations
        context.addServiceListener(interceptedServiceRegistry, "(" + INTERCEPTORS_PROPERTY + "=true)");
        context.addServiceListener(interceptorRegistry, "(" + INTERCEPTOR_PROPERTY + "=true)");

        // register existing services/interceptors
        ofNullable(context.getAllServiceReferences(null, "(" + INTERCEPTORS_PROPERTY + "=true)"))
                .ifPresent(refs -> Stream.of(refs).forEach(ref -> interceptedServiceRegistry.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref))));
        ofNullable(context.getAllServiceReferences(null, "(" + INTERCEPTOR_PROPERTY + "=true)"))
                .ifPresent(refs -> Stream.of(refs).forEach(ref -> interceptorRegistry.serviceChanged(new ServiceEvent(ServiceEvent.REGISTERED, ref))));

        // ensure we filter out the proxied services to only return proxies
        hooksRegistration = context.registerService(
                new String[]{FindHook.class.getName(), EventListenerHook.class.getName()},
                new InterceptedInstancesHooks(context.getBundle().getBundleId()),
                new Hashtable<>());
    }

    @Override
    public void stop(final BundleContext context) {
        context.removeServiceListener(interceptorRegistry);
        context.removeServiceListener(interceptedServiceRegistry);
        hooksRegistration.unregister();
        proxiesManager.stop();
    }

    private void onServiceAddition(final ServiceReference<?> ref) {
        proxiesManager.onInterceptedInstanceAddition(ref);
    }

    private void onServiceRemoval(final ServiceReference<?> ref) {
        proxiesManager.onInterceptedInstanceRemoval(ref);
    }

    private void onInterceptorAddition(final Class<?> aClass) {
        proxiesManager.onInterceptorAddition(aClass);
    }

    private void onInterceptorRemoval(final Class<?> aClass) {
        proxiesManager.onInterceptorRemoval(aClass);
    }
}
