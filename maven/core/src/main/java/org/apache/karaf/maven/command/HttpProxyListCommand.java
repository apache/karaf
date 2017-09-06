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
package org.apache.karaf.maven.command;

import java.util.Dictionary;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.apache.maven.settings.Proxy;

@Command(scope = "maven", name = "http-proxy-list", description = "Lists HTTP proxy configurations for Maven remote repositories")
@Service
public class HttpProxyListCommand extends MavenSecuritySupport {

    @Override
    public void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        System.out.println();
        if (mavenSettings != null && mavenSettings.getProxies() != null && mavenSettings.getProxies().size() > 0) {
            ShellTable table = new ShellTable();
            table.column("ID");
            table.column("Host");
            table.column("Port");
            table.column("Non-proxy hosts");
            table.column("Username");
            if (showPasswords) {
                table.column("Password");
            }
            for (Proxy _p : mavenSettings.getProxies()) {
                Row row = table.addRow();
                row.addContent(_p.getId(), _p.getHost(), _p.getPort(), _p.getNonProxyHosts());
                row.addContent(_p.getUsername() != null ? _p.getUsername() : "");
                if (showPasswords) {
                    addPasswordInfo(row, proxyPasswords, _p.getId(), _p.getPassword());
                }
            }
            table.print(System.out);
        } else {
            System.out.print("No HTTP proxies configured");
            if (settings != null && settings.value != null) {
                System.out.print(" in " + settings.value);
            }
            System.out.println();
        }

        System.out.println();
    }

}
