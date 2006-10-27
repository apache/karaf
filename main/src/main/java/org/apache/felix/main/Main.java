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
package org.apache.felix.main;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.util.*;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.MutablePropertyResolverImpl;
import org.apache.felix.framework.util.StringMap;

/**
 * <p>
 * This class is the default way to instantiate and execute the framework. It is not
 * intended to be the only way to instantiate and execute the framework; rather, it is
 * one example of how to do so. When embedding the framework in a host application,
 * this class can serve as a simple guide of how to do so. It may even be
 * worthwhile to reuse some of its property handling capabilities. This class
 * is completely static and is only intended to start a single instance of
 * the framework.
 * </p>
**/
public class Main
{
    /**
     * The system property name used to specify an URL to the system
     * property file.
    **/
    public static final String SYSTEM_PROPERTIES_PROP = "felix.system.properties";
    /**
     * The default name used for the system properties file.
    **/
    public static final String SYSTEM_PROPERTIES_FILE_VALUE = "system.properties";
    /**
     * The system property name used to specify an URL to the configuration
     * property file to be used for the created the framework instance.
    **/
    public static final String CONFIG_PROPERTIES_PROP = "felix.config.properties";
    /**
     * The default name used for the configuration properties file.
    **/
    public static final String CONFIG_PROPERTIES_FILE_VALUE = "config.properties";

    public static final String KEYSTORE_FILE_PROP = "felix.keystore";

    public static final String KEYSTORE_FILE_VALUE = System.getProperty("java.home") +
        File.separatorChar + "lib" + File.separatorChar + "security" +
        File.separatorChar + "cacerts" + File.pathSeparatorChar + System.getProperty("user.home") +
        File.separatorChar + ".keystore";

    public static final String KEYSTORE_TYPE_PROP = "felix.keystore.type";

    public static final String KEYSTORE_TYPE_VALUE = "JKS" + File.pathSeparatorChar + "JKS";

    public static final String KEYSTORE_PASS_PROP = "felix.keystore.pass";

    public static final String KEYSTORE_PASS_VALUE = "changeit" + File.pathSeparatorChar + "changeit";

    private static Felix m_felix = null;

