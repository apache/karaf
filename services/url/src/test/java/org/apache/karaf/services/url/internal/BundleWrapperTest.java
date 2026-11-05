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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.junit.Test;

import static org.junit.Assert.*;

public class BundleWrapperTest {

    private static final String JAR_INFO = "wrap:mvn:org.example/test/1.0.0";

    private static final String CLASS_RESOURCE = OverwriteMode.class.getName().replace('.', '/') + ".class";

    private static final String CLASS_PACKAGE = OverwriteMode.class.getPackage().getName();

    @Test
    public void testWrapPlainJar() throws Exception {
        Attributes headers = wrap(createJar(new Manifest()), new Properties(), OverwriteMode.KEEP);

        assertEquals("2", headers.getValue("Bundle-ManifestVersion"));
        assertEquals("wrap_mvn_org.example_test_1.0.0", headers.getValue("Bundle-SymbolicName"));
        assertTrue(headers.getValue("Export-Package").contains(CLASS_PACKAGE));
    }

    @Test
    public void testWrapJarWithoutManifest() throws Exception {
        Attributes headers = wrap(createJar(null), new Properties(), OverwriteMode.KEEP);

        assertEquals("wrap_mvn_org.example_test_1.0.0", headers.getValue("Bundle-SymbolicName"));
        assertTrue(headers.getValue("Export-Package").contains(CLASS_PACKAGE));
    }

    @Test
    public void testImportsAreOptionalByDefault() throws Exception {
        Properties instructions = new Properties();
        // do not export the package, so that the imports only contain the referred packages
        instructions.setProperty("Export-Package", "!*");
        instructions.setProperty("Private-Package", "*");
        byte[] jar = createJar(new Manifest(), BundleWrapperTest.class);

        Attributes headers = wrap(jar, instructions, OverwriteMode.KEEP);

        assertNull(headers.getValue("Export-Package"));
        assertTrue(headers.getValue("Import-Package"), headers.getValue("Import-Package").contains("org.junit;resolution:=optional"));
    }

    @Test
    public void testWrapWithInstructions() throws Exception {
        Properties instructions = new Properties();
        instructions.setProperty("Bundle-SymbolicName", "org.example.test;singleton:=true");
        instructions.setProperty("Bundle-Version", "1.2.3");
        instructions.setProperty("Export-Package", CLASS_PACKAGE + ";version=\"4.5.6\"");
        instructions.setProperty("Custom-Header", "custom");

        Attributes headers = wrap(createJar(new Manifest()), instructions, OverwriteMode.KEEP);

        assertEquals("org.example.test;singleton:=true", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
        assertEquals(CLASS_PACKAGE + ";version=\"4.5.6\"", headers.getValue("Export-Package"));
        assertEquals("custom", headers.getValue("Custom-Header"));
    }

    @Test
    public void testKeepExistingBundle() throws Exception {
        Properties instructions = new Properties();
        instructions.setProperty("Bundle-SymbolicName", "org.example.wrapped");
        instructions.setProperty("Bundle-Version", "1.2.3");

        Attributes headers = wrap(createJar(bundleManifest()), instructions, OverwriteMode.KEEP);

        assertEquals("org.example.existing", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.0.0", headers.getValue("Bundle-Version"));
        assertEquals("org.example.existing.api", headers.getValue("Export-Package"));
    }

    @Test
    public void testMergeExistingBundle() throws Exception {
        Properties instructions = new Properties();
        instructions.setProperty("Bundle-Version", "1.2.3");

        Attributes headers = wrap(createJar(bundleManifest()), instructions, OverwriteMode.MERGE);

        assertEquals("org.example.existing", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
    }

    @Test
    public void testFullOverwriteExistingBundle() throws Exception {
        Properties instructions = new Properties();
        instructions.setProperty("Bundle-Version", "1.2.3");

        Attributes headers = wrap(createJar(bundleManifest()), instructions, OverwriteMode.FULL);

        assertEquals("wrap_mvn_org.example_test_1.0.0", headers.getValue("Bundle-SymbolicName"));
        assertEquals("1.2.3", headers.getValue("Bundle-Version"));
        assertTrue(headers.getValue("Export-Package").contains(CLASS_PACKAGE));
    }

    @Test
    public void testWrappedJarKeepsContent() throws Exception {
        byte[] jar = createJar(new Manifest());

        try (JarInputStream wrapped = new JarInputStream(BundleWrapper.wrap(new ByteArrayInputStream(jar),
                new Properties(), JAR_INFO, OverwriteMode.KEEP))) {
            List<String> files = new ArrayList<>();
            for (JarEntry entry; (entry = wrapped.getNextJarEntry()) != null;) {
                if (!entry.isDirectory()) {
                    files.add(entry.getName());
                    assertArrayEquals(readClass(OverwriteMode.class), wrapped.readAllBytes());
                }
            }
            assertEquals(Collections.singletonList(CLASS_RESOURCE), files);
        }
    }

    @Test(expected = IOException.class)
    public void testWrapInvalidJar() throws Exception {
        BundleWrapper.wrap(new ByteArrayInputStream("not-a-jar".getBytes()), new Properties(), JAR_INFO, OverwriteMode.KEEP);
    }

    @Test
    public void testToSymbolicName() {
        assertEquals("wrap_file__repo_test-1.0.jar", BundleWrapper.toSymbolicName("wrap:file:/repo/test-1.0.jar"));
        assertEquals("org.example.test", BundleWrapper.toSymbolicName("org.example.test"));
        assertEquals("org.example_test;singleton:=true", BundleWrapper.toSymbolicName("org.example test;singleton:=true"));
    }

    private static Attributes wrap(byte[] jar, Properties instructions, OverwriteMode overwriteMode) throws IOException {
        try (JarInputStream wrapped = new JarInputStream(BundleWrapper.wrap(new ByteArrayInputStream(jar),
                instructions, JAR_INFO, overwriteMode))) {
            assertNotNull(wrapped.getManifest());
            return wrapped.getManifest().getMainAttributes();
        }
    }

    private static Manifest bundleManifest() {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Bundle-ManifestVersion", "2");
        manifest.getMainAttributes().putValue("Bundle-SymbolicName", "org.example.existing");
        manifest.getMainAttributes().putValue("Bundle-Version", "1.0.0");
        manifest.getMainAttributes().putValue("Export-Package", "org.example.existing.api");
        return manifest;
    }

    static byte[] createJar(Manifest manifest) throws IOException {
        return createJar(manifest, OverwriteMode.class);
    }

    private static byte[] createJar(Manifest manifest, Class<?> clazz) throws IOException {
        if (manifest != null) {
            manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (JarOutputStream jar = manifest != null ? new JarOutputStream(out, manifest) : new JarOutputStream(out)) {
            jar.putNextEntry(new JarEntry(clazz.getName().replace('.', '/') + ".class"));
            jar.write(readClass(clazz));
            jar.closeEntry();
        }
        return out.toByteArray();
    }

    private static byte[] readClass(Class<?> clazz) throws IOException {
        try (InputStream is = clazz.getResourceAsStream(clazz.getSimpleName() + ".class")) {
            return is.readAllBytes();
        }
    }

}
