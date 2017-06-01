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

import java.util.ArrayList;
import java.util.List;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jaas", name = "user-list", description = "List the users of the selected JAAS realm/login module")
@Service
public class ListUsersCommand extends JaasCommandSupport {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Override
    public Object execute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);

        if (realm == null || entry == null) {
            System.err.println("No JAAS Realm/Login Module has been selected");
            return null;
        }

        BackingEngine engine = getBackingEngine(entry);

        if (engine == null) {
            System.err.println("Can't get the list of users (no backing engine service found)");
            return null;
        }

        return doExecute(engine);
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        List<UserPrincipal> users = engine.listUsers();

        ShellTable table = new ShellTable();
        table.column("User Name");
        table.column("Group");
        table.column("Role");

        for (UserPrincipal user : users) {
            List<String> reportedRoles = new ArrayList<>();
            String userName = user.getName();

            for (GroupPrincipal group : engine.listGroups(user)) {
                reportedRoles.addAll(displayGroupRoles(engine, userName, group, table));
            }

            for (RolePrincipal role : engine.listRoles(user)) {
                String roleName = role.getName();
                if (reportedRoles.contains(roleName)) {
                    continue;
                }
                reportedRoles.add(roleName);
                table.addRow().addContent(userName, "", roleName);
            }

            if (reportedRoles.size() == 0) {
                table.addRow().addContent(userName, "", "");
            }

        }

        table.print(System.out, !noFormat);

        return null;
    }

    private List<String> displayGroupRoles(BackingEngine engine, String userName, GroupPrincipal group, ShellTable table) {
        List<String> names = new ArrayList<>();
        List<RolePrincipal> roles = engine.listRoles(group);

        if (roles != null && roles.size() >= 1) {
            for (RolePrincipal role : roles) {
                String roleName = role.getName();
                names.add(roleName);
                table.addRow().addContent(userName, group.getName(), roleName);
            }
        }
        return names;
    }

}
