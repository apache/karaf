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

import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;


/**
 * LogServlet provides support for reading the log messages.
 */
public class LogServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    private static final String LABEL = "logs";
    private static final String TITLE = "%log.pluginTitle";
    private static final String CSS[] = { "/res/ui/logs.css" };

    private final static int MAX_LOGS = 200; //maximum number of log entries

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public LogServlet()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/logs.html" );
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        final int minLevel = WebConsoleUtil.getParameterInt( req, "minLevel", LogService.LOG_DEBUG );

        resp.setContentType( "application/json" );
        resp.setCharacterEncoding( "utf-8" );

        renderJSON( resp.getWriter(), minLevel );
    }


    private final void renderJSON( final PrintWriter pw, int minLogLevel ) throws IOException
    {
        // create status line
        final LogReaderService logReaderService = ( LogReaderService ) this.getService( LogReaderService.class
            .getName() );

        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( logReaderService == null ? Boolean.FALSE : Boolean.TRUE );

            jw.key( "data" );
            jw.array();

            if ( logReaderService != null )
            {
                int index = 0;
                for ( Enumeration logEntries = logReaderService.getLog(); logEntries.hasMoreElements()
                    && index < MAX_LOGS; )
                {
                    LogEntry nextLog = ( LogEntry ) logEntries.nextElement();
                    if ( nextLog.getLevel() <= minLogLevel )
                    {
                        logJson( jw, nextLog, index++ );
                    }
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


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final int minLevel = WebConsoleUtil.getParameterInt( request, "minLevel", LogService.LOG_DEBUG );
        final String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            response.setContentType( "application/json" );
            response.setCharacterEncoding( "UTF-8" );

            PrintWriter pw = response.getWriter();
            this.renderJSON( pw, minLevel );
            return;
        }
        super.doGet( request, response );
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        response.getWriter().print(TEMPLATE);
    }


    private static final void logJson( JSONWriter jw, LogEntry info, int index ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( String.valueOf( index ) );
        jw.key( "received" );
        jw.value( info.getTime() );
        jw.key( "level" );
        jw.value( logLevel( info.getLevel() ) );
        jw.key( "raw_level" );
        jw.value( info.getLevel() );
        jw.key( "message" );
        jw.value( info.getMessage() );
        jw.key( "service" );
        jw.value( serviceDescription( info.getServiceReference() ) );
        jw.key( "exception" );
        jw.value( exceptionMessage( info.getException() ) );
        jw.endObject();
    }


    private static final String serviceDescription( ServiceReference serviceReference )
    {
        if ( serviceReference == null )
            return "";
        else
            return serviceReference.toString();
    }


    private static final String logLevel( int level )
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


    private static final String exceptionMessage( Throwable e )
    {
        if ( e == null )
            return "";
        else
            return e.getClass().getName()+": "+e.getMessage();
    }

}
