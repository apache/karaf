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
package javax.xml.transform;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;

class $FactoryFinder {

    private static final String DEFAULT_PACKAGE = "com.sun.xml.internal.";

    private static boolean debug;

    final private static Properties cacheProps = new Properties();

    private static volatile boolean firstTime = true;

    static {
        try {
            String val = getSystemProperty("jaxp.debug");
            debug = val != null && !"false".equals(val);
        } catch (SecurityException se) {
            debug = false;
        }
    }

    private static void dPrint(Supplier<String> msgGen) {
        if (debug) {
            System.err.println("JAXP: " + msgGen.get());
        }
    }


    static private Class getProviderClass(String className, ClassLoader cl, boolean doFallback, boolean useBSClsLoader) throws ClassNotFoundException {
        try {
            if (cl == null) {
                if (useBSClsLoader) {
                    return Class.forName(className, false, $FactoryFinder.class.getClassLoader());
                } else {
                    cl = getContextClassLoader();
                    if (cl == null) {
                        throw new ClassNotFoundException();
                    } else {
                        return Class.forName(className, false, cl);
                    }
                }
            } else {
                return Class.forName(className, false, cl);
            }
        } catch (ClassNotFoundException e1) {
            if (doFallback) {
                return Class.forName(className, false, $FactoryFinder.class.getClassLoader());
            } else {
                throw e1;
            }
        }
    }


    static <T> T newInstance(Class<T> type, String className, ClassLoader cl, boolean doFallback) {
        return newInstance(type, className, cl, doFallback, false);
    }


    static <T> T newInstance(Class<T> type, String className, ClassLoader cl, boolean doFallback, boolean useBSClsLoader) {
        assert type != null;
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                cl = null;
                useBSClsLoader = true;
            }
        }
        try {
            Class<?> providerClass = getProviderClass(className, cl, doFallback, useBSClsLoader);
            if (!type.isAssignableFrom(providerClass)) {
                throw new ClassCastException(className + " cannot be cast to " + type.getName());
            }
            Object instance = providerClass.getConstructor().newInstance();
            final ClassLoader clD = cl;
            dPrint(() -> "created new instance of " + providerClass + " using ClassLoader: " + clD);
            return type.cast(instance);
        } catch (ClassNotFoundException x) {
            throw new TransformerFactoryConfigurationError(x, "Provider " + className + " not found");
        } catch (Exception x) {
            throw new TransformerFactoryConfigurationError(x, "Provider " + className + " could not be instantiated: " + x);
        }
    }


    static <T> T find(Class<T> type, String fallbackClassName) {
        return find(type, type.getName(), null, fallbackClassName);
    }

    static <T> T find(Class<T> type, String factoryId, ClassLoader cl, String fallbackClassName) {
        try {
            // If we are deployed into an OSGi environment, leverage it
            Class<? extends T> spiClass = org.apache.karaf.specs.locator.OsgiLocator.locate(type, factoryId);
            if (spiClass != null) {
                return spiClass.getConstructor().newInstance();
            }
        } catch (Throwable e) {
        }

        try {
            final String systemProp;
            if (type.getName().equals(factoryId)) {
                systemProp = getSystemProperty(factoryId);
            } else {
                systemProp = System.getProperty(factoryId);
            }
            if (systemProp != null) {
                dPrint(() -> "found system property, value=" + systemProp);
                return newInstance(type, systemProp, cl, true);
            }
        } catch (SecurityException se) {
            throw new TransformerFactoryConfigurationError(se, "Failed to read factoryId '" + factoryId + "'");
        }
        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        firstTime = false;
                        String javaHome = getSystemProperty("java.home");
                        String configFile;
                        configFile = javaHome + File.separator + "conf" + File.separator + "jaxp.properties";
                        File jaxp = new File(configFile);
                        if (doesFileExist(jaxp)) {
                            cacheProps.load(getFileInputStream(jaxp));
                        }
                        configFile = javaHome + File.separator + "conf" + File.separator + "stax.properties";
                        File stax = new File(configFile);
                        if (doesFileExist(stax)) {
                            cacheProps.load(getFileInputStream(stax));
                        }
                    }
                }
            }
            final String factoryClassName = cacheProps.getProperty(factoryId);
            if (factoryClassName != null) {
                return newInstance(type, factoryClassName, cl, true);
            }
        } catch (Exception ex) {
            if (debug) ex.printStackTrace();
        }
        if (type.getName().equals(factoryId)) {
            final T provider = findServiceProvider(type, cl);
            if (provider != null) {
                return provider;
            }
        } else {
            assert fallbackClassName == null;
        }
        if (fallbackClassName == null) {
            throw new TransformerFactoryConfigurationError("Provider for " + factoryId + " cannot be found");
        }
        dPrint(() -> "loaded from fallback value: " + fallbackClassName);
        return newInstance(type, fallbackClassName, cl, true);
    }


    private static <T> T findServiceProvider(final Class<T> type, final ClassLoader cl) {
        try {
            return AccessController.doPrivileged((PrivilegedAction<T>) () -> {
                final ServiceLoader<T> serviceLoader;
                if (cl == null) {
                    serviceLoader = ServiceLoader.load(type);
                } else {
                    serviceLoader = ServiceLoader.load(type, cl);
                }
                final Iterator<T> iterator = serviceLoader.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                } else {
                    return null;
                }
            });
        } catch (ServiceConfigurationError e) {
            final RuntimeException x = new RuntimeException("Provider for " + type + " cannot be created", e);
            throw new TransformerFactoryConfigurationError(x, x.getMessage());
        }
    }

    private static ClassLoader getContextClassLoader() throws SecurityException {
        return AccessController.doPrivileged((PrivilegedAction<ClassLoader>) () -> {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            return cl;
        });
    }

    private static String getSystemProperty(final String propName) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> System.getProperty(propName));
    }

    private static FileInputStream getFileInputStream(final File file) throws FileNotFoundException {
        try {
            return AccessController.doPrivileged((PrivilegedExceptionAction<FileInputStream>) () -> new FileInputStream(file));
        } catch (PrivilegedActionException e) {
            throw (FileNotFoundException) e.getException();
        }
    }

    private static boolean doesFileExist(final File f) {
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) f::exists);
    }
}
