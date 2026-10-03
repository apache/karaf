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
package org.apache.karaf.services.url.internal;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class WrapUrlHandlerTest {

    @Rule
    public TemporaryFolder tempDir = new TemporaryFolder();

    private final WrapUrlHandler handler = new WrapUrlHandler();

    // outside of OSGi, the wrap: protocol is not registered and the handler service can not parse the URL itself
    private final URLStreamHandler protocol = new URLStreamHandler() {
        @Override
        protected URLConnection openConnection(URL url) throws IOException {
            return handler.openConnection(url);
        }
    };

    private String jarUrl;

    @Before
    public void setUp() throws Exception {
        File jar = tempDir.newFile("test.jar");
        Files.write(jar.toPath(), BundleWrapperTest.createJar(new Manifest()));
        jarUrl = jar.toURI().toURL().toExternalForm();
    }

    @Test
    public void testWrap() throws Exception {
        Attributes headers = open("wrap:" + jarUrl);

        assertEquals(("wrap:" + jarUrl).replaceAll("[^a-zA-Z_0-9.-]", "_"), headers.getValue("Bundle-SymbolicName"));
        assertNotNull(headers.getValue("Export-Package"));
    }

    @Test
    public void testWrapWithInstructions() throws Exception {
        Attributes headers = open("wrap:" + jarUrl + "$Bundle-SymbolicName=org.example.test&Bundle-Version=1.2.3");

        assertEquals("org.example.test", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
    }

    @Test
    public void testWrapWithInstructionsFile() throws Exception {
        String instructionsUrl = createInstructionsFile("Bundle-SymbolicName=org.example.file", "Bundle-Version=1.2.3");

        Attributes headers = open("wrap:" + jarUrl + "," + instructionsUrl);

        assertEquals("org.example.file", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
    }

    @Test
    public void testUrlInstructionsOverrideInstructionsFile() throws Exception {
        String instructionsUrl = createInstructionsFile("Bundle-SymbolicName=org.example.file", "Bundle-Version=1.2.3");

        Attributes headers = open("wrap:" + jarUrl + "," + instructionsUrl + "$Bundle-SymbolicName=org.example.url");

        assertEquals("org.example.url", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
    }

    @Test
    public void testOverwriteMode() throws Exception {
        File bundle = tempDir.newFile("bundle.jar");
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Bundle-SymbolicName", "org.example.existing");
        manifest.getMainAttributes().putValue("Bundle-Version", "1.0.0");
        manifest.getMainAttributes().putValue("Import-Package", "org.example.other");
        Files.write(bundle.toPath(), BundleWrapperTest.createJar(manifest));
        String bundleUrl = bundle.toURI().toURL().toExternalForm();

        Attributes kept = open("wrap:" + bundleUrl + "$Bundle-Version=1.2.3");
        assertEquals("org.example.existing", kept.getValue("Bundle-SymbolicName"));
        assertEquals("1.0.0", kept.getValue("Bundle-Version"));

        Attributes merged = open("wrap:" + bundleUrl + "$Bundle-Version=1.2.3&overwrite=merge");
        assertEquals("org.example.existing", merged.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", merged.getValue("Bundle-Version"));
        assertNull(merged.getValue("overwrite"));

        Attributes full = open("wrap:" + bundleUrl + "," + createInstructionsFile("overwrite=FULL") + "$Bundle-Version=1.2.3");
        assertNotEquals("org.example.existing", full.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", full.getValue("Bundle-Version"));
    }

    @Test(expected = MalformedURLException.class)
    public void testOpenConnectionWithInvalidUrl() throws Exception {
        handler.openConnection(new URL(null, "wrap:" + jarUrl + "$Bundle-SymbolicName", protocol));
    }

    @Test(expected = IOException.class)
    public void testWrapMissingJar() throws Exception {
        open("wrap:" + new File(tempDir.getRoot(), "missing.jar").toURI().toURL());
    }

    private Attributes open(String url) throws IOException {
        try (JarInputStream jar = new JarInputStream(new URL(null, url, protocol).openStream())) {
            assertNotNull(jar.getManifest());
            return jar.getManifest().getMainAttributes();
        }
    }

    private String createInstructionsFile(String... instructions) throws IOException {
        File file = tempDir.newFile();
        Files.write(file.toPath(), Arrays.asList(instructions));
        return file.toURI().toURL().toExternalForm();
    }

}
