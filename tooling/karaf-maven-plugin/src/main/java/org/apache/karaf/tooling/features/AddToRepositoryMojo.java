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

import org.apache.karaf.tooling.features.model.BundleRef;
import org.apache.karaf.tooling.features.model.Feature;
import org.apache.maven.artifact.Artifact;
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
public class AddToRepositoryMojo extends AbstractFeatureMojo {

    /**
     * @parameter expression="${project.build.directory}/features-repo"
     */
    protected File repository;

    /**
     * If set to true the exported bundles will be directly copied into the repository dir.
     * If set to false the default maven repository layout will be used
     * @parameter
     */
    private boolean flatRepoLayout;

    /**
     * @parameter
     */
    protected List<CopyFileBasedDescriptor> copyFileBasedDescriptors;

    public void execute() throws MojoExecutionException, MojoFailureException {
        Set<Feature> featuresSet = resolveFeatures();
        
        for (Artifact descriptor : descriptorArtifacts) {
            copy(descriptor, repository);
        }

        for (Feature feature : featuresSet) {
            copyBundlesToDestRepository(feature.getBundles());
            copyArtifactsToDestRepository(feature.getConfigFiles());
        }
        
        copyFileBasedDescriptorsToDestRepository();
        
    }

    private void copyBundlesToDestRepository(List<BundleRef> bundleRefs) throws MojoExecutionException {
        for (BundleRef bundle : bundleRefs) {
            Artifact artifact = bundle.getArtifact();
            if (artifact != null) {
                copy(artifact, repository);
            }
        }
    }

    protected void copy(Artifact artifact, File destRepository) {
        getLog().info("Copying artifact: " + artifact);
        File destFile = new File(destRepository, getRelativePath(artifact));
        copy(artifact.getFile(), destFile);
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

    private void copyArtifactsToDestRepository(List<String> list) throws MojoExecutionException {
        for (String bundle : list) {
            Artifact artifact = resourceToArtifact(bundle, skipNonMavenProtocols);
            if (artifact != null) {
                resolveArtifact(artifact, remoteRepos);
                copy(artifact, repository);
            }
            checkDoGarbageCollect();
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

}
