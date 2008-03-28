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
package org.apache.felix.ipojo.handler.event;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/** 
 * Event Subscriber Handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminSubscriberHandler extends PrimitiveHandler implements EventHandler {

    /**
     * Handler Namespace. 
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handler.event.EventAdminSubscriberHandler";

    /**
     * Instance Manager.
     */
    private InstanceManager m_manager;

    /**
     * List of subscriber.
     */
    private List m_subEvent = new ArrayList();

    /**
     * iPOJO Properties representing all the topics.
     */
    private String[] m_topics;

    /**
     * Initialize the component type.
     * @param cd : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription cd, Element metadata) throws ConfigurationException {
        // Update the current component description
        Dictionary dict = new Properties();
        cd.addProperty(new PropertyDescription("event.topics", Dictionary.class.getName(), dict.toString()));

        // Update the current component description
        dict = new Properties();
        cd.addProperty(new PropertyDescription("event.filter", Dictionary.class.getName(), dict.toString()));

    }

    /**
     * Constructor. 
     * @param metadata : component type metadata
     * @param conf : instance configuration
     * @throws ConfigurationException : one event subscription is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {

        // Store the component manager
        m_manager = getInstanceManager();
        PojoMetadata mm = getFactory().getPojoMetadata();

        // Get Metadata subscribers
        Element[] subscribers = metadata.getElements("subscriber", NAMESPACE);
        if (subscribers != null) {
            // then check publishers are well formed and fill the publishers'
            // map
            for (int i = 0; i < subscribers.length; i++) {
                EventAdminSubscriberMetadata eventAdminSubscriberMetadata = new EventAdminSubscriberMetadata(m_manager, subscribers[i]);

                if (eventAdminSubscriberMetadata.getCallbackStr() != null) {
                    if (mm.getMethod(eventAdminSubscriberMetadata.getCallbackStr(), new String[] {Event.class.getName()}) != null) {
                        MethodMetadata methodMetadata = mm.getMethod(eventAdminSubscriberMetadata.getCallbackStr(), new String[] {Event.class.getName()});
                        Callback cb = new Callback(methodMetadata, m_manager);
                        eventAdminSubscriberMetadata.setCallback(cb);
                    } else {
                        warn(" EVENT HANDLER SUBSCRIBERS : malformed subscriber : the method " + eventAdminSubscriberMetadata.getCallbackStr() + "(Event myEvent) is not present in the component");
                        throw new ConfigurationException("EVENT HANDLER SUBSCRIBERS : malformed subscriber : the method " + eventAdminSubscriberMetadata.getCallbackStr() + "(Event myEvent) is not present in the component");
                    }
                    m_subEvent.add(eventAdminSubscriberMetadata);
                } else {
                    warn(" EVENT HANDLER SUBSCRIBERS : malformed subscriber !");
                    throw new ConfigurationException("EVENT HANDLER SUBSCRIBERS : malformed subscriber !");
                }
            }
        } else {
            error(" EVENT HANDLER SUBSCRIBERS : no Suscribers detected !");
            throw new ConfigurationException(" EVENT HANDLER SUBSCRIBERS : no Suscribers detected !");
        }

        // if well formed publishers or subscribers found
        if (!m_subEvent.isEmpty()) {
            // register the handler
            info(" EVENT HANDLER SUBSCRIBERS has been configured !");
        } else {
            return;
        }

        // Check if the configuration has a event.topics property and update the
        // topic if needed
        if (conf.get("event.topics") != null) {
            // Map <callbackName, topics>
            Dictionary d = (Dictionary) conf.get("event.topics");
            Enumeration e = d.keys();
            while (e.hasMoreElements()) {
                String myName = (String) e.nextElement(); // name
                for (int i = 0; i < m_subEvent.size(); i++) {
                    EventAdminSubscriberMetadata met = (EventAdminSubscriberMetadata) (m_subEvent.get(i));
                    if (met.getName().equals(myName)) {
                        met.setTopics((String) d.get(myName));
                        break;
                    }
                }
            }
        }
        // Check if the configuration has a event.filter property and update the
        // filter if needed
        if (conf.get("event.filter") != null) {
            // Map <callbackName, topics>
            Dictionary d = (Dictionary) conf.get("event.filter");
            Enumeration e = d.keys();
            while (e.hasMoreElements()) {
                String myName = (String) e.nextElement(); // name
                for (int i = 0; i < m_subEvent.size(); i++) {
                    EventAdminSubscriberMetadata met = (EventAdminSubscriberMetadata) (m_subEvent.get(i));
                    if (met.getName().equals(myName)) {
                        try {
                            met.setFilter((String) d.get(myName));
                        } catch (InvalidSyntaxException e1) {
                            e1.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }
    }

    /** 
     * Handler start method.
     * Expose the EventHandler service. 
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        if (!m_subEvent.isEmpty()) {
            // build the topic to listen
            // Topics is a merge of all required topics by subscribers
            String topics = "";

            // for all subscribers
            for (int i = 0; i < m_subEvent.size(); i++) {
                EventAdminSubscriberMetadata sub = (EventAdminSubscriberMetadata) m_subEvent.get(i);
                String stopics = sub.getTopics();
                // gets its topic
                if (topics.length() > 0 && stopics.length() > 0) {
                    topics += ",";
                }
                // concat to the main topic
                topics += stopics;
            }

            // Put the m_topics properties to the good value
            m_topics = ParseUtils.split(topics, ",");
        }
    }

    /***************************************************************************************************************************************************************************************************
     * Handler lifecycle management
     **************************************************************************************************************************************************************************************************/

    /**
     * Handler state change method.
     * register or unregister the EventHandler service according to the new state. 
     * @param state : new state
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == InstanceManager.VALID) {
            start();
            return;
        }
        if (state == InstanceManager.INVALID) {
            stop();
            return;
        }
    }

    /***************************************************************************************************************************************************************************************************
     * OSGi EventHandler management
     **************************************************************************************************************************************************************************************************/

    /** 
     * Receive an event.
     * The event is dispatch to attached subscribers.
     * @param event : the received event.
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    public void handleEvent(Event event) {
        // for each subscribers
        for (int i = 0; i < m_subEvent.size(); i++) {
            EventAdminSubscriberMetadata eventSubscriberData = (EventAdminSubscriberMetadata) m_subEvent.get(i);
            synchronized (this) {
                // check if the subscribers topic match

                if (eventSubscriberData.matchingTopic(event.getTopic()) && event.matches(eventSubscriberData.getFilter()) && m_manager.getState() == InstanceManager.VALID) {
                    try {
                        Callback c = eventSubscriberData.getCallback();
                        if (c != null) {
                            c.call(new Object[] { event });
                        }
                    } catch (Exception e) {
                        error("EVENT HANDLER SUBSCRIBERS CALLBACK error : " + eventSubscriberData.getCallbackStr() + " exception :" + e.getMessage());
                        // stop the component :
                        m_manager.setState(InstanceManager.INVALID);
                    }
                }
            }
        }
    }

    /**
     * The stop method of a handler.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
    }

}
