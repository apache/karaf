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
package javax.xml.xpath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.security.*;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.function.Supplier;

class $XPathFactoryFinder {
    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xpath.internal";

    private static boolean debug = false;

    static {
        try {
            debug = getSystemProperty("jaxp.debug") != null;
        } catch (Exception unused) {
            debug = false;
        }
    }

    private static final Properties cacheProps = new Properties();

    private volatile static boolean firstTime = true;

    private static void debugPrintln(Supplier<String> msgGen) {
        if (debug) {
            System.err.println("JAXP: " + msgGen.get());
        }
    }

    private final ClassLoader classLoader;

    public $XPathFactoryFinder(ClassLoader loader) {
        this.classLoader = loader;
        if (debug) {
            debugDisplayClassLoader();
        }
    }

    private void debugDisplayClassLoader() {
        try {
            if (classLoader == getContextClassLoader()) {
                debugPrintln(() -> "using thread context class loader (" + classLoader + ") for search");
                return;
            }
        } catch (Throwable unused) {
        }

        if (classLoader == ClassLoader.getSystemClassLoader()) {
            debugPrintln(() -> "using system class loader (" + classLoader + ") for search");
            return;
        }

        debugPrintln(() -> "using class loader (" + classLoader + ") for search");
    }

    public XPathFactory newFactory(String uri) throws XPathFactoryConfigurationException {
        if (uri == null) {
            throw new NullPointerException();
        }
        XPathFactory f = _newFactory(uri);
        if (f != null) {
            debugPrintln(() -> "factory '" + f.getClass().getName() + "' was found for " + uri);
        } else {
            debugPrintln(() -> "unable to find a factory for " + uri);
        }
        return f;
    }

    private XPathFactory _newFactory(String uri) throws XPathFactoryConfigurationException {
        XPathFactory xpathFactory = null;

        String propertyName = SERVICE_CLASS.getName() + ":" + uri;

        try {
            debugPrintln(() -> "Looking up system property '" + propertyName + "'");
            String r = getSystemProperty(propertyName);
            if (r != null) {
                debugPrintln(() -> "The value is '" + r + "'");
                xpathFactory = createInstance(r, true);
                if (xpathFactory != null) {
                    return xpathFactory;
                }
            } else
                debugPrintln(() -> "The property is undefined.");
        } catch (Throwable t) {
            if (debug) {
                debugPrintln(() -> "failed to look up system property '" + propertyName + "'");
                t.printStackTrace();
            }
        }

        String javah = getSystemProperty("java.home");
        String configFile = javah + File.separator +
                "conf" + File.separator + "jaxp.properties";

        try {
            if (firstTime) {
                synchronized (cacheProps) {
                    if (firstTime) {
                        File f = new File(configFile);
                        firstTime = false;
                        if (doesFileExist(f)) {
                            debugPrintln(() -> "Read properties file " + f);
                            cacheProps.load(getFileInputStream(f));
                        }
                    }
                }
            }
            final String factoryClassName = cacheProps.getProperty(propertyName);
            debugPrintln(() -> "found " + factoryClassName + " in $java.home/conf/jaxp.properties");

            if (factoryClassName != null) {
                xpathFactory = createInstance(factoryClassName, true);
                if (xpathFactory != null) {
                    return xpathFactory;
                }
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }

        assert xpathFactory == null;
        xpathFactory = findServiceProvider(uri);


        if (xpathFactory != null) {
            return xpathFactory;
        }

        if (uri.equals(XPathFactory.DEFAULT_OBJECT_MODEL_URI)) {
            debugPrintln(() -> "attempting to use the platform default W3C DOM XPath lib");
            return createInstance("com.sun.org.apache.xpath.internal.jaxp.XPathFactoryImpl", true);
        }

        debugPrintln(() -> "all things were tried, but none was found. bailing out.");
        return null;
    }

    private Class<?> createClass(String className) {
        Class clazz;
        boolean internal = false;
        if (System.getSecurityManager() != null) {
            if (className != null && className.startsWith(DEFAULT_PACKAGE)) {
                internal = true;
            }
        }

        try {
            if (classLoader != null && !internal) {
                clazz = Class.forName(className, false, classLoader);
            } else {
                clazz = Class.forName(className);
            }
        } catch (Throwable t) {
            if (debug) {
                t.printStackTrace();
            }
            return null;
        }

        return clazz;
    }

    XPathFactory createInstance(String className)
            throws XPathFactoryConfigurationException {
        return createInstance(className, false);
    }

    XPathFactory createInstance(String className, boolean useServicesMechanism)
            throws XPathFactoryConfigurationException {
        XPathFactory xPathFactory = null;

        debugPrintln(() -> "createInstance(" + className + ")");

        Class<?> clazz = createClass(className);
        if (clazz == null) {
            debugPrintln(() -> "failed to getClass(" + className + ")");
            return null;
        }
        debugPrintln(() -> "loaded " + className + " from " + which(clazz));

        try {
            if (!useServicesMechanism) {
                xPathFactory = newInstanceNoServiceLoader(clazz);
            }
            if (xPathFactory == null) {
                xPathFactory = (XPathFactory) clazz.newInstance();
            }
        } catch (ClassCastException | IllegalAccessException | InstantiationException classCastException) {
            debugPrintln(() -> "could not instantiate " + clazz.getName());
            if (debug) {
                classCastException.printStackTrace();
            }
            return null;
        }

        return xPathFactory;
    }

    private static XPathFactory newInstanceNoServiceLoader(
            Class<?> providerClass
    ) throws XPathFactoryConfigurationException {
        if (System.getSecurityManager() == null) {
            return null;
        }
        try {
            Method creationMethod =
                    providerClass.getDeclaredMethod(
                            "newXPathFactoryNoServiceLoader"
                    );
            final int modifiers = creationMethod.getModifiers();

            // Do not call "newXPathFactoryNoServiceLoader" if it's
            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                return null;
            }

            // Only calls "newXPathFactoryNoServiceLoader" if it's
            final Class<?> returnType = creationMethod.getReturnType();
            if (SERVICE_CLASS.isAssignableFrom(returnType)) {
                return SERVICE_CLASS.cast(creationMethod.invoke(null, (Object[]) null));
            } else {
                throw new ClassCastException(returnType
                        + " cannot be cast to " + SERVICE_CLASS);
            }
        } catch (ClassCastException e) {
            throw new XPathFactoryConfigurationException(e);
        } catch (Exception exc) {
            return null;
        }
    }

