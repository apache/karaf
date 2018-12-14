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

import org.apache.karaf.examples.redis.api.UserServiceRedis;
import org.apache.karaf.examples.redis.command.completer.CompleterRedis;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.util.List;

@Service
@Command(scope = "user", name = "remove-redis", description = "Remove user from current list on Redis by ID")
public class RemoveUserRedisCommand implements Action {

    @Reference
    private UserServiceRedis userServiceRedis;

    @Argument(index = 0, name = "ids", description = "List of ID user thar want to remove from redis list", required = true, multiValued = true)
    @Completion(CompleterRedis.class)
    List<Integer> ids;

    public Object execute() throws Exception {
        for(int id : ids){
            userServiceRedis.remove(id);
        }
        return null;
    }
}
