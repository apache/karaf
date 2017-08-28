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

import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.Feature;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.karaf.features.internal.util.MapUtils.filter;

/**
 * Requirement for a feature
 * 
 * <p>The syntax of a requirement as a String is name[/versionRange].
 * If no versionRange is given then a range of [0,) is assumeed which matches all versions.
 * 
 * <p>
 * - name: Can be a feature name or a glob like myfeat*
 * - versionRange: version or range
 * - version: Will specify a specific version. Like [version,version]. An exemption is 0.0.0 which matches all versions.
 * - range: Like defined in OSGi VersionRange. Example: [1.0.0, 1.1.0)  
 */
public class FeatureReq {

    public static final String VERSION_SEPARATOR = "/";

    private static final VersionRange RANGE_ALL = new VersionRange(VersionRange.LEFT_CLOSED, Version.emptyVersion, null, VersionRange.RIGHT_OPEN);
    private static final String FEATURE_OSGI_REQUIREMENT_PREFIX = "feature:";

    private String name;
    private VersionRange versionRange;

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
        this.versionRange = (parts.length == 1) ? RANGE_ALL : range(parts[1]);
    }
    
    public FeatureReq(String name, String versionRange) {
        this.name = name;
        this.versionRange = range(versionRange);
    }
    
    public FeatureReq(String name, VersionRange versionRange) {
        this.name = name;
        this.versionRange = versionRange;
    }
    
    public FeatureReq(Feature feature) {
        this(feature.getName(), exactVersion(feature.getVersion()));
    }
    
    public String getName() {
        return name;
    }
    
    public VersionRange getVersionRange() {
        return versionRange;
    }

    public Set<FeatureReq> getMatchingRequirements(Set<FeatureReq> reqs) {
        Pattern pattern = Pattern.compile(name);
        // TODO: should we use the intersection of the 2 ranges ?
        return filter(reqs, fr -> pattern.matcher(fr.getName()).matches()
                                && versionRange.includes(fr.getVersionRange().getLeft()));
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
                if (availableVersion.compareTo(latest) >= 0 && versionRange.includes(availableVersion)) {
                    feature = versions.get(available);
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
            return RANGE_ALL;
        }
        versionRange = versionRange.trim();
        if ("0.0.0".equals(versionRange)) {
            return RANGE_ALL;
        }
        if (versionRange.contains(",")) {
            return new VersionRange(versionRange);
        } else {
            return exactVersion(versionRange);
        }
    }

    private static VersionRange exactVersion(String versionRange) {
        return new VersionRange(VersionRange.LEFT_CLOSED,
                new Version(versionRange),
                new Version(versionRange),
                VersionRange.RIGHT_CLOSED);
    }

}
