package org.apache.felix.sigil.common.runtime;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.ServiceLoader;

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
    
    static {
        options = new Options();
        options.addOption( "?", "help", false, "Print help for the Sigil launcher" );
    }

    public static void main( String[] args )
    {
        try {
            Parser parser = new PosixParser();
            CommandLine cl = parser.parse( options, args );
            
            if ( cl.hasOption( '?' ) ) {
                printHelp();
            }
            else {
                ServiceLoader<FrameworkFactory> loader = ServiceLoader.load( FrameworkFactory.class );
                FrameworkFactory factory = loader.iterator().next();
                
                Map<String, String> config = buildConfig( cl );
                
                framework = factory.newFramework( config );
                framework.init();
                
                launch( args );
                
                framework.waitForStop( 0 );
            }
        }
        catch (NoSuchElementException e) {
            System.err.println( "No " + FrameworkFactory.class.getName() + " found on classpath" );
            System.exit( 1 );
        }
        catch ( BundleException e )
        {
            e.printStackTrace();
        }
        catch ( InterruptedException e )
        {
            System.err.println( "Interrupted prior to framework stop" );
        }
        catch ( ParseException e )
        {
            printHelp();
        }
    }

    private static Map<String, String> buildConfig( CommandLine cl )
    {
        HashMap<String, String> config = new HashMap<String, String>();
        return config;
    }

    private static void launch( String[] args ) throws ParseException
    {
    }
    
    private static void printHelp()
    {
        HelpFormatter f = new HelpFormatter();
        f.printHelp( COMMAND_LINE_SYNTAX, options );
    }
}
