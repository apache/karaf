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
     * @return the list of the JMS connection factories name.
     * @throws MBeanException
     */
    List<String> getConnectionfactories() throws MBeanException;

    /**
     * Create a JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @param type the JMS connection factory type (ActiveMQ or WebsphereMQ).
     * @param url the JMS connection factory URL. NB: when type is WebsphereMQ, the URL has the format host/port/queuemanager/channel.
     * @throws MBeanException
     */
    void create(String name, String type, String url) throws MBeanException;

    /**
     * Create a JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @param type the JMS connection factory type (ActiveMQ or WebsphereMQ).
     * @param url the JMS connection factory URL. NB: when type is WebsphereMQ, the URL has the format host/port/queuemanager/channel.
     * @param username the JMS connection factory authentication username.
     * @param password the JMS connection factory authentication password.
     * @throws MBeanException
     */
    void create(String name, String type, String url, String username, String password) throws MBeanException;

    /**
     * Delete a JMS connection factory.
     *
     * @param name the JMS connection factory name.
     * @throws MBeanException
     */
    void delete(String name) throws MBeanException;

    /**
     * Get details about a JMS connection factory.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return a map (property/value) containing details.
     * @throws MBeanException
     */
    Map<String, String> info(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * Count the messages on a given JMS queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the JMS queue name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return
     * @throws MBeanException
     */
    int count(String connectionFactory, String queue, String username, String password) throws MBeanException;

    /**
     * List the JMS queues.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the list of JMS queues.
     * @throws MBeanException
     */
    List<String> queues(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * List the JMS topics.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the list of JMS topics.
     * @throws MBeanException
     */
    List<String> topics(String connectionFactory, String username, String password) throws MBeanException;

    /**
     * Browse the messages in a JMS queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the JMS queue name.
     * @param selector a selector to use to browse only certain messages.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return a tabular data with messages details.
     * @throws MBeanException
     */
    TabularData browse(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException;

    /**
     * Send a JMS message to given queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the JMS queue name.
     * @param content the message content.
     * @param replyTo the message ReplyTo.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @throws MBeanException
     */
    void send(String connectionFactory, String queue, String content, String replyTo, String username, String password) throws MBeanException;

    /**
     * Consume JMS messages from a given queue.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param queue the JMS queue name.
     * @param selector a selector to use to consume only certain messages.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the number of messages consumed.
     * @throws MBeanException
     */
    int consume(String connectionFactory, String queue, String selector, String username, String password) throws MBeanException;

    /**
     * Move JMS messages from one queue to another.
     *
     * @param connectionFactory the JMS connection factory name.
     * @param source the source JMS queue name.
     * @param destination the destination JMS queue name.
     * @param selector a selector to move only certain messages.
     * @param username optional username to connect to the JMS broker.
     * @param password optional password to connect to the JMS broker.
     * @return the number of messages moved.
     * @throws MBeanException
     */
    int move(String connectionFactory, String source, String destination, String selector, String username, String password) throws MBeanException;

}
