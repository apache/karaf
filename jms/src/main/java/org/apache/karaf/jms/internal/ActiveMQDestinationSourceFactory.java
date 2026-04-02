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

import jakarta.jms.Connection;
import jakarta.jms.ConnectionMetaData;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class ActiveMQDestinationSourceFactory implements DestinationSource.Factory {

    @Override
    public DestinationSource create(Connection connection) {
        try {
            ConnectionMetaData cmd = connection.getMetaData();
            if (cmd.getJMSProviderName().equals("ActiveMQ") && (cmd.getProviderVersion().startsWith("5.") || cmd.getProviderVersion().startsWith("6."))) {
                return type -> getNames(connection, type);
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }

    private List<String> getNames(Connection connection, DestinationSource.DestinationType type) {
        try {
            // Start the connection so advisory message consumers can receive
            connection.start();

            // Call connection.getDestinationSource() via reflection (public method)
            Method getDestinationSource = connection.getClass().getMethod("getDestinationSource");
            Object destSource = getDestinationSource.invoke(connection);

            String methodName = type == DestinationSource.DestinationType.Queue ? "getQueues" : "getTopics";
            Method getter = destSource.getClass().getMethod(methodName);

            // Poll for advisory messages to be processed (up to 5 seconds)
            Set<?> destinations = Collections.emptySet();
            for (int i = 0; i < 25; i++) {
                Thread.sleep(200);
                destinations = (Set<?>) getter.invoke(destSource);
                if (!destinations.isEmpty()) {
                    break;
                }
            }

            // Use reflection to get names since ActiveMQ destinations implement
            // javax.jms.Queue/Topic, not jakarta.jms.Queue/Topic
            String nameMethod = type == DestinationSource.DestinationType.Queue ? "getQueueName" : "getTopicName";
            List<String> names = new ArrayList<>();
            for (Object dest : destinations) {
                Method getName = dest.getClass().getMethod(nameMethod);
                names.add((String) getName.invoke(dest));
            }

            // Stop the destination source listener
            Method stop = destSource.getClass().getMethod("stop");
            stop.invoke(destSource);

            return names;
        } catch (Exception e) {
            // Ignore
        }
        return Collections.emptyList();
    }

}
