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
package org.apache.felix.webconsole.internal.compendium;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;


/**
 * ComponentsServlet provides a plugin for managing Service Components Runtime.
 */
public class ComponentsServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{

    private static final long serialVersionUID = 1L;

    private static final String LABEL = "components";
    private static final String TITLE = "%scr.pluginTitle";
    private static final String CSS[] = { "/res/ui/bundles.css" }; // yes, it's correct!

    // actions
    private static final String OPERATION = "action";
    private static final String OPERATION_ENABLE = "enable";
    private static final String OPERATION_DISABLE = "disable";
    private static final String OPERATION_CONFIGURE = "configure";

    // needed services
    private static final String SCR_SERVICE = ScrService.class.getName();
    private static final String META_TYPE_NAME = MetaTypeService.class.getName();
    private static final String CONFIGURATION_ADMIN_NAME = ConfigurationAdmin.class.getName();

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public ComponentsServlet()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/components.html" );
    }



    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        final RequestInfo reqInfo = new RequestInfo(request);
        if ( reqInfo.component == null && reqInfo.componentRequested ) {
            response.sendError(404);
            return;
        }
        if ( !reqInfo.componentRequested ) {
            response.sendError(500);
            return;
        }
        String op = request.getParameter( OPERATION );
        if ( OPERATION_ENABLE.equals( op ) )
        {
            reqInfo.component.enable();
        }
        else if ( OPERATION_DISABLE.equals( op ) )
        {
            reqInfo.component.disable();
        }

        final PrintWriter pw = response.getWriter();
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );
        renderResult( pw, null);
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
    IOException {
        final RequestInfo reqInfo = new RequestInfo(request);
        if ( reqInfo.component == null && reqInfo.componentRequested ) {
            response.sendError(404);
            return;
        }
        if ( reqInfo.extension.equals("json")  )
        {
            response.setContentType( "application/json" );
            response.setCharacterEncoding( "UTF-8" );

            this.renderResult(response.getWriter(), reqInfo.component);

            // nothing more to do
            return;
        }
        super.doGet( request, response );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
        renderResult( w2, reqInfo.component );

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__drawDetails__", reqInfo.componentRequested ? Boolean.TRUE : Boolean.FALSE );
        vars.put( "__data__", w.toString() );

        response.getWriter().print( TEMPLATE );

    }


    private void renderResult( final PrintWriter pw,
                               final Component component) throws IOException
    {
        final JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            final ScrService scrService = getScrService();
            if ( scrService == null )
            {
                jw.key( "status" );
                jw.value( -1 );
            }
            else
            {
                final Component[] components = scrService.getComponents();

                if ( components == null || components.length == 0 )
                {
                    jw.key( "status" );
                    jw.value( 0 );
                }
                else
                {
                    // order components by name
                    TreeMap componentMap = new TreeMap();
                    for ( int i = 0; i < components.length; i++ )
                    {
                        Component c = components[i];
                        componentMap.put( c.getName(), c );
                    }

                    final StringBuffer buffer = new StringBuffer();
                    buffer.append(componentMap.size());
                    buffer.append(" component");
                    if ( componentMap.size() != 1 ) {
                        buffer.append('s');
                    }
                    buffer.append(" installed.");
                    jw.key("status");
                    jw.value(componentMap.size());

                    // render components
                    jw.key( "data" );
                    jw.array();
                    if ( component != null )
                    {
                        component( jw, component, true );
                    }
                    else
                    {
                        for ( Iterator ci = componentMap.values().iterator(); ci.hasNext(); )
                        {
                            component( jw, ( Component ) ci.next(), false );
                        }
                    }
                    jw.endArray();
                }
            }

            jw.endObject();
        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }
    }

    private void component( JSONWriter jw, Component component, boolean details ) throws JSONException
    {
        String id = String.valueOf( component.getId() );
        String name = component.getName();
        int state = component.getState();

        jw.object();

        // component information
        jw.key( "id" );
        jw.value( id );
        jw.key( "name" );
        jw.value( name );
        jw.key( "state" );
        jw.value( ComponentConfigurationPrinter.toStateString( state ) );

        final String pid = ( String ) component.getProperties().get( Constants.SERVICE_PID );
        if ( pid != null )
        {
            jw.key("pid");
            jw.value(pid);
        }
        // component actions
        jw.key( "actions" );
        jw.array();

        if ( state == Component.STATE_DISABLED )
        {
            action(jw, true, OPERATION_ENABLE, "Enable", "enable" );
        }
        if ( state != Component.STATE_DISABLED && state != Component.STATE_DESTROYED )
        {
            action(jw, true, OPERATION_DISABLE, "Disable", "disable" );
        }
        if ( pid != null )
        {
            if ( isConfigurable( component.getBundle(), pid ) )
            {
                action(jw, true, OPERATION_CONFIGURE, "Configure", "configure" );
            }
        }

        jw.endArray();

        // component details
        if ( details )
        {
            gatherComponentDetails( jw, component );
        }

        jw.endObject();
    }

    private void action( JSONWriter jw, boolean enabled, String op, String opLabel, String image ) throws JSONException
    {
        jw.object();
        jw.key( "enabled" ).value( enabled );
        jw.key( "name" ).value( opLabel );
        jw.key( "link" ).value( op );
        jw.key( "image" ).value( image );
        jw.endObject();
    }

    private void gatherComponentDetails( JSONWriter jw, Component component ) throws JSONException
    {
        jw.key( "props" );
        jw.array();

        keyVal( jw, "Bundle", component.getBundle().getSymbolicName() + " (" + component.getBundle().getBundleId()
            + ")" );
        keyVal( jw, "Default State", component.isDefaultEnabled() ? "enabled" : "disabled" );
        keyVal( jw, "Activation", component.isImmediate() ? "immediate" : "delayed" );

        listServices( jw, component );
        listReferences( jw, component );
        listProperties( jw, component );

        jw.endArray();
    }


    private void listServices( JSONWriter jw, Component component )
    {
        String[] services = component.getServices();
        if ( services == null )
        {
            return;
        }

        keyVal( jw, "Service Type", component.isServiceFactory() ? "service factory" : "service" );

        JSONArray buf = new JSONArray();
        for ( int i = 0; i < services.length; i++ )
        {
            buf.put( services[i] );
        }

        keyVal( jw, "Services", buf );
    }


    private void listReferences( JSONWriter jw, Component component )
    {
        Reference[] refs = component.getReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                JSONArray buf = new JSONArray();
                buf.put( refs[i].isSatisfied() ? "Satisfied" : "Unsatisfied" );
                buf.put( "Service Name: " + refs[i].getServiceName());
                if ( refs[i].getTarget() != null )
                {
                    buf.put( "Target Filter: " + refs[i].getTarget() );
                }
                buf.put( "Multiple: " + (refs[i].isMultiple() ? "multiple" : "single" ));
                buf.put( "Optional: " + (refs[i].isOptional() ? "optional" : "mandatory" ));
                buf.put( "Policy: " + (refs[i].isStatic() ? "static" : "dynamic" ));

                // list bound services
                ServiceReference[] boundRefs = refs[i].getServiceReferences();
                if ( boundRefs != null && boundRefs.length > 0 )
                {
                    for ( int j = 0; j < boundRefs.length; j++ )
                    {
                        final StringBuffer b = new StringBuffer();
                        b.append( "Bound Service ID " );
                        b.append( boundRefs[j].getProperty( Constants.SERVICE_ID ) );

                        String name = ( String ) boundRefs[j].getProperty( ComponentConstants.COMPONENT_NAME );
                        if ( name == null )
                        {
                            name = ( String ) boundRefs[j].getProperty( Constants.SERVICE_PID );
                            if ( name == null )
                            {
                                name = ( String ) boundRefs[j].getProperty( Constants.SERVICE_DESCRIPTION );
                            }
                        }
                        if ( name != null )
                        {
                            b.append( " (" );
                            b.append( name );
                            b.append( ")" );
                        }
                        buf.put(b.toString());
                    }
                }
                else
                {
                    buf.put( "No Services bound" );
                }

                keyVal( jw, "Reference " + refs[i].getName(), buf.toString() );
            }
        }
    }


    private void listProperties( JSONWriter jw, Component component )
    {
        Dictionary props = component.getProperties();
        if ( props != null )
        {
            JSONArray buf = new JSONArray();
            TreeSet keys = new TreeSet( Util.list( props.keys() ) );
            for ( Iterator ki = keys.iterator(); ki.hasNext(); )
            {
                final String key = ( String ) ki.next();
                final StringBuffer b = new StringBuffer();
                b.append( key ).append( " = " );

                Object prop = props.get( key );
                if ( prop.getClass().isArray() )
                {
                    prop = Arrays.asList( ( Object[] ) prop );
                }
                b.append( prop );
                buf.put(b.toString());
            }

            keyVal( jw, "Properties", buf );
        }

    }


    private void keyVal( JSONWriter jw, String key, Object value )
    {
        try
        {
            WebConsoleUtil.keyVal( jw, key, value );
        }
        catch ( JSONException je )
        {
            // don't care
        }
    }


    /**
     * Check if the component with the specified pid is
     * configurable
     * @param providingBundle The Bundle providing the component. This may be
     *      theoretically be <code>null</code>.
     * @param pid A non null pid
     * @return <code>true</code> if the component is configurable.
     */
    private boolean isConfigurable( final Bundle providingBundle, final String pid )
    {
        // we first check if the config admin has something for this pid
        final ConfigurationAdmin ca = this.getConfigurationAdmin();
        if ( ca != null )
        {
            try
            {
                // we use listConfigurations to not create configuration
                // objects persistently without the user providing actual
                // configuration
                String filter = "(" + Constants.SERVICE_PID + "=" + pid + ")";
                Configuration[] configs = ca.listConfigurations( filter );
                if ( configs != null && configs.length > 0 )
                {
                    return true;
                }
            }
            catch ( InvalidSyntaxException ise )
            {
                // should print message
            }
            catch ( IOException ioe )
            {
                // should print message
            }
        }
        // second check is using the meta type service
        if ( providingBundle != null )
        {
            final MetaTypeService mts = this.getMetaTypeService();
            if ( mts != null )
            {
                final MetaTypeInformation mti = mts.getMetaTypeInformation( providingBundle );
                if ( mti != null )
                {
                    return mti.getObjectClassDefinition( pid, null ) != null;
                }
            }
        }
        return false;
    }

    private final ConfigurationAdmin getConfigurationAdmin()
    {
        return ( ConfigurationAdmin ) getService( CONFIGURATION_ADMIN_NAME );
    }

    final ScrService getScrService()
    {
        return ( ScrService ) getService( SCR_SERVICE );
    }

    private final MetaTypeService getMetaTypeService()
    {
        return ( MetaTypeService ) getService( META_TYPE_NAME );
    }

    private final class RequestInfo
    {
        public final String extension;
        public final Component component;
        public final boolean componentRequested;

        protected long getComponentId( final String componentIdPar )
        {
            try
            {
                return Long.parseLong( componentIdPar );
            }
            catch ( NumberFormatException nfe )
            {
                // TODO: log
            }

            // no bundleId or wrong format
            return -1;
        }

        protected RequestInfo( final HttpServletRequest request )
        {
            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if ( info.endsWith(".json") )
            {
                extension = "json";
                info = info.substring(0, info.length() - 5);
            }
            else
            {
                extension = "html";
            }

            long componentId = getComponentId(info.substring(info.lastIndexOf('/') + 1));
            if ( componentId == -1 )
            {
                componentRequested = false;
                component = null;
            }
            else
            {
                componentRequested = true;
                final ScrService scrService = getScrService();
                if ( scrService != null )
                {
                    component = scrService.getComponent( componentId );
                }
                else
                {
                    component = null;
                }
            }
            request.setAttribute(ComponentsServlet.class.getName(), this);
        }

    }

    static RequestInfo getRequestInfo(final HttpServletRequest request)
    {
        return (RequestInfo)request.getAttribute(ComponentsServlet.class.getName());
    }
}