    /**
     * <p>
     * This method performs the main task of constructing an framework instance
     * and starting its execution. The following functions are performed
     * when invoked:
     * </p>
     * <ol>
     *   <li><i><b>Read the system properties file.<b></i> This is a file
     *       containing properties to be pushed into <tt>System.setProperty()</tt>
     *       before starting the framework. This mechanism is mainly shorthand
     *       for people starting the framework from the command line to avoid having
     *       to specify a bunch of <tt>-D</tt> system property definitions.
     *       The only properties defined in this file that will impact the framework's
     *       behavior are the those concerning setting HTTP proxies, such as
     *       <tt>http.proxyHost</tt>, <tt>http.proxyPort</tt>, and
     *       <tt>http.proxyAuth</tt>.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on system
     *       properties.</b></i> Any system properties in the system property
     *       file whose value adheres to <tt>${&lt;system-prop-name&gt;}</tt>
     *       syntax will have their value substituted with the appropriate
     *       system property value.
     *   </li>
     *   <li><i><b>Read the framework's configuration property file.</b></i> This is
     *       a file containing properties used to configure the framework
     *       instance and to pass configuration information into
     *       bundles installed into the framework instance. The configuration
     *       property file is called <tt>config.properties</tt> by default
     *       and is located in the same directory as the <tt>felix.jar</tt>
     *       file, which is typically in the <tt>lib/</tt> directory of the
     *       Felix installation directory. It is possible to use a different
     *       location for the property file by specifying the desired URL
     *       using the <tt>felix.config.properties</tt> system property;
     *       this should be set using the <tt>-D</tt> syntax when executing
     *       the JVM. Refer to the
     *       <a href="Felix.html#start(org.apache.felix.framework.util.MutablePropertyResolver, org.apache.felix.framework.util.MutablePropertyResolver, java.util.List)">
     *       <tt>Felix.start()</tt></a> method documentation for more
     *       information on the framework configuration options.
     *   </li>
     *   <li><i><b>Perform system property variable substitution on configuration
     *       properties.</b></i> Any configuration properties whose value adheres to
     *       <tt>${&lt;system-prop-name&gt;}</tt> syntax will have their value
     *       substituted with the appropriate system property value.
     *   </li>
     *   <li><i><b>Ensure the default bundle cache has sufficient information to
     *       initialize.</b></i> The default implementation of the bundle cache
     *       requires either a profile name or a profile directory in order to
     *       start. The configuration properties are checked for at least one
     *       of the <tt>felix.cache.profile</tt> or <tt>felix.cache.profiledir</tt>
     *       properties. If neither is found, the user is asked to supply a profile
     *       name that is added to the configuration property set. See the
     *       <a href="cache/DefaultBundleCache.html"><tt>DefaultBundleCache</tt></a>
     *       documentation for more details its configuration options.
     *   </li>
     *   <li><i><b>Creates and starts a framework instance.</b></i> A simple
     *       <a href="util/MutablePropertyResolver.html"><tt>MutablePropertyResolver</tt></a>
     *       is created for the configuration property file and is passed
     *       into the framework when it is started.
     *   </li>
     * </ol>
     * <p>
     * It should be noted that simply starting an instance of the framework is not enough
     * to create an interactive session with it. It is necessary to install
     * and start bundles that provide an interactive impl; this is generally
     * done by specifying an "auto-start" property in the framework configuration
     * property file. If no interactive impl bundles are installed or if
     * the configuration property file cannot be found, the framework will appear to
     * be hung or deadlocked. This is not the case, it is executing correctly,
     * there is just no way to interact with it. Refer to the
     * <a href="Felix.html#start(org.apache.felix.framework.util.MutablePropertyResolver, org.apache.felix.framework.util.MutablePropertyResolver, java.util.List)">
     * <tt>Felix.start()</tt></a> method documentation for more information on
     * framework configuration options.
     * </p>
     * @param argv An array of arguments, all of which are ignored.
     * @throws Exception If an error occurs.
    **/
    public static void main(String[] argv) throws Exception
    {
        // Load system properties.
        Main.loadSystemProperties();

        // Read configuration properties.
        Properties configProps = Main.loadConfigProperties();

        // See if the profile name property was specified.
        String profileName = configProps.getProperty(BundleCache.CACHE_PROFILE_PROP);

        // See if the profile directory property was specified.
        String profileDirName = configProps.getProperty(BundleCache.CACHE_PROFILE_DIR_PROP);

        // Print welcome banner.
        System.out.println("\nWelcome to Felix.");
        System.out.println("=================\n");

        // If no profile or profile directory is specified in the
        // properties, then ask for a profile name.
        if ((profileName == null) && (profileDirName == null))
        {
            System.out.print("Enter profile name: ");
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            try
            {
                profileName = in.readLine();
            }
            catch (IOException ex)
            {
                System.err.println("Could not read input.");
                System.exit(-1);
            }
            System.out.println("");
            if (profileName.length() != 0)
            {
                configProps.setProperty(BundleCache.CACHE_PROFILE_PROP, profileName);
            }
        }

        // A profile directory or name must be specified.
        if ((profileDirName == null) && (profileName.length() == 0))
        {
            System.err.println("You must specify a profile name or directory.");
            System.exit(-1);
        }

        try
        {
            // Now create an instance of the framework.
            m_felix = new Felix();
            m_felix.start(
                new MutablePropertyResolverImpl(new StringMap(configProps, false)),
                null, (System.getSecurityManager() == null) ? null : new TrustManager(configProps));
        }
        catch (Exception ex)
        {
            System.err.println("Could not create framework: " + ex);
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * <p>
     * Loads the properties in the system property file associated with the
     * framework installation into <tt>System.setProperty()</tt>. These properties
     * are not directly used by the framework in anyway. By default, the system
     * property file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>system.properties</tt>". The
     * installation directory of Felix is assumed to be the parent directory of
     * the <tt>felix.jar</tt> file as found on the system class path property.
     * The precise file from which to load system properties can be set by
     * initializing the "<tt>felix.system.properties</tt>" system property to an
     * arbitrary URL.
     * </p>
    **/
    public static void loadSystemProperties()
    {
        // The system properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.

        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty(SYSTEM_PROPERTIES_PROP);
        if (custom != null)
        {
            try
            {
                propURL = new URL(custom);
            }
            catch (MalformedURLException ex)
            {
                System.err.print("Main: " + ex);
                return;
            }
        }
        else
        {
            // Determine where the configuration directory is by figuring
            // out where felix.jar is located on the system class path.
            File confDir = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index > start)
            {
                String jarLocation = classpath.substring(start, index);
                if (jarLocation.length() == 0)
                {
                    jarLocation = ".";
                }
                confDir = new File(new File(jarLocation).getParent(), "conf");
            }
            else
            {
                // Can't figure it out so use the current directory as default.
                confDir = new File(System.getProperty("user.dir"));
            }

            try
            {
                propURL = new File(confDir, SYSTEM_PROPERTIES_FILE_VALUE).toURL();
            }
            catch (MalformedURLException ex)
            {
                System.err.print("Main: " + ex);
                return;
            }
        }

        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = propURL.openConnection().getInputStream();
            props.load(is);
            is.close();
        }
        catch (FileNotFoundException ex)
        {
            // Ignore file not found.
        }
        catch (Exception ex)
        {
            System.err.println(
                "Main: Error loading system properties from " + propURL);
            System.err.println("Main: " + ex);
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex2)
            {
                // Nothing we can do.
            }
            return;
        }

