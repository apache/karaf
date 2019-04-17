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
import java.util.List;
import java.util.Set;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.internal.model.Bundle;
import org.apache.karaf.features.internal.model.ConfigFile;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.tooling.utils.MavenUtil;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Add features to a repository directory
 */
@Mojo(name = "features-add-to-repository", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class AddToRepositoryMojo extends AbstractFeatureMojo {

    @Parameter(defaultValue = "${project.build.directory}/features-repo")
    protected File repository;

    /**
     * If set to true the exported bundles will be directly copied into the repository dir.
     * If set to false the default maven repository layout will be used
     */
    @Parameter
    private boolean flatRepoLayout;

    @Parameter
    protected List<CopyFileBasedDescriptor> copyFileBasedDescriptors;

    @Parameter(defaultValue = "false")
    private boolean timestampedSnapshot;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Feature> featuresSet = resolveFeatures();
        
        for (Artifact descriptor : descriptorArtifacts) {
            copy(descriptor, repository);
        }

        for (Feature feature : featuresSet) {
            copyBundlesToDestRepository(feature.getBundle());
            for(Conditional conditional : feature.getConditional()) {
                copyBundlesConditionalToDestRepository(conditional.getBundles());
            }
            copyConfigFilesToDestRepository(feature.getConfigfile());
        }
        
        copyFileBasedDescriptorsToDestRepository();
        
    }

    private void copyBundlesConditionalToDestRepository(List<? extends BundleInfo> artifactRefsConditional) throws MojoExecutionException {
        for (BundleInfo artifactRef : artifactRefsConditional) {
            if (ignoreDependencyFlag || (!ignoreDependencyFlag && !artifactRef.isDependency())) {
                Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
                // Avoid getting NPE on artifact.getFile in some cases 
                resolveArtifact(artifact, remoteRepos);
                if (artifact != null) {
                    copy(artifact, repository);
                }
            }
        }
    }
    
    private void copyBundlesToDestRepository(List<? extends Bundle> artifactRefs) throws MojoExecutionException {
        for (Bundle artifactRef : artifactRefs) {
            Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
            // Avoid getting NPE on artifact.getFile in some cases 
            resolveArtifact(artifact, remoteRepos);
            if (artifact != null) {
                copy(artifact, repository);
            }
        }
    }

    private void copyConfigFilesToDestRepository(List<? extends ConfigFile> artifactRefs) throws MojoExecutionException {
        for (ConfigFile artifactRef : artifactRefs) {
            Artifact artifact = resourceToArtifact(artifactRef.getLocation(), skipNonMavenProtocols);
            // Avoid getting NPE on artifact.getFile in some cases
            resolveArtifact(artifact, remoteRepos);
            if (artifact != null) {
                copy(artifact, repository);
            }
        }
    }

    protected void copy(Artifact artifact, File destRepository) {
        try {
            getLog().info("Copying artifact: " + artifact);
            File destFile = new File(destRepository, getRelativePath(artifact));
            if (artifact.getFile() == null) {
                throw new IllegalStateException("Artifact is not present in local repo."); 
            }
            copy(artifact.getFile(), destFile);
        } catch (Exception e) {
            getLog().warn("Error copying artifact " + artifact, e);
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
        String name = MavenUtil.getFileName(artifact, timestampedSnapshot);
        return dir + name;
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

}
