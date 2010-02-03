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
package org.apache.felix.karaf.testing;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Inject;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;

public final class Helper {

    private Helper() {
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
    }

    /*
     * Explode the dictionary into a ,-delimited list of key=value pairs
     */
    private static String explode(Dictionary dictionary) {
        Enumeration keys = dictionary.keys();
        StringBuffer result = new StringBuffer();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            result.append(String.format("%s=%s", key, dictionary.get(key)));
            if (keys.hasMoreElements()) {
                result.append(", ");
            }
        }
        return result.toString();
    }

    /*
     * Provides an iterable collection of references, even if the original array is null
     */
    private static final Collection<ServiceReference> asCollection(ServiceReference[] references) {
        List<ServiceReference> result = new LinkedList<ServiceReference>();
        if (references != null) {
            for (ServiceReference reference : references) {
                result.add(reference);
            }
        }
        return result;
    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

    /**
     * <p>
     * This method performs property variable substitution on the
     * specified value. If the specified value contains the syntax
     * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
     * refers to either a configuration property or a system property,
     * then the corresponding property value is substituted for the variable
     * placeholder. Multiple variable placeholders may exist in the
     * specified value as well as nested variable placeholders, which
     * are substituted from inner most to outer most. Configuration
     * properties override system properties.
     * </p>
     *
     * @param val         The string on which to perform property substitution.
     * @param currentKey  The key of the property being evaluated used to
     *                    detect cycles.
     * @param cycleMap    Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *                                  property placeholder syntax or a recursive variable reference.
     */
    private static String substVars(String val, String currentKey,
                                    Map<String, String> cycleMap, Properties configProps)
            throws IllegalArgumentException {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null) {
            cycleMap = new HashMap<String, String>();
        }

        // Put the current key in the cycle map.
        cycleMap.put(currentKey, currentKey);

        // Assume we have a value that is something like:
        // "leading ${foo.${bar}} middle ${baz} trailing"

        // Find the first ending '}' variable delimiter, which
        // will correspond to the first deepest nested variable
        // placeholder.
        int stopDelim = val.indexOf(DELIM_STOP);

        // Find the matching starting "${" variable delimiter
        // by looping until we find a start delimiter that is
        // greater than the stop delimiter we have found.
        int startDelim = val.indexOf(DELIM_START);
        while (stopDelim >= 0) {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim)) {
                break;
            } else if (idx < stopDelim) {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) && (stopDelim < 0)) {
            return val;
        }
        // At this point, we found a stop delimiter without a start,
        // so throw an exception.
        else if (((startDelim < 0) || (startDelim > stopDelim))
                && (stopDelim >= 0)) {
            throw new IllegalArgumentException(
                    "stop delimiter with no start delimiter: "
                            + val);
        }

        // At this point, we have found a variable placeholder so
        // we must perform a variable substitution on it.
        // Using the start and stop delimiter indices, extract
        // the first, deepest nested variable placeholder.
        String variable =
                val.substring(startDelim + DELIM_START.length(), stopDelim);

        // Verify that this is not a recursive variable reference.
        if (cycleMap.get(variable) != null) {
            throw new IllegalArgumentException(
                    "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
                ? configProps.getProperty(variable, null)
                : null;
        if (substValue == null) {
            // Ignore unknown property values.
            substValue = System.getProperty(variable, "");
        }

        // Remove the found variable from the cycle map, since
        // it may appear more than once in the value and we don't
        // want such situations to appear as a recursive reference.
        cycleMap.remove(variable);

        // Append the leading characters, the substituted value of
        // the variable, and the trailing characters to get the new
        // value.
        val = val.substring(0, startDelim)
                + substValue
                + val.substring(stopDelim + DELIM_STOP.length(), val.length());

        // Now perform substitution again, since there could still
        // be substitutions to make.
        val = substVars(val, currentKey, cycleMap, configProps);

        // Return the value.
        return val;
    }

    private static MavenArtifactProvisionOption convertToMaven(String location) {
        String[] p = location.split("/");
        if (p.length >= 4 && p[p.length-1].startsWith(p[p.length-3] + "-" + p[p.length-2])) {
            MavenArtifactProvisionOption opt = new MavenArtifactProvisionOption();
            int artifactIdVersionLength = p[p.length-3].length() + 1 + p[p.length-2].length(); // (artifactId + "-" + version).length
            if (p[p.length-1].charAt(artifactIdVersionLength) == '-') {
                opt.classifier((p[p.length-1].substring(artifactIdVersionLength + 1, p[p.length-1].lastIndexOf('.'))));
            }
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < p.length - 3; j++) {
                if (j > 0) {
                    sb.append('.');
                }
                sb.append(p[j]);
            }
            opt.groupId(sb.toString());
            opt.artifactId(p[p.length-3]);
            opt.version(p[p.length-2]);
            opt.type(p[p.length-1].substring(p[p.length-1].lastIndexOf('.') + 1));
            return opt;
        } else {
            throw new IllegalArgumentException("Unable to extract maven information from " + location);
        }
    }

    public static Properties getDefaultSystemOptions() {
        return getDefaultSystemOptions("target/karaf.home");
    }

    public static Properties getDefaultSystemOptions(String karafHome) {
        Properties sysProps = new Properties();
        sysProps.setProperty("karaf.name", "root");
        sysProps.setProperty("karaf.home", karafHome);
        sysProps.setProperty("karaf.base", karafHome);
        sysProps.setProperty("karaf.startLocalConsole", "false");
        sysProps.setProperty("karaf.startRemoteShell", "false");
        sysProps.setProperty("org.osgi.framework.startlevel.beginning", "100");
        return sysProps;
    }

    public static Option[] getDefaultConfigOptions() throws Exception {
        return getDefaultConfigOptions(getDefaultSystemOptions(),
                                       getResource("/org/apache/felix/karaf/testing/config.properties"));
    }

    public static Option[] getDefaultConfigOptions(Properties sysProps, URL configProperties) throws Exception {
        // Load props
        Properties configProps = new Properties();
        InputStream is = configProperties.openStream();
        try {
            configProps.load(is);
        } finally {
            is.close();
        }
        // Set system props
        for (Enumeration e = sysProps.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            configProps.setProperty(key, sysProps.getProperty(key));
        }
        // Perform variable substitution for system properties.
        for (Enumeration e = configProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            configProps.setProperty(name, substVars(configProps.getProperty(name), name, null, configProps));
        }
        // Transform to sys props options
        List<Option> options = new ArrayList<Option>();
        for (Enumeration e = configProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = configProps.getProperty(name);
            value = value.replaceAll("\r", "").replaceAll("\n", "").replaceAll(" ", "");
            options.add(new SystemPropertyOption(name).value(value));
            System.err.println("sysprop: " + name + " = " + value);
        }
        if (configProps.getProperty("org.osgi.framework.startlevel.beginning") != null) {
            options.add(frameworkStartLevel(Integer.parseInt(configProps.getProperty("org.osgi.framework.startlevel.beginning"))));
        }
        return options.toArray(new Option[options.size()]);
    }

    public static Option[] getDefaultProvisioningOptions() throws Exception {
        return getDefaultProvisioningOptions(getDefaultSystemOptions(),
                                            getResource("/org/apache/felix/karaf/testing/startup.properties"));
    }

    private static URL getResource(String location) throws Exception {
        URL url = null;
        if (Thread.currentThread().getContextClassLoader() != null) {
            url = Thread.currentThread().getContextClassLoader().getResource(location);
        }
        if (url == null) {
            url = Helper.class.getResource(location);
        }
        System.err.println("Trying to load resource: " + location + ". Found: " + url);
        return url;
    }

    public static Option[] getDefaultProvisioningOptions(Properties sysProps, URL configProperties) throws Exception {
        Properties startupProps = new Properties();
        InputStream is = configProperties.openStream();
        try {
            startupProps.load(is);
        } finally {
            is.close();
        }
        // Perform variable substitution for system properties.
        for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            startupProps.setProperty(name, substVars(startupProps.getProperty(name), name, null, sysProps));
        }
        // Transform to sys props options
        List<Option> options = new ArrayList<Option>();
        options.add(bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework());
        options.add(bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework());
        for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = startupProps.getProperty(name);
            MavenArtifactProvisionOption opt = convertToMaven(name);
            if (opt.getURL().contains("org.apache.felix.karaf.features")) {
                opt.noStart();
            }
            opt.startLevel(Integer.parseInt(value));
            options.add(opt);
        }
        options.add(mavenBundle("org.apache.felix.karaf.tooling", "org.apache.felix.karaf.tooling.testing"));
        // We need to add pax-exam-junit here when running with the ibm
        // jdk to avoid the following exception during the test run:
        // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            options.add(wrappedBundle(maven("org.ops4j.pax.exam", "pax-exam-junit")));
        }
        return options.toArray(new Option[options.size()]);
    }

    public static Option[] getDefaultOptions() throws Exception {
        return combine(getDefaultConfigOptions(), getDefaultProvisioningOptions());
    }

    public static MavenArtifactProvisionOption findMaven(Option[] options, String groupId, String artifactId) {
        for (Option option : options) {
            if (option instanceof MavenArtifactProvisionOption) {
                MavenArtifactProvisionOption mvn = (MavenArtifactProvisionOption) option;
                if (mvn.getURL().startsWith("mvn:" + groupId + "/" + artifactId + "/")) {
                    return mvn;
                }
            }
        }
        return null;
    }

    public abstract static class AbstractIntegrationTest {

        public static final long DEFAULT_TIMEOUT = 30000;

        @Inject
        protected BundleContext bundleContext;

        protected <T> T getOsgiService(Class<T> type, long timeout) {
            return getOsgiService(type, null, timeout);
        }

        protected <T> T getOsgiService(Class<T> type) {
            return getOsgiService(type, null, DEFAULT_TIMEOUT);
        }

        protected <T> T getOsgiService(Class<T> type, String filter, long timeout) {
            ServiceTracker tracker = null;
            try {
                String flt;
                if (filter != null) {
                    if (filter.startsWith("(")) {
                        flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")" + filter + ")";
                    } else {
                        flt = "(&(" + Constants.OBJECTCLASS + "=" + type.getName() + ")(" + filter + "))";
                    }
                } else {
                    flt = "(" + Constants.OBJECTCLASS + "=" + type.getName() + ")";
                }
                Filter osgiFilter = FrameworkUtil.createFilter(flt);
                tracker = new ServiceTracker(bundleContext, osgiFilter, null);
                tracker.open(true);
                // Note that the tracker is not closed to keep the reference
                // This is buggy, as the service reference may change i think
                Object svc = type.cast(tracker.waitForService(timeout));
                if (svc == null) {
                    Dictionary dic = bundleContext.getBundle().getHeaders();
                    System.err.println("Test bundle headers: " + explode(dic));

                    for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, null))) {
                        System.err.println("ServiceReference: " + ref);
                    }

                    for (ServiceReference ref : asCollection(bundleContext.getAllServiceReferences(null, flt))) {
                        System.err.println("Filtered ServiceReference: " + ref);
                    }

                    throw new RuntimeException("Gave up waiting for service " + flt);
                }
                return type.cast(svc);
            } catch (InvalidSyntaxException e) {
                throw new IllegalArgumentException("Invalid filter", e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        protected Bundle installBundle(String groupId, String artifactId) throws Exception {
            MavenArtifactProvisionOption mvnUrl = mavenBundle(groupId, artifactId);
            return bundleContext.installBundle(mvnUrl.getURL());
        }

        protected Bundle getInstalledBundle(String symbolicName) {
            for (Bundle b : bundleContext.getBundles()) {
                if (b.getSymbolicName().equals(symbolicName)) {
                    return b;
                }
            }
            return null;
        }

        public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
            return CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId).versionAsInProject();
        }

        /*
         * Explode the dictionary into a ,-delimited list of key=value pairs
         */
        private static String explode(Dictionary dictionary) {
            Enumeration keys = dictionary.keys();
            StringBuffer result = new StringBuffer();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                result.append(String.format("%s=%s", key, dictionary.get(key)));
                if (keys.hasMoreElements()) {
                    result.append(", ");
                }
            }
            return result.toString();
        }

        /*
         * Provides an iterable collection of references, even if the original array is null
         */
        private static final Collection<ServiceReference> asCollection(ServiceReference[] references) {
            List<ServiceReference> result = new LinkedList<ServiceReference>();
            if (references != null) {
                for (ServiceReference reference : references) {
                    result.add(reference);
                }
            }
            return result;
        }

        private static final String DELIM_START = "${";
        private static final String DELIM_STOP = "}";

        /**
         * <p>
         * This method performs property variable substitution on the
         * specified value. If the specified value contains the syntax
         * <tt>${&lt;prop-name&gt;}</tt>, where <tt>&lt;prop-name&gt;</tt>
         * refers to either a configuration property or a system property,
         * then the corresponding property value is substituted for the variable
         * placeholder. Multiple variable placeholders may exist in the
         * specified value as well as nested variable placeholders, which
         * are substituted from inner most to outer most. Configuration
         * properties override system properties.
         * </p>
         *
         * @param val         The string on which to perform property substitution.
         * @param currentKey  The key of the property being evaluated used to
         *                    detect cycles.
         * @param cycleMap    Map of variable references used to detect nested cycles.
         * @param configProps Set of configuration properties.
         * @return The value of the specified string after system property substitution.
         * @throws IllegalArgumentException If there was a syntax error in the
         *                                  property placeholder syntax or a recursive variable reference.
         */
        private static String substVars(String val, String currentKey,
                                        Map<String, String> cycleMap, Properties configProps)
                throws IllegalArgumentException {
            // If there is currently no cycle map, then create
            // one for detecting cycles for this invocation.
            if (cycleMap == null) {
                cycleMap = new HashMap<String, String>();
            }

            // Put the current key in the cycle map.
            cycleMap.put(currentKey, currentKey);

            // Assume we have a value that is something like:
            // "leading ${foo.${bar}} middle ${baz} trailing"

            // Find the first ending '}' variable delimiter, which
            // will correspond to the first deepest nested variable
            // placeholder.
            int stopDelim = val.indexOf(DELIM_STOP);

            // Find the matching starting "${" variable delimiter
            // by looping until we find a start delimiter that is
            // greater than the stop delimiter we have found.
            int startDelim = val.indexOf(DELIM_START);
            while (stopDelim >= 0) {
                int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
                if ((idx < 0) || (idx > stopDelim)) {
                    break;
                } else if (idx < stopDelim) {
                    startDelim = idx;
                }
            }

            // If we do not have a start or stop delimiter, then just
            // return the existing value.
            if ((startDelim < 0) && (stopDelim < 0)) {
                return val;
            }
            // At this point, we found a stop delimiter without a start,
            // so throw an exception.
            else if (((startDelim < 0) || (startDelim > stopDelim))
                    && (stopDelim >= 0)) {
                throw new IllegalArgumentException(
                        "stop delimiter with no start delimiter: "
                                + val);
            }

            // At this point, we have found a variable placeholder so
            // we must perform a variable substitution on it.
            // Using the start and stop delimiter indices, extract
            // the first, deepest nested variable placeholder.
            String variable =
                    val.substring(startDelim + DELIM_START.length(), stopDelim);

            // Verify that this is not a recursive variable reference.
            if (cycleMap.get(variable) != null) {
                throw new IllegalArgumentException(
                        "recursive variable reference: " + variable);
            }

            // Get the value of the deepest nested variable placeholder.
            // Try to configuration properties first.
            String substValue = (configProps != null)
                    ? configProps.getProperty(variable, null)
                    : null;
            if (substValue == null) {
                // Ignore unknown property values.
                substValue = System.getProperty(variable, "");
            }

            // Remove the found variable from the cycle map, since
            // it may appear more than once in the value and we don't
            // want such situations to appear as a recursive reference.
            cycleMap.remove(variable);

            // Append the leading characters, the substituted value of
            // the variable, and the trailing characters to get the new
            // value.
            val = val.substring(0, startDelim)
                    + substValue
                    + val.substring(stopDelim + DELIM_STOP.length(), val.length());

            // Now perform substitution again, since there could still
            // be substitutions to make.
            val = substVars(val, currentKey, cycleMap, configProps);

            // Return the value.
            return val;
        }

        private static MavenArtifactProvisionOption convertToMaven(String location) {
            String[] p = location.split("/");
            if (p.length >= 4 && p[p.length-1].startsWith(p[p.length-3] + "-" + p[p.length-2])) {
                MavenArtifactProvisionOption opt = new MavenArtifactProvisionOption();
                int artifactIdVersionLength = p[p.length-3].length() + 1 + p[p.length-2].length(); // (artifactId + "-" + version).length
                if (p[p.length-1].charAt(artifactIdVersionLength) == '-') {
                    opt.classifier((p[p.length-1].substring(artifactIdVersionLength + 1, p[p.length-1].lastIndexOf('.'))));
                }
                StringBuffer sb = new StringBuffer();
                for (int j = 0; j < p.length - 3; j++) {
                    if (j > 0) {
                        sb.append('.');
                    }
                    sb.append(p[j]);
                }
                opt.groupId(sb.toString());
                opt.artifactId(p[p.length-3]);
                opt.version(p[p.length-2]);
                opt.type(p[p.length-1].substring(p[p.length-1].lastIndexOf('.') + 1));
                return opt;
            } else {
                throw new IllegalArgumentException("Unable to extract maven information from " + location);
            }
        }

        public static Properties getDefaultSystemOptions() {
            return getDefaultSystemOptions("target/karaf.home");
        }

        public static Properties getDefaultSystemOptions(String karafHome) {
            Properties sysProps = new Properties();
            sysProps.setProperty("karaf.name", "root");
            sysProps.setProperty("karaf.home", karafHome);
            sysProps.setProperty("karaf.base", karafHome);
            sysProps.setProperty("karaf.startLocalConsole", "false");
            sysProps.setProperty("karaf.startRemoteShell", "false");
            sysProps.setProperty("org.osgi.framework.startlevel.beginning", "100");
            return sysProps;
        }

        public static Option[] getDefaultConfigOptions() throws Exception {
            return getDefaultConfigOptions(getDefaultSystemOptions(),
                                           Helper.class.getResource("/config.properties"));
        }

        public static Option[] getDefaultConfigOptions(Properties sysProps, URL configProperties) throws Exception {
            // Load props
            Properties configProps = new Properties();
            InputStream is = configProperties.openStream();
            try {
                configProps.load(is);
            } finally {
                is.close();
            }
            // Set system props
            for (Enumeration e = sysProps.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                configProps.setProperty(key, sysProps.getProperty(key));
            }
            // Perform variable substitution for system properties.
            for (Enumeration e = configProps.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                configProps.setProperty(name, substVars(configProps.getProperty(name), name, null, configProps));
            }
            // Transform to sys props options
            List<Option> options = new ArrayList<Option>();
            for (Enumeration e = configProps.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                String value = configProps.getProperty(name);
                value = value.replaceAll("\r", "").replaceAll("\n", "").replaceAll(" ", "");
                options.add(new SystemPropertyOption(name).value(value));
            }
            if (configProps.getProperty("org.osgi.framework.startlevel.beginning") != null) {
                options.add(frameworkStartLevel(Integer.parseInt(configProps.getProperty("org.osgi.framework.startlevel.beginning"))));
            }
            return options.toArray(new Option[options.size()]);
        }

        public static Option[] getDefaultProvisioningOptions() throws Exception {
            return getDefaultProvisioningOptions(getDefaultSystemOptions(),
                                                 Helper.class.getResource("/startup.properties"));
        }

        public static Option[] getDefaultProvisioningOptions(Properties sysProps, URL configProperties) throws Exception {
            Properties startupProps = new Properties();
            InputStream is = configProperties.openStream();
            try {
                startupProps.load(is);
            } finally {
                is.close();
            }
            // Perform variable substitution for system properties.
            for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                startupProps.setProperty(name, substVars(startupProps.getProperty(name), name, null, sysProps));
            }
            // Transform to sys props options
            List<Option> options = new ArrayList<Option>();
            options.add(bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework());
            options.add(bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework());
            for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
                String name = (String) e.nextElement();
                String value = startupProps.getProperty(name);
                MavenArtifactProvisionOption opt = convertToMaven(name);
                if (opt.getURL().contains("org.apache.felix.karaf.features")) {
                    opt.noStart();
                }
                opt.startLevel(Integer.parseInt(value));
                options.add(opt);
            }
            // We need to add pax-exam-junit here when running with the ibm
            // jdk to avoid the following exception during the test run:
            // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
            if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
                options.add(wrappedBundle(maven("org.ops4j.pax.exam", "pax-exam-junit")));
            }
            return options.toArray(new Option[options.size()]);
        }

        public static Option[] getDefaultOptions() throws Exception {
            return combine(getDefaultConfigOptions(), getDefaultProvisioningOptions());
        }

        public static MavenArtifactProvisionOption findMaven(Option[] options, String groupId, String artifactId) {
            for (Option option : options) {
                if (option instanceof MavenArtifactProvisionOption) {
                    MavenArtifactProvisionOption mvn = (MavenArtifactProvisionOption) option;
                    if (mvn.getURL().startsWith("mvn:" + groupId + "/" + artifactId + "/")) {
                        return mvn;
                    }
                }
            }
            return null;
        }
    }
}