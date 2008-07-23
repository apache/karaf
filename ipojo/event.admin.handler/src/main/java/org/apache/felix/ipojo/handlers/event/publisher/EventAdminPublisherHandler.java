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
import java.util.Map;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.EventAdmin;

/**
 * Event Publisher Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventAdminPublisherHandler extends PrimitiveHandler implements
        ServiceListener {

    /**
     * Handler Namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.handlers.event.EventAdminHandler";

    /**
     * Names of instance configuration properties.
     */
    public static final String TOPICS_PROPERTY = "event.topics";

    /**
     * Prefix for logged messages.
     */
    private static final String LOG_PREFIX = "EVENT ADMIN PUBLISHER HANDLER : ";

    /**
     * The Instance Manager.
     */
    private InstanceManager m_manager;

    /**
     * The bundle context.
     */
    private BundleContext m_context;

    /**
     * The current Event Admin service reference.
     */
    private ServiceReference m_eaReference;

    /**
     * The current Event Admin service.
     */
    private EventAdmin m_ea;

    /**
     * The publishers accessible by their names.
     */
    private Map m_publishersByName = new Hashtable();

    /**
     * The publishers accessible by their fields.
     */
    private Map m_publishersByField = new Hashtable();

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
    }

    /**
     * Constructor.
     * 
     * @param metadata :
     *            component type metadata
     * @param conf :
     *            instance configuration
     * @throws ConfigurationException :
     *             one event publication is not correct
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager,
     *      org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    // @Override
    public void configure(Element metadata, Dictionary conf)
        throws ConfigurationException {

        // Store the component manager
        m_manager = getInstanceManager();

        // Get Metadata publishers
        Element[] publishers = metadata.getElements("publisher", NAMESPACE);

        if (publishers != null) {
            // then check publishers are well formed and fill the publishers'
            // map
            for (int i = 0; i < publishers.length; i++) {

                try {
                    // Extract the publisher configuration
                    EventAdminPublisherMetadata publisherMetadata = new EventAdminPublisherMetadata(
                            publishers[i], conf);
                    String name = publisherMetadata.getName();
                    info(LOG_PREFIX + "configuring publisher " + name);

                    // Create the associated Publisher
                    Publisher publisher = new PublisherImpl(this,
                            publisherMetadata.getTopics(), publisherMetadata
                                    .isSynchronous(), publisherMetadata
                                    .getDataKey(), m_manager.getInstanceName());

                    // Check field existence and type
                    String field = publisherMetadata.getField();
                    FieldMetadata fieldMetadata = getPojoMetadata().getField(
                            publisherMetadata.getField(),
                            Publisher.class.getName());
                    if (fieldMetadata == null) {
                        throw new ConfigurationException(
                                "Field not found in the component : "
                                        + Publisher.class.getName() + " "
                                        + field);
                    }

                    // Insert in the publisher tables.
                    Object old;
                    if ((old = m_publishersByName.put(name, publisher)) != null) {
                        m_publishersByName.put(name, old);
                        throw new ConfigurationException("The publisher "
                                + name + "already exists");
                    }
                    if ((old = m_publishersByField.put(field, publisher)) != null) {
                        m_publishersByField.put(field, old);
                        m_publishersByName.remove(name);
                        throw new ConfigurationException("The field " + field
                                + " is already associated to a publisher");
                    }

                    // Register the callback that return the publisher
                    // reference when the specified field is read by the
                    // POJO.
                    m_manager.register(fieldMetadata, this);

                } catch (Exception e) {
                    // Ignore invalid publishers
                    warn(LOG_PREFIX
                            + "Ignoring publisher : Error in configuration", e);
                }
            }
        } else {
            info(LOG_PREFIX + "no publisher detected !");
        }
    }

    /**
     * Start the handler instance. This method tries to get an initial reference
     * of the EventAdmin service.
     */
    // @Override
    public void start() {
        info(LOG_PREFIX + "STARTING");

        // Look for the EventAdmin service at startup
        m_context = m_manager.getContext();
        m_eaReference = m_context.getServiceReference(EventAdmin.class
                .getName());
        if (m_eaReference != null) {
            m_ea = (EventAdmin) m_context.getService(m_eaReference);
            if (m_ea != null) {
                info(LOG_PREFIX + "EventAdmin service caught");
            }
        }

        // Update handler validity
        setValidity(m_ea != null);

        // Register service listener for EventAdmin services
        try {
            m_context.addServiceListener(this, "(OBJECTCLASS="
                    + EventAdmin.class.getName() + ")");
        } catch (InvalidSyntaxException e) {
            error(LOG_PREFIX + "Cannot register ServiceListener", e);
        }
        info(LOG_PREFIX + "STARTED");
    }

    /**
     * Stop the handler instance. This method release the used EventAdmin
     * service (if any).
     */
    // @Override
    public void stop() {
        info(LOG_PREFIX + "STOPPING");

        // Unregister service
        if (m_ea != null) {
            m_ea = null;
            m_context.ungetService(m_eaReference);
            info(LOG_PREFIX + "EventAdmin service released");
        }

        info(LOG_PREFIX + "STOPPED");
    }

    /***************************************************************************
     * OSGi ServiceListener callback
     **************************************************************************/

    /**
     * Service listener callback. This method check the
     * registration/unregistration of EventAdmin services and update the
     * valididy of the handler.
     * 
     * @param event
     *            the concerned service reference
     */
    // @Override
    public void serviceChanged(ServiceEvent event) {
        warn(LOG_PREFIX + "serviceChanged()");

        if (m_eaReference == null && event.getType() == ServiceEvent.REGISTERED) {
            // An EventAdmin service appeared and was expected
            m_eaReference = event.getServiceReference();
            m_ea = (EventAdmin) m_context.getService(m_eaReference);
            info(LOG_PREFIX + "EventAdmin service caught");
        } else if (m_eaReference != null
                && event.getType() == ServiceEvent.UNREGISTERING
                && m_eaReference == event.getServiceReference()) {
            // The used EventAdmin service disappeared
            m_ea = null;
            m_context.ungetService(m_eaReference);
            info(LOG_PREFIX + "EventAdmin service released");

            // Find another EventAdmin service if available
            m_eaReference = m_context.getServiceReference(EventAdmin.class
                    .getName());
            if (m_eaReference != null) {
                m_ea = (EventAdmin) m_context.getService(m_eaReference);
                info(LOG_PREFIX + "EventAdmin service caught");
            }
        }

        // Update handler validity
        setValidity(m_ea != null);
    }

    /**
     * Field interceptor callback. This method is called when the component
     * attempt to one of its Publisher field.
     * 
     * @param pojo
     *            the accessed field
     * @param fieldName
     *            the name of the accessed field
     * @param value
     *            the value of the field (useless here)
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
     * @return the current EventAdmin service or {@code null} if unavailable.
     */
    public EventAdmin getEventAdminService() {
        return m_ea;
    }
}
