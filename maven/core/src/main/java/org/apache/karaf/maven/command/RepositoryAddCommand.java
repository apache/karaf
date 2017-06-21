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
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.osgi.service.cm.Configuration;

@Command(scope = "maven", name = "repository-add", description = "Adds Maven repository")
@Service
public class RepositoryAddCommand extends RepositoryEditCommandSupport {

    @Option(name = "-idx", description = "Index at which new repository is to be inserted (0-based) (defaults to last - repository will be appended)", required = false, multiValued = false)
    int idx = -1;

    @Option(name = "-s", aliases = { "--snapshots" }, description = "Enable SNAPSHOT handling in the repository", required = false, multiValued = false)
    boolean snapshots = false;

    @Option(name = "-nr", aliases = { "--no-releases" }, description = "Disable release handling in this repository", required = false, multiValued = false)
    boolean noReleases = false;

    @Option(name = "-up", aliases = { "--update-policy" }, description = "Update policy for repository (never, daily (default), interval:N, always)", required = false, multiValued = false)
    String updatePolicy = "daily";

    @Option(name = "-cp", aliases = { "--checksum-policy" }, description = "Checksum policy for repository (ignore, warn (default), fail)", required = false, multiValued = false)
    String checksumPolicy = "warn";

    @Argument(description = "Repository URI. It may be file:// based, http(s):// based, may use other known protocol or even property placeholders (like ${karaf.base})")
    String uri;

    @Option(name = "-u", aliases = { "--username" }, description = "Username for remote repository", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "Password for remote repository (may be encrypted, see \"maven:password -ep\")", required = false, multiValued = false)
    String password;

    @Override
    protected void edit(String prefix, Dictionary<String, Object> config) throws Exception {
        MavenRepositoryURL[] repositories = repositories(config, !defaultRepository);

        MavenRepositoryURL[] repositoriesFromPidProperty = Arrays.stream(repositories)
                .filter((repo) -> repo.getFrom() == MavenRepositoryURL.FROM.PID)
                .toArray(MavenRepositoryURL[]::new);

        if (idx > repositoriesFromPidProperty.length) {
            // TOCONSIDER: should we allow to add repository to settings.xml too?
            System.err.printf("List of %s repositories has %d elements. Can't insert at position %s.\n",
                    (defaultRepository ? "default" : "remote"), repositories.length, id);
            return;
        }

        if (id == null || "".equals(id.trim())) {
            System.err.println("Please specify ID of repository");
            return;
        }

        Optional<MavenRepositoryURL> first = Arrays.stream(repositories)
                .filter((repo) -> id.equals(repo.getId())).findAny();
        if (first.isPresent()) {
            System.err.printf("Repository with ID \"%s\" is already configured for URL %s\n", id, first.get().getURL());
            return;
        }

        SourceAnd<String> up = updatePolicy(updatePolicy);
        if (!up.valid) {
            System.err.println("Unknown value of update policy: \"" + updatePolicy + "\"");
            return;
        }

        SourceAnd<String> cp = checksumPolicy(checksumPolicy);
        if (!cp.valid) {
            System.err.println("Unknown value of checksum policy: \"" + checksumPolicy + "\"");
            return;
        }

        if (uri == null || "".equals(uri.trim())) {
            System.err.println("Please specify repository location");
            return;
        }
        String urlResolved = InterpolationHelper.substVars(uri, "uri", null, null, context);
        URL url = null;
        try {
            url = new URL(urlResolved);
            urlResolved = url.toString();

            if ("file".equals(url.getProtocol()) && new File(url.toURI()).isDirectory()) {
                System.err.println("Location \"" + urlResolved + "\" is not accessible");
                return;
            }
        } catch (MalformedURLException e) {
            // a directory?
            File location = new File(urlResolved);
            if (!location.exists() || !location.isDirectory()) {
                System.err.println("Location \"" + urlResolved + "\" is not accessible");
                return;
            } else {
                url = location.toURI().toURL();
                urlResolved = url.toString();
            }
        }

        if (defaultRepository && !"file".equals(url.getProtocol())) {
            System.err.println("Default repositories should be locally accessible (use file:// protocol or normal directory path)");
            return;
        }

        boolean hasUsername = username != null && !"".equals(username.trim());
        boolean hasPassword = password != null && !"".equals(password.trim());
        boolean hasCredentials = hasUsername && hasPassword;

        if ((hasUsername && !hasPassword) || (!hasUsername && hasPassword)) {
            System.err.println("Please specify both username and password");
            return;
        }

        if (defaultRepository && hasCredentials) {
            System.out.println("User credentials won't be used for default repository");
            // no return
        }

        // credentials for remote repository can be stored only in settings.xml

        if (!defaultRepository && hasCredentials) {
            if (!confirm("Maven settings will be updated and org.ops4j.pax.url.mvn.settings property will change. Continue? (y/N) ")) {
                return;
            }

            File dataDir = context.getDataFile(".");
            if (!dataDir.isDirectory()) {
                System.err.println("Can't access data directory for " + context.getBundle().getSymbolicName() + " bundle");
                return;
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

            // prepare configadmin configuration update
            File newSettingsFile = nextSequenceFile(dataDir, RE_SETTINGS, PATTERN_SETTINGS);
            config.put(prefix + PROPERTY_SETTINGS_FILE, newSettingsFile.getCanonicalPath());

            try (FileWriter fw = new FileWriter(newSettingsFile)) {
                new SettingsXpp3Writer().write(fw, mavenSettings);
            }
            System.out.println("New settings stored in \"" + newSettingsFile.getCanonicalPath() + "\"");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(urlResolved);
        sb.append(ServiceConstants.SEPARATOR_OPTIONS + ServiceConstants.OPTION_ID + "=" + id);
        if (snapshots) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS + ServiceConstants.OPTION_ALLOW_SNAPSHOTS);
        }
        if (noReleases) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS + ServiceConstants.OPTION_DISALLOW_RELEASES);
        }
        sb.append(ServiceConstants.SEPARATOR_OPTIONS + ServiceConstants.OPTION_UPDATE + "=" + updatePolicy);
        sb.append(ServiceConstants.SEPARATOR_OPTIONS + ServiceConstants.OPTION_CHECKSUM + "=" + checksumPolicy);

        MavenRepositoryURL newRepository = new MavenRepositoryURL(sb.toString());
        List<MavenRepositoryURL> newRepos = new LinkedList<>(Arrays.asList(repositoriesFromPidProperty));
        if (idx >= 0) {
            newRepos.add(idx, newRepository);
        } else {
            newRepos.add(newRepository);
        }

        String newList = newRepos.stream().map(MavenRepositoryURL::asRepositorySpec)
                .collect(Collectors.joining(","));

        if (defaultRepository) {
            config.put(prefix + PROPERTY_DEFAULT_REPOSITORIES, newList);
        } else {
            config.put(prefix + PROPERTY_REPOSITORIES, newList);
        }

        Configuration cmConfig = cm.getConfiguration(PID);
        cmConfig.update(config);

        success = true;
    }

}
