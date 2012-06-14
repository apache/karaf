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

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.LinkedList;
import java.util.Queue;

@Command(scope = "jaas", name = "update", description = "Update JAAS realm")
public class UpdateCommand extends JaasCommandSupport {

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
        Queue<? extends JaasCommandSupport> commands = (Queue<? extends JaasCommandSupport>) session.get(JAAS_CMDS);

        if (commands == null || commands.isEmpty()) {
            System.err.println("No JAAS command in queue");
            return null;
        }

        //Loop throught the commands and execute them.
        while (!commands.isEmpty()) {
            Object obj = commands.remove();
            if (obj instanceof JaasCommandSupport) {
                ((JaasCommandSupport) obj).doExecute(engine);
            }
        }
        //Cleanup the session
        session.put(JAAS_REALM, null);
        session.put(JAAS_ENTRY, null);
        session.put(JAAS_CMDS, new LinkedList<JaasCommandSupport>());
        return null;
    }
}
