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
package org.apache.felix.utils.log;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

import java.io.PrintStream;

/**
 * Internal logger to be used in order to avoid a mandatory dependency on OSGi LogService.
 * It first tries to log to a log service implementation if there is one available and then fallback to System out/err
 * in case there is no log service available.
 */
public class Logger
{
    public static final int LOG_ERROR = 1;
    public static final int LOG_WARNING = 2;
    public static final int LOG_INFO = 3;
    public static final int LOG_DEBUG = 4;

    /**
     * Bundle context.
     */
    private final BundleContext m_context;
    private boolean m_isLogClassPresent;

    /**
     * Constructor.
     *
     * @param context bundle context
     */
    public Logger(BundleContext context)
    {
        m_context = context;
        try
        {
            org.osgi.service.log.LogService.class.getName();
            m_isLogClassPresent = true;
        }
        catch (NoClassDefFoundError ex)
        {
            m_isLogClassPresent = false;
        }
    }

    /**
     * @see LogService#log(int, String)
     */
    public void log(int level, String message)
    {
        log(level, message, null);
    }

    /**
     * @see LogService#log(int, String, Throwable)
     */
    public void log(int level, String message, Throwable exception)
    {
        if (!m_isLogClassPresent || !_log(level, message, exception))
        {
            final PrintStream stream = getStream(level);
            stream.println(message);
            if (exception != null)
            {
                exception.printStackTrace(stream);
            }

        }
    }

    /**
     * Lookup the OSGi LogService and if available use it.
     */
    private boolean _log(int level, String message, Throwable exception)
    {
        try
        {
            ServiceReference reference = null;
            reference = m_context.getServiceReference(LogService.class.getName());
            if (reference != null)
            {
                final LogService logService = (LogService) m_context.getService(reference);
                if (logService != null)
                {
                    logService.log(level, message, exception);
                    m_context.ungetService(reference);
                    return true;
                }
            }
        }
        catch (NoClassDefFoundError e)
        {
            //ignore
        }
        return false;
    }

    /**
     * Return the standard print streams to use depending on log level.
     *
     * @param level log level
     * @return print stream corresponding to log level
     */
    private PrintStream getStream(int level)
    {
        switch (level)
        {
            case LOG_ERROR:
                System.err.print("ERROR: ");
                return System.err;
            case LOG_WARNING:
                System.err.print("WARNING: ");
                return System.err;
            case LOG_INFO:
                System.out.print("INFO: ");
                return System.out;
            case LOG_DEBUG:
                System.out.print("DEBUG: ");
                return System.out;
            default:
                System.out.print("UNKNOWN: ");
                return System.out;
        }
    }
}