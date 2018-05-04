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
package org.apache.karaf.jms;

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;
import java.util.List;
import java.util.Map;

/**
 * JMS MBean.
 */
public interface JmsMBean {

    /**
     * List the JMS connection factories.
     *
     * @return The {@link List} of the JMS connection factories name.
     * @throws MBeanException If the MBean fails.
     */
    List<String> getConnectionfactories() throws MBeanException;

    /**
     * Create a JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @param type The JMS connection factory type (ActiveMQ or WebsphereMQ).
     * @param url The JMS connection factory URL. NB: when type is WebsphereMQ, the URL has the format host/port/queuemanager/channel.
     * @throws MBeanException If the MBean fails.
     */
    void create(String name, String type, String url) throws MBeanException;

    /**
     * Create a JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @param type The JMS connection factory type (ActiveMQ or WebsphereMQ).
     * @param url The JMS connection factory URL. NB: when type is WebsphereMQ, the URL has the format host/port/queuemanager/channel.
     * @param username The JMS connection factory authentication username.
     * @param password The JMS connection factory authentication password.
     * @param pool The JMS connection factory pooling to use.
     * @throws MBeanException If the MBean fails.
     */
    void create(String name, String type, String url, String username, String password, String pool) throws MBeanException;

    /**
     * Delete a JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @throws MBeanException If the MBean fails.
     */
    void delete(String name) throws MBeanException;

    /**
     * Get details about a JMS connection factory.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return A {@link Map} (property/value) containing details.
     * @throws MBeanException If the MBean fails.
     */
    Map<String, String> info(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * Count the messages on a given JMS queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The JMS queue name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages in the queue.
     * @throws MBeanException If the MBean fails.
     */
    int count(String connectionFactory, String queue, String username, String password) throws MBeanException;

    /**
     * List the JMS queues.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The {@link List} of JMS queues.
     * @throws MBeanException If the MBean fails.
     */
    List<String> queues(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * List the JMS topics.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The @link List} of JMS topics.
     * @throws MBeanException If the MBean fails.
     */
    List<String> topics(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * Browse the messages in a JMS queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The JMS queue name.
     * @param selector A selector to use to browse only certain messages.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return A {@link TabularData} containing messages details.
     * @throws MBeanException If the MBean fails.
     */
    TabularData browse(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException;

    /**
     * Send a JMS message to given queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The JMS queue name.
     * @param content The message content.
     * @param replyTo The message ReplyTo.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @throws MBeanException If the MBean fails.
     */
    void send(String connectionFactory, String queue, String content, String replyTo, String username, String password) throws MBeanException;

    /**
     * Consume JMS messages from a given queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The JMS queue name.
     * @param selector A selector to use to consume only certain messages.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages consumed.
     * @throws MBeanException If the MBean fails.
     */
    int consume(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException;

    /**
     * Move JMS messages from one queue to another.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param source The source JMS queue name.
     * @param destination The destination JMS queue name.
     * @param selector A selector to move only certain messages.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages moved.
     * @throws MBeanException If the MBean fails.
     */
    int move(String connectionFactory, String source, String destination, String selector, String username, String password) throws MBeanException;

}
