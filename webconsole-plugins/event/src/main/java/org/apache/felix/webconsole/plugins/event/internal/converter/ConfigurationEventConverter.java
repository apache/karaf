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
import org.osgi.service.cm.ConfigurationEvent;

public class ConfigurationEventConverter {

    public static final String TOPIC = ConfigurationEventConverter.class.getName().replace('.', '/');

    public static EventInfo getInfo(final ConfigurationEvent event) {
        if ( event == null )
        {
            return null;
        }

        final StringBuffer topic = new StringBuffer(TOPIC);
        topic.append('/');
        final StringBuffer buffer = new StringBuffer( "Configuration event: " );
        buffer.append(event.getPid());
        if ( event.getFactoryPid() != null )
        {
            buffer.append(" (factoryPid=");
            buffer.append(event.getFactoryPid());
            buffer.append(")");
        }
        switch ( event.getType() )
        {
            case ConfigurationEvent.CM_DELETED:
                buffer.append( " deleted" );
                topic.append( "CM_DELETED" );
                break;
            case ConfigurationEvent.CM_UPDATED:
                buffer.append( " changed" );
                topic.append( "CM_UPDATED" );
                break;
            default:
                return null; // IGNORE
        }

        return new EventInfo(topic.toString(), buffer.toString(), "config");
    }
}
