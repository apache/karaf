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
package org.apache.felix.karaf.features.command;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.karaf.features.FeaturesService;
import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.Repository;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "features", name = "list", description = "List existing features.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases={"--installed"}, description="Display the list of installed features")
    boolean installed;

    private static final String STATE = "State";
    private static final String INSTALLED = "installed  ";
    private static final String UNINSTALLED = "uninstalled";

    private static final String VERSION = "Version";
    private static final String NAME = "Name";
    private static final String REPOSITORY = "Repository";

    protected void doExecute(FeaturesService admin) throws Exception {

        // Get the feature data to print.
        List<Feature> features = new ArrayList<Feature>();
        List<Repository> repositories = new ArrayList<Repository>();
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {
                if (installed && !admin.isInstalled(f)) {
                    continue;
                }
                features.add(f);
                repositories.add(r);
            }
        }
        if (features.size() == 0) {
            if (installed) {
                System.out.println("No features installed.");
            }
            else {
                System.out.println("No features available.");
            }
            return;
        }

        // Print column headers.
        int maxVersionSize = VERSION.length();
        for (Feature f : features) {
            maxVersionSize = Math.max(maxVersionSize, f.getVersion().length());
        }
        int maxNameSize = NAME.length();
        for (Feature f : features) {
            maxNameSize = Math.max(maxNameSize, f.getName().length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append(STATE).append("         ").append(VERSION).append("   ");
        for (int i = VERSION.length(); i < maxVersionSize; i++) {
            sb.append(" ");
        }
        sb.append(NAME).append(" ");
        for (int i = NAME.length(); i < maxNameSize; i++) {
            sb.append(" ");
        }
        sb.append(REPOSITORY);
        System.out.println(sb.toString());

        // Print the feature data.
        boolean needsLegend = false;
        for (Feature f : features) {

            sb.setLength(0);
            sb.append("[");
            if (admin.isInstalled(f)) {
                sb.append(INSTALLED);
            } else {
                sb.append(UNINSTALLED);
            }

            sb.append("] [");
            String str = f.getVersion();
            sb.append(str);
            for (int i = str.length(); i < maxVersionSize; i++) {
                sb.append(" ");
            }
            sb.append("] ");

            str = f.getName();
            sb.append(str);
            for (int i = str.length(); i < maxNameSize; i++) {
                sb.append(" ");
            }

            sb.append(" ");
            String name = repositories.get(0).getName();
            sb.append(name);
            repositories.remove(0);
            System.out.println(sb.toString());
            if (name.charAt(name.length() - 1) == '*') {
                needsLegend = true;
            }

        }

        if (needsLegend) {
            System.out.println("* Installed via deploy directory");
        }

    }

}
