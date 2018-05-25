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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;

@Command(scope = "jms", name = "create", description = "Create a JMS connection factory.")
@Service
public class CreateCommand extends JmsCommandSupport {

    @Argument(index = 0, name = "name", description = "The JMS connection factory name", required = true, multiValued = false)
    String name;

    @Option(name = "-t", aliases = { "--type" }, description = "The JMS connection factory type (ActiveMQ, Artemis or WebsphereMQ)", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "activemq", "artemis", "webspheremq" })
    String type = "activemq";

    @Option(name = "--url", description = "URL of the JMS broker. For WebsphereMQ type, the URL is hostname/port/queuemanager/channel", required = false, multiValued = false)
    String url = "tcp://localhost:61616";

    @Option(name = "--pool", description = "The pool mechanism to use for this connection factory", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "pooledjms", "narayama", "transx" })
    String pool = "pooledjms";

    @Option(name = "-u", aliases = { "--username" }, description = "Username to connect to the JMS broker", required = false, multiValued = false)
    String username = "karaf";

    @Option(name = "-p", aliases = { "--password" }, description = "Password to connect to the JMS broker", required = false, multiValued = false)
    String password = "karaf";

    @Override
    public Object execute() throws Exception {
        getJmsService().create(name, type, url, username, password, pool);
        return null;
    }

}
