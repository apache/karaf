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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
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

    @Option(name = "-u", aliases = { "--username" }, description = "Username for remote repository", required = false, multiValued = false)
    String username;

    @Option(name = "-p", aliases = { "--password" }, description = "Password for remote repository (may be encrypted, see \"maven:password -ep\")", required = false, multiValued = false)
    String password;

    @Argument(description = "Repository URI. It may be file:// based, http(s):// based, may use other known protocol or even property placeholders (like ${karaf.base})")
    String uri;

    @Override
    protected void edit(String prefix, Dictionary<String, Object> config,
                        MavenRepositoryURL[] allRepos, MavenRepositoryURL[] pidRepos, MavenRepositoryURL[] settingsRepos) throws Exception {

        if (idx > pidRepos.length) {
            // TOCONSIDER: should we allow to add repository to settings.xml too?
            System.err.printf("List of %s repositories has %d elements. Can't insert at position %s.\n",
                    (defaultRepository ? "default" : "remote"), pidRepos.length, id);
            return;
        }

        Optional<MavenRepositoryURL> first = Arrays.stream(allRepos)
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

        SourceAnd<String> urlResolved = validateRepositoryURL(uri, defaultRepository);
        if (!urlResolved.valid) {
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
            if (!updateCredentials(force, id, username, password, prefix, config)) {
                return;
            }
            updateSettings(prefix, config);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(urlResolved.val());
        sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_ID).append("=").append(id);
        if (snapshots) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_ALLOW_SNAPSHOTS);
        }
        if (noReleases) {
            sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_DISALLOW_RELEASES);
        }
        sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_UPDATE).append("=").append(updatePolicy);
        sb.append(ServiceConstants.SEPARATOR_OPTIONS).append(ServiceConstants.OPTION_CHECKSUM).append("=").append(checksumPolicy);

        MavenRepositoryURL newRepository = new MavenRepositoryURL(sb.toString());
        List<MavenRepositoryURL> newRepos = new LinkedList<>(Arrays.asList(pidRepos));
        if (idx >= 0) {
            newRepos.add(idx, newRepository);
        } else {
            newRepos.add(newRepository);
        }

        updatePidRepositories(prefix, config, defaultRepository, newRepos, settingsRepos.length > 0);

        Configuration cmConfig = cm.getConfiguration(PID);
        cmConfig.update(config);

        success = true;
    }

}
