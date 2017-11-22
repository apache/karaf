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
package org.apache.karaf.features.internal.model.processing;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.utils.version.VersionCleaner;
import org.apache.felix.utils.version.VersionRange;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.internal.service.Blacklist;
import org.apache.karaf.features.LocationPattern;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.service.Overrides.OVERRIDE_RANGE;

/**
 * A set of instructions to process {@link org.apache.karaf.features.internal.model.Features} model. The actual
 * use of these instructions is moved to {@link org.apache.karaf.features.internal.service.FeaturesProcessorImpl}
 */
@XmlRootElement(name = "featuresProcessing", namespace = FeaturesProcessing.FEATURES_PROCESSING_NS)
@XmlType(name = "featuresProcessing", propOrder = {
        "blacklistedRepositories",
        "blacklistedFeatures",
        "blacklistedBundles",
        "overrideBundleDependency",
        "bundleReplacements",
        "featureReplacements"
})
public class FeaturesProcessing {

    public static Logger LOG = LoggerFactory.getLogger(FeaturesProcessing.class);
    public static final String FEATURES_PROCESSING_NS = "http://karaf.apache.org/xmlns/features-processing/v1.0.0";

    @XmlElementWrapper(name = "blacklistedRepositories")
    @XmlElement(name = "repository")
    private List<String> blacklistedRepositories = new LinkedList<>();
    @XmlTransient
    private List<LocationPattern> blacklistedRepositoryLocationPatterns = new LinkedList<>();

    @XmlElementWrapper(name = "blacklistedFeatures")
    @XmlElement(name = "feature")
    private List<BlacklistedFeature> blacklistedFeatures = new LinkedList<>();

    @XmlElementWrapper(name = "blacklistedBundles")
    @XmlElement(name = "bundle")
    private List<String> blacklistedBundles = new LinkedList<>();

    @XmlElement
    private OverrideBundleDependency overrideBundleDependency;

    @XmlElement
    private BundleReplacements bundleReplacements;

    @XmlElement
    private FeatureReplacements featureReplacements;

    @XmlTransient
    private Blacklist blacklist;

    public FeaturesProcessing() {
        overrideBundleDependency = new OverrideBundleDependency();
        bundleReplacements = new BundleReplacements();
        featureReplacements = new FeatureReplacements();
    }

    public List<String> getBlacklistedRepositories() {
        return blacklistedRepositories;
    }

    public List<LocationPattern> getBlacklistedRepositoryLocationPatterns() {
        return blacklistedRepositoryLocationPatterns;
    }

    public List<BlacklistedFeature> getBlacklistedFeatures() {
        return blacklistedFeatures;
    }

    public List<String> getBlacklistedBundles() {
        return blacklistedBundles;
    }

    public OverrideBundleDependency getOverrideBundleDependency() {
        return overrideBundleDependency;
    }

    public void setOverrideBundleDependency(OverrideBundleDependency overrideBundleDependency) {
        this.overrideBundleDependency = overrideBundleDependency;
    }

    public BundleReplacements getBundleReplacements() {
        return bundleReplacements;
    }

    public void setBundleReplacements(BundleReplacements bundleReplacements) {
        this.bundleReplacements = bundleReplacements;
    }

    public FeatureReplacements getFeatureReplacements() {
        return featureReplacements;
    }

    public void setFeatureReplacements(FeatureReplacements featureReplacements) {
        this.featureReplacements = featureReplacements;
    }

    public Blacklist getBlacklist() {
        return blacklist;
    }

    /**
     * <p>Perform <em>compilation</em> of rules declared in feature processing XML file.</p>
     * <p>Additional blacklist and overrides definitions will be added to this model</p>
     *
     * @param blacklist additional {@link Blacklist} definition with lower priority
     * @param overrides additional overrides definition with lower priority
     */
    public void postUnmarshall(Blacklist blacklist, Set<String> overrides) {
        // configure Blacklist tool
        List<String> blacklisted = new LinkedList<>();

        // compile blacklisted repository URIs (from XML and additional blacklist)
        blacklist.getRepositoryBlacklist().stream()
                .map(LocationPattern::getOriginalUri)
                .forEach(uri -> getBlacklistedRepositories().add(uri));
        for (String repositoryURI : getBlacklistedRepositories()) {
            try {
                blacklistedRepositoryLocationPatterns.add(new LocationPattern(repositoryURI));
                blacklisted.add(repositoryURI + ";" + Blacklist.BLACKLIST_TYPE + "=" + Blacklist.TYPE_REPOSITORY);
            } catch (MalformedURLException e) {
                LOG.warn("Can't parse blacklisted repository location pattern: " + repositoryURI + ". Ignoring.");
            }
        }

        // add external blacklisted features to this model
        blacklist.getFeatureBlacklist()
                .forEach(fb -> getBlacklistedFeatures().add(new BlacklistedFeature(fb.getName(), fb.getVersion())));
        blacklisted.addAll(getBlacklistedFeatures().stream()
                .map(bf -> bf.getName() + ";" + Blacklist.BLACKLIST_TYPE + "=" + Blacklist.TYPE_FEATURE + (bf.getVersion() == null ? "" : ";" + FeaturePattern.RANGE + "=\"" + bf.getVersion() + "\""))
                .collect(Collectors.toList()));

        // add external blacklisted bundle URIs to this model
        blacklist.getBundleBlacklist().stream()
                .map(LocationPattern::getOriginalUri)
                .forEach(uri -> getBlacklistedBundles().add(uri));
        blacklisted.addAll(getBlacklistedBundles().stream()
                .map(bl -> bl + ";" + Blacklist.BLACKLIST_TYPE + "=" + Blacklist.TYPE_BUNDLE)
                .collect(Collectors.toList()));

        this.blacklist = new Blacklist(blacklisted);

        // verify bundle override definitions (from XML and additional overrides)
        bundleReplacements.getOverrideBundles().addAll(parseOverridesClauses(overrides));
        for (Iterator<BundleReplacements.OverrideBundle> iterator = bundleReplacements.getOverrideBundles().iterator(); iterator.hasNext(); ) {
            BundleReplacements.OverrideBundle overrideBundle = iterator.next();
            if (overrideBundle.getOriginalUri() == null) {
                // we have to derive it from replacement - as with etc/overrides.properties entry
                if (overrideBundle.getMode() == BundleReplacements.BundleOverrideMode.MAVEN) {
                    LOG.warn("Can't override bundle in maven mode without explicit original URL. Switching to osgi mode.");
                    overrideBundle.setMode(BundleReplacements.BundleOverrideMode.OSGI);
                }
                String originalUri = calculateOverridenURI(overrideBundle.getReplacement(), null);
                if (originalUri != null) {
                    overrideBundle.setOriginalUri(originalUri);
                } else {
                    iterator.remove();
                    continue;
                }
            }
            try {
                overrideBundle.compile();
            } catch (MalformedURLException e) {
                LOG.warn("Can't parse override URL location pattern: " + overrideBundle.getOriginalUri() + ". Ignoring.");
                iterator.remove();
            }
        }
    }

