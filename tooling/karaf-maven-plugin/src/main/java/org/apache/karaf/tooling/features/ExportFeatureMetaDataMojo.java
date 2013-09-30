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
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.tooling.features.model.BundleRef;
import org.apache.karaf.tooling.features.model.Feature;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.osgi.framework.Version;

/**
 * Export meta data about features
 *
 * @goal features-export-meta-data
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Export meta data about features
 */
public class ExportFeatureMetaDataMojo extends AbstractFeatureMojo {
    
    /**
     * If set to true then all bundles will be merged into one combined feature.
     * In this case duplicates will be eliminated
     * 
     * @parameter
     */
    private boolean mergedFeature;
    
    /**
     * If set to true then for each bundle symbolic name only the highest version will be used
     * @parameter
     */
    protected boolean oneVersion;

    /**
     * Name of the file for exported feature meta data
     * 
     * @parameter expression="${project.build.directory}/features.xml"
     */
    private File metaDataFile;
    
    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Feature> featuresSet = resolveFeatures();
        if (mergedFeature) {
            Feature feature = oneVersion ? mergeFeatureOneVersion(featuresSet) : mergeFeature(featuresSet);
            featuresSet = new HashSet<Feature>();
            featuresSet.add(feature);
        }
        try {
            metaDataFile.getParentFile().mkdirs();
            FeatureMetaDataExporter exporter = new FeatureMetaDataExporter(new FileOutputStream(metaDataFile));
            for (Feature feature : featuresSet) {
                exporter.writeFeature(feature);
            }
            exporter.close();
        } catch (Exception e) {
            throw new RuntimeException("Error writing feature meta data to " + metaDataFile + ": " + e.getMessage(), e);
        }
    }

    private Feature mergeFeature(Set<Feature> featuresSet) {
        Feature merged = new Feature("merged");
        Set<String> bundleIds = new HashSet<String>();
        for (Feature feature : featuresSet) {
            for (BundleRef bundle : feature.getBundles()) {
                bundle.readManifest();
                String bundleId = bundle.getBundleSymbolicName() + ":" + bundle.getBundleVersion();
                if (!bundleIds.contains(bundleId)) {
                    bundleIds.add(bundleId);
                    merged.getBundles().add(bundle);
                }
            }
        }
        return merged;
    }
    
    private Feature mergeFeatureOneVersion(Set<Feature> featuresSet) {
        Feature merged = new Feature("merged");
        Map<String, BundleRef> bundleVersions = new HashMap<String, BundleRef>();
        for (Feature feature : featuresSet) {
            for (BundleRef bundle : feature.getBundles()) {
                bundle.readManifest();
                BundleRef existingBundle = bundleVersions.get(bundle.getBundleSymbolicName());
                if (existingBundle != null) {
                    Version existingVersion = new Version(existingBundle.getBundleVersion());
                    Version newVersion = new Version(bundle.getBundleVersion());
                    if (newVersion.compareTo(existingVersion) > 0) {
                        bundleVersions.put(bundle.getBundleSymbolicName(), bundle);
                    }
                } else {
                    bundleVersions.put(bundle.getBundleSymbolicName(), bundle);
                }
            }
        }
        for (BundleRef bundle : bundleVersions.values()) {
            merged.getBundles().add(bundle);
        }
        return merged;
    }

}
