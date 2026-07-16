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
package org.apache.karaf.services.url;

import java.io.File;
import java.io.IOException;

/**
 * Service interface for resolving Maven artifact URIs.
 * <p>
 * Maven URIs follow the syntax:
 * {@code mvn:[repository_url!]groupId/artifactId[/[version][/[type][/classifier]]]}
 */
public interface MavenResolver {

    /**
     * Resolve a Maven artifact URI to a local file.
     *
     * @param url the Maven artifact URI (e.g. {@code mvn:groupId/artifactId/version})
     * @return the resolved file
     * @throws IOException if the artifact cannot be resolved
     */
    File resolve(String url) throws IOException;

    /**
     * Resolve a Maven artifact by its coordinates to a local file.
     *
     * @param groupId    the group ID
     * @param artifactId the artifact ID
     * @param version    the version (can be {@code null} for LATEST)
     * @param type       the type/extension (can be {@code null} for jar)
     * @param classifier the classifier (can be {@code null})
     * @return the resolved file
     * @throws IOException if the artifact cannot be resolved
     */
    File resolve(String groupId, String artifactId, String version, String type, String classifier) throws IOException;

    /**
     * Get the local repository path.
     *
     * @return the local repository directory
     */
    File getLocalRepository();

}
