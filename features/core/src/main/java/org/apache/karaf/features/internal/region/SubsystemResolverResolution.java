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

import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.eclipse.equinox.region.Region;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.repository.Repository;

/**
 * Public API of {@link SubsystemResolver} - for the purpose of documentation and categorization to public and internal
 * methods. This interface groups methods related to resolution of {@link Subsystem subsystems}.
 */
public interface SubsystemResolverResolution {

    /**
     * <p>Prepares the resolver by configuring {@link Subsystem} hierarchy</p>
     * <p>The input is a mapping from {@link Region region names} to a set of logical requirements.<br/>
     * The effect is:<ul>
     *     <li>A tree of {@link Subsystem subsystems} where the root subsystem represents {@link FeaturesService#ROOT_REGION}
     *      with regions like <code>root/app1</code> represented as child subsystems.</li>
     *     <li>A subsystem is created for each feature requirement and added as child and requirement for given region's subsystem</li>
     *     <li>Each subsystem for a feature has optional requirements for conditional features</li>
     * </ul></p>
     *
     * @param allFeatures all currently available features partitioned by name
     * @param requirements desired mapping from regions to logical requirements
     * @param system mapping from regions to unmanaged {@link BundleRevision}s
     * @throws Exception
     */
    void prepare(Map<String, List<Feature>> allFeatures,
                 Map<String, Set<String>> requirements,
                 Map<String, Set<BundleRevision>> system) throws Exception;

    /**
     * Before attempting {@link #resolve resolution}, we can collect features' prerequisites. If there are any,
     * caller may decide to deploy another set of requirements <strong>before</strong> the initial ones.
     * Prerequisites allow to install for example <code>wrap</code> feature before installing a feature with bundle
     * using <code>wrap:</code> protocol.
     * @return
     */
    Set<String> collectPrerequisites();

    /**
     *
     * @param featureResolutionRange
     * @param serviceRequirements how to handle requirements from {@link org.osgi.namespace.service.ServiceNamespace#SERVICE_NAMESPACE}
     * namespace
     * @param globalRepository
     * @param outputFile
     * @return
     * @throws Exception
     */
    public Map<Resource, List<Wire>> resolve(String featureResolutionRange,
                                             FeaturesService.ServiceRequirementsBehavior serviceRequirements,
                                             final Repository globalRepository,
                                             String outputFile) throws Exception;

}
