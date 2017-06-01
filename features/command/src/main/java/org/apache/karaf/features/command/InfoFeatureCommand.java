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

import java.util.LinkedList;
import java.util.List;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.ConfigFileInfo;
import org.apache.karaf.features.ConfigInfo;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.command.completers.AllFeatureCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "feature", name = "info", description = "Shows information about selected feature.")
@Service
public class InfoFeatureCommand extends FeaturesCommandSupport {

    private static final String INDENT = "  ";
    private static final String FEATURE_CONTENT = "Feature";
    private static final String CONDITIONAL_CONTENT = "Conditional(%s)";

    @Argument(index = 0, name = "name", description = "The name of the feature", required = true, multiValued = false)
    @Completion(AllFeatureCompleter.class)
    private String name;

    @Argument(index = 1, name = "version", description = "The version of the feature", required = false, multiValued = false)
    private String version;

    @Option(name = "-c", aliases={"--configuration"}, description="Display configuration info", required = false, multiValued = false)
    private boolean config;

    @Option(name = "-d", aliases={"--dependency"}, description="Display dependencies info", required = false, multiValued = false)
    private boolean dependency;

    @Option(name = "-b", aliases={"--bundle"}, description="Display bundles info", required = false, multiValued = false)
    private boolean bundle;

    @Option(name = "--conditional", description="Display conditional info", required = false, multiValued = false)
    private boolean conditional;

    @Option(name = "-t", aliases={"--tree"}, description="Display feature tree", required = false, multiValued = false)
    private boolean tree;

    protected void doExecute(FeaturesService admin) throws Exception {
        Feature[] features = null;

        if (version != null && version.length() > 0) {
            features = admin.getFeatures(name, version);
        } else {
            features = admin.getFeatures(name);
        }

        if (features == null || features.length == 0) {
            System.out.println("Feature not found");
            return;
        }

        // default behavior
        if (!config && !dependency && !bundle && !conditional) {
            config = true;
            dependency = true;
            bundle = true;
            conditional = true;
        }

        boolean first = true;
        for (Feature feature : features) {
            if (first) {
                first = false;
            } else {
                System.out.println("------------------------------------");
            }
            System.out.println("Feature " + feature.getName() + " " + feature.getVersion());
            if (feature.getDescription() != null) {
                System.out.println("Description:");
                System.out.println(INDENT + feature.getDescription());
            }

            if (feature.getDetails() != null) {
                System.out.println("Details:");
                printWithIndent(feature.getDetails());
            }

            if (config) {
                displayConfigInformation(feature, FEATURE_CONTENT);
                displayConfigFileInformation(feature, FEATURE_CONTENT);
            }

            if (dependency) {
                displayDependencyInformation(feature, FEATURE_CONTENT);
            }

            if (bundle) {
                displayBundleInformation(feature, FEATURE_CONTENT);
            }

            if (conditional) {
                displayConditionalInfo(feature);
            }

            if (tree) {
                if (config || dependency || bundle) {
                    System.out.println("\nFeature tree");
                }

                int unresolved = displayFeatureTree(admin, feature.getName(), feature.getVersion(), "");
                if (unresolved > 0) {
                    System.out.println("Tree contains " + unresolved + " unresolved dependencies");
                    System.out.println(" * means that node declares dependency but the dependent feature is not available.");
                }
            }
        }
    }

    private void printWithIndent(String details) {
    	String[] lines = details.split("\r?\n");
    	for (String line : lines) {
			System.out.println(INDENT + line);
		}
	}

	private void displayBundleInformation(Feature feature, String contentType) {
        List<BundleInfo> bundleInfos = feature.getBundles();
        if (bundleInfos.isEmpty()) {
            System.out.println(contentType + " has no bundles.");
        } else {
            System.out.println(contentType + " contains followed bundles:");
            for (BundleInfo featureBundle : bundleInfos) {
                int startLevel = featureBundle.getStartLevel();
                StringBuilder sb = new StringBuilder();
                sb.append(INDENT).append(featureBundle.getLocation());
                if(startLevel > 0) {
                    sb.append(" start-level=").append(startLevel);
                }
                System.out.println(sb.toString());
            }
        }
    }

    private void displayDependencyInformation(Feature feature, String contentType) {
        List<Dependency> dependencies = feature.getDependencies();
        if (dependencies.isEmpty()) {
            System.out.println(contentType + " has no dependencies.");
        } else {
            System.out.println(contentType + " depends on:");
            for (Dependency featureDependency : dependencies) {
                System.out.println(INDENT + featureDependency.getName() + " " + featureDependency.getVersion());
            }
        }
    }

