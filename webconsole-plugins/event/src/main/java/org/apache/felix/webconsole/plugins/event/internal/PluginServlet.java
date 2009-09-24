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
package org.apache.felix.webconsole.plugins.event.internal;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import org.json.JSONException;
import org.json.JSONWriter;

/**
 * The Event Plugin
 */
public class PluginServlet extends HttpServlet
{
    private static final String ACTION_CLEAR = "clear";

    private static final String PARAMETER_ACTION = "action";

    /** The event collector. */
    private final EventCollector collector;

    /** Is the event admin available? */
    private boolean eventAdminAvailable = false;

    /** Is the config admin available? */
    private boolean configAdminAvailable = false;

    public PluginServlet()
    {
        this.collector = new EventCollector(null);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest req, HttpServletResponse resp )
    throws ServletException, IOException
    {
        final String action = req.getParameter( PARAMETER_ACTION );
        // for now we only have the clear action
        if ( ACTION_CLEAR.equals( action ) )
        {
            this.collector.clear();
        }
        // we always send back the json data
        resp.setContentType( "application/json" );
        resp.setCharacterEncoding( "utf-8" );

        renderJSON( resp.getWriter() );
    }

    private void renderJSON( final PrintWriter pw ) throws IOException
    {
        List events = this.collector.getEvents();

        StringBuffer statusLine = new StringBuffer();
        if ( this.eventAdminAvailable )
        {
            statusLine.append("Event Admin service is available. ");
        }
        else
        {
            statusLine.append("Event Admin service is not available. ");
        }
        statusLine.append( events.size() );
        statusLine.append( " Event");
        if ( events.size() != 1 )
        {
            statusLine.append('s');
        }
        statusLine.append( " received" );
        if ( !events.isEmpty() )
        {
            statusLine.append( " since " );
            Date d = new Date();
            d.setTime( ( ( EventInfo ) events.get( 0 ) ).received );
            statusLine.append( d );
        }
        statusLine.append( "." );

        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "data" );

            jw.array();

            // display list in reverse order
            for ( int index = events.size() - 1; index >= 0; index-- )
            {
                eventJson( jw, ( EventInfo ) events.get( index ), index );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {

        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            response.setContentType( "application/json" );
            response.setCharacterEncoding( "UTF-8" );

            PrintWriter pw = response.getWriter();
            this.renderJSON( pw );

            // nothing more to do
            return;
        }

        this.renderContent( request, response );
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        final PrintWriter pw = response.getWriter();

        final String appRoot = ( String ) request.getAttribute( "felix.webconsole.appRoot" );
        pw.println( "<script src='" + appRoot + "/res/ui/" + "events.js" + "' language='JavaScript'></script>" );

        pw.println( "<div id='plugin_content'/>");

        pw.println( "<script type='text/javascript'>" );
        pw.println( "// <![CDATA[" );
        pw.println( "renderEvents( );" );
        pw.println( "// ]]>" );
        pw.println( "</script>" );
    }


    private void eventJson( JSONWriter jw, EventInfo info, int index ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf( index ) );
        jw.key( "received" );
        jw.value( info.received );
        jw.key( "topic" );
        jw.value( info.topic );
        if ( info.info != null )
        {
            jw.key( "info" );
            jw.value( info.info );
        }
        jw.key( "properties" );
        jw.object();
        if ( info.properties != null && info.properties.size() > 0 )
        {
            final Iterator i = info.properties.entrySet().iterator();
            while ( i.hasNext() )
            {
                final Map.Entry current = (Entry) i.next();
                jw.key( current.getKey().toString() );
                final Object value = current.getValue();
                if ( value.getClass().isArray() )
                {
                    // as we can't use 1.5 functionality we have to print the array ourselves
                    Object[] arr = (Object[])value;
                    final StringBuffer b = new StringBuffer("[");
                    for(int m=0; m<arr.length; m++) {
                        if ( m > 0 )
                        {
                            b.append(", ");
                        }
                        b.append(arr[m].toString());
                    }
                    b.append(']');
                    jw.value(b.toString());
                }
                else
                {
                    jw.value(value.toString());
                }
            }
        }
        jw.endObject();

        jw.endObject();
    }

    public void updateConfiguration( Dictionary dict)
    {
        this.collector.updateConfiguration(dict);
    }

    public EventCollector getCollector()
    {
        return this.collector;
    }

    public void setEventAdminAvailable(final boolean flag)
    {
        this.eventAdminAvailable = flag;
    }
}
