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
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.*;

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
        statusLine.append( ". (Event admin: " );
        if ( !this.eventAdminAvailable )
        {
            statusLine.append("un");
        }
        statusLine.append("available; Config admin: ");
        if ( !this.configAdminAvailable )
        {
            statusLine.append("un");
        }
        statusLine.append("available)");

        // Compute scale: startTime is 0, lastTimestamp is 100%
        final long startTime = this.collector.getStartTime();
        final long endTime = (events.size() == 0 ? startTime : ((EventInfo)events.get(events.size() - 1)).received);
        final float scale = (endTime == startTime ? 100.0f : 100.0f / (endTime - startTime));

        pw.write("{");

        jsonKey( pw, "status" );
        jsonValue( pw, statusLine.toString() );
        pw.write(',');
        jsonKey( pw, "data" );

        pw.write('[');

        // display list in reverse order
        for ( int index = events.size() - 1; index >= 0; index-- )
        {
            eventJson( pw, ( EventInfo ) events.get( index ), index, startTime, scale );
            if ( index > 0 )
            {
                pw.write(',');
            }
        }

        pw.write(']');

        pw.write("}");
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
        pw.println( "<script src='" + appRoot + "/events/res/ui/" + "events.js" + "' language='JavaScript'></script>" );

        pw.println( "<div id='plugin_content'/>");

        pw.println( "<script type='text/javascript'>" );
        pw.println( "// <![CDATA[" );
        pw.println( "renderEvents( );" );
        pw.println( "// ]]>" );
        pw.println( "</script>" );
    }

    public URL getResource(String path)
    {
        if ( path.startsWith("/events/res/ui/") )
        {
            return this.getClass().getResource(path.substring(7));
        }
        return null;
    }

    private void jsonValue( final PrintWriter pw, final String v)
    throws IOException
    {
        if (v == null || v.length() == 0)
        {
            pw.write("\"\"");
            return;
        }

        pw.write('"');
        char previousChar = 0;
        char c;

        for (int i = 0; i < v.length(); i += 1)
        {
            c = v.charAt(i);
            switch (c)
            {
                case '\\':
                case '"':
                    pw.write('\\');
                    pw.write(c);
                    break;
                case '/':
                    if (previousChar == '<')
                    {
                        pw.write('\\');
                    }
                    pw.write(c);
                    break;
                case '\b':
                    pw.write("\\b");
                    break;
                case '\t':
                    pw.write("\\t");
                    break;
                case '\n':
                    pw.write("\\n");
                    break;
                case '\f':
                    pw.write("\\f");
                    break;
                case '\r':
                    pw.write("\\r");
                    break;
                default:
                    if (c < ' ')
                    {
                        final String hexValue = "000" + Integer.toHexString(c);
                        pw.write("\\u");
                        pw.write(hexValue.substring(hexValue.length() - 4));
                    }
                    else
                    {
                        pw.write(c);
                    }
            }
            previousChar = c;
        }
        pw.write('"');
    }

    private void jsonValue( final PrintWriter pw, final long l)
    {
        pw.write(Long.toString(l));
    }

    private void jsonKey( final PrintWriter pw, String key)
    throws IOException
    {
        jsonValue( pw, key);
        pw.write(':');
    }

    private void eventJson( PrintWriter jw, EventInfo info, int index, final long start, final float scale )
    throws IOException
    {
        final long msec = info.received - start;

        // Compute color bar size and make sure the bar is visible
        final int percent = Math.max((int)(msec * scale), 2);

        jw.write("{");
        jsonKey(jw, "id" );
        jsonValue(jw, String.valueOf( index ) );
        jw.write(',');
        jsonKey(jw, "offset" );
        jsonValue(jw, msec );
        jw.write(',');
        jsonKey(jw, "width" );
        jsonValue(jw, percent );
        jw.write(',');
        jsonKey(jw, "category" );
        jsonValue(jw, info.category );
        jw.write(',');
        jsonKey(jw, "received" );
        jsonValue(jw, info.received );
        jw.write(',');
        jsonKey(jw, "topic" );
        jsonValue(jw, info.topic );
        if ( info.info != null )
        {
            jw.write(',');
            jsonKey(jw, "info" );
            jsonValue(jw, info.info );
        }
        jw.write(',');
        jsonKey(jw, "properties" );
        jw.write("{");
        if ( info.properties != null && info.properties.size() > 0 )
        {
            final Iterator i = info.properties.entrySet().iterator();
            boolean first = true;
            while ( i.hasNext() )
            {
                final Map.Entry current = (Entry) i.next();
                if ( !first)
                {
                    jw.write(',');
                }
                first = false;
                jsonKey(jw, current.getKey().toString() );
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
                    jsonValue(jw, b.toString());
                }
                else
                {
                    jsonValue(jw, value.toString());
                }
            }
        }
        jw.write("}");

        jw.write("}");
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

    public void setConfigAdminAvailable(final boolean flag)
    {
        this.configAdminAvailable = flag;
    }
}
