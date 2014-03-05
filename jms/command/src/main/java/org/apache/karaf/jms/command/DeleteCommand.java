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


import org.apache.karaf.jms.command.completers.ConnectionFactoriesFileNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jms", name = "delete", description = "Delete a JMS connection factory")
@Service
public class DeleteCommand extends JmsCommandSupport {

    @Argument(index = 0, name = "name", description = "The JMS connection factory name", required = true, multiValued = false)
    @Completion(ConnectionFactoriesFileNameCompleter.class)
    String name;

    @Override
    public Object execute() throws Exception {
        getJmsService().delete(name);
        return null;
    }

}
