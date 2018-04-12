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

import org.apache.karaf.maven.core.MavenRepositoryURL;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;
import org.apache.maven.settings.Server;

@Command(scope = "maven", name = "repository-list", description = "Maven repository summary.")
@Service
public class RepositoryListCommand extends MavenSecuritySupport {

    @Option(name = "-v", aliases = { "--verbose" }, description = "Show additional information (policies, source)", required = false, multiValued = false)
    boolean verbose;

    @Override
    public void doAction(String prefix, Dictionary<String, Object> config) throws Exception {
        ShellTable table = new ShellTable();
        table.column("ID");
        table.column("URL");
        if (verbose) {
            table.column("Releases");
            table.column("Snapshots");
            table.column("Defined in");
        }
        if (showPasswords) {
            table.column("Username");
            table.column("Password");
        }

        System.out.println();
        System.out.println("== Remote repositories");
        MavenRepositoryURL[] repositories = repositories(config, true);
        for (MavenRepositoryURL repoURL : repositories) {
            Row row = table.addRow();
            row.addContent(repoURL.getId(), repoURL.getURL());
            if (verbose) {
                row.addContent(repositoryKindInfo(repoURL, false),
                        repositoryKindInfo(repoURL, true),
                        repoURL.getFrom());
            }
            if (showPasswords) {
                if (servers.containsKey(repoURL.getId())) {
                    Server server = servers.get(repoURL.getId());
                    row.addContent(server.getUsername() == null ? "" : server.getUsername());
                    addPasswordInfo(row, serverPasswords, repoURL.getId(), server.getPassword());
                } else {
                    row.addContent("", "");
                }
            }
        }

        table.print(System.out);

        table = new ShellTable();
        table.column("ID");
        table.column("URL");
        if (verbose) {
            table.column("Releases");
            table.column("Snapshots");
        }

        System.out.println();
        System.out.println("== Default repositories");
        repositories = repositories(config, false);
        for (MavenRepositoryURL repoURL : repositories) {
            Row row = table.addRow();
            row.addContent(repoURL.getId(),
                    repoURL.getURL());
            if (verbose) {
                row.addContent(repositoryKindInfo(repoURL, false),
                        repositoryKindInfo(repoURL, true),
                        repoURL.getFrom());
            }
        }

        table.print(System.out);
        System.out.println();
    }

    /**
     * Information about release/snapshot handing for give repository URL
     * @param repoURL
     * @param snapshots
     * @return
     */
    private Object repositoryKindInfo(MavenRepositoryURL repoURL, boolean snapshots) {
        if (snapshots) {
            if (repoURL.isSnapshotsEnabled()) {
                String snapshotsUpdatePolicy = repoURL.getSnapshotsUpdatePolicy();
                return String.format("yes (%s)",
                        snapshotsUpdatePolicy == null || "".equals(snapshotsUpdatePolicy.trim()) ? "daily" : snapshotsUpdatePolicy
                );
            }
        } else {
            if (repoURL.isReleasesEnabled()) {
                String releasesUpdatePolicy = repoURL.getReleasesUpdatePolicy();
                return String.format("yes (%s)",
                        releasesUpdatePolicy == null || "".equals(releasesUpdatePolicy.trim()) ? "daily" : releasesUpdatePolicy
                );
            }
        }

        return "no";
    }

}
