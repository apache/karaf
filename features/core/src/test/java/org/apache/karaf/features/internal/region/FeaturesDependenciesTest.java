/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.region;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.service.Deployer;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.junit.Assert.assertEquals;

public class FeaturesDependenciesTest {

    Logger logger = LoggerFactory.getLogger(FeaturesDependenciesTest.class);
    Resolver resolver = new ResolverImpl(new Slf4jResolverLog(logger));

    @Test
    public void testFeatureDependency1() throws Exception {
        doTestFeatureDependency(
                new String[] { "f1" },
                new String[] { "a1/1.0.0", "b/2.0.0" }
        );
    }

    @Test
    public void testFeatureDependency1b() throws Exception {
        doTestFeatureDependency(
                new String[] { "f1", "dep/[1.0,2.0)"},
                new String[] { "a1/1.0.0", "b/1.0.0" }
        );
    }

    @Test
    public void testFeatureDependency2() throws Exception {
        doTestFeatureDependency(
                new String[] { "f2" },
                new String[] { "a1/1.0.0", "b/2.0.0" }
        );
    }

    @Test
    public void testFeatureDependency2b() throws Exception {
        doTestFeatureDependency(
                new String[] { "f2", "dep/[1.0,2.0)"},
                new String[] { "a1/1.0.0", "b/1.0.0" }
        );
    }

    @Test
    public void testFeatureDependency3() throws Exception {
        doTestFeatureDependency(
                new String[] { "f3" },
                new String[] { "a2/1.0.0" }
        );
    }

    @Test
    public void testFeatureDependency3b() throws Exception {
        doTestFeatureDependency(
                new String[] { "f3", "dep/[1.0,2.0)"},
                new String[] { "a2/1.0.0", "b/1.0.0" }
        );
    }

    @Test
    public void testFeatureDependency4() throws Exception {
        doTestFeatureDependency(
                new String[] { "f4" },
                new String[] { "a2/1.0.0", "b/2.0.0" }
        );
    }

    @Test
    public void testFeatureDependency4b() throws Exception {
        doTestFeatureDependency(
                new String[] { "f4", "dep/[1.0,2.0)"},
                new String[] { "a2/1.0.0", "b/1.0.0" }
        );
    }

    @Test
    public void testSpring() throws Exception {
        doTestFeatureDependency(
                new String[] { "spring-dm-web"},
                new String[] { "spring-osgi-core/1.2.1", "spring-core/3.2.14" }
        );
    }

    @Test
    public void testFeatureDependencyLevel() throws Exception {
        doTestFeatureDependency(
                new String[] { "tf1" },
                new String[] { "a2/1.0.0" }
        );
    }

    private void doTestFeatureDependency(String[] features, String[] bundles) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data8/features.xml").toURI());

        Map<String, Set<String>> requirements = new HashMap<>();
        for (String feature : features) {
            addToMapSet(requirements, "root", feature);
        }

        Map<String, Set<String>> expected = new HashMap<>();
        for (String bundle : bundles) {
            addToMapSet(expected, "root", bundle);
        }

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data8"));
        resolver.prepare(partitionByName(repo.getFeatures()),
                requirements,
                Collections.emptyMap());
        resolver.resolve(FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                null, null, null);

        verify(resolver, expected);
    }

    private void verify(SubsystemResolver resolver, Map<String, Set<String>> expected) {
        Map<String, Set<String>> mapping = getBundleNamesPerRegions(resolver);
        if (!expected.equals(mapping)) {
            dumpBundles(resolver);
            dumpWiring(resolver);
            assertEquals("Resolution failed", expected, mapping);
        }
    }

    private void dumpBundles(SubsystemResolver resolver) {
        System.out.println("Bundle mapping");
        Map<String, Set<Resource>> bundles = resolver.getBundlesPerRegions();
        for (Map.Entry<String, Set<Resource>> entry : bundles.entrySet()) {
            System.out.println("    " + entry.getKey());
            for (Resource b : entry.getValue()) {
                System.out.println("        " + b);
            }
        }
    }

    private Map<String, Set<String>> getBundleNamesPerRegions(SubsystemResolver resolver) {
        Map<String, Set<String>> mapping = new HashMap<>();
        Map<String, Set<Resource>> bundles = resolver.getBundlesPerRegions();
        for (Map.Entry<String,Set<Resource>> entry : bundles.entrySet()) {
            for (Resource r : entry.getValue()) {
                addToMapSet(mapping, entry.getKey(), r.toString());
            }
        }
        return mapping;
    }


    private void dumpWiring(SubsystemResolver resolver) {
        System.out.println("Wiring");
        Map<Resource, List<Wire>> wiring = resolver.getWiring();
        List<Resource> resources = new ArrayList<>(wiring.keySet());
        resources.sort(Comparator.comparing(this::getName));
        for (Resource resource : resources) {
            System.out.println("    " + getName(resource));
            for (Wire wire : wiring.get(resource)) {
                System.out.println("        " + wire);
            }
        }
    }

    private String getName(Resource resource) {
        Capability cap = resource.getCapabilities(IdentityNamespace.IDENTITY_NAMESPACE).get(0);
        return cap.getAttributes().get(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE) + ": "
                + cap.getAttributes().get(IdentityNamespace.IDENTITY_NAMESPACE) + "/"
                + cap.getAttributes().get(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
    }

    private Map<String, List<Feature>> partitionByName(Feature[] features) {
        Deployer.DeploymentState ds = new Deployer.DeploymentState();
        ds.partitionFeatures(Arrays.asList(features));
        return ds.featuresByName();
    }

}
