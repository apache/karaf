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
package org.apache.karaf.features.command;

import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.MultiException;
import org.apache.karaf.shell.support.table.Row;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "feature", name = "repo-list", description = "Displays a list of all defined repositories.")
@Service
public class RepoListCommand extends FeaturesCommandSupport {

    @Option(name="-r", description="Reload all feature urls", required = false, multiValued = false)
    boolean reload;

    @Option(name = "-b", aliases = { " --show-blacklisted" }, description = "Also display blacklisted repositories", required = false, multiValued = false)
    boolean showBlacklisted = false;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected void doExecute(FeaturesService featuresService) throws Exception {
        if (reload) {
            reloadAllRepos(featuresService);
        }
        
        ShellTable table = new ShellTable();
        table.column("Repository");
        table.column("URL");
        if (showBlacklisted) {
            table.column("Blacklisted");
        }
        table.emptyTableText("No repositories available");

        Repository[] repos = featuresService.listRepositories();
        for (Repository repo : repos) {
            if (repo != null) {
                if (showBlacklisted || !repo.isBlacklisted()) {
                    Row row = table.addRow();
                    row.addContent(repo.getName(), repo.getURI().toString());
                    if (showBlacklisted) {
                        row.addContent(repo.isBlacklisted() ? "yes" : "no");
                    }
                }
            }
        }
        table.print(System.out, !noFormat);
    }

    private void reloadAllRepos(FeaturesService featuresService) throws Exception {
        System.out.println("Reloading all repositories from their urls");
        System.out.println();
        List<Exception> exceptions = new ArrayList<>();
        for (Repository repo : featuresService.listRepositories()) {
            try {
                featuresService.addRepository(repo.getURI());
            } catch (Exception e) {
                exceptions.add(e);
            }
        }
        MultiException.throwIf("Unable to reload repositories", exceptions);
    }
    
}
