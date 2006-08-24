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
package org.apache.felix.tool.mangen;

import java.io.File;
import java.io.FileInputStream;

import java.util.Properties;

/**
 *
 * @version $Revision: 14 $
 * @author <A HREF="mailto:robw@ascert.com">Rob Walker</A> 
 */ 
public class PropertyManager
{
    //////////////////////////////////////////////////
    // STATIC VARIABLES
    //////////////////////////////////////////////////

    private static final String DELIM_START = "${";
    private static final char DELIM_STOP  = '}';
    private static final int DELIM_START_LEN = 2;
    private static final int DELIM_STOP_LEN  = 1;
    
    public static Properties    props = new Properties();
    
    //////////////////////////////////////////////////
    // STATIC PUBLIC METHODS
    //////////////////////////////////////////////////
    
    /**
     * Initialise the properties either from a file specified by -Dmangen.properties
     * or if this is not specified, from the default lib\mangen.properties file.
     */
    public static void initProperties(String propFileKey)
            throws Exception
    {
        String propsFile = System.getProperty(propFileKey);
        
        if (propsFile != null)
        {
            props.load(new FileInputStream(propsFile));
        }
        else
        {
            getDefaultProperties(propFileKey);
        }
        
        if (props.size() == 0)
        {
            System.err.println("Warning: no mangen properties specified.");
        }
    }
    
    /**
     * Get the default mangen properties from lib\mangen.properties
     */
    public static void getDefaultProperties(String propFileKey)
    {
        // Determine where mangen.jar is located by looking at the system class path.
        String jarLoc = null;
        String classpath = System.getProperty("java.class.path");
        int index = classpath.toLowerCase().indexOf("mangen.jar");
        int start = classpath.lastIndexOf(File.pathSeparator, index) + 1;
        if (index > start)
        {
            jarLoc = classpath.substring(start, index);
            if (jarLoc.length() == 0)
            {
                jarLoc = ".";
            }
        }

        // see if we can load from a default mangen.properties file
        try
        {
            props.load(new FileInputStream(jarLoc + propFileKey));
        }
        catch (Exception e)
        {
            // ok to ignore, we'll report error in caller
        }
    }
    
    /**
     * Provides a wrapper into either System properties, if the specified
     * property is present there, or our mangen.properties if not. As with the
     * standartd getProperty call the default value is returned if the key is not
     * present in either of these property sets. Variable substitution using 
     * ${var} style markers is also supported.
     */
    public static String getProperty(String key, String def)
    {
        String retval = null;
        
        retval = System.getProperty(key);
        if (retval == null)
        {
            retval = props.getProperty(key, def);
        }
        
        if (retval != null)
        {
            retval = substVars(retval, key);
        }
        
        return retval;
    }
    
    /**
     * No default wrapper for getProperty.
     */
    public static String getProperty(String key)
    {
        return getProperty(key, null);
    }


    /**
     * <p>
     * This method performs property variable substitution on the
     * specified string value. If the specified string contains the
     * syntax <code>${&lt;system-prop-name&gt;}</code>, then the corresponding
     * system property value is substituted for the marker.
     * </p>
     * @param val The string on which to perform system property substitution.
     * @param currentKey The current key name, used to detect recursion
     * @return The value of the specified string after system property substitution.
     * @throws IllegalArgumentException If there was a syntax error in the
     *         system property variable marker syntax or recursion will occur.
    **/
    public static String substVars(String val, String currentKey)
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
                    
                    if (key.equals(currentKey))
                    {
                        throw new IllegalArgumentException("recursive property substitution in: " + currentKey);
                    }
                    // Try system properties.
                    String replacement = getProperty(key, null);
                    if (replacement != null)
                    {
                        sbuf.append(replacement);
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }
    
    //////////////////////////////////////////////////
    // INSTANCE VARIABLES
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // CONSTRUCTORS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // ACCESSOR METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PUBLIC INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // INTERFACE METHODS
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // PROTECTED INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // PRIVATE INSTANCE METHODS
    //////////////////////////////////////////////////
    
    //////////////////////////////////////////////////
    // STATIC INNER CLASSES
    //////////////////////////////////////////////////

    //////////////////////////////////////////////////
    // NON-STATIC INNER CLASSES
    //////////////////////////////////////////////////
    
}
