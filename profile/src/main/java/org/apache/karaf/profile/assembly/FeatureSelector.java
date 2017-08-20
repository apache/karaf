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
package org.apache.karaf.profile.assembly;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.osgi.framework.Version;

public class FeatureSelector {
    Map<String, Set<Feature>> featuresCache;

    public FeatureSelector(Set<Feature> features) {
        featuresCache = new HashMap<>();
        for (Feature feature : features) {
            featuresCache.computeIfAbsent(feature.getName(), fn -> new HashSet<>())
                .add(feature);
        }
    }
    
    public Set<Feature> selectMatching(List<String> features) {
        Set<Feature> selected = new HashSet<>();
        for (String feature : features) {
            addFeatures(feature, selected, true);
        }
        return selected;
    }
    
    private void addFeatures(String feature, Set<Feature> features, boolean mandatory) {
        Set<Feature> set = getMatching(feature);
        if (mandatory && set.isEmpty()) {
            throw new IllegalStateException("Could not find matching feature for " + feature);
        }
        for (Feature f : set) {
            features.add(f);
            for (Dependency dep : f.getFeature()) {
                addFeatures(dep.toString(), features, isMandatory(dep));
            }
        }
    }

    private boolean isMandatory(Dependency dep) {
        return !dep.isDependency() && !dep.isPrerequisite();
    }

    private Set<Feature> getMatching(String nameAndVersion) {
        VersionRange range;
        String name;
        int idx = nameAndVersion.indexOf('/');
        if (idx > 0) {
            name = nameAndVersion.substring(0, idx);
            String version = nameAndVersion.substring(idx + 1);
            version = version.trim();
            if (version.equals(org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION)) {
                range = new VersionRange(Version.emptyVersion);
            } else {
                range = new VersionRange(version, true, true);
            }
        } else {
            name = nameAndVersion;
            range = new VersionRange(Version.emptyVersion);
        }
        Set<Feature> versionToFeatures = featuresCache.get(name);
        if (versionToFeatures == null) {
            return Collections.emptySet();
        }
        return versionToFeatures.stream()
            .filter(f -> f.getName().equals(name) && range.contains(VersionTable.getVersion(f.getVersion())))
            .collect(Collectors.toSet());  
    }
}
