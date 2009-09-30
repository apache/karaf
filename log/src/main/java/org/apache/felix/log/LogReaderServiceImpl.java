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

import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

/**
 * Implementation of the OSGi {@link LogReaderService} interface.  See section 101
 * of the OSGi service compendium.
 * <p>
 * The {@link LogReaderService} maintains a list of {@link org.osgi.service.log.LogEntry}
 * objects called the <i>log</i>.  The {@link LogReaderService} is a service that bundle
 * developers can use to retrieve information contained in this log, and receive
 * notifications about {@link org.osgi.service.log.LogEntry} objects when they are created
 * through the {@link org.osgi.service.log.LogService}.
 */
final class LogReaderServiceImpl implements LogReaderService
{
    /** The log implementation. */
    private final Log m_log;
    /** The listeners associated with this service. */
    private final List m_listeners = new Vector();

    /**
     * Create a new instance.
     * @param log the log implementation
     */
    LogReaderServiceImpl(final Log log)
    {
        this.m_log = log;
    }

    /**
     * This method is used to subscribe to the Log Reader Service in order to receive
     * log messages as they occur.  Unlike the previously recorded log entries, all
     * log messages must be sent to subscribers of the Log Reader Service as they are
     * recorded.
     * <p>
     * A subscriber to the Log Reader Service must implement the {@link LogListener}
     * interface.
     * <p>
     * After a subscription of the Log Reader Service has been started, the subscriber's
     * {@link LogListener#logged(LogEntry)} method must be called with a {@link LogEntry}
     * object for the message each time a message is logged.
     * @param listener the listener object to subscribe
     */
    public synchronized void addLogListener(final LogListener listener)
    {
        m_listeners.add(listener);
        m_log.addListener(listener);
    }

    /**
     * This method is used to unsubscribe from the Log Reader Service.
     * @param listener the listener object to unsubscribe
     */
    public synchronized void removeLogListener(final LogListener listener)
    {
        m_listeners.remove(listener);
        m_log.removeListener(listener);
    }

    /**
     * This method retrieves past log entries as an enumeration with the most recent
     * entry first.
     * @return an enumeration of the {@link LogEntry} objects that have been stored
     */
    public Enumeration getLog()
    {
        return m_log.getEntries();
    }

    /**
     * Remove all log listeners registered through this service.
     */
    synchronized void removeAllLogListeners()
    {
        Iterator listenerIt = m_listeners.iterator();
        while (listenerIt.hasNext())
        {
            LogListener listener = (LogListener) listenerIt.next();
            m_log.removeListener(listener);
        }
    }
}