package org.apache.karaf.main.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

public class PropertiesHelper {
    private static final String INCLUDES_PROPERTY = "${includes}";

    private static final String DELIM_START = "${";
    private static final String DELIM_STOP = "}";

     /**
      * <p>
      * Loads the properties into into <tt>System.setProperty()</tt>. These properties
      * are not directly used by the framework in anyway. By default, the system
      * property file is located in the <tt>etc/</tt> directory of the karaf
      * installation directory and is called "<tt>system.properties</tt>".
      * The precise file from which to load system properties can be set by
      * initializing the "<tt>system.properties</tt>" system property to an
      * arbitrary URL.
      * </p>
      */
     public static void updateSystemProperties(Properties props) {
		for (Enumeration<?> e = props.propertyNames(); e.hasMoreElements();) {
		    String name = (String) e.nextElement();
		    String value = System.getProperty(name, props.getProperty(name));
		    System.setProperty(name, substVars(value, name, null, props));
		}
	}

	/**
	 * Perform variable substitution for the given properties
	 * @param configProps
	 */
	public static void substituteVariables(Properties configProps) {
        for (Enumeration<?> e = configProps.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            configProps.setProperty(name,
                    substVars(configProps.getProperty(name), name, null, configProps));
        }
	}

    public static String nextLocation(StringTokenizer st) {
        String retVal = null;

        if (st.countTokens() > 0) {
            String tokenList = "\" ";
            StringBuffer tokBuf = new StringBuffer(10);
            String tok;
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
    
    public static Properties loadPropertiesFile(File baseFolder, String fileName, boolean failIfNotFound) throws Exception {
        File file = new File(baseFolder, fileName);
        URL configPropURL = file.toURI().toURL();
        return loadPropertiesUrl(configPropURL, failIfNotFound);
    }

    protected static Properties loadPropertiesUrl(URL configPropURL, boolean failIfNotFound) throws Exception {
        // Read the properties file.
        Properties configProps = new Properties();
        InputStream is = null;
        try {
            is = configPropURL.openConnection().getInputStream();
            configProps.load(is);
            is.close();
        }
        catch (FileNotFoundException ex) {
            if (failIfNotFound) {
                throw ex;
            }
        }
        catch (Exception ex) {
            System.err.println("Error loading config properties from " + configPropURL);
            System.err.println("Main: " + ex);
            return configProps;
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            }
            catch (IOException ex2) {
                // Nothing we can do.
            }
        }
        addIncludes(configPropURL, configProps);
        trimValues(configProps);
        return configProps;
    }

	private static void trimValues(Properties configProps) {
		for (Enumeration<?> e = configProps.propertyNames(); e.hasMoreElements();) {
            Object key = e.nextElement();
            if (key instanceof String) {
                String v = configProps.getProperty((String) key);
                configProps.put(key, v.trim());
            }
        }
	}

	private static void addIncludes(URL configPropURL, Properties configProps)
			throws MalformedURLException, Exception {
		String includes = configProps.getProperty(INCLUDES_PROPERTY);
        if (includes != null) {
            StringTokenizer st = new StringTokenizer(includes, "\" ", true);
            if (st.countTokens() > 0) {
                String location;
                do {
                    location = nextLocation(st);
                    if (location != null) {
                        URL url = new URL(configPropURL, location);
                        Properties props = loadPropertiesUrl(url, true);
                        configProps.putAll(props);
                    }
                }
                while (location != null);
            }
            configProps.remove(INCLUDES_PROPERTY);
        }
	}

    public static void copySystemProperties(Properties configProps) {
        for (Enumeration<?> e = System.getProperties().propertyNames();
             e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.startsWith("felix.") ||
                    key.startsWith("karaf.") ||
                    key.startsWith("org.osgi.framework.")) {
                configProps.setProperty(key, System.getProperty(key));
            }
        }
    }
    
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
    public static String substVars(String val, String currentKey,
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
    

}
