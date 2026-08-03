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
import java.util.Locale;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.utils.json.JSONWriter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;


/**
 * ServicesServlet provides a plugin for inspecting the registered services.
 */
public class ServicesServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
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

    /** the label for the services plugin */
    public static final String LABEL = "services"; //$NON-NLS-1$
    private static final String TITLE = "%services.pluginTitle"; //$NON-NLS-1$
    private static final String CSS[] = null;

    // an LDAP filter, that is used to search services
    private static final String FILTER_PARAM = "filter";

    private final String TEMPLATE;

    /** Default constructor */
    public ServicesServlet() {
        super(LABEL, TITLE, CATEGORY_OSGI, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/services.html" ); //$NON-NLS-1$
    }

    private ServiceRegistration bipReg;

    public void activate(BundleContext bundleContext)
    {
        super.activate(bundleContext);
        bipReg = new ServicesUsedInfoProvider( bundleContext.getBundle() ).register( bundleContext );
    }

    public void deactivate() {
        if ( null != bipReg )
        {
            bipReg.unregister();
            bipReg = null;
        }
        super.deactivate();
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
            ServiceReference[] refs = BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getAllServiceReferences( null, filterStr );
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


    private final ServiceReference[] getServices(String filter)
    {
        // empty filter string will return nothing, must set it to null to return all services
        if (filter != null && filter.trim().length() == 0) {
            filter = null;
        }
        try
        {
            final ServiceReference[] refs = BundleContextUtil.getWorkingBundleContext(this.getBundleContext()).getAllServiceReferences( null, filter );
            if ( refs != null )
            {
                return refs;
            }
        }
        catch ( InvalidSyntaxException e )
        {
            log( "Unable to access service reference list.", e );
        }

        // no services or invalid filter syntax (unlikely)
        return NO_REFS;
    }


    static final String getStatusLine( final ServiceReference[] services )
    {
        final int count = services.length;
        final StringBuffer buffer = new StringBuffer();
        buffer.append( count );
        buffer.append( " service" );
        if ( count != 1 )
            buffer.append( 's' );
        buffer.append( " in total" );
        return buffer.toString();
    }


    static final String propertyAsString( ServiceReference ref, String name )
    {
        final Object value = ref.getProperty( name );
        return WebConsoleUtil.toString( value );
    }


    private void renderJSON( final HttpServletResponse response, final ServiceReference service, final Locale locale )
            throws IOException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON( pw, service, locale, null);
    }

    private void keyVal( JSONWriter jw, String key, Object val) throws IOException
    {
        if ( val != null )
        {
            jw.object();
            jw.key("key").value(key);
            jw.key("value").value(val);
            jw.endObject();
        }
    }

    private void serviceDetails( JSONWriter jw, ServiceReference service ) throws IOException
    {
        String[] keys = service.getPropertyKeys();

        jw.key( "props" );
        jw.array();

        for ( int i = 0; i < keys.length; i++ )
        {
            String key = keys[i];
            if ( Constants.SERVICE_PID.equals( key ) )
            {
                keyVal(jw, "Service PID", service.getProperty( key ));
            }
            else if ( Constants.SERVICE_DESCRIPTION.equals( key ) )
            {
                keyVal(jw, "Service Description", service.getProperty( key ));
            }
            else if ( Constants.SERVICE_VENDOR.equals( key ) )
            {
                keyVal(jw, "Service Vendor", service.getProperty( key ));
            }
            else if ( !Constants.OBJECTCLASS.equals( key ) && !Constants.SERVICE_ID.equals( key ) )
            {
                keyVal(jw, key, service.getProperty( key ));
            }

        }

        jw.endArray();

    }


    private void usingBundles( JSONWriter jw, ServiceReference service, Locale locale ) throws IOException
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


    private void serviceInfo( JSONWriter jw, ServiceReference service, boolean details, final Locale locale )
            throws IOException
    {
        jw.object();
        jw.key( "id" );
        jw.value( propertyAsString( service, Constants.SERVICE_ID ) );
        jw.key( "types" );
        jw.value( propertyAsString( service, Constants.OBJECTCLASS ) );
        jw.key( "pid" );
        jw.value( propertyAsString( service, Constants.SERVICE_PID ) );
        jw.key( "ranking" );
        final Object ranking = service.getProperty(Constants.SERVICE_RANKING);
        if ( ranking != null )
        {
            jw.value( ranking.toString() );
        }
        else
        {
            jw.value("");
        }
        bundleInfo( jw, service.getBundle(), locale );

        if ( details )
        {
            serviceDetails( jw, service );
            usingBundles( jw, service, locale );
        }

        jw.endObject();
    }


    private void bundleInfo( final JSONWriter jw, final Bundle bundle, final Locale locale )
            throws IOException
    {
        jw.key( "bundleId" );
        jw.value( bundle.getBundleId() );
        jw.key( "bundleName" );
        jw.value( Util.getName( bundle, locale ) );
        jw.key( "bundleVersion" );
        jw.value( Util.getHeaderValue( bundle, Constants.BUNDLE_VERSION ) );
        jw.key( "bundleSymbolicName" );
        jw.value( bundle.getSymbolicName() );
    }


    private void writeJSON(final Writer pw, final ServiceReference service, final Locale locale, final String filter) throws IOException
    {
        writeJSON( pw, service, false, locale, filter );
    }


    private void writeJSON( final Writer pw, final ServiceReference service, final boolean fullDetails, final Locale locale, final String filter )
            throws IOException
    {
        final ServiceReference[] allServices = this.getServices(filter);
        final String statusLine = getStatusLine( allServices );

        final ServiceReference[] services = ( service != null ) ? new ServiceReference[]
                { service } : allServices;

                final JSONWriter jw = new JSONWriter( pw );

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
        final String filter = request.getParameter(FILTER_PARAM);
        writeJSON(w, reqInfo.service, request.getLocale(), filter);

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "bundlePath", appRoot +  "/" + BundlesServlet.NAME + "/" );
        vars.put( "drawDetails", String.valueOf(reqInfo.serviceRequested));
        vars.put( "__data__", w.toString() );
        // CVE-2025-25247 (FELIX-6751): the filter is rendered into a JavaScript
        // string literal in templates/services.html, so HTML encoding is unsafe.
        // Apply JavaScript-string encoding instead.
        vars.put( "filter", filter == null ? "" : escapeJavaScript(filter));

        response.getWriter().print( TEMPLATE );
    }


    private static String escapeJavaScript( final String input )
    {
        final StringBuilder sb = new StringBuilder( input.length() + 16 );
        for ( int i = 0; i < input.length(); i++ )
        {
            final char c = input.charAt( i );
            switch ( c )
            {
                case '\\': sb.append( "\\\\" ); break;
                case '\'': sb.append( "\\'" ); break;
                case '"':  sb.append( "\\\"" ); break;
                case '/':  sb.append( "\\/" ); break;
                case '\n': sb.append( "\\n" ); break;
                case '\r': sb.append( "\\r" ); break;
                case '\t': sb.append( "\\t" ); break;
                case '\b': sb.append( "\\b" ); break;
                case '\f': sb.append( "\\f" ); break;
                case '<':  sb.append( "\\u003C" ); break;
                case '>':  sb.append( "\\u003E" ); break;
                case '&':  sb.append( "\\u0026" ); break;
                default:
                    if ( c < 0x20 || c == 0x2028 || c == 0x2029 )
                    {
                        sb.append( String.format( "\\u%04X", (int) c ) );
                    }
                    else
                    {
                        sb.append( c );
                    }
            }
        }
        return sb.toString();
    }
}
