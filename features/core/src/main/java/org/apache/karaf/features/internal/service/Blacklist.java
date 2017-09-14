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

import static java.util.stream.Collectors.toSet;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public static final String BLACKLIST_URL = "url";
    public static final String BLACKLIST_RANGE = "range";
    public static final String BLACKLIST_TYPE = "type";
    public static final String TYPE_FEATURE = "feature";
    public static final String TYPE_BUNDLE = "bundle";
    public static final String TYPE_REPOSITORY = "repository";

    private static final Logger LOGGER = LoggerFactory.getLogger(Blacklist.class);
    private Clause[] clauses;
    
    public Blacklist() {
        this(Collections.emptyList());
    }

    public Blacklist(List<String> blacklist) {
        this.clauses = org.apache.felix.utils.manifest.Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
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
    }

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

    public boolean isFeatureBlacklisted(String name, String version) {
        for (Clause clause : clauses) {
            if (clause.getName().equals(name)) {
                // Check feature version
                VersionRange range = VersionRange.ANY_VERSION;
                String vr = clause.getAttribute(BLACKLIST_RANGE);
                if (vr != null) {
                    range = new VersionRange(vr, true);
                }
                if (range.contains(VersionTable.getVersion(version))) {
                    String type = clause.getAttribute(BLACKLIST_TYPE);
                    if (type == null || TYPE_FEATURE.equals(type)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isBundleBlacklisted(String uri) {
        return isBlacklisted(uri, TYPE_BUNDLE);
    }

    public boolean isBlacklisted(String uri, String btype) {
        for (Clause clause : clauses) {
            String url = clause.getName();
            if (clause.getAttribute(BLACKLIST_URL) != null) {
                url = clause.getAttribute(BLACKLIST_URL);
            }
            if (uri.equals(url)) {
                String type = clause.getAttribute(BLACKLIST_TYPE);
                if (type == null || btype.equals(type)) {
                    return true;
                }
            }
        }
        return false;
    }
}
