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

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueRequestor;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import java.io.StringReader;
import java.util.Collections;
import java.util.List;

class ArtemisDestinationSourceFactory implements DestinationSource.Factory {

    @Override
    public DestinationSource create(Connection connection) throws JMSException {
        if (connection.getClass().getName().matches("org\\.apache\\.activemq\\.artemis\\.jms\\.client\\.ActiveMQ(XA)?Connection")) {
            return type -> getNames(connection, type);
        }
        return null;
    }

    private List<String> getNames(Connection connection, DestinationSource.DestinationType type) {
        try {
            QueueSession session = ((QueueConnection) connection).createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue managementQueue = session.createQueue("activemq.management");
            QueueRequestor requestor = new QueueRequestor(session, managementQueue);
            connection.start();
            TextMessage m = session.createTextMessage();
            m.setStringProperty("_AMQ_ResourceName", "broker");
            m.setStringProperty("_AMQ_OperationName", "getQueueNames");
            String routing = type == DestinationSource.DestinationType.Queue ? "ANYCAST" : "MULTICAST";
            m.setText("[\"" + routing + "\"]");
            Message reply = requestor.request(m);
            String json = ((TextMessage) reply).getText();
            List<?> array = (List<?>) JsonReader.read(new StringReader(json));
            return (List<String>) array.get(0);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
