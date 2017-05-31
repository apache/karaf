/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.log.core.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

/**
 * A list that only keep the last N elements added
 */
public class LruList implements PaxAppender {

    private PaxLoggingEvent[] elements;
    private transient int start = 0;
    private transient int end = 0;
    private transient boolean full = false;
    private final int maxElements;
    private final List<PaxAppender> appenders;

    public LruList(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("The size must be greater than 0");
        }
        elements = new PaxLoggingEvent[size];
        maxElements = elements.length;
        appenders = new ArrayList<>();
    }

    public synchronized int size() {
        int size = 0;
        if (end < start) {
            size = maxElements - start + end;
        } else if (end == start) {
            size = (full ? maxElements : 0);
        } else {
            size = end - start;
        }
        return size;
    }

    public synchronized void clear() {
        start = 0;
        end = 0;
        full = false;
        elements = new PaxLoggingEvent[maxElements];
    }

    public synchronized void add(PaxLoggingEvent element) {
        if (null == element) {
             throw new NullPointerException("Attempted to add null object to buffer");
        }
        if (size() == maxElements) {
            Object e = elements[start];
            if (null != e) {
                elements[start++] = null;
                if (start >= maxElements) {
                    start = 0;
                }
                full = false;
            }
        }
        elements[end++] = new KarafLogEvent(element);
        if (end >= maxElements) {
            end = 0;
        }
        if (end == start) {
            full = true;
        }
        for (PaxAppender appender : appenders) {
            try {
                appender.doAppend(element);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

    public synchronized Iterable<PaxLoggingEvent> getElements() {
        return getElements(size());
    }

    public synchronized Iterable<PaxLoggingEvent> getElements(int nb) {
        int s = size();
        nb = Math.min(Math.max(0, nb), s);
        PaxLoggingEvent[] e = new PaxLoggingEvent[nb];
        for (int i = 0; i < nb; i++) {
            e[i] = elements[(i + s - nb + start) % maxElements];
        }
        return Arrays.asList(e);
    }

    public synchronized void addAppender(PaxAppender appender) {
        this.appenders.add(appender);
    }

    public synchronized void removeAppender(PaxAppender appender) {
        this.appenders.remove(appender);
    }
    
    public void doAppend(PaxLoggingEvent event) {
        event.getProperties(); // ensure MDC properties are copied
        add(event);
    }

}
