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

import org.osgi.service.log.LogEntry;

/**
 * Implementation of the {@link Enumeration} interface for a linked list of
 * {@link LogNode} entries.
 */
final class LogNodeEnumeration implements Enumeration
{
    /** The next node. */
    private LogNode m_next;
    /** The last node. */
    private final LogNode m_last;

    /**
     * Creates a new instance.
     * @param start the first node to return
     * @param end the last node to return
     */
    LogNodeEnumeration(final LogNode start, final LogNode end)
    {
        m_next = start;
        m_last = end;
    }

    /**
     * Determines whether there are any more elements to return.
     * @return <code>true</code> if there are more elements; <code>false</code> otherwise
     */
    public boolean hasMoreElements()
    {
        return m_next != null;
    }

    /**
     * Returns the current element and moves onto the next element.
     * @return the current element
     */
    public Object nextElement()
    {
        LogEntry result = null;

        if (m_next == m_last)
        {
            result = m_next.getEntry();
            m_next = null;
        }
        else if (m_next != null)
        {
            result = m_next.getEntry();
            m_next = m_next.getNextNode();
        }

        return result;
    }
}