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

import java.util.*;

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.table.ShellTable;

@Command(scope = "feature", name = "list", description = "Lists all existing features available from the defined repositories.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases = {"--installed"}, description = "Display a list of all installed features only", required = false, multiValued = false)
    boolean onlyInstalled;

    @Option(name = "-o", aliases = {"--ordered"}, description = "Display a list using alphabetical order ", required = false, multiValued = false)
    boolean ordered;

    @Option(name = "--no-format", description = "Disable table rendered output", required = false, multiValued = false)
    boolean noFormat;

    protected void doExecute(FeaturesService featuresService) throws Exception {
        boolean needsLegend = false;

        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Version");
        table.column("Installed");
        table.column("Repository");
        table.column("Description").maxSize(50);
        table.emptyTableText(onlyInstalled ? "No features installed" : "No features available");

        List<FeatureAndRepository> featuresAndRepositories = new ArrayList<FeatureAndRepository>();
        for (Repository repository : Arrays.asList(featuresService.listRepositories())) {
            for (Feature feature : Arrays.asList(repository.getFeatures())) {
                if (onlyInstalled && !featuresService.isInstalled(feature)) {
                    continue;
                }
                featuresAndRepositories.add(new FeatureAndRepository(feature, repository));
            }
        }

        if (ordered) {
            Collections.sort(featuresAndRepositories, new FeatureAndRepositoryComparator());
        }

        for (FeatureAndRepository far : featuresAndRepositories) {
            table.addRow().addContent(
                    far.feature.getName(),
                    far.feature.getVersion(),
                    featuresService.isInstalled(far.feature) ? "x" : "",
                    far.repository.getName(),
                    far.feature.getDescription());
            if (isInstalledViaDeployDir(far.repository.getName())) {
                needsLegend = true;
            }
        }

        table.print(System.out, !noFormat);

        if (needsLegend) {
            System.out.println("* Installed via deploy directory");
        }

    }

    private boolean isInstalledViaDeployDir(String st) {
        return (st == null || st.length() <= 1) ? false : (st.charAt(st.length() - 1) == '*');
    }

    class FeatureAndRepository {
        public Feature feature;
        public Repository repository;

        FeatureAndRepository(Feature feature, Repository repository) {
            this.feature = feature;
            this.repository = repository;
        }
    }

    class FeatureAndRepositoryComparator implements Comparator<FeatureAndRepository> {
        public int compare(FeatureAndRepository o1, FeatureAndRepository o2) {
            return o1.feature.getName().toLowerCase().compareTo( o2.feature.getName().toLowerCase() );
        }
    }

}
