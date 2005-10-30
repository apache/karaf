/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.felix.framework.cache.DefaultBundleCache;
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
        Properties configProps = Main.readConfigProperties();

        // See if the profile name property was specified.
        String profileName = configProps.getProperty(DefaultBundleCache.CACHE_PROFILE_PROP);

        // See if the profile directory property was specified.
        String profileDirName = configProps.getProperty(DefaultBundleCache.CACHE_PROFILE_DIR_PROP);

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
                configProps.setProperty(DefaultBundleCache.CACHE_PROFILE_PROP, profileName);
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
                null);
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
     * are not directly used by the framework in anyway. By default, the system property
     * file is located in the same directory as the <tt>felix.jar</tt> file and
     * is called "<tt>system.properties</tt>". This may be changed by setting the
     * "<tt>felix.system.properties</tt>" system property to an
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
            // Determine where felix.jar is located by looking at the
            // system class path.
            String jarLoc = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index > start)
            {
                jarLoc = classpath.substring(start, index);
                if (jarLoc.length() == 0)
                {
                    jarLoc = ".";
                }
            }
            else
            {
                // Can't figure it out so use the current directory as default.
                jarLoc = System.getProperty("user.dir");
            }

            try
            {
                propURL = new File(jarLoc, SYSTEM_PROPERTIES_FILE_VALUE).toURL();
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

        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();
            System.setProperty(name, substVars((String) props.getProperty(name)));
        }
    }

    /**
     * <p>
     * Reads the configuration properties in the configuration property
     * file associated with the framework installation; these properties are
     * only accessible to the framework and are intended for configuration
     * purposes. By default, the configuration property file is located in
     * the same directory as the <tt>felix.jar</tt> file and is called
     * "<tt>config.properties</tt>". This may be changed by setting the
     * "<tt>felix.config.properties</tt>" system property to an
     * arbitrary URL.
     * </p>
     * @return A <tt>Properties</tt> instance or <tt>null</tt> if there was an error.
    **/
    public static Properties readConfigProperties()
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
            // Determine where felix.jar is located by looking at the
            // system class path.
            String jarLoc = null;
            String classpath = System.getProperty("java.class.path");
            int index = classpath.toLowerCase().indexOf("felix.jar");
            int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
            if (index > start)
            {
                jarLoc = classpath.substring(start, index);
                if (jarLoc.length() == 0)
                {
                    jarLoc = ".";
                }
            }
            else
            {
                // Can't figure it out so use the current directory as default.
                jarLoc = System.getProperty("user.dir");
            }

            try
            {
                propURL = new File(jarLoc, CONFIG_PROPERTIES_FILE_VALUE).toURL();
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
            props.setProperty(name, substVars((String) props.getProperty(name)));
        }

        return props;
    }

    private static final String DELIM_START = "${";
    private static final char DELIM_STOP  = '}';
    private static final int DELIM_START_LEN = 2;
    private static final int DELIM_STOP_LEN  = 1;

    /**
     * <p>
     * This method performs system property variable substitution on the
     * specified string value. If the specified string contains the
     * syntax <tt>${&lt;system-prop-name&gt;}</tt>, then the corresponding
     * system property value is substituted for the marker.
     * </p>
     * @param val The string on which to perform system property substitution.
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         system property variable marker syntax.
    **/
    public static String substVars(String val)
        throws IllegalArgumentException
    {
        StringBuffer sbuf = new StringBuffer();

        if (val == null)
        {
            return val;
        }

        int i = 0;
        int j, k;

        while (true)
        {
            j = val.indexOf(DELIM_START, i);
            if (j == -1)
            {
                if (i == 0)
                {
                    return val;
                }
                else
                {
                    sbuf.append(val.substring(i, val.length()));
                    return sbuf.toString();
                }
            }
            else
            {
                sbuf.append(val.substring(i, j));
                k = val.indexOf(DELIM_STOP, j);
                if (k == -1)
                {
                    throw new IllegalArgumentException(
                    '"' + val +
                    "\" has no closing brace. Opening brace at position "
                    + j + '.');
                }
                else
                {
                    j += DELIM_START_LEN;
                    String key = val.substring(j, k);
                    // Try system properties.
                    String replacement = System.getProperty(key, null);
                    if (replacement != null)
                    {
                        sbuf.append(replacement);
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }
}