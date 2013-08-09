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
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.tooling.features.model.BundleRef;
import org.apache.karaf.tooling.features.model.Feature;
import org.apache.karaf.tooling.features.model.Repository;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.AbstractArtifactResolutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Add features to a repository directory
 *
 * @goal features-add-to-repository
 * @phase compile
 * @execute phase="compile"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Add the features to the repository
 */
public class AddToRepositoryMojo extends MojoSupport {

    private final static String KARAF_CORE_STANDARD_FEATURE_URL =
        "mvn:org.apache.karaf.features/standard/%s/xml/features";
    private final static String KARAF_CORE_ENTERPRISE_FEATURE_URL =
        "mvn:org.apache.karaf.features/enterprise/%s/xml/features";

    /**
     * @parameter
     */
    private List<String> descriptors;

    /**
     * @parameter
     */
    private List<String> features;

    /**
     * @parameter expression="${project.build.directory}/features-repo"
     */
    private File repository;

    /**
     * the target karaf version used to resolve Karaf core features descriptors
     *
     * @parameter
     */
    private String karafVersion;

    /**
     * @parameter
     */
    private boolean includeMvnBasedDescriptors = false;

    /**
     * @parameter
     */
    private List<CopyFileBasedDescriptor> copyFileBasedDescriptors;

    /**
     * @parameter
     */
    private boolean skipNonMavenProtocols = true;

    /**
     * @parameter
     */
    private boolean failOnArtifactResolutionError = true;

    /**
     * @parameter
     */
    private boolean resolveDefinedRepositoriesRecursively = true;

    /**
     * @parameter
     */
    private boolean addTransitiveFeatures = true;
    
    /**
     * If set to true the exported bundles will be directly copied into the repository dir.
     * If set to false the default maven repository layout will be used
     * @parameter
     */
    private boolean flatRepoLayout;
    
    /**
     * If set to true then the resolved features and bundles will be exported into a single xml file.
     * This is intended to allow further build scripts to create configs for OSGi containers
     * @parameter
     */
    private boolean exportMetaData;
    
    /**
     * Name of the file for exported feature meta data
     * 
     * @parameter expression="${project.build.directory}/features.xml"
     */
    private File metaDataFile;
    
    /**
     * Internal counter for garbage collection
     */
    private int resolveCount = 0;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (karafVersion == null) {
            Package p = Package.getPackage("org.apache.karaf.tooling.features");
            karafVersion = p.getImplementationVersion();
        }

        addFeatureRepo(String.format(KARAF_CORE_ENTERPRISE_FEATURE_URL, karafVersion));
        addFeatureRepo(String.format(KARAF_CORE_STANDARD_FEATURE_URL, karafVersion));
        addFeatureRepo(String.format(KARAF_CORE_STANDARD_FEATURE_URL, karafVersion));

