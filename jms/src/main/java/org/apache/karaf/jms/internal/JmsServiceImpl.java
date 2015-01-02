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

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.advisory.DestinationSource;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.activemq.pool.PooledConnection;
import org.apache.karaf.jms.JmsMessage;
import org.apache.karaf.jms.JmsService;
import org.apache.karaf.util.TemplateUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

import javax.jms.*;

import java.io.*;
import java.lang.IllegalStateException;
import java.util.*;

/**
 * Default implementation of the JMS Service.
 */
public class JmsServiceImpl implements JmsService {

    private BundleContext bundleContext;
    private File deployFolder;
    
    public JmsServiceImpl() {
        File karafBase = new File(System.getProperty("karaf.base"));
        deployFolder = new File(karafBase, "deploy");
    }

    @Override
    public void create(String name, String type, String url) throws Exception {
        create(name, type, url, null, null);
    }

    @Override
    public void create(String name, String type, String url, String username, String password) throws Exception {
        if (!type.equalsIgnoreCase("activemq") && !type.equalsIgnoreCase("webspheremq")) {
            throw new IllegalArgumentException("JMS connection factory type not known");
        }

        File outFile = getConnectionFactoryFile(name);
        String template;
        HashMap<String, String> properties = new HashMap<String, String>();
        properties.put("name", name);

        if (type.equalsIgnoreCase("activemq")) {
            // activemq
            properties.put("url", url);
            properties.put("username", username);
            properties.put("password", password);
            template = "connectionfactory-activemq.xml";
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
        TemplateUtils.createFromTemplate(outFile, is, properties);
    }

    private File getConnectionFactoryFile(String name) {
        return new File(deployFolder, "connectionfactory-" + name + ".xml");
    }

    @Override
    public void delete(String name) throws Exception {
        File connectionFactoryFile = getConnectionFactoryFile(name);
        if (!connectionFactoryFile.exists()) {
            throw new IllegalStateException("The JMS connection factory file " + connectionFactoryFile.getPath() + " doesn't exist");
        }
        connectionFactoryFile.delete();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public List<String> connectionFactories() throws Exception {
        List<String> connectionFactories = new ArrayList<String>();
        ServiceReference[] references = bundleContext.getServiceReferences(ConnectionFactory.class.getName(), null);
        if (references != null) {
            for (ServiceReference reference : references) {
                if (reference.getProperty("osgi.jndi.service.name") != null) {
                    connectionFactories.add((String) reference.getProperty("osgi.jndi.service.name"));
                } else if (reference.getProperty("name") != null) {
                    connectionFactories.add((String) reference.getProperty("name"));
                } else {
                    connectionFactories.add(reference.getProperty(Constants.SERVICE_ID).toString());
                }
            }
        }
        return connectionFactories;
    }

    @Override
    public List<String> connectionFactoryFileNames() throws Exception {
        String[] connectionFactoryFileNames = deployFolder.list(new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("connectionfactory-") && name.endsWith(".xml");
            }
        });

        return Arrays.asList(connectionFactoryFileNames);
    }

    @Override
    public Map<String, String> info(String connectionFactory, String username, String password) throws IOException, JMSException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            ConnectionMetaData metaData = connector.connect().getMetaData();
            Map<String, String> map = new HashMap<String, String>();
            map.put("product", metaData.getJMSProviderName());
            map.put("version", metaData.getProviderVersion());
            return map;
        } finally {
            connector.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public int count(String connectionFactory, final String destination, String username, String password) throws IOException, JMSException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try  {
            Session session = connector.createSession();
            QueueBrowser browser = session.createBrowser(session.createQueue(destination));
            Enumeration<Message> enumeration = browser.getEnumeration();
            int count = 0;
            while (enumeration.hasMoreElements()) {
                enumeration.nextElement();
                count++;
            }
            browser.close();
            return count;
        } finally {
            connector.close();
        }
    }

    private DestinationSource getDestinationSource(Connection connection) throws JMSException {
        if (connection instanceof PooledConnection) {
            connection = ((PooledConnection) connection).getConnection();
        }
        if (connection instanceof ActiveMQConnection) {
            return ((ActiveMQConnection) connection).getDestinationSource();
        } else {
            return null;
        }
    }
    
    @Override
    public List<String> queues(String connectionFactory, String username, String password) throws JMSException, IOException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            List<String> queues = new ArrayList<String>();
            DestinationSource destinationSource = getDestinationSource(connector.connect());
            if (destinationSource != null) {
                Set<ActiveMQQueue> activeMQQueues = destinationSource.getQueues();
                for (ActiveMQQueue activeMQQueue : activeMQQueues) {
                    queues.add(activeMQQueue.getQueueName());
                }
            }
            return queues;
        } finally {
            connector.close();
        }
    }

    @Override
    public List<String> topics(String connectionFactory, String username, String password) throws IOException, JMSException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            DestinationSource destinationSource = getDestinationSource(connector.connect());
            List<String> topics = new ArrayList<String>();
            if (destinationSource != null) {
                Set<ActiveMQTopic> activeMQTopics = destinationSource.getTopics();
                for (ActiveMQTopic activeMQTopic : activeMQTopics) {
                    topics.add(activeMQTopic.getTopicName());
                }
            }
            return topics;
        } finally {
            connector.close();
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<JmsMessage> browse(String connectionFactory, final String queue, final String filter,
                                   String username, String password) throws JMSException, IOException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            List<JmsMessage> messages = new ArrayList<JmsMessage>();
            Session session = connector.createSession();
            QueueBrowser browser = session.createBrowser(session.createQueue(queue), filter);
            Enumeration<Message> enumeration = browser.getEnumeration();
            while (enumeration.hasMoreElements()) {
                Message message = enumeration.nextElement();

                messages.add(new JmsMessage(message));
            }
            browser.close();
            return messages;
        } finally {
            connector.close();
        }
    }

    @Override
    public void send(String connectionFactory, final String queue, final String body, final String replyTo,
                     String username, String password) throws IOException, JMSException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            Session session = connector.createSession();
            Message message = session.createTextMessage(body);
            if (replyTo != null) {
                message.setJMSReplyTo(session.createQueue(replyTo));
            }
            MessageProducer producer = session.createProducer(session.createQueue(queue));
            producer.send(message);
            producer.close();
        } finally {
            connector.close();
        }
    }

    @Override
    public int consume(String connectionFactory, final String queue, final String selector, String username,
                       String password) throws Exception {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            int count = 0;
            Session session = connector.createSession();
            MessageConsumer consumer = session.createConsumer(session.createQueue(queue), selector);
            Message message;
            do {
                message = consumer.receive(5000L);
                if (message != null) {
                    count++;
                }
            } while (message != null);
            return count;
        } finally {
            connector.close();
        }
    }

    @Override
    public int move(String connectionFactory, final String sourceQueue, final String targetQueue,
                    final String selector, String username, String password) throws IOException, JMSException {
        JmsConnector connector = new JmsConnector(bundleContext, connectionFactory, username, password);
        try {
            int count = 0;
            Session session = connector.createSession(Session.SESSION_TRANSACTED);
            MessageConsumer consumer = session.createConsumer(session.createQueue(sourceQueue), selector);
            Message message;
            do {
                message = consumer.receive(5000L);
                if (message != null) {
                    MessageProducer producer = session.createProducer(session.createQueue(targetQueue));
                    producer.send(message);
                    count++;
                }
            } while (message != null);
            session.commit();
            consumer.close();
            return count;
        } finally {
            connector.close();
        }
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}