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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.handlers.event.EventUtil;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.BundleContext;
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
    private String[] m_topics;

    /**
     * The key where user data are stored in the event dictionary.
     */
    private String m_dataKey;

    /**
     * The type of received data.
     */
    private final Class m_dataType;

    /**
     * Event filter.
     */
    private Filter m_filter;

    /**
     * The context of the bundle.
     */
    private final BundleContext m_bundleContext;

    /**
     * Constructor.
     * 
     * @param bundleContext the bundle context of the managed instance.
     * @param subscriber the subscriber metadata.
     * @throws ConfigurationException if the configuration of the component or
     *             the instance is invalid.
     */
    public EventAdminSubscriberMetadata(BundleContext bundleContext,
            Element subscriber) throws ConfigurationException {
        m_bundleContext = bundleContext;

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
        } else if (subscriber.containsAttribute("method")) {
            m_callback = subscriber.getAttribute("method");
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + CALLBACK_ATTRIBUTE);
        }

        // TOPICS_ATTRIBUTE
        if (subscriber.containsAttribute(TOPICS_ATTRIBUTE)) {
            setTopics(subscriber.getAttribute(TOPICS_ATTRIBUTE));
        } else {
            m_topics = null;
            // Nothing to do if TOPICS_ATTRIBUTE is not present as it can be
            // overridden in the instance configuration.
        }

        /**
         * Setup optional attributes
         */

        // DATA_KEY_ATTRIBUTE
        m_dataKey = subscriber.getAttribute(DATA_KEY_ATTRIBUTE);
        if (m_dataKey == null) { // Alternative configuration
            m_dataKey = subscriber.getAttribute("data_key");
        }
        
        String t = subscriber.getAttribute(DATA_TYPE_ATTRIBUTE);
        if (t == null) { // Alternative configuration
            t = subscriber.getAttribute("data_type");
        }
        
        if (t != null) {
            // Check that the data-key attribute is set.
            if (m_dataKey == null) {
                throw new ConfigurationException(
                        "Missing attribute in component configuration : "
                                + DATA_KEY_ATTRIBUTE);
            }
            Class type;
            try {
                type = m_bundleContext.getBundle().loadClass(t);
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Data type class not found : "
                        + t);
            }
            m_dataType = type;
        } else {
            m_dataType = DEFAULT_DATA_TYPE_VALUE;
        }

        // FILTER_ATTRIBUTE
        if (subscriber.containsAttribute(FILTER_ATTRIBUTE)) {
            setFilter(subscriber.getAttribute(FILTER_ATTRIBUTE));
        }
    }

    /**
     * Sets the topics attribute of the subscriber.
     * 
     * @param topicsString the comma separated list of the topics to listen
     * @throws ConfigurationException if the specified topic list is malformed
     */
    public void setTopics(String topicsString) throws ConfigurationException {
        String[] newTopics = ParseUtils.split(topicsString,
                EventUtil.TOPIC_SEPARATOR);
        // Check each topic is valid
        for (int i = 0; i < newTopics.length; i++) {
            String topicScope = newTopics[i];
            if (!EventUtil.isValidTopicScope(topicScope)) {
                throw new ConfigurationException("Invalid topic scope : \""
                        + topicScope + "\".");
            }
        }
        m_topics = newTopics;
    }

    /**
     * Sets the filter attribute of the subscriber.
     * 
     * @param filterString the string representation of the event filter
     * @throws ConfigurationException if the LDAP filter is malformed
     */
    public void setFilter(String filterString) throws ConfigurationException {
        try {
            m_filter = m_bundleContext.createFilter(filterString);
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException("Invalid filter syntax");
        }
    }

    /**
     * Checks that the required instance configurable attributes are all set.
     * 
     * @throws ConfigurationException if a required attribute is missing
     */
    public void check() throws ConfigurationException {
        if (m_topics == null || m_topics.length == 0) {
            throw new ConfigurationException(
                    "Missing required attribute in component or instance configuration : "
                            + TOPICS_ATTRIBUTE);
        }
    }

    /**
     * Gets the name attribute of the subscriber.
     * 
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets the topics attribute of the subscriber.
     * 
     * @return the topics
     */
    public String[] getTopics() {
        return m_topics;
    }

    /**
     * Gets the callback attribute of the subscriber.
     * 
     * @return the callback
     */
    public String getCallback() {
        return m_callback;
    }

    /**
     * Gets the data key attribute of the subscriber.
     * 
     * @return the dataKey
     */
    public String getDataKey() {
        return m_dataKey;
    }

    /**
     * Gets the data type attribute of the subscriber.
     * 
     * @return the dataType
     */
    public Class getDataType() {
        return m_dataType;
    }

    /**
     * Gets the filter attribute of the subscriber.
     * 
     * @return the filter
     */
    public Filter getFilter() {
        return m_filter;
    }
}
