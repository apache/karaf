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
import org.osgi.framework.BundleEvent;

public class BundleEventConverter {

    public static final String TOPIC = BundleEvent.class.getName().replace('.', '/');

    public static EventInfo getInfo(final BundleEvent event) {
        if ( event == null )
        {
            return null;
        }
        final StringBuffer topic = new StringBuffer(TOPIC);
        topic.append('/');
        final StringBuffer buffer = new StringBuffer( "Bundle " );
        buffer.append( event.getBundle().getSymbolicName() );
        buffer.append( ' ' );
        switch ( event.getType() )
        {
            case BundleEvent.INSTALLED:
                buffer.append( "installed" );
                topic.append("INSTALLED");
                break;
            case BundleEvent.RESOLVED:
                buffer.append( "resolved" );
                topic.append("RESOLVED");
                break;
            case BundleEvent.STARTED:
                buffer.append( "started" );
                topic.append("STARTED");
                break;
            case BundleEvent.STARTING:
                buffer.append( "starting" );
                topic.append("STARTED");
                break;
            case BundleEvent.STOPPED:
                buffer.append( "stopped" );
                topic.append("STOPPED");
                break;
            case BundleEvent.STOPPING:
                buffer.append( "stopping" );
                topic.append("STOPPING");
                break;
            case BundleEvent.UNINSTALLED:
                buffer.append( "uninstalled" );
                topic.append("UNINSTALLED");
                break;
            case BundleEvent.UNRESOLVED:
                buffer.append( "unresolved" );
                topic.append("UNINSTALLED");
                break;
            case BundleEvent.UPDATED:
                buffer.append( "updated" );
                topic.append("UPDATED");
                break;
            default:
                return null; // IGNORE
        }

        return new EventInfo(topic.toString(), buffer.toString(), "bundle");
    }
}
