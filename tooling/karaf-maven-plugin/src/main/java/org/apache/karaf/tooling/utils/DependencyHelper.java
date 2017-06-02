/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.utils;

import java.io.File;
import java.util.Collection;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuildingRequest;

/**
 * <p>An interface for accessing available Aether subsystem (Sonatype for Maven 3.0.x or Eclipse for Maven 3.1.x)</p>
 *
 * <p>Some methods have {@link Object} parameters because they should be able to receive Aether classes from
 * both Aether variants.</p>
 */
public interface DependencyHelper {

    Collection<LocalDependency> getLocalDependencies();

    String getTreeListing();

    void getDependencies(MavenProject project, boolean useTransitiveDependencies) throws MojoExecutionException;

    boolean isArtifactAFeature(Object artifact);

    String getArtifactId(Object artifact);

	String getBaseVersion(Object artifact);

	String getGroupId(Object artifact);
    
    String getClassifier(Object artifact);

    File resolve(Object artifact, Log log);

    File resolveById(String id, Log log) throws MojoFailureException;

    void setRepositorySession(ProjectBuildingRequest request) throws MojoExecutionException;
    
    /**
     * Convert a Maven <code>Artifact</code> into a PAX URL mvn format.
     *
     * @param artifact The Maven <code>Artifact</code>.
     * @return The corresponding PAX URL mvn format (mvn:groupId/artifactId/version/type/classifier)
     * @throws MojoExecutionException If the plugin execution fails.
     */
    String artifactToMvn(Artifact artifact, String versionOrRange) throws MojoExecutionException;

    /**
     * Convert an Aether (Sonatype or Eclipse) artifact into a PAX URL mvn format.
     *
     * @param object The Aether <code>org.sonatype|eclipse.aether.artifact.Artifact</code>.
     * @return The corresponding PAX URL mvn format (mvn:groupId/artifactId/version/type/classifier).
     * @throws MojoExecutionException If the plugin execution fails.
     */
    String artifactToMvn(Object object, String versionOrRange) throws MojoExecutionException;

    Artifact mvnToArtifact(String name) throws MojoExecutionException;

    /**
     * Convert a PAX URL mvn format into a filesystem path.
     *
     * @param name PAX URL mvn format (mvn:groupId/artifactId/version/type/classifier).
     * @return The filesystem path.
     * @throws MojoExecutionException If the plugin execution fails.
     */
    String pathFromMaven(String name) throws MojoExecutionException;

    /**
     * Convert an Aether coordinate format into a filesystem path.
     *
     * @param name The Aether coordinate format (groupId:artifactId[:extension[:classifier]]:version).
     * @return The filesystem path.
     * @throws MojoExecutionException If the plugin execution fails.
     */
    String pathFromAether(String name) throws MojoExecutionException;
}
