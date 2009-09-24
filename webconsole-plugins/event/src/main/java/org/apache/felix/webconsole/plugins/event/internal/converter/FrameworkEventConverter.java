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
package org.apache.felix.webconsole.plugins.event.internal.converter;

import org.apache.felix.webconsole.plugins.event.internal.EventInfo;
import org.osgi.framework.FrameworkEvent;

public class FrameworkEventConverter {

    public static final String TOPIC = FrameworkEvent.class.getName().replace('.', '/');

    public static EventInfo getInfo(final FrameworkEvent event) {
        if ( event == null )
        {
            return null;
        }

        final StringBuffer topic = new StringBuffer(TOPIC);
        topic.append('/');
        final StringBuffer buffer = new StringBuffer( "Framework event " );
        switch ( event.getType() )
        {
            case FrameworkEvent.ERROR:
                buffer.append( "error" );
                topic.append( "ERROR" );
                break;
            case FrameworkEvent.INFO:
                buffer.append( "info" );
                topic.append( "INFO" );
                break;
            case FrameworkEvent.PACKAGES_REFRESHED:
                buffer.append( "packages refreshed" );
                topic.append( "PACKAGES_REFRESHED" );
                break;
            case FrameworkEvent.STARTED:
                buffer.append( "started" );
                topic.append( "STARTED" );
                break;
            case FrameworkEvent.STARTLEVEL_CHANGED:
                buffer.append( "start level changed" );
                topic.append( "STARTLEVEL_CHANGED" );
                break;
            case FrameworkEvent.WARNING:
                buffer.append( "warning" );
                topic.append( "WARNING" );
                break;
            default:
                return null; // IGNORE
        }

        return new EventInfo(topic.toString(), buffer.toString(), "framework");
    }
}
