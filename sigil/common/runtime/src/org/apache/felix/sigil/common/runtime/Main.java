package org.apache.felix.sigil.common.runtime;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.commons.cli.PosixParser;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;


public class Main
{
    private static final String COMMAND_LINE_SYNTAX = "java -jar org.apache.felix.sigil.common.runtime";

    private static Framework framework;
    private static final Options options;

    static
    {
        options = new Options();
        options.addOption( "?", "help", false, "Print help for the Sigil launcher" );
        options.addOption( "p", "port", true, "Port to launch server on (0 implies auto allocate) [default 0]" );
        options.addOption( "a", "address", true, "Address to bind server to [default all]");
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
                Map<String, String> config = buildConfig( cl );

                framework = factory.newFramework( config );
                framework.init();

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
            "META-INF/services/org.osgi.framework.launch.FrameworkFactory");
        if (url != null)
        {
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            try
            {
                for (String s = br.readLine(); s != null; s = br.readLine())
                {
                    s = s.trim();
                    // Try to load first non-empty, non-commented line.
                    if ((s.length() > 0) && (s.charAt(0) != '#'))
                    {
                        return (FrameworkFactory) Class.forName(s).newInstance();
                    }
                }
            }
            finally
            {
                if (br != null) br.close();
            }
        }

        throw new Exception("Could not find framework factory.");
    }
    
    private static Map<String, String> buildConfig( CommandLine cl )
    {
        HashMap<String, String> config = new HashMap<String, String>();
        return config;
    }


    private static Server launch( CommandLine line ) throws IOException
    {
        Server server = new Server( framework );
        String v = line.getOptionValue('a');
        InetAddress addr = v == null ? null : InetAddress.getByName( v );
        int port = Integer.parseInt( line.getOptionValue( 'p', "0" ) );
        server.start( addr, port );
        return server;
    }


    private static void printHelp()
    {
        HelpFormatter f = new HelpFormatter();
        f.printHelp( COMMAND_LINE_SYNTAX, options );
    }
}
