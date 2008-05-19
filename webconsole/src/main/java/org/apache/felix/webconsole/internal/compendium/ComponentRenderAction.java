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
import org.apache.felix.webconsole.Action;
import org.apache.felix.webconsole.Render;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentConstants;


public class ComponentRenderAction extends AbstractScrPlugin implements Render, Action
{

    public static final String NAME = "components";

    public static final String LABEL = "Components";

    public static final String COMPONENT_ID = "componentId";

    public static final String OPERATION = "op";

    public static final String OPERATION_DETAILS = "details";

    public static final String OPERATION_ENABLE = "enable";

    public static final String OPERATION_DISABLE = "disable";


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public boolean performAction( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        ScrService scrService = getScrService();
        if ( scrService != null )
        {

            long componentId = getComponentId( request );
            Component component = scrService.getComponent( componentId );

            if ( component != null )
            {
                String op = request.getParameter( OPERATION );
                if ( OPERATION_DETAILS.equals( op ) )
                {
                    return sendAjaxDetails( component, response );
                }
                else if ( OPERATION_ENABLE.equals( op ) )
                {
                    component.enable();
                }
                else if ( OPERATION_DISABLE.equals( op ) )
                {
                    component.disable();
                }
            }

        }

        return true;
    }


    public void render( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        PrintWriter pw = response.getWriter();

        this.header( pw );

        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='5' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        this.tableHeader( pw );

        ScrService scrService = getScrService();
        if ( scrService == null )
        {
            pw.println( "<tr class='content'>" );
            pw
                .println( "<td class='content' colspan='5'>Apache Felix Declarative Service required for this function</td>" );
            pw.println( "</tr>" );
        }
        else
        {
            Component[] components = scrService.getComponents();
            if ( components == null || components.length == 0 )
            {
                pw.println( "<tr class='content'>" );
                pw.println( "<td class='content' colspan='5'>No " + this.getLabel() + " installed currently</td>" );
                pw.println( "</tr>" );

            }
            else
            {

                // order components by id
                TreeMap componentMap = new TreeMap();
                for ( int i = 0; i < components.length; i++ )
                {
                    Component component = components[i];
                    componentMap.put( component.getName(), component );
                }

                // render components
                long previousComponent = -1;
                for ( Iterator ci = componentMap.values().iterator(); ci.hasNext(); )
                {
                    Component component = ( Component ) ci.next();
                    if ( previousComponent >= 0 )
                    {
                        // prepare for injected table information row
                        pw.println( "<tr id='component" + previousComponent + "'></tr>" );
                    }

                    component( pw, component );

                    previousComponent = component.getId();
                }

                if ( previousComponent >= 0 )
                {
                    // prepare for injected table information row
                    pw.println( "<tr id='component" + previousComponent + "'></tr>" );
                }
            }
        }

        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='5' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        this.footer( pw );
    }


