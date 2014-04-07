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

    public void create(String name, String type, String url) throws Exception {
        if (!type.equalsIgnoreCase("activemq") && !type.equalsIgnoreCase("webspheremq")) {
            throw new IllegalArgumentException("JMS connection factory type not known");
        }

        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File  outFile = new File(deployFolder, "connectionfactory-" + name + ".xml");

        if (type.equalsIgnoreCase("activemq")) {
            // activemq
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put("${name}", name);
            properties.put("${url}", url);
            copyDataSourceFile(outFile, "connectionfactory-activemq.xml", properties);
        } else {
            // webspheremq
            String[] splitted = url.split("/");
            if (splitted.length != 4) {
                throw new IllegalStateException("WebsphereMQ URI should be in the following format: host/port/queuemanager/channel");
            }
            HashMap<String, String> properties = new HashMap<String, String>();
            properties.put("${name}", name);
            properties.put("${host}", splitted[0]);
            properties.put("${port}", splitted[1]);
            properties.put("${queuemanager}", splitted[2]);
            properties.put("${channel}", splitted[3]);
            copyDataSourceFile(outFile, "connectionfactory-webspheremq.xml", properties);
        }
    }

    public void delete(String name) throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");
        File connectionFactoryFile = new File(deployFolder, "connectionfactory-" + name + ".xml");
        if (!connectionFactoryFile.exists()) {
            throw new IllegalStateException("The JMS connection factory file " + connectionFactoryFile.getPath() + " doesn't exist");
        }
        connectionFactoryFile.delete();
    }

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

    public List<String> connectionFactoryFileNames() throws Exception {
        File karafBase = new File(System.getProperty("karaf.base"));
        File deployFolder = new File(karafBase, "deploy");

        String[] connectionFactoryFileNames = deployFolder.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith("connectionfactory-") && name.endsWith(".xml");
            }
        });

        return Arrays.asList(connectionFactoryFileNames);
    }

    public Map<String, String> info(String connectionFactory, String username, String password) throws Exception {
        Map<String, String> map = new HashMap<String, String>();
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            ConnectionMetaData metaData = connection.getMetaData();
            map.put("product", metaData.getJMSProviderName());
            map.put("version", metaData.getProviderVersion());
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return map;
    }

    public int count(String connectionFactory, String destination, String username, String password) throws Exception {
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
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
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
    }

    public List<String> queues(String connectionFactory, String username, String password) throws Exception {
        List<String> queues = new ArrayList<String>();
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            if (connection instanceof PooledConnection) {
                connection = ((PooledConnection) connection).getConnection();
            }
            if (connection instanceof ActiveMQConnection) {
                DestinationSource destinationSource = ((ActiveMQConnection) connection).getDestinationSource();
                Set<ActiveMQQueue> activeMQQueues = destinationSource.getQueues();
                for (ActiveMQQueue activeMQQueue : activeMQQueues) {
                    queues.add(activeMQQueue.getQueueName());
                }
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return queues;
    }

    public List<String> topics(String connectionFactory, String username, String password) throws Exception {
        List<String> topics = new ArrayList<String>();
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            if (connection instanceof PooledConnection) {
                connection = ((PooledConnection) connection).getConnection();
            }
            if (connection instanceof ActiveMQConnection) {
                DestinationSource destinationSource = ((ActiveMQConnection) connection).getDestinationSource();
                Set<ActiveMQTopic> activeMQTopics = destinationSource.getTopics();
                for (ActiveMQTopic activeMQTopic : activeMQTopics) {
                    topics.add(activeMQTopic.getTopicName());
                }
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return topics;
    }

    public List<JmsMessage> browse(String connectionFactory, String queue, String filter, String username, String password) throws Exception {
        List<JmsMessage> messages = new ArrayList<JmsMessage>();
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueBrowser browser = session.createBrowser(session.createQueue(queue), filter);
            Enumeration<Message> enumeration = browser.getEnumeration();
            while (enumeration.hasMoreElements()) {
                Message message = enumeration.nextElement();

                messages.add(new JmsMessage(message));
            }
            browser.close();
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return messages;
    }

    public void send(String connectionFactory, String queue, String body, String replyTo, String username, String password) throws Exception {
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Message message = session.createTextMessage(body);
            if (replyTo != null) {
                message.setJMSReplyTo(session.createQueue(replyTo));
            }
            MessageProducer producer = session.createProducer(session.createQueue(queue));
            producer.send(message);
            producer.close();
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
    }

    public int consume(String connectionFactory, String queue, String selector, String username, String password) throws Exception {
        int count = 0;
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageConsumer consumer = session.createConsumer(session.createQueue(queue), selector);
            Message message;
            do {
                message = consumer.receive(5000L);
                if (message != null) {
                    count++;
                }
            } while (message != null);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return count;
    }

    public int move(String connectionFactory, String sourceQueue, String targetQueue, String selector, String username, String password) throws Exception {
        int count = 0;
        ServiceReference reference = this.lookupConnectionFactory(connectionFactory);
        Connection connection = null;
        Session session = null;
        try {
            ConnectionFactory cf = (ConnectionFactory) bundleContext.getService(reference);
            connection = cf.createConnection(username, password);
            connection.start();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
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
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
            if (reference != null) {
                bundleContext.ungetService(reference);
            }
        }
        return count;
    }

    private ServiceReference lookupConnectionFactory(String name) throws Exception {
        ServiceReference[] references = bundleContext.getServiceReferences(ConnectionFactory.class.getName(), "(|(osgi.jndi.service.name=" + name + ")(name=" + name + ")(service.id=" + name + "))");
        if (references == null || references.length == 0) {
            throw new IllegalArgumentException("No JMS connection factory found for " + name);
        }
        if (references.length > 1) {
            throw new IllegalArgumentException("Multiple JMS connection factories found for " + name);
        }
        return references[0];
    }

    private void copyDataSourceFile(File outFile, String resource, HashMap<String, String> properties) throws Exception {
        if (!outFile.exists()) {
            InputStream is = JmsServiceImpl.class.getResourceAsStream(resource);
            if (is == null) {
                throw new IllegalArgumentException("Resource " + resource + " doesn't exist");
            }
            try {
                // read it line at a time so that we can use the platform line ending when we write it out
                PrintStream out = new PrintStream(new FileOutputStream(outFile));
                try {
                    Scanner scanner = new Scanner(is);
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        line = filter(line, properties);
                        out.println(line);
                    }
                } finally {
                    safeClose(out);
                }
            } finally {
                safeClose(is);
            }
        } else {
            throw new IllegalArgumentException("File " + outFile.getPath() + " already exists. Remove it if you wish to recreate it.");
        }
    }

    private void safeClose(InputStream is) throws IOException {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Throwable ignore) {
            // nothing to do
        }
    }

    private void safeClose(OutputStream is) throws IOException {
        if (is == null)
            return;
        try {
            is.close();
        } catch (Throwable ignore) {
            // nothing to do
        }
    }

    private String filter(String line, HashMap<String, String> props) {
        for (Map.Entry<String, String> i : props.entrySet()) {
            int p1 = line.indexOf(i.getKey());
            if (p1 >= 0) {
                String l1 = line.substring(0, p1);
                String l2 = line.substring(p1 + i.getKey().length());
                line = l1 + i.getValue() + l2;
            }
        }
        return line;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

}
