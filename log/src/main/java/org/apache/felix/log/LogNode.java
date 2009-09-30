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

import org.osgi.service.log.LogEntry;

/**
 * The class used as a doubly linked list node in the log.
 */
final class LogNode
{
    /** The previous node. */
    private LogNode m_previous;
    /** The next node. */
    private LogNode m_next;
    /** The log entry. */
    private final LogEntry m_entry;

    /**
     * Create a new instance.
     * @param entry the log entry.
     */
    LogNode(final LogEntry entry)
    {
        m_entry = entry;
    }

    /**
     * Returns the associated entry.
     * @return the associated entry
     */
    LogEntry getEntry()
    {
        return m_entry;
    }

    /**
     * Get the next node.
     * @return the next node
     */
    LogNode getNextNode()
    {
        return m_next;
    }

    /**
     * Set the next node.
     * @param next the next node
     */
    void setNextNode(final LogNode next)
    {
        m_next = next;
    }

    /**
     * Get the previous node.
     * @return the previous node
     */
    LogNode getPreviousNode()
    {
        return m_previous;
    }

    /**
     * Set the previous node.
     * @param previous the previous node
     */
    void setPreviousNode(final LogNode previous)
    {
        m_previous = previous;
    }
}