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

import org.apache.karaf.features.Feature;
import org.osgi.framework.Version;
import org.osgi.framework.VersionRange;

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
    private static Version HIGHEST = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static final VersionRange RANGE_ALL = new VersionRange(VersionRange.LEFT_CLOSED, Version.emptyVersion, HIGHEST, VersionRange.RIGHT_CLOSED);
    private String name;
    private VersionRange versionRange;
    
    public FeatureReq(String nameAndRange) {
        String[] parts = nameAndRange.trim().split(VERSION_SEPARATOR);
        this.name = parts[0];
        this.versionRange = (parts.length == 1) ? RANGE_ALL : range(parts[1]);
    }
    
    public FeatureReq(String name, String versionRange) {
        this.name = name;
        this.versionRange = range(versionRange);
    }
    
    private VersionRange range(String versionRange) {
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
    
    @Override
    public String toString() {
        return this.name + "/" + this.getVersionRange().toString();
    }
}
