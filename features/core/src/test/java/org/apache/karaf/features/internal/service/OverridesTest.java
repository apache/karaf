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
package org.apache.karaf.features.internal.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.karaf.features.internal.resolver.ResourceBuilder;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleException;
import org.osgi.resource.Resource;

import static org.apache.karaf.features.internal.resolver.ResourceUtils.getUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OverridesTest {

    private String bsn = "bsn";
    private Resource b100;
    private Resource b101;
    private Resource b102;
    private Resource b110;
    private Resource c100;
    private Resource c101;
    private Resource c110;

    @Before
    public void setUp() throws BundleException {
        b100 = resource("karaf-100.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.0")
                .build();

        b101 = resource("karaf-101.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.1")
                .build();

        b102 = resource("karaf-102.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.2")
                .build();

        b110 = resource("karaf-110.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.1.0")
                .build();

        c100 = resource("karafc-100.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.0")
                .set("Bundle-Vendor", "Apache")
                .build();

        c101 = resource("karafc-101.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.0.1")
                .set("Bundle-Vendor", "NotApache")
                .build();

        c110 = resource("karafc-110.jar")
                .set("Bundle-SymbolicName", bsn)
                .set("Bundle-Version", "1.1.0")
                .set("Bundle-Vendor", "NotApache")
                .build();
    }

    @Test
    public void testDifferentVendors() throws IOException {
        Map<String, Resource> map = asResourceMap(c100, c101, c110);
        assertEquals(c100, map.get(getUri(c100)));
        Overrides.override(map, Arrays.asList(getUri(c101), getUri(c110)));
        assertEquals(c101, map.get(getUri(c100)));
    }

    @Test
    public void testMatching101() throws IOException {
        Map<String, Resource> map = asResourceMap(b100, b101, b110);
        assertEquals(b100, map.get(getUri(b100)));
        Overrides.override(map, Arrays.asList(getUri(b101), getUri(b110)));
        assertEquals(b101, map.get(getUri(b100)));
    }

    @Test
    public void testMatching102() throws IOException {
        Map<String, Resource> map = asResourceMap(b100, b101, b102, b110);
        assertEquals(b100, map.get(getUri(b100)));
        Overrides.override(map, Arrays.asList(getUri(b101), getUri(b102), getUri(b110)));
        assertEquals(b102, map.get(getUri(b100)));
    }

    @Test
    public void testMatchingRange() throws IOException {
        Map<String, Resource> map = asResourceMap(b100, b101, b110);
        assertEquals(b100, map.get(getUri(b100)));
        Overrides.override(map, Arrays.asList(getUri(b101), getUri(b110) + ";range=\"[1.0, 2.0)\""));
        assertEquals(b110, map.get(getUri(b100)));
    }

    @Test
    public void testNotMatching() throws IOException {
        Map<String, Resource> map = asResourceMap(b100, b110);
        assertEquals(b100, map.get(getUri(b100)));
        Overrides.override(map, Arrays.asList(getUri(b110)));
        assertEquals(b100, map.get(getUri(b100)));
    }

    @Test
    public void testLoadOverrides() {
        Set<String> overrides = Overrides.loadOverrides(getClass().getResource("overrides.properties").toExternalForm());
        assertEquals(2, overrides.size());

        Clause karafAdminCommand = null;
        Clause karafAdminCore = null;
        for (Clause clause : Parser.parseClauses(overrides.toArray(new String[overrides.size()]))) {
            if (clause.getName().equals("mvn:org.apache.karaf.admin/org.apache.karaf.admin.command/2.3.0.61033X")) {
                karafAdminCommand = clause;
            }
            if (clause.getName().equals("mvn:org.apache.karaf.admin/org.apache.karaf.admin.core/2.3.0.61033X")) {
                karafAdminCore = clause;
            }
        }
        assertNotNull("Missing admin.command bundle override", karafAdminCommand);
        assertNotNull("Missing admin.core bundle override", karafAdminCore);
        assertNotNull("Missing range on admin.core override", karafAdminCore.getAttribute(Overrides.OVERRIDE_RANGE));
    }

    static Builder resource(String uri) {
        return new Builder(uri);
    }

    static Map<String, Resource> asResourceMap(Resource... resources) {
        Map<String, Resource> map = new HashMap<>();
        for (Resource resource : resources) {
            map.put(getUri(resource), resource);
        }
        return map;
    }

    static class Builder {
        String uri;
        Map<String,String> headers = new HashMap<>();
        Builder(String uri) {
            this.uri = uri;
            this.headers.put("Bundle-ManifestVersion", "2");
        }
        Builder set(String key, String value) {
            this.headers.put(key, value);
            return this;
        }
        Resource build() throws BundleException {
            return ResourceBuilder.build(uri, headers);
        }
    }

}
