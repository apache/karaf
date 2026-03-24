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

import org.apache.karaf.util.json.JsonReader;

import javax.jms.ConnectionMetaData;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.TextMessage;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

class ArtemisDestinationSourceFactory implements DestinationSource.Factory {

    @Override
    public DestinationSource create(JMSContext context) {
        try {
            ConnectionMetaData cmd = context.getMetaData();
            if (cmd.getJMSProviderName().equals("ActiveMQ") && cmd.getProviderVersion().startsWith("2.")) {
                return type -> getNames(context, type);
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }

    private List<String> getNames(JMSContext context, DestinationSource.DestinationType type) {
        try {
            Queue managementQueue = context.createQueue("activemq.management");
            Queue replyTo = context.createTemporaryQueue();

            context.start();

            String routing = type == DestinationSource.DestinationType.Queue ? "ANYCAST" : "MULTICAST";
            context.createProducer()
                    .setProperty("_AMQ_ResourceName", "broker")
                    .setProperty("_AMQ_OperationName", "getQueueNames")
                    .setJMSReplyTo(replyTo)
                    .send(managementQueue, "[\"" + routing + "\"]");
            try (JMSConsumer consumer = context.createConsumer(replyTo)) {
                Message reply = consumer.receive(500);
                String json = ((TextMessage) reply).getText();
                List<?> array = (List<?>) JsonReader.read(new StringReader(json));
                return (List<String>) array.get(0);
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
