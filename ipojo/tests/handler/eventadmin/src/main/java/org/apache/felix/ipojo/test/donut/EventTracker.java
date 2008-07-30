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

import org.osgi.service.event.Event;

/**
 * Specification of an event tracker.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface EventTracker {

    /**
     * Clear the received events list. (Useful before tests)
     */
    void clearEvents();

    /**
     * Get the first received event and remove it from the events list.
     * 
     * @return the first received event or null if no event is available
     */
    Event getEvent();

    /**
     * Get the whole list of received events.
     * 
     * @return the array containing all received events
     */
    Event[] getAllEvents();

    /**
     * Get the first event if available or wait for an incoming event. The
     * returned event is removed from the eaten event list.
     * 
     * @return the first available event.
     */
    Event waitForEvent();

    /**
     * Return the size of the events list.
     * 
     * @return the size of the events list
     */
    int getNumberOfEvents();
}
