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

import org.apache.karaf.examples.redis.api.UserService;
import org.apache.karaf.examples.redis.command.completer.Completer;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Service
@Command(scope = "user", name = "remove", description = "Remove user from list by ID.")
public class RemoveUserCommand implements Action {

    @Reference
    private UserService userService;

    @Argument(index = 0, name = "ids", description = "ID of user that you want to remove him/his.", required = true, multiValued = true)
    @Completion(Completer.class)
    List<Integer> ids;

    public Object execute() throws Exception {
        for(int id : ids){
            userService.remove(id);
        }
        return null;
    }
}
