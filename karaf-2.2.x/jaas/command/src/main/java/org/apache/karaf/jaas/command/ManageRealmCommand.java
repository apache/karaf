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
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

import javax.security.auth.login.AppConfigurationEntry;
import java.util.LinkedList;
import java.util.Queue;

@Command(scope = "jaas", name = "manage", description = "Manage user and roles of a JAAS Realm")
public class ManageRealmCommand extends JaasCommandSupport {

    @Option(name = "--realm", description = "JAAS Realm", required = false, multiValued = false)
    String realmName;

    @Option(name = "--index", description = "JAAS Realm Index", required = false, multiValued = false)
    int index;

    @Option(name = "--module", aliases = {}, description = "JAAS Realm Module", required = false, multiValued = false)
    String moduleName;

    @Option(name = "--force", aliases = {}, description = "Force the management of this realm, even if another one was under management", required = false, multiValued = false)
    boolean force;

    @Override
    protected Object doExecute() throws Exception {
        if (realmName == null && index <= 0 ) {
            System.err.println("A valid realm or the realm index need to be specified");
            return null;
        }
        JaasRealm oldRealm = (JaasRealm) this.session.get(JAAS_REALM);
        AppConfigurationEntry oldEntry = (AppConfigurationEntry) this.session.get(JAAS_ENTRY);

        if (oldRealm != null && !oldRealm.getName().equals(realmName) && !force) {
            System.err.println("Another realm is being edited. Cancel/update first, or use the --force option.");
        } else if (oldEntry != null && !oldEntry.getLoginModuleName().equals(moduleName) && !force) {
            System.err.println("Another module is being edited. Cancel/update first, or use the --force option.");
        } else {

            JaasRealm realm = findRealmByNameOrIndex(realmName, index);

            if (realm != null) {
                AppConfigurationEntry entry = findEntryByRealmAndName(realm, moduleName);

                if (entry != null) {
                    Queue<JaasCommandSupport> commands = null;

                    commands = (Queue<JaasCommandSupport>) this.session.get(JAAS_CMDS);
                    if (commands == null) {
                        commands = new LinkedList<JaasCommandSupport>();
                    }


                    this.session.put(JAAS_REALM, realm);
                    this.session.put(JAAS_ENTRY, entry);
                    this.session.put(JAAS_CMDS, commands);
                } else {
                    System.err.println(String.format("Could not find module %s in realm %s", moduleName, realmName));
                }
            } else {
                System.err.println(String.format("Could not find realm %s", realmName));
            }
        }
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}
