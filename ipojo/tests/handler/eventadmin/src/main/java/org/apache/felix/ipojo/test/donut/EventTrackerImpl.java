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
package org.apache.felix.ipojo.test.donut;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.test.util.EahTestUtils;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Implementation of an event tracker.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventTrackerImpl implements EventTracker, EventHandler {

    /**
     * The name of the event tracker.
     */
    private String m_name;

    /**
     * The list of received events
     */
    private List m_events = new ArrayList();

    /**
     * Event receiver callback. This method is called by the event admin service
     * when a event is received.
     * 
     * @param event
     *            the received event
     */
    public void handleEvent(Event event) {
        synchronized (m_events) {
            m_events.add(event);
            m_events.notify();
            if (EahTestUtils.TRACE) {
                System.err.println("[" + this.getClass().getSimpleName() + ":"
                        + m_name + "] Event received : " + event);
            }
        }
    }

    /**
     * Clear the received events list. (Useful before tests)
     */
    public void clearEvents() {
        synchronized (m_events) {
            m_events.clear();
        }
    }

    /**
     * Get the first received event and remove it from the events list.
     * 
     * @return the first received event or null if no event is available
     */
    public Event getEvent() {
        Event event = null;
        synchronized (m_events) {
            if (!m_events.isEmpty()) {
                event = (Event) m_events.remove(0);
            }
        }
        return event;
    }

    /**
     * Get the whole list of received events.
     * 
     * @return the array containing all received events
     */
    public Event[] getAllEvents() {
        Event[] events = new Event[0];
        synchronized (m_events) {
            events = (Event[]) m_events.toArray(events);
            m_events.clear();
        }
        return events;
    }

    /**
     * Get the first event if available or wait for an incoming event. The
     * returned event is removed from the eaten event list.
     * 
     * @return the first available event.
     */
    public Event waitForEvent() {
        Event event = null;
        synchronized (m_events) {
            while (event == null) {
                if (m_events.isEmpty()) {
                    try {
                        m_events.wait();
                    } catch (InterruptedException e) {
                        // Thanks Checkstyle to forbid empty catch statements
                        // ;-(
                    }
                } else {
                    event = (Event) m_events.remove(0);
                }
            }
        }
        return event;
    }

    /**
     * Return the size of the events list.
     * 
     * @return the size of the events list
     */
    public int getNumberOfEvents() {
        int length;
        synchronized (m_events) {
            length = m_events.size();
        }
        return length;
    }

    /**
     * Return the string representation of a given event.
     * 
     * @return the string representation of a given event
     */
    public static String eventToString(Event e) {
        StringBuilder buf = new StringBuilder();
        buf.append("[" + e.getTopic() + "] {");

        String[] properties = e.getPropertyNames();
        int n = properties.length - 1;
        for (int i = 0; i <= n; i++) {
            String name = properties[i];
            buf.append(name + "=" + e.getProperty(name));
            if (i != n)
                buf.append(", ");
        }
        buf.append("}");
        return buf.toString();
    }
}
