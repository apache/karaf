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
package org.apache.felix.webconsole.internal.core;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * ServicesServlet provides a plugin for inspecting the registered services.
 */
public class ServicesServlet extends SimpleWebConsolePlugin implements ConfigurationPrinter, OsgiManagerPlugin
{
    // don't create empty reference array all the time, create it only once - it is immutable
    private static final ServiceReference[] NO_REFS = new ServiceReference[0];

    private final class RequestInfo
    {
        public final String extension;
        public final ServiceReference service;
        public final boolean serviceRequested;


        protected RequestInfo( final HttpServletRequest request )
        {
            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring( getLabel().length() + 1 );

            // get extension
            if ( info.endsWith( ".json" ) )
            {
                extension = "json";
                info = info.substring( 0, info.length() - 5 );
            }
            else
            {
                extension = "html";
            }

            // we only accept direct requests to a service if they have a slash
            // after the label
            String serviceInfo = null;
            if ( info.startsWith( "/" ) )
            {
                serviceInfo = info.substring( 1 );
            }
            if ( serviceInfo == null || serviceInfo.length() == 0 )
            {
                service = null;
                serviceRequested = false;
            }
            else
            {
                service = getServiceById( serviceInfo );
                serviceRequested = true;
            }
            request.setAttribute( ServicesServlet.class.getName(), this );
        }

    }


    static RequestInfo getRequestInfo( final HttpServletRequest request )
    {
        return ( RequestInfo ) request.getAttribute( ServicesServlet.class.getName() );
    }

    private ServiceRegistration configurationPrinter;

    /** the label for the services plugin */
    public static final String LABEL = "services";
    private static final String TITLE = "%services.pluginTitle";
    private static final String CSS[] = null;

    private final String TEMPLATE;

