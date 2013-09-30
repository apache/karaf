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

/**
 * Common functionality for mojos that need to reolve features
 */
public abstract class AbstractFeatureMojo extends MojoSupport {

    protected static final String KARAF_CORE_STANDARD_FEATURE_URL = "mvn:org.apache.karaf.features/standard/%s/xml/features";
    protected static final String KARAF_CORE_ENTERPRISE_FEATURE_URL = "mvn:org.apache.karaf.features/enterprise/%s/xml/features";
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
     * the target karaf version used to resolve Karaf core features descriptors
     *
     * @parameter
     */
    protected String karafVersion;
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
            resolveArtifact(featureDescArtifact, remoteRepos);
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
        if (descriptor != null) {
            resolveArtifact(descriptor, remoteRepos);
            descriptorArtifacts.add(descriptor);
        }
        if (includeMvnBasedDescriptors) {
            bundles.add(uri);
        }
        URI repoURI = URI.create(translateFromMaven(uri.replaceAll(" ", "%20")));
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
     * @param remoteRepos
     */
    @SuppressWarnings("deprecation")
    protected void resolveArtifact(Artifact artifact, List<ArtifactRepository> remoteRepos) {
        try {
            List<ArtifactRepository> usedRemoteRepos = artifact.getRepository() != null ? 
                    Collections.singletonList(artifact.getRepository())
                    : remoteRepos;
            resolver.resolve(artifact, usedRemoteRepos, localRepo);
        } catch (AbstractArtifactResolutionException e) {
            if (failOnArtifactResolutionError) {
                throw new RuntimeException("Can't resolve bundle " + artifact, e);
            }
            getLog().error("Can't resolve bundle " + artifact, e);
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
            
            addFeatures(features, featuresSet, featuresMap, addTransitiveFeatures);
    
            getLog().info("Base repo: " + localRepo.getUrl());
            for (Feature feature : featuresSet) {
                resolveBundles(feature.getBundles());
            }            
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
        return featuresSet;
    }
    
    private void resolveBundles(List<BundleRef> bundleRefs) throws MojoExecutionException {
        for (BundleRef bundle : bundleRefs) {
            Artifact artifact = resourceToArtifact(bundle.getUrl(), skipNonMavenProtocols);
            if (artifact != null) {
                // Store artifact in bundle for later export
                bundle.setArtifact(artifact);
                resolveArtifact(artifact, remoteRepos);
            }
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


}