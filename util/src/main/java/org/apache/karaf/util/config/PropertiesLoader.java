/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.util.config;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.Properties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static org.apache.felix.utils.properties.InterpolationHelper.substVars;

public class PropertiesLoader {

    private static final String INCLUDES_PROPERTY = "${includes}"; // mandatory includes

    private static final String OPTIONALS_PROPERTY = "${optionals}"; // optionals include

    private static final String OVERRIDE_PREFIX = "karaf.override."; // prefix that marks that system property should override defaults.

    /**
     * <p>
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties
     * are accessible to the framework and to bundles and are intended
     * for configuration purposes. By default, the configuration property
     * file is located in the <code>conf/</code> directory of the Felix
     * installation directory and is called "<code>config.properties</code>".
     * The installation directory of Felix is assumed to be the parent
     * directory of the <code>felix.jar</code> file as found on the system class
     * path property. The precise file from which to load configuration
     * properties can be set by initializing the "<code>felix.config.properties</code>"
     * system property to an arbitrary URL.
     * </p>
     *
     * @param file the config file where to load the properties.
     * @return A <code>Properties</code> instance or <code>null</code> if there was an error.
     * @throws Exception if something wrong occurs.
     */
    public static Properties loadConfigProperties(File file) throws Exception {
        // See if the property URL was specified as a property.
        URL configPropURL;
        try {
            configPropURL = file.toURI().toURL();
        } catch (MalformedURLException ex) {
            System.err.print("Main: " + ex);
            return null;
        }

        Properties configProps = loadPropertiesFile(configPropURL, false);
        copySystemProperties(configProps);
        configProps.substitute(new InterpolationHelper.BundleContextSubstitutionCallback(null) {
            @Override
            public String getValue(final String key) {
                final String value = super.getValue(key);
                if (value == null) {
                    return lookupVirtualProperty(configProps, key);
                }
                return value;
            }
        });

        // Perform variable substitution for system properties.
//        for (Enumeration<?> e = configProps.propertyNames(); e.hasMoreElements();) {
//            String name = (String) e.nextElement();
//            configProps.setProperty(name,
//                    SubstHelper.substVars(configProps.getProperty(name), name, null, configProps));
//        }

        return configProps;
    }

