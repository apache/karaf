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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Command(scope = "features", name = "list", description = "Lists all existing features available from the defined repositories.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases = {"--installed"}, description = "Display a list of all installed features only", required = false, multiValued = false)
    boolean installed;

    @Option(name = "-o", aliases = {"--ordered"}, description = "Display a list using alphabetical order ", required = false, multiValued = false)
    boolean ordered;
    private static final String STATE = "State";
    private static final String INSTALLED = "installed  ";
    private static final String UNINSTALLED = "uninstalled";

    private static final String VERSION = "Version";
    private static final String NAME = "Name";
    private static final String REPOSITORY = "Repository";
    private static final String DESCRIPTION = "Description";

    protected void doExecute(FeaturesService admin) throws Exception {

        // Get the feature data to print.
        List<FeatureAndRepository> featuresAndRepositories = new ArrayList<FeatureAndRepository>();
        for (Repository r : Arrays.asList(admin.listRepositories())) {
            for (Feature f : r.getFeatures()) {
                if (installed && !admin.isInstalled(f)) {
                    continue;
                }
                featuresAndRepositories.add(new FeatureAndRepository(f, r));
            }
        }
        if (featuresAndRepositories.size() == 0) {
            if (installed) {
                System.out.println("No features installed.");
            } else {
                System.out.println("No features available.");
            }
            return;
        }

        // Print column headers.
        int maxVersionSize = VERSION.length();
        for (FeatureAndRepository far : featuresAndRepositories) {
            maxVersionSize = Math.max(maxVersionSize, far.feature.getVersion().length());
        }
        int maxNameSize = NAME.length();
        for (FeatureAndRepository far : featuresAndRepositories) {
            maxNameSize = Math.max(maxNameSize, far.feature.getName().length());
        }
        int maxRepositorySize = REPOSITORY.length();
        for (FeatureAndRepository far : featuresAndRepositories) {
            maxRepositorySize = Math.max(maxRepositorySize, far.repository.getName().length());
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
        sb.append(REPOSITORY).append(" ");

        for (int i = REPOSITORY.length(); i < maxRepositorySize; i++) {
            sb.append(" ");
        }
        sb.append(DESCRIPTION);
        System.out.println(sb.toString());

        // Print the feature data.
        boolean needsLegend = false;
        if (ordered) {
            Collections.sort(featuresAndRepositories, new FeatureAndRepositoryComparator());
        }
        for (FeatureAndRepository far : featuresAndRepositories) {

            sb.setLength(0);
            sb.append("[");
            if (admin.isInstalled(far.feature)) {
                sb.append(INSTALLED);
            } else {
                sb.append(UNINSTALLED);
            }

            sb.append("] [");
            String str = far.feature.getVersion();
            sb.append(str);
            for (int i = str.length(); i < maxVersionSize; i++) {
                sb.append(" ");
            }
            sb.append("] ");

            str = far.feature.getName();
            sb.append(str);
            for (int i = str.length(); i < maxNameSize; i++) {
                sb.append(" ");
            }

            sb.append(" ");
            String name = far.repository.getName();
            sb.append(name);

            if (name.charAt(name.length() - 1) == '*') {
                needsLegend = true;
            }

            for (int i = name.length(); i < maxRepositorySize; i++) {
                sb.append(" ");
            }

            sb.append(" ");
            String description = "";
            if (far.feature.getDescription() != null) {
                description = far.feature.getDescription();
            }
            sb.append(description);

            System.out.println(sb.toString());
        }

        if (needsLegend) {
            System.out.println("* Installed via deploy directory");
        }

    }

    class FeatureAndRepository
    {
        public Feature feature;
        public Repository repository;

        FeatureAndRepository(Feature feature, Repository repository) {
            this.feature = feature;
            this.repository = repository;
        }
    }

    class FeatureAndRepositoryComparator implements Comparator<FeatureAndRepository>
    {
        public int compare(FeatureAndRepository o1, FeatureAndRepository o2)
        {
            return o1.feature.getName().toLowerCase().compareTo( o2.feature.getName().toLowerCase() );
        }
    }

}
