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

import org.apache.karaf.jms.JmsMBean;
import org.apache.karaf.jms.JmsMessage;
import org.apache.karaf.jms.JmsService;

import javax.management.MBeanException;
import javax.management.openmbean.*;
import java.util.List;
import java.util.Map;

/**
 * Default implementation of the JMS MBean.
 */
public class JmsMBeanImpl implements JmsMBean {

    private JmsService jmsService;

    @Override
    public List<String> getConnectionfactories() throws MBeanException {
        try {
            return jmsService.connectionFactories();
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void create(String name, String type, String url) throws MBeanException {
        try {
            jmsService.create(name, type, url);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void create(String name, String type, String url, String username, String password, String pool) throws MBeanException {
        try {
            jmsService.create(name, type, url, username, password, pool);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void delete(String name) throws MBeanException {
        try {
            jmsService.delete(name);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public Map<String, String> info(String connectionFactory, String username, String password) throws MBeanException {
        try {
            return jmsService.info(connectionFactory, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public int count(String connectionFactory, String queue, String username, String password) throws MBeanException {
        try {
            return jmsService.count(connectionFactory, queue, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public List<String> queues(String connectionFactory, String username, String password) throws MBeanException {
        try {
            return jmsService.queues(connectionFactory, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public List<String> topics(String connectionFactory, String username, String password) throws MBeanException {
        try {
            return jmsService.topics(connectionFactory, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public void send(String connectionFactory, String queue, String content, String replyTo, String username, String password) throws MBeanException {
        try {
            jmsService.send(connectionFactory, queue, content, replyTo, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public int consume(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException {
        try {
            return jmsService.consume(connectionFactory, queue, selector, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public int move(String connectionFactory, String source, String destination, String selector, String username, String password) throws MBeanException {
        try {
            return jmsService.move(connectionFactory, source, destination, selector, username, password);
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    @Override
    public TabularData browse(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException {
        try {
            CompositeType type = new CompositeType("message", "JMS Message",
                    new String[]{ "id", "content", "charset", "type", "correlation", "delivery", "destination", "expiration", "priority", "redelivered", "replyto", "timestamp" },
                    new String[]{ "Message ID", "Content", "Charset", "Type", "Correlation ID", "Delivery Mode", "Destination", "Expiration Date", "Priority", "Redelivered", "Reply-To", "Timestamp" },
                    new OpenType[]{ SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.STRING, SimpleType.INTEGER, SimpleType.BOOLEAN, SimpleType.STRING, SimpleType.STRING });
            TabularType tableType = new TabularType("messages", "JMS Messages", type, new String[]{ "id" });
            TabularData table = new TabularDataSupport(tableType);
            for (JmsMessage message : getJmsService().browse(connectionFactory, queue, selector, username, password)) {
                CompositeData data = new CompositeDataSupport(type,
                        new String[]{ "id", "content", "charset", "type", "correlation", "delivery", "destination", "expiration", "priority", "redelivered", "replyto", "timestamp" },
                        new Object[]{ message.getMessageId(), message.getContent(), message.getCharset(), message.getType(), message.getCorrelationID(), message.getDeliveryMode(), message.getDestination(), message.getExpiration(), message.getPriority(), message.isRedelivered(), message.getReplyTo(), message.getTimestamp() }
                        );
                table.put(data);
            }
            return table;
        } catch (Throwable t) {
            throw new MBeanException(null, t.getMessage());
        }
    }

    public JmsService getJmsService() {
        return jmsService;
    }

    public void setJmsService(JmsService jmsService) {
        this.jmsService = jmsService;
    }

}
