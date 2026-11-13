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

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
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
    public void setUp() throws Exception {
        URL hello = getClass().getClassLoader().getResource("www/hello.txt");
        URL nested = getClass().getClassLoader().getResource("www/sub/nested.txt");
        URL sub = getClass().getClassLoader().getResource("www/sub/");
        Bundle bundle = mock(Bundle.class);
        expect(bundle.getLastModified()).andReturn(1000L).anyTimes();
        expect(bundle.getEntry("/")).andReturn(sub).anyTimes();
        // the entries of the bundle, then the ones of its fragment
        expect(bundle.findEntries("/", "*", true)).andAnswer(() -> Collections.enumeration(Arrays.asList(
                entry(1, "/index.html", hello),
                entry(1, "/css/", null),
                entry(1, "/css/app.css", hello),
                // no entry for WEB-INF and WEB-INF/classes, but entries inside
                entry(1, "/WEB-INF/classes/App.class", hello),
                entry(2, "/index.html", nested),
                entry(2, "/css/theme.css", nested),
                // the entry of a directory without a trailing slash, as a bundle installed from a directory
                // may return it
                entry(2, "/js", null),
                entry(2, "/js/app.js", nested)))).once();
        replay(bundle);
        resourceSet = new BundleResourceSet(null, bundle);
    }

    private static URL entry(long bundleId, String path, URL content) throws MalformedURLException {
        return new URL("bundle", bundleId + ".0", -1, path, new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(URL url) throws IOException {
                if (content == null) {
                    throw new IOException("Not a file: " + url);
                }
                return content.openConnection();
            }
        });
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
    public void fragment() throws Exception {
        WebResource resource = resourceSet.getResource("/css/theme.css");
        assertTrue(resource.isFile());
        assertEquals("nested", new String(resource.getContent(), StandardCharsets.UTF_8).trim());
        // the entry of the bundle wins over the one of the fragment
        assertEquals("Hello resource",
                new String(resourceSet.getResource("/index.html").getContent(), StandardCharsets.UTF_8).trim());
    }

    @Test
    public void directory() {
        assertTrue(resourceSet.getResource("/css").isDirectory());
        assertTrue(resourceSet.getResource("/css/").isDirectory());
        assertTrue(resourceSet.getResource("/js").isDirectory());
        // no entry for WEB-INF, but entries inside
        WebResource webInf = resourceSet.getResource("/WEB-INF");
        assertTrue(webInf.exists());
        assertTrue(webInf.isDirectory());
        assertEquals("WEB-INF", webInf.getName());
        assertTrue(resourceSet.getResource("/WEB-INF/classes").isDirectory());
        assertTrue(resourceSet.getResource("/").isDirectory());
    }

    @Test
    public void missing() {
        assertFalse(resourceSet.getResource("/missing.html").exists());
        assertFalse(resourceSet.getResource("/index.html/").exists());
    }

    @Test
    public void list() {
        assertArrayEquals(new String[]{"index.html", "css", "WEB-INF", "js"}, resourceSet.list("/"));
        assertArrayEquals(new String[]{"app.css", "theme.css"}, resourceSet.list("/css/"));
        assertArrayEquals(new String[]{"classes"}, resourceSet.list("/WEB-INF"));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("/WEB-INF/classes/")),
                resourceSet.listWebAppPaths("/WEB-INF/"));
        assertEquals(0, resourceSet.list("/missing").length);
    }

}
