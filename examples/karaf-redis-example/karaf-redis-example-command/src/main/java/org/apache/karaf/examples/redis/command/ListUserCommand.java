/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.examples.redis.command;

import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.Collection;

@Service
@Command(scope = "user", name = "list", description = "Get list of current user.")
public class ListUserCommand implements Action {

    @Reference
    private UserService userService;

    public Object execute() throws Exception {

        Collection<User> users = userService.list();
        ShellTable shellTable = new ShellTable();
        shellTable.column("ID");
        shellTable.column("First Name");
        shellTable.column("Last Name");
        shellTable.column("Phone Number");
        for(User user : users){
            shellTable.addRow().addContent(user.getId(), user.getFirstName(), user.getLastName(), user.getPhoneNumber());
        }
        shellTable.print(System.out);

        return null;
    }
}
