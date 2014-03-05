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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jms", name = "move", description = "Move messages from one JMS queue to another one.")
@Service
public class MoveCommand extends JmsConnectionCommandSupport {

    @Argument(index = 1, name = "source", description = "The source JMS queue", required = true, multiValued = false)
    String source;

    @Argument(index = 2, name = "destination", description = "The destination JMS queue", required = true, multiValued = false)
    String destination;

    @Option(name = "-s", aliases = { "--selector" }, description = "Selector to move only some messages", required = false, multiValued = false)
    String selector;

    @Override
    public Object execute() throws Exception {
        System.out.println(getJmsService().move(connectionFactory, source, destination, selector, username, password) + " message(s) moved");
        return null;
    }

}
