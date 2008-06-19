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
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.Component;
import org.apache.felix.scr.Reference;
import org.apache.felix.scr.ScrService;
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;


public class ComponentsServlet extends BaseWebConsolePlugin
{

    public static final String NAME = "components";

    public static final String LABEL = "Components";

    public static final String COMPONENT_ID = "componentId";

    public static final String OPERATION = "action";

    public static final String OPERATION_ENABLE = "enable";

    public static final String OPERATION_DISABLE = "disable";

    private static final String SCR_SERVICE = ScrService.class.getName();


    public String getTitle()
    {
        return LABEL;
    }


    public String getLabel()
    {
        return NAME;
    }


    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        ScrService scrService = getScrService();
        if ( scrService != null )
        {

            long componentId = getComponentId( request );
            Component component = scrService.getComponent( componentId );

            if ( component != null )
            {
                String op = request.getParameter( OPERATION );
                if ( OPERATION_ENABLE.equals( op ) )
                {
                    component.enable();
                }
                else if ( OPERATION_DISABLE.equals( op ) )
                {
                    component.disable();
                }

                sendAjaxDetails( component, response );
            }

        }
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/datatable.js' language='JavaScript'></script>" );

        Util.startScript( pw );

        pw.print( "var components = " );
        renderResult( request, pw );
        pw.println( ";" );

        pw.println( "renderDataTable( components );" );

