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
package org.apache.karaf.services.logging;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;

public class LogReaderServiceImpl implements LogReaderService {

    private static final int MAX_ENTRIES = 500;

    private final List<LogListener> listeners = new CopyOnWriteArrayList<>();
    private final Deque<LogEntry> entries = new ArrayDeque<>(MAX_ENTRIES);
    private final AtomicLong sequenceGenerator = new AtomicLong(0);

    @Override
    public void addLogListener(LogListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLogListener(LogListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public Enumeration<LogEntry> getLog() {
        List<LogEntry> snapshot;
        synchronized (entries) {
            snapshot = new ArrayList<>(entries);
        }
        return Collections.enumeration(snapshot);
    }

    public long nextSequence() {
        return sequenceGenerator.incrementAndGet();
    }

    public void fire(LogEntry entry) {
        synchronized (entries) {
            if (entries.size() >= MAX_ENTRIES) {
                entries.removeFirst();
            }
            entries.addLast(entry);
        }
        for (LogListener listener : listeners) {
            try {
                listener.logged(entry);
            } catch (Throwable t) {
                // Ignore listener exceptions as per OSGi spec
            }
        }
    }
}
