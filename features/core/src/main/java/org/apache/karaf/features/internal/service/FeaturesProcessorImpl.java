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
package org.apache.karaf.features.internal.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.FeaturePattern;
import org.apache.karaf.features.LocationPattern;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.Conditional;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.processing.BundleReplacements;
import org.apache.karaf.features.internal.model.processing.FeatureReplacements;
import org.apache.karaf.features.internal.model.processing.FeaturesProcessing;
import org.apache.karaf.features.internal.model.processing.OverrideBundleDependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Configurable {@link FeaturesProcessor}, controlled by several files from <code>etc/</code> directory:<ul>
 *     <li><code>etc/overrides.properties</code>: may alter bundle versions in features</li>
 *     <li><code>etc/blacklisted.properties</code>: may filter out some features/bundles</li>
 *     <li><code>etc/org.apache.karaf.features.xml</code> (<strong>new!</strong>): incorporates two above files
 *     and may define additional processing (changing G/A/V, adding bundles to features, changing <code>dependency</code>
 *     attributes, ...)</li>
 * </ul></p>
 */
public class FeaturesProcessorImpl implements FeaturesProcessor {

    public static Logger LOG = LoggerFactory.getLogger(FeaturesProcessorImpl.class);

    private FeaturesProcessingSerializer serializer = new FeaturesProcessingSerializer();

    // empty, but fully functional features processing configuration
    private FeaturesProcessing processing = new FeaturesProcessing();

    /**
     * <p>Creates instance of features processor using 1 external URI, additional {@link Blacklist} instance
     * and additional set of override clauses.</p>
     * @param featureModificationsURI
     * @param featureProcessingVersions
     * @param blacklistDefinitions
     * @param overrides
     */
    public FeaturesProcessorImpl(String featureModificationsURI, String featureProcessingVersions,
                                 Blacklist blacklistDefinitions, Set<String> overrides) {
        if (featureModificationsURI != null) {
            try {
                try (InputStream stream = new URL(featureModificationsURI).openStream()) {
                    Properties versions = new Properties();
                    if (featureProcessingVersions != null) {
                        File versionsProperties = new File(new URL(featureProcessingVersions).getPath());
                        if (versionsProperties.isFile()) {
                            try (InputStream propsStream = new URL(featureProcessingVersions).openStream()) {
                                versions.load(propsStream);
                            }
                        }
                    }
                    processing = serializer.read(stream, versions);
                }
            } catch (FileNotFoundException e) {
                LOG.debug("Can't find feature processing file (" + featureModificationsURI + "), skipping");
            } catch (Exception e) {
                LOG.warn("Can't initialize feature processor: " + e.getMessage());
            }
        }

        processing.postUnmarshall(blacklistDefinitions, overrides);
    }

    /**
     * <p>Creates instance of features processor using 3 external (optional) URIs.</p>
     * @param featureModificationsURI
     * @param featureProcessingVersions
     * @param blacklistedURI
     * @param overridesURI
     */
    public FeaturesProcessorImpl(String featureModificationsURI, String featureProcessingVersions,
                                 String blacklistedURI, String overridesURI) {
        this(featureModificationsURI, featureProcessingVersions, new Blacklist(blacklistedURI), Overrides.loadOverrides(overridesURI));
    }

    /**
     * <p>Creates instance of features processor using {@link FeaturesServiceConfig configuration object} where
     * three files may be specified: overrides.properties, blacklisted.properties and org.apache.karaf.features.xml.</p>
     * @param configuration
     */
    public FeaturesProcessorImpl(FeaturesServiceConfig configuration) {
        this(configuration.featureModifications, configuration.featureProcessingVersions, configuration.blacklisted, configuration.overrides);
    }

    /**
     * Writes model to output stream.
     * @param output
     */
    public void writeInstructions(OutputStream output) {
        serializer.write(processing, output);
    }

    public FeaturesProcessing getInstructions() {
        return processing;
    }

    /**
     * For the purpose of assembly builder, we can configure additional overrides that are read from profiles
     * @param overrides
     */
    public void addOverrides(Set<String> overrides) {
        processing.getBundleReplacements().getOverrideBundles()
                .addAll(FeaturesProcessing.parseOverridesClauses(overrides));
    }

