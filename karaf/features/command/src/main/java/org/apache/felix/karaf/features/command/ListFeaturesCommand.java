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
import java.util.List;

import org.apache.felix.karaf.features.FeaturesService;
import org.apache.felix.karaf.features.Feature;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;

@Command(scope = "features", name = "list", description = "List existing features.")
public class ListFeaturesCommand extends FeaturesCommandSupport {

    @Option(name = "-i", aliases={"--installed"}, description="Display the list of installed features")
    boolean installed;

    protected void doExecute(FeaturesService admin) throws Exception {
        List<Feature> features;
        List<Feature> installedFeatures;
        if (installed) {
            features = Arrays.asList(admin.listInstalledFeatures());
            installedFeatures = features;
            if (features == null || features.size() == 0) {
                System.out.println("No features installed.");
                return;
            }
        } else {
            features = Arrays.asList(admin.listFeatures());
            installedFeatures = Arrays.asList(admin.listInstalledFeatures());
            if (features == null || features.size() == 0) {
                System.out.println("No features available.");
                return;
            }
        }
        int maxVersionSize = 7;
        for (Feature feature : features) {
            maxVersionSize = Math.max(maxVersionSize, feature.getVersion().length());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("  State         Version    ");
        for (int i = 7; i < maxVersionSize; i++) {
            sb.append(" ");
        }
        sb.append("Name");
        System.out.println(sb.toString());
        for (Feature feature : features) {
            sb.setLength(0);
            sb.append("[");
            if (installedFeatures.contains(feature)) {
                sb.append("installed  ");
            } else {
                sb.append("uninstalled");
            }
            sb.append("] [");
            String v = feature.getVersion();
            sb.append(v);
            for (int i = v.length(); i < maxVersionSize; i++) {
                sb.append(" ");
            }
            sb.append("] ");
            sb.append(feature.getName());
            System.out.println(sb.toString());
        }
    }

}
