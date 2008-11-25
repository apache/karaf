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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.handlers.event.EventUtil;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;

/**
 * Represent a publisher.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
class EventAdminPublisherMetadata {

    // Names of metadata attributes

    /**
     * The name attribute in the component metadata.
     */
    public static final String NAME_ATTRIBUTE = "name";

    /**
     * The field attribute in the component metadata.
     */
    public static final String FIELD_ATTRIBUTE = "field";

    /**
     * The topics attribute in the component metadata.
     */
    public static final String TOPICS_ATTRIBUTE = "topics";

    /**
     * The synchronous attribute in the component metadata.
     */
    public static final String SYNCHRONOUS_ATTRIBUTE = "synchronous";

    /**
     * The data key attribute in the component metadata.
     */
    public static final String DATA_KEY_ATTRIBUTE = "data-key";

    // Default values

    /**
     * The data key attribute's default value.
     */
    public static final String DEFAULT_DATA_KEY_VALUE = "user.data";

    /**
     * The synchronous attribute's default value.
     */
    public static final boolean DEFAULT_SYNCHRONOUS_VALUE = false;

    /**
     * The name which acts as an identifier.
     */
    private final String m_name;

    /**
     * The name of the field representing the publisher in the component.
     */
    private final String m_field;

    /**
     * Topics to which events are sent.
     */
    private String[] m_topics;

    /**
     * Events sending mode.
     */
    private final boolean m_synchronous;

    /**
     * The key where user data are stored in the event dictionary.
     */
    private final String m_dataKey;

    /**
     * Constructs a publisher from its metadata description.
     * 
     * @param publisher the publisher metadata description.
     * @throws ConfigurationException if the configuration of the component or
     *             the instance is invalid.
     */
    public EventAdminPublisherMetadata(Element publisher)
        throws ConfigurationException {

        /**
         * Setup required attributes
         */

        // NAME_ATTRIBUTE
        if (publisher.containsAttribute(NAME_ATTRIBUTE)) {
            m_name = publisher.getAttribute(NAME_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + NAME_ATTRIBUTE);
        }

        // FIELD_ATTRIBUTE
        if (publisher.containsAttribute(FIELD_ATTRIBUTE)) {
            m_field = publisher.getAttribute(FIELD_ATTRIBUTE);
        } else {
            throw new ConfigurationException(
                    "Missing required attribute in component configuration : "
                            + FIELD_ATTRIBUTE);
        }

        // TOPICS_ATTRIBUTE
        if (publisher.containsAttribute(TOPICS_ATTRIBUTE)) {
            setTopics(publisher.getAttribute(TOPICS_ATTRIBUTE));
        } else {
            m_topics = null;
            // Nothing to do if TOPICS_ATTRIBUTE is not present as it can be
            // overridden in the instance configuration.
        }

        /**
         * Setup optional attributes
         */

        // SYNCHRONOUS_ATTRIBUTE
        if (publisher.containsAttribute(SYNCHRONOUS_ATTRIBUTE)) {
            m_synchronous = "true".equalsIgnoreCase(publisher
                    .getAttribute(SYNCHRONOUS_ATTRIBUTE));
        } else {
            m_synchronous = DEFAULT_SYNCHRONOUS_VALUE;
        }

        // DATA_KEY_ATTRIBUTE
        if (publisher.containsAttribute(DATA_KEY_ATTRIBUTE)) {
            m_dataKey = publisher.getAttribute(DATA_KEY_ATTRIBUTE);
        } else if (publisher.containsAttribute("data_key")) {
            m_dataKey = publisher.getAttribute("data_key");
        } else {
            m_dataKey = DEFAULT_DATA_KEY_VALUE;
        }
    }

    /**
     * Sets the topics attribute of the publisher.
     * 
     * @param topicsString the comma separated list of the topics on which
     *            events are sent
     * @throws ConfigurationException the specified topic string is malformed
     */
    public void setTopics(String topicsString) throws ConfigurationException {
        String[] newTopics = ParseUtils.split(topicsString,
                EventUtil.TOPIC_SEPARATOR);
        // Check each topic is valid
        for (int i = 0; i < newTopics.length; i++) {
            String topic = newTopics[i];
            if (!EventUtil.isValidTopic(topic)) {
                throw new ConfigurationException("Invalid topic : \"" + topic
                        + "\".");
            }
        }
        m_topics = newTopics;
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
     * Gets the name attribute of the publisher.
     * 
     * @return the name
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets the field attribute of the publisher.
     * 
     * @return the field
     */
    public String getField() {
        return m_field;
    }

    /**
     * Gets the topics attribute of the publisher.
     * 
     * @return the topics
     */
    public String[] getTopics() {
        return m_topics;
    }

    /**
     * Gets the synchronous attribute of the publisher.
     * 
     * @return the synchronous mode
     */
    public boolean isSynchronous() {
        return m_synchronous;
    }

    /**
     * Gets the dataKey attribute of the publisher.
     * 
     * @return the data key
     */
    public String getDataKey() {
        return m_dataKey;
    }
}
