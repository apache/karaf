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
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.osgi.service.event.EventAdmin;

/**
 * Event Publisher Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminPublisherHandler extends PrimitiveHandler {

    /**
     * The handler Namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handlers.event";

    /**
     * The names of instance configuration properties.
     */
    public static final String TOPICS_PROPERTY = "event.topics";

    /**
     * The prefix for logged messages.
     */
    private static final String LOG_PREFIX = "EVENT ADMIN PUBLISHER HANDLER : ";

    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

    /**
     * The current EventAdmin service.
     */
    private EventAdmin m_ea;

    /**
     * The publishers accessible by their fields.
     */
    private Map m_publishersByField = new Hashtable();

    /**
     * Initializes the component type.
     * 
     * @param cd the component type description to populate
     * @param metadata the component type metadata
     * @throws ConfigurationException if the given metadata is incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription,
     *      org.apache.felix.ipojo.metadata.Element)
     */
    // @Override
    public void initializeComponentFactory(ComponentTypeDescription cd,
            Element metadata)
        throws ConfigurationException {

        // Update the current component description
        Dictionary dict = new Properties();
        PropertyDescription pd = new PropertyDescription(TOPICS_PROPERTY,
                Dictionary.class.getName(), dict.toString());
        cd.addProperty(pd);

        // Get Metadata publishers
        Element[] publishers = metadata.getElements("publisher", NAMESPACE);
        if (publishers != null) {

            // Maps used to check name and field are unique
            Set nameSet = new HashSet();
            Set fieldSet = new HashSet();

            // Check all publishers are well formed
            for (int i = 0; i < publishers.length; i++) {

                // Check the publisher configuration is correct by creating an
                // unused publisher metadata
                EventAdminPublisherMetadata publisherMetadata = new EventAdminPublisherMetadata(
                        publishers[i]);
                String name = publisherMetadata.getName();
                info(LOG_PREFIX + "Checking publisher " + name);

                // Check field existence and type
                String field = publisherMetadata.getField();
                FieldMetadata fieldMetadata = getPojoMetadata()
                        .getField(publisherMetadata.getField(),
                                Publisher.class.getName());
                if (fieldMetadata == null) {
                    throw new ConfigurationException(
                            "Field not found in the component : "
                                    + Publisher.class.getName() + " " + field);
                }

                // Check name and field are unique
                if (nameSet.contains(name)) {
                    throw new ConfigurationException(
                            "A publisher with the same name already exists : "
                                    + name);
                } else if (fieldSet.contains(field)) {
                    throw new ConfigurationException("The field " + field
                            + " is already associated to a publisher");
                }
                nameSet.add(name);
                fieldSet.add(field);
            }
        } else {
            info(LOG_PREFIX + "No publisher to check");
        }
    }

    /**
     * Constructor.
     * 
     * @param metadata the component type metadata
     * @param conf the instance configuration
     * @throws ConfigurationException if one event publication is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager,
     *      org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    // @Override
    public void configure(Element metadata, Dictionary conf)
        throws ConfigurationException {

        // Store the component manager
        m_manager = getInstanceManager();

        // Get the topics instance configuration
        Dictionary instanceTopics = (Dictionary) conf.get(TOPICS_PROPERTY);

        // Get Metadata publishers
        Element[] publishers = metadata.getElements("publisher", NAMESPACE);

        if (publishers != null) {
            // then check publishers are well formed and fill the publishers'
            // map
            for (int i = 0; i < publishers.length; i++) {

                // Extract the publisher configuration
                EventAdminPublisherMetadata publisherMetadata = new EventAdminPublisherMetadata(
                        publishers[i]);
                String name = publisherMetadata.getName();
                info(LOG_PREFIX + "Configuring publisher " + name);

                // Get the topic instance configuration if redefined
                String topicsString = (instanceTopics != null) ? (String) instanceTopics
                        .get(name)
                        : null;
                if (topicsString != null) {
                    publisherMetadata.setTopics(topicsString);
                }

                // Check the publisher is correctly configured
                publisherMetadata.check();

                // Create the associated Publisher and insert it in the
                // publisher map
                Publisher publisher = new PublisherImpl(this, publisherMetadata
                        .getTopics(), publisherMetadata.isSynchronous(),
                        publisherMetadata.getDataKey(), m_manager
                                .getInstanceName());
                m_publishersByField
                        .put(publisherMetadata.getField(), publisher);

                // Register the callback that return the publisher
                // reference when the specified field is read by the
                // POJO.
                FieldMetadata fieldMetadata = getPojoMetadata()
                        .getField(publisherMetadata.getField(),
                                Publisher.class.getName());
                m_manager.register(fieldMetadata, this);
            }
        } else {
            info(LOG_PREFIX + "No publisher to configure");
        }
    }

    /**
     * Starts the handler instance.
     * 
     * This method does nothing.
     */
    // @Override
    public void start() {
    }

    /**
     * Stops the handler instance.
     * 
     * This method does nothing.
     */
    // @Override
    public void stop() {
    }

    /**
     * Field interceptor callback. This method is called when the component
     * attempt to one of its Publisher field.
     * 
     * @param pojo the accessed field
     * @param fieldName the name of the accessed field
     * @param value the value of the field (useless here)
     * 
     * @return the Publisher associated with the accessed field's name
     */
    // @Override
    public Object onGet(Object pojo, String fieldName, Object value) {
        // Retrieve the publisher associated to the given field name
        Publisher pub = (Publisher) m_publishersByField.get(fieldName);
        if (pub == null) {
            error(LOG_PREFIX + "No publisher associated to the field "
                    + fieldName);
        }
        return pub;
    }

    /**
     * This method is called by managed publishers to obtain the current
     * EventAdmin service.
     * 
     * @return the current EventAdmin service.
     */
    public EventAdmin getEventAdminService() {
        return m_ea;
    }
}
