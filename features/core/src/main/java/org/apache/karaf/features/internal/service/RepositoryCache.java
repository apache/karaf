/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.features.internal.service;

import java.net.URI;
import java.util.Set;

import org.apache.karaf.features.Repository;

/**
 * <p>An interface for accessing repository/features information. Simple implementations
 * may just map feature XMLs directly to JAXB model
 * (see: {@link org.apache.karaf.features.internal.model.Features}).</p>
 *
 * <p>In more complex cases, additional processing (blacklisting, overrides, patching)
 * may be performed.</p>
 */
public interface RepositoryCache {

    /**
     * Creates {@link Repository} without adding it to cache
     * @param uri an URI (e.g., <code>mvn:groupId/artifactId/version/xml/features</code> of repository
     * @param validate whether to perform XML Schema validation of loaded features XML
     * @return a {@link Repository} that may be inspected or added to cache
     */
    Repository create(URI uri, boolean validate);

    /**
     * Adds existing {@link Repository} to be tracked/managed by this cache and later be available e.g., via
     * {@link #getRepository(String)}
     * @param repository existing repository to add to cache
     */
    void addRepository(Repository repository);

    /**
     * Removes existing {@link Repository} by its {@link URI}
     * @param repositoryUri {@link URI} of the {@link Repository} to remove
     */
    void removeRepository(URI repositoryUri);

    /**
     * Gets {@link Repository} by its {@link URI}
     * @param uri {@link URI} of the repository
     * @return {@link Repository} as it's stored inside the cache
     */
    Repository getRepository(String uri);

    /**
     * Gets {@link Repository} by its name
     * @param name Name of the repository
     * @return {@link Repository} as it's stored inside the cache
     */
    Repository getRepositoryByName(String name);

    /**
     * Returns an array of all cached {@link Repository repositories}
     * @return list of all {@link Repository repositories}
     */
    Repository[] listRepositories();

    /**
     * Returns an array of cached {@link Repository repositories} for a set of {@link URI repository URIs}
     * @return list of matched {@link Repository repositories}
     */
    Repository[] listMatchingRepositories(Set<String> uris);

    /**
     * Returns a set of {@link Repository repositories} including passed repository and all referenced repositories.
     * @param repo A {@link Repository}, that possibly references other feature repositories.
     * @return A closure of {@link Repository repositories}
     */
    Set<Repository> getRepositoryClosure(Repository repo);

}
