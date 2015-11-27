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
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Slf4jResolverLog;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.apache.karaf.features.internal.support.TestDownloadManager;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.framework.wiring.BundleRevision;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;
import org.osgi.service.resolver.Resolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.junit.Assert.assertEquals;

public class SubsystemTest {

    Logger logger = LoggerFactory.getLogger(SubsystemTest.class);;
    Resolver resolver = new ResolverImpl(new Slf4jResolverLog(logger));

    @Test
    public void test1() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root", "f1");
        addToMapSet(features, "root/apps1", "f2");

        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root", "a/1.0.0");
        addToMapSet(expected, "root", "c/1.0.0");
        addToMapSet(expected, "root/apps1", "b/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data1"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                         features,
                         Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                         FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                         null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void test2() throws Exception {

        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data2/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "f1");
        addToMapSet(features, "root/apps1", "f3");
        addToMapSet(features, "root/apps2", "f1");

        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "c/1.0.0");
        addToMapSet(expected, "root/apps1", "b/1.0.0");
        addToMapSet(expected, "root/apps1", "e/1.0.0");
        addToMapSet(expected, "root/apps1#f1", "a/1.0.0");
        addToMapSet(expected, "root/apps1#f1", "d/1.0.0");
        addToMapSet(expected, "root/apps2", "b/1.0.0");
        addToMapSet(expected, "root/apps2", "c/1.0.0");
        addToMapSet(expected, "root/apps2#f1", "a/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data2"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                         features,
                         Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                         FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                         null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testOverrides() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data3/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "f1");

        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "a/1.0.1");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data3"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                         features,
                         Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.singleton("b"),
                         FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                         null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testConditionalUnsatisfiedWithOptional() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data4/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "f1");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "a/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data4"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                         features,
                         Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                         FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                         null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testConditionalSatisfiedWithOptional() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data4/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "f1");
        addToMapSet(features, "root/apps1", "f2");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "a/1.0.0");
        addToMapSet(expected, "root/apps1", "b/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data4"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                         features,
                         Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                         FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                         null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testBundle() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data1/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "bundle:a");
        addToMapSet(features, "root/apps1", "bundle:c;dependency=true");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "a/1.0.0");
        addToMapSet(expected, "root/apps1", "c/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data1"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                features,
                Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testFeatureOptional() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data5/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root", "f1");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root", "a/1.0.0");
        addToMapSet(expected, "root", "b/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data5"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                features,
                Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testFeatureOptionalAlreadyProvided() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data5/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root", "f1");
        addToMapSet(features, "root", "f3");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root", "a/1.0.0");
        addToMapSet(expected, "root", "c/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data5"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                features,
                Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testFeatureOptionalAlreadyProvided2() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data6/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root", "pax-http");
        addToMapSet(features, "root", "pax-http-tomcat");
        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root", "a/1.0.0");
        addToMapSet(expected, "root", "c/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data6"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                features,
                Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
                null, null, null);

        verify(resolver, expected);
    }

    @Test
    public void testResourceRepositories() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data7/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root", "f1");
        addToMapSet(features, "root/apps1", "f2");

        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root", "a/1.0.0");
        addToMapSet(expected, "root", "c/1.0.0");
        addToMapSet(expected, "root/apps1", "b/1.0.0");

        SubsystemResolver resolver = new SubsystemResolver(this.resolver, new TestDownloadManager(getClass(), "data7"));
        resolver.prepare(Arrays.asList(repo.getFeatures()),
                features,
                Collections.<String, Set<BundleRevision>>emptyMap());
        resolver.resolve(Collections.<String>emptySet(),
                FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE,
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
        Map<String, Set<String>> mapping = new HashMap<String, Set<String>>();
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
        List<Resource> resources = new ArrayList<Resource>(wiring.keySet());
        Collections.sort(resources, new Comparator<Resource>() {
            @Override
            public int compare(Resource o1, Resource o2) {
                return getName(o1).compareTo(getName(o2));
            }
        });
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

}
