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
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(
        scope = "jaas",
        name = "pending-list",
        description = "List the pending modification on the active JAAS Realm/Login Module")
@Service
public class ListPendingCommand extends JaasCommandSupport {

    @Override
    public Object execute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);
        @SuppressWarnings("unchecked")
        Queue<JaasCommandSupport> commandQueue = (Queue<JaasCommandSupport>) session.get(JAAS_CMDS);

        if (realm != null && entry != null) {
            String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
            System.out.println(
                    String.format(
                            "JAAS Realm %s/JAAS Login Module %s", realm.getName(), moduleClass));

            if (commandQueue != null && !commandQueue.isEmpty()) {
                for (JaasCommandSupport command : commandQueue) {
                    System.out.println(command);
                }
            } else {
                System.err.println("No JAAS modification command in queue");
            }
        } else {
            System.err.println("No JAAS Realm/Login Module selected");
        }
        return null;
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        return null;
    }
}