    /**
     * <p>
     * Loads the properties in the system property file associated with the
     * framework installation into <code>System.setProperty()</code>. These properties
     * are not directly used by the framework in anyway. By default, the system
     * property file is located in the <code>conf/</code> directory of the Felix
     * installation directory and is called "<code>system.properties</code>". The
     * installation directory of Felix is assumed to be the parent directory of
     * the <code>felix.jar</code> file as found on the system class path property.
     * The precise file from which to load system properties can be set by
     * initializing the "<code>felix.system.properties</code>" system property to an
     * arbitrary URL.
     * </p>
     *
     * @param file the Karaf base folder.
     * @throws IOException if the system file can't be loaded.
     */
    public static void loadSystemProperties(File file) throws IOException {
        Properties props = null;
        try {
            URL configPropURL = file.toURI().toURL();
            props = loadPropertiesFile(configPropURL, true);
        } catch (Exception ex) {
            // Ignore
            return;
        }

        InterpolationHelper.SubstitutionCallback callback = new InterpolationHelper.BundleContextSubstitutionCallback(null);
        for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements(); ) {
            String name = (String) e.nextElement();
            if (name.startsWith(OVERRIDE_PREFIX)) {
                String overrideName = name.substring(OVERRIDE_PREFIX.length());
                String value = props.getProperty(name);
                System.setProperty(overrideName, substVars(value, name, null, props, callback));
            } else {
                String value = System.getProperty(name, props.getProperty(name));
                System.setProperty(name, substVars(value, name, null, props, callback));
            }
        }
    }

    public static void copySystemProperties(Properties configProps) {
        for (Enumeration<?> e = System.getProperties().propertyNames();
             e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            if (key.startsWith("felix.") ||
                    key.startsWith("karaf.") ||
                    key.startsWith("org.osgi.framework.")) {
                configProps.setProperty(key, System.getProperty(key));
            }
        }
    }

    public static Properties loadPropertiesOrFail(File configFile) {
        try {
            URL configPropURL = configFile.toURI().toURL();
            return loadPropertiesFile(configPropURL, true);
        } catch (Exception e) {
            throw new RuntimeException("Error loading properties from " + configFile, e);
        }
    }

    public static Properties loadPropertiesFile(URL configPropURL, boolean failIfNotFound) throws Exception {
        Properties configProps = new Properties(null, false);
        try {
            configProps.load(configPropURL);
        } catch (FileNotFoundException ex) {
            if (failIfNotFound) {
                throw ex;
            }
        } catch (Exception ex) {
            System.err.println("Error loading config properties from " + configPropURL);
            System.err.println("Main: " + ex);
            return configProps;
        }
        loadIncludes(INCLUDES_PROPERTY, true, configPropURL, configProps);
        loadIncludes(OPTIONALS_PROPERTY, false, configPropURL, configProps);
        trimValues(configProps);
        return configProps;
    }

    private static void loadIncludes(String propertyName, boolean mandatory, URL configPropURL, Properties configProps)
            throws Exception {
        String includes = configProps.get(propertyName);
        if (includes != null) {
            StringTokenizer st = new StringTokenizer(includes, "\" ", true);
            if (st.countTokens() > 0) {
                String location;
                do {
                    location = nextLocation(st);
                    if (location != null) {
                        URL url = new URL(configPropURL, location);
                        Properties props = loadPropertiesFile(url, mandatory);
                        configProps.putAll(props);
                    }
                }
                while (location != null);
            }
        }
        configProps.remove(propertyName);
    }

    private static void trimValues(Properties configProps) {
        for (String key : configProps.keySet()) {
            configProps.put(key, configProps.get(key).trim());
        }
    }

    private static String nextLocation(StringTokenizer st) {
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "\" ";
            StringBuilder tokBuf = new StringBuilder(10);
            String tok;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit)) {
                tok = st.nextToken(tokenList);
                switch (tok) {
                    case "\"":
                        inQuote = !inQuote;
                        if (inQuote) {
                            tokenList = "\"";
                        } else {
                            tokenList = "\" ";
                        }
                        break;
                    case " ":
                        if (tokStarted) {
                            retVal = tokBuf.toString();
                            tokStarted = false;
                            tokBuf = new StringBuilder(10);
                            exit = true;
                        }
                        break;
                    default:
                        tokStarted = true;
                        tokBuf.append(tok.trim());
                        break;
                }
            }

            // Handle case where end of token stream and
            // still got data
            if ((!exit) && (tokStarted)) {
                retVal = tokBuf.toString();
            }
        }

        return retVal;
    }

    private static String lookupVirtualProperty(final Properties properties, final Object key) {
        final String keyString = String.valueOf(key);
        if (keyString.startsWith("jre-")) {
            final String versionString = keyString.substring("jre-".length());
            final Integer versionInt = toJavaIntVersion(properties, versionString);
            if (versionInt == null) {
                return null;
            }

            // for now jre-9 always had been ok but if we have a closer definition we prefer it
            for (int i = versionInt - 1; i >= 9; i--) {
                final String testedKey = "jre-" + i;
                final String otherValue = properties.getProperty(testedKey);
                if (otherValue != null) {
                    return otherValue;
                }
            }
        } else if (keyString.startsWith("eecap-")) { // done in Felix.java but too late since karaf uses it in interpolations too early
            final String versionString = keyString.substring("eecap-".length());
            final Integer versionInt = toJavaIntVersion(properties, versionString);
            if (versionInt == null) {
                return null;
            }

            final String tenToLastVersion = IntStream.rangeClosed(10, versionInt)
                    .boxed()
                    .map(Object::toString)
                    .collect(joining(",", ",", ""));
            return "osgi.ee; osgi.ee=\"OSGi/Minimum\"; version:List<Version>=\"1.0,1.1,1.2\"," +
                    "osgi.ee; osgi.ee=\"JavaSE\"; version:List<Version>=\"1.0,1.1,1.2,1.3,1.4,1.5,1.6,1.7,1.8,9" + tenToLastVersion + "\"," +
                    "osgi.ee; osgi.ee=\"JavaSE/compact1\"; version:List<Version>=\"1.8,9" + tenToLastVersion + "\"," +
                    "osgi.ee; osgi.ee=\"JavaSE/compact2\"; version:List<Version>=\"1.8,9" + tenToLastVersion + "\"," +
                    "osgi.ee; osgi.ee=\"JavaSE/compact3\"; version:List<Version>=\"1.8,9" + tenToLastVersion + "\"";
        }
        return null;
    }

    private static Integer toJavaIntVersion(final Properties properties, final String versionString) {
        final String forcedVersion = properties.getProperty("java.specification.version");
        if (forcedVersion != null && !forcedVersion.equals(versionString)) {
            return null;
        }

        // fallback for future versions and avoid to have to define it, felix does it for eecap already
        int versionInt;
        try {
            versionInt = Integer.parseInt(versionString);
        } catch (final NumberFormatException nfe) {
            return null;
        }

        if (versionInt < 9) { // must be defined, no help there
            return null;
        }
        return versionInt;
    }
}
