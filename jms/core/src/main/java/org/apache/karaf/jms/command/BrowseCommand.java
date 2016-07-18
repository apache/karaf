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

import java.util.List;

import org.apache.karaf.jms.JmsMessage;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jms", name = "browse", description = "Browse a JMS queue")
@Service
public class BrowseCommand extends JmsConnectionCommandSupport {

    @Argument(index = 1, name = "queue", description = "The JMS queue to browse", required = true, multiValued = false)
    String queue;

    @Option(name = "-s", aliases = { "--selector" }, description = "The selector to select the messages to browse", required = false, multiValued = false)
    String selector;

    @Option(name = "-v", aliases = { "--verbose" }, description = "Display JMS properties", required = false, multiValued = false)
    boolean verbose = false;

    @Override
    public Object execute() throws Exception {

        ShellTable table = new ShellTable();
        table.column("Message ID");
        table.column("Content").maxSize(80);
        table.column("Charset");
        table.column("Type");
        table.column("Correlation ID");
        table.column("Delivery Mode");
        table.column("Destination");
        table.column("Expiration");
        table.column("Priority");
        table.column("Redelivered");
        table.column("ReplyTo");
        table.column("Timestamp");
        if (verbose) {
            table.column("Properties");
        }

        List<JmsMessage> messages = getJmsService().browse(connectionFactory, queue, selector, username, password);
        for (JmsMessage message : messages) {
            if (verbose) {
                StringBuilder properties = new StringBuilder();
                for (String property : message.getProperties().keySet()) {
                    properties.append(property).append("=").append(message.getProperties().get(property)).append("\n");
                }
                table.addRow().addContent(
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
                        message.getTimestamp(),
                        properties.toString());
            } else {
                table.addRow().addContent(
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
                        message.getTimestamp());
            }
        }

        table.print(System.out);

        return null;
    }

}
