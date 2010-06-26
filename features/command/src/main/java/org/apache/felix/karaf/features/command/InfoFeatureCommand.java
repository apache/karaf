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

import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.FeaturesService;

/**
 * Utility command to display info about features.
 */
@Command(scope = "features", name = "info", description = "Shows information about selected information.")
public class InfoFeatureCommand extends FeaturesCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the feature", required = true, multiValued = false)
    private String name;

    @Argument(index = 1, name = "version", description = "The version of the feature", required = false, multiValued = false)
    private String version;

    @Option(name = "-c", aliases={"--configuration"}, description="Display configuration info", required = false, multiValued = false)
    private boolean config;

    @Option(name = "-d", aliases={"--dependency"}, description="Display dependencies info", required = false, multiValued = false)
    private boolean dependency;

    @Option(name = "-b", aliases={"--bundle"}, description="Display bundles info", required = false, multiValued = false)
    private boolean bundle;

    @Option(name = "-t", aliases={"--tree"}, description="Display feature tree", required = false, multiValued = false)
    private boolean tree;

    protected void doExecute(FeaturesService admin) throws Exception {
        Feature feature = null;

        if (version != null && version.length() > 0) {
            feature = admin.getFeature(name, version);
        } else {
            feature = admin.getFeature(name);
        }

        if (feature == null) {
            System.out.println("Feature not found");
            return;
        }

        // default behavior
        if (!config && !dependency && !bundle) {
            config = true;
            dependency = true;
            bundle = true;
        }

        System.out.println("Description of " + feature.getName() + " " + feature.getVersion() + " feature");
        System.out.println("----------------------------------------------------------------");
        if (config) {
            displayConfigInformation(feature);
        }

        if (dependency) {
            displayDependencyInformation(feature);
        }

        if (bundle) {
            displayBundleInformation(feature);
        }

        if (tree) {
            if (config || dependency || bundle) {
                System.out.println("\nFeature tree");
            }

            int unresolved = displayFeatureTree(admin, feature, 0, false);
            if (unresolved > 0) {
                System.out.println("Tree contains " + unresolved + " unresolved dependencies");
                System.out.println(" * means that node declares dependency but the dependant feature is not available.");
            }
        }
    }

    private void displayBundleInformation(Feature feature) {
        List<String> bundles = feature.getBundles();
        if (bundles.isEmpty()) {
            System.out.println("Feature has no bundles.");
        } else {
            System.out.println("Feature contains followed bundles:");
            for (String featureBundle : bundles) {
                System.out.println("  " + featureBundle);
            }
        }
    }

    private void displayDependencyInformation(Feature feature) {
        List<Feature> dependencies = feature.getDependencies();
        if (dependencies.isEmpty()) {
            System.out.println("Feature has no dependencies.");
        } else {
            System.out.println("Feature depends on:");
            for (Feature featureDependency : dependencies) {
                System.out.println("  " + featureDependency.getName() + " " + featureDependency.getVersion());
            }
        }
    }

    private void displayConfigInformation(Feature feature) {
        Map<String, Map<String, String>> configurations = feature.getConfigurations();
        if (configurations.isEmpty()) {
            System.out.println("Feature has no configuration");
        } else {
            System.out.println("Feature configuration:");
            for (String name : configurations.keySet()) {
                System.out.println("  " + name);
            }
        }
    }


    private int displayFeatureTree(FeaturesService admin, Feature feature, int level, boolean last) throws Exception {
        int unresolved = 0;
        String prefix = repeat("   ", level);

        Feature resolved = resolveFeature(admin, feature);
        if (resolved != null) {
            System.out.println(prefix + " " + resolved.getName() + " " + resolved.getVersion());
        } else {
            System.out.println(prefix + " " + feature.getName() + " " + feature.getVersion() + " *");
            unresolved++;
        }

        if (bundle) {
            List<String> bundles = resolved != null ? resolved.getBundles() : feature.getBundles();
            for (int i = 0, j = bundles.size(); i < j; i++) {
                System.out.println(prefix + " " + (i+1 == j ? "\\" : "+") + " " + bundles.get(i));
            }
        }
        List<Feature> dependencies = resolved != null ? resolved.getDependencies() : feature.getDependencies();
        for (int i = 0, j = dependencies.size(); i < j; i++) {
            Feature toDisplay = resolveFeature(admin, dependencies.get(i));
            if (toDisplay == null) {
                toDisplay = dependencies.get(i);
            }
            unresolved += displayFeatureTree(admin, toDisplay, level+1, i + 1 == j);
        }

        return unresolved;
    }

    private Feature resolveFeature(FeaturesService admin, Feature feature) throws Exception {
        return admin.getFeature(feature.getName(), feature.getVersion());
    }

    private static String repeat(String string, int times) {
        if (times <= 0) {
            return "";
        }
        else if (times % 2 == 0) {
            return repeat(string+string, times/2);
        }
        else {
           return string + repeat(string+string, times/2);
        }
    }
}
