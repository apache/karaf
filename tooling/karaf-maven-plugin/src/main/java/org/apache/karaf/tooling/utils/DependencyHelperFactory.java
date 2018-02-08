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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.repository.RemoteRepository;

import java.util.List;

/**
 * <p>Service-locator based factory for available Aether system (Sonatype for Maven 3.0.x or Eclipse for Maven 3.1.x)</p>
 */
public class DependencyHelperFactory {

    /**
     * <p>Create a new {@link DependencyHelper} based on what has been found in {@link org.codehaus.plexus.PlexusContainer}</p>
     *
     * <p>{@code karaf-maven-plugin} depends on {@code maven-core:3.0}, so for Maven 3.0.x, it may use this API directly.
     * When using Maven 3.1.x/3.2.x, it should use reflection to invoke org.apache.maven.RepositoryUtils.toArtifact(Artifact)
     * as this method directly references specific Aether implementation.</p>
     *
     * <p>When {@code karaf-maven-plugin} switches to {@code maven-core:3.1.0+}, reflection should be use for Sonatype variant of Aether.</p>
     *
     * @param container The Maven Plexus container to use.
     * @param mavenProject The Maven project to use.
     * @param mavenSession The Maven session.
     * @param log The log to use for the messages.
     * @return The {@link DependencyHelper} depending of the Maven version used.
     * @throws MojoExecutionException If the plugin execution fails.
     */
    public static DependencyHelper createDependencyHelper(PlexusContainer container, MavenProject mavenProject, MavenSession mavenSession, int cacheSize, Log log) throws MojoExecutionException {
        try {
            if (container.hasComponent("org.sonatype.aether.RepositorySystem")) {
                org.sonatype.aether.RepositorySystem system = container.lookup(org.sonatype.aether.RepositorySystem.class);
                org.sonatype.aether.RepositorySystemSession session = mavenSession.getRepositorySession();
                List<RemoteRepository> repositories = mavenProject.getRemoteProjectRepositories();
                return new Dependency30Helper(repositories, session, system);
            } else if (container.hasComponent("org.eclipse.aether.RepositorySystem")) {
                org.eclipse.aether.RepositorySystem system = container.lookup(org.eclipse.aether.RepositorySystem.class);
                Object session;
                try {
                    session = MavenSession.class.getMethod("getRepositorySession").invoke(mavenSession);
                } catch (Exception e) {
                    throw new MojoExecutionException(e.getMessage(), e);
                }
                List<?> repositories = mavenProject.getRemoteProjectRepositories();
                return new Dependency31Helper(repositories, session, system, cacheSize);
            }
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
        throw new MojoExecutionException("Cannot locate either org.sonatype.aether.RepositorySystem or org.eclipse.aether.RepositorySystem");
    }

}