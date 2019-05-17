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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Option;
import org.apache.maven.settings.Server;

public abstract class RepositoryEditCommandSupport extends MavenSecuritySupport {

    @Option(name = "-id", description = "Identifier of repository", required = true, multiValued = false)
    String id;

    @Option(name = "-d", aliases = { "--default" }, description = "Edit default repository instead of remote one", required = false, multiValued = false)
    boolean defaultRepository = false;

    @Option(name = "-f", aliases = { "--force" }, description = "Do not ask for confirmation", required = false, multiValued = false)
    boolean force = false;

    boolean success = false;

    @Override
    public final void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        if (id == null || "".equals(id.trim())) {
            System.err.println("Please specify ID of repository");
            return;
        }

        MavenRepositoryURL[] repositories = repositories(config, !defaultRepository);

        MavenRepositoryURL[] repositoriesFromPidProperty = Arrays.stream(repositories)
                .filter((repo) -> repo.getFrom() == MavenRepositoryURL.FROM.PID)
                .toArray(MavenRepositoryURL[]::new);

        MavenRepositoryURL[] repositoriesFromSettings = Arrays.stream(repositories)
                .filter((repo) -> repo.getFrom() == MavenRepositoryURL.FROM.SETTINGS)
                .toArray(MavenRepositoryURL[]::new);

        edit(prefix, config, repositories, repositoriesFromPidProperty, repositoriesFromSettings);

        if (success) {
            if (showPasswords) {
                session.execute("maven:repository-list -v -x");
            } else {
                session.execute("maven:repository-list -v");
            }
        }
    }

    /**
     * Peform action on repository (add, remove, change)
     * @param prefix property prefix for <code>org.ops4j.pax.url.mvn</code> PID
     * @param config
     * @param allRepos
     * @param pidRepos
     * @param settingsRepos
     * @throws Exception
     */
    protected abstract void edit(String prefix, Dictionary<String, Object> config,
                                 MavenRepositoryURL[] allRepos, MavenRepositoryURL[] pidRepos, MavenRepositoryURL[] settingsRepos) throws Exception;

    /**
     * Stores new repository list in relevant <code>org.ops4j.pax.url.mvn</code> PID property
     * @param prefix
     * @param config
     * @param defaultRepository default (<code>true</code>) or remote repositories?
     * @param newRepos new list of repositories
     * @param hasSettingsRepositories whether we have repositories stored in <code>settings.xml</code> as well
     */
    protected void updatePidRepositories(String prefix, Dictionary<String, Object> config, boolean defaultRepository,
                                         List<MavenRepositoryURL> newRepos, boolean hasSettingsRepositories) {
        String newList = newRepos.stream().map(MavenRepositoryURL::asRepositorySpec)
                .collect(Collectors.joining(","));

        if (defaultRepository) {
            config.put(prefix + PROPERTY_DEFAULT_REPOSITORIES, newList);
        } else {
            if (hasSettingsRepositories) {
                newList = "+" + newList;
            }
            config.put(prefix + PROPERTY_REPOSITORIES, newList);
        }
    }

    /**
     * Stores credential information in settings, without persisting them
     * @param force
     * @param id
     * @param username
     * @param password
     * @param prefix
     * @param config
     */
    protected boolean updateCredentials(boolean force, String id, String username, String password,
                                      String prefix, Dictionary<String, Object> config) throws IOException {
        if (!force && !confirm("Maven settings will be updated and org.ops4j.pax.url.mvn.settings property will change. Continue? (y/N) ")) {
            return false;
        }

        Optional<Server> existingServer = mavenSettings.getServers().stream()
                .filter((s) -> id.equals(s.getId())).findAny();
        Server server = null;
        if (existingServer.isPresent()) {
            server = existingServer.get();
        } else {
            server = new Server();
            server.setId(id);
            mavenSettings.getServers().add(server);
        }
        server.setUsername(username);
        server.setPassword(password);

        return true;
    }

    /**
     * Takes passed-in repository URI and performs basic validation
     * @param uri
     * @param defaultRepository
     * @return
     */
    protected SourceAnd<String> validateRepositoryURL(String uri, boolean defaultRepository) throws URISyntaxException, MalformedURLException {
        SourceAnd<String> result = new SourceAnd<>();
        result.valid = false;

        if (uri == null || "".equals(uri.trim())) {
            System.err.println("Please specify repository location");
            return result;
        }
        String urlResolved = InterpolationHelper.substVars(uri, "uri", null, null, context);
        URL url = null;
        try {
            url = new URL(urlResolved);
            urlResolved = url.toString();

            if ("file".equals(url.getProtocol()) && new File(url.toURI()).isDirectory()) {
                System.err.println("Location \"" + urlResolved + "\" is not accessible");
                return result;
            }
        } catch (MalformedURLException e) {
            // a directory?
            File location = new File(urlResolved);
            if (!location.exists() || !location.isDirectory()) {
                System.err.println("Location \"" + urlResolved + "\" is not accessible");
                return result;
            } else {
                url = location.toURI().toURL();
                urlResolved = url.toString();
            }
        }

        if (defaultRepository && !"file".equals(url.getProtocol())) {
            System.err.println("Default repositories should be locally accessible (use file:// protocol or normal directory path)");
            return result;
        }

        result.valid = true;
        result.value = urlResolved;

        return result;
    }

}
