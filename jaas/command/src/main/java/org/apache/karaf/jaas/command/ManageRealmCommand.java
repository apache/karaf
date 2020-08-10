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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.command.completers.LoginModuleNameCompleter;
import org.apache.karaf.jaas.command.completers.RealmCompleter;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jaas", name = "realm-manage", description = "Manage users and roles of a JAAS Realm")
@Service
public class ManageRealmCommand extends JaasCommandSupport {

    @Option(name = "--realm", description = "JAAS Realm. Must be combined with --module", required = false, multiValued = false)
    @Completion(RealmCompleter.class)
    String realmName;

    @Option(name = "--index", description = "Realm Index", required = false, multiValued = false)
    int index;

    @Option(name = "--module", description = "JAAS Login Module Class Name. Must be combined with --realm", required = false, multiValued = false)
    @Completion(LoginModuleNameCompleter.class)
    String moduleName;

    @Option(name = "-f", aliases = { "--force" }, description = "Force the management of this realm, even if another one was under management", required = false, multiValued = false)
    boolean force;

    @Option(name = "-h", aliases = {"--hidden"}, description = "Manage hidden realms", required = false, multiValued = false)
    boolean hidden;

    @SuppressWarnings("unchecked")
    @Override
    public Object execute() throws Exception {
        if (realmName == null && index <= 0) {
            System.err.println("A valid realm or the realm index need to be specified");
            return null;
        }
        JaasRealm oldRealm = (JaasRealm) this.session.get(JAAS_REALM);
        AppConfigurationEntry oldEntry = (AppConfigurationEntry) this.session.get(JAAS_ENTRY);

        if (oldRealm != null && !oldRealm.getName().equals(realmName) && !force) {
            System.err.println("Another JAAS Realm is being edited. Cancel/update first, or use the --force option.");
        } else if (oldEntry != null && !oldEntry.getLoginModuleName().equals(moduleName) && !force) {
            System.err.println("Another JAAS Login Module is being edited. Cancel/update first, or use the --force option.");
        } else {

            JaasRealm realm = null;
            AppConfigurationEntry entry = null;

            if (index > 0) {
                // user provided the index, get the realm AND entry from the index
                List<JaasRealm> realms = getRealms(hidden);
                if (realms != null && realms.size() > 0) {
                    int i = 1;
                    realms_loop: for (JaasRealm r : realms) {
                        AppConfigurationEntry[] entries = r.getEntries();

                        if (entries != null) {
                            for (AppConfigurationEntry entry1 : entries) {
                                if (i == index) {
                                    realm = r;
                                    entry = entry1;
                                    break realms_loop;
                                }
                                i++;
                            }
                        }
                    }
                }
            } else {
                List<JaasRealm> realms = getRealms(hidden);
                if (realms != null && realms.size() > 0) {
                    for (JaasRealm r : realms) {
                        if (r.getName().equals(realmName)) {
                            realm = r;
                            AppConfigurationEntry[] entries = realm.getEntries();
                            if (entries != null) {
                                for (AppConfigurationEntry e : entries) {
                                    String moduleClass = (String) e.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                                    if (moduleName == null) {
                                        if (getBackingEngine(e) != null) {
                                            entry = e;
                                            break;
                                        }
                                    } else {
                                        if (moduleName.equals(e.getLoginModuleName()) || moduleName.equals(moduleClass)) {
	                                        if (getBackingEngine(e) != null) {
												entry = e;
												break;
											}
                                        }
                                    }
                                }
                                if (entry != null) {
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            if (realm == null) {
                System.err.println("JAAS realm has not been found.");
                return null;
            }

            if (entry == null) {
                System.err.println("JAAS module has not been found.");
                return null;
            }

            Queue<JaasCommandSupport> commands = null;

            commands = (Queue<JaasCommandSupport>) this.session.get(JAAS_CMDS);
            if (commands == null) {
                commands = new LinkedList<>();
            }

            this.session.put(JAAS_REALM, realm);
            this.session.put(JAAS_ENTRY, entry);
            this.session.put(JAAS_CMDS, commands);
        }
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

}
