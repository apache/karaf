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
package org.apache.felix.webconsole.internal.misc;


import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;


public class ConfigurationRender extends BaseWebConsolePlugin
{

    public static final String LABEL = "config";

    public static final String TITLE = "Configuration Status";

    private static final String[] CSS_REFS =
        { "res/ui/configurationrender.css" };

    /**
     * Formatter pattern to generate a relative path for the generation
     * of the plain text or zip file representation of the status. The file
     * name consists of a base name and the current time of status generation.
     */
    private static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat( "'" + LABEL
        + "/configuration-status-'yyyyMMdd'-'HHmmZ" );

    /**
     * Formatter pattern to render the current time of status generation.
     */
    private static final DateFormat DISPLAY_DATE_FORMAT = SimpleDateFormat.getDateTimeInstance( SimpleDateFormat.LONG,
        SimpleDateFormat.LONG, Locale.US );

    private ServiceTracker cfgPrinterTracker;

    private int cfgPrinterTrackerCount;

    private SortedMap configurationPrinters = new TreeMap();


    public String getTitle()
    {
        return TITLE;
    }


    public String getLabel()
    {
        return LABEL;
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if ( request.getPathInfo().endsWith( ".txt" ) )
        {
            response.setContentType( "text/plain; charset=utf-8" );
            ConfigurationWriter pw = new PlainTextConfigurationWriter( response.getWriter() );
            printConfigurationStatus( pw );
            pw.flush();
        }
        else if ( request.getPathInfo().endsWith( ".zip" ) )
        {
            String type = getServletContext().getMimeType( request.getPathInfo() );
            if ( type == null )
            {
                type = "application/x-zip";
            }
            response.setContentType( type );

            ZipOutputStream zip = new ZipOutputStream( response.getOutputStream() );
            zip.setLevel( 9 );
            zip.setMethod( ZipOutputStream.DEFLATED );

            ConfigurationWriter pw = new ZipConfigurationWriter( zip );
            printConfigurationStatus( pw );
            pw.flush();

            zip.finish();
        }
        else
        {
            super.doGet( request, response );
        }
    }


    protected String[] getCssReferences()
    {
        return CSS_REFS;
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        ConfigurationWriter pw = new HtmlConfigurationWriter( response.getWriter() );

        String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );
        Util.script( pw, appRoot, "tw-1.1.js" );

        Util.startScript( pw );
        pw.println( "    $(document).ready(function(){" );
        //                   set up the tabs (still hidden)
        pw.println( "        $('#cfgprttabs').tabworld({speed:0});" );
        //                   show the finished tabs
        pw.println( "        $('#divcfgprttabs').removeClass('divcfgprttabshidden');" );
        //                   hide the "please wait" message
        pw.println( "        $('#divcfgprttabswait').addClass('divcfgprttabshidden');" );
        pw.println( "    });" );
        Util.endScript( pw );

        final Date currentTime = new Date();
        synchronized ( DISPLAY_DATE_FORMAT )
        {
            pw.println( "<p>Date: " + DISPLAY_DATE_FORMAT.format( currentTime ) + "</p>" );
        }

        synchronized ( FILE_NAME_FORMAT )
        {
            String fileName = FILE_NAME_FORMAT.format( currentTime );
            pw.println( "<p>Download as <a href='" + fileName + ".txt'>[Single File]</a> or as <a href='" + fileName
                + ".zip'>[ZIP]</a></p>" );
        }

        // display some information while the data is loading
        pw.println( "<div id='divcfgprttabswait'>Loading status information. Please wait....</div>" );

        // load the data (hidden to begin with)
        pw.println( "<div id='divcfgprttabs' class='divcfgprttabshidden'>" );
        pw.println( "<ul id='cfgprttabs'>" );

        printConfigurationStatus( pw );

        pw.println( "</ul>" );
        pw.println( "</div>" );

