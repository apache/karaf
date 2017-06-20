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
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.apache.maven.settings.Proxy;

@Command(scope = "maven", name = "http-proxy", description = "Manage HTTP proxy configuration for Maven remote repositories")
@Service
public class HttpProxyCommand extends MavenSecuritySupport {

    @Option(name = "--add", description = "Adds HTTP proxy configuration to Maven settings", required = false, multiValued = false)
    boolean add;

    @Option(name = "--remove", description = "Removes HTTP proxy configuration from Maven settings", required = false, multiValued = false)
    boolean remove;

    @Override
    public void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        if (add && remove) {
            System.err.println("Please specify only one of --add and --remove");
            return;
        }

        if (add) {
            // add
        }

        if (remove) {
            // remove
        }

        // list (also after --add or --remove)
        System.out.println();
        if (mavenSettings != null && mavenSettings.getProxies() != null && mavenSettings.getProxies().size() > 0) {
            ShellTable table = new ShellTable();
            table.column("ID");
            table.column("Host");
            table.column("Port");
            table.column("Username");
            if (showPasswords) {
                table.column("Password");
            }
            for (Proxy proxy : mavenSettings.getProxies()) {
                Row row = table.addRow();
                row.addContent(proxy.getId(), proxy.getHost(), proxy.getPort());
                row.addContent(proxy.getUsername() != null ? proxy.getUsername() : "");
                if (showPasswords) {
                    addPasswordInfo(row, proxyPasswords, proxy.getId(), proxy.getPassword());
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
