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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.internal.model.*;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;

import org.apache.maven.plugins.annotations.Parameter;
import org.osgi.framework.Version;

/**
 * Common functionality for mojos that need to resolve features
 */
public abstract class AbstractFeatureMojo extends MojoSupport {
    
    @Parameter
    protected List<String> descriptors;
    
    protected Set<Artifact> descriptorArtifacts;

    @Parameter
    protected List<String> features;

    @Parameter
    protected boolean addTransitiveFeatures = true;

    @Parameter
    private boolean includeMvnBasedDescriptors = false;

    @Parameter
    private boolean failOnArtifactResolutionError = true;

    @Parameter
    private boolean resolveDefinedRepositoriesRecursively = true;

    @Parameter
    protected boolean skipNonMavenProtocols = true;

    /**
     * Ignore the dependency flag on the bundles in the features XML
     */
    @Parameter(defaultValue = "false")
    protected boolean ignoreDependencyFlag;
    
    /**
     * The start level exported when no explicit start level is set for a bundle
     */
    @Parameter
    private int defaultStartLevel = 80;

    @Parameter(defaultValue = "false")
    protected boolean useJson;

    /**
     * Internal counter for garbage collection
     */
    private int resolveCount = 0;

    public AbstractFeatureMojo() {
        super();
        descriptorArtifacts = new HashSet<>();
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
        String uriToUse = descriptor != null
                ? descriptor.getFile().toURI().toString()
                : translateFromMaven(uri);
        Features repo = null;
        if (JacksonUtil.isJson(uriToUse)) {
            try {
                repo = JacksonUtil.unmarshal(uriToUse);
            } catch (Exception e) {
                getLog().error("Error unmarshalling features json", e);
                return;
            }
        } else {
            repo = JaxbUtil.unmarshal(uriToUse, true);
        }
        for (Feature f : repo.getFeature()) {
            featuresMap.put(f.getId(), f);
        }
        if (resolveDefinedRepositoriesRecursively) {
            for (String r : repo.getRepository()) {
                retrieveDescriptorsRecursively(r, bundles, featuresMap);
            }
        }
    }