        // Perform variable substitution on specified properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            System.setProperty(name,
                substVars(props.getProperty(name), name, null, null));
        }
    }

    /**
     * <p>
     * Loads the configuration properties in the configuration property file
     * associated with the framework installation; these properties
     * are accessible to the framework and to bundles and are intended
     * for configuration purposes. By default, the configuration property
     * file is located in the <tt>conf/</tt> directory of the Felix
     * installation directory and is called "<tt>config.properties</tt>".
     * The installation directory of Felix is assumed to be the parent
     * directory of the <tt>felix.jar</tt> file as found on the system class
     * path property. The precise file from which to load configuration
     * properties can be set by initializing the "<tt>felix.config.properties</tt>"
     * system property to an arbitrary URL.
     * </p>
     * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
    **/
    public static Properties loadConfigProperties()
    {
        // The config properties file is either specified by a system
        // property or it is in the same directory as the Felix JAR file.
        // Try to load it from one of these places.

        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty(CONFIG_PROPERTIES_PROP);
        if (custom != null)
        {
            try
            {
                propURL = new URL(custom);
            }
            catch (MalformedURLException ex)
            {
                System.err.print("Main: " + ex);
                return null;
            }
        }
        else
        {
            // Determine where the configuration directory is by figuring
            // out where felix.jar is located on the system class path.
            File confDir = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index > start)
            {
                String jarLocation = classpath.substring(start, index);
                if (jarLocation.length() == 0)
                {
                    jarLocation = ".";
                }
                confDir = new File(new File(jarLocation).getParent(), "conf");
            }
            else
            {
                // Can't figure it out so use the current directory as default.
                confDir = new File(System.getProperty("user.dir"));
            }

            try
            {
                propURL = new File(confDir, CONFIG_PROPERTIES_FILE_VALUE).toURL();
            }
            catch (MalformedURLException ex)
            {
                System.err.print("Main: " + ex);
                return null;
            }
        }

        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = propURL.openConnection().getInputStream();
            props.load(is);
            is.close();
        }
        catch (FileNotFoundException ex)
        {
            // Ignore file not found.
        }
        catch (Exception ex)
        {
            System.err.println(
                "Error loading config properties from " + propURL);
            System.err.println("Main: " + ex);
            try
            {
                if (is != null) is.close();
            }
            catch (IOException ex2)
            {
                // Nothing we can do.
            }
            return null;
        }

        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            props.setProperty(name,
                substVars(props.getProperty(name), name, null, props));
        }

        return props;
    }

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP  = "}";

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
     * @param val The string on which to perform property substitution.
     * @param currentKey The key of the property being evaluated used to
     *        detect cycles.
     * @param cycleMap Map of variable references used to detect nested cycles.
     * @param configProps Set of configuration properties.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         property placeholder syntax or a recursive variable reference.
    **/
    public static String substVars(String val, String currentKey,
        Map cycleMap, Properties configProps)
        throws IllegalArgumentException
    {
        // If there is currently no cycle map, then create
        // one for detecting cycles for this invocation.
        if (cycleMap == null)
        {
            cycleMap = new HashMap();
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
        while (stopDelim >= 0)
        {
            int idx = val.indexOf(DELIM_START, startDelim + DELIM_START.length());
            if ((idx < 0) || (idx > stopDelim))
            {
                break;
            }
            else if (idx < stopDelim)
            {
                startDelim = idx;
            }
        }

        // If we do not have a start or stop delimiter, then just
        // return the existing value.
        if ((startDelim < 0) && (stopDelim < 0))
        {
            return val;
        }
        // At this point, we found a stop delimiter without a start,
        // so throw an exception.
        else if (((startDelim < 0) || (startDelim > stopDelim))
            && (stopDelim >= 0))
        {
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
        if (cycleMap.get(variable) != null)
        {
            throw new IllegalArgumentException(
                "recursive variable reference: " + variable);
        }

        // Get the value of the deepest nested variable placeholder.
        // Try to configuration properties first.
        String substValue = (configProps != null)
            ? configProps.getProperty(variable, null)
            : null;
        if (substValue == null)
        {
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

    private static class TrustManager extends AbstractCollection
    {
        private String[] m_keystores = null;
        private String[] m_passwds = null;
        private String[] m_types = null;
        private ArrayList m_stores = null;

        TrustManager(Properties config)
        {
            StringTokenizer tok = new StringTokenizer(System.getProperty(KEYSTORE_FILE_PROP,
                config.getProperty(KEYSTORE_FILE_PROP, KEYSTORE_FILE_VALUE)), File.pathSeparator);

            m_keystores = new String[tok.countTokens()];

            for (int i = 0;tok.hasMoreTokens();i++)
            {
                m_keystores[i] = tok.nextToken();
            }

            tok = new StringTokenizer(System.getProperty(KEYSTORE_PASS_PROP,
                config.getProperty(KEYSTORE_PASS_PROP, KEYSTORE_PASS_VALUE)), File.pathSeparator);

            m_passwds = new String[tok.countTokens()];

            for (int i = 0;tok.hasMoreTokens();i++)
            {
                m_passwds[i] = tok.nextToken();
            }

            tok = new StringTokenizer(System.getProperty(KEYSTORE_TYPE_PROP,
                config.getProperty(KEYSTORE_TYPE_PROP, KEYSTORE_TYPE_VALUE)), File.pathSeparator);

            m_types = new String[tok.countTokens()];

            for (int i = 0;tok.hasMoreTokens();i++)
            {
                m_types[i] = tok.nextToken();
            }
        }

        public synchronized Iterator iterator()
        {
            if (m_stores == null)
            {
                loadStores();
            }

            return m_stores.iterator();
        }

        public synchronized int size()
        {
            if (m_stores == null)
            {
                loadStores();
            }

            return m_stores.size();
        }

        private void loadStores()
        {
            m_stores = new ArrayList();

            if ((m_keystores.length == m_passwds.length) && (m_passwds.length == m_types.length)
                && (System.getSecurityManager() != null))
            {
                AccessController.doPrivileged(new PrivilegedAction()
                {
                    public Object run()
                    {
                        List certs = new ArrayList();

                        for (int i = 0;i < m_keystores.length;i++)
                        {

                            try
                            {
                                KeyStore ks = KeyStore.getInstance(m_types[i]);
                                ks.load(new FileInputStream(m_keystores[i]), m_passwds[i].toCharArray());
                                for (Enumeration e = ks.aliases(); e.hasMoreElements();)
                                {
                                    String alias = (String) e.nextElement();
                                    if (ks.isCertificateEntry(alias))
                                    {
                                        certs.add(ks.getCertificate(alias));
                                    }
                                }
                            }
                            catch (Exception ex)
                            {
                                certs.clear();
                                ex.printStackTrace(System.err);

                                System.err.println("WARNING: Error accessing keystore: " + m_keystores[i]);
                            }

                            if (!certs.isEmpty())
                            {
                                m_stores.addAll(certs);
                                certs.clear();
                            }
                        }

                        return null;
                    }
                });
            }
            if (m_stores.isEmpty())
            {
                System.err.println("WARNING: No trusted CA certificates!");
            }
        }
    }
}
