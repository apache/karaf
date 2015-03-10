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
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.boot.principal.GroupPrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

@Command(scope = "jaas", name = "groups", description = "List groups in a realm")
public class ListGroupsCommand extends JaasCommandSupport {

    private static final String GROUP_LIST_FORMAT = "%-10s  %-80s";

        
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
        System.out.println(String.format(GROUP_LIST_FORMAT, "Group", "Roles"));

        for (GroupPrincipal group : engine.listGroups().keySet()) {
            
            System.out.println(String.format(GROUP_LIST_FORMAT, group.getName(), engine.listGroups().get(group)));
        }
        return null;
    }
}
    
