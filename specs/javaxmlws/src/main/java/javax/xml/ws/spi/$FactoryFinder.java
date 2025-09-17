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
package javax.xml.ws.spi;

import javax.xml.ws.WebServiceException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;

class $FactoryFinder {

    private static final Logger LOGGER = Logger.getLogger("javax.xml.ws");

    @SuppressWarnings("unchecked")
    static <T> T find(Class<T> factoryClass, String fallbackClassName) {
        ClassLoader classLoader = contextClassLoader();

        T provider = firstByServiceLoader(factoryClass);
        if (provider != null) {
            return provider;
        }

        String factoryId = factoryClass.getName();

        provider = (T) fromJDKProperties(factoryId, fallbackClassName, classLoader);
        if (provider != null) {
            return provider;
        }

        provider = (T) fromSystemProperty(factoryId, fallbackClassName, classLoader);
        if (provider != null) {
            return provider;
        }

        try {
            Class<T> spiClass = org.apache.karaf.specs.locator.OsgiLocator.locate(factoryClass);
            if (spiClass != null) {
                return spiClass.getConstructor().newInstance();
            }
        } catch (Throwable t) {
        }

        if (fallbackClassName == null) {
            throw new WebServiceException("Provider for " + factoryId + " cannot be found", null);
        }

        return (T) newInstance(fallbackClassName, fallbackClassName, classLoader);
    }

    private static Object fromSystemProperty(String factoryId, String fallbackClassName, ClassLoader classLoader) {
        try {
            String systemProp = System.getProperty(factoryId);
            if (systemProp != null) {
                return newInstance(systemProp, fallbackClassName, classLoader);
            }
        } catch (SecurityException ignored) {
        }
        return null;
    }

    private static Object fromJDKProperties(String factoryId, String fallbackClassName, ClassLoader classLoader) {
        Path path = null;
        try {
            String JAVA_HOME = System.getProperty("java.home");
            path = Paths.get(JAVA_HOME, "conf", "jaxws.properties");
            if (!Files.exists(path)) {
                path = Paths.get(JAVA_HOME, "lib", "jaxws.properties");
            }
            if (Files.exists(path)) {
                Properties props = new Properties();
                try (InputStream inStream = Files.newInputStream(path)) {
                    props.load(inStream);
                }
                String factoryClassName = props.getProperty(factoryId);
                return newInstance(factoryClassName, fallbackClassName, classLoader);
            }
        } catch (Exception ignored) {
            LOGGER.log(Level.SEVERE, "Error reading JAX-WS configuration from ["  + path +
                    "] file. Check it is accessible and has correct format.", ignored);
        }
        return null;
    }

    private static <T> T firstByServiceLoader(Class<T> spiClass) throws WebServiceException {
        LOGGER.log(Level.FINE, "Using java.util.ServiceLoader to find {0}", spiClass.getName());
        try {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(spiClass);
            for (T impl : serviceLoader) {
                LOGGER.fine("ServiceProvider loading Facility used; returning object [" + impl.getClass().getName() + "]");
                return impl;
            }
        } catch (Throwable t) {
            throw new WebServiceException("Error while searching for service [" + spiClass.getName() + "]", t);
        }
        return null;
    }

    private static void checkPackageAccess(String className) {
        SecurityManager s = System.getSecurityManager();
        if (s != null) {
            int i = className.lastIndexOf('.');
            if (i != -1) {
                s.checkPackageAccess(className.substring(0, i));
            }
        }
    }

    private static Class nullSafeLoadClass(String className, ClassLoader classLoader) throws ClassNotFoundException {
        if (classLoader == null) {
            return Class.forName(className);
        } else {
            return classLoader.loadClass(className);
        }
    }

    private static Object newInstance(String className, String defaultImplClassName, ClassLoader classLoader) throws WebServiceException {
        try {
            return safeLoadClass(className, defaultImplClassName, classLoader).getConstructor().newInstance();
        } catch (ClassNotFoundException x) {
            throw new WebServiceException("Provider " + className + " not found", x);
        } catch (Exception x) {
            throw new WebServiceException("Provider " + className + " could not be instantiated: " + x, x);
        }
    }

    private static Class<?> safeLoadClass(String className, String defaultImplClassName, ClassLoader classLoader) throws ClassNotFoundException {
        try {
            checkPackageAccess(className);
        } catch (SecurityException se) {
            if (defaultImplClassName != null && defaultImplClassName.equals(className)) {
                return Class.forName(className);
            }
            throw se;
        }
        return nullSafeLoadClass(className, classLoader);
    }

    private static ClassLoader contextClassLoader() throws WebServiceException {
        try {
            return Thread.currentThread().getContextClassLoader();
        } catch (Exception x) {
            throw new WebServiceException(x.toString(), x);
        }
    }

}