        try {
            Set<String> artifactsToCopy = new HashSet<String>();
            Map<String, Feature> featuresMap = new HashMap<String, Feature>();
            for (String uri : descriptors) {
                retrieveDescriptorsRecursively(uri, artifactsToCopy, featuresMap);
            }

            // no features specified, handle all of them
            if (features == null) {
                features = new ArrayList<String>(featuresMap.keySet());
            }
            
            Set<Feature> featuresSet = new HashSet<Feature>();
            
            addFeatures(features, featuresSet, featuresMap, addTransitiveFeatures);

            getLog().info("Base repo: " + localRepo.getUrl());
            for (Feature feature : featuresSet) {
            	copyBundlesToDestRepository(feature.getBundles());
            	copyArtifactsToDestRepository(feature.getConfigFiles());
            }
            
            copyFileBasedDescriptorsToDestRepository();
            
            if (exportMetaData) {
            	exportMetaData(featuresSet, metaDataFile);
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
    }

    private void exportMetaData(Set<Feature> featuresSet, File metaDataFile) {
        try {
            FeatureMetaDataExporter exporter = new FeatureMetaDataExporter(new FileOutputStream(metaDataFile));
            for (Feature feature : featuresSet) {
                exporter.writeFeature(feature);
            }
            exporter.close();
        } catch (Exception e) {
            throw new RuntimeException("Error writing feature meta data to " + metaDataFile + ": " + e.getMessage(), e);
        }
    }

    private void addFeatureRepo(String featureUrl) throws MojoExecutionException {
        Artifact featureDescArtifact = resourceToArtifact(featureUrl, true);
        if (featureDescArtifact == null) {
            return;
        }
        try {
            resolveAndCopyArtifact(featureDescArtifact, remoteRepos);
            descriptors.add(0, featureUrl);
        } catch (Exception e) {
            getLog().warn("Can't add " + featureUrl + " in the descriptors set");
            getLog().debug(e);
        }
    }

    private void retrieveDescriptorsRecursively(String uri, Set<String> bundles, Map<String, Feature> featuresMap) {
        // let's ensure a mvn: based url is sitting in the local repo before we try reading it
        Artifact descriptor;
        try {
            descriptor = resourceToArtifact(uri, true);
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (descriptor != null) {
            resolveAndCopyArtifact(descriptor, remoteRepos);
        }
        if (includeMvnBasedDescriptors) {
            bundles.add(uri);
        }
        Repository repo = new Repository(URI.create(translateFromMaven(uri.replaceAll(" ", "%20"))));
        for (Feature f : repo.getFeatures()) {
            featuresMap.put(f.getName() + "/" + f.getVersion(), f);
        }
        if (resolveDefinedRepositoriesRecursively) {
            for (String r : repo.getDefinedRepositories()) {
                retrieveDescriptorsRecursively(r, bundles, featuresMap);
            }
        }
    }

    private void copyBundlesToDestRepository(List<BundleRef> bundleRefs) throws MojoExecutionException {
        for (BundleRef bundle : bundleRefs) {
            Artifact artifact = resourceToArtifact(bundle.getUrl(), skipNonMavenProtocols);
            if (artifact != null) {
                // Store artifact in bundle for later export
                bundle.setArtifact(artifact);
                resolveAndCopyArtifact(artifact, remoteRepos);
            }
            checkDoGarbageCollect();
        }
    }

    private void copyArtifactsToDestRepository(List<String> list) throws MojoExecutionException {
        for (String bundle : list) {
            Artifact artifact = resourceToArtifact(bundle, skipNonMavenProtocols);
            if (artifact != null) {
                resolveAndCopyArtifact(artifact, remoteRepos);
            }
            checkDoGarbageCollect();
        }
    }
    
    /**
     * Maven ArtifactResolver leaves file handles around so need to clean up
     * or we will run out of file descriptors
     */
    private void checkDoGarbageCollect() {
        if (this.resolveCount++ % 100 == 0) {
            System.gc();
            System.runFinalization();
        }
    }

    private void copyFileBasedDescriptorsToDestRepository() {
        if (copyFileBasedDescriptors != null) {
            for (CopyFileBasedDescriptor fileBasedDescriptor : copyFileBasedDescriptors) {
                File destDir = new File(repository, fileBasedDescriptor.getTargetDirectory());
                File destFile = new File(destDir, fileBasedDescriptor.getTargetFileName());
                copy(fileBasedDescriptor.getSourceFile(), destFile);
            }
        }
    }

    /**
     * Resolves and copies the given artifact to the repository path.
     * Prefers to resolve using the repository of the artifact if present.
     * 
     * @param artifact
     * @param remoteRepos
     */
    @SuppressWarnings("deprecation")
    private void resolveAndCopyArtifact(Artifact artifact, List<ArtifactRepository> remoteRepos) {
        try {
            getLog().info("Copying artifact: " + artifact);
            List<ArtifactRepository> usedRemoteRepos = artifact.getRepository() != null ? 
                    Collections.singletonList(artifact.getRepository())
                    : remoteRepos;
            resolver.resolve(artifact, usedRemoteRepos, localRepo);
            File destFile = new File(repository, getRelativePath(artifact));
            copy(artifact.getFile(), destFile);
        } catch (AbstractArtifactResolutionException e) {
            if (failOnArtifactResolutionError) {
                throw new RuntimeException("Can't resolve bundle " + artifact, e);
            }
            getLog().error("Can't resolve bundle " + artifact, e);
        }
    }

    /**
     * Get relative path for artifact
     * TODO consider DefaultRepositoryLayout
     * @param artifact
     * @return relative path of the given artifact in a default repo layout
     */
    private String getRelativePath(Artifact artifact) {
    	String dir = (this.flatRepoLayout) ? "" : MavenUtil.getDir(artifact);
        String name = MavenUtil.getFileName(artifact);
        return dir + name;
    }





    /**
     * Populate the features by traversing the listed features and their
     * dependencies if transitive is true
     *  
     * @param featureNames
     * @param features
     * @param featuresMap
     * @param transitive
     */
    private void addFeatures(List<String> featureNames, Set<Feature> features,
            Map<String, Feature> featuresMap, boolean transitive) {
        for (String feature : featureNames) {
            Feature f = getMatchingFeature(featuresMap, feature);
            features.add(f);
            if (transitive) {
            	addFeatures(f.getDependencies(), features, featuresMap, true);
            }
        }
    }

    private Feature getMatchingFeature(Map<String, Feature> featuresMap, String feature) {
        // feature could be only the name or name/version
        int delimIndex = feature.indexOf('/');
        String version = null;
        if (delimIndex > 0) {
            version = feature.substring(delimIndex + 1);
            feature = feature.substring(0, delimIndex);
        }
        Feature f = null;
        if (version != null) {
            // looking for a specific feature with name and version
            f = featuresMap.get(feature + "/" + version);
        } else {
            // looking for the first feature name (whatever the version is)
            for (String key : featuresMap.keySet()) {
                String[] nameVersion = key.split("/");
                if (feature.equals(nameVersion[0])) {
                    f = featuresMap.get(key);
                    break;
                }
            }
        }
        if (f == null) {
            throw new IllegalArgumentException("Unable to find the feature '" + feature + "'");
        }
        return f;
    }

}
