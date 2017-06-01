/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.jaas.command.completers;

import java.util.LinkedList;
import java.util.List;

import javax.security.auth.login.AppConfigurationEntry;

import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

@Service
public class LoginModuleNameCompleter implements Completer {

    @Reference
    private List<JaasRealm> realms;

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        StringsCompleter delegate = new StringsCompleter();
        try {
            if (realms != null && !realms.isEmpty())
                for (JaasRealm realm : realms) {
                    List<String> moduleClassNames = findLoginModuleClassNames(realm);
                    if (moduleClassNames != null && !moduleClassNames.isEmpty())
                    delegate.getStrings().addAll(moduleClassNames);
                }
        } catch (Exception e) {
            // Ignore
        }
        return delegate.complete(session, commandLine, candidates);
    }

    /**
     * Finds the login module class name in the {@link JaasRealm} entries.
     * @param realm
     * @return
     */
    private List<String> findLoginModuleClassNames(JaasRealm realm) {
        List<String> moduleClassNames = new LinkedList<>();
        for (AppConfigurationEntry entry : realm.getEntries()) {
            String moduleClass = (String) entry.getOptions().get(ProxyLoginModule.PROPERTY_MODULE);
            if (moduleClass != null) {
                moduleClassNames.add(moduleClass);
            }

        }
        return moduleClassNames;
    }

}
