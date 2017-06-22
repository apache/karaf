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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.maven.settings.Proxy;
import org.osgi.service.cm.Configuration;

@Command(scope = "maven", name = "http-proxy", description = "Manage HTTP proxy configuration for Maven remote repositories")
@Service
public class HttpProxyCommand extends MavenSecuritySupport {

    @Option(name = "--add", description = "Adds HTTP proxy configuration to Maven settings", required = false, multiValued = false)
    boolean add;

    @Option(name = "--change", description = "Changes HTTP proxy configuration in Maven settings", required = false, multiValued = false)
    boolean change;

    @Option(name = "--remove", description = "Removes HTTP proxy configuration from Maven settings", required = false, multiValued = false)
    boolean remove;

    @Option(name = "-id", description = "Identifier of HTTP proxy", required = true, multiValued = false)
    String id;

    @Option(name = "-f", aliases = { "--force" }, description = "Do not ask for confirmation", required = false, multiValued = false)
    boolean force = false;

    @Option(name = "-u", aliases = { "--username" }, description = "Username for remote repository", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "Password for remote repository (may be encrypted, see \"maven:password -ep\")", required = false, multiValued = false)
    String password;

    @Option(name = "-n", aliases = { "--non-proxy-hosts" }, description = "Non-proxied hosts (in the format '192.168.*|localhost|...')", required = false, multiValued = false)
    String nonProxyHosts;

    @Argument(description = "host:port of HTTP proxy", required = false, multiValued = false)
    String hostPort;

    @Override
    public void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        if (add && (change || remove) || change && remove) {
            System.err.println("Please specify only one of --add/--change/--remove");
            return;
        }

        if (id == null || "".equals(id.trim())) {
            System.err.println("Please specify ID of HTTP proxy");
            return;
        }

        if (mavenSettings.getProxies() == null) {
            mavenSettings.setProxies(new LinkedList<>());
        }
        Optional<Proxy> existingProxy = mavenSettings.getProxies().stream()
                .filter((p) -> id.equals(p.getId())).findAny();

        if (add) {
            if (hostPort == null || "".equals(hostPort.trim())) {
                System.err.println("Please specify host:port of new HTTP proxy");
                return;
            }
            if (existingProxy.isPresent()) {
                System.err.printf("HTTP proxy with ID \"%s\" is already configured\n", id);
                return;
            }
        } else if (!existingProxy.isPresent()) {
            System.err.printf("Can't find HTTP proxy with ID \"%s\"\n", id);
            return;
        }

        boolean hasUsername = username != null && !"".equals(username.trim());
        boolean hasPassword = password != null && !"".equals(password.trim());
        boolean hasCredentials = hasUsername && hasPassword;

        if ((hasUsername && !hasPassword) || (!hasUsername && hasPassword)) {
            System.err.println("Please specify both username and password");
            return;
        }

        Proxy proxy = null;
        if (add) {
            proxy = new Proxy();
            proxy.setId(id);
            mavenSettings.getProxies().add(proxy);
        } else if (change) {
            proxy = existingProxy.get(); // should be there
        } else /*if (remove)*/ {
            // remove
            List<Proxy> newProxies = mavenSettings.getProxies().stream()
                    .filter((p) -> !id.equals(p.getId())).collect(Collectors.toList());
            mavenSettings.setProxies(newProxies);
        }

        if (add || change) {
            proxy.setActive(true);
            proxy.setProtocol("http");
            if (nonProxyHosts != null && !"".equals(nonProxyHosts.trim())) {
                proxy.setNonProxyHosts(nonProxyHosts);
            }
            if (hostPort != null && !"".equals(hostPort.trim())) {
                if (hostPort.contains(":")) {
                    proxy.setHost(hostPort.substring(0, hostPort.indexOf(':')));
                    proxy.setPort(Integer.parseInt(hostPort.substring(hostPort.indexOf(':') + 1)));
                } else {
                    proxy.setHost(hostPort);
                    proxy.setPort(3128);
                }
            }
            if (hasCredentials) {
                proxy.setUsername(username);
                proxy.setPassword(password);
            }
        }

        updateSettings(prefix, config);

        Configuration cmConfig = cm.getConfiguration(PID);
        cmConfig.update(config);

        // list (also after --add or --remove)
        if (showPasswords) {
            session.execute("maven:http-proxy-list -x");
        } else {
            session.execute("maven:http-proxy-list");
        }
    }

}