    /** Default constructor */
    public ServicesServlet() {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/services.html" );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#activate(org.osgi.framework.BundleContext)
     */
    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );
        configurationPrinter = bundleContext.registerService( ConfigurationPrinter.SERVICE, this, null );
    }


    /**
     * @see org.apache.felix.webconsole.SimpleWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        if ( configurationPrinter != null )
        {
            configurationPrinter.unregister();
            configurationPrinter = null;
        }

        super.deactivate();
    }


    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration( PrintWriter pw )
    {
        try
        {
            StringWriter w = new StringWriter();
            writeJSON( w, null, true, Locale.ENGLISH );
            String jsonString = w.toString();
            JSONObject json = new JSONObject( jsonString );

            pw.println( "Status: " + json.get( "status" ) );
            pw.println();

            JSONArray data = json.getJSONArray( "data" );
            for ( int i = 0; i < data.length(); i++ )
            {
                if ( !data.isNull( i ) )
                {
                    JSONObject service = data.getJSONObject( i );

                    pw.println( MessageFormat.format( "Service {0} - {1} (pid: {2})", new Object[]
                        { service.get( "id" ), service.get( "types" ), service.get( "pid" ) } ) );
                    pw.println( MessageFormat.format( "  from Bundle {0} - {1} ({2}), version {3}", new Object[]
                        { service.get( "bundleId" ), service.get( "bundleName" ), service.get( "bundleSymbolicName" ),
                            service.get( "bundleVersion" ) } ) );

                    JSONArray props = service.getJSONArray( "props" );
                    for ( int pi = 0; pi < props.length(); pi++ )
                    {
                        if ( !props.isNull( pi ) )
                        {
                            JSONObject entry = props.getJSONObject( pi );

                            pw.print( "    " + entry.get( "key" ) + ": " );

                            Object entryValue = entry.get( "value" );
                            if ( entryValue instanceof JSONArray )
                            {
                                pw.println();
                                JSONArray entryArray = ( JSONArray ) entryValue;
                                for ( int ei = 0; ei < entryArray.length(); ei++ )
                                {
                                    if ( !entryArray.isNull( ei ) )
                                    {
                                        pw.println( "        " + entryArray.get( ei ) );
                                    }
                                }
                            }
                            else
                            {
                                pw.println( entryValue );
                            }
                        }
                    }

                    JSONArray usingBundles = service.getJSONArray( "usingBundles" );
                    for ( int ui = 0; ui < usingBundles.length(); ui++ )
                    {
                        if ( !usingBundles.isNull( ui ) )
                        {
                            JSONObject bundle = usingBundles.getJSONObject( ui );
                            pw.println( MessageFormat.format( "  Using Bundle {0} - {1} ({2}), version {3}", new Object[]
                                { bundle.get( "bundleId" ), bundle.get( "bundleName" ),
                                    bundle.get( "bundleSymbolicName" ), bundle.get( "bundleVersion" ) } ) );
                        }
                    }

                    pw.println();
                }
            }
        }
        catch ( Exception e )
        {
            log( "Problem rendering Bundle details for configuration status", e );
        }
    }


    private static final void appendServiceInfoCount( final StringBuffer buf, String msg, int count )
    {
        buf.append( count );
        buf.append( " service" );
        if ( count != 1 )
            buf.append( 's' );
        buf.append( ' ' );
        buf.append( msg );
    }


    final ServiceReference getServiceById( String pathInfo )
    {
        // only use last part of the pathInfo
        pathInfo = pathInfo.substring( pathInfo.lastIndexOf( '/' ) + 1 );

        StringBuffer filter = new StringBuffer();
        filter.append( "(" ).append( Constants.SERVICE_ID ).append( "=" );
        filter.append( pathInfo ).append( ")" );
        String filterStr = filter.toString();
        try
        {
            ServiceReference[] refs = getBundleContext().getServiceReferences( null, filterStr );
            if ( refs == null || refs.length != 1 )
            {
                return null;
            }
            return refs[0];
        }
        catch ( InvalidSyntaxException e )
        {
            log( "Unable to search for services using filter " + filterStr, e );
            // this shouldn't happen
            return null;
        }
    }


    private final ServiceReference[] getServices()
    {
        try
        {
            return getBundleContext().getServiceReferences( null, null );
        }
        catch ( InvalidSyntaxException e )
        {
            log( "Unable to access service reference list.", e );
            return NO_REFS;
        }
    }


    private static final String getStatusLine( final ServiceReference[] services )
    {
        final StringBuffer buffer = new StringBuffer();
        buffer.append( "Services information: " );
        appendServiceInfoCount( buffer, "in total", services.length );
        return buffer.toString();
    }


    private String propertyAsString( ServiceReference ref, String name )
    {
        Object value = ref.getProperty( name );
        if ( value instanceof Object[] )
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
        else if ( value != null )
        {
            return value.toString();
        }
        else
        {
            return "n/a";
        }
    }


    private void renderJSON( final HttpServletResponse response, final ServiceReference service, final Locale locale )
        throws IOException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON( pw, service, locale );
    }


    private void serviceDetails( JSONWriter jw, ServiceReference service ) throws JSONException
    {
        String[] keys = service.getPropertyKeys();

        jw.key( "props" );
        jw.array();

        for ( int i = 0; i < keys.length; i++ )
        {
            String key = keys[i];
            if ( Constants.SERVICE_PID.equals( key ) )
            {
                WebConsoleUtil.keyVal( jw, "Service PID", service.getProperty( key ) );
            }
            else if ( Constants.SERVICE_DESCRIPTION.equals( key ) )
            {
                WebConsoleUtil.keyVal( jw, "Service Description", service.getProperty( key ) );
            }
            else if ( Constants.SERVICE_VENDOR.equals( key ) )
            {
                WebConsoleUtil.keyVal( jw, "Service Vendor", service.getProperty( key ) );
            }
            else if ( !Constants.OBJECTCLASS.equals( key ) && !Constants.SERVICE_ID.equals( key ) )
            {
                WebConsoleUtil.keyVal( jw, key, service.getProperty( key ) );
            }

        }

        jw.endArray();

    }


    private void usingBundles( JSONWriter jw, ServiceReference service, Locale locale ) throws JSONException
    {
        jw.key( "usingBundles" );
        jw.array();

        Bundle[] usingBundles = service.getUsingBundles();
        if ( usingBundles != null )
        {
            for ( int i = 0; i < usingBundles.length; i++ )
            {
                jw.object();
                bundleInfo( jw, usingBundles[i], locale );
                jw.endObject();
            }
        }

        jw.endArray();

    }


    private void serviceInfo( JSONWriter jw, ServiceReference service, boolean details, final Locale locale ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( propertyAsString( service, Constants.SERVICE_ID ) );
        jw.key( "types" );
        jw.value( propertyAsString( service, Constants.OBJECTCLASS ) );
        jw.key( "pid" );
        jw.value( propertyAsString( service, Constants.SERVICE_PID ) );

        bundleInfo( jw, service.getBundle(), locale );

        if ( details )
        {
            serviceDetails( jw, service );
            usingBundles( jw, service, locale );
        }

        jw.endObject();
    }


    private void bundleInfo( final JSONWriter jw, final Bundle bundle, final Locale locale ) throws JSONException
    {
        jw.key( "bundleId" );
        jw.value( bundle.getBundleId() );
        jw.key( "bundleName" );
        jw.value( Util.getName( bundle, locale ) );
        jw.key( "bundleVersion" );
        jw.value( Util.getHeaderValue( bundle, Constants.BUNDLE_VERSION ) );
        jw.key( "bundleSymbolicName" );
        jw.value( Util.getHeaderValue( bundle, Constants.BUNDLE_SYMBOLICNAME ) );
    }


    private void writeJSON( final Writer pw, final ServiceReference service, final Locale locale ) throws IOException
    {
        writeJSON( pw, service, false, locale );
    }


    private void writeJSON( final Writer pw, final ServiceReference service, final boolean fullDetails, final Locale locale )
        throws IOException
    {
        final ServiceReference[] allServices = this.getServices();
        final String statusLine = getStatusLine( allServices );

        final ServiceReference[] services = ( service != null ) ? new ServiceReference[]
            { service } : allServices;

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "serviceCount" );
            jw.value( allServices.length );

            jw.key( "data" );

            jw.array();

            for ( int i = 0; i < services.length; i++ )
            {
                serviceInfo( jw, services[i], fullDetails || service != null, locale );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if (request.getPathInfo().indexOf("/res/") == -1)
        { // not resource
            final RequestInfo reqInfo = new RequestInfo( request );
            if ( reqInfo.service == null && reqInfo.serviceRequested )
            {
                response.sendError( 404 );
                return;
            }
            if ( reqInfo.extension.equals( "json" ) )
            {
                this.renderJSON( response, reqInfo.service, request.getLocale() );

                // nothing more to do
                return;
            }
        }

        super.doGet( request, response );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo( request );

        final String appRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT );
        StringWriter w = new StringWriter();
        writeJSON(w, reqInfo.service, request.getLocale());

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "bundlePath", appRoot +  "/" + BundlesServlet.NAME + "/" );
        vars.put( "drawDetails", reqInfo.serviceRequested ? Boolean.TRUE : Boolean.FALSE );
        vars.put( "__data__", w.toString() );

        response.getWriter().print( TEMPLATE );
    }

}
