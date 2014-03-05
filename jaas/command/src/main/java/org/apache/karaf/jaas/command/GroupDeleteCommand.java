/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.jaas.command;

import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jaas", name = "group-delete", description = "Remove a user from a group")
@Service
public class GroupDeleteCommand extends JaasCommandSupport {

    @Argument(index = 0, name = "username", description = "Username", required = true, multiValued = false)
    private String username;

    @Argument(index = 1, name = "group", description = "Group", required = true, multiValued = false)
    private String group;

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        engine.deleteGroup(username, group);
        return null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    @Override
    public String toString() {
        return "GroupDeleteCommand {username='" + username + "', group='" + group + "'}";
    }

}
