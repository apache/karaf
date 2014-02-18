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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Completer;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.completer.StringsCompleter;
import org.apache.karaf.shell.inject.Service;

@Command(scope = "jms", name = "create", description = "Create a JMS connection factory.")
@Service
public class CreateCommand extends JmsCommandSupport {

    @Argument(index = 0, name = "name", description = "The JMS connection factory name", required = true, multiValued = false)
    String name;

    @Option(name = "-t", aliases = { "--type" }, description = "The JMS connection factory type (ActiveMQ or WebsphereMQ)", required = false, multiValued = false)
    @Completer(value = StringsCompleter.class, values = { "activemq", "webspheremq" })
    String type = "ActiveMQ";

    @Option(name = "--url", description = "URL of the JMS broker. For WebsphereMQ type, the URL is hostname/port/queuemanager/channel", required = false, multiValued = false)
    String url = "tcp://localhost:61616";
    
    public Object doExecute() throws Exception {
        getJmsService().create(name, type, url);
        return null;
    }

}
