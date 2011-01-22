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
package org.apache.karaf.testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Manifest;

import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.options.JUnitBundlesOption;
import org.ops4j.pax.exam.options.MavenArtifactProvisionOption;
import org.ops4j.pax.exam.container.def.options.FeaturesScannerProvisionOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.osgi.framework.Constants;

import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.bootDelegationPackages;
import static org.ops4j.pax.exam.CoreOptions.frameworkStartLevel;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.vmOption;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;

/**
 * Helper class for setting up a pax-exam test environment for karaf.
 *
 * A simple configuration for pax-exam can be create using the following
 * code:
 * <pre>
 *   @Configuration
 *   public static Option[] configuration() throws Exception{
 *       return combine(
 *           // Default karaf environment
 *           Helper.getDefaultOptions(),
 *           // Test on both equinox and felix
 *           equinox(), felix()
 *       );
 *   }
 * </pre>
 *
 */
public final class Helper {
	
	public static final String INCLUDES_PROPERTY = "${includes}";

    private Helper() {
    }

    public static Customizer felixProvisionalApis() {
        return new Customizer() {
            @Override
            public InputStream customizeTestProbe(InputStream testProbe) throws Exception {
                Manifest mf = new Manifest();
                testProbe = HeaderParser.wireTapManifest( testProbe, mf );
                List<HeaderParser.PathElement> elems = HeaderParser.parseHeader( mf.getMainAttributes().getValue( Constants.IMPORT_PACKAGE ));
                StringBuilder sb = new StringBuilder();
                for (HeaderParser.PathElement elem : elems) {
                    if (elem.getName().startsWith("org.apache.felix.service.")) {
                        elem.getAttributes().put("status", "provisional");
                    }
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(elem.toString());
                }

                System.out.println("==============================");
                System.out.println("");
                System.out.println("Old: " + mf.getMainAttributes().getValue( Constants.IMPORT_PACKAGE ));
                System.out.println("");
                System.out.println("==============================");
                System.out.println("");
                System.out.println("New: " + sb.toString());
                System.out.println("");
                System.out.println("==============================");

                return modifyBundle( testProbe )
                            .set( Constants.IMPORT_PACKAGE, sb.toString() )
                            .build();
            }
        };
    }

