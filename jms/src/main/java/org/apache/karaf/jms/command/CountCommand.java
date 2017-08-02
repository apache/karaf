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
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jms", name = "count", description = "Count the number of messages on a JMS queue.")
@Service
public class CountCommand extends JmsConnectionCommandSupport {

    @Argument(index = 1, name = "queue", description = "The JMS queue name", required = true, multiValued = false)
    String queue;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Messages Count");
        table.addRow().addContent(getJmsService().count(connectionFactory, queue, username, password));
        table.print(System.out);
        return null;
    }

}
