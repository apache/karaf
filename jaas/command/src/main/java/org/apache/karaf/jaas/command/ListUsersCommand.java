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

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

@Command(scope = "jaas", name = "users", description = "List the users of the selected JAAS Realm/Login Module")
public class ListUsersCommand extends JaasCommandSupport {

    private static final String OUTPUT_FORMAT = "%-20s %-20s %-20s";

    @Override
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);

        if (realm == null || entry == null) {
            System.err.println("No JAAS Realm / Module has been selected.");
            return null;
        }

        BackingEngine engine = backingEngineService.get(entry);

        if (engine == null) {
            System.err.println("Can't get the list of users (no backing engine service registered)");
            return null;
        }

        return doExecute(engine);
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        List<UserPrincipal> users = engine.listUsers();
        System.out.println(String.format(OUTPUT_FORMAT, "User Name", "Group", "Role"));

        for (UserPrincipal user : users) {
            List<String> reportedRoles = new ArrayList<String>();
            String userName = user.getName();

            for(GroupPrincipal group : engine.listGroups(user)) {
                String groupName = group.getName();
                reportedRoles.addAll(displayRole(engine, userName, groupName, group));
            }

            reportedRoles.addAll(displayRole(engine, userName, "", user, reportedRoles));
            if (reportedRoles.size() == 0) {
                System.out.println(String.format(OUTPUT_FORMAT, userName, "", ""));
            }
        }
        return null;
    }

    private List<String> displayRole(BackingEngine engine, String userName, String groupName, Principal principal) {
        return displayRole(engine, userName, groupName, principal, Collections.<String>emptyList());
    }

    private List<String> displayRole(BackingEngine engine, String userName, String groupName, Principal principal, List<String> excludedRoles) {
        List<String> names = new ArrayList<String>();
        List<RolePrincipal> roles = engine.listRoles(principal);

        if (roles != null && roles.size() >= 1) {
            for (RolePrincipal role : roles) {
                String roleName = role.getName();
                if (excludedRoles.contains(roleName))
                    continue;

                names.add(roleName);
                System.out.println(String.format(OUTPUT_FORMAT, userName, groupName, roleName));
            }
        }
        return names;
    }
}
