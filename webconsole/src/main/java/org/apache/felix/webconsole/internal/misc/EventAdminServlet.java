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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.*;


/**
 * @scr.component metatype="false"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.service interface="org.osgi.service.event.EventHandler"
 * @scr.property name="event.topics" value="*"
 * @scr.property name="felix.webconsole.label" valueRef="LABEL"
 */
public class EventAdminServlet
extends BaseWebConsolePlugin
implements EventHandler
{

    public static final String LABEL = "events";

    public static final String TITLE = "Event Admin";

    /** Number of events to be displayed. */
    private int maxSize = 50;

    protected final List events = new ArrayList();

    public String getLabel()
    {
        return LABEL;
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
     */
    public String getTitle()
    {
        return TITLE;
    }

    /**
     * Activate this component.
     */
    protected void activate(ComponentContext context)
    {
        this.activate(context.getBundleContext());
        this.events.clear();
    }

    /**
     * Deactivate this component.
     */
    protected void deactivate(ComponentContext context)
    {
        this.deactivate();
        this.events.clear();
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event)
    {
        // we add everything which is not a log event
        if ( !event.getTopic().startsWith("org/osgi/service/log") )
        {
            this.events.add(event);
            if ( events.size() > this.maxSize )
            {
                events.remove(1);
            }
        }
    }

    protected void renderContent( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {

        PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/events.js' language='JavaScript'></script>" );

        pw.println("<h1>Events</h1>");
        final EventAdmin admin = (EventAdmin) this.getService(EventAdmin.class.getName());
        if ( admin == null ) {
            pw.println("<p><em>Event Admin is not installed.</em></p>");
        }

        Util.startScript( pw );
        pw.println( "var eventListData = " );
        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "data" );

            jw.array();

            for(int i=0; i<events.size(); i++)
            {
                eventJson( jw, (Event)events.get(i), i );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

        pw.println( ";" );
        pw.println( "renderEvents( eventListData );" );
        Util.endScript( pw );
    }

    private void eventJson( JSONWriter jw, Event e, int index)
    throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf(index) );
        jw.key( "topic" );
        jw.value( e.getTopic());
        jw.key( "properties" );
        jw.object();
        final String[] names = e.getPropertyNames();
        if ( names != null && names.length > 0 ) {
            for(int i=0;i<names.length;i++) {
                jw.key(names[i]);
                jw.value(e.getProperty(names[i]).toString());
            }
        }
        jw.endObject();

        jw.endObject();
    }

}
