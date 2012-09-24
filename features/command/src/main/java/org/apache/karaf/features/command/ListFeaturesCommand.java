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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    protected void doExecute(FeaturesService featuresService) throws Exception {
        boolean needsLegend = false;
        
        ShellTable table = new ShellTable();
        table.column("Name");
        table.column("Version");
        table.column("Installed");
        table.column("Repository");
        table.column("Description");
        table.emptyTableText(onlyInstalled ? "No features installed" : "No features available");

        List<Repository> repos = Arrays.asList(featuresService.listRepositories());
        for (Repository r : repos) {
            List<Feature> features = Arrays.asList(r.getFeatures());
            if (ordered) {
                Collections.sort(features, new FeatureComparator());
            }
            for (Feature f : features) {
                if (onlyInstalled && !featuresService.isInstalled(f)) {
                    // Filter out not installed features if we only want to see the installed ones
                    continue;
                }
                table.addRow().addContent(
                        f.getName(),
                        f.getVersion(),
                        featuresService.isInstalled(f) ? "x" : "",
                        r.getName(),
                        f.getDescription());
                if (isInstalledViaDeployDir(r.getName())) {
                    needsLegend = true;
                }
            }
        }

        table.print(System.out);

        if (needsLegend) {
            System.out.println("* Installed via deploy directory");
        }

    }

    private boolean isInstalledViaDeployDir(String st) {
        return (st == null || st.length() <= 1) ? false : (st.charAt(st.length() - 1) == '*');
    }

    class FeatureComparator implements Comparator<Feature> {
        public int compare(Feature o1, Feature o2) {
            return o1.getName().toLowerCase().compareTo( o2.getName().toLowerCase() );
        }
    }

}
