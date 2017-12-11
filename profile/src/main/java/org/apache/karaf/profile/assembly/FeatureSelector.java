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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.internal.model.Dependency;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.service.FeatureReq;

public class FeatureSelector {

    private final Set<Feature> allFeatures;
    Map<String, Set<Feature>> featuresCache;

    public FeatureSelector(Set<Feature> features) {
        allFeatures = features;
        featuresCache = new HashMap<>();
        for (Feature feature : features) {
            featuresCache.computeIfAbsent(feature.getName(), fn -> new HashSet<>())
                .add(feature);
        }
    }

    /**
     * Assuming <code>idOrPattern</code> may be a pattern (with glob and version range), get all matching features
     * @param idOrPattern
     * @param repositories
     * @return
     */
    public static Collection<String> getMatchingFeatures(String idOrPattern, Collection<Features> repositories) {
        List<String> result = new LinkedList<>();
        FeaturePattern pattern = new FeaturePattern(idOrPattern);
        for (Features features : repositories) {
            for (Feature feature : features.getFeature()) {
                // blacklisting will be applied anyway, so no need to do it here
                if (/*!feature.isBlacklisted() && */pattern.matches(feature.getName(), feature.getVersion())) {
                    result.add(feature.getId());
                }
            }
        }
        return result;
    }

    /**
     * Get all matching features
     * @param idOrPattern
     * @param features
     * @return
     */
    public static Collection<String> getMatchingFeatures(FeaturePattern idOrPattern, Collection<Feature> features) {
        List<String> result = new LinkedList<>();
        for (Feature feature : features) {
            // blacklisting will be applied anyway, so no need to do it here
            if (/*!feature.isBlacklisted() && */idOrPattern.matches(feature.getName(), feature.getVersion())) {
                result.add(feature.getId());
            }
        }
        return result;
    }

    /**
     * Features matching the given feature selectors including dependent features
     *
     * @param features feature selector name, name/version, name/version-range
     *
     * @return matching features
     */
    public Set<Feature> getMatching(List<String> features) {
        Set<Feature> selected = new HashSet<>();
        for (String feature : features) {
            for (String featureId : getMatchingFeatures(new FeaturePattern(feature), allFeatures)) {
                addFeatures(featureId, selected, true);
            }
        }
        return selected;
    }

    private void addFeatures(String feature, Set<Feature> features, boolean mandatory) {
        Set<Feature> set = getMatching(feature);
        if (mandatory && set.isEmpty()) {
            throw new IllegalStateException("Could not find matching feature for " + feature);
        }
        for (Feature f : set) {
            if (features.add(f)) {
                for (Dependency dep : f.getFeature()) {
                    addFeatures(dep.toString(), features, isMandatory(dep));
                }
            }
        }
    }

    private boolean isMandatory(Dependency dep) {
        return !dep.isDependency() && !dep.isPrerequisite();
    }

    private Set<Feature> getMatching(String nameAndVersion) {
        FeatureReq req = new FeatureReq(nameAndVersion);
        Set<Feature> versionToFeatures = featuresCache.get(req.getName());
        if (versionToFeatures == null) {
            return Collections.emptySet();
        }
        return versionToFeatures.stream()
            .filter(f -> f.getName().equals(req.getName()) && req.getVersionRange().contains(VersionTable.getVersion(f.getVersion())))
            .collect(Collectors.toSet());  
    }
}
