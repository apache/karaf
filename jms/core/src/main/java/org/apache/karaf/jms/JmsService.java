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
     * @return the list of JMS connection factory names.
     * @throws Exception
     */
    List<String> connectionFactories() throws Exception;

    /**
     * List the JMS connecion factories file names.
     *
     * @return the list of JMS connection factory file names.
     * @throws Exception
     */
    List<String> connectionFactoryFileNames() throws Exception;

    /**
     * Create a new JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @param type the JMS connection factory type (ActiveMQ, WebsphereMQ, ...).
     * @param url the JMS URL to use.
     * @throws Exception
     */
    void create(String name, String type, String url) throws Exception;

    /**
     * Create a new JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @param type the JMS connection factory type (ActiveMQ, WebsphereMQ, ...).
     * @param url the JMS URL to use.
     * @param username the username to use.
     * @param password the password to use.
     * @throws Exception
     */
    void create(String name, String type, String url, String username, String password) throws Exception;

    /**
     * Delete a JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @throws Exception
     */
    void delete(String name) throws Exception;

    /**
     * Get details about a given JMS connection factory.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return a map (property/value) containing details.
     * @throws Exception
     */
    Map<String, String> info(String connectionFactory, String username, String password) throws Exception;

    /**
     * Count the number of messages in a JMS queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the queue name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the number of messages in a JMS queue.
     * @throws Exception
     */
    int count(String connectionFactory, String queue, String username, String password) throws Exception;

    /**
     * List the queues.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the list of queues.
     * @throws Exception
     */
    List<String> queues(String connectionFactory, String username, String password) throws Exception;

    /**
     * List the topics.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the list of topics.
     * @throws Exception
     */
    List<String> topics(String connectionFactory, String username, String password) throws Exception;

    /**
     * Browse a destination.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the queue name.
     * @param selector the selector.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the list of messages.
     * @throws Exception
     */
    List<JmsMessage> browse(String connectionFactory, String queue, String selector, String username, String password) throws Exception;

    /**
     * Send a message on the given queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the queue name.
     * @param body the message body.
     * @param replyTo the message replyTo header.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @throws Exception
     */
    void send(String connectionFactory, String queue, String body, String replyTo, String username, String password) throws Exception;

    /**
     * Consume messages from a given destination.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the queue name.
     * @param selector the messages selector.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the number of messages consumed.
     * @throws Exception
     */
    int consume(String connectionFactory, String queue, String selector, String username, String password) throws Exception;

    /**
     * Move messages from a destination to another.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param sourceQueue the source queue.
     * @param targetQueue the target queue.
     * @param selector the messages selector on the source queue.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the number of messages moved.
     * @throws Exception
     */
    int move(String connectionFactory, String sourceQueue, String targetQueue, String selector, String username, String password) throws Exception;

}
