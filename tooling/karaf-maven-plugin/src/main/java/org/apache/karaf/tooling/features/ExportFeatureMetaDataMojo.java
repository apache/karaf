/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.features;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.karaf.features.internal.model.*;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.osgi.framework.Version;

/**
 * Export meta data about features
 */
@Mojo(name = "features-export-meta-data", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class ExportFeatureMetaDataMojo extends AbstractFeatureMojo {
    
    /**
     * If set to true then all bundles will be merged into one combined feature.
     * In this case duplicates will be eliminated
     */
    @Parameter
    private boolean mergedFeature;
    
    /**
     * If set to true then for each bundle symbolic name only the highest version will be used
     */
    @Parameter
    protected boolean oneVersion;

    /**
     * Name of the file for exported feature meta data
     */
    @Parameter(defaultValue = "${project.build.directory}/features.xml")
    private File metaDataFile;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Feature> featuresSet = resolveFeatures();
        if (mergedFeature) {
            Feature feature = oneVersion ? mergeFeatureOneVersion(featuresSet) : mergeFeature(featuresSet);
            featuresSet = new HashSet<>();
            featuresSet.add(feature);
        }
        try {
            metaDataFile.getParentFile().mkdirs();
            Features features = new Features();
            features.getFeature().addAll(featuresSet);
            try (OutputStream os = new FileOutputStream(metaDataFile)) {
                if (useJson) {
                    JacksonUtil.marshal(features, os);
                } else {
                    JaxbUtil.marshal(features, os);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error writing feature meta data to " + metaDataFile + ": " + e.getMessage(), e);
        }
    }

    private Feature mergeFeature(Set<Feature> featuresSet) throws MojoExecutionException {
        Feature merged = new Feature("merged");
        Set<String> bundleIds = new HashSet<>();
        for (Feature feature : featuresSet) {
            for (Bundle bundle : feature.getBundle()) {
                String symbolicName = getBundleSymbolicName(bundle);
                if (symbolicName == null) {
                    logIgnored(bundle);
                    continue;
                }
                String bundleId = symbolicName + ":" + getBundleVersion(bundle);
                if (!bundleIds.contains(bundleId)) {
                    bundleIds.add(bundleId);
                    merged.getBundle().add(bundle);
                }
            }
        }
        return merged;
    }
    
    private Feature mergeFeatureOneVersion(Set<Feature> featuresSet) throws MojoExecutionException {
        Feature merged = new Feature("merged");
        Map<String, Bundle> bundleVersions = new HashMap<>();
        for (Feature feature : featuresSet) {
            for (Bundle bundle : feature.getBundle()) {
                String symbolicName = getBundleSymbolicName(bundle);
                if (symbolicName == null) {
                    logIgnored(bundle);
                    continue;
                }
                Bundle existingBundle = bundleVersions.get(symbolicName);
                if (existingBundle != null) {
                    Version existingVersion = new Version(getBundleVersion(existingBundle));
                    Version newVersion = new Version(getBundleVersion(bundle));
                    if (newVersion.compareTo(existingVersion) > 0) {
                        bundleVersions.put(symbolicName, bundle);
                    }
                } else {
                    bundleVersions.put(symbolicName, bundle);
                }
            }
        }
        for (Bundle bundle : bundleVersions.values()) {
            merged.getBundle().add(bundle);
        }
        return merged;
    }

    private void logIgnored(Bundle bundle) {
        getLog().warn("Ignoring jar without BundleSymbolicName: " + bundle.getLocation());
    }

    private Map<String, Attributes> manifests = new HashMap<>();

    private String getBundleVersion(Bundle bundle) throws MojoExecutionException {
        return getManifest(bundle).getValue("Bundle-Version");
    }

    private String getBundleSymbolicName(Bundle bundle) throws MojoExecutionException {
        return getManifest(bundle).getValue("Bundle-SymbolicName");
    }

    private Attributes getManifest(Bundle bundle) throws MojoExecutionException {
        Attributes attributes = manifests.get(bundle.getLocation());
        if (attributes == null) {
            Artifact artifact = resourceToArtifact(bundle.getLocation(), skipNonMavenProtocols);
            if (artifact.getFile() == null) {
                resolveArtifact(artifact, remoteRepos);
            }
            try (JarInputStream jis = new JarInputStream(new FileInputStream(artifact.getFile()))) {
                Manifest manifest = jis.getManifest();
                if (manifest != null) {
                    attributes = manifest.getMainAttributes();
                } else {
                    attributes = new Attributes();
                }
                manifests.put(bundle.getLocation(), attributes);
            } catch (IOException e) {
                throw new MojoExecutionException("Error reading bundle manifest from " + bundle.getLocation(), e);
            }
        }
        return attributes;
    }

}
