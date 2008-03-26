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
package org.apache.felix.http.jetty;


import java.util.HashMap;
import java.util.Map;

import org.mortbay.log.Logger;
import org.osgi.service.log.LogService;


public class LogServiceLog implements Logger
{
    private static Map loggers = new HashMap();

    private final String m_name;

    private boolean m_debugEnabled;


    public LogServiceLog()
    {
        this( "org.mortbay.log" );
    }


    public LogServiceLog( String name )
    {
        this.m_name = name;
    }


    public Logger getLogger( String name )
    {
        Logger logger = ( Logger ) loggers.get( name );
        if ( logger == null )
        {
            logger = new LogServiceLog( name );
            logger.setDebugEnabled( isDebugEnabled() );
            loggers.put( name, logger );
        }
        return logger;
    }


    public boolean isDebugEnabled()
    {
        return m_debugEnabled;
    }


    public void setDebugEnabled( boolean enabled )
    {
        this.m_debugEnabled = enabled;
    }


    public void debug( String msg, Throwable throwable )
    {
        log( LogService.LOG_DEBUG, msg, throwable );
    }


    public void debug( String msg, Object arg0, Object arg1 )
    {
        log( LogService.LOG_DEBUG, format( msg, arg0, arg1 ), null );
    }


    public void info( String msg, Object arg0, Object arg1 )
    {
        log( LogService.LOG_INFO, format( msg, arg0, arg1 ), null );
    }


    public void warn( String msg, Throwable throwable )
    {
        log( LogService.LOG_WARNING, msg, throwable );
    }


    public void warn( String msg, Object arg0, Object arg1 )
    {
        log( LogService.LOG_WARNING, format( msg, arg0, arg1 ), null );
    }


    public String toString()
    {
        return m_name;
    }


    private String format( String msg, Object arg0, Object arg1 )
    {
        int i0 = msg.indexOf( "{}" );
        int i1 = i0 < 0 ? -1 : msg.indexOf( "{}", i0 + 2 );

        if ( arg1 != null && i1 >= 0 )
            msg = msg.substring( 0, i1 ) + arg1 + msg.substring( i1 + 2 );
        if ( arg0 != null && i0 >= 0 )
            msg = msg.substring( 0, i0 ) + arg0 + msg.substring( i0 + 2 );
        return msg;
    }


    private void log( int level, String message, Throwable throwable )
    {
        Activator.log( level, m_name + ":" + message, throwable );
    }
}
