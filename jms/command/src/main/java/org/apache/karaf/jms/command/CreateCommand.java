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

@Command(scope = "jms", name = "create", description = "Create a JMS connection factory.")
public class CreateCommand extends JmsCommandSupport {

    @Argument(index = 0, name = "name", description = "The JMS connection factory name", required = true, multiValued = false)
    String name;

    @Option(name = "-t", aliases = { "--type" }, description = "The JMS connection factory type (ActiveMQ or WebsphereMQ)", required = true, multiValued = false)
    String type;

    @Option(name = "-u", aliases = { "--url" }, description = "The JMS URL. NB: for WebsphereMQ type, the URL is hostname/port/queuemanager/channel", required = true, multiValued = false)
    String url;

    public Object doExecute() throws Exception {
        getJmsService().create(name, type, url);
        return null;
    }

}