    private void displayConfigInformation(Feature feature, String contentType) {
		List<ConfigInfo> configurations = feature.getConfigurations();
        if (configurations.isEmpty()) {
            System.out.println(contentType + " has no configuration");
        } else {
            System.out.println(contentType + " configuration:");
			for (ConfigInfo configInfo : configurations) {
				System.out.println(INDENT + configInfo.getName());
            }
        }
    }
    
    private void displayConfigFileInformation(Feature feature, String contentType) {
    	List<ConfigFileInfo> configurationFiles = feature.getConfigurationFiles();
    	if (configurationFiles.isEmpty()) {
    		System.out.println(contentType + " has no configuration files");
    	} else {
    		System.out.println(contentType + " configuration files: ");
    		for (ConfigFileInfo configFileInfo : configurationFiles) {
				System.out.println(INDENT + configFileInfo.getFinalname());
			}
    	}    	
    }

    /**
     * Called originally with featureName and featureVersion that have already been resolved successfully.
     *
     * @param admin
     * @param featureName
     * @param featureVersion
     * @param prefix
     * @return
     * @throws Exception
     */
    private int displayFeatureTree(FeaturesService admin, String featureName, String featureVersion, String prefix) throws Exception {
        int unresolved = 0;

        Feature[] resolvedFeatures = admin.getFeatures(featureName, featureVersion);
        for (Feature resolved:resolvedFeatures) {
            if (resolved != null) {
                System.out.println(prefix + " " + resolved.getName() + " " + resolved.getVersion());
            } else {
                System.out.println(prefix + " " + featureName + " " + featureVersion + " *");
                unresolved++;
            }

            if (resolved != null) {
                if (bundle) {
                    List<String> bundleLocation = new LinkedList<>();
                    List<BundleInfo> bundles = resolved.getBundles();
                    for (BundleInfo bundleInfo : bundles) {
                        bundleLocation.add(bundleInfo.getLocation());
                    }

                    if (conditional) {
                        for (Conditional cond : resolved.getConditional()) {
                            List<String> condition = cond.getCondition();
                            List<BundleInfo> conditionalBundles = cond.getBundles();
                            for (BundleInfo bundleInfo : conditionalBundles) {
                                bundleLocation.add(bundleInfo.getLocation() + "(condition:" + condition + ")");
                            }
                        }
                    }
                    for (int i = 0, j = bundleLocation.size(); i < j; i++) {
                        System.out.println(prefix + " " + (i + 1 == j ? "\\" : "+") + " " + bundleLocation.get(i));
                    }
                }
                prefix += "   ";
                List<Dependency> dependencies = resolved.getDependencies();
                for (Dependency toDisplay : dependencies) {
                    unresolved += displayFeatureTree(admin, toDisplay.getName(), toDisplay.getVersion(), prefix + 1);
                }

                if (conditional) {
                    for (Conditional cond : resolved.getConditional()) {
                        List<Dependency> conditionDependencies = cond.getDependencies();
                        for (int i = 0, j = conditionDependencies.size(); i < j; i++) {
                            Dependency toDisplay = dependencies.get(i);
                            unresolved += displayFeatureTree(admin, toDisplay.getName(), toDisplay.getVersion(), prefix + 1);
                        }
                    }
                }
            }
        }

        return unresolved;
    }

    private void displayConditionalInfo(Feature feature) {
        List<? extends Conditional> conditionals = feature.getConditional();
        if (conditionals.isEmpty()) {
            System.out.println("Feature has no conditionals.");
        } else {
            System.out.println("Feature contains followed conditionals:");
            for (Conditional featureConditional : conditionals) {
                String conditionDescription = getConditionDescription(featureConditional);
                Feature wrappedConditional = featureConditional.asFeature();
                if (config) {
                    displayConfigInformation(wrappedConditional, String.format(CONDITIONAL_CONTENT, conditionDescription));
                    displayConfigFileInformation(wrappedConditional, String.format(CONDITIONAL_CONTENT, conditionDescription));
                }

                if (dependency) {
                    displayDependencyInformation(wrappedConditional, String.format(CONDITIONAL_CONTENT, conditionDescription));
                }

                if (bundle) {
                    displayBundleInformation(wrappedConditional, String.format(CONDITIONAL_CONTENT, conditionDescription));
                }
            }
        }
    }

    private String getConditionDescription(Conditional cond) {
        StringBuffer sb = new StringBuffer();
        for (String dep : cond.getCondition()) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(dep);
        }
        return sb.toString();
    }

}
