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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Conditional;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to deal with blacklisted features and bundles.
 */
public class Blacklist {

    public static Logger LOG = LoggerFactory.getLogger(Blacklist.class);

    public static final String BLACKLIST_URL = "url";
    public static final String BLACKLIST_RANGE = "range";
    public static final String BLACKLIST_TYPE = "type"; // null -> "feature"
    public static final String TYPE_FEATURE = "feature";
    public static final String TYPE_BUNDLE = "bundle";
    public static final String TYPE_REPOSITORY = "repository";

    private static final Logger LOGGER = LoggerFactory.getLogger(Blacklist.class);
    private Clause[] clauses;
    private Map<String, LocationPattern> bundleBlacklist = new LinkedHashMap<>();

    public Blacklist() {
        this(Collections.emptyList());
    }

    public Blacklist(List<String> blacklist) {
        this.clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
        compileClauses();
    }

    public Blacklist(String blacklistUrl) {
        Set<String> blacklist = new HashSet<>();
        if (blacklistUrl != null) {
            try (InputStream is = new URL(blacklistUrl).openStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                reader.lines() //
                    .map(String::trim) //
                    .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                    .forEach(blacklist::add);
            } catch (FileNotFoundException e) {
                LOGGER.debug("Unable to load blacklist bundles list", e.toString());
            } catch (Exception e) {
                LOGGER.debug("Unable to load blacklist bundles list", e);
            }
        }
        this.clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
        compileClauses();
    }

    /**
     * Extracts blacklisting clauses related to bundles, features and repositories and changes them to more
     * usable form.
     */
    private void compileClauses() {
        for (Clause c : clauses) {
            String type = c.getAttribute(BLACKLIST_TYPE);
            if (type == null) {
                String url = c.getAttribute(BLACKLIST_URL);
                if (url != null || c.getName().startsWith("mvn:")) {
                    // some special rules from etc/blacklisted.properties
                    type = TYPE_BUNDLE;
                } else {
                    type = TYPE_FEATURE;
                }
            }
            switch (type) {
                case TYPE_FEATURE:
                    break;
                case TYPE_BUNDLE:
                    String location = c.getName();
                    if (c.getAttribute(BLACKLIST_URL) != null) {
                        location = c.getAttribute(BLACKLIST_URL);
                    }
                    if (location == null) {
                        // should not happen?
                        LOG.warn("Bundle blacklist URI is empty. Ignoring.");
                    } else {
                        try {
                            bundleBlacklist.put(location, location.startsWith("mvn:") ? new LocationPattern(location) : null);
                        } catch (MalformedURLException e) {
                            LOG.warn("Problem parsing blacklist URI \"" + location + "\": " + e.getMessage() + ". Ignoring.");
                        }
                    }
                    break;
                case TYPE_REPOSITORY:
            }
        }
    }

    /**
     * TODO: set {@link Feature#setBlacklisted(boolean)} instead of removing from collection
     * @param features
     */
    public void blacklist(Features features) {
        features.getFeature().removeIf(this::blacklist);
    }

    public boolean blacklist(Feature feature) {
        for (Clause clause : clauses) {
            // Check feature name
            if (clause.getName().equals(feature.getName())) {
                // Check feature version
                VersionRange range = VersionRange.ANY_VERSION;
                String vr = clause.getAttribute(BLACKLIST_RANGE);
                if (vr != null) {
                    range = new VersionRange(vr, true);
                }
                if (range.contains(VersionTable.getVersion(feature.getVersion()))) {
                    String type = clause.getAttribute(BLACKLIST_TYPE);
                    if (type == null || TYPE_FEATURE.equals(type)) {
                        return true;
                    }
                }
            }
            // Check bundles
            blacklist(feature.getBundle());
            // Check conditional bundles
            for (Conditional cond : feature.getConditional()) {
                blacklist(cond.getBundle());
            }
        }
        return false;
    }

    private void blacklist(List<Bundle> bundles) {
        for (Iterator<Bundle> iterator = bundles.iterator(); iterator.hasNext();) {
            Bundle info = iterator.next();
            for (Clause clause : clauses) {
                String url = clause.getName();
                if (clause.getAttribute(BLACKLIST_URL) != null) {
                    url = clause.getAttribute(BLACKLIST_URL);
                }
                if (info.getLocation().equals(url)) {
                    String type = clause.getAttribute(BLACKLIST_TYPE);
                    if (type == null || TYPE_BUNDLE.equals(type)) {
                        iterator.remove();
                        break;
                    }
                }
            }
        }
    }

    public boolean isBundleBlacklisted(String uri) {
        for (Map.Entry<String, LocationPattern> clause : bundleBlacklist.entrySet()) {
            if (mavenMatches(clause.getKey(), clause.getValue(), uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether given <code>uri</code> matches Maven artifact pattern (group, artifact, optional type/classifier, version
     * range, globs).
     * @param blacklistedUri
     * @param compiledUri
     * @param uri
     * @return
     */
    private boolean mavenMatches(String blacklistedUri, LocationPattern compiledUri, String uri) {
        if (compiledUri == null) {
            // non maven URI - we can't be smart
            return blacklistedUri.equals(uri);
        } else {
            return compiledUri.matches(uri);
        }
    }

    public boolean isFeatureBlacklisted(String name, String version) {
        for (Clause clause : clauses) {
            String type = clause.getAttribute(BLACKLIST_TYPE);
            if (type != null && !TYPE_FEATURE.equals(type)) {
                continue;
            }
            if (Pattern.matches(clause.getName().replaceAll("\\*", ".*"), name)) {
                // Check feature version
                VersionRange range = VersionRange.ANY_VERSION;
                String vr = clause.getAttribute(BLACKLIST_RANGE);
                if (vr != null) {
                    range = new VersionRange(vr, true);
                }
                if (range.contains(VersionTable.getVersion(version))) {
                    if (type == null || TYPE_FEATURE.equals(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isRepositoryBlacklisted(String uri) {
        for (Clause clause : clauses) {
            String url = clause.getName();
            if (clause.getAttribute(BLACKLIST_URL) != null) {
                url = clause.getAttribute(BLACKLIST_URL);
            }
            if (uri.equals(url)) {
                String type = clause.getAttribute(BLACKLIST_TYPE);
                if (type == null || TYPE_REPOSITORY.equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Merge clauses from another {@link Blacklist} into this object
     * @param others
     */
    public void merge(Blacklist others) {
        Clause[] ours = this.clauses;
        if (ours == null) {
            this.clauses = Arrays.copyOf(others.clauses, others.clauses.length);
        } else if (others != null && others.clauses.length > 0) {
            this.clauses = new Clause[ours.length + others.clauses.length];
            System.arraycopy(ours, 0, this.clauses, 0, ours.length);
            System.arraycopy(others.clauses, ours.length, this.clauses, 0, others.clauses.length);
        }
        if (others != null) {
            this.bundleBlacklist.putAll(others.bundleBlacklist);
        }
    }

    public Clause[] getClauses() {
        return clauses;
    }

}
