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

import java.util.*;

/**
 * This class collects events
 */
public class EventCollector
{

    private static final String PROPERTY_MAX_SIZE = "max.size";
    private static final int DEFAULT_MAX_SIZE = 250;

    private List eventInfos;

    private long startTime;

    private int maxSize;

    public EventCollector(final Dictionary props)
    {
        this.clear();
        this.updateConfiguration(props);
    }

    public void add(final EventInfo info)
    {
        if ( info != null )
        {
            synchronized ( this )
            {
                this.eventInfos.add( info );
                if ( eventInfos.size() > this.maxSize )
                {
                    eventInfos.remove( 0 );
                }
                if ( this.eventInfos.size() == 1 )
                {
                    this.startTime = info.received;
                }
            }
        }
    }

    public void clear()
    {
        synchronized ( this )
        {
            this.eventInfos = new ArrayList();
            this.startTime = System.currentTimeMillis();
        }
    }

    /**
     * Return a copy of the current event list
     */
    public List getEvents()
    {
        synchronized ( this )
        {
            return new ArrayList(eventInfos);
        }
    }

    public void updateConfiguration( final Dictionary props)
    {
        this.maxSize = OsgiUtil.toInteger(props, PROPERTY_MAX_SIZE, DEFAULT_MAX_SIZE);
        synchronized ( this )
        {
            while ( eventInfos.size() > this.maxSize )
            {
                eventInfos.remove( 0 );
            }

        }
    }

    public long getStartTime()
    {
        return this.startTime;
    }
}
