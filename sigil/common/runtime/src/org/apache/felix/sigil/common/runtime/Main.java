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

package org.apache.felix.sigil.common.runtime;


import static org.apache.felix.sigil.common.runtime.Runtime.ADDRESS_PROPERTY;
import static org.apache.felix.sigil.common.runtime.Runtime.PORT_PROPERTY;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;

import org.apache.felix.sigil.common.runtime.cli.CommandLine;
import org.apache.felix.sigil.common.runtime.cli.HelpFormatter;
import org.apache.felix.sigil.common.runtime.cli.Options;
import org.apache.felix.sigil.common.runtime.cli.ParseException;
import org.apache.felix.sigil.common.runtime.cli.Parser;
import org.apache.felix.sigil.common.runtime.cli.PosixParser;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;


public class Main
{
    private static final String COMMAND_LINE_SYNTAX = "java -jar org.apache.felix.sigil.common.runtime";

    private static Framework framework;
    private static final Options options;
    
    private static boolean verbose = false;

    static
    {
        options = new Options();
        options.addOption( "?", "help", false, "Print help for the Sigil launcher" );
        options.addOption( "p", "port", true, "Port to launch server on (0 implies auto allocate) [default 0]" );
        options.addOption( "a", "address", true, "Address to bind server to [default all]" );
        options.addOption( "c", "clean", false, "Clean bundle cache directory on init" );
        options.addOption( "s", "startLevel", true, "Start level for framework" );
        options.addOption( "v", "verbose", false, "Verbose output" );
    }


    public static void main( String[] args ) throws Exception
    {
        FrameworkFactory factory = getFrameworkFactory();

        try
        {
            Parser parser = new PosixParser();
            CommandLine cl = parser.parse( options, args );

            if ( cl.hasOption( '?' ) )
            {
                printHelp();
            }
            else
            {
                verbose = cl.hasOption('v');
                
                Map<String, String> config = buildConfig( cl );

                framework = factory.newFramework( config );
                framework.init();
                framework.start();

                Server server = launch( cl );

                framework.waitForStop( 0 );

                if ( server != null )
                {
                    server.stop();
                }
            }
        }
        catch ( NoSuchElementException e )
        {
            System.err.println( "No " + FrameworkFactory.class.getName() + " found on classpath" );
            System.exit( 1 );
        }
        catch ( InterruptedException e )
        {
            System.err.println( "Interrupted prior to framework stop" );
            System.exit( 1 );
        }
        catch ( ParseException e )
        {
            printHelp();
            System.exit( 1 );
        }
        catch ( BundleException e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            System.exit( 1 );
        }
    }


    /**
     * Simple method to parse META-INF/services file for framework factory.
     * Currently, it assumes the first non-commented line is the class name
     * of the framework factory implementation.
     * @return The created <tt>FrameworkFactory</tt> instance.
     * @throws Exception if any errors occur.
    **/
    private static FrameworkFactory getFrameworkFactory() throws Exception
    {
        URL url = Main.class.getClassLoader().getResource(
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory" );
        if ( url != null )
        {
            BufferedReader br = new BufferedReader( new InputStreamReader( url.openStream() ) );
            try
            {
                for ( String s = br.readLine(); s != null; s = br.readLine() )
                {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ( ( s.length() > 0 ) && ( s.charAt( 0 ) != '#' ) )
                    {
                        return ( FrameworkFactory ) Class.forName( s ).newInstance();
                    }
                }
            }
            finally
            {
                if ( br != null )
                    br.close();
            }
        }

        throw new Exception( "Could not find framework factory." );
    }


    private static Map<String, String> buildConfig( CommandLine cl )
    {
        HashMap<String, String> config = new HashMap<String, String>();
        if ( cl.hasOption( 'c' ))
            config.put(  "org.osgi.framework.storage.clean", "onFirstInit" );
        
        if ( cl.hasOption( 's' ) )
            config.put( "org.osgi.framework.startlevel.beginning", cl.getOptionValue( 's' ) );
        
        return config;
    }


    private static Server launch( CommandLine line ) throws IOException
    {
        Server server = new Server( framework );
        Properties props = new Properties();
        props.put( ADDRESS_PROPERTY, line.getOptionValue( 'a' ) );
        props.put( PORT_PROPERTY, line.getOptionValue( 'p' ) );
        server.start( props );
        return server;
    }


    private static void printHelp()
    {
        HelpFormatter f = new HelpFormatter();
        f.printHelp( COMMAND_LINE_SYNTAX, options );
    }
    
    public static void log(String msg) {
        if ( verbose )
            System.out.println( msg );
    }
}
