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
import org.apache.karaf.util.TemplateUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.jms.*;

import java.io.*;
import java.lang.IllegalStateException;
import java.lang.reflect.Method;
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
    private Path deployFolder;
    
    public JmsServiceImpl() {
        deployFolder = Paths.get(System.getProperty("karaf.base"), "deploy");
    }

    @Override
    public void create(String name, String type, String url) throws Exception {
        create(name, type, url, null, null);
    }

    @Override
    public void create(String name, String type, String url, String username, String password) throws Exception {
        if (!type.equalsIgnoreCase("activemq")
                && !type.equalsIgnoreCase("artemis")
                && !type.equalsIgnoreCase("webspheremq")) {
            throw new IllegalArgumentException("JMS connection factory type not known");
        }

        Path outFile = getConnectionFactoryFile(name);
        String template;
        HashMap<String, String> properties = new HashMap<>();
        properties.put("name", name);

        if (type.equalsIgnoreCase("activemq")) {
            // activemq
            properties.put("url", url);
            properties.put("username", username);
            properties.put("password", password);
            template = "connectionfactory-activemq.xml";
        } else if (type.equalsIgnoreCase("artemis")) {
            // artemis
            properties.put("url", url);
            properties.put("username", username);
            properties.put("password", password);
            template = "connectionfactory-artemis.xml";
        } else {
            // webspheremq
            String[] splitted = url.split("/");
            if (splitted.length != 4) {
                throw new IllegalStateException("WebsphereMQ URI should be in the following format: host/port/queuemanager/channel");
            }
            
            properties.put("host", splitted[0]);
            properties.put("port", splitted[1]);
            properties.put("queuemanager", splitted[2]);
            properties.put("channel", splitted[3]);
            template = "connectionfactory-webspheremq.xml";
        }
        InputStream is = this.getClass().getResourceAsStream(template);
        if (is == null) {
            throw new IllegalArgumentException("Template resource " + template + " doesn't exist");
        }
        TemplateUtils.createFromTemplate(outFile.toFile(), is, properties);
    }

    private Path getConnectionFactoryFile(String name) {
        return deployFolder.resolve("connectionfactory-" + name + ".xml");
    }

    @Override
    public void delete(String name) throws Exception {
        Path connectionFactoryFile = getConnectionFactoryFile(name);
        if (!Files.isRegularFile(connectionFactoryFile)) {
            throw new IllegalStateException("The JMS connection factory file " + connectionFactoryFile + " doesn't exist");
        }
        Files.delete(connectionFactoryFile);
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
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            ConnectionMetaData metaData = connector.connect().getMetaData();
            Map<String, String> map = new HashMap<>();
            map.put("product", metaData.getJMSProviderName());
            map.put("version", metaData.getProviderVersion());
            return map;
        }
    }

    @Override
    public int count(String connectionFactory, final String destination, String username, String password) throws IOException, JMSException {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            Session session = connector.createSession();
            QueueBrowser browser = session.createBrowser(session.createQueue(destination));
            @SuppressWarnings("unchecked")
            Enumeration<Message> enumeration = browser.getEnumeration();
            int count = 0;
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                count++;
            }
            browser.close();
            return count;
        }
    }

    private DestinationSource getDestinationSource(Connection connection) throws JMSException {
        while (true) {
            try {
                Method mth = connection.getClass().getMethod("getConnection");
                connection = (Connection) mth.invoke(connection);
            } catch (Throwable e) {
                break;
            }
        }
        List<DestinationSource.Factory> factories = Arrays.asList(
                new ActiveMQDestinationSourceFactory(),
                new ArtemisDestinationSourceFactory()
        );
        DestinationSource source = null;
        for (DestinationSource.Factory factory : factories) {
            source = factory.create(connection);
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
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            return getDestinationSource(connector.connect()).getNames(DestinationSource.DestinationType.Queue);
        }
    }

    @Override
    public List<String> topics(String connectionFactory, String username, String password) throws IOException, JMSException {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            return getDestinationSource(connector.connect()).getNames(DestinationSource.DestinationType.Topic);
        }
    }

    @Override
    public List<JmsMessage> browse(String connectionFactory, final String queue, final String filter,
                                   String username, String password) throws JMSException, IOException {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            List<JmsMessage> messages = new ArrayList<>();
            Session session = connector.createSession();
            QueueBrowser browser = session.createBrowser(session.createQueue(queue), filter);
            @SuppressWarnings("unchecked")
            Enumeration<Message> enumeration = browser.getEnumeration();
            while (enumeration.hasMoreElements()) {
                Message message = enumeration.nextElement();

                messages.add(new JmsMessage(message));
            }
            browser.close();
            return messages;
        }
    }

    @Override
    public void send(String connectionFactory, final String queue, final String body, final String replyTo,
                     String username, String password) throws IOException, JMSException {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            Session session = connector.createSession();
            Message message = session.createTextMessage(body);
            if (replyTo != null) {
                message.setJMSReplyTo(session.createQueue(replyTo));
            }
            MessageProducer producer = session.createProducer(session.createQueue(queue));
            producer.send(message);
            producer.close();
        }
    }

    @Override
    public int consume(String connectionFactory, final String queue, final String selector, String username,
                       String password) throws Exception {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            int count = 0;
            Session session = connector.createSession();
            MessageConsumer consumer = session.createConsumer(session.createQueue(queue), selector);
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

    @Override
    public int move(String connectionFactory, final String sourceQueue, final String targetQueue,
                    final String selector, String username, String password) throws IOException, JMSException {
        try (JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password)) {
            int count = 0;
            Session session = connector.createSession(Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue(sourceQueue), selector);
            Message message;
            do {
                message = consumer.receive(500L);
                if (message != null) {
                    MessageProducer producer = session.createProducer(session.createQueue(targetQueue));
                    producer.send(message);
                    count++;
                }
            } while (message != null);
            session.commit();
            consumer.close();
            return count;
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}