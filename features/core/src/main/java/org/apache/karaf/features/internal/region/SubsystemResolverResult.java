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
package org.apache.karaf.features.internal.region;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleException;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.ResolveContext;

/**
 * Public API of {@link SubsystemResolver} - for the purpose of documentation and categorization to public and internal
 * methods. This interface groups methods invoked after performing resolution of {@link Subsystem subsystems}.
 */
public interface SubsystemResolverResult {

    /**
     * Get a map between regions, bundle locations and actual {@link BundleInfo}
     * @return
     */
    Map<String, Map<String, BundleInfo>> getBundleInfos();

    /**
     * Get map of all downloaded resources (location -&gt; provider)
     * @return
     */
    Map<String, StreamProvider> getProviders();

    /**
     * Returns a result of {@link org.osgi.service.resolver.Resolver#resolve(ResolveContext)}
     * @return
     */
    Map<Resource, List<Wire>> getWiring();

    /**
     * Return directed graph of {@link org.eclipse.equinox.region.Region regions} after resolution.
     * @return
     * @throws BundleException
     * @throws InvalidSyntaxException
     */
    RegionDigraph getFlatDigraph() throws BundleException, InvalidSyntaxException;

    /**
     * Returns a mapping between regions and a set of bundle {@link Resource resources}
     * @return
     */
    Map<String, Set<Resource>> getBundlesPerRegions();

    /**
     * Returns a mapping between regions and a set of feature {@link Resource resources}
     * @return
     */
    Map<String, Set<Resource>> getFeaturesPerRegions();

}
