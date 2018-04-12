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

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "jaas", name = "realm-list", description = "List JAAS realms")
@Service
public class ListRealmsCommand extends JaasCommandSupport {

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    @Option(name = "-h", aliases = {"--hidden"}, description = "Show hidden realms", required = false, multiValued = false)
    boolean hidden;

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Index");
        table.column("Realm Name");
        table.column("Login Module Class Name");

        List<JaasRealm> realms = getRealms(hidden);
        if (realms != null && realms.size() > 0) {
            int index = 1;
            for (JaasRealm realm : realms) {
                String realmName = realm.getName();
                AppConfigurationEntry[] entries = realm.getEntries();

                if (entries != null && entries.length > 0) {
                    for (AppConfigurationEntry entry : entries) {
                        String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
                        table.addRow().addContent(index++, realmName, moduleClass);
                    }
                }
            }
        }

        table.print(System.out, !noFormat);

        return null;
    }

}