        Util.endScript( pw );
    }


    private void renderResult( HttpServletRequest request, PrintWriter pw ) throws IOException
    {
        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "numActions" );
            jw.value( 2 );

            ScrService scrService = getScrService();
            if ( scrService == null )
            {
                jw.key( "error" );
                jw.value( "Apache Felix Declarative Service required for this function" );
            }
            else
            {
                Component[] components = null;
                boolean details = false;

                long componentId = getComponentId( request );
                if ( componentId >= 0 )
                {
                    Component component = scrService.getComponent( componentId );
                    if ( component != null )
                    {
                        components = new Component[]
                            { component };
                        details = true;
                    }
                }

                if ( components == null )
                {
                    components = scrService.getComponents();
                }

                if ( components == null || components.length == 0 )
                {
                    jw.key( "error" );
                    jw.value( "No components installed currently" );
                }
                else
                {
                    // order components by name
                    TreeMap componentMap = new TreeMap();
                    for ( int i = 0; i < components.length; i++ )
                    {
                        Component component = components[i];
                        componentMap.put( component.getName(), component );
                    }

                    // render components
                    jw.key( "data" );
                    jw.array();
                    for ( Iterator ci = componentMap.values().iterator(); ci.hasNext(); )
                    {
                        component( jw, ( Component ) ci.next(), details );
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


    private void sendAjaxDetails( Component component, HttpServletResponse response ) throws IOException
    {

        // send the result
        response.setContentType( "text/javascript" );

        JSONWriter jw = new JSONWriter( response.getWriter() );
        try
        {
            if ( component != null )
            {
                component( jw, component, true );
            }
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
        jw.value( toStateString( state ) );

        // component actions
        jw.key( "actions" );
        jw.array();

        jw.object();
        jw.key( "name" );
        jw.value( "Enable" );
        jw.key( "link" );
        jw.value( OPERATION_ENABLE );
        jw.key( "enabled" );
        jw.value( state == Component.STATE_DISABLED );
        jw.endObject();

        jw.object();
        jw.key( "name" );
        jw.value( "Disable" );
        jw.key( "link" );
        jw.value( OPERATION_DISABLE );
        jw.key( "enabled" );
        jw.value( state != Component.STATE_DISABLED && state != Component.STATE_DESTROYED );
        jw.endObject();

        jw.endArray();

        // component details
        if ( details )
        {
            gatherComponentDetails( jw, component );
        }

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

        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < services.length; i++ )
        {
            if ( i > 0 )
            {
                buf.append( "<br />" );
            }
            buf.append( services[i] );
        }

        keyVal( jw, "Services", buf.toString() );
    }


    private void listReferences( JSONWriter jw, Component component )
    {
        Reference[] refs = component.getReferences();
        if ( refs != null )
        {
            for ( int i = 0; i < refs.length; i++ )
            {
                StringBuffer buf = new StringBuffer();
                buf.append( refs[i].isSatisfied() ? "Satisfied" : "Unsatisfied" ).append( "<br />" );
                buf.append( "Service Name: " ).append( refs[i].getServiceName() ).append( "<br />" );
                if ( refs[i].getTarget() != null )
                {
                    buf.append( "Target Filter: " ).append( refs[i].getTarget() ).append( "<br />" );
                }
                buf.append( "Multiple: " ).append( refs[i].isMultiple() ? "multiple" : "single" ).append( "<br />" );
                buf.append( "Optional: " ).append( refs[i].isOptional() ? "optional" : "mandatory" ).append( "<br />" );
                buf.append( "Policy: " ).append( refs[i].isStatic() ? "static" : "dynamic" ).append( "<br />" );

                // list bound services
                ServiceReference[] boundRefs = refs[i].getServiceReferences();
                if ( boundRefs != null && boundRefs.length > 0 )
                {
                    for ( int j = 0; j < boundRefs.length; j++ )
                    {
                        if ( j > 0 )
                        {
                            buf.append( "<br />" );
                        }

                        buf.append( "Bound Service ID " );
                        buf.append( boundRefs[j].getProperty( Constants.SERVICE_ID ) );

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
                            buf.append( " (" );
                            buf.append( name );
                            buf.append( ")" );
                        }
                    }
                }
                else
                {
                    buf.append( "No Services bound" );
                }
                buf.append( "<br />" );

                keyVal( jw, "Reference " + refs[i].getName(), buf.toString() );
            }
        }
    }


    private void listProperties( JSONWriter jw, Component component )
    {
        Dictionary props = component.getProperties();
        if ( props != null )
        {
            StringBuffer buf = new StringBuffer();
            TreeSet keys = new TreeSet( Collections.list( props.keys() ) );
            for ( Iterator ki = keys.iterator(); ki.hasNext(); )
            {
                String key = ( String ) ki.next();
                buf.append( key ).append( " = " );

                Object prop = props.get( key );
                if ( prop.getClass().isArray() )
                {
                    prop = Arrays.asList( ( Object[] ) prop );
                }
                buf.append( prop );
                if ( ki.hasNext() )
                {
                    buf.append( "<br />" );
                }
            }

            keyVal( jw, "Properties", buf.toString() );
        }

    }


    private void keyVal( JSONWriter jw, String key, Object value )
    {
        if ( key != null && value != null )
        {
            try
            {
                jw.object();
                jw.key( "key" );
                jw.value( key );
                jw.key( "value" );
                jw.value( value );
                jw.endObject();
            }
            catch ( JSONException je )
            {
                // don't care
            }
        }
    }


    static String toStateString( int state )
    {
        switch ( state )
        {
            case Component.STATE_DISABLED:
                return "disabled";
            case Component.STATE_ENABLED:
                return "enabled";
            case Component.STATE_UNSATISFIED:
                return "unsatisifed";
            case Component.STATE_ACTIVATING:
                return "activating";
            case Component.STATE_ACTIVE:
                return "active";
            case Component.STATE_REGISTERED:
                return "registered";
            case Component.STATE_FACTORY:
                return "factory";
            case Component.STATE_DEACTIVATING:
                return "deactivating";
            case Component.STATE_DESTROYED:
                return "destroyed";
            default:
                return String.valueOf( state );
        }
    }


    protected long getComponentId( HttpServletRequest request )
    {
        String componentIdPar = request.getParameter( ComponentsServlet.COMPONENT_ID );
        if ( componentIdPar == null )
        {
            String info = request.getPathInfo();
            componentIdPar = info.substring( info.lastIndexOf( '/' ) + 1 );
        }

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


    private ScrService getScrService()
    {
        return ( ScrService ) getService( SCR_SERVICE );
    }
}
