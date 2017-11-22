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
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.model.Features;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class to deal with blacklisted features and bundles. It doesn't process JAXB model at all - it only
 * provides information about repository/feature/bundle being blacklisted.
 * The task of actual blacklisting (altering JAXB model) is performed in {@link FeaturesProcessor}
 */
public class Blacklist {

    public static Logger LOG = LoggerFactory.getLogger(Blacklist.class);

    public static final String BLACKLIST_URL = "url";
    public static final String BLACKLIST_TYPE = "type"; // null -> "feature"
    public static final String TYPE_FEATURE = "feature";
    public static final String TYPE_BUNDLE = "bundle";
    public static final String TYPE_REPOSITORY = "repository";

    private static final Logger LOGGER = LoggerFactory.getLogger(Blacklist.class);
    private Clause[] clauses;

    private List<LocationPattern> repositoryBlacklist = new LinkedList<>();
    private List<FeaturePattern> featureBlacklist = new LinkedList<>();
    private List<LocationPattern> bundleBlacklist = new LinkedList<>();

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
            String location;
            switch (type) {
                case TYPE_REPOSITORY:
                    location = c.getName();
                    if (c.getAttribute(BLACKLIST_URL) != null) {
                        location = c.getAttribute(BLACKLIST_URL);
                    }
                    if (location == null) {
                        // should not happen?
                        LOG.warn("Repository blacklist URI is empty. Ignoring.");
                    } else {
                        try {
                            repositoryBlacklist.add(new LocationPattern(location));
                        } catch (MalformedURLException e) {
                            LOG.warn("Problem parsing repository blacklist URI \"" + location + "\": " + e.getMessage() + ". Ignoring.");
                        }
                    }
                    break;
                case TYPE_FEATURE:
                    try {
                        featureBlacklist.add(new FeaturePattern(c.toString()));
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Problem parsing blacklisted feature identifier \"" + c.toString() + "\": " + e.getMessage() + ". Ignoring.");
                    }
                    break;
                case TYPE_BUNDLE:
                    location = c.getName();
                    if (c.getAttribute(BLACKLIST_URL) != null) {
                        location = c.getAttribute(BLACKLIST_URL);
                    }
                    if (location == null) {
                        // should not happen?
                        LOG.warn("Bundle blacklist URI is empty. Ignoring.");
                    } else {
                        try {
                            bundleBlacklist.add(new LocationPattern(location));
                        } catch (MalformedURLException e) {
                            LOG.warn("Problem parsing bundle blacklist URI \"" + location + "\": " + e.getMessage() + ". Ignoring.");
                        }
                    }
                    break;
            }
        }
    }

    /**
     * Checks whether features XML repository URI is blacklisted.
     * @param uri
     * @return
     */
    public boolean isRepositoryBlacklisted(String uri) {
        for (LocationPattern pattern : repositoryBlacklist) {
            if (pattern.matches(uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the feature is blacklisted according to configured rules by name
     * (possibly with wildcards) and optional version (possibly specified as version range)
     * @param name
     * @param version
     * @return
     */
    public boolean isFeatureBlacklisted(String name, String version) {
        for (FeaturePattern pattern : featureBlacklist) {
            if (pattern.matches(name, version)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the bundle URI is blacklisted according to configured rules
     * @param uri
     * @return
     */
    public boolean isBundleBlacklisted(String uri) {
        for (LocationPattern pattern : bundleBlacklist) {
            if (pattern.matches(uri)) {
                return true;
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
            this.repositoryBlacklist.addAll(others.repositoryBlacklist);
            this.featureBlacklist.addAll(others.featureBlacklist);
            this.bundleBlacklist.addAll(others.bundleBlacklist);
        }
    }

    public Clause[] getClauses() {
        return clauses;
    }

    public void blacklist(Features featuresModel) {
    }

    /**
     * Directly add {@link LocationPattern} as blacklisted features XML repository URI
     * @param locationPattern
     */
    public void blacklistRepository(LocationPattern locationPattern) {
        repositoryBlacklist.add(locationPattern);
    }

    /**
     * Directly add {@link FeaturePattern} as blacklisted feature ID
     * @param featurePattern
     */
    public void blacklistFeature(FeaturePattern featurePattern) {
        featureBlacklist.add(featurePattern);
    }

    /**
     * Directly add {@link LocationPattern} as blacklisted bundle URI
     * @param locationPattern
     */
    public void blacklistBundle(LocationPattern locationPattern) {
        bundleBlacklist.add(locationPattern);
    }

}
