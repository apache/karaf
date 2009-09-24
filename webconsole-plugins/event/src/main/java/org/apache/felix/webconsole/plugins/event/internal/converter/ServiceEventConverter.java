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
import org.osgi.framework.*;

public class ServiceEventConverter {

    public static final String TOPIC = ServiceEvent.class.getName().replace('.', '/');

    public static EventInfo getInfo(final ServiceEvent event) {
        if ( event == null )
        {
            return null;
        }

        final ServiceReference ref = event.getServiceReference();

        final StringBuffer buffer = new StringBuffer( "Service " );
        final Object pid = ref.getProperty( Constants.SERVICE_PID );
        if ( pid != null)
        {
            buffer.append( pid );
        }
        buffer.append( "(id=" );
        buffer.append( ref.getProperty( Constants.SERVICE_ID ) );

        final String[] arr = (String[]) ref.getProperty( Constants.OBJECTCLASS );
        if ( arr != null )
        {
            buffer.append(", objectClass=");
            if ( arr.length > 1 )
            {
                buffer.append('[');
            }
            for(int m = 0; m <arr.length; m++)
            {
                if ( m > 0 )
                {
                    buffer.append(", ");
                }
                buffer.append(arr[m].toString());
            }
            if ( arr.length > 1 )
            {
                buffer.append(']');
            }
        }
        buffer.append( ", bundle=" );
        buffer.append( ref.getBundle().getSymbolicName() );
        buffer.append( ") " );
        final StringBuffer topic = new StringBuffer(TOPIC);
        topic.append('/');
        switch ( event.getType() )
        {
            case ServiceEvent.REGISTERED:
                buffer.append( "registered" );
                topic.append("REGISTERED");
                break;
            case ServiceEvent.MODIFIED:
                buffer.append( "modified" );
                topic.append("MODIFIED");
                break;
            case ServiceEvent.UNREGISTERING:
                buffer.append( "unregistering" );
                topic.append("UNREGISTERING");
                break;
            default:
                return null; // IGNORE
        }

        return new EventInfo(topic.toString(), buffer.toString(), "service");
    }
}