    private boolean isObjectModelSupportedBy(final XPathFactory factory,
                                             final String objectModel,
                                             AccessControlContext acc) {
        return AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> factory.isObjectModelSupported(objectModel), acc);
    }

    private XPathFactory findServiceProvider(final String objectModel)
            throws XPathFactoryConfigurationException {

        assert objectModel != null;
        final AccessControlContext acc = AccessController.getContext();
        try {
            return AccessController.doPrivileged((PrivilegedAction<XPathFactory>) () -> {
                final ServiceLoader<XPathFactory> loader =
                        ServiceLoader.load(SERVICE_CLASS);
                for (XPathFactory factory : loader) {
                    if (isObjectModelSupportedBy(factory, objectModel, acc)) {
                        return factory;
                    }
                }
                return null;
            });
        } catch (ServiceConfigurationError error) {
            throw new XPathFactoryConfigurationException(error);
        }
    }

    private static final Class<XPathFactory> SERVICE_CLASS = XPathFactory.class;

    private static String which(Class<?> clazz) {
        return getClassSource(clazz);
    }

    static ClassLoader getContextClassLoader() throws SecurityException{
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
            throw (FileNotFoundException)e.getException();
        }
    }

    private static String getClassSource(Class<?> cls) {
        return AccessController.doPrivileged((PrivilegedAction<String>) () -> {
            CodeSource cs = cls.getProtectionDomain().getCodeSource();
            if (cs != null) {
                URL loc = cs.getLocation();
                return loc != null ? loc.toString() : "(no location)";
            } else {
                return "(no code source)";
            }
        });
    }

    private static boolean doesFileExist(final File f) {
        return AccessController.doPrivileged((PrivilegedAction<Boolean>) f::exists);
    }
}