    private void header( PrintWriter pw )
    {
        Util.startScript( pw );
        pw.println( "function showDetails(componentId) {" );
        pw.println( "    var span = document.getElementById('component' + componentId);" );
        pw.println( "    if (!span) {" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw.println( "    if (span.innerHTML) {" );
        pw.println( "        span.innerHTML = '';" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw.println( "    var parm = '?" + Util.PARAM_ACTION + "=" + NAME + "&" + OPERATION + "=" + OPERATION_DETAILS
            + "&" + COMPONENT_ID + "=' + componentId;" );
        pw.println( "    sendRequest('GET', parm, displayComponentDetails);" );
        pw.println( "}" );
        pw.println( "function displayComponentDetails(obj) {" );
        pw.println( "    var span = document.getElementById('component' + obj." + COMPONENT_ID + ");" );
        pw.println( "    if (!span) {" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw
            .println( "    var innerHtml = '<td class=\"content\">&nbsp;</td><td class=\"content\" colspan=\"4\"><table broder=\"0\">';" );
        pw.println( "    var props = obj.props;" );
        pw.println( "    for (var i=0; i < props.length; i++) {" );
        pw
            .println( "        innerHtml += '<tr><td valign=\"top\" noWrap>' + props[i].key + '</td><td valign=\"top\">' + props[i].value + '</td></tr>';" );
        pw.println( "    }" );
        pw.println( "    innerHtml += '</table></td>';" );
        pw.println( "    span.innerHTML = innerHtml;" );
        pw.println( "}" );
        Util.endScript( pw );

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
    }


    private void tableHeader( PrintWriter pw )
    {

        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content'>ID</th>" );
        pw.println( "<th class='content' width='100%'>Name</th>" );
        pw.println( "<th class='content'>Status</th>" );
        pw.println( "<th class='content' colspan='2'>Actions</th>" );
        pw.println( "</tr>" );
    }


    private void footer( PrintWriter pw )
    {
        pw.println( "</table>" );
    }


    private void component( PrintWriter pw, Component component )
    {
        String name = component.getName();

        pw.println( "<tr>" );
        pw.println( "<td class='content right'>" + component.getId() + "</td>" );
        pw.println( "<td class='content'><a href='javascript:showDetails(" + component.getId() + ")'>" + name
            + "</a></td>" );
        pw.println( "<td class='content center'>" + toStateString( component.getState() ) + "</td>" );

        boolean enabled = component.getState() == Component.STATE_DISABLED;
        this.actionForm( pw, enabled, component.getId(), OPERATION_ENABLE, "Enable" );

        enabled = component.getState() != Component.STATE_DISABLED && component.getState() != Component.STATE_DESTROYED;
        this.actionForm( pw, enabled, component.getId(), OPERATION_DISABLE, "Disable" );

        pw.println( "</tr>" );
    }


    private void actionForm( PrintWriter pw, boolean enabled, long componentId, String op, String opLabel )
    {
        pw.println( "<form name='form" + componentId + "' method='post'>" );
        pw.println( "<td class='content' align='right'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + NAME + "' />" );
        pw.println( "<input type='hidden' name='" + OPERATION + "' value='" + op + "' />" );
        pw.println( "<input type='hidden' name='" + COMPONENT_ID + "' value='" + componentId + "' />" );
        pw.println( "<input class='submit' type='submit' value='" + opLabel + "'" + ( enabled ? "" : "disabled" )
            + " />" );
        pw.println( "</td>" );
        pw.println( "</form>" );
    }


    private boolean sendAjaxDetails( Component component, HttpServletResponse response ) throws IOException
    {
        JSONObject result = null;
        try
        {
            if ( component != null )
            {

                JSONArray props = new JSONArray();
                keyVal( props, "Bundle", component.getBundle().getSymbolicName() + " ("
                    + component.getBundle().getBundleId() + ")" );
                keyVal( props, "Default State", component.isDefaultEnabled() ? "enabled" : "disabled" );
                keyVal( props, "Activation", component.isImmediate() ? "immediate" : "delayed" );

                listServices( props, component );
                listReferences( props, component );
                listProperties( props, component );

                result = new JSONObject();
                result.put( ComponentRenderAction.COMPONENT_ID, component.getId() );
                result.put( "props", props );
            }
        }
        catch ( Exception exception )
        {
            // create an empty result on problems
            result = new JSONObject();
        }

        // send the result
        response.setContentType( "text/javascript" );
        response.getWriter().print( result.toString() );

        return false;
    }


    private void listServices( JSONArray props, Component component )
    {
        String[] services = component.getServices();
        if ( services == null )
        {
            return;
        }

        keyVal( props, "Service Type", component.isServiceFactory() ? "service factory" : "service" );

        StringBuffer buf = new StringBuffer();
        for ( int i = 0; i < services.length; i++ )
        {
            if ( i > 0 )
            {
                buf.append( "<br />" );
            }
            buf.append( services[i] );
        }

        keyVal( props, "Services", buf.toString() );
    }


    private void listReferences( JSONArray props, Component component )
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

                keyVal( props, "Reference " + refs[i].getName(), buf.toString() );
            }
        }
    }


    private void listProperties( JSONArray jsonProps, Component component )
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
            keyVal( jsonProps, "Properties", buf.toString() );
        }

    }


    private void keyVal( JSONArray props, String key, Object value )
    {
        if ( key != null && value != null )
        {
            try
            {
                JSONObject obj = new JSONObject();
                obj.put( "key", key );
                obj.put( "value", value );
                props.put( obj );
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
        String componentIdPar = request.getParameter( ComponentRenderAction.COMPONENT_ID );
        if ( componentIdPar != null )
        {
            try
            {
                return Long.parseLong( componentIdPar );
            }
            catch ( NumberFormatException nfe )
            {
                // TODO: log
            }
        }

        // no bundleId or wrong format
        return -1;
    }

}
