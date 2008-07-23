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
package org.apache.felix.ipojo.handlers.event.subscriber;

import java.util.Collection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.event.EventUtil;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Event Subscriber Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminSubscriberHandler extends PrimitiveHandler implements
        EventHandler {

    /**
     * Handler Namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handlers.event.EventAdminHandler";

    // Names of instance configuration properties.

    /**
     * The event's topics instance configuration property.
     */
    public static final String TOPICS_PROPERTY = "event.topics";

    /**
     * The event's filter instance configuration property.
     */
    public static final String FILTER_PROPERTY = "event.filter";

    /**
     * Prefix for logged messages.
     */
    private static final String LOG_PREFIX = "EVENT ADMIN SUBSCRIBER HANDLER : ";

    /**
     * Instance Manager.
     */
    private InstanceManager m_manager;

    /**
     * List of subscriber accessible by name.
     */
    private Map m_subscribersByName = new HashMap();

    /**
     * List of callbacks accessible by subscribers' names.
     */
    private Map m_callbacks = new Hashtable();

    /**
     * iPOJO Properties representing all the topics.
     */
    private String[] m_topics;

    /**
     * Initialize the component type.
     * 
     * @param cd :
     *            component type description to populate.
     * @param metadata :
     *            component type metadata.
     * @throws ConfigurationException :
     *             metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription,
     *      org.apache.felix.ipojo.metadata.Element)
     */
    // @Override
    public void initializeComponentFactory(ComponentTypeDescription cd,
            Element metadata)
        throws ConfigurationException {

        // Update the current component description
        Dictionary dict = new Properties();
        cd.addProperty(new PropertyDescription(TOPICS_PROPERTY,
                Dictionary.class.getName(), dict.toString()));
        dict = new Properties();
        cd.addProperty(new PropertyDescription(FILTER_PROPERTY,
                Dictionary.class.getName(), dict.toString()));
    }

    /**
     * Constructor.
     * 
     * @param metadata :
     *            component type metadata
     * @param conf :
     *            instance configuration
     * @throws ConfigurationException :
     *             one event subscription is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager,
     *      org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    // @Override
    public void configure(Element metadata, Dictionary conf)
        throws ConfigurationException {

        // Store the component manager
        m_manager = getInstanceManager();

        // Get Metadata subscribers
        Element[] subscribers = metadata.getElements("subscriber", NAMESPACE);

        if (subscribers != null) {

            // then check subscribers are well formed and fill the subscriber'
            // map
            for (int i = 0; i < subscribers.length; i++) {

                try {
                    // Extract the subscriber configuration
                    EventAdminSubscriberMetadata subscriberMetadata = new EventAdminSubscriberMetadata(
                            m_manager, subscribers[i], conf);
                    String subscriberName = subscriberMetadata.getName();
                    info(LOG_PREFIX + "configuring subscriber "
                            + subscriberName);

                    // Determine the callback prototype
                    PojoMetadata pojoMetadata = getFactory().getPojoMetadata();
                    String callbackType;
                    if (subscriberMetadata.getDataKey() == null) {
                        callbackType = Event.class.getName();
                    } else {
                        callbackType = subscriberMetadata.getDataType()
                                .getName();
                    }

                    // Find the specified callback
                    MethodMetadata methodMetadata = pojoMetadata.getMethod(
                            subscriberMetadata.getCallback(),
                            new String[] { callbackType });
                    if (methodMetadata == null) {
                        throw new ConfigurationException(
                                "Unable to find callback "
                                        + subscriberMetadata.getCallback()
                                        + "(" + callbackType + ")");
                    }
                    Callback callback = new Callback(methodMetadata, m_manager);

                    // Add the subscriber to the subscriber list and
                    // register callback
                    Object old;
                    if ((old = m_subscribersByName.put(subscriberName,
                            subscriberMetadata)) != null) {
                        m_subscribersByName.put(subscriberName, old);
                        throw new ConfigurationException("The subscriber "
                                + subscriberName + "already exists");
                    }
                    m_callbacks.put(subscriberName, callback);

                } catch (Exception e) {
                    // Ignore invalid subscribers
                    warn(LOG_PREFIX
                            + "Ignoring subscriber : Error in configuration", e);
                }
            }
        } else {
            info(LOG_PREFIX + "no subscriber detected !");
        }
    }

    /**
     * Handler start method.
     * 
     * @see org.apache.felix.ipojo.Handler#start()
     */
    // @Override
    public void start() {

        Set topics = new TreeSet();

        // Build the topic to listen
        // Topics is a merge of all required topics by subscribers
        if (!m_subscribersByName.isEmpty()) {
            Collection subscribers = m_subscribersByName.values();
            for (Iterator i = subscribers.iterator(); i.hasNext();) {
                String[] subTopics = ((EventAdminSubscriberMetadata) i.next())
                        .getTopics();
                for (int j = 0; j < subTopics.length; j++) {
                    topics.add(subTopics[j]);
                }
            }
        }

        m_topics = new String[topics.size()];
        int i = 0;
        for (Iterator iterator = topics.iterator(); iterator.hasNext();) {
            String tmp = (String) iterator.next();
            m_topics[i++] = tmp;
        }
    }

    /**
     * Handler stop method.
     * 
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    // @Override
    public void stop() {
    }

    /***************************************************************************
     * OSGi EventHandler callback
     **************************************************************************/

    /**
     * Receive an event. The event is dispatch to attached subscribers.
     * 
     * @param event :
     *            the received event.
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {

        // Retrieve the event's topic
        String topic = event.getTopic();

        // For each subscribers
        Collection subscribers = m_subscribersByName.values();
        for (Iterator i = subscribers.iterator(); i.hasNext();) {
            EventAdminSubscriberMetadata subscriberMetadata = (EventAdminSubscriberMetadata) i
                    .next();

            synchronized (this) {
                // Check if the subscriber's topic and filter match
                Filter filter = subscriberMetadata.getFilter();

                if (EventUtil.matches(topic, subscriberMetadata.getTopics())
                        && (filter == null || event.matches(filter))) {

                    String name = subscriberMetadata.getName();
                    String dataKey = subscriberMetadata.getDataKey();
                    Callback callback = (Callback) m_callbacks.get(name);
                    Object callbackParam;

                    try {
                        // Depending on the subscriber type...
                        if (dataKey == null) {

                            // Generic event subscriber : pass the event to the
                            // registered
                            // callback
                            callbackParam = event;

                        } else {

                            // Check for a data key in the event
                            boolean dataKeyPresent = false;
                            String[] properties = event.getPropertyNames();
                            for (int j = 0; j < properties.length
                                    && !dataKeyPresent; j++) {
                                if (dataKey.equals(properties[j])) {
                                    dataKeyPresent = true;
                                }
                            }

                            if (dataKeyPresent) {
                                // Data event : check type compatibility and
                                // pass the given object to the registered
                                // callback
                                Object data = event.getProperty(dataKey);
                                Class dataType = subscriberMetadata
                                        .getDataType();
                                Class dataClazz = data.getClass();
                                if (dataType.isAssignableFrom(dataClazz)) {
                                    callbackParam = data;
                                } else {
                                    throw new ClassCastException(
                                            "Cannot convert "
                                                    + dataClazz.getName()
                                                    + " to "
                                                    + dataType.getName());
                                }

                            } else {
                                throw new java.lang.NoSuchFieldException(
                                        dataKey);
                            }
                        }

                        // Run the callback
                        callback.call(new Object[] { callbackParam });

                    } catch (ClassCastException e) {
                        // Ignore the data event if type doesn't match
                        warn(
                                LOG_PREFIX
                                        + "Ignoring data event : Bad data type",
                                e);
                    } catch (NoSuchFieldException e) {
                        // Ignore events without data field for data events
                        // subscriber
                        warn(LOG_PREFIX + "Ignoring data event : No data", e);
                    } catch (Exception e) {
                        // Unexpected exception
                        error(LOG_PREFIX
                                + "Unexpected exception when calling callback",
                                e);
                    }
                }
            }
        }
    }
}
