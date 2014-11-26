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
import java.net.URI;
import java.util.*;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.tooling.features.model.ArtifactRef;
import org.apache.karaf.tooling.features.model.Feature;
import org.apache.karaf.tooling.features.model.Repository;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;

import org.osgi.framework.Version;

/**
 * Common functionality for mojos that need to resolve features
 */
public abstract class AbstractFeatureMojo extends MojoSupport {
    
    /**
     * @parameter
     */
    protected List<String> descriptors;
    
    protected Set<Artifact> descriptorArtifacts;
    
    /**
     * @parameter
     */
    protected List<String> features;

    /**
     * @parameter
     */
    protected boolean addTransitiveFeatures = true;
    /**
     * @parameter
     */
    private boolean includeMvnBasedDescriptors = false;
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
    protected boolean skipNonMavenProtocols = true;

    /**
     * The start level exported when no explicit start level is set for a bundle
     * @parameter 
     */
    private int defaultStartLevel = 80;

    /**
     * Internal counter for garbage collection
     */
    private int resolveCount = 0;

    public AbstractFeatureMojo() {
        super();
        descriptorArtifacts = new HashSet<Artifact>();
    }

    protected void addFeatureRepo(String featureUrl) throws MojoExecutionException {
        Artifact featureDescArtifact = resourceToArtifact(featureUrl, true);
        if (featureDescArtifact == null) {
            return;
        }
        try {
            resolveArtifact(featureDescArtifact);
            descriptors.add(0, featureUrl);
        } catch (Exception e) {
            getLog().warn("Can't add " + featureUrl + " in the descriptors set");
            getLog().debug(e);
        }
    }

    protected void retrieveDescriptorsRecursively(String uri, Set<String> bundles, Map<String, Feature> featuresMap) {
        // let's ensure a mvn: based url is sitting in the local repo before we try reading it
        Artifact descriptor;
        try {
            descriptor = resourceToArtifact(uri, true);
        } catch (MojoExecutionException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        URI repoURI;
        if (descriptor != null) {
            resolveArtifact(descriptor);
            descriptorArtifacts.add(descriptor);
            repoURI = URI.create(translateFromDescriptor(descriptor));
        }else {
            repoURI = URI.create(translateFromMaven(uri.replaceAll(" ", "%20")));
        }
        if (includeMvnBasedDescriptors) {
            bundles.add(uri);
        }
        Repository repo = new Repository(repoURI, defaultStartLevel);
        for (Feature f : repo.getFeatures()) {
            featuresMap.put(f.getName() + "/" + f.getVersion(), f);
        }
        if (resolveDefinedRepositoriesRecursively) {
            for (String r : repo.getDefinedRepositories()) {
                retrieveDescriptorsRecursively(r, bundles, featuresMap);
            }
        }
    }

    /**
     * Resolves and copies the given artifact to the repository path.
     * Prefers to resolve using the repository of the artifact if present.
     * 
     * @param artifact
     */
    protected void resolveArtifact(Artifact artifact) {
        try {
            String paxUrl = dependencyHelper.artifactToMvn(artifact);
            File file = dependencyHelper.resolveById(paxUrl, getLog());
            artifact.setFile(file);
        } catch (Exception e) {
            if (failOnArtifactResolutionError) {
                throw new RuntimeException("Can't resolve artifact " + artifact, e);
            }
            getLog().warn("Can't resolve artifact " + artifact);
            getLog().debug(e);
        }
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
    protected void addFeatures(List<String> featureNames, Set<Feature> features, Map<String, Feature> featuresMap, boolean transitive) {
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
            if (f == null) {
                //it's probably is a version range so try to use VersionRange Utils
                VersionRange versionRange = new VersionRange(version);
                for (String key : featuresMap.keySet()) {
                    String[] nameVersion = key.split("/");
                    if (feature.equals(nameVersion[0])) {
                        String verStr = featuresMap.get(key).getVersion();
                        Version ver = VersionTable.getVersion(verStr);
                        if (versionRange.contains(ver)) {
                            if (f == null || VersionTable.getVersion(f.getVersion()).compareTo(VersionTable.getVersion(featuresMap.get(key).getVersion())) < 0) {    
                                f = featuresMap.get(key);
                            }
                        }
                    }
                }
            }
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

    protected Set<Feature> resolveFeatures() throws MojoExecutionException {
        Set<Feature> featuresSet = new HashSet<Feature>();
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
            
            addFeatures(features, featuresSet, featuresMap, addTransitiveFeatures);
    
            getLog().info("Base repo: " + localRepo.getUrl());
            for (Feature feature : featuresSet) {
                resolveArtifacts(feature.getBundles());
                resolveArtifacts(feature.getConfigFiles());
            }            
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
        return featuresSet;
    }
    
    private void resolveArtifacts(List<? extends ArtifactRef> artifactRefs) throws MojoExecutionException {
        for (ArtifactRef artifactRef : artifactRefs) {
            Artifact artifact = resourceToArtifact(artifactRef.getUrl(), skipNonMavenProtocols);
            if (artifact != null) {
                artifactRef.setArtifact(artifact);
                resolveArtifact(artifact);
            }
            // TODO: Is this still necessary when Aether is used (from comment on the method i guess no)?
            checkDoGarbageCollect();
        }
    }
    
    /**
     * Maven ArtifactResolver leaves file handles around so need to clean up
     * or we will run out of file descriptors
     */
    protected void checkDoGarbageCollect() {
        if (this.resolveCount++ % 100 == 0) {
            System.gc();
            System.runFinalization();
        }
    }

    private String translateFromDescriptor(Artifact descriptor) {
        String path = descriptor.getFile().getPath();
        path = path.replaceAll("\\\\", "/").replaceAll(" ", "%20");
        return extractProtocolFromLocalMavenRepo() + ":///" + path;
    }

}
