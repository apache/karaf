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

import java.util.Dictionary;
import java.util.Enumeration;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Represent an subscriber.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class EventAdminSubscriberMetadata {

    // Names of metadata attributes

    /**
     * The name attribute in the component metadata.
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * The callback attribute in the component metadata.
     */
    public static final String CALLBACK_ATTRIBUTE = "callback";

    /**
     * The topics attribute in the component metadata.
     */
    public static final String TOPICS_ATTRIBUTE = "topics";

    /**
     * The data key attribute in the component metadata.
     */
    public static final String DATA_KEY_ATTRIBUTE = "data-key";

    /**
     * The data type attribute in the component metadata.
     */
    public static final String DATA_TYPE_ATTRIBUTE = "data-type";

    /**
     * The filter attribute in the component metadata.
     */
    public static final String FILTER_ATTRIBUTE = "filter";

    // Default values

    /**
     * The data type atttribute's default value.
     */
    public static final Class DEFAULT_DATA_TYPE_VALUE = java.lang.Object.class;

    /**
     * The name which acts as an identifier.
     */
    private final String m_name;

    /**
     * Name of the callback method.
     */
    private final String m_callback;

    /**
     * Listened topics.
     */
    private final String[] m_topics;

    /**
     * The key where user data are stored in the event dictionary.
     */
    private final String m_dataKey;

    /**
     * The type of received data.
     */
    private final Class m_dataType;

    /**
     * Event filter.
     */
    private final Filter m_filter;

    /**
     * The Instance Manager.
     */
    private final InstanceManager m_instanceManager;

    /**
     * Constructor.
     * 
     * @param instanceManager :
     *            instance manager.
     * @param subscriber :
     *            subscriber metadata.
     * @param instanceConf :
     *            the configuration of the component instance
     * @throws ConfigurationException
     *             if the configuration of the component or the instance is
     *             invalid.
     */
    public EventAdminSubscriberMetadata(InstanceManager instanceManager,
            Element subscriber, Dictionary instanceConf)
        throws ConfigurationException {

        m_instanceManager = instanceManager;

        /**
         * Setup required attributes
         */

        // NAME_ATTRIBUTE
        if (subscriber.containsAttribute(NAME_ATTRIBUTE)) {
            m_name = subscriber.getAttribute(NAME_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + NAME_ATTRIBUTE);
        }

        // CALLBACK_ATTRIBUTE
        if (subscriber.containsAttribute(CALLBACK_ATTRIBUTE)) {
            m_callback = subscriber.getAttribute(CALLBACK_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + CALLBACK_ATTRIBUTE);
        }

        // TOPICS_ATTRIBUTE
        String topicsString = null;
        if (subscriber.containsAttribute(TOPICS_ATTRIBUTE)) {
            topicsString = subscriber.getAttribute(TOPICS_ATTRIBUTE);
        }
        // Check TOPICS_PROPERTY in the instance configuration
        Dictionary instanceTopics = (Dictionary) instanceConf
                .get(EventAdminSubscriberHandler.TOPICS_PROPERTY);
        if (instanceTopics != null) {
            Enumeration e = instanceTopics.keys();
            while (e.hasMoreElements()) {
                String myName = (String) e.nextElement(); // name
                if (m_name.equals(myName)) {
                    topicsString = (String) instanceTopics.get(myName);
                    break;
                }
            }
        }
        if (topicsString != null) {
            m_topics = ParseUtils.split(topicsString, ",");
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component or instance configuration : "
                            + TOPICS_ATTRIBUTE);
        }

        /**
         * Setup optional attributes
         */

        // DATA_KEY_ATTRIBUTE
        m_dataKey = subscriber.getAttribute(DATA_KEY_ATTRIBUTE);
        if (subscriber.containsAttribute(DATA_TYPE_ATTRIBUTE)) {
            Class type;
            try {
                type = m_instanceManager.getContext().getBundle().loadClass(
                        subscriber.getAttribute(DATA_TYPE_ATTRIBUTE));
            } catch (ClassNotFoundException e) {
                m_instanceManager
                        .getFactory()
                        .getLogger()
                        .log(
                                Logger.WARNING,
                                "Ignoring data-type (using default) : Malformed attribute in metadata",
                                e);
                type = DEFAULT_DATA_TYPE_VALUE;
            }
            m_dataType = type;
        } else {
            m_dataType = DEFAULT_DATA_TYPE_VALUE;
        }

        // FILTER_ATTRIBUTE
        String filterString = null;
        if (subscriber.containsAttribute(FILTER_ATTRIBUTE)) {
            filterString = subscriber.getAttribute(FILTER_ATTRIBUTE);
        }
        // Check FILTER_PROPERTY in the instance configuration
        Dictionary instanceFilter = (Dictionary) instanceConf
                .get(EventAdminSubscriberHandler.FILTER_PROPERTY);
        if (instanceFilter != null) {
            Enumeration e = instanceFilter.keys();
            while (e.hasMoreElements()) {
                String myName = (String) e.nextElement(); // name
                if (m_name.equals(myName)) {
                    filterString = (String) instanceFilter.get(myName);
                    break;
                }
            }
        }
        Filter filter;
        if (filterString != null) {
            try {
                filter = m_instanceManager.getContext().createFilter(
                        filterString);
            } catch (InvalidSyntaxException e) {
                // Ignore filter if malformed
                m_instanceManager.getFactory().getLogger().log(Logger.WARNING,
                        "Ignoring filter : Malformed attribute in metadata", e);
                filter = null;
            }
        } else {
            filter = null;
        }
        m_filter = filter;
    }

    /**
     * Get the name attribute of the subscriber.
     * 
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the topics attribute of the subscriber.
     * 
     * @return the topics
     */
    public String[] getTopics() {
        return m_topics;
    }

    /**
     * Get the callback attribute of the subscriber.
     * 
     * @return the callback
     */
    public String getCallback() {
        return m_callback;
    }

    /**
     * Get the data key attribute of the subscriber.
     * 
     * @return the dataKey
     */
    public String getDataKey() {
        return m_dataKey;
    }

    /**
     * Get the data type attribute of the subscriber.
     * 
     * @return the dataType
     */
    public Class getDataType() {
        return m_dataType;
    }

    /**
     * Get the filter attribute of the subscriber.
     * 
     * @return the filter
     */
    public Filter getFilter() {
        return m_filter;
    }

}
