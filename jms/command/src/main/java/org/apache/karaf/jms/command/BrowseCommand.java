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
package org.apache.karaf.jms.command;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.jms.JmsMessage;

import java.util.List;

@Command(scope = "jms", name = "browse", description = "Browse a JMS queue")
public class BrowseCommand extends JmsCommandSupport {

    @Argument(index = 0, name = "connectionFactory", description = "The JMS connection factory name", required = true, multiValued = false)
    String connectionFactory;

    @Argument(index = 1, name = "queue", description = "The JMS queue to browse", required = true, multiValued = false)
    String queue;

    @Option(name = "-s", aliases = { "--selector" }, description = "The selector to select the messages to browse", required = false, multiValued = false)
    String selector;

    @Option(name = "-u", aliases = { "--username" }, description = "Username to connect to the JMS broker", required = false, multiValued = false)
    String username = "karaf";

    @Option(name = "-p", aliases = { "--password" }, description = "Password to connect to the JMS broker", required = false, multiValued = false)
    String password = "karaf";

    private final static String JMS_QUEUE_FORMAT = "%15s %40% %5s %10s %10s %10s %10s %10s %10s %10s %10s %10s";

    public Object doExecute() throws Exception {

        System.out.println(String.format(JMS_QUEUE_FORMAT, "ID", "Content", "Charset", "Type", "Correlation", "Delivery", "Destination", "Expiration", "Priority", "Redelivered", "ReplyTo", "Timestamp"));

        List<JmsMessage> messages = getJmsService().browse(connectionFactory, queue, selector, username, password);
        for (JmsMessage message : messages) {
            System.out.println(String.format(JMS_QUEUE_FORMAT,
                    message.getMessageId(),
                    message.getContent(),
                    message.getCharset(),
                    message.getType(),
                    message.getCorrelationID(),
                    message.getDeliveryMode(),
                    message.getDestination(),
                    message.getExpiration(),
                    message.getPriority(),
                    message.isRedelivered(),
                    message.getReplyTo(),
                    message.getTimestamp()));
        }

        return null;
    }

}