    @Override
    public void process(Features features) {
        List<Feature> featureList = features.getFeature();
        for (int i = 0; i < featureList.size(); i++) {
            Feature f = featureList.get(i);
            // overriding features first, so we can further override their bundles
            for (FeatureReplacements.OverrideFeature override : getInstructions().getFeatureReplacements().getReplacements()) {
                if (f.getId().equals(override.getFeature().getId())) {
                    switch (override.getMode()) {
                        case REPLACE:
                            featureList.set(i, override.getFeature());
                            break;
                        case MERGE:
                            f.getBundle().addAll(override.getFeature().getBundle());
                            break;
                        case REMOVE:
                            // TODO
                            break;
                    }
                }
            }
        }

        for (Feature feature : features.getFeature()) {
            // blacklisting features
            boolean allBlacklisted = features.isBlacklisted();
            feature.setBlacklisted(allBlacklisted || isFeatureBlacklisted(feature));

            // override dependency flag (null - don't touch, false - change to false, true - change to true)
            Boolean dependency = null;
            for (OverrideBundleDependency.OverrideFeatureDependency overrideFeatureDependency : getInstructions().getOverrideBundleDependency().getFeatures()) {
                FeaturePattern pattern = new FeaturePattern(overrideFeatureDependency.getName() + "/" + overrideFeatureDependency.getVersion());
                if (pattern.matches(feature.getName(), feature.getVersion())) {
                    dependency = overrideFeatureDependency.isDependency();
                }
            }

            // blacklisting bundles and processing bundles
            processBundles(feature.getBundle(), allBlacklisted, dependency);
            for (Conditional c : feature.getConditional()) {
                processBundles(c.getBundle(), allBlacklisted, dependency);
            }

            // TODO: think about overriding at repository level
//            for (OverrideBundleDependency.OverrideDependency overrideDependency : getInstructions().getOverrideBundleDependency().getRepositories()) {
//            }
        }
    }

    private void processBundles(List<Bundle> bundles, boolean allBlacklisted, Boolean forceDependency) {
        for (Bundle bundle : bundles) {
            boolean bundleBlacklisted = allBlacklisted || isBundleBlacklisted(bundle.getLocation());
            if (bundleBlacklisted) {
                // blacklisting has higher priority
                bundle.setBlacklisted(true);
            } else {
                // if not blacklisted, it may be overriden
                staticOverrideBundle(bundle);
                // and may have dependency flag altered
                if (forceDependency != null) {
                    // set at feature level
                    bundle.setDependency(forceDependency);
                } else {
                    // may have dependency overriden at bundle level
                    for (OverrideBundleDependency.OverrideDependency overrideBundleDependency : getInstructions().getOverrideBundleDependency().getBundles()) {
                        LocationPattern pattern = new LocationPattern(overrideBundleDependency.getUri());
                        if (pattern.matches(bundle.getLocation())) {
                            bundle.setDependency(overrideBundleDependency.isDependency());
                        }
                    }
                }
            }
        }
    }

    /**
     * Processes {@link Bundle bundle definition} and (according to override instructions) maybe sets different target
     * location and {@link BundleInfo#isOverriden()} flag
     * @param bundle
     */
    private void staticOverrideBundle(Bundle bundle) {
        bundle.setOverriden(BundleInfo.BundleOverrideMode.NONE);

        for (BundleReplacements.OverrideBundle override : this.getInstructions().getBundleReplacements().getOverrideBundles()) {
            String originalLocation = bundle.getLocation();
            if (override.getOriginalUriPattern().matches(originalLocation)) {
                LOG.debug("Overriding bundle location \"" + originalLocation + "\" with \"" + override.getReplacement() + "\"");
                bundle.setOriginalLocation(originalLocation);
                if (override.getMode() == BundleReplacements.BundleOverrideMode.MAVEN) {
                    bundle.setOverriden(BundleInfo.BundleOverrideMode.MAVEN);
                } else {
                    bundle.setOverriden(BundleInfo.BundleOverrideMode.OSGI);
                }
                bundle.setLocation(override.getReplacement());
                // TOCHECK: last rule wins - no break!!!
                //break;
            }
        }
    }

    @Override
    public boolean isRepositoryBlacklisted(String uri) {
        for (LocationPattern lp : processing.getBlacklistedRepositoryLocationPatterns()) {
            if (lp.matches(uri)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Matching name and version of given feature, checks whether this feature is blacklisted
     * @param feature
     * @return
     */
    private boolean isFeatureBlacklisted(Feature feature) {
        return getInstructions().getBlacklist().isFeatureBlacklisted(feature.getName(), feature.getVersion());
    }

    /**
     * Matching location of the bundle, checks whether this bundle is blacklisted
     * @param location
     * @return
     */
    @Override
    public boolean isBundleBlacklisted(String location) {
        return getInstructions().getBlacklist().isBundleBlacklisted(location);
    }

    /**
     * Checks whether the configuration in this processor contains any instructions (for bundles, repositories,
     * overrides, ...)
     * @return
     */
    public boolean hasInstructions() {
        int count = 0;
        count += getInstructions().getBlacklistedRepositories().size();
        count += getInstructions().getBlacklistedFeatures().size();
        count += getInstructions().getBlacklistedBundles().size();
        count += getInstructions().getOverrideBundleDependency().getRepositories().size();
        count += getInstructions().getOverrideBundleDependency().getFeatures().size();
        count += getInstructions().getOverrideBundleDependency().getBundles().size();
        count += getInstructions().getBundleReplacements().getOverrideBundles().size();
        count += getInstructions().getFeatureReplacements().getReplacements().size();

        return count > 0;
    }

}
