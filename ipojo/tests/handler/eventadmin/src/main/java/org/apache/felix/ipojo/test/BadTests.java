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
package org.apache.felix.ipojo.test;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.junit4osgi.OSGiTestCase;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.test.donut.Donut;
import org.apache.felix.ipojo.test.donut.DonutConsumer;
import org.apache.felix.ipojo.test.donut.DonutProvider;
import org.apache.felix.ipojo.test.util.EahTestUtils;
import org.apache.felix.ipojo.test.util.IPojoTestUtils;
import org.osgi.framework.ServiceReference;

/**
 * Test the good behaviour of the EventAdminHandler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class BadTests extends OSGiTestCase {
	
	 /**
     * The utility class instance.
     */
    public EahTestUtils m_utils;

    /**
     * The namespace of the Event admin handler.
     */
    private static final String NAMESPACE = "org.apache.felix.ipojo.handlers.event.EventAdminHandler";

    /**
     * The available components.
     */
    private Element[] m_components;

    /**
     * The description of a component that uses an event publisher.
     */
    private Element m_provider;

    /**
     * The event publisher description.
     */
    private Element m_publisher;

    /**
     * The name attribute of the event publisher.
     */
    private Attribute m_publisherName;

    /**
     * The field attribute of the event publisher.
     */
    private Attribute m_publisherField;

    /**
     * The topics attribute of the event publisher.
     */
    private Attribute m_publisherTopics;

    /**
     * The data-key attribute of the event publisher.
     */
    private Attribute m_publisherDataKey;

    /**
     * The synchronous attribute of the event publisher.
     */
    private Attribute m_publisherSynchronous;

    /**
     * The description of a component that uses an event subscriber.
     */
    private Element m_consumer;

    /**
     * The event subscriber description.
     */
    private Element m_subscriber;

    /**
     * The name attribute of the event subscriber.
     */
    private Attribute m_subscriberName;

    /**
     * The callback attribute of the event subscriber.
     */
    private Attribute m_subscriberCallback;

    /**
     * The topics attribute of the event subscriber.
     */
    private Attribute m_subscriberTopics;

    /**
     * The data-key attribute of the event subscriber.
     */
    private Attribute m_subscriberDataKey;

    /**
     * The data-type attribute of the event subscriber.
     */
    private Attribute m_subscriberDataType;

    private Element getManipulationForComponent(String compName) {
        for (int i = 0; i < m_components.length; i++) {
            if (m_components[i].containsAttribute("name")
                    && m_components[i].getAttribute("name").equals(compName)) {
                return m_components[i].getElements("manipulation")[0];
            }
        }
        return null;
    }

    /**
     * Initialization before test cases.
     * 
     * Create all the instances
     * 
     */
    public void setUp() {
    	m_utils = new EahTestUtils(context);
        /**
         * Get the list of available components.
         */
        try {
            String header = (String) context.getBundle().getHeaders().get(
                    "iPOJO-Components");
            m_components = ManifestMetadataParser.parseHeaderMetadata(header)
                    .getElements("component");
        } catch (ParseException e) {
            fail("Parse Exception when parsing iPOJO-Component");
        }

        /**
         * Initialize the standard publishing component (based on the
         * asynchronous donut provider).
         */
        m_provider = new Element("component", "");
        m_provider.addAttribute(new Attribute("className",
                "org.apache.felix.ipojo.test.donut.DonutProviderImpl"));
        m_provider.addAttribute(new Attribute("name",
                "standard donut provider for bad tests"));

        // The provided service of the publisher
        Element providesDonutProvider = new Element("provides", "");
        providesDonutProvider.addAttribute(new Attribute("interface",
                "org.apache.felix.ipojo.test.donut.DonutProvider"));
        Element providesDonutProviderProperty = new Element("property", "");
        providesDonutProviderProperty
                .addAttribute(new Attribute("name", "name"));
        providesDonutProviderProperty.addAttribute(new Attribute("field",
                "m_name"));
        providesDonutProviderProperty.addAttribute(new Attribute("value",
                "Unknown donut vendor"));
        providesDonutProvider.addElement(providesDonutProviderProperty);
        m_provider.addElement(providesDonutProvider);

        // The event publisher, corresponding to the following description :
        // <ev:publisher name="donut-publisher" field="m_publisher"
        // topics="food/donuts" data-key="food" synchronous="false"/>
        m_publisher = new Element("publisher", NAMESPACE);
        m_publisherName = new Attribute("name", "donut-publisher");
        m_publisherField = new Attribute("field", "m_publisher");
        m_publisherTopics = new Attribute("topics", "food/donuts");
        m_publisherDataKey = new Attribute("data-key", "food");
        m_publisherSynchronous = new Attribute("synchronous", "false");
        m_publisher.addAttribute(m_publisherName);
        m_publisher.addAttribute(m_publisherField);
        m_publisher.addAttribute(m_publisherTopics);
        m_publisher.addAttribute(m_publisherDataKey);
        m_publisher.addAttribute(m_publisherSynchronous);
        m_provider.addElement(m_publisher);

        m_provider.addElement(getManipulationForComponent("donut-provider"));

        /**
         * Initialize the standard subscribing component (based on the donut
         * consumer).
         */
        m_consumer = new Element("component", "");
        m_consumer.addAttribute(new Attribute("className",
                "org.apache.felix.ipojo.test.donut.DonutConsumerImpl"));
        m_consumer.addAttribute(new Attribute("name",
                "standard donut consumer for bad tests"));

        // The provided service of the publisher
        Element providesDonutConsumer = new Element("provides", "");
        providesDonutConsumer.addAttribute(new Attribute("interface",
                "org.apache.felix.ipojo.test.donut.DonutConsumer"));
        Element providesDonutConsumerNameProperty = new Element("property", "");
        providesDonutConsumerNameProperty.addAttribute(new Attribute("name",
                "name"));
        providesDonutConsumerNameProperty.addAttribute(new Attribute("field",
                "m_name"));
        providesDonutConsumerNameProperty.addAttribute(new Attribute("value",
                "Unknown donut consumer"));
        providesDonutConsumer.addElement(providesDonutConsumerNameProperty);
        Element providesDonutConsumerSlowProperty = new Element("property", "");
        providesDonutConsumerSlowProperty.addAttribute(new Attribute("name",
                "slow"));
        providesDonutConsumerSlowProperty.addAttribute(new Attribute("field",
                "m_isSlow"));
        providesDonutConsumerSlowProperty.addAttribute(new Attribute("value",
                "false"));
        providesDonutConsumer.addElement(providesDonutConsumerSlowProperty);
        m_consumer.addElement(providesDonutConsumer);

        // The event publisher, corresponding to the following description :
        // <ev:subscriber name="donut-subscriber" callback="receiveDonut"
        // topics="food/donuts" data-key="food"
        // data-type="org.apache.felix.ipojo.test.donut.Donut"/>
        m_subscriber = new Element("subscriber", NAMESPACE);
        m_subscriberName = new Attribute("name", "donut-subscriber");
        m_subscriberCallback = new Attribute("callback", "receiveDonut");
        m_subscriberTopics = new Attribute("topics", "food/donuts");
        m_subscriberDataKey = new Attribute("data-key", "food");
        m_subscriberDataType = new Attribute("data-type",
                "org.apache.felix.ipojo.test.donut.Donut");
        m_subscriber.addAttribute(m_subscriberName);
        m_subscriber.addAttribute(m_subscriberCallback);
        m_subscriber.addAttribute(m_subscriberTopics);
        m_subscriber.addAttribute(m_subscriberDataKey);
        m_subscriber.addAttribute(m_subscriberDataType);
        m_consumer.addElement(m_subscriber);

        m_consumer.addElement(getManipulationForComponent("donut-consumer"));
    }

    /**
     * Test the base configuration is correct to be sure the bad tests will fail
     * because of they are really bad, and not because of an other application
     * error.
     * 
     * This test simply create a provider and a consumer instance, send one
     * event and check it is received.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testGoodConfig()
        throws ConfigurationException, UnacceptableConfiguration,
        MissingHandlerException {
        /**
         * Create the provider and the consumer instances.
         */
        Dictionary properties = new Hashtable();

        // Provider
        ComponentFactory providerFactory = new ComponentFactory(context,
                m_provider);
        providerFactory.start();
        properties.put("instance.name","Emperor of donuts");
        ComponentInstance providerInstance = providerFactory
                .createComponentInstance(properties);
        ServiceReference providerService = IPojoTestUtils
                .getServiceReferenceByName(context, DonutProvider.class
                        .getName(), providerInstance.getInstanceName());
        DonutProvider provider = (DonutProvider) context
                .getService(providerService);

        // The consumer
        properties = new Hashtable();
        ComponentFactory consumerFactory = new ComponentFactory(context,
                m_consumer);
        consumerFactory.start();
        properties.put("instance.name","Homer Simpson");
        properties.put("slow", "false");
        ComponentInstance consumerInstance = consumerFactory
                .createComponentInstance(properties);
        ServiceReference consumerService = IPojoTestUtils
                .getServiceReferenceByName(context, DonutConsumer.class
                        .getName(), consumerInstance.getInstanceName());
        DonutConsumer consumer = (DonutConsumer) context
                .getService(consumerService);

        /**
         * Test the normal behaviour of the instances.
         */
        consumer.clearDonuts();
        Donut sentDonut = provider.sellDonut();
        Donut receivedDonut = consumer.waitForDonut();
        assertEquals("The received donut must be the same as the sent one.",
                sentDonut, receivedDonut);

        /**
         * Destroy component's instances.
         */
        context.ungetService(providerService);
        providerInstance.dispose();
        context.ungetService(consumerService);
        consumerInstance.dispose();
        providerFactory.stop();
        consumerFactory.stop();
    }

    /**
     * Try to create a publisher with no name.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithoutName()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher
        m_publisher.removeAttribute(m_publisherName);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when no name is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.addAttribute(m_publisherName);
        }
    }

    /**
     * Try to create a publisher with no field.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithoutField()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher
        m_publisher.removeAttribute(m_publisherField);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when no field is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.addAttribute(m_publisherField);
        }
    }

    /**
     * Try to create a publisher with an unexisting field.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithUnexistingField()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher and replace with an
        // unexisting field name
        m_publisher.removeAttribute(m_publisherField);
        Attribute unexistingField = new Attribute("field", "m_unexistingField");
        m_publisher.addAttribute(unexistingField);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when an unexisting field is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.removeAttribute(unexistingField);
            m_publisher.addAttribute(m_publisherField);
        }
    }

    /**
     * Try to create a publisher with a bad typed field.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithBadTypedField()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher and replace with an
        // bad typed field name
        m_publisher.removeAttribute(m_publisherField);
        Attribute badTypedField = new Attribute("field", "m_name");
        m_publisher.addAttribute(badTypedField);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when an bad typed field is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.removeAttribute(badTypedField);
            m_publisher.addAttribute(m_publisherField);
        }
    }

    /**
     * Try to create a publisher instance without topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithoutTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the publisher
        m_publisher.removeAttribute(m_publisherTopics);
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        fact.start();

        // Try to create an instance without specified topics
        Dictionary conf = new Hashtable();
        conf.put("instance.name","provider without topics");

        ComponentInstance instance;
        try {
            instance = fact.createComponentInstance(conf);
            // Should not be executed
            instance.dispose();
            fail("The factory must not create instance without specified topics.");
        } catch (ConfigurationException e) {
            // OK
        } finally {
            fact.stop();
            // Restore the original state of the publisher
            m_publisher.addAttribute(m_publisherTopics);
        }
    }

    /**
     * Try to create a publisher with malformed topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithMalformedTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the publisher and replace with a
        // malformed one
        m_publisher.removeAttribute(m_publisherTopics);
        Attribute malformedTopics = new Attribute("topics",
                "| |\\| \\/ /-\\ |_ | |)");
        m_publisher.addAttribute(malformedTopics);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when invalid topics are specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.removeAttribute(malformedTopics);
            m_publisher.addAttribute(m_publisherTopics);
        }
    }
    
    /**
     * Try to create a publisher with a pattern topic (ending with '*') instead of a fixed topic.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithPatternTopic()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the publisher and replace with a
        // malformed one
        m_publisher.removeAttribute(m_publisherTopics);
        Attribute malformedTopics = new Attribute("topics",
                "a/pattern/topic/*");
        m_publisher.addAttribute(malformedTopics);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when invalid topics are specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_publisher.removeAttribute(malformedTopics);
            m_publisher.addAttribute(m_publisherTopics);
        }
    }

    /**
     * Try to create a publisher with malformed instance topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testPublisherWithMalformedInstanceTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the publisher and replace with a
        // malformed one
        m_publisher.removeAttribute(m_publisherTopics);
        ComponentFactory fact = new ComponentFactory(context, m_provider);
        fact.start();

        // Try to create an instance with malformed specified topics
        Dictionary conf = new Hashtable();
        conf.put("instance.name","provider with malformed topics");
        Dictionary topics = new Hashtable();
        topics.put("donut-publisher", "| |\\| \\/ /-\\ |_ | |)");
        conf.put("event.topics", topics);

        ComponentInstance instance;
        try {
            instance = fact.createComponentInstance(conf);
            // Should not be executed
            instance.dispose();
            fail("The factory must not create instance with invalid specified topics.");
        } catch (ConfigurationException e) {
            // OK
        } finally {
            fact.stop();
            // Restore the original state of the publisher
            m_publisher.addAttribute(m_publisherTopics);
        }
    }

    /**
     * Try to create a subscriber with no name.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithoutName()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher
        m_subscriber.removeAttribute(m_subscriberName);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when no name is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_subscriber.addAttribute(m_subscriberName);
        }
    }

    /**
     * Try to create a subscriber with no callback.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithoutCallback()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the name attribute of the publisher
        m_subscriber.removeAttribute(m_subscriberCallback);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when no callback is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the publisher
            m_subscriber.addAttribute(m_subscriberCallback);
        }
    }

    /**
     * Try to create a subscriber instance without topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithoutTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the subscriber
        m_subscriber.removeAttribute(m_subscriberTopics);
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        fact.start();

        // Try to create an instance without specified topics
        Dictionary conf = new Hashtable();
        conf.put("instance.name","consumer without topics");
        conf.put("slow", "false");

        ComponentInstance instance;
        try {
            instance = fact.createComponentInstance(conf);
            // Should not be executed
            instance.dispose();
            fail("The factory must not create instance without specified topics.");
        } catch (ConfigurationException e) {
            // OK
        } finally {
            fact.stop();
            // Restore the original state of the subscriber
            m_subscriber.addAttribute(m_subscriberTopics);
        }
    }

    /**
     * Try to create a subscriber with malformed topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithMalformedTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the subscriber and replace with a
        // malformed one
        m_subscriber.removeAttribute(m_subscriberTopics);
        Attribute malformedTopics = new Attribute("topics",
                "| |\\| \\/ /-\\ |_ | |)");
        m_subscriber.addAttribute(malformedTopics);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when invalid topics are specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the subscriber
            m_subscriber.removeAttribute(malformedTopics);
            m_subscriber.addAttribute(m_subscriberTopics);
        }
    }

    /**
     * Try to create a subscriber with malformed instance topics.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithMalformedInstanceTopics()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the topics attribute of the subscriber and replace with a
        // malformed one
        m_subscriber.removeAttribute(m_subscriberTopics);
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        fact.start();

        // Try to create an instance with malformed specified topics
        Dictionary conf = new Hashtable();
        conf.put("instance.name","consumer with malformed topics");
        Dictionary topics = new Hashtable();
        topics.put("donut-subscriber", "| |\\| \\/ /-\\ |_ | |)");
        conf.put("event.topics", topics);

        ComponentInstance instance;
        try {
            instance = fact.createComponentInstance(conf);
            // Should not be executed
            instance.dispose();
            fail("The factory must not create instance with invalid specified topics.");
        } catch (ConfigurationException e) {
            // OK
        } finally {
            fact.stop();
            // Restore the original state of the subscriber
            m_subscriber.addAttribute(m_subscriberTopics);
        }
    }

    /**
     * Try to create a subscriber with unknown data type.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithUnknownDataType()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the data-type attribute of the subscriber and replace with a
        // malformed one
        m_subscriber.removeAttribute(m_subscriberDataType);
        Attribute unknownType = new Attribute("data-type", "org.unknown.Clazz");
        m_subscriber.addAttribute(unknownType);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when unknown data type is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the subscriber
            m_subscriber.removeAttribute(unknownType);
            m_subscriber.addAttribute(m_subscriberDataType);
        }
    }

    /**
     * Try to create a subscriber with a data type that does not match with the
     * callback parameter type.
     * 
     * @throws ConfigurationException
     *             something bad happened
     * @throws MissingHandlerException
     *             something bad happened
     * @throws UnacceptableConfiguration
     *             something bad happened
     */
    public void testSubscriberWithUnappropriatedDataType()
        throws ConfigurationException, MissingHandlerException,
        UnacceptableConfiguration {

        // Remove the data-type attribute of the subscriber and replace with a
        // malformed one
        m_subscriber.removeAttribute(m_subscriberDataType);
        Attribute unknownType = new Attribute("data-type", "java.lang.String");
        m_subscriber.addAttribute(unknownType);

        // Create and try to start the factory
        ComponentFactory fact = new ComponentFactory(context, m_consumer);
        try {
            fact.start();
            // Should not be executed
            fact.stop();
            fail("The factory must not start when unappropriated data type is specified.");
        } catch (IllegalStateException e) {
            // OK
        } finally {
            // Restore the original state of the subscriber
            m_subscriber.removeAttribute(unknownType);
            m_subscriber.addAttribute(m_subscriberDataType);
        }
    }

    /**
     * Finalization after test cases.
     * 
     * Release all services references and destroy instances.
     */
    public void tearDown() {

    }

    // DEBUG
    public void dumpElement(String message, Element root) {
        System.err.println(message + "\n" + dumpElement(0, root));
    }

    // DEBUG
    private String dumpElement(int level, Element element) {
        StringBuilder sb = new StringBuilder();
        // Enter tag
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append('<');
        sb.append(element.getName());
        Attribute[] attributes = element.getAttributes();
        for (int i = 0; i < attributes.length; i++) {
            Attribute attribute = attributes[i];
            sb.append(' ');
            sb.append(attribute.getName());
            sb.append('=');
            sb.append(attribute.getValue());
        }
        sb.append(">\n");
        // Children
        Element[] elements = element.getElements();
        for (int i = 0; i < elements.length; i++) {
            sb.append(dumpElement(level + 1, elements[i]));
        }
        // Exit tag
        for (int i = 0; i < level; i++) {
            sb.append("  ");
        }
        sb.append("</" + element.getName() + ">\n");
        return sb.toString();
    }
    
    /**
     * Creates a subscriber listening on a pattern topic (ending with '*').
     * @throws ConfigurationException something bad happened.
     * @throws MissingHandlerException something bad happened.
     * @throws UnacceptableConfiguration something bad happened.
     */
    public void testSubscriberWithPatternTopic() throws UnacceptableConfiguration, MissingHandlerException, ConfigurationException {
    	Dictionary properties = new Hashtable();
    	Dictionary topics = new Hashtable();

        // Create the donut consumer instance, listening on a pattern topic
        properties.put("instance.name","subscriber with pattern topic");
        topics.put("donut-subscriber", "a/pattern/topic/*rf");
        properties.put("event.topics", topics);
        ComponentInstance instance = null;
        try {
        	instance = m_utils.getDonutConsumerFactory()
            .createComponentInstance(properties);
        	
        	// Should not been executed
        	instance.dispose();
        	 fail("An invalid topic scope was accepted)");
        	 
        } catch (ConfigurationException e) {
        	// Nothing to do
        }
        
    	properties = new Hashtable();
    	topics = new Hashtable();

        // Create the donut consumer instance, listening on a pattern topic
        properties.put("instance.name","subscriber with pattern topic");
        topics.put("donut-subscriber", "a/pattern/*topic/rf");
        properties.put("event.topics", topics);
        
        try {
        	instance = m_utils.getDonutConsumerFactory()
            .createComponentInstance(properties);
        	instance.dispose();
        	fail("An invalid topic scope was accepted (2)");
        } catch (ConfigurationException e) {
        	// Nothing to do
        }
    }

}
