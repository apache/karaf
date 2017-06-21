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

import java.net.URL;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.RepositoryPolicy;
import org.osgi.service.cm.Configuration;

@Command(scope = "maven", name = "repository-change", description = "Changes configuration of Maven repository")
@Service
public class RepositoryChangeCommand extends RepositoryEditCommandSupport {

    @Option(name = "-s", aliases = { "--snapshots" }, description = "Enable SNAPSHOT handling in the repository", required = false, multiValued = false)
    boolean snapshots = false;

    @Option(name = "-nr", aliases = { "--no-releases" }, description = "Disable release handling in this repository", required = false, multiValued = false)
    boolean noReleases = false;

    @Option(name = "-up", aliases = { "--update-policy" }, description = "Update policy for repository (never, daily (default), interval:N, always)", required = false, multiValued = false)
    String updatePolicy;

    @Option(name = "-cp", aliases = { "--checksum-policy" }, description = "Checksum policy for repository (ignore, warn (default), fail)", required = false, multiValued = false)
    String checksumPolicy;

    @Option(name = "-u", aliases = { "--username" }, description = "Username for remote repository", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "Password for remote repository (may be encrypted, see \"maven:password -ep\")", required = false, multiValued = false)
    String password;

    @Argument(description = "Repository URI. It may be file:// based, http(s):// based, may use other known protocol or even property placeholders (like ${karaf.base})", required = false)
    String uri;

    @Override
    protected void edit(String prefix, Dictionary<String, Object> config,
                        MavenRepositoryURL[] allRepos, MavenRepositoryURL[] pidRepos, MavenRepositoryURL[] settingsRepos) throws Exception {

        Optional<MavenRepositoryURL> first = Arrays.stream(allRepos)
                .filter((repo) -> id.equals(repo.getId())).findFirst();
        if (!first.isPresent()) {
            System.err.printf("Can't find %s repository with ID \"%s\"\n", (defaultRepository ? "default" : "remote"), id);
            return;
        }

        MavenRepositoryURL changedRepository = first.get();

        changedRepository.setSnapshotsEnabled(snapshots);
        changedRepository.setReleasesEnabled(!noReleases);

        if (updatePolicy != null) {
            SourceAnd<String> up = updatePolicy(updatePolicy);
            if (!up.valid) {
                System.err.println("Unknown value of update policy: \"" + updatePolicy + "\"");
                return;
            }
            changedRepository.setReleasesUpdatePolicy(up.val());
            changedRepository.setSnapshotsUpdatePolicy(up.val());
        }

        if (checksumPolicy != null) {
            SourceAnd<String> cp = checksumPolicy(checksumPolicy);
            if (!cp.valid) {
                System.err.println("Unknown value of checksum policy: \"" + checksumPolicy + "\"");
                return;
            }
            changedRepository.setReleasesChecksumPolicy(cp.val());
            changedRepository.setSnapshotsChecksumPolicy(cp.val());
        }

        if (uri != null) {
            SourceAnd<String> urlResolved = validateRepositoryURL(uri, defaultRepository);
            if (!urlResolved.valid) {
                return;
            }
            changedRepository.setURL(new URL(urlResolved.val()));
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

        boolean credentialsUpdated = false;
        // credentials for remote repository can be stored only in settings.xml
        if (!defaultRepository && hasCredentials) {
            if (!updateCredentials(force, id, username, password, prefix, config)) {
                return;
            }
            credentialsUpdated = true;
        }

        if (!defaultRepository && changedRepository.getFrom() == MavenRepositoryURL.FROM.SETTINGS) {
            // find <repository> in any active profile to change it
            for (Profile profile : mavenSettings.getProfiles()) {
                Optional<Repository> repository = profile.getRepositories().stream().filter((r) -> id.equals(r.getId())).findFirst();
                if (repository.isPresent()) {
                    // I can't imagine it's not...
                    Repository r = repository.get();
                    r.setUrl(changedRepository.getURL().toString());
                    if (!changedRepository.isSnapshotsEnabled()) {
                        r.setSnapshots(new RepositoryPolicy());
                        r.getSnapshots().setEnabled(false);
                    } else {
                        RepositoryPolicy rp = r.getSnapshots() == null ? new RepositoryPolicy() : r.getSnapshots();
                        rp.setEnabled(true);
                        if (checksumPolicy != null) {
                            rp.setChecksumPolicy(changedRepository.getSnapshotsChecksumPolicy());
                        } else if (rp.getChecksumPolicy() == null) {
                            rp.setChecksumPolicy("warn");
                        }
                        if (updatePolicy != null) {
                            rp.setUpdatePolicy(changedRepository.getSnapshotsUpdatePolicy());
                        } else if (rp.getUpdatePolicy() == null) {
                            rp.setUpdatePolicy("daily");
                        }
                        r.setSnapshots(rp);
                    }
                    if (!changedRepository.isReleasesEnabled()) {
                        r.setReleases(new RepositoryPolicy());
                        r.getReleases().setEnabled(false);
                    } else {
                        RepositoryPolicy rp = r.getReleases() == null ? new RepositoryPolicy() : r.getReleases();
                        rp.setEnabled(true);
                        if (checksumPolicy != null) {
                            rp.setChecksumPolicy(changedRepository.getReleasesChecksumPolicy());
                        } else if (rp.getChecksumPolicy() == null) {
                            rp.setChecksumPolicy("warn");
                        }
                        if (updatePolicy != null) {
                            rp.setUpdatePolicy(changedRepository.getReleasesUpdatePolicy());
                        } else if (rp.getUpdatePolicy() == null) {
                            rp.setUpdatePolicy("daily");
                        }
                        r.setReleases(rp);
                    }
                    updateSettings(prefix, config);
                    break;
                }
            }
        } else if (changedRepository.getFrom() == MavenRepositoryURL.FROM.PID) {
            List<MavenRepositoryURL> newRepos = new LinkedList<>();
            for (MavenRepositoryURL repo : pidRepos) {
                MavenRepositoryURL _r = repo;
                if (id.equals(repo.getId())) {
                    _r = changedRepository;
                }
                newRepos.add(_r);
            }
            updatePidRepositories(prefix, config, defaultRepository, newRepos, settingsRepos.length > 0);
            if (credentialsUpdated) {
                updateSettings(prefix, config);
            }
        }

        Configuration cmConfig = cm.getConfiguration(PID);
        cmConfig.update(config);

        success = true;
    }

}
