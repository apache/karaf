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
package org.apache.felix.cm;


import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;


/**
 * The <code>MockLogService</code> is a very simple log service, which just
 * prints the loglevel and message to StdErr.
 */
public class MockLogService implements LogService
{

    public void log( int logLevel, String message )
    {
        System.err.print( toMessageLine( logLevel, message ) );
    }


    public void log( int logLevel, String message, Throwable t )
    {
        log( logLevel, message );
    }


    public void log( ServiceReference ref, int logLevel, String message )
    {
        log( logLevel, message );
    }


    public void log( ServiceReference ref, int logLevel, String message, Throwable t )
    {
        log( logLevel, message );
    }


    /**
     * Helper method to format log level and log message exactly the same as the
     * <code>ConfigurationManager.log()</code> does.
     */
    public static String toMessageLine( int level, String message )
    {
        String messageLine;
        switch ( level )
        {
            case LogService.LOG_INFO:
                messageLine = "*INFO *";
                break;

            case LogService.LOG_WARNING:
                messageLine = "*WARN *";
                break;

            case LogService.LOG_ERROR:
                messageLine = "*ERROR*";
                break;

            case LogService.LOG_DEBUG:
            default:
                messageLine = "*DEBUG*";
        }
        return messageLine + " " + message + System.getProperty( "line.separator" );
    }
}
