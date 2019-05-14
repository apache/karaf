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
package javax.xml.validation;

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

class $SchemaFactoryFinder {

    private static boolean debug = false;
    private static final String DEFAULT_PACKAGE = "com.sun.org.apache.xerces.internal";
    private static final Properties cacheProps = new Properties();

    private static volatile boolean firstTime = true;

    static {
        try {
            debug = getSystemProperty("jaxp.debug") != null;
        } catch (Exception unused) {
            debug = false;
        }
    }

    private static void debugPrintln(Supplier<String> msgGen) {
        if (debug) {
            System.err.println("JAXP: " + msgGen.get());
        }
    }

    private final ClassLoader classLoader;

    public $SchemaFactoryFinder(ClassLoader loader) {
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

    public SchemaFactory newFactory(String schemaLanguage) {
        if (schemaLanguage == null) {
            throw new NullPointerException();
        }
        SchemaFactory f = _newFactory(schemaLanguage);
        if (f != null) {
            debugPrintln(() -> "factory '" + f.getClass().getName() + "' was found for " + schemaLanguage);
        } else {
            debugPrintln(() -> "unable to find a factory for " + schemaLanguage);
        }
        return f;
    }

    private SchemaFactory _newFactory(String schemaLanguage) {
        SchemaFactory sf;

        String propertyName = SERVICE_CLASS.getName() + ":" + schemaLanguage;

        try {
            debugPrintln(() -> "Looking up system property '" + propertyName + "'");
            String r = getSystemProperty(propertyName);
            if (r != null) {
                debugPrintln(() -> "The value is '" + r + "'");
                sf = createInstance(r, true);
                if (sf != null) return sf;
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
                sf = createInstance(factoryClassName, true);
                if (sf != null) {
                    return sf;
                }
            }
        } catch (Exception ex) {
            if (debug) {
                ex.printStackTrace();
            }
        }

        final SchemaFactory factoryImpl = findServiceProvider(schemaLanguage);


        if (factoryImpl != null) {
            return factoryImpl;
        }

        if (schemaLanguage.equals("http://www.w3.org/2001/XMLSchema")) {
            debugPrintln(() -> "attempting to use the platform default XML Schema validator");
            return createInstance("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory", true);
        }

        debugPrintln(() -> "all things were tried, but none was found. bailing out.");
        return null;
    }

    private Class<?> createClass(String className) {
        Class<?> clazz;
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

    SchemaFactory createInstance(String className) {
        return createInstance(className, false);
    }

    SchemaFactory createInstance(String className, boolean useServicesMechanism) {
        SchemaFactory schemaFactory = null;

        debugPrintln(() -> "createInstance(" + className + ")");

        Class<?> clazz = createClass(className);
        if (clazz == null) {
            debugPrintln(() -> "failed to getClass(" + className + ")");
            return null;
        }
        debugPrintln(() -> "loaded " + className + " from " + which(clazz));

        try {
            if (!SchemaFactory.class.isAssignableFrom(clazz)) {
                throw new ClassCastException(clazz.getName()
                        + " cannot be cast to " + SchemaFactory.class);
            }
            if (!useServicesMechanism) {
                schemaFactory = newInstanceNoServiceLoader(clazz);
            }
            if (schemaFactory == null) {
                schemaFactory = (SchemaFactory) clazz.newInstance();
            }
        } catch (ClassCastException | IllegalAccessException | InstantiationException classCastException) {
            debugPrintln(() -> "could not instantiate " + clazz.getName());
            if (debug) {
                classCastException.printStackTrace();
            }
            return null;
        }

        return schemaFactory;
    }

    private static SchemaFactory newInstanceNoServiceLoader(
            Class<?> providerClass
    ) {
        if (System.getSecurityManager() == null) {
            return null;
        }
        try {
            final Method creationMethod =
                    providerClass.getDeclaredMethod(
                            "newXMLSchemaFactoryNoServiceLoader"
                    );
            final int modifiers = creationMethod.getModifiers();

            if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
                return null;
            }

            // Only calls "newXMLSchemaFactoryNoServiceLoader" if it's
            final Class<?> returnType = creationMethod.getReturnType();
            if (SERVICE_CLASS.isAssignableFrom(returnType)) {
                return SERVICE_CLASS.cast(creationMethod.invoke(null, (Object[]) null));
            } else {
                throw new ClassCastException(returnType
                        + " cannot be cast to " + SERVICE_CLASS);
            }
        } catch (ClassCastException e) {
            throw new SchemaFactoryConfigurationError(e.getMessage(), e);
        } catch (Exception exc) {
            return null;
        }
    }

    private boolean isSchemaLanguageSupportedBy(final SchemaFactory factory,
                                                final String schemaLanguage,
                                                AccessControlContext acc) {
        return AccessController.doPrivileged(
            (PrivilegedAction<Boolean>) () -> factory.isSchemaLanguageSupported(schemaLanguage), acc);
    }

    private SchemaFactory findServiceProvider(final String schemaLanguage) {
        assert schemaLanguage != null;
        final AccessControlContext acc = AccessController.getContext();
        try {
            return AccessController.doPrivileged((PrivilegedAction<SchemaFactory>) () -> {
                final ServiceLoader<SchemaFactory> loader =
                        ServiceLoader.load(SERVICE_CLASS);
                for (SchemaFactory factory : loader) {
                    if (isSchemaLanguageSupportedBy(factory, schemaLanguage, acc)) {
                        return factory;
                    }
                }
                return null;
            });
        } catch (ServiceConfigurationError error) {
            throw new SchemaFactoryConfigurationError(
                    "Provider for " + SERVICE_CLASS + " cannot be created", error);
        }
    }

    private static final Class<SchemaFactory> SERVICE_CLASS = SchemaFactory.class;


    private static String which(Class<?> clazz) {
        return getClassSource(clazz);
    }

    static ClassLoader getContextClassLoader() throws SecurityException {
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
