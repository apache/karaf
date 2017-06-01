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
import java.net.URL;
import java.util.*;

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

    private Blacklist() {
    }

    public static void blacklist(Features features, String blacklisted) {
        Set<String> blacklist = loadBlacklist(blacklisted);
        blacklist(features, blacklist);
    }

    public static void blacklist(Features features, Collection<String> blacklist) {
        Clause[] clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
        blacklist(features, clauses);
    }

    public static void blacklist(Features features, Clause[] clauses) {
        features.getFeature().removeIf(feature -> blacklist(feature, clauses));
    }

    public static boolean blacklist(Feature feature, Clause[] clauses) {
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
            blacklist(feature.getBundle(), clauses);
            // Check conditional bundles
            for (Conditional cond : feature.getConditional()) {
                blacklist(cond.getBundle(), clauses);
            }
        }
        return false;
    }

    private static void blacklist(List<Bundle> bundles, Clause[] clauses) {
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

    public static Set<String> loadBlacklist(String blacklistUrl) {
        Set<String> blacklist = new HashSet<>();
        try {
            if (blacklistUrl != null) {
                try (
                        InputStream is = new URL(blacklistUrl).openStream()
                ) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (!line.isEmpty() && !line.startsWith("#")) {
                            blacklist.add(line);
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            LOGGER.debug("Unable to load blacklist bundles list", e.toString());
        } catch (Exception e) {
            LOGGER.debug("Unable to load blacklist bundles list", e);
        }
        return blacklist;
    }

    public static boolean isFeatureBlacklisted(List<String> blacklist, String name, String version) {
        Clause[] clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
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

    public static boolean isBundleBlacklisted(List<String> blacklist, String uri) {
        return isBlacklisted(blacklist, uri, TYPE_BUNDLE);
    }

    public static boolean isBlacklisted(List<String> blacklist, String uri, String btype) {
        Clause[] clauses = Parser.parseClauses(blacklist.toArray(new String[blacklist.size()]));
        return isBlacklisted(clauses, uri, btype);
    }

    public static boolean isBlacklisted(Clause[] clauses, String uri, String btype) {
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
