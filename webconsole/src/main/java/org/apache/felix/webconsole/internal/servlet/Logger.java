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
package org.apache.felix.webconsole.internal.servlet;


import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;


public class Logger
{

    private ServiceTracker logTracker;


    Logger( BundleContext bundleContext )
    {
        logTracker = new ServiceTracker( bundleContext, LogService.class.getName(), null );
        logTracker.open();
    }


    void dispose()
    {
        if ( logTracker != null )
        {
            logTracker.close();
        }
    }


    public void log( int logLevel, String message )
    {
        log( logLevel, message, null );
    }


    public void log( int logLevel, String message, Throwable t )
    {
        Object log = logTracker.getService();
        if ( log != null )
        {
            ( ( LogService ) log ).log( logLevel, message, t );
        }
        else
        {
            String level;
            switch ( logLevel )
            {
                case LogService.LOG_DEBUG:
                    level = "*DEBUG*";
                    break;
                case LogService.LOG_INFO:
                    level = "*INFO *";
                    break;
                case LogService.LOG_WARNING:
                    level = "*WARN *";
                    break;
                case LogService.LOG_ERROR:
                    level = "*ERROR*";
                    break;
                default:
                    level = "*" + logLevel + "*";
                    break;
            }

            if ( message == null && t != null )
            {
                message = t.getMessage();
            }

            System.out.println( level + " " + message );
            if ( t != null )
            {
                t.printStackTrace( System.out );
            }
        }
    }
}
