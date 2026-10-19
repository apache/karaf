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
package org.apache.karaf.services.web.internal.wab;

import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import org.apache.catalina.WebResource;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/**
 * The bundle is mocked as Felix behaves: with a jar, the entry of a directory is returned for its path with or
 * without the trailing slash, and a directory may have no entry, only entries inside; with a directory, the entry
 * of a file is returned for its path with a trailing slash.
 */
public class BundleResourceSetTest {

    private BundleResourceSet resourceSet;

    @Before
    public void setUp() {
        URL hello = getClass().getClassLoader().getResource("www/hello.txt");
        URL sub = getClass().getClassLoader().getResource("www/sub/");
        Bundle bundle = mock(Bundle.class);
        expect(bundle.getLastModified()).andReturn(1000L).anyTimes();
        expect(bundle.getEntry("/")).andReturn(sub).anyTimes();
        expect(bundle.getEntry("index.html")).andReturn(hello).anyTimes();
        expect(bundle.getEntry("css/")).andReturn(sub).anyTimes();
        expect(bundle.getEntry("css")).andReturn(sub).anyTimes();
        // the entry of a file with a trailing slash, as a bundle installed from a directory returns it
        expect(bundle.getEntry("index.html/")).andReturn(hello).anyTimes();
        expect(bundle.getEntry(anyString())).andReturn(null).anyTimes();
        expect(bundle.getEntryPaths("/")).andAnswer(() -> Collections.enumeration(
                Arrays.asList("index.html", "css/", "WEB-INF/"))).anyTimes();
        expect(bundle.getEntryPaths("css/")).andAnswer(() -> Collections.enumeration(
                Collections.singletonList("css/app.css"))).anyTimes();
        expect(bundle.getEntryPaths("WEB-INF/")).andAnswer(() -> Collections.enumeration(
                Collections.singletonList("WEB-INF/classes/"))).anyTimes();
        expect(bundle.getEntryPaths(anyString())).andReturn(null).anyTimes();
        replay(bundle);
        resourceSet = new BundleResourceSet(null, bundle);
    }

    @Test
    public void file() throws Exception {
        WebResource resource = resourceSet.getResource("/index.html");
        assertTrue(resource.exists());
        assertTrue(resource.isFile());
        assertEquals("index.html", resource.getName());
        byte[] content = resource.getContent();
        assertEquals("Hello resource", new String(content, StandardCharsets.UTF_8).trim());
        // the length depends on the line ending of the checkout, \r\n on Windows
        assertEquals(content.length, resource.getContentLength());
    }

    @Test
    public void directory() {
        // the entry returned for "css" is the one of the directory
        assertTrue(resourceSet.getResource("/css").isDirectory());
        assertTrue(resourceSet.getResource("/css/").isDirectory());
        // no entry for WEB-INF, but entries inside
        WebResource webInf = resourceSet.getResource("/WEB-INF");
        assertTrue(webInf.exists());
        assertTrue(webInf.isDirectory());
        assertEquals("WEB-INF", webInf.getName());
        assertTrue(resourceSet.getResource("/").isDirectory());
    }

    @Test
    public void missing() {
        assertFalse(resourceSet.getResource("/missing.html").exists());
        assertFalse(resourceSet.getResource("/index.html/").exists());
    }

    @Test
    public void list() {
        assertArrayEquals(new String[]{"index.html", "css", "WEB-INF"}, resourceSet.list("/"));
        assertArrayEquals(new String[]{"classes"}, resourceSet.list("/WEB-INF"));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("/WEB-INF/classes/")),
                resourceSet.listWebAppPaths("/WEB-INF/"));
        assertEquals(0, resourceSet.list("/missing").length);
    }

}
