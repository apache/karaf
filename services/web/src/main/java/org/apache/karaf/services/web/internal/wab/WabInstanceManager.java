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
package org.apache.karaf.services.web.internal.wab;

import java.lang.reflect.InvocationTargetException;

import org.apache.tomcat.InstanceManager;

/**
 * Creates the servlets, filters and listeners of a web application bundle with the bundle class loader, and the
 * Tomcat ones (the default servlet) with the class loader of the web container.
 * <p>
 * The {@code jakarta.annotation} annotations ({@code @PostConstruct}, {@code @PreDestroy}, {@code @Resource}) are
 * not processed.
 */
class WabInstanceManager implements InstanceManager {

    private final ClassLoader bundleClassLoader;
    private final ClassLoader containerClassLoader = WabInstanceManager.class.getClassLoader();

    WabInstanceManager(ClassLoader bundleClassLoader) {
        this.bundleClassLoader = bundleClassLoader;
    }

    @Override
    public Object newInstance(Class<?> clazz) throws IllegalAccessException, InvocationTargetException,
            InstantiationException, NoSuchMethodException {
        return clazz.getConstructor().newInstance();
    }

    @Override
    public Object newInstance(String className) throws IllegalAccessException, InvocationTargetException,
            InstantiationException, ClassNotFoundException, NoSuchMethodException {
        return newInstance(loadClass(className, bundleClassLoader));
    }

    @Override
    public Object newInstance(String fqcn, ClassLoader classLoader) throws IllegalAccessException,
            InvocationTargetException, InstantiationException, ClassNotFoundException, NoSuchMethodException {
        return newInstance(loadClass(fqcn, classLoader));
    }

    private Class<?> loadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (className.startsWith("org.apache.catalina.")) {
            return containerClassLoader.loadClass(className);
        }
        return classLoader.loadClass(className);
    }

    @Override
    public void newInstance(Object o) {
    }

    @Override
    public void destroyInstance(Object o) {
    }

}
