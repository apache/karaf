/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jms.internal;

import org.apache.karaf.jms.JmsMessage;
import org.apache.karaf.jms.JmsService;
import org.ops4j.pax.jms.service.ConnectionFactoryFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import javax.jms.*;
import javax.jms.Queue;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of the JMS Service.
 */
public class JmsServiceImpl implements JmsService {

    private BundleContext bundleContext;
    private ConfigurationAdmin configAdmin;
    private Path deployFolder;
    
    public JmsServiceImpl() {
        deployFolder = Paths.get(System.getProperty("karaf.base"), "deploy");
    }

    @Override
    public void create(String name, String type, String url) throws Exception {
        create(name, type, url, null, null, "jmspooled");
    }

    @Override
    public void create(String name, String type, String url, String username, String password, String pool) throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("JMS connection factory type not known");
        }

        if (connectionFactories().contains(name)) {
            throw new IllegalArgumentException("There is already a ConnectionFactory with the name " + name);
        }

        Dictionary<String, String> properties = new Hashtable<>();
        properties.put("osgi.jndi.service.name", "jms/" + name);
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, name);
        properties.put(ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_TYPE, type);
        put(properties, ConnectionFactoryFactory.JMS_URL, url);
        put(properties, ConnectionFactoryFactory.JMS_USER, username);
        put(properties, ConnectionFactoryFactory.JMS_PASSWORD, password);
        if (pool.equals("narayana")) {
            put(properties, "pool", "narayana");
        }
        if (pool.equals("transx") || type.equalsIgnoreCase("activemq")) {
            put(properties, "pool", "transx");
        }
        Configuration config = configAdmin.createFactoryConfiguration("org.ops4j.connectionfactory", null);
        config.update(properties);
    }

    private void put(Dictionary<String, String> properties, String key, String value) {
        if (value != null) {
            properties.put(key, value);
        }
    }

    @Override
    public void delete(String name) throws Exception {
        String filter = String.format("(&(service.factoryPid=org.ops4j.connectionfactory)(%s=%s))", ConnectionFactoryFactory.JMS_CONNECTIONFACTORY_NAME, name);
        Configuration[] configs = configAdmin.listConfigurations(filter);
        for (Configuration config : configs) {
            config.delete();
        }
    }

    @Override
    public List<String> connectionFactories() throws Exception {
        return bundleContext.getServiceReferences(ConnectionFactory.class, null).stream()
                .map(this::getConnectionFactoryName)
                .distinct()
                .collect(Collectors.toList());
    }

    private String getConnectionFactoryName(ServiceReference<ConnectionFactory> reference) {
        if (reference.getProperty("osgi.jndi.service.name") != null) {
            return (String) reference.getProperty("osgi.jndi.service.name");
        } else if (reference.getProperty("name") != null) {
            return (String) reference.getProperty("name");
        } else {
            return reference.getProperty(Constants.SERVICE_ID).toString();
        }
    }

    @Override
    public List<String> connectionFactoryFileNames() throws Exception {
        return Files.list(deployFolder)
                .map(Path::getFileName)
                .map(Path::toString)
                .filter(name -> name.startsWith("connectionfactory-") && name.endsWith(".xml"))
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> info(String connectionFactory, String username, String password) throws IOException, JMSException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            ConnectionMetaData metaData = context.getMetaData();
            Map<String, String> map = new HashMap<>();
            map.put("product", metaData.getJMSProviderName());
            map.put("version", metaData.getProviderVersion());
            return map;
        }
    }

    @Override
    public int count(String connectionFactory, final String destination, String username, String password) throws IOException, JMSException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            try (QueueBrowser browser = context.createBrowser(context.createQueue(destination))) {
                @SuppressWarnings("unchecked")
                Enumeration<Message> enumeration = browser.getEnumeration();
                int count = 0;
                while (enumeration.hasMoreElements()) {
                    enumeration.nextElement();
                    count++;
                }
                return count;
            }
        }
    }

    private JMSContext createContext(String name, String username, String password) {
        return createContext(name, username, password, JMSContext.AUTO_ACKNOWLEDGE);
    }

    private JMSContext createContext(String name, String username, String password, int sessionMode) {
        ServiceReference<ConnectionFactory> sr = lookupConnectionFactory(name);
        ConnectionFactory cf = bundleContext.getService(sr);
        try {
            return cf.createContext(username, password, sessionMode);
        } finally {
            bundleContext.ungetService(sr);
        }
    }

    private ServiceReference<ConnectionFactory> lookupConnectionFactory(String name) {
        try {
            Collection<ServiceReference<ConnectionFactory>> references = bundleContext.getServiceReferences(
                    ConnectionFactory.class,
                    "(|(osgi.jndi.service.name=" + name + ")(name=" + name + ")(service.id=" + name + "))");
            return references.stream()
                    .sorted(Comparator.<ServiceReference<?>>naturalOrder().reversed())
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No JMS connection factory found for " + name));
        } catch (InvalidSyntaxException e) {
            throw new RuntimeException("Error finding connection factory service " + name, e);
        }
    }

    private DestinationSource getDestinationSource(JMSContext context) throws JMSException {
        List<DestinationSource.Factory> factories = Arrays.asList(
                new ActiveMQDestinationSourceFactory(),
                new ArtemisDestinationSourceFactory()
        );
        DestinationSource source = null;
        for (DestinationSource.Factory factory : factories) {
            source = factory.create(context);
            if (source != null) {
                break;
            }
        }
        if (source == null) {
            source = d -> Collections.emptyList();
        }
        return source;
    }
    
    @Override
    public List<String> queues(String connectionFactory, String username, String password) throws JMSException, IOException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            return getDestinationSource(context).getNames(DestinationSource.DestinationType.Queue);
        }
    }

    @Override
    public List<String> topics(String connectionFactory, String username, String password) throws IOException, JMSException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            return getDestinationSource(context).getNames(DestinationSource.DestinationType.Topic);
        }
    }

    @Override
    public List<JmsMessage> browse(String connectionFactory, final String queue, final String filter,
                                   String username, String password) throws JMSException, IOException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            try (QueueBrowser browser = context.createBrowser(context.createQueue(queue), filter)) {
                List<JmsMessage> messages = new ArrayList<>();
                @SuppressWarnings("unchecked")
                Enumeration<Message> enumeration = browser.getEnumeration();
                while (enumeration.hasMoreElements()) {
                    Message message = enumeration.nextElement();

                    messages.add(new JmsMessage(message));
                }
                return messages;
            }
        }
    }

    @Override
    public void send(String connectionFactory, final String queue, final String body, final String replyTo,
                     String username, String password) throws IOException, JMSException {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            JMSProducer producer = context.createProducer();
            if (replyTo != null) {
                producer.setJMSReplyTo(context.createQueue(replyTo));
            }
            producer.send(context.createQueue(queue), body);
        }
    }

    @Override
    public int consume(String connectionFactory, final String queue, final String selector, String username,
                       String password) throws Exception {
        try (JMSContext context = createContext(connectionFactory, username, password)) {
            try (JMSConsumer consumer = context.createConsumer(context.createQueue(queue), selector)) {
                int count = 0;
                Message message;
                do {
                    message = consumer.receive(500L);
                    if (message != null) {
                        count++;
                    }
                } while (message != null);
                return count;
            }
        }
    }

    @Override
    public int move(String connectionFactory, final String sourceQueue, final String targetQueue,
                    final String selector, String username, String password) throws IOException, JMSException {
        try (JMSContext context = createContext(connectionFactory, username, password, JMSContext.SESSION_TRANSACTED)) {
            Queue source = context.createQueue(sourceQueue);
            Queue target = context.createQueue(targetQueue);
            try (JMSConsumer consumer = context.createConsumer(source, selector)) {
                int count = 0;
                while (true) {
                    Message message = consumer.receive(500L);
                    if (message != null) {
                        context.createProducer().send(target, message);
                        context.commit();
                        count++;
                    } else {
                        break;
                    }
                }
                return count;
            }
        }
   }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }
}