        pw.flush();
    }


    private void printConfigurationStatus( ConfigurationWriter pw )
    {
        this.printSystemProperties( pw );
        this.printServices( pw );
        this.printThreads( pw );

        for ( Iterator cpi = getConfigurationPrinters().iterator(); cpi.hasNext(); )
        {
            printConfigurationPrinter( pw, ( ConfigurationPrinter ) cpi.next() );
        }
    }


    private Collection getConfigurationPrinters()
    {
        if ( cfgPrinterTracker == null )
        {
            cfgPrinterTracker = new ServiceTracker( getBundleContext(), ConfigurationPrinter.SERVICE, null );
            cfgPrinterTracker.open();
            cfgPrinterTrackerCount = -1;
        }

        if ( cfgPrinterTrackerCount != cfgPrinterTracker.getTrackingCount() )
        {
            SortedMap cp = new TreeMap();
            Object[] services = cfgPrinterTracker.getServices();
            if ( services != null )
            {
                for ( int i = 0; i < services.length; i++ )
                {
                    Object srv = services[i];
                    ConfigurationPrinter cfgPrinter = ( ConfigurationPrinter ) srv;
                    cp.put( cfgPrinter.getTitle(), cfgPrinter );
                }
            }
            configurationPrinters = cp;
            cfgPrinterTrackerCount = cfgPrinterTracker.getTrackingCount();
        }

        return configurationPrinters.values();
    }


    private void printSystemProperties( ConfigurationWriter pw )
    {
        pw.title( "System properties" );

        Properties props = System.getProperties();
        SortedSet keys = new TreeSet( props.keySet() );
        for ( Iterator ki = keys.iterator(); ki.hasNext(); )
        {
            Object key = ki.next();
            infoLine( pw, null, ( String ) key, props.get( key ) );
        }

        pw.end();
    }


    // This is Sling stuff, we comment it out for now
    //    private void printRawFrameworkProperties(PrintWriter pw) {
    //        pw.println("*** Raw Framework properties:");
    //
    //        File file = new File(getBundleContext().getProperty("sling.home"),
    //            "sling.properties");
    //        if (file.exists()) {
    //            Properties props = new Properties();
    //            InputStream ins = null;
    //            try {
    //                ins = new FileInputStream(file);
    //                props.load(ins);
    //            } catch (IOException ioe) {
    //                // handle or ignore
    //            } finally {
    //                IOUtils.closeQuietly(ins);
    //            }
    //
    //            SortedSet keys = new TreeSet(props.keySet());
    //            for (Iterator ki = keys.iterator(); ki.hasNext();) {
    //                Object key = ki.next();
    //                infoLine(pw, null, (String) key, props.get(key));
    //            }
    //
    //        } else {
    //            pw.println("  No Framework properties in " + file);
    //        }
    //
    //        pw.println();
    //    }


    private void printServices( ConfigurationWriter pw )
    {
        pw.title(  "Services" );

        // get the list of services sorted by service ID (ascending)
        SortedMap srMap = new TreeMap();
        try
        {
            ServiceReference[] srs = getBundleContext().getAllServiceReferences( null, null );
            for ( int i = 0; i < srs.length; i++ )
            {
                srMap.put( srs[i].getProperty( Constants.SERVICE_ID ), srs[i] );
            }
        }
        catch ( InvalidSyntaxException ise )
        {
            // should handle, for now just print nothing, actually this is not
            // expected
        }

        for ( Iterator si = srMap.values().iterator(); si.hasNext(); )
        {
            ServiceReference sr = ( ServiceReference ) si.next();

            infoLine( pw, null, String.valueOf( sr.getProperty( Constants.SERVICE_ID ) ), sr
                .getProperty( Constants.OBJECTCLASS ) );
            infoLine( pw, "  ", "Bundle", this.getBundleString( sr.getBundle() ) );

            Bundle[] users = sr.getUsingBundles();
            if ( users != null && users.length > 0 )
            {
                for ( int i = 0; i < users.length; i++ )
                {
                    infoLine( pw, "  ", "Using Bundle", this.getBundleString( users[i] ) );
                }
            }

            String[] keys = sr.getPropertyKeys();
            Arrays.sort( keys );
            for ( int i = 0; i < keys.length; i++ )
            {
                if ( !Constants.SERVICE_ID.equals( keys[i] ) && !Constants.OBJECTCLASS.equals( keys[i] ) )
                {
                    infoLine( pw, "  ", keys[i], sr.getProperty( keys[i] ) );
                }
            }

            pw.println();
        }

        pw.end();
    }


    private void printConfigurationPrinter( ConfigurationWriter pw, ConfigurationPrinter cp )
    {
        pw.title(  cp.getTitle() );
        cp.printConfiguration( pw );
        pw.end();
    }


    public static void infoLine( PrintWriter pw, String indent, String label, Object value )
    {
        if ( indent != null )
        {
            pw.print( indent );
        }

        if ( label != null )
        {
            pw.print( label );
            pw.print( '=' );
        }

        printObject( pw, value );

        pw.println();
    }


    private static void printObject( PrintWriter pw, Object value )
    {
        if ( value == null )
        {
            pw.print( "null" );
        }
        else if ( value.getClass().isArray() )
        {
            printArray( pw, ( Object[] ) value );
        }
        else
        {
            pw.print( value );
        }
    }


    private static void printArray( PrintWriter pw, Object[] values )
    {
        pw.print( '[' );
        if ( values != null && values.length > 0 )
        {
            for ( int i = 0; i < values.length; i++ )
            {
                if ( i > 0 )
                {
                    pw.print( ", " );
                }
                printObject( pw, values[i] );
            }
        }
        pw.print( ']' );
    }


    private String getBundleString( Bundle bundle )
    {
        StringBuffer buf = new StringBuffer();

        if ( bundle.getSymbolicName() != null )
        {
            buf.append( bundle.getSymbolicName() );
        }
        else if ( bundle.getLocation() != null )
        {
            buf.append( bundle.getLocation() );
        }
        else
        {
            buf.append( bundle.getBundleId() );
        }

        Dictionary headers = bundle.getHeaders();
        if ( headers.get( Constants.BUNDLE_VERSION ) != null )
        {
            buf.append( " (" ).append( headers.get( Constants.BUNDLE_VERSION ) ).append( ')' );
        }

        if ( headers.get( Constants.BUNDLE_NAME ) != null )
        {
            buf.append( " \"" ).append( headers.get( Constants.BUNDLE_NAME ) ).append( '"' );
        }

        return buf.toString();
    }


    private void printThreads( ConfigurationWriter pw )
    {
        // first get the root thread group
        ThreadGroup rootGroup = Thread.currentThread().getThreadGroup();
        while ( rootGroup.getParent() != null )
        {
            rootGroup = rootGroup.getParent();
        }

        pw.title(  "Threads" );

        printThreadGroup( pw, rootGroup );

        int numGroups = rootGroup.activeGroupCount();
        ThreadGroup[] groups = new ThreadGroup[2 * numGroups];
        rootGroup.enumerate( groups );
        for ( int i = 0; i < groups.length; i++ )
        {
            printThreadGroup( pw, groups[i] );
        }

        pw.end();
    }


    private void printThreadGroup( PrintWriter pw, ThreadGroup group )
    {
        if ( group != null )
        {
            StringBuffer info = new StringBuffer();
            info.append("ThreadGroup ").append(group.getName());
            info.append( " [" );
            info.append( "maxprio=" ).append( group.getMaxPriority() );

            info.append( ", parent=" );
            if ( group.getParent() != null )
            {
                info.append( group.getParent().getName() );
            }
            else
            {
                info.append( '-' );
            }

            info.append( ", isDaemon=" ).append( group.isDaemon() );
            info.append( ", isDestroyed=" ).append( group.isDestroyed() );
            info.append( ']' );

            infoLine( pw, null, null, info.toString() );

            int numThreads = group.activeCount();
            Thread[] threads = new Thread[numThreads * 2];
            group.enumerate( threads, false );
            for ( int i = 0; i < threads.length; i++ )
            {
                printThread( pw, threads[i] );
            }

            pw.println();
        }
    }


    private void printThread( PrintWriter pw, Thread thread )
    {
        if ( thread != null )
        {
            StringBuffer info = new StringBuffer();
            info.append("Thread ").append( thread.getName() );
            info.append( " [" );
            info.append( "priority=" ).append( thread.getPriority() );
            info.append( ", alive=" ).append( thread.isAlive() );
            info.append( ", daemon=" ).append( thread.isDaemon() );
            info.append( ", interrupted=" ).append( thread.isInterrupted() );
            info.append( ", loader=" ).append( thread.getContextClassLoader() );
            info.append( ']' );

            infoLine( pw, "  ", null, info.toString() );
        }
    }

    private abstract static class ConfigurationWriter extends PrintWriter
    {

        ConfigurationWriter( Writer delegatee )
        {
            super( delegatee );
        }


        abstract void title( String title );


        abstract void end();

    }

    private static class HtmlConfigurationWriter extends ConfigurationWriter
    {

        // whether or not to filter "<" signs in the output
        private boolean doFilter;


        HtmlConfigurationWriter( Writer delegatee )
        {
            super( delegatee );
        }


        public void title( String title )
        {
            println( "<li>" );
            println( title );
            println( "<q>" );
            doFilter = true;
        }


        public void end()
        {
            doFilter = false;
            println( "</q>" );
            println( "</li>" );
        }


        // IE has an issue with white-space:pre in our case so, we write
        // <br/> instead of [CR]LF to get the line break. This also works
        // in other browsers.
        public void println()
        {
            if ( doFilter )
            {
                super.write( "<br/>", 0, 5 );
            }
            else
            {
                super.println();
            }
        }


        // write the character unmodified unless filtering is enabled and
        // the character is a "<" in which case &lt; is written
        public void write( final int character )
        {
            if ( doFilter && character == '<' )
            {
                super.write( "&lt;" );
            }
            else
            {
                super.write( character );
            }
        }


        // write the characters unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write( final char[] chars, final int off, final int len )
        {
            if ( doFilter )
            {
                writeFiltered( new String( chars, off, len ) );
            }
            else
            {
                super.write( chars, off, len );
            }
        }


        // write the string unmodified unless filtering is enabled in
        // which case the writeFiltered(String) method is called for filtering
        public void write( final String string, final int off, final int len )
        {
            if ( doFilter )
            {
                writeFiltered( string.substring( off, len ) );
            }
            else
            {
                super.write( string, off, len );
            }
        }


        // helper method filter the string for "<" before writing
        private void writeFiltered( final String string )
        {
            if ( string.indexOf( '<' ) >= 0 )
            {
                super.write( string.replaceAll( "<", "&lt;" ) );
            }
            else
            {
                // no filtering needed write as is
                super.write( string, 0, string.length() );
            }
        }
    }

    private static class PlainTextConfigurationWriter extends ConfigurationWriter
    {

        PlainTextConfigurationWriter( Writer delegatee )
        {
            super( delegatee );
        }


        public void title( String title )
        {
            print( "*** " );
            print( title );
            println( ":" );
        }


        public void end()
        {
            println();
        }
    }

    private static class ZipConfigurationWriter extends ConfigurationWriter
    {
        private final ZipOutputStream zip;

        private int counter;


        ZipConfigurationWriter( ZipOutputStream zip )
        {
            super( new OutputStreamWriter( zip ) );
            this.zip = zip;
        }


        public void title( String title )
        {
            String name = MessageFormat.format( "{0,number,000}-{1}.txt", new Object[]
                { new Integer( counter ), title } );

            counter++;

            ZipEntry entry = new ZipEntry( name );
            try
            {
                zip.putNextEntry( entry );
            }
            catch ( IOException ioe )
            {
                // should handle
            }
        }


        public void end()
        {
            flush();

            try
            {
                zip.closeEntry();
            }
            catch ( IOException ioe )
            {
                // should handle
            }
        }
    }
}
