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
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;


public class LogServlet extends BaseWebConsolePlugin
{
    public static final String LABEL = "logs";
    public static final String TITLE = "Log Service";

    private final static int MAX_LOGS = 200; //maximum number of log entries


    public LogServlet()
    {
    }


    public String getLabel()
    {
        return LABEL;
    }


    public String getTitle()
    {
        return TITLE;
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        final String minLevel = getParameter( req, "minLevel" );
        resp.setContentType( "application/json" );
        resp.setCharacterEncoding( "utf-8" );

        renderJSON( resp.getWriter(), extractLogLevel( minLevel ) );
    }


    private void renderJSON( final PrintWriter pw, int minLogLevel ) throws IOException
    {
        // create status line
        final LogReaderService logReaderService = ( LogReaderService ) this.getService( LogReaderService.class
            .getName() );

        StringBuffer statusLine = new StringBuffer();
        if ( logReaderService == null )
        {
            statusLine.append( "Log Service is not installed/running." );
        }
        else
        {
            statusLine.append( "Log Service is running." );
        }

        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "data" );
            jw.array();

            int index = 0;
            for ( Enumeration logEntries = logReaderService.getLog(); logEntries.hasMoreElements() && index < MAX_LOGS; )
            {
                LogEntry nextLog = ( LogEntry ) logEntries.nextElement();
                if ( nextLog.getLevel() <= minLogLevel )
                {
                    logJson( jw, nextLog, index++ );
                }
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }


    private int extractLogLevel( String minLevel )
    {
        if ( minLevel == null )
            return LogService.LOG_DEBUG;

        int minLogLevel = LogService.LOG_DEBUG;;
        try
        {
            minLogLevel = Integer.parseInt( minLevel );
        }
        catch ( Throwable t )
        {
            minLogLevel = LogService.LOG_DEBUG;
        }
        return minLogLevel;
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final String minLevel = getParameter( request, "minLevel" );
        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            response.setContentType( "application/json" );
            response.setCharacterEncoding( "UTF-8" );

            PrintWriter pw = response.getWriter();
            this.renderJSON( pw, extractLogLevel( minLevel ) );
            return;
        }
        super.doGet( request, response );
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final PrintWriter pw = response.getWriter();

        final String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        Util.script( pw, appRoot, "logs.js" );

        pw.println( "<div id='plugin_content'/>" );

        Util.startScript( pw );
        pw.println( "renderLogs( );" );
        Util.endScript( pw );
    }


    private void logJson( JSONWriter jw, LogEntry info, int index ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf( index ) );
        jw.key( "received" );
        jw.value( info.getTime() );
        jw.key( "level" );
        jw.value( logLevel( info.getLevel() ) );
        jw.key( "message" );
        jw.value( info.getMessage() );
        jw.key( "service" );
        jw.value( serviceDescription( info.getServiceReference() ) );
        jw.key( "exception" );
        jw.value( exceptionMessage( info.getException() ) );
        jw.endObject();
    }


    private String serviceDescription( ServiceReference serviceReference )
    {
        if ( serviceReference == null )
            return "";
        else
            return serviceReference.toString();
    }


    private String logLevel( int level )
    {
        switch ( level )
        {
            case LogService.LOG_INFO:
                return "INFO";
            case LogService.LOG_WARNING:
                return "WARNING";
            case LogService.LOG_ERROR:
                return "ERROR";
            case LogService.LOG_DEBUG:
            default:
                return "DEBUG";
        }
    }


    private String exceptionMessage( Throwable e )
    {
        if ( e == null )
            return "";
        else
            return e.getClass().getName()+": "+e.getMessage();
    }

}
