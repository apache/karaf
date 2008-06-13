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
package org.apache.felix.webconsole.internal.misc;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;


/**
 * The <code>LicenseServlet</code> TODO
 */
public class LicenseServlet extends AbstractWebConsolePlugin implements OsgiManagerPlugin
{

    public String getLabel()
    {
        return "licenses";
    }


    public String getTitle()
    {
        return "Licenses";
    }


    protected void renderContent( HttpServletRequest req, HttpServletResponse res ) throws IOException
    {
        PrintWriter pw = res.getWriter();

        String appRoot = req.getContextPath() + req.getServletPath();
        pw.println( "<link href='" + appRoot + "/res/ui/license.css' rel='stylesheet' type='text/css'>" );
        pw.println( "<script src='" + appRoot + "/res/ui/license.js' language='JavaScript'></script>" );

        Bundle[] bundles = getBundleContext().getBundles();
        Util.sort( bundles );

        Util.startScript( pw );
        pw.print( "bundleData = " );
        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();
            for ( int i = 0; i < bundles.length; i++ )
            {
                Bundle bundle = bundles[i];
                jw.key( String.valueOf( bundle.getBundleId() ) );

                jw.object();

                jw.key( "title" );
                jw.value( Util.getName( bundle ) );

                jw.key( "files" );
                jw.object();
                findResource( jw, bundle, new String[]
                    { "README", "DISCLAIMER", "LICENSE", "NOTICE" } );
                jw.endObject();

                jw.endObject();
            }
            jw.endObject();
            pw.println( ";" );
        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }
        Util.endScript( pw );

        pw.println( "<div id='licenseContent'>" );

        pw.println( "<div id='licenseLeft'>" );
        for ( int i = 0; i < bundles.length; i++ )
        {
            Bundle bundle = bundles[i];
            String link = "displayBundle( \"" + bundle.getBundleId() + "\" );";
            pw.println( "<a href='javascript:" + link + "'>" + Util.getName( bundle ) + "</a><br />" );

        }
        pw.println( "</div>" );

        pw.println( "<div id='licenseRight'>" );
        pw.println( "<div id='licenseButtons' class='licenseButtons'>&nbsp;</div>" );
        pw.println( "<br />" );
        pw.println( "<div id='licenseDetails' class='licenseDetails'>&nbsp;</div>" );
        pw.println( "</div>" );

        pw.println( "<div id='licenseClear'>&nbsp;</div>" );

        pw.println( "</div>" ); // licenseContent

        Util.startScript( pw );
        pw.println( "displayBundle( '0' );" );
        Util.endScript( pw );
    }


    private String getName( String path )
    {
        return path.substring( path.lastIndexOf( '/' ) + 1 );
    }


    private void findResource( JSONWriter jw, Bundle bundle, String[] patterns ) throws IOException, JSONException
    {
        jw.key( "Bundle Resources" ); // aka the bundle files
        jw.array();
        for ( int i = 0; i < patterns.length; i++ )
        {
            Enumeration entries = bundle.findEntries( "/", patterns[i] + "*", true );
            if ( entries != null )
            {
                while ( entries.hasMoreElements() )
                {
                    URL url = ( URL ) entries.nextElement();
                    jw.object();
                    jw.key( "url" );
                    jw.value( getName( url.getPath() ) );
                    jw.key( "data" );
                    jw.value( readResource( url ) );
                    jw.endObject();
                }
            }
        }
        jw.endArray();

        Enumeration entries = bundle.findEntries( "/", "*.jar", true );
        if ( entries != null )
        {
            while ( entries.hasMoreElements() )
            {
                URL url = ( URL ) entries.nextElement();

                jw.key( "Embedded " + getName( url.getPath() ) );
                jw.array();

                for ( int i = 0; i < patterns.length; i++ )
                {
                    String pattern = ".*/" + patterns[i] + "[^/]*$";

                    InputStream ins = null;
                    try
                    {
                        ins = url.openStream();
                        ZipInputStream zin = new ZipInputStream( ins );
                        ZipEntry zentry = zin.getNextEntry();
                        while ( zentry != null )
                        {
                            String name = zentry.getName();
                            if ( !name.endsWith( "/" ) && "/".concat( name ).matches( pattern ) )
                            {
                                jw.object();
                                jw.key( "url" );
                                jw.value( getName( name ) );
                                jw.key( "data" );
                                jw.value( readResource( new FilterInputStream( zin )
                                {
                                    public void close()
                                    {
                                        // nothing for now
                                    }
                                } ) );
                                jw.endObject();
                            }

                            zentry = zin.getNextEntry();
                        }
                    }
                    finally
                    {
                        if ( ins != null )
                        {
                            try
                            {
                                ins.close();
                            }
                            catch ( IOException ignore )
                            {
                            }
                        }
                    }
                }

                jw.endArray();
            }
        }
    }


    private String getResource( Bundle bundle, String[] path ) throws IOException
    {
        for ( int i = 0; i < path.length; i++ )
        {
            URL resource = bundle.getResource( path[i] );
            if ( resource != null )
            {
                return readResource( resource );
            }
        }

        return null;
    }


    private String readResource( URL resource ) throws IOException
    {
        return readResource( resource.openStream() );
    }


    private String readResource( InputStream resource ) throws IOException
    {
        Reader r = null;
        StringBuffer buffer = new StringBuffer();
        try
        {
            char[] buf = new char[1024];
            r = new InputStreamReader( resource, "ISO-8859-1" );
            int num;
            while ( ( num = r.read( buf ) ) >= 0 )
            {
                buffer.append( buf, 0, num );
            }
        }
        finally
        {
            if ( r != null )
            {
                try
                {
                    r.close();
                }
                catch ( IOException ignore )
                {
                }
            }
        }
        return buffer.toString();
    }


    protected void activate( ComponentContext context )
    {
        activate( context.getBundleContext() );
    }


    protected void deactivate( ComponentContext context )
    {
        deactivate();
    }

}
