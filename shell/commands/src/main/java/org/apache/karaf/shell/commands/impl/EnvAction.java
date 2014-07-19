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
package org.apache.karaf.shell.commands.impl;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;

/**
 * Command to get/set the value of a session variable.
 */
@Command(scope = "shell", name = "env", description = "Get/set the value of a console session variable.")
@Service
public class EnvAction implements Action {

    @Argument(index = 0, name = "variable", description = "The name of the console session variable.", required = true, multiValued = false)
    String variable;

    @Argument(index = 1, name = "value", description = "The new value of the console session variable.", required = false, multiValued = false)
    String value;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        if (value == null) {
            System.out.println(session.get(variable));
        } else {
            session.put(variable, value);
        }
        return null;
    }

}
