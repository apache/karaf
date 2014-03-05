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
import java.util.Queue;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.modules.BackingEngine;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "jaas", name = "update", description = "Apply pending modification on the edited JAAS Realm")
@Service
public class UpdateCommand extends JaasCommandSupport {

    @Override
    public Object execute() throws Exception {
        JaasRealm realm = (JaasRealm) session.get(JAAS_REALM);
        AppConfigurationEntry entry = (AppConfigurationEntry) session.get(JAAS_ENTRY);

        if (realm == null || entry == null) {
            System.err.println("No JAAS Realm/Login Module selected");
            return null;
        }

        BackingEngine engine = getBackingEngine(entry);

        if (engine == null) {
            System.err.println("Can't update the JAAS realm (no backing engine service registered)");
            return null;
        }

        return doExecute(engine);
    }

    @Override
    protected Object doExecute(BackingEngine engine) throws Exception {
        @SuppressWarnings("unchecked")
        Queue<? extends JaasCommandSupport> commands = (Queue<? extends JaasCommandSupport>) session.get(JAAS_CMDS);

        if (commands == null || commands.isEmpty()) {
            System.err.println("No JAAS pending modification");
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
