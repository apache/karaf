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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (karafVersion == null) {
            Package p = Package.getPackage("org.apache.karaf.tooling.features");
            karafVersion = p.getImplementationVersion();
        }

        addFeatureRepo(String.format(KARAF_CORE_ENTERPRISE_FEATURE_URL, karafVersion));
        addFeatureRepo(String.format(KARAF_CORE_STANDARD_FEATURE_URL, karafVersion));
        addFeatureRepo(String.format(KARAF_CORE_STANDARD_FEATURE_URL, karafVersion));

        try {
            Set<String> bundles = new HashSet<String>();
            Map<String, Feature> featuresMap = new HashMap<String, Feature>();
            for (String uri : descriptors) {
                retrieveDescriptorsRecursively(uri, bundles, featuresMap);
            }

            // no features specified, handle all of them
            if (features == null) {
                features = new ArrayList<String>(featuresMap.keySet());
            }

            Set<String> featuresBundles = new HashSet<String>();
            Set<String> transitiveFeatures = new HashSet<String>();
            addFeatures(features, featuresBundles, transitiveFeatures, featuresMap);

            // add the bundles of the configured features to the bundles list
            bundles.addAll(featuresBundles);

            // if transitive features are enabled we add the contents of those
            // features to the bundles list
            if (addTransitiveFeatures) {
                for (String feature : transitiveFeatures) {
                    // transitiveFeatures contains name/version
                    Feature f = featuresMap.get(feature);
                    getLog().info("Adding contents of transitive feature: " + feature);
                    bundles.addAll(f.getBundles());
                    // Treat the config files as bundles, since it is only copying
                    bundles.addAll(f.getConfigFiles());
                }
            }

            getLog().info("Base repo: " + localRepo.getUrl());
            int currentBundle = 0;
            for (String bundle : bundles) {
                Artifact artifact = resourceToArtifact(bundle, skipNonMavenProtocols);

                // Maven ArtifactResolver leaves file handles around so need to clean up
                // or we will run out of file descriptors
                if (currentBundle++ % 100 == 0) {
                    System.gc();
                    System.runFinalization();
                }

                if (artifact == null) {
                    continue;
                }
                resolveAndCopyArtifact(artifact, remoteRepos);
            }

            if (copyFileBasedDescriptors != null) {
                for (CopyFileBasedDescriptor fileBasedDescriptor : copyFileBasedDescriptors) {
                    File destDir = new File(repository, fileBasedDescriptor.getTargetDirectory());
                    File destFile = new File(destDir, fileBasedDescriptor.getTargetFileName());
                    copy(fileBasedDescriptor.getSourceFile(), destFile);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Error populating repository", e);
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
        String dir = artifact.getGroupId().replace('.', '/') + "/" + artifact.getArtifactId() + "/" + artifact.getBaseVersion() + "/";
        String name = artifact.getArtifactId() + "-" + artifact.getBaseVersion()
            + (artifact.getClassifier() != null ? "-" + artifact.getClassifier() : "") + "." + artifact.getType();
        return dir + name;
    }

    private void addFeatures(List<String> features, Set<String> featuresBundles, Set<String> transitiveFeatures,
            Map<String, Feature> featuresMap) {
        for (String feature : features) {

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
            // only add the feature to transitives if it is not
            // listed in the features list defined by the config
            if (!this.features.contains(f.getName() + "/" + f.getVersion())) {
                transitiveFeatures.add(f.getName() + "/" + f.getVersion());
            } else {
                // add the bundles of the feature to the bundle set
                getLog().info("Adding contents for feature: " + f.getName() + "/" + f.getVersion());
                featuresBundles.addAll(f.getBundles());
                // Treat the config files as bundles, since it is only copying
                featuresBundles.addAll(f.getConfigFiles());
            }
            addFeatures(f.getDependencies(), featuresBundles, transitiveFeatures, featuresMap);
        }
    }

}
