/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.plugins.event.internal;

import java.util.Map;

/**
 * An event as displayed in the console.
 * The console displays the time {@link #received},
 * the {@link #topic} and if available the {@link #info}.
 * If the info is not available, the {@link #properties}
 * are displayed instead.
 */
public class EventInfo {

    /** The event topic. */
    public final String topic;

    /** Additional information for this event. */
    public final String info;

    /** The time this event has been received. */
    public final long received;

    /** Properties. */
    public final Map properties;

    /** The event class. */
    public final String category;

    public EventInfo( final String topic, final String info, final String category )
    {
        this.topic = topic;
        this.info = info;
        this.received = System.currentTimeMillis();
        this.properties = null;
        this.category = category;
    }

    public EventInfo( final String topic, final String info, final String category, final Map props )
    {
        this.topic = topic;
        this.info = info;
        this.received = System.currentTimeMillis();
        this.properties = props;
        this.category = category;
    }
}