    /**
     * Resolves and copies the given artifact to the repository path.
     * Prefers to resolve using the repository of the artifact if present.
     * 
     * @param artifact The artifact.
     * @param remoteRepos The {@link List} of remote repositories to use for artifact resolution.
     */
    @SuppressWarnings("deprecation")
    protected void resolveArtifact(Artifact artifact, List<ArtifactRepository> remoteRepos) {
        try {
            if (artifact == null) {
                return;
            }
            List<ArtifactRepository> usedRemoteRepos = artifact.getRepository() != null ? 
                    Collections.singletonList(artifact.getRepository())
                    : remoteRepos;
            artifactResolver.resolve(artifact, usedRemoteRepos, localRepo);
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
     * @param featureNames The {@link List} of feature names.
     * @param features The {@link Set} of features.
     * @param featuresMap The {@link Map} of features.
     * @param transitive True to add transitive features, false else.
     */
    protected void addFeatures(List<String> featureNames, Set<Feature> features, Map<String, Feature> featuresMap, boolean transitive) {
        for (String feature : featureNames) {
            String[] split = feature.split("/");
            List<Feature> innerFeatures = getMatchingFeature(featuresMap, split[0], split.length > 1 ? split[1] : null);
            for (Feature f : innerFeatures) {
                features.add(f);
                if (transitive) {
                    addFeaturesDependencies(f.getFeature(), features, featuresMap, true);
                }
            }
        }
    }

    protected void addFeaturesDependencies(List<Dependency> featureNames, Set<Feature> features, Map<String, Feature> featuresMap, boolean transitive) {
        for (Dependency dependency : featureNames) {
            List<Feature> innerFeatures = getMatchingFeature(featuresMap, dependency.getName(), dependency.getVersion());
            for (Feature f : innerFeatures) {
                features.add(f);
                if (transitive) {
                    addFeaturesDependencies(f.getFeature(), features, featuresMap, true);
                }
            }
        }
    }

    private List<Feature> getMatchingFeature(Map<String, Feature> featuresMap, String feature, String version) {
        List<Feature> features = new ArrayList<>();
        Pattern namePattern = Pattern.compile(feature);
        if (version != null && !version.equals(Feature.DEFAULT_VERSION)) {
            // looking for a specific feature with name and version
            Feature f = null;
            for (String key : featuresMap.keySet()) {
                String[] nameAndVersion = key.split("/");
                Matcher matcher = namePattern.matcher(nameAndVersion[0]);
                if (matcher.matches() && version.equals(nameAndVersion[1])) {
                    f = featuresMap.get(key);
                    features.add(f);
                }
            }
            if (f == null) {
                //it's probably is a version range so try to use VersionRange Utils
                VersionRange versionRange = new VersionRange(version);
                for (String key : featuresMap.keySet()) {
                    String[] nameVersion = key.split("/");
                    Matcher matcher = namePattern.matcher(nameVersion[0]);
                    if (matcher.matches()) {
                        String verStr = featuresMap.get(key).getVersion();
                        Version ver = VersionTable.getVersion(verStr);
                        if (versionRange.contains(ver)) {
                            if (f == null || VersionTable.getVersion(f.getVersion()).compareTo(VersionTable.getVersion(featuresMap.get(key).getVersion())) < 0) {    
                                features.add(featuresMap.get(key));
                            }
                        }
                    }
                }
            }
        } else {
            // looking for the first feature name (whatever the version is)
            for (String key : featuresMap.keySet()) {
                String[] nameVersion = key.split("/");
                Matcher matcher = namePattern.matcher(nameVersion[0]);
                if (matcher.matches()) {
                    features.add(featuresMap.get(key));
                }
            }
        }
        if (features.size() == 0) {
            throw new IllegalArgumentException("Unable to find the feature '" + feature + "'");
        }
        return features;
    }

    protected Set<Feature> resolveFeatures() throws MojoExecutionException {
        Set<Feature> featuresSet = new HashSet<>();
        try {
            Set<String> artifactsToCopy = new HashSet<>();
            Map<String, Feature> featuresMap = new HashMap<>();
            for (String uri : descriptors) {
                retrieveDescriptorsRecursively(uri, artifactsToCopy, featuresMap);
            }
    
            // no features specified, handle all of them
            if (features == null) {
                features = new ArrayList<>(featuresMap.keySet());
            }
            
            addFeatures(features, featuresSet, featuresMap, addTransitiveFeatures);
    
            getLog().info("Using local repository at: " + localRepo.getUrl());
            for (Feature feature : featuresSet) {
                try {
                    for (Bundle bundle : feature.getBundle()) {
                        resolveArtifact(bundle.getLocation());
                    }
                    for (Conditional conditional : feature.getConditional()) {
                        for (BundleInfo bundle : conditional.getBundles()) {
                            if (ignoreDependencyFlag || (!ignoreDependencyFlag && !bundle.isDependency())) {
                                resolveArtifact(bundle.getLocation());
                            }
                        }
                    }
                    for (ConfigFile configfile : feature.getConfigfile()) {
                        resolveArtifact(configfile.getLocation());
                    }
                } catch (RuntimeException e) {
                    throw new RuntimeException("Error resolving feature " + feature.getName() + "/" + feature.getVersion(), e);
                }
            }            
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
        }
        return featuresSet;
    }

    private Artifact resolveArtifact(String location) throws MojoExecutionException {
        Artifact artifact = resourceToArtifact(location, skipNonMavenProtocols);
        if (artifact != null) {
            try {
                resolveArtifact(artifact, remoteRepos);
            } catch (RuntimeException e) {
                throw new RuntimeException("Error resolving artifact " + location, e);
            }
        }
        checkDoGarbageCollect();
        return artifact;
    }

    /**
     * Maven ArtifactResolver leaves file handles around so need to clean up
     * or we will run out of file descriptors.
     */
    protected void checkDoGarbageCollect() {
        if (this.resolveCount++ % 100 == 0) {
            System.gc();
            System.runFinalization();
        }
    }


}
