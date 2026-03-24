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

import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.Topic;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class ActiveMQDestinationSourceFactory implements DestinationSource.Factory {

    @Override
    public DestinationSource create(JMSContext context) {
        try {
            ConnectionMetaData cmd = context.getMetaData();
            if (cmd.getJMSProviderName().equals("ActiveMQ") && cmd.getProviderVersion().startsWith("5.")) {
                return type -> getNames(context, type);
            }
        } catch (Throwable t) {
            // Ignore
        }
        return null;
    }

    private List<String> getNames(JMSContext context, DestinationSource.DestinationType type) {
        try {
            List<String> names = new ArrayList<>();
            context.start();
            String dest = "ActiveMQ.Advisory." +
                    (type == DestinationSource.DestinationType.Queue ? "Queue" : "Topic");
            try (JMSConsumer consumer = context.createConsumer(context.createTopic(dest))) {
                while (true) {
                    Message message = consumer.receive(100);
                    if (message == null) {
                        return names;
                    }
                    Destination destination = (Destination) getField(message, "super.dataStructure", "destination");
                    if (destination instanceof Queue) {
                        names.add(((Queue) destination).getQueueName());
                    } else {
                        names.add(((Topic) destination).getTopicName());
                    }
                }

            }
        } catch (Exception e) {
            // Ignore
            String msg = e.toString();
        }
        return Collections.emptyList();
    }

    private static Object getField(Object context, String... fields) throws NoSuchFieldException, IllegalAccessException {
        Object obj = context;
        for (String field : fields) {
            Class cl = obj.getClass();
            while (field.startsWith("super.")) {
                cl = cl.getSuperclass();
                field = field.substring("super.".length());
            }
            Field f = cl.getDeclaredField(field);
            f.setAccessible(true);
            obj = f.get(obj);
        }
        return obj;
    }

}
