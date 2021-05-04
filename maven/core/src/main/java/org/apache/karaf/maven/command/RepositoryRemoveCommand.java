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

import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Server;
import org.osgi.service.cm.Configuration;

@Command(scope = "maven", name = "repository-remove", description = "Removes Maven repository")
@Service
public class RepositoryRemoveCommand extends RepositoryEditCommandSupport {

    @Override
    protected void edit(String prefix, Dictionary<String, Object> config,
                        MavenRepositoryURL[] allRepos, MavenRepositoryURL[] pidRepos, MavenRepositoryURL[] settingsRepos) throws Exception {

        Optional<MavenRepositoryURL> first = Arrays.stream(allRepos)
                .filter((repo) -> id.equals(repo.getId())).findAny();
        if (!first.isPresent()) {
            System.err.printf("Can't find %s repository with ID \"%s\"\n", (defaultRepository ? "default" : "remote"), id);
            return;
        }

        if (force || confirm(String.format("Are you sure to remove repository with ID \"%s\" for URL %s? (y/N) ", id, first.get().getURL()))) {
            if (!defaultRepository && first.get().getFrom() == MavenRepositoryURL.FROM.SETTINGS) {
                // remove <server> (credentials) if available
                List<Server> newServers = mavenSettings.getServers().stream()
                        .filter((s) -> !id.equals(s.getId())).collect(Collectors.toList());
                mavenSettings.setServers(newServers);

                // find <repository> in any active profile and remove it
                for (Profile profile : mavenSettings.getProfiles()) {
                    if (profile.getRepositories().stream().anyMatch((r) -> id.equals(r.getId()))) {
                        List<Repository> newRepos = profile.getRepositories().stream()
                                .filter((r) -> !id.equals(r.getId())).collect(Collectors.toList());
                        profile.setRepositories(newRepos);
                        System.out.printf("Repository with ID \"%s\" was removed from profile \"%s\"\n", id, profile.getId());
                        break;
                    }
                }

                updateSettings(prefix, config);
            } else if (first.get().getFrom() == MavenRepositoryURL.FROM.PID) {
                List<MavenRepositoryURL> newRepos = Arrays.stream(pidRepos).filter((r) -> !id.equals(r.getId())).collect(Collectors.toList());

                updatePidRepositories(prefix, config, defaultRepository, newRepos, settingsRepos.length > 0);

                // if there are credentials for this repository, we have to remove them from settings.xml
                if (mavenSettings != null &&  mavenSettings.getServers().stream().anyMatch((s) -> id.equals(s.getId()))) {
                    // remove <server> (credentials) if available
                    List<Server> newServers = mavenSettings.getServers().stream()
                            .filter((s) -> !id.equals(s.getId())).collect(Collectors.toList());
                    mavenSettings.setServers(newServers);

                    updateSettings(prefix, config);
                }
            }

            Configuration cmConfig = cm.getConfiguration(PID);
            cmConfig.update(config);

            success = true;
        }
    }

}
