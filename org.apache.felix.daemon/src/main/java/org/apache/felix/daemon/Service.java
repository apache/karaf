/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.daemon;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.directory.daemon.DaemonApplication;
import org.apache.directory.daemon.InstallationLayout;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.cache.BundleCache;
import org.apache.felix.framework.util.StringMap;


/**
 * NOTE: Does not set system properties which are done via a daemon ui, some
 * init script, or a main() application entry point.
 */
public class Service implements DaemonApplication
{
    /**
     * The system property name used to specify an URL to the configuration
     * property file to be used for the created the framework instance.
     */
    public static final String CONFIG_PROPERTIES_PROP = "felix.config.properties";
    /** The default name used for the configuration properties file. */
    public static final String CONFIG_PROPERTIES_FILE_VALUE = "config.properties";
    /** the default profile if no profile name or path is specified */
    public static final String DEFAULT_PRODUCTION_PROFILE = "production";

    /** the instance of Felix managed by this daemon/service */
    private Felix instance;
    /** the configuration properties loaded from the configuration file */
    private Properties configationProperties;
    /** the felix installation layout */
    private FelixLayout layout;

    
    public Felix getFelixInstance()
    {
        return instance;
    }
    

    public void init( InstallationLayout suppliedLayout, String[] args ) throws Exception
    {
        if ( !( suppliedLayout instanceof FelixLayout ) )
        {
            this.layout = new FelixLayout( suppliedLayout );
        }
        else
        {
            this.layout = ( FelixLayout ) suppliedLayout;
        }
        
        configationProperties = readConfigProperties();
        instance = new Felix(new StringMap(configationProperties, false ), null );
    }


    public void start()
    {
        // See if the profile name property was specified.
        String profileName = configationProperties.getProperty( BundleCache.CACHE_PROFILE_PROP );

        // See if the profile directory property was specified.
        String profileDirName = configationProperties.getProperty( BundleCache.CACHE_PROFILE_DIR_PROP );

        // If no profile or profile directory is specified in the properties, then set the 
        // name to the default production mode profile name since this is not started from main()
        if ( ( profileName == null ) && ( profileDirName == null ) )
        {
            configationProperties.setProperty( BundleCache.CACHE_PROFILE_PROP, DEFAULT_PRODUCTION_PROFILE );
        }

        // start up the instance using the loaded and possibly altered configuration 
        try
        {
            instance.start();
        }
        catch (Exception ex) 
        {
            // TODO: find out what to do
        }
    }


    public void stop( String[] arg0 ) throws Exception
    {
        instance.stop();
    }


    public void destroy()
    {
    }
    
    
    /**
     * Exposes configuration properties for potential alteration between load 
     * time at init() and start() by the managing framework or by the main().
     * 
     * @return the configuration properties loaded by default from conf/config.properties
     */
    public Properties getConfigurationProperties()
    {
        return configationProperties;
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
     */
    private Properties readConfigProperties()
    {
        // The config properties file is either present in a default 
        // location using the layout, or is specified by a system property
        // Try to load it from one of these places.

        // See if the property URL was specified as a property.
        URL propURL = null;
        String custom = System.getProperty( CONFIG_PROPERTIES_PROP );
        if ( custom != null )
        {
            try
            {
                propURL = new URL( custom );
            }
            catch ( MalformedURLException ex )
            {
                System.err.print( "Main: " + ex );
                return null;
            }
        }
        else
        {
            try
            {
                propURL = layout.getConfigurationFile().toURL();
            }
            catch ( MalformedURLException ex )
            {
                System.err.print( "Main: " + ex );
                return null;
            }
        }

        // Read the properties file.
        Properties props = new Properties();
        InputStream is = null;
        try
        {
            is = propURL.openConnection().getInputStream();
            props.load( is );
            is.close();
        }
        catch ( FileNotFoundException ex )
        {
            // Ignore file not found.
        }
        catch ( Exception ex )
        {
            System.err.println( "Error loading config properties from " + propURL );
            System.err.println( "Main: " + ex );
            
            try
            {
                if ( is != null ) 
                {
                    is.close();
                }
            }
            catch ( IOException ex2 )
            {
                // Nothing we can do.
            }
            
            return null;
        }

        // Perform variable substitution for system properties.
        for ( Enumeration e = props.propertyNames(); e.hasMoreElements(); /* EMPTY */ )
        {
            String name = ( String ) e.nextElement();
            props.setProperty( name, substVars( ( String ) props.getProperty( name ) ) );
        }

        return props;
    }

    
    private static final String DELIM_START = "${";
    private static final char DELIM_STOP = '}';
    private static final int DELIM_START_LEN = 2;
    private static final int DELIM_STOP_LEN = 1;


    /**
     * <p>
     * This method performs system property variable substitution on the
     * specified string value. If the specified string contains the syntax
     * <tt>${&lt;system-prop-name&gt;}</tt>, then the corresponding system
     * property value is substituted for the marker.
     * </p>
     * 
     * @param val
     *            The string on which to perform system property substitution.
     * @return The value of the specified string after system property
     *         substitution.
     * @throws IllegalArgumentException
     *             If there was a syntax error in the system property variable
     *             marker syntax.
     */
    public static String substVars( String val ) throws IllegalArgumentException
    {
        StringBuffer sbuf = new StringBuffer();

        if ( val == null )
        {
            return val;
        }

        int i = 0;
        int j, k;

        while ( true )
        {
            j = val.indexOf( DELIM_START, i );
            if ( j == -1 )
            {
                if ( i == 0 )
                {
                    return val;
                }
                else
                {
                    sbuf.append( val.substring( i, val.length() ) );
                    return sbuf.toString();
                }
            }
            else
            {
                sbuf.append( val.substring( i, j ) );
                k = val.indexOf( DELIM_STOP, j );
                if ( k == -1 )
                {
                    throw new IllegalArgumentException( '"' + val
                        + "\" has no closing brace. Opening brace at position " + j + '.' );
                }
                else
                {
                    j += DELIM_START_LEN;
                    String key = val.substring( j, k );
                    // Try system properties.
                    String replacement = System.getProperty( key, null );
                    if ( replacement != null )
                    {
                        sbuf.append( replacement );
                    }
                    i = k + DELIM_STOP_LEN;
                }
            }
        }
    }
}
