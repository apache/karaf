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
import java.util.*;

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
            synchronized ( this.events )
            {
                this.events.add(new EventInfo(event));
                if ( events.size() > this.maxSize )
                {
                    events.remove(0);
                }
            }
        }
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, IOException
    {
        final String action = getParameter(req, "action");
        // for now we only have the clear action
        if ( "clear".equals(action) )
        {
            synchronized ( this.events )
            {
                this.events.clear();
            }
        }
        // we always send back the json data
        resp.setContentType("application/json");
        resp.setCharacterEncoding("utf-8");

        renderJSON(resp.getWriter());
    }

    private void renderJSON(final PrintWriter pw)
    throws IOException
    {
        List copiedEvents;
        synchronized ( this.events )
        {
            copiedEvents = new ArrayList(this.events);
        }
        // create status line
        final EventAdmin admin = (EventAdmin) this.getService(EventAdmin.class.getName());
        StringBuffer statusLine = new StringBuffer();
        if ( admin == null ) {
            statusLine.append("Event Admin is not installed/running.");
        } else {
            statusLine.append("Event Admin is running.");
        }
        statusLine.append(" ");
        statusLine.append(copiedEvents.size());
        statusLine.append(" Events received");
        if ( !copiedEvents.isEmpty() ) {
            statusLine.append(" since ");
            Date d = new Date();
            d.setTime(((EventInfo)copiedEvents.get(0)).received);
            statusLine.append(d);
        }
        statusLine.append(".");

        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "status" );
            jw.value ( statusLine );

            jw.key( "data" );

            jw.array();

            // display list in reverse order
            for(int index = copiedEvents.size() -1; index >= 0; index--)
            {
                eventJson( jw, (EventInfo)copiedEvents.get(index), index );
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
            response.setContentType( "text/javascript" );
            response.setCharacterEncoding( "UTF-8" );

            PrintWriter pw = response.getWriter();
            this.renderJSON(pw);

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }

    protected void renderContent( HttpServletRequest request, HttpServletResponse response )
    throws ServletException, IOException
    {
        final PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/jquery-1.2.6.min.js' language='JavaScript'></script>" );
        pw.println( "<script src='" + appRoot + "/res/ui/jquery.tablesorter-2.0.3.min.js' language='JavaScript'></script>" );
        pw.println( "<script src='" + appRoot + "/res/ui/events.js' language='JavaScript'></script>" );

        Util.startScript( pw );
        pw.println( "renderEvents( );" );
        Util.endScript( pw );
    }

    private void eventJson( JSONWriter jw, EventInfo info, int index)
    throws JSONException
    {
        final Event e = info.event;
        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf(index) );
        jw.key( "received");
        jw.value( info.received );
        jw.key( "topic" );
        jw.value( e.getTopic());
        jw.key( "properties" );
        jw.object();
        final String[] names = e.getPropertyNames();
        if ( names != null && names.length > 0 )
        {
            for(int i=0;i<names.length;i++)
            {
                jw.key(names[i]);
                jw.value(e.getProperty(names[i]).toString());
            }
        }
        jw.endObject();

        jw.endObject();
    }

    private static final class EventInfo
    {

        public final Event event;
        public final long  received;

        public EventInfo(final Event e)
        {
            this.event = e;
            this.received = System.currentTimeMillis();
        }
    }
}