    /**
     *  Create an provisioning option for the specified maven artifact
     * (groupId and artifactId), using the version found in the list
     * of dependencies of this maven project.
     *
     * @param groupId the groupId of the maven bundle
     * @param artifactId the artifactId of the maven bundle
     * @return the provisioning option for the given bundle
     */
    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId, null, null, null);
    }

    public static MavenArtifactProvisionOption mavenBundle(String groupId, String artifactId, String version, String classifier, String type) {
        MavenArtifactProvisionOption m = CoreOptions.mavenBundle().groupId(groupId).artifactId(artifactId);
        if (version != null) {
            m.version(version);
        } else {
            try {
                m.versionAsInProject();
            } catch (RuntimeException t) {
                //in eclipse, the dependencies.properties may not be avail since it's not
                //generated into a source directory (directly into classes).
                //thus, try and load it manually

                try {
                    File file = new File("META-INF/maven/dependencies.properties");
                    if (!file.exists()) {
                        file = new File("target/classes/META-INF/maven/dependencies.properties");
                    }
                    if (file.exists()) {
                        Properties dependencies = new Properties();
                        InputStream is = new FileInputStream(file);
                        try {
                            dependencies.load(is);
                        } finally {
                            is.close();
                        }
                        version = dependencies.getProperty( groupId + "/" + artifactId + "/version" );
                        m.version(version);
                    } else {
                        throw t;
                    }
                } catch (Throwable t2) {
                    throw t;
                }
            }
        }
        if (classifier != null) {
            m.classifier(classifier);
        }
        if (type != null) {
            m.type(type);
        }
        return m;
    }

    /**
     * Return a map of system properties for karaf.
     * The default karaf home directory is "target/karaf.home".
     *
     * @return a list of system properties for karaf
     */
    public static Properties getDefaultSystemOptions() {
        return getDefaultSystemOptions("target/karaf.home");
    }

    /**
     * Return a map of system properties for karaf,
     * using the specified folder for the karaf home directory.
     *
     * @param karafHome the karaf home directory
     * @return a list of system properties for karaf
     */
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

    /**
     * Return an array of pax-exam options to correctly configure the osgi
     * framework for karaf.
     *
     * @param sysOptions test-specific system property options
     * @return default pax-exam options for karaf osgi framework
     */
    public static Option[] getDefaultConfigOptions(SystemPropertyOption... sysOptions) {
        return getDefaultConfigOptions(getDefaultSystemOptions(),
                                       getResource("/org/apache/karaf/testing/config.properties"),
                                       sysOptions);
    }

    /**
     * Return an array of pax-exam options to configure the osgi
     * framework for karaf, given the system properties and the
     * location of the osgi framework properties file.
     *
     * @param sysProps karaf system properties
     * @param configProperties the URL to load the osgi framework properties from
     * @param sysOptions test-specific system property options
     * @return pax-exam options for karaf osgi framework
     */
    public static Option[] getDefaultConfigOptions(Properties sysProps, URL configProperties, SystemPropertyOption... sysOptions) {
        // Load props
        Properties configProps = loadProperties(configProperties);

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
        // Transform system properties to VM options
        List<Option> options = new ArrayList<Option>();
        String vmOptions = "";
        for (Enumeration e = configProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = configProps.getProperty(name);
            value = align(value);
            if ("org.osgi.framework.system.packages".equals(name)) {
            	String extra = align(configProps.getProperty("org.osgi.framework.system.packages.extra"));
            	if (extra != null && extra.length() > 0) {
                	vmOptions = vmOptions + " -D" + name + "=" + value + "," + extra;
                } else {
                	vmOptions = vmOptions + " -D" + name + "=" + value;
                }
            } else if ("org.osgi.framework.bootdelegation".equals(name)) {
                options.add(bootDelegationPackages(value));
            } else {
                vmOptions = vmOptions + " -D" + name + "=" + value;
            }
        }

        // add test-specific system properties
        if (sysOptions != null) {
            for (SystemPropertyOption sysOption : sysOptions) {
                vmOptions = vmOptions + " -D" + sysOption.getKey() + "=" + sysOption.getValue();
            }
        }

        if (configProps.getProperty("org.osgi.framework.startlevel.beginning") != null) {
            options.add(frameworkStartLevel(Integer.parseInt(configProps.getProperty("org.osgi.framework.startlevel.beginning"))));
        }

        options.add(vmOption(vmOptions));
 
        return options.toArray(new Option[options.size()]);
    }

    /**
     * Return an array of pax-exam options for the provisioning of karaf system bundles.
     *
     * @return an array of pax-exam options for provisioning karaf system bundles
     */
    public static Option[] getDefaultProvisioningOptions() {
        return getDefaultProvisioningOptions(getDefaultSystemOptions(),
                                            getResource("/org/apache/karaf/testing/startup.properties"));
    }

    /**
     * Return an array of pax-exam options for the provisioning of karaf system bundles,
     * given the karaf system properties and the location of the startup bundles config file.
     *
     * @param sysProps karaf system properties
     * @param startupProperties the URL to load the system bundles from
     * @return an array of pax-exam options for provisioning karaf system bundles
     */
    public static Option[] getDefaultProvisioningOptions(Properties sysProps, URL startupProperties) {
        Properties startupProps = loadProperties(startupProperties);
        // Perform variable substitution for system properties.
        for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            startupProps.setProperty(name, substVars(startupProps.getProperty(name), name, null, sysProps));
        }
        // Transform to sys props options
        List<Option> options = new ArrayList<Option>();
        options.add(bootClasspathLibrary(mavenBundle("org.apache.karaf.jaas", "org.apache.karaf.jaas.boot")).afterFramework());
        options.add(bootClasspathLibrary(mavenBundle("org.apache.karaf", "org.apache.karaf.main")).afterFramework());
        for (Enumeration e = startupProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            String value = startupProps.getProperty(name);
            MavenArtifactProvisionOption opt = convertToMaven(name);
            if (opt.getURL().contains("org.apache.karaf.features")) {
                opt.noStart();
            }
            opt.startLevel(Integer.parseInt(value));
            options.add(opt);
        }
        options.add(mavenBundle("org.apache.karaf.tooling", "org.apache.karaf.tooling.testing"));
        options.add(wrappedBundle(mavenBundle("org.ops4j.pax.exam", "pax-exam-container-default")));
        // We need to add pax-exam-junit here when running with the ibm
        // jdk to avoid the following exception during the test run:
        // ClassNotFoundException: org.ops4j.pax.exam.junit.Configuration
        if ("IBM Corporation".equals(System.getProperty("java.vendor"))) {
            options.add(wrappedBundle(maven("org.ops4j.pax.exam", "pax-exam-junit")));
        }
		// Add ServiceMix junit bundle
		JUnitBundlesOption jubo = new JUnitBundlesOption();
		((MavenArtifactProvisionOption) jubo.getDelegate()).groupId("org.apache.servicemix.bundles").artifactId("org.apache.servicemix.bundles.junit").version("4.7_1");
		options.add(jubo);
        return options.toArray(new Option[options.size()]);
    }

    /**
     * Return an array of options for setting up a pax-exam test environment for karaf.
     *
     * @return an array of pax-exam options
     */
    public static Option[] getDefaultOptions() {
        return getDefaultOptions(null);
    }

    /**
     * Return an array of options for setting up a pax-exam test environment for karaf.
     *
     * @param sysOptions test-specific system property options
     * @return an array of pax-exam options
     */
    public static Option[] getDefaultOptions(SystemPropertyOption... sysOptions) {
        return combine(getDefaultConfigOptions(sysOptions), getDefaultProvisioningOptions());
    }

    /**
     * Configures the required system property to set the log-level in Karaf.
     *
     * @param logLevel the log level which should be used for pax-logging. Possible values are TRACE, DEBUG, INFO, WARN,
     *        ERROR and FATAL
     * @return a pax-exam option
     */
    public static SystemPropertyOption setLogLevel(String logLevel) {
        return systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value(logLevel);
    }

    /**
     * Method to directly register Karaf standard features.
     *
     * @param features a list of features which should be loaded from the Karaf standard feature file.
     * @return a pax-exam option
     */
    public static FeaturesScannerProvisionOption loadKarafStandardFeatures(String... features) {
        return scanFeatures(
            maven().groupId("org.apache.karaf.features.assembly").artifactId("standard").type("xml")
                .classifier("features").versionAsInProject(), features);
    }

    /**
     * Method to directly register Karaf enterprise features.
     * 
     * @param features a list of features which should be loaded from the Karaf enterprise feature file.
     * @return a pax-exam option
     */
    public static FeaturesScannerProvisionOption loadKarafEnterpriseFeatures(String... features) {
        return scanFeatures(
            maven().groupId("org.apache.karaf.features.assembly").artifactId("enterprise").type("xml")
                .classifier("features").versionAsInProject(), features);
    }

    /**
     * Returns the vmOption to configure pax exam with debugging support.
     *
     * @param debuggingPort the port where the remote debugger should be allowed to be attached
     * @return the option to enable debugging
     */
    public static Option activateDebugging(String debuggingPort) {
        return vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=" + debuggingPort);
    }

    /**
     * Retrieve the pax-exam option for provisioning the given maven bundle.
     *
     * @param options the list of pax-exam options
     * @param groupId the maven group id
     * @param artifactId the maven artifact id
     * @return the pax-exam provisioning option for the bundle or <code>null</code> if not found
     */
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

    private static Properties loadProperties(URL location) {
        try {
            Properties props = new Properties();
            InputStream is = location.openStream();
            try {
                props.load(is);
            } finally {
                is.close();
            }
            String includes = props.getProperty(INCLUDES_PROPERTY);
            if (includes != null) {
                StringTokenizer st = new StringTokenizer(includes, "\" ", true);
                if (st.countTokens() > 0) {
                    String includeUrl;
                    do {
                    	includeUrl = nextLocation(st);
                        if (includeUrl != null) {
                        	URL url = new URL(location, includeUrl);
                            Properties properties = loadProperties(url);
                            props.putAll(properties);
                        }
                    }
                    while (includeUrl != null);
                }
                props.remove(INCLUDES_PROPERTY);
            }
            return props;
        } catch (IOException e) {
            throw new RuntimeException("Unable to load properties from " + location, e);
        }
    }

    private static URL getResource(String location) {
        URL url = null;
        if (Thread.currentThread().getContextClassLoader() != null) {
            url = Thread.currentThread().getContextClassLoader().getResource(location);
        }
        if (url == null) {
            url = Helper.class.getResource(location);
        }
        if (url == null) {
            throw new RuntimeException("Unable to find resource " + location);
        }
        return url;
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


    private static String align(String value) {
        return value != null ? value.replaceAll("\r", "").replaceAll("\n", "").replaceAll(" ", "") : "";
    }

    private static String nextLocation(StringTokenizer st) {
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok = null;
            boolean inQuote = false;
            boolean tokStarted = false;
            boolean exit = false;
            while ((st.hasMoreTokens()) && (!exit)) {
                tok = st.nextToken(tokenList);
                if (tok.equals("\"")) {
                    inQuote = !inQuote;
                    if (inQuote) {
                        tokenList = "\"";
                    } else {
                        tokenList = "\" ";
                    }

                } else if (tok.equals(" ")) {
                    if (tokStarted) {
                        retVal = tokBuf.toString();
                        tokStarted = false;
                        tokBuf = new StringBuffer(10);
                        exit = true;
                    }
                } else {
                    tokStarted = true;
                    tokBuf.append(tok.trim());
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
}
