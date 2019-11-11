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
package org.apache.karaf.service.interceptor.impl.runtime.proxy;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.security.ProtectionDomain;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.apache.karaf.service.interceptor.impl.runtime.hook.InterceptorInstance;
import org.apache.karaf.service.interceptor.impl.runtime.invoker.InterceptorInvocationContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;

public class ProxyFactory {

    private static final Class<?>[] EMPTY_CLASSES = new Class<?>[0];

    public <T> T create(final ServiceReference<T> ref, final List<Class<?>> classes,
                        final List<InterceptorInstance<?>> interceptors,
                        final Map<Method, List<Class<?>>> interceptorsPerMethod) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("Can't proxy an empty list of type: " + ref);
        }

        final Map<Method, List<InterceptorInstance<?>>> interceptorInstancePerMethod = interceptorsPerMethod.entrySet().stream()
            .collect(toMap(Map.Entry::getKey, m -> m.getValue().stream()
                    .map(binding -> interceptors.stream().filter(i -> i.getBinding() == binding).findFirst().orElse(null))
                    .collect(toList())));

        final ProxyClassLoader loader = new ProxyClassLoader(Thread.currentThread().getContextClassLoader(), ref.getBundle());
        if (classes.stream().allMatch(Class::isInterface)) {
            final Object proxyInstance = Proxy.newProxyInstance(
                    loader,
                    classes.toArray(EMPTY_CLASSES),
                    (proxy, method, args) -> doInvoke(ref, method, args, interceptorInstancePerMethod));
            return (T) proxyInstance;
        }
        final AsmProxyFactory asm = new AsmProxyFactory();
        final Class<?> proxyClass = asm.createProxyClass(
                loader,
                getProxyClassName(classes),
                classes.stream().sorted(this::compareClasses).toArray(Class<?>[]::new),
                findInterceptedMethods(classes));
        return asm.create(proxyClass, (method, args) -> doInvoke(ref, method, args, interceptorInstancePerMethod));
    }

    private <T> Object doInvoke(final ServiceReference<T> ref,
                                final Method method, final Object[] args,
                                final Map<Method, List<InterceptorInstance<?>>> interceptorsPerMethod) throws Exception {
        final List<InterceptorInstance<?>> methodInterceptors = interceptorsPerMethod.getOrDefault(method, emptyList());
        return new InterceptorInvocationContext<>(ref, methodInterceptors, method, args).proceed();
    }

    private int compareClasses(final Class<?> c1, final Class<?> c2) {
        if (c1 == c2) {
            return 0;
        }
        if (c1.isAssignableFrom(c2)) {
            return 1;
        }
        if (c2.isAssignableFrom(c1)) {
            return -1;
        }
        if (c1.isInterface() && !c2.isInterface()) {
            return 1;
        }
        if (c2.isInterface() && !c1.isInterface()) {
            return -1;
        }
        if (!c1.isInterface() && !c2.isInterface()) {
            throw new IllegalArgumentException("No common class between " + c1 + " and " + c2);
        }
        return c1.getName().compareTo(c2.getName()); // just to be deterministic
    }

    private Method[] findInterceptedMethods(final List<Class<?>> classes) {
        return classes.stream()
                .flatMap(c -> c.isInterface() ? Stream.of(c.getMethods()) : findMethods(c))
                .distinct()
                .filter(method -> Modifier.isPublic(method.getModifiers())) // todo: enable protected? not that scr friendly but doable
                .toArray(Method[]::new);
    }

    private Stream<Method> findMethods(final Class<?> clazz) {
        return clazz == null || Object.class == clazz ?
                Stream.empty() :
                Stream.concat(Stream.of(clazz.getDeclaredMethods()), findMethods(clazz.getSuperclass()));
    }

    private String getProxyClassName(final List<Class<?>> classes) {
        return classes.iterator().next().getName() + "$$KarafInterceptorProxy" +
                classes.stream().skip(1).map(c -> c.getName().replace(".", "_").replace("$", "")).collect(joining("__"));
    }

    static class ProxyClassLoader extends ClassLoader {
        private final Bundle bundle;
        private final Map<String, Class<?>> classes = new ConcurrentHashMap<>();

        ProxyClassLoader(final ClassLoader parent, final Bundle bundle) {
            super(parent);
            this.bundle = bundle;
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            final Class<?> clazz = classes.get(name);
            if (clazz != null) {
                return clazz;
            }
            if (bundle != null) {
                try {
                    return bundle.loadClass(name);
                } catch (final ClassNotFoundException cnfe) {
                    if (name != null && name.startsWith("org.apache.karaf.service.interceptor.")) {
                        return getClass().getClassLoader().loadClass(name);
                    }
                    throw cnfe;
                }
            }
            return super.loadClass(name, resolve);
        }

        @Override
        public URL getResource(final String name) {
            return bundle.getResource(name);
        }

        @Override
        public Enumeration<URL> getResources(final String name) throws IOException {
            return bundle.getResources(name);
        }

        @Override
        public InputStream getResourceAsStream(final String name) {
            return ofNullable(getResource(name)).map(u -> {
                try {
                    return u.openStream();
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }).orElse(null);
        }

        public <T> Class<T> getOrRegister(final String proxyClassName, final byte[] proxyBytes,
                                          final Package pck, final ProtectionDomain protectionDomain) {
            final String key = proxyClassName.replace('/', '.');
            Class<?> existing = classes.get(key);
            if (existing == null) {
                synchronized (this) {
                    existing = classes.get(key);
                    if (existing == null) {
                        definePackageFor(pck, protectionDomain);
                        existing = super.defineClass(proxyClassName, proxyBytes, 0, proxyBytes.length);
                        resolveClass(existing);
                        classes.put(key, existing);
                    }
                }
            }
            return (Class<T>) existing;
        }

        private void definePackageFor(final Package model, final ProtectionDomain protectionDomain) {
            if (model == null) {
                return;
            }
            if (getPackage(model.getName()) == null) {
                if (model.isSealed() && protectionDomain != null &&
                        protectionDomain.getCodeSource() != null &&
                        protectionDomain.getCodeSource().getLocation() != null) {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            protectionDomain.getCodeSource().getLocation());
                } else {
                    definePackage(
                            model.getName(),
                            model.getSpecificationTitle(),
                            model.getSpecificationVersion(),
                            model.getSpecificationVendor(),
                            model.getImplementationTitle(),
                            model.getImplementationVersion(),
                            model.getImplementationVendor(),
                            null);
                }
            }
        }
    }
}
