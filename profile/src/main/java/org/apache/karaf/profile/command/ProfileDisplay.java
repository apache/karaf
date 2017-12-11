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
package org.apache.karaf.profile.command;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.profile.Profile;
import org.apache.karaf.profile.ProfileService;
import org.apache.karaf.profile.command.completers.ProfileCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(name = "display", scope = "profile", description = "Displays information about the specified profile")
@Service
public class ProfileDisplay implements Action {

    @Option(name = "--overlay", aliases = "-o", description = "Shows the overlay profile settings, taking into account the settings inherited from parent profiles.")
    private Boolean overlay = false;

    @Option(name = "--effective", aliases = "-e", description = "Shows the effective profile settings, taking into account properties substitution.")
    private Boolean effective = false;

    @Option(name = "--display-resources", aliases = "-r", description = "Displays the content of additional profile resources.")
    private Boolean displayResources = false;

    @Argument(index = 0, required = true, name = "profile", description = "The name of the profile.")
    @Completion(ProfileCompleter.class)
    private String profileId;

    @Reference
    private ProfileService profileService;

    @Override
    public Object execute() {
        displayProfile(profileService.getRequiredProfile(profileId));
        return null;
    }

    private static void printConfigList(String header, PrintStream out, List<String> list) {
        out.println(header);
        for (String str : list) {
            out.printf("\t%s\n", str);
        }
        out.println();
    }

    private void displayProfile(Profile profile) {
        PrintStream output = System.out;

        output.println("Profile id: " + profile.getId());

        output.println("Attributes: ");
        Map<String, String> props = profile.getAttributes();
        for (String key : props.keySet()) {
            output.println("\t" + key + ": " + props.get(key));
        }

        if (overlay) {
            profile = profileService.getOverlayProfile(profile);
        }
        if (effective) {
            profile = profileService.getEffectiveProfile(profile);
        }

        Map<String, Map<String, Object>> configuration = new HashMap<>(profile.getConfigurations());
        Map<String, byte[]> resources = profile.getFileConfigurations();
        Map<String,Object> profileConfiguration = profile.getConfiguration(Profile.INTERNAL_PID);
        List<String> profileProperties = new ArrayList<>();
        List<String> systemProperties = new ArrayList<>();
        List<String> configProperties = new ArrayList<>();
        for (Map.Entry<String, Object> entry : profileConfiguration.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String && ((String) value).contains(",")) {
                value = "\t" + ((String) value).replace(",", ",\n\t\t");
            }

            if (key.startsWith("system.")) {
                systemProperties.add("  " + key.substring("system.".length()) + " = " + value);
            }
            else if (key.startsWith("config.")) {
                configProperties.add("  " + key.substring("config.".length()) + " = " + value);
            }
            else if (!key.startsWith("feature.") && !key.startsWith("repository") &&
                        !key.startsWith("bundle.") && !key.startsWith("fab.") &&
                        !key.startsWith("override.") && !key.startsWith("attribute.")) {
                profileProperties.add("  " + key + " = " + value);
            }
        }

        if (configuration.containsKey(Profile.INTERNAL_PID)) {
            output.println("\nContainer settings");
            output.println("----------------------------");

            if (profile.getLibraries().size() > 0) {
                printConfigList("Libraries : ", output, profile.getLibraries());
            }
            if (profile.getRepositories().size() > 0) {
                printConfigList("Repositories : ", output, profile.getRepositories());
            }
            if (profile.getFeatures().size() > 0) {
                printConfigList("Features : ", output, profile.getFeatures());
            }
            if (profile.getBundles().size() > 0) {
                printConfigList("Bundles : ", output, profile.getBundles());
            }
            if (profile.getOverrides().size() > 0) {
                printConfigList("Overrides : ", output, profile.getOverrides());
            }

            if (profileProperties.size() > 0) {
                printConfigList("Profile Properties : ", output, profileProperties);
            }

            if (systemProperties.size() > 0) {
                printConfigList("System Properties : ", output, systemProperties);
            }

            if (configProperties.size() > 0) {
                printConfigList("Config Properties : ", output, configProperties);
            }

            configuration.remove(Profile.INTERNAL_PID);
        }

        output.println("\nConfiguration details");
        output.println("----------------------------");
        for (Map.Entry<String, Map<String, Object>> cfg : configuration.entrySet()) {
            output.println("PID: " + cfg.getKey());

            for (Map.Entry<String, Object> values : cfg.getValue().entrySet()) {
                output.println("  " + values.getKey() + " " + values.getValue());
            }
            output.println("\n");
        }

        output.println("\nOther resources");
        output.println("----------------------------");
        for (Map.Entry<String,byte[]> resource : resources.entrySet()) {
            String name = resource.getKey();
            if (!name.endsWith(".properties")) {
                output.println("Resource: " + resource.getKey());
                if (displayResources) {
                    output.println(new String(resource.getValue()));
                    output.println("\n");
                }
            }
        }
    }

}
