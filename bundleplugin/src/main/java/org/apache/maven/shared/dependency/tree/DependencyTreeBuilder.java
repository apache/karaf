package org.apache.maven.shared.dependency.tree;

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

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.project.MavenProject;

/**
 * Builds a tree of dependencies for a given Maven project.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: DependencyTreeBuilder.java 661727 2008-05-30 14:21:49Z bentmann $
 */
public interface DependencyTreeBuilder
{
    // fields -----------------------------------------------------------------

    /**
     * The plexus role for this component.
     */
    String ROLE = DependencyTreeBuilder.class.getName();

    // public methods ---------------------------------------------------------

    /**
     * Builds a tree of dependencies for the specified Maven project.
     * 
     * @param project
     *            the Maven project
     * @param repository
     *            the artifact repository to resolve against
     * @param factory
     *            the artifact factory to use
     * @param metadataSource
     *            the artifact metadata source to use
     * @param collector
     *            the artifact collector to use
     * @return the dependency tree of the specified Maven project
     * @throws DependencyTreeBuilderException
     *             if the dependency tree cannot be resolved
     * @deprecated As of 1.1, replaced by
     *             {@link #buildDependencyTree(MavenProject, ArtifactRepository, ArtifactFactory, ArtifactMetadataSource, ArtifactFilter, ArtifactCollector)}
     */
    DependencyTree buildDependencyTree( MavenProject project, ArtifactRepository repository, ArtifactFactory factory,
                                        ArtifactMetadataSource metadataSource, ArtifactCollector collector )
        throws DependencyTreeBuilderException;

    /**
     * Builds a tree of dependencies for the specified Maven project.
     * 
     * @param project
     *            the Maven project
     * @param repository
     *            the artifact repository to resolve against
     * @param factory
     *            the artifact factory to use
     * @param metadataSource
     *            the artifact metadata source to use
     * @param filter
     *            the artifact filter to use
     * @param collector
     *            the artifact collector to use
     * @return the dependency tree root node of the specified Maven project
     * @throws DependencyTreeBuilderException
     *             if the dependency tree cannot be resolved
     * @since 1.1
     */
    DependencyNode buildDependencyTree( MavenProject project, ArtifactRepository repository, ArtifactFactory factory,
                                        ArtifactMetadataSource metadataSource, ArtifactFilter filter, ArtifactCollector collector )
        throws DependencyTreeBuilderException;
}
