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

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.Topic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class ActiveMQDestinationSourceFactory implements DestinationSource.Factory {

    @Override
    public DestinationSource create(Connection connection) throws JMSException {
        if (connection.getClass().getName().matches("org\\.apache\\.activemq\\.ActiveMQ(XA)?Connection")) {
            try {
                final Object destSource = connection.getClass().getMethod("getDestinationSource").invoke(connection);
                return type -> getNames(destSource, type);
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    private List<String> getNames(Object destSource, DestinationSource.DestinationType type) {
        try {
            if (type == DestinationSource.DestinationType.Queue) {
                @SuppressWarnings("unchecked")
                Set<Queue> queues = (Set) destSource.getClass().getMethod("getQueues").invoke(destSource);
                List<String> names = new ArrayList<>();
                for (Queue queue : queues) {
                    names.add(queue.getQueueName());
                }
                return names;
            }
            if (type == DestinationSource.DestinationType.Topic) {
                @SuppressWarnings("unchecked")
                Set<Topic> topics = (Set) destSource.getClass().getMethod("getTopics").invoke(destSource);
                List<String> names = new ArrayList<>();
                for (Topic topic : topics) {
                    names.add(topic.getTopicName());
                }
                return names;
            }
        } catch (Exception e) {
            // Ignore
        }
        return Collections.emptyList();
    }
}
