/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.features;

import java.util.regex.Pattern;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.osgi.framework.Version;

/**
 * <p>Helper class to compare feature identifiers that may use globs and version ranges.</p>
 *
 * <p>Following feature identifiers are supported:<ul>
 *     <li>name (simple name)</li>
 *     <li>name/version (Karaf feature ID syntax)</li>
 *     <li>name/version-range (Karaf feature ID syntax using version-range)</li>
 *     <li>name;range=version (OSGi manifest header with <code>range</code> <em>attribute</em>)</li>
 *     <li>name;range=version-range (OSGi manifest header with <code>range</code> <em>attribute</em>)</li>
 * </ul></p>
 */
public class FeaturePattern {

    public static final String RANGE = "range";

    private String originalId;
    private String nameString;
    private Pattern namePattern;
    private String versionString;
    private Version version;
    private VersionRange versionRange;

    public FeaturePattern(String featureId) throws IllegalArgumentException {
        if (featureId == null) {
            throw new IllegalArgumentException("Feature ID to match should not be null");
        }
        originalId = featureId;
        nameString = originalId;
        if (originalId.indexOf("/") > 0) {
            nameString = originalId.substring(0, originalId.indexOf("/"));
            versionString = originalId.substring(originalId.indexOf("/") + 1);
        } else if (originalId.contains(";")) {
            Clause[] c = org.apache.felix.utils.manifest.Parser.parseClauses(new String[] { originalId });
            nameString = c[0].getName();
            versionString = c[0].getAttribute(RANGE);
        }
        namePattern = LocationPattern.toRegExp(nameString);

        if (versionString != null && versionString.length() >= 1) {
            try {
                char first = versionString.charAt(0);
                if (first == '[' || first == '(') {
                    // range
                    versionRange = new VersionRange(versionString, true, false);
                } else {
                    version = new Version(VersionCleaner.clean(versionString));
                }
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Can't parse version \"" + versionString + "\" as OSGi version object.", e);
            }
        } else {
            versionRange = new VersionRange(Version.emptyVersion);
        }
    }

    public String getOriginalFeatureId() {
        return originalId;
    }

    public String getName() {
        return nameString;
    }

    public String getVersion() {
        return versionString;
    }

    /**
     * Returns <code>true</code> if this feature pattern matches given feature/version
     * @param featureName
     * @param featureVersion
     * @return
     */
    public boolean matches(String featureName, String featureVersion) {
        if (featureName == null) {
            return false;
        }
        boolean match = namePattern.matcher(featureName).matches();
        if (!match) {
            return false;
        }
        if (featureVersion == null) {
            featureVersion = "0";
        }
        Version otherVersion = new Version(VersionCleaner.clean(featureVersion));
        if (versionRange != null) {
            match = versionRange.contains(otherVersion);
        } else if (version != null) {
            match = version.equals(otherVersion);
        }
        return match;
    }

}
