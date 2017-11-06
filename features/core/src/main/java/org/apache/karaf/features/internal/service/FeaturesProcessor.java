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

import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.model.Features;

/**
 * Service that can process (enhance, modify, trim, ...) a set of features read from {@link Repository}.
 */
public interface FeaturesProcessor {

    /**
     * Checks whether given repository URI is <em>blacklisted</em>
     * @param uri
     * @return
     */
    boolean isRepositoryBlacklisted(URI uri);

    /**
     * Processes original {@link Features JAXB model of features}
     * @param features
     */
    void process(Features features);

}
