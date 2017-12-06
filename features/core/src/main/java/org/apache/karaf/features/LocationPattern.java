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

import java.net.MalformedURLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.util.maven.Parser;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Helper class to compare Maven URIs (and falling back to other URIs) that may use globs and version ranges.</p>
 *
 * <p>Each Maven URI may contain these components: groupId, artifactId, optional version, optional type and optional
 * classifier. Concrete URIs do not use globs and use precise versions (we do not consider <code>LATEST</code>
 * and <code>RELEASE</code> Maven versions here).</p>
 *
 * <p>When comparing two Maven URIs, we split them to components and may use RegExps and
 * {@link org.apache.felix.utils.version.VersionRange}s</p>
 *
 * <p>When pattern URI doesn't use <code>mvn:</code> scheme, plain {@link String#equals(Object)} is used or
 * {@link Matcher#matches()} when pattern uses <code>*</code> glob.</p>
 */
public class LocationPattern {

    public static Logger LOG = LoggerFactory.getLogger(LocationPattern.class);

    private String originalUri;
    private Pattern originalPattern;
    private String groupId;
    private Pattern groupIdPattern;
    private String artifactId;
    private Pattern artifactIdPattern;
    private String versionString;
    private Version version;
    private VersionRange versionRange;
    private String type;
    private Pattern typePattern;
    private String classifier;
    private Pattern classifierPattern;

    public LocationPattern(String uri) throws IllegalArgumentException {
        if (uri == null) {
            throw new IllegalArgumentException("URI to match should not be null");
        }
        originalUri = uri;
        if (!originalUri.startsWith("mvn:")) {
            originalPattern = toRegExp(originalUri);
        } else {
            uri = uri.substring(4);
            Parser parser = null;
            try {
                parser = new Parser(uri);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
            if (Parser.VERSION_LATEST.equals(parser.getVersion())) {
                parser.setVersion(null);
            }
            groupId = parser.getGroup();
            if (groupId.contains("*")) {
                groupIdPattern = toRegExp(groupId);
            }
            artifactId = parser.getArtifact();
            if (artifactId.contains("*")) {
                artifactIdPattern = toRegExp(artifactId);
            }
            versionString = parser.getVersion();
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
                    IllegalArgumentException mue = new IllegalArgumentException("Can't parse version \"" + versionString + "\" as OSGi version object.", e);
                    throw mue;
                }
            }
            type = parser.getType();
            if (type != null && type.contains("*")) {
                typePattern = toRegExp(type);
            }
            classifier = parser.getClassifier();
            if (classifier != null && classifier.contains("*")) {
                classifierPattern = toRegExp(classifier);
            }
        }
    }

    public String getOriginalUri() {
        return originalUri;
    }

    /**
     * Converts a String with one special character (<code>*</code>) into working {@link Pattern}
     * @param value
     * @return
     */
    public static Pattern toRegExp(String value) {
        // TODO: escape all RegExp special chars that are valid path characters, only convert '*' into '.*'
        return Pattern.compile(value
                .replaceAll("\\.", "\\\\\\.")
                .replaceAll("\\$", "\\\\\\$")
                .replaceAll("\\^", "\\\\\\^")
                .replaceAll("\\*", ".*")
        );
    }

    /**
     * Returns <code>true</code> if this location pattern matches other pattern.
     * @param otherUri
     * @return
     */
    public boolean matches(String otherUri) {
        if (otherUri == null) {
            return false;
        }
        if (originalPattern != null) {
            // this pattern is not mvn:
            return originalPattern.matcher(otherUri).matches();
        }

        LocationPattern other;
        try {
            other = new LocationPattern(otherUri);
        } catch (IllegalArgumentException e) {
            LOG.debug("Can't parse \"" + otherUri + "\" as Maven URI. Ignoring.");
            return false;
        }

        if (other.originalPattern != null) {
            // other pattern is not mvn:
            return false;
        }

        if (other.versionRange != null) {
            LOG.warn("Matched URI can't use version ranges: " + otherUri);
            return false;
        }

        boolean match;

        if (groupIdPattern == null) {
            match = groupId.equals(other.groupId);
        } else {
            match = groupIdPattern.matcher(other.groupId).matches();
        }
        if (!match) {
            return false;
        }
        if (artifactIdPattern == null) {
            match = artifactId.equals(other.artifactId);
        } else {
            match = artifactIdPattern.matcher(other.artifactId).matches();
        }
        if (!match) {
            return false;
        }
        if (versionRange != null && other.version != null) {
            match = versionRange.contains(other.version);
        } else {
            match = version == null || version.equals(other.version);
        }
        if (!match) {
            return false;
        }
        if (typePattern != null) {
            match = typePattern.matcher(other.type == null ? "jar" : other.type).matches();
        } else {
            match = versionString == null || type.equals(other.type);
        }
        if (!match) {
            return false;
        }
        if (classifierPattern != null) {
            match = classifierPattern.matcher(other.classifier == null ? "" : other.classifier).matches();
        } else if (classifier != null) {
            match = classifier.equals(other.classifier);
        } else {
            match = other.classifierPattern == null;
        }

        return match;
    }

    @Override
    public String toString() {
        return originalUri;
    }

}
