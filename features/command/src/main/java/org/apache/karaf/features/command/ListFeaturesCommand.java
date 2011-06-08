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

import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Command(scope = "features", name = "list", description = "Lists all existing features available from the defined repositories.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases = {"--installed"}, description = "Display a list of all installed features only", required = false, multiValued = false)
    boolean installed;

    private static final String STATE = "State";
    private static final String INSTALLED = "installed";
    private static final String UNINSTALLED = "uninstalled";

    private static final String VERSION = "Version";
    private static final String NAME = "Name";
    private static final String REPOSITORY = "Repository";
    private static final String DESCRIPTION = "Description";

    protected void doExecute(FeaturesService admin) throws Exception {
        List<InfoLine> lines = new ArrayList<ListFeaturesCommand.InfoLine>();
        boolean needsLegend = false;

        // Get the feature data to print.
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {
                if (installed && !admin.isInstalled(f)) {
                    // Filter out not installed features if we only want to see the installed ones
                    continue;
                }
                InfoLine line = new InfoLine();
                line.state = admin.isInstalled(f) ? INSTALLED : UNINSTALLED;
                line.version = getSafeString(f.getVersion());
                line.name = getSafeString(f.getName());
                line.repoName = getSafeString(r.getName());
                line.description = getSafeString(f.getDescription());
                if (isInstalledViaDeployDir(line.repoName)) {
                    needsLegend = true;
                }
                lines.add(line);
            }
        }
        if (lines.size() == 0) {
            if (installed) {
                System.out.println("No features installed.");
            } else {
                System.out.println("No features available.");
            }
            return;
        }

        // Determine size of columns
        int maxVersionSize = VERSION.length();
        int maxNameSize = NAME.length();
        int maxRepositorySize = REPOSITORY.length();
        for (InfoLine line : lines) {
            maxVersionSize = Math.max(maxVersionSize, line.version.length());
            maxNameSize = Math.max(maxNameSize, line.name.length());
            maxRepositorySize = Math.max(maxRepositorySize, line.repoName.length());
        }

        // Print feature info in columns
        String formatHeader = "%-13s %-17s %-" + maxNameSize + "s %-" + maxRepositorySize + "s %s";
        String formatLine = "[%-11s] [%-15s] %-" + maxNameSize + "s %-" + maxRepositorySize + "s %s";
        System.out.println(String.format(formatHeader, STATE, VERSION, NAME, REPOSITORY, DESCRIPTION));
        for (InfoLine line : lines) {
            System.out.println(String.format(formatLine, line.state, line.version, line.name, line.repoName, line.description));
        }

        if (needsLegend) {
            System.out.println("* Installed via deploy directory");
        }

    }

    private String getSafeString(String st) {
        return st == null ? "" : st;
    }
    
    private boolean isInstalledViaDeployDir(String st) {
        return (st == null || st.length() <= 1) ? false : (st.charAt(st.length() - 1) == '*');
    }

    private class InfoLine {
        String state;
        String version;
        String name;
        String repoName;
        String description;
    }
    
}