    /**
     * Changes overrides list (old format) into a list of {@link BundleReplacements.OverrideBundle} definitions.
     * @param overrides
     * @return
     */
    public static Collection<? extends BundleReplacements.OverrideBundle> parseOverridesClauses(Set<String> overrides) {
        List<BundleReplacements.OverrideBundle> result = new LinkedList<>();

        for (Clause clause : Parser.parseClauses(overrides.toArray(new String[overrides.size()]))) {
            // name of the clause will become a bundle replacement
            String mvnURI = clause.getName();
            URI uri = URI.create(mvnURI);
            if (!"mvn".equals(uri.getScheme())) {
                LOG.warn("Override URI \"" + mvnURI + "\" should use mvn: scheme. Ignoring.");
                continue;
            }
            BundleReplacements.OverrideBundle override = new BundleReplacements.OverrideBundle();
            override.setMode(BundleReplacements.BundleOverrideMode.OSGI);
            override.setReplacement(mvnURI);
            String originalUri = calculateOverridenURI(mvnURI, clause.getAttribute(OVERRIDE_RANGE));
            if (originalUri != null) {
                override.setOriginalUri(originalUri);
                try {
                    override.compile();
                    result.add(override);
                } catch (MalformedURLException e) {
                    LOG.warn("Can't parse override URL location pattern: " + originalUri + ". Ignoring.");
                }
            }
        }

        return result;
    }

    /**
     * For <code>etc/overrides.properties</code>, we know what is the target URI for bundles we should use. We need
     * a pattern of original bundle URIs that are candidates for replacement
     * @param replacement
     * @param range
     * @return
     */
    private static String calculateOverridenURI(String replacement, String range) {
        try {
            org.apache.karaf.util.maven.Parser parser = new org.apache.karaf.util.maven.Parser(replacement);
            if (parser.getVersion() != null
                    && (parser.getVersion().startsWith("[") || parser.getVersion().startsWith("("))) {
                // replacement URI should not contain ranges
                throw new MalformedURLException("Override URI should use single version.");
            }
            if (range != null) {
                // explicit range determines originalUri
                VersionRange vr = new VersionRange(range, true);
                if (vr.isOpenCeiling() && vr.getCeiling() == VersionRange.INFINITE_VERSION) {
                    // toString() will give only floor version
                    parser.setVersion(String.format("%s%s,*)",
                            vr.isOpenFloor() ? "(" : "[",
                            vr.getFloor()));
                } else {
                    parser.setVersion(vr.toString());
                }
            } else {
                // no range: originalUri based on replacemenet URI with range deducted using default rules
                // assume version in override URI is NOT a range
                Version v;
                try {
                    v = new Version(VersionCleaner.clean(parser.getVersion()));
                } catch (IllegalArgumentException e) {
                    LOG.warn("Problem parsing override URI \"" + replacement + "\": " + e.getMessage() + ". Version ranges are not handled. Ignoring.");
                    return null;
                }
                Version vfloor = new Version(v.getMajor(), v.getMinor(), 0, null);
                parser.setVersion(new VersionRange(false, vfloor, v, true).toString());
            }
            return parser.toMvnURI();
        } catch (MalformedURLException e) {
            LOG.warn("Problem parsing override URI \"" + replacement + "\": " + e.getMessage() + ". Ignoring.");
            return null;
        }
    }

    @XmlType(name = "blacklistedFeature")
    public static class BlacklistedFeature {
        @XmlValue
        private String name;
        @XmlAttribute
        private String version;

        public BlacklistedFeature() {
        }

        public BlacklistedFeature(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

}
