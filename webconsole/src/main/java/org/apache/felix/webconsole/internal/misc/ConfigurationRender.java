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


import java.io.*;
import java.net.URL;
import java.text.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.*;
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
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
            printConfigurationStatus( pw, ConfigurationPrinter.MODE_TXT );
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

            final ConfigurationWriter pw = new ZipConfigurationWriter( zip );
            printConfigurationStatus( pw, ConfigurationPrinter.MODE_ZIP );
            pw.flush();

            addAttachments( pw, ConfigurationPrinter.MODE_ZIP );
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

        printConfigurationStatus( pw, ConfigurationPrinter.MODE_WEB );

        pw.println( "</ul>" );
        pw.println( "</div>" );

        pw.flush();
    }


    private void printConfigurationStatus( ConfigurationWriter pw, final String mode )
    {
        this.printSystemProperties( pw );
        this.printThreads( pw );

        for ( Iterator cpi = getConfigurationPrinters().iterator(); cpi.hasNext(); )
        {
            final PrinterDesc desc = (PrinterDesc) cpi.next();
            if ( desc.match(mode) )
            {
                printConfigurationPrinter( pw, desc.printer, mode );
            }
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
            ServiceReference[] refs = cfgPrinterTracker.getServiceReferences();
            if ( refs != null )
            {
                for ( int i = 0; i < refs.length; i++ )
                {
                    ConfigurationPrinter cfgPrinter =  ( ConfigurationPrinter ) cfgPrinterTracker.getService(refs[i]);
                    if ( cfgPrinter != null )
                    {
                        cp.put( cfgPrinter.getTitle(), new PrinterDesc(cfgPrinter, refs[i].getProperty(ConfigurationPrinter.PROPERTY_MODES)) );
                    }
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


    private void printConfigurationPrinter( final ConfigurationWriter pw,
                                            final ConfigurationPrinter cp,
                                            final String mode )
    {
        pw.title(  cp.getTitle() );
        if ( cp instanceof ModeAwareConfigurationPrinter )
        {
            ((ModeAwareConfigurationPrinter)cp).printConfiguration( pw , mode);
        }
        else
        {
            cp.printConfiguration( pw );
        }
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
            pw.print( " = " );
        }

        pw.print( asString( value ) );

        pw.println();
    }


    private static String asString( final Object value )
    {
        if ( value == null )
        {
            return "n/a";
        }
        else if ( value.getClass().isArray() )
        {
            StringBuffer dest = new StringBuffer();
            Object[] values = ( Object[] ) value;
            for ( int j = 0; j < values.length; j++ )
            {
                if ( j > 0 )
                    dest.append( ", " );
                dest.append( values[j] );
            }
            return dest.toString();
        }
        else
        {
            return value.toString();
        }
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

        public void handleAttachments(final String title, final URL[] urls)
        throws IOException
        {
            throw new UnsupportedOperationException("handleAttachments not supported by this configuration writer: " + this);
        }

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
                // TODO: replace with WebConsoleUtil.escapeHtml()
                // this "convoluted" code replaces "<" by "&lt;"
                final StringTokenizer tokener = new StringTokenizer( string, "<", true );
                while ( tokener.hasMoreElements() )
                {
                    final String token = tokener.nextToken();
                    if ( "<".equals( token ) )
                    {
                        super.write( "&lt;" );
                    }
                    else
                    {
                        super.write( token );
                    }
                }
            }
            else
            {
                // no filtering needed write as is
                super.write( string, 0, string.length() );
            }
        }
    }

    private void addAttachments( final ConfigurationWriter cf, final String mode )
    throws IOException
    {
        for ( Iterator cpi = getConfigurationPrinters().iterator(); cpi.hasNext(); )
        {
            // check if printer supports zip mode
            final PrinterDesc desc = (PrinterDesc) cpi.next();
            if ( desc.match(mode) )
            {
                // check if printer implements binary configuration printer
                if ( desc.printer instanceof AttachmentProvider )
                {
                    final URL[] attachments = ((AttachmentProvider)desc.printer).getAttachments(mode);
                    if ( attachments != null )
                    {
                        cf.handleAttachments(desc.printer.getTitle(), attachments);
                    }
                }
            }
        }

    }

    private static final class PrinterDesc
    {
        private final String[] modes;
        public final ConfigurationPrinter printer;

        private static final List CUSTOM_MODES = new ArrayList();
        static
        {
            CUSTOM_MODES.add(ConfigurationPrinter.MODE_TXT);
            CUSTOM_MODES.add(ConfigurationPrinter.MODE_WEB);
            CUSTOM_MODES.add(ConfigurationPrinter.MODE_ZIP);
        }

        public PrinterDesc(final ConfigurationPrinter printer, final Object modes)
        {
            this.printer = printer;
            if ( modes == null || !(modes instanceof String || modes instanceof String[]) )
            {
                this.modes = null;
            }
            else
            {
                if ( modes instanceof String )
                {
                    if ( CUSTOM_MODES.contains(modes) )
                    {
                        this.modes = new String[] {modes.toString()};
                    }
                    else
                    {
                        this.modes = null;
                    }
                }
                else
                {
                    final String[] values = (String[])modes;
                    boolean valid = values.length > 0;
                    for(int i=0; i<values.length; i++)
                    {
                        if ( !CUSTOM_MODES.contains(values[i]) )
                        {
                            valid = false;
                            break;
                        }
                    }
                    if ( valid)
                    {
                        this.modes = values;
                    }
                    else
                    {
                        this.modes = null;
                    }
                }
            }
        }

        public boolean match(final String mode)
        {
            if ( this.modes == null)
            {
                return true;
            }
            for(int i=0; i<this.modes.length; i++)
            {
                if ( this.modes[i].equals(mode) )
                {
                    return true;
                }
            }
            return false;
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

        private OutputStream startFile( String title, String name)
        {
            final String path = MessageFormat.format( "{0,number,000}-{1}/{2}", new Object[]
                 { new Integer( counter ), title, name } );
            ZipEntry entry = new ZipEntry( path );
            try
            {
                zip.putNextEntry( entry );
            }
            catch ( IOException ioe )
            {
                // should handle
            }
            return zip;
        }

        public void handleAttachments( final String title, final URL[] attachments)
        throws IOException
        {
            for(int i = 0; i < attachments.length; i++)
            {
                final URL current = attachments[i];
                final String path = current.getPath();
                final String name;
                if ( path == null || path.length() == 0 )
                {
                    // sanity code, we should have a path, but if not let's just create
                    // some random name
                    name = UUID.randomUUID().toString();
                }
                else
                {
                    final int pos = path.lastIndexOf('/');
                    name = (pos == -1 ? path : path.substring(pos + 1));
                }
                final OutputStream os = this.startFile(title, name);
                final InputStream is = current.openStream();
                try
                {
                    IOUtils.copy(is, os);
                }
                finally
                {
                    IOUtils.closeQuietly(is);
                }
                this.end();
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
