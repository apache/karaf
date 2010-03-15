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


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;


/**
 * LicenseServlet provides the licenses plugin that browses through the bundles,
 * searching for common license files.
 *
 * TODO: add support for 'Bundle-License' manifest header
 */
public final class LicenseServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    // common names (without extension) of the license files.
    static final String LICENSE_FILES[] =  { "README", "DISCLAIMER", "LICENSE", "NOTICE" };

    static final String LABEL = "licenses";
    static final String TITLE = "%license.pluginTitle";
    static final String CSS[] = { "/res/ui/license.css" };

    // templates
    private final String TEMPLATE;

    /**
     * Default constructor
     */
    public LicenseServlet()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/license.html" );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        final PathInfo pathInfo = PathInfo.parse( request.getPathInfo() );
        if ( pathInfo != null )
        {
            if ( !sendResource( pathInfo, response ) )
            {
                response.sendError( HttpServletResponse.SC_NOT_FOUND, "Cannot send data .." );
            }
        }
        else
        {
            super.doGet( request, response );
        }
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse res ) throws IOException
    {
        Bundle[] bundles = getBundleContext().getBundles();
        Util.sort( bundles, request.getLocale() );

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", getBundleData( bundles, request.getLocale() ).toString());

        res.getWriter().print(TEMPLATE);
    }

    private static final JSONArray getBundleData(Bundle[] bundles, Locale locale) throws IOException
    {
        JSONArray ret = new JSONArray();
        try
        {
            for (int i = 0; i < bundles.length; i++)
            {
                Bundle bundle = bundles[i];

                JSONObject files = findResource(bundle, LICENSE_FILES);
                if (files.length() > 0)
                { // has resources
                    JSONObject data = new JSONObject();
                    data.put( "bid", bundle.getBundleId() );
                    data.put( "title", Util.getName( bundle, locale ) );
                    data.put( "files", files );
                    ret.put( data );
                }
            }
        }
        catch (JSONException je)
        {
            throw new IOException(je.toString());
        }
        return ret;
    }


    private static final String getName( String path )
    {
        return path.substring( path.lastIndexOf( '/' ) + 1 );
    }


    private static final JSONObject findResource( Bundle bundle, String[] patterns ) throws IOException, JSONException
    {
        JSONObject ret = new JSONObject();

        for ( int i = 0; i < patterns.length; i++ )
        {
            Enumeration entries = bundle.findEntries( "/", patterns[i] + "*", true );
            if ( entries != null )
            {
                while ( entries.hasMoreElements() )
                {
                    URL url = ( URL ) entries.nextElement();
                    JSONObject entry = new JSONObject();
                    entry.put( "path", url.getPath() );
                    entry.put( "url", getName( url.getPath() ) );
                    ret.append( "__res__", entry );
                }
            }
        }

        Enumeration entries = bundle.findEntries( "/", "*.jar", true );
        if ( entries != null )
        {
            while ( entries.hasMoreElements() )
            {
                URL url = ( URL ) entries.nextElement();
                final String resName = getName( url.getPath() );

                InputStream ins = null;
                try
                {
                    ins = url.openStream();
                    ZipInputStream zin = new ZipInputStream( ins );
                    for ( ZipEntry zentry = zin.getNextEntry(); zentry != null; zentry = zin.getNextEntry() )
                    {
                        String name = zentry.getName();

                        // ignore directory entries
                        if ( name.endsWith( "/" ) )
                        {
                            continue;
                        }

                        // cut off path and use file name for checking against patterns
                        name = name.substring( name.lastIndexOf( '/' ) + 1 );
                        for ( int i = 0; i < patterns.length; i++ )
                        {
                            if ( name.startsWith( patterns[i] ) )
                            {
                                JSONObject entry = new JSONObject();
                                entry.put( "jar", url.getPath() );
                                entry.put( "path", zentry.getName() );
                                entry.put( "url", getName( name ) );
                                ret.append( resName, entry );
                            }
                        }
                    }
                }
                finally
                {
                    IOUtils.closeQuietly( ins );
                }

            }
        }

        return ret;
    }


    private boolean sendResource( final PathInfo pathInfo, final HttpServletResponse response ) throws IOException
    {

        final String name = pathInfo.licenseFile.substring( pathInfo.licenseFile.lastIndexOf( '/' ) + 1 );
        boolean isLicense = false;
        for ( int i = 0; !isLicense && i < LICENSE_FILES.length; i++ )
        {
            isLicense = name.startsWith( LICENSE_FILES[i] );
        }

        final Bundle bundle = getBundleContext().getBundle( pathInfo.bundleId );
        if ( bundle == null )
        {
            return false;
        }

        // prepare the response
        WebConsoleUtil.setNoCache( response );
        response.setContentType( "text/plain" );

        if ( pathInfo.innerJar == null )
        {
            final URL resource = bundle.getResource( pathInfo.licenseFile );
            if ( resource != null )
            {
                final InputStream input = resource.openStream();
                try
                {
                    IOUtils.copy( input, response.getWriter() );
                    return true;
                }
                finally
                {
                    IOUtils.closeQuietly( input );
                }
            }
        }

        // license is in a nested JAR
        final URL zipResource = bundle.getResource( pathInfo.innerJar );
        if ( zipResource != null )
        {
            final InputStream input = zipResource.openStream();
            ZipInputStream zin = null;
            try
            {
                zin = new ZipInputStream( input );
                for ( ZipEntry zentry = zin.getNextEntry(); zentry != null; zentry = zin.getNextEntry() )
                {
                    if ( pathInfo.licenseFile.equals( zentry.getName() ) )
                    {
                        IOUtils.copy( zin, response.getWriter() );
                        return true;
                    }
                }
            }
            finally
            {

                IOUtils.closeQuietly( zin );
                IOUtils.closeQuietly( input );
            }
        }

        // throw new ServletException("License file:" + url + " not found!");
        return false;
    }

    // package private for unit testing of the parse method
    static class PathInfo
    {
        final long bundleId;
        final String innerJar;
        final String licenseFile;


        static PathInfo parse( final String pathInfo )
        {
            if ( pathInfo == null || pathInfo.length() == 0 || !pathInfo.startsWith( "/" + LABEL + "/" ) )
            {
                return null;
            }

            // cut off label prefix including slashes around the label
            final String parts = pathInfo.substring( LABEL.length() + 2 );

            int slash = parts.indexOf( '/' );
            if ( slash <= 0 )
            {
                return null;
            }

            final long bundleId;
            try
            {
                bundleId = Long.parseLong( parts.substring( 0, slash ) );
                if ( bundleId < 0 )
                {
                    return null;
                }
            }
            catch ( NumberFormatException nfe )
            {
                // illegal bundle id
                return null;
            }

            final String innerJar;
            int jarSep = parts.indexOf( "!/", slash );
            if ( jarSep < 0 )
            {
                innerJar = null;
            }
            else
            {
                innerJar = parts.substring( slash, jarSep );
                slash = jarSep + 2; // ignore bang-slash
            }

            final String licenseFile = parts.substring( slash );

            return new PathInfo( bundleId, innerJar, licenseFile );
        }


        private PathInfo( final long bundleId, final String innerJar, final String licenseFile )
        {
            this.bundleId = bundleId;
            this.innerJar = innerJar;
            this.licenseFile = licenseFile;
        }
    }
}
