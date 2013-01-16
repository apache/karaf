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

import java.util.List;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.jaas.modules.RolePrincipal;
import org.apache.karaf.jaas.modules.UserPrincipal;

@Command(scope = "jaas", name = "users", description = "List the users of the active realm/module")
public class ListUsersCommand extends JaasCommandSupport {

    private static final String OUTPUT_FORMAT = "%-20s %-20s";

    @Override
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);

        if (realm == null || entry == null) {
            System.err.println("No JAAS Realm/Module has been selected");
            return null;
        }

        BackingEngine engine = backingEngineService.get(entry);

        if (engine == null) {
            System.err.println(String.format("Failed to resolve backing engine for realm %s and module %s", realm.getName(), entry.getLoginModuleName()));
            return null;
        }

        return doExecute(engine);
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        List<UserPrincipal> users = engine.listUsers();
        System.out.println(String.format(OUTPUT_FORMAT, "User Name", "Role"));

        for (UserPrincipal user : users) {
            String userName = user.getName();
            List<RolePrincipal> roles = engine.listRoles(user);

            if (roles != null && roles.size() >= 1) {
                for (RolePrincipal role : roles) {
                    String roleName = role.getName();
                    System.out.println(String.format(OUTPUT_FORMAT, userName, roleName));
                }
            } else {
                System.out.println(String.format(OUTPUT_FORMAT, userName, ""));
            }

        }
        return null;
    }
}
