package org.apache.maven.shared.dependency.tree.filter;

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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.shared.dependency.tree.DependencyNode;

/**
 * A dependency node filter that delegates to an artifact filter.
 * 
 * @author <a href="mailto:markhobson@gmail.com">Mark Hobson</a>
 * @version $Id: ArtifactDependencyNodeFilter.java 661727 2008-05-30 14:21:49Z bentmann $
 * @since 1.1
 */
public class ArtifactDependencyNodeFilter implements DependencyNodeFilter
{
    // fields -----------------------------------------------------------------

    /**
     * The artifact filter this dependency node filter delegates to.
     */
    private final ArtifactFilter filter;

    // constructors -----------------------------------------------------------

    /**
     * Creates a dependency node filter that delegates to the specified artifact filter.
     * 
     * @param filter
     *            the artifact filter to delegate to
     */
    public ArtifactDependencyNodeFilter( ArtifactFilter filter )
    {
        this.filter = filter;
    }

    // DependencyNodeFilter methods -------------------------------------------

    /**
     * {@inheritDoc}
     */
    public boolean accept( DependencyNode node )
    {
        Artifact artifact = node.getArtifact();

        return filter.include( artifact );
    }

    // public methods ---------------------------------------------------------

    /**
     * Gets the artifact filter this dependency node filter delegates to.
     * 
     * @return the artifact filter this dependency node filter delegates to
     */
    public ArtifactFilter getArtifactFilter()
    {
        return filter;
    }
}
