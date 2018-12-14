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
package org.apache.karaf.examples.redis.provider;

import org.apache.karaf.examples.redis.api.User;
import org.apache.karaf.examples.redis.api.UserService;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UserServiceImpl implements UserService {

    private Map<Integer, User> users = new HashMap<>();

    public void add(User user) {
        users.put(user.getId(), user);
    }

    public void remove(int id) {
        if(users.keySet().contains(id)) {
            users.remove(id);
        }
    }

    public Collection<User> list() {
        return users.values();
    }

    public void clear() {
        users.clear();
    }
}
