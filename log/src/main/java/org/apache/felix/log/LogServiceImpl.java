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
package org.apache.felix.log;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;

/**
 * Implementation of the OSGi {@link LogService}.
 */
final class LogServiceImpl implements LogService
{
    /** The log implementation. */
    private final Log m_log;
    /** The bundle associated with this implementation. */
    private final Bundle m_bundle;

    /**
     * Create a new instance.
     * @param log the log implementation
     * @param bundle the bundle associated with this implementation
     */
    LogServiceImpl(final Log log, final Bundle bundle)
    {
        this.m_log = log;
        this.m_bundle = bundle;
    }

    /**
     * Log the specified message at the specified level.
     * @param level the level to log the message at
     * @param message the message to log
     */
    public void log(final int level, final String message)
    {
        log(null, level, message, null);
    }

    /**
     * Log the specified message along with the specified exception at the
     * specified level.
     * @param level the level to log the message and exception at
     * @param message the message to log
     * @param exception the exception to log
     */
    public void log(final int level,
        final String message,
        final Throwable exception)
    {
        log(null, level, message, exception);
    }

    /**
     * Log the specified message along with the speicified service reference
     * at the specified level.
     * @param sr the service reference of the service that produced the message
     * @param level the level to log the message at
     * @param message the message to log
     */
    public void log(final ServiceReference sr,
        final int level,
        final String message)
    {
        log(sr, level, message, null);
    }

    /**
     * Log the specified message along with the specified exception and
     * service reference at the specified level.
     * @param sr the service reference of the service that produced the message
     * @param level the level to log the message at
     * @param message the message to log
     * @param exception the exception to log
     */
    public void log(final ServiceReference sr,
        final int level,
        final String message,
        final Throwable exception)
    {
        m_log.addEntry(new LogEntryImpl((sr != null) ? sr.getBundle() : m_bundle,
            sr,
            level,
            message,
            exception));
    }
}