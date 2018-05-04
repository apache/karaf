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

import java.util.List;
import java.util.Map;

/**
 * JMS Service.
 */
public interface JmsService {

    /**
     * List the JMS connection factories.
     *
     * @return The {@link List} of JMS connection factory names.
     * @throws Exception If the service fails.
     */
    List<String> connectionFactories() throws Exception;

    /**
     * List the JMS connection factories file names.
     *
     * @return The {@link List} of JMS connection factory file names.
     * @throws Exception If the service fails.
     */
    List<String> connectionFactoryFileNames() throws Exception;

    /**
     * Create a new JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @param type The JMS connection factory type (ActiveMQ, WebsphereMQ, ...).
     * @param url The JMS URL to use.
     * @throws Exception If the service fails.
     */
    void create(String name, String type, String url) throws Exception;

    /**
     * Create a new JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @param type The JMS connection factory type (ActiveMQ, WebsphereMQ, ...).
     * @param url The JMS URL to use.
     * @param username The username to use.
     * @param password The password to use.
     * @param pool Kind of pool to use.
     * @throws Exception If the service fails.
     */
    void create(String name, String type, String url, String username, String password, String pool) throws Exception;

    /**
     * Delete a JMS connection factory.
     *
     * @param name The JMS connection factory name.
     * @throws Exception If the service fails.
     */
    void delete(String name) throws Exception;

    /**
     * Get details about a given JMS connection factory.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return A {@link Map} (property/value) containing details.
     * @throws Exception If the service fails.
     */
    Map<String, String> info(String connectionFactory, String username, String password) throws Exception;

    /**
     * Count the number of messages in a JMS queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The queue name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages in a JMS queue.
     * @throws Exception If the service fails.
     */
    int count(String connectionFactory, String queue, String username, String password) throws Exception;

    /**
     * List the queues.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The {@link List} of queues.
     * @throws Exception If the service fails.
     */
    List<String> queues(String connectionFactory, String username, String password) throws Exception;

    /**
     * List the topics.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The {@link List} of topics.
     * @throws Exception If the service fails.
     */
    List<String> topics(String connectionFactory, String username, String password) throws Exception;

    /**
     * Browse a destination.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The queue name.
     * @param selector The selector.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The {@link List} of messages.
     * @throws Exception If the service fails.
     */
    List<JmsMessage> browse(String connectionFactory, String queue, String selector, String username, String password) throws Exception;

    /**
     * Send a message on the given queue.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The queue name.
     * @param body The message body.
     * @param replyTo The message replyTo header.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @throws Exception If the service fails.
     */
    void send(String connectionFactory, String queue, String body, String replyTo, String username, String password) throws Exception;

    /**
     * Consume messages from a given destination.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param queue The queue name.
     * @param selector The messages selector.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages consumed.
     * @throws Exception If the service fails.
     */
    int consume(String connectionFactory, String queue, String selector, String username, String password) throws Exception;

    /**
     * Move messages from a destination to another.
     *
     * @param connectionFactory The JMS connection factory name.
     * @param sourceQueue The source queue.
     * @param targetQueue The target queue.
     * @param selector The messages selector on the source queue.
     * @param username The (optional) username to connect to the JMS broker.
     * @param password The (optional) password to connect to the JMS broker.
     * @return The number of messages moved.
     * @throws Exception If the service fails.
     */
    int move(String connectionFactory, String sourceQueue, String targetQueue, String selector, String username, String password) throws Exception;

}
