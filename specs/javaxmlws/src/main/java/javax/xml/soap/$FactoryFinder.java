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
package javax.xml.soap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.logging.Logger;


class $FactoryFinder {

    private static final Logger LOGGER = Logger.getLogger("javax.xml.soap");

    @SuppressWarnings("unchecked")
    static <T> T find(Class<T> factoryClass, String defaultClassName, boolean tryFallback, String deprecatedFactoryId) throws SOAPException {
        ClassLoader tccl = contextClassLoader();

        String factoryId = factoryClass.getName();
        String className = fromSystemProperty(factoryId, deprecatedFactoryId);
        if (className != null) {
            Object result = newInstance(className, defaultClassName, tccl);
            return (T) result;
         }

        className = fromJDKProperties(factoryId, deprecatedFactoryId);
        if (className != null) {
            Object result = newInstance(className, defaultClassName, tccl);
            return (T) result;
        }

        try {
            Class<T> spiClass = org.apache.karaf.specs.locator.OsgiLocator.locate(factoryClass);
            if (spiClass != null) {
                return spiClass.getConstructor().newInstance();
            }
        } catch (Throwable t) {
        }

        T factory = firstByServiceLoader(factoryClass);
        if (factory != null) {
            return factory;
        }

        className = fromMetaInfServices(deprecatedFactoryId, tccl);
        if (className != null) {
            LOGGER.log(Level.WARNING,
                    "Using deprecated META-INF/services mechanism with non-standard property: {0}. " +
                            "Property {1} should be used instead.",
                    new Object[]{deprecatedFactoryId, factoryId});
            Object result = newInstance(className, defaultClassName, tccl);
            return (T) result;
        }

        if (!tryFallback)
            return null;

        if (defaultClassName == null) {
            throw new SOAPException("Provider for " + factoryId + " cannot be found", null);
        }
        return (T) newInstance(defaultClassName, defaultClassName, tccl);
    }

    static <T> T find(Class<T> factoryClass, String defaultClassName, boolean tryFallback) throws SOAPException {
        return find(factoryClass, defaultClassName, tryFallback, null);
    }

    private static String fromMetaInfServices(String deprecatedFactoryId, ClassLoader tccl) {
        String serviceId = "META-INF/services/" + deprecatedFactoryId;
        LOGGER.log(Level.FINE, "Checking deprecated {0} resource", serviceId);

        try (InputStream is =
                     tccl == null
                             ? ClassLoader.getSystemResourceAsStream(serviceId)
                             : tccl.getResourceAsStream(serviceId)) {

            if (is != null) {
                String factoryClassName;
                try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                     BufferedReader rd = new BufferedReader(isr)) {
                    factoryClassName = rd.readLine();
                }

                logFound(factoryClassName);
                if (factoryClassName != null && !"".equals(factoryClassName)) {
                    return factoryClassName;
                }
            }
        } catch (IOException e) {
        }
        return null;
    }

    private static String fromJDKProperties(String factoryId, String deprecatedFactoryId) {
        Path path = null;
        try {
            String JAVA_HOME = getSystemProperty("java.home");
            path = Paths.get(JAVA_HOME, "conf", "jaxm.properties");
            LOGGER.log(Level.FINE, "Checking configuration in {0}", path);

            if (!Files.exists(path)) {
                path = Paths.get(JAVA_HOME, "lib", "jaxm.properties");
            }

            LOGGER.log(Level.FINE, "Checking configuration in {0}", path);
            if (Files.exists(path)) {
                Properties props = new Properties();
                try (InputStream inputStream = Files.newInputStream(path)) {
                    props.load(inputStream);
                }

                LOGGER.log(Level.FINE, "Checking property {0}", factoryId);
                String factoryClassName = props.getProperty(factoryId);
                logFound(factoryClassName);
                if (factoryClassName != null) {
                    return factoryClassName;
                }

                if (deprecatedFactoryId != null) {
                    LOGGER.log(Level.FINE, "Checking deprecated property {0}", deprecatedFactoryId);
                    factoryClassName = props.getProperty(deprecatedFactoryId);
                    logFound(factoryClassName);
                    if (factoryClassName != null) {
                        LOGGER.log(Level.WARNING,
                                "Using non-standard property: {0}. Property {1} should be used instead.",
                                new Object[]{deprecatedFactoryId, factoryId});
                        return factoryClassName;
                    }
                }
            }
        } catch (Exception ignored) {
            LOGGER.log(Level.SEVERE, "Error reading SAAJ configuration from ["  + path +
                    "] file. Check it is accessible and has correct format.", ignored);
        }
        return null;
    }

    private static String fromSystemProperty(String factoryId, String deprecatedFactoryId) {
        String systemProp = getSystemProperty(factoryId);
        if (systemProp != null) {
            return systemProp;
        }
        if (deprecatedFactoryId != null) {
            systemProp = getSystemProperty(deprecatedFactoryId);
            if (systemProp != null) {
                LOGGER.log(Level.WARNING,
                        "Using non-standard property: {0}. Property {1} should be used instead.",
                        new Object[] {deprecatedFactoryId, factoryId});
                return systemProp;
            }
        }
        return null;
    }

    private static String getSystemProperty(final String property) {
        LOGGER.log(Level.FINE, "Checking system property {0}", property);
        String value = AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(property));
        logFound(value);
        return value;
    }

    private static void logFound(String value) {
        if (value != null) {
            LOGGER.log(Level.FINE, "  found {0}", value);
        } else {
            LOGGER.log(Level.FINE, "  not found");
        }
    }

    private static <T> T firstByServiceLoader(Class<T> spiClass) throws SOAPException {
        LOGGER.log(Level.FINE, "Using java.util.ServiceLoader to find {0}", spiClass.getName());
        try {
            ServiceLoader<T> serviceLoader = ServiceLoader.load(spiClass);
            for (T impl : serviceLoader) {
                LOGGER.fine("ServiceProvider loading Facility used; returning object [" + impl.getClass().getName() + "]");
                return impl;
            }
        } catch (Throwable t) {
            throw new SOAPException("Error while searching for service [" + spiClass.getName() + "]", t);
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

    static Object newInstance(String className, String defaultImplClassName, ClassLoader classLoader) throws SOAPException {
        try {
            return safeLoadClass(className, defaultImplClassName, classLoader).getConstructor().newInstance();
        } catch (ClassNotFoundException x) {
            throw new SOAPException("Provider " + className + " not found", x);
        } catch (Exception x) {
            throw new SOAPException("Provider " + className + " could not be instantiated: " + x, x);
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

    private static ClassLoader contextClassLoader() throws SOAPException {
        try {
            return Thread.currentThread().getContextClassLoader();
        } catch (Exception x) {
            throw new SOAPException(x.toString(), x);
        }
    }

}
