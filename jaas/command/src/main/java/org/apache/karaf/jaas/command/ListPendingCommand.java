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

import java.util.Queue;
import javax.security.auth.login.AppConfigurationEntry;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;

@Command(scope = "jaas", name = "pending", description = "Lists the modification on the active realm/module.")
public class ListPendingCommand extends JaasCommandSupport {

    @Override
    protected Object doExecute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);
        Queue<JaasCommandSupport> commandQueue = (Queue<JaasCommandSupport>) session.get(JAAS_CMDS);

        if (realm != null && entry != null) {
            String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
            System.out.println(String.format("Jaas Realm:%s Jaas Module:%s", realm.getName(), moduleClass));

            if (commandQueue != null && !commandQueue.isEmpty()) {
                for (JaasCommandSupport command : commandQueue) {
                    System.out.println(command);
                }
            } else {
                System.err.println("No JAAS command in queue.");
            }
        } else {
            System.err.println("No JAAS Realm / Module has been selected.");
        }
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}