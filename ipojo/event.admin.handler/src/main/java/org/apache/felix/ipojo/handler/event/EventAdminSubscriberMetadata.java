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

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.event.Event;

/**
 * Represent an subscriber.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class EventAdminSubscriberMetadata {

    /**
     * Name of the callback method.
     */
    private String m_callbackStr;

    /**
     * Callback method.
     */
    private Callback m_callback;

    /**
     * Listened topics.
     */
    private String m_topics;

    /**
     * String form of the event filter.
     */
    private String m_filterStr;

    /**
     * Event filter.
     */
    private Filter m_filter;

    /**
     * Event object.
     */
    private Event m_value;

    /**
     * The name which acts as an identifier.
     */
    private String m_name;

    /**
     * Ths Instance Manager.
     */
    private InstanceManager m_instanceManager;

    /**
     * Constructor.
     * @param instanceManager : instance manager.
     * @param subscriber : subscriber metadata.
     */
    public EventAdminSubscriberMetadata(InstanceManager instanceManager, Element subscriber) {
        this.m_callbackStr = null;
        this.m_topics = null;
        this.m_filterStr = "";
        this.m_filter = null;
        this.m_value = null;
        this.m_name = null;
        this.m_instanceManager = instanceManager;
        if (checkValidity(subscriber)) {
            try {
                if (subscriber.containsAttribute("topics")) {
                    this.m_topics = subscriber.getAttribute("topics");
                }
                this.m_callbackStr = subscriber.getAttribute("callback");
                if (subscriber.containsAttribute("filter")) {
                    this.m_filterStr = subscriber.getAttribute("filter");
                } else {
                    this.m_filterStr = "(event.topics=*)";
                }
                this.m_filter = instanceManager.getContext().createFilter(this.m_filterStr);

                this.m_name = subscriber.getAttribute("name");
            } catch (InvalidSyntaxException e) {
                instanceManager.getFactory().getLogger().log(Logger.WARNING, "===> EVENT handler : " + e.getMessage());
            }
        }

    }

    /**
     * Does the topic match with the listenned topics ?
     * @param topic : topic to test.
     * @return true if the given topic is a listenned topic.
     */
    public boolean matchingTopic(String topic) {
        return EventUtil.matches(topic, ParseUtils.split(m_topics, ","));
    }

    /**
     * Is the subscriber metadata valid ? This method check only the existence of a callback and a name attribute.
     * @param subscriber : metadata.
     * @return true if the metadata is valid.
     */
    private boolean checkValidity(Element subscriber) {
        return subscriber.containsAttribute("callback") && subscriber.containsAttribute("name");
    }

    /**
     * Get the callback method name.
     * @return the callback method name.
     */
    public String getCallbackStr() {
        return m_callbackStr;
    }

    /**
     * Get the callback object.
     * @return the callback object.
     */
    public Callback getCallback() {
        return m_callback;
    }

    /**
     * Set the callback object.
     * @param c : callback.
     */
    public void setCallback(Callback c) {
        this.m_callback = c;
    }

    /**
     * Get listened topics.
     * @return the string form of the listened topics.
     */
    public String getTopics() {
        return m_topics;
    }

    public Event getValue() {
        return m_value;
    }

    public void setValue(Event value) {
        this.m_value = value;
    }

    public Filter getFilter() {
        return m_filter;
    }

    public void setTopics(String top) {
        m_topics = top;
    }

    /**
     * Create and set the filter. The filter is create from the given argument.
     * @param filter : the String form of the LDAP filter.
     * @throws InvalidSyntaxException : occurs when the given filter is invalid.
     */
    public void setFilter(String filter) throws InvalidSyntaxException {
        this.m_filterStr = filter;
        this.m_filter = m_instanceManager.getContext().createFilter(this.m_filterStr);
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        this.m_name = name;
    }

}
