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
package org.apache.felix.ipojo.handlers.event.publisher;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.event.Event;

/**
 * The PublisherImpl class is the implementation of the Publisher object used by
 * components to send events.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PublisherImpl implements Publisher {

    /**
     * The key where the component instance name is stored.
     */
    public static final String INSTANCE_NAME_PROPERTY = "publisher.instance.name";

    /**
     * The managing handler.
     */
    private EventAdminPublisherHandler m_handler;

    /**
     * The topics of sent events.
     */
    private final String[] m_topics;

    /**
     * The sending mode of events.
     */
    private final boolean m_synchronous;

    /**
     * The key, in the content of the event, where user data are stored.
     */
    private final String m_dataKey;

    /**
     * The name of the component instance using this publisher.
     */
    private final String m_instanceName;

    /**
     * Constructs an Publisher with given parameters.
     * 
     * @param handler the handler that will manage this publisher
     * @param topics the topics on which events are sent
     * @param synchronous the sending mode of events
     * @param dataKey The key, in the content of the event, where user data are
     *            stored (may be {@code null})
     * @param instanceName the name of the instance creating this publisher.
     */
    public PublisherImpl(EventAdminPublisherHandler handler, String[] topics,
            boolean synchronous, String dataKey, String instanceName) {

        // Initialize the publisher's fields
        m_handler = handler;
        m_topics = topics;
        m_synchronous = synchronous;
        m_dataKey = dataKey;
        m_instanceName = instanceName;
    }

    /**
     * Sends an event with the specified content.
     * 
     * @param content the content of the event
     */
    public void send(Dictionary content) {
        // Add instance information in the event
        content.put(INSTANCE_NAME_PROPERTY, m_instanceName);
        // We sent the event on each topic
        for (int i = 0; i < m_topics.length; i++) {
            // Create an event with the given topic and content
            Event e = new Event(m_topics[i], content);
            // Send the event, depending on the sending mode
            if (!m_synchronous) {
                m_handler.getEventAdminService().postEvent(e); // Asynchronous
            } else {
                m_handler.getEventAdminService().sendEvent(e); // Synchronous
            }
        }
    }

    /**
     * Sends a data event.
     * 
     * @param object the data to send
     */
    public void sendData(Object object) {
        // Construct the content of the event with the given object
        Dictionary content = new Hashtable();
        content.put(m_dataKey, object);
        send(content);
    }
}
