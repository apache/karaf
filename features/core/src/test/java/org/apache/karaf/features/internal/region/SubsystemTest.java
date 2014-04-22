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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.download.StreamProvider;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.apache.karaf.features.internal.download.simple.SimpleDownloader;
import org.junit.Test;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.resource.Capability;
import org.osgi.resource.Resource;
import org.osgi.resource.Wire;

import static org.apache.karaf.features.internal.util.MapUtils.addToMapSet;
import static org.junit.Assert.assertEquals;

public class SubsystemTest {

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

        SubsystemResolver resolver = new SubsystemResolver(new TestDownloadManager("data1"));
        resolver.resolve(Collections.<Repository>singletonList(repo),
                         features,
                         Collections.<Resource>emptyList(),
                         Collections.<String>emptySet(),
                         FeaturesServiceImpl.DEFAULT_FEATURE_RESOLUTION_RANGE);

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

        SubsystemResolver resolver = new SubsystemResolver(new TestDownloadManager("data2"));
        resolver.resolve(Collections.<Repository>singletonList(repo),
                         features,
                         Collections.<Resource>emptyList(),
                         Collections.<String>emptySet(),
                         FeaturesServiceImpl.DEFAULT_FEATURE_RESOLUTION_RANGE);

        verify(resolver, expected);
    }

    @Test
    public void testOverrides() throws Exception {
        RepositoryImpl repo = new RepositoryImpl(getClass().getResource("data3/features.xml").toURI());

        Map<String, Set<String>> features = new HashMap<String, Set<String>>();
        addToMapSet(features, "root/apps1", "f1");

        Map<String, Set<String>> expected = new HashMap<String, Set<String>>();
        addToMapSet(expected, "root/apps1", "a/1.0.1");

        SubsystemResolver resolver = new SubsystemResolver(new TestDownloadManager("data3"));
        resolver.resolve(Collections.<Repository>singletonList(repo),
                features,
                Collections.<Resource>emptyList(),
                Collections.singleton("b"),
                FeaturesServiceImpl.DEFAULT_FEATURE_RESOLUTION_RANGE);

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

    class TestDownloadManager extends SimpleDownloader {

        private final String dir;

        TestDownloadManager(String dir) {
            this.dir = dir;
        }

        @Override
        protected StreamProvider createProvider(String location) throws MalformedURLException {
            return new TestProvider(location);
        }

        class TestProvider implements StreamProvider {
            private final IOException exception;
            private final Map<String, String> headers;
            private final byte[] data;

            TestProvider(String location) {
                Map<String, String> headers = null;
                byte[] data = null;
                IOException exception = null;
                try {
                    Manifest man = new Manifest(getClass().getResourceAsStream(dir +"/" + location + ".mf"));
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    JarOutputStream jos = new JarOutputStream(baos, man);
                    jos.close();
                    data = baos.toByteArray();
                    headers = new HashMap<String, String>();
                    for (Map.Entry attr : man.getMainAttributes().entrySet()) {
                        headers.put(attr.getKey().toString(), attr.getValue().toString());
                    }
                } catch (IOException e) {
                    exception = e;
                }
                this.headers = headers;
                this.data = data;
                this.exception = exception;
            }

            @Override
            public InputStream open() throws IOException {
                if (exception != null)
                    throw exception;
                return new ByteArrayInputStream(data);
            }

            @Override
            public Map<String, String> getMetadata() throws IOException {
                if (exception != null)
                    throw exception;
                return headers;
            }
        }
    }
}
