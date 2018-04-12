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

@Command(scope = "jaas", name = "user-add", description = "Add a user")
@Service
public class UserAddCommand extends JaasCommandSupport {

    @Argument(index = 0, name = "username", description = "User Name", required = true, multiValued = false)
    private String username;

    @Argument(index = 1, name = "password", description = "Password", required = true, multiValued = false, censor = true, mask = '#')
    private String password;

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        engine.addUser(username, password);
        return null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "UserAddCommand{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }

}
