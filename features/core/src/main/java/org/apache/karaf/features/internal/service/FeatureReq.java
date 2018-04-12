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
package org.apache.karaf.features.internal.service;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Version;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.apache.felix.utils.version.VersionRange.ANY_VERSION;
import static org.apache.karaf.features.internal.util.MapUtils.filter;

/**
 * Requirement for a feature
 * 
 * <p>The syntax of a requirement as a String is name[/versionRange].
 * If no versionRange is given then a range of [0,) is assumed which matches all versions.
 * 
 * <p>
 * - name: Can be a feature name or a regexp like myfeat.*
 * - versionRange: version or range
 * - version: Will specify a specific version. Like [version,version]. An exemption is 0.0.0 which matches all versions.
 * - range: Like defined in OSGi VersionRange. Example: [1.0.0, 1.1.0)  
 */
public class FeatureReq {

    public static final String VERSION_SEPARATOR = "/";

    private static final String FEATURE_OSGI_REQUIREMENT_PREFIX = "feature:";

    private String name;
    private VersionRange versionRange;
    private boolean blacklisted = false;

    public static FeatureReq parseRequirement(String featureReq) {
        if (!featureReq.startsWith(FEATURE_OSGI_REQUIREMENT_PREFIX)) {
            return null;
        }
        String featureReq1 = featureReq.substring(FEATURE_OSGI_REQUIREMENT_PREFIX.length());
        return parseNameAndRange(featureReq1);
    }

    public static FeatureReq parseNameAndRange(String nameAndRange) {
        return new FeatureReq(nameAndRange);
    }

    public FeatureReq(String nameAndRange) {
        String[] parts = nameAndRange.trim().split(VERSION_SEPARATOR);
        this.name = parts[0];
        this.versionRange = (parts.length == 1) ? ANY_VERSION : range(parts[1]);
    }
    
    public FeatureReq(String name, String versionRange) {
        this.name = name;
        this.versionRange = range(versionRange);
    }
    
    public FeatureReq(String name, VersionRange versionRange, boolean blacklisted) {
        this.name = name;
        this.versionRange = versionRange;
        this.blacklisted = blacklisted;
    }
    
    public FeatureReq(Feature feature) {
        this(feature.getName(), exactVersion(feature.getVersion()), feature.isBlacklisted());
    }
    
    public String getName() {
        return name;
    }
    
    public VersionRange getVersionRange() {
        return versionRange;
    }

    public Set<FeatureReq> getMatchingRequirements(Set<FeatureReq> reqs) {
        Pattern pattern = Pattern.compile(name);
        return filter(reqs, fr -> pattern.matcher(fr.getName()).matches()
                                && versionRange.intersect(fr.getVersionRange()) != null);
    }

    public Stream<Feature> getMatchingFeatures(Map<String, Map<String, Feature>> allFeatures) {
        Pattern pattern = Pattern.compile(name);
        Function<String, Optional<Feature>> func = featureName -> {
            Feature matchingFeature = null;
            if (pattern.matcher(featureName).matches()) {
                Map<String, Feature> versions = allFeatures.get(featureName);
                matchingFeature = getLatestFeature(versions, versionRange);
            }
            return Optional.ofNullable(matchingFeature);
        };
        return allFeatures.keySet().stream().map(func).filter(Optional::isPresent).map(Optional::get);
    }

    private static Feature getLatestFeature(Map<String, Feature> versions, VersionRange versionRange) {
        Feature feature = null;
        if (versions != null && !versions.isEmpty()) {
            Version latest = Version.emptyVersion;
            for (String available : versions.keySet()) {
                Version availableVersion = VersionTable.getVersion(available);
                if (availableVersion.compareTo(latest) >= 0 && versionRange.contains(availableVersion)) {
                    Feature possiblyBlacklisted = versions.get(available);
                    // return only if there are no more non-blaclisted features
                    if (feature == null || !possiblyBlacklisted.isBlacklisted()) {
                        feature = possiblyBlacklisted;
                    }
                    latest = availableVersion;
                }
            }
        }
        return feature;
    }

    @Override
    public String toString() {
        return this.name + "/" + this.getVersionRange().toString();
    }

    public String toRequirement() {
        return FEATURE_OSGI_REQUIREMENT_PREFIX + toString();
    }

    public boolean isBlacklisted() {
        return blacklisted;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FeatureReq that = (FeatureReq) o;
        return Objects.equals(name, that.name) &&
               Objects.equals(versionRange, that.versionRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, versionRange);
    }

    private static VersionRange range(String versionRange) {
        if (versionRange == null) {
            return ANY_VERSION;
        }
        versionRange = versionRange.trim();
        if ("0.0.0".equals(versionRange)) {
            return ANY_VERSION;
        }
        if (versionRange.contains(",")) {
            return new VersionRange(versionRange, false, true);
        } else {
            return exactVersion(versionRange);
        }
    }

    private static VersionRange exactVersion(String versionRange) {
        return new VersionRange(versionRange, true, true);
    }

}
