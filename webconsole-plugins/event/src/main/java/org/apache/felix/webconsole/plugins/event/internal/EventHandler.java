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


import java.util.HashMap;
import java.util.Map;

import org.apache.felix.webconsole.plugins.event.internal.converter.*;
import org.osgi.service.event.Event;

/**
 * Event listener for the OSGi event admin, listening
 * to all events.
 */
public class EventHandler implements org.osgi.service.event.EventHandler
{
    /** A list of topics that are always excluded, there is no way to change this
     * via configuration.
     * This list includes the framework events that are already covered by the
     * event listener and all log events.
     */
    private final String[] excludeTopics = new String[] {"org/osgi/service/log",
            BundleEventConverter.TOPIC,
            FrameworkEventConverter.TOPIC,
            ServiceEventConverter.TOPIC};

    private final EventCollector collector;

    public EventHandler(final EventCollector c)
    {
        this.collector = c;
    }

    /**
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent( Event event )
    {
        boolean include = true;
        for(int i=0; i<excludeTopics.length; i++)
        {
            if ( event.getTopic().startsWith(excludeTopics[i]))
            {
                include = false;
                break;
            }
        }
        if ( include )
        {
            Map props = null;
            final String[] names = event.getPropertyNames();
            if ( names != null && names.length > 0 )
            {
                props = new HashMap();
                for ( int i = 0; i < names.length; i++ )
                {
                    props.put(names[i], event.getProperty(names[i]));
                }
            }
            collector.add(new EventInfo(event.getTopic(), null, null, props));
        }
    }
}
