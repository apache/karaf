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
package org.apache.karaf.main.util;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UtilsTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void firstStartCreatesMarkerAndKeepsCache() throws Exception {
        File data = temporaryFolder.newFolder("data");
        File cache = new File(data, "cache");
        Files.createDirectories(cache.toPath());
        Path bundleFile = cache.toPath().resolve("bundle0");
        Files.write(bundleFile, new byte[0]);

        String originalVersion = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "17");
            Utils.cleanCacheOnJdkChange(data, cache);

            assertTrue("cache content must be preserved on first start", Files.exists(bundleFile));
            File marker = new File(data, Utils.JDK_VERSION_MARKER_FILE);
            assertTrue(marker.exists());
            assertEquals("17", new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim());
        } finally {
            restore(originalVersion);
        }
    }

    @Test
    public void sameJdkVersionKeepsCache() throws Exception {
        File data = temporaryFolder.newFolder("data");
        File cache = new File(data, "cache");
        Files.createDirectories(cache.toPath());
        Path bundleFile = cache.toPath().resolve("bundle0");
        Files.write(bundleFile, new byte[0]);

        String originalVersion = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "17");
            Utils.cleanCacheOnJdkChange(data, cache);
            // started again with the same JDK version
            Utils.cleanCacheOnJdkChange(data, cache);

            assertTrue("cache content must be preserved across restarts with the same JDK", Files.exists(bundleFile));
        } finally {
            restore(originalVersion);
        }
    }

    @Test
    public void jdkVersionChangeCleansCache() throws Exception {
        File data = temporaryFolder.newFolder("data");
        File cache = new File(data, "cache");
        Files.createDirectories(cache.toPath());
        Path bundleFile = cache.toPath().resolve("bundle0");
        Files.write(bundleFile, new byte[0]);

        String originalVersion = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", "11");
            Utils.cleanCacheOnJdkChange(data, cache);
            assertTrue(Files.exists(bundleFile));

            // restart under a different major JDK version
            System.setProperty("java.specification.version", "17");
            Utils.cleanCacheOnJdkChange(data, cache);

            assertFalse("stale cache from the previous JDK must be cleaned", Files.exists(bundleFile));
            assertTrue("cache directory must be recreated", cache.exists());
            File marker = new File(data, Utils.JDK_VERSION_MARKER_FILE);
            assertEquals("17", new String(Files.readAllBytes(marker.toPath()), StandardCharsets.UTF_8).trim());
        } finally {
            restore(originalVersion);
        }
    }

    private void restore(String originalVersion) {
        if (originalVersion == null) {
            System.clearProperty("java.specification.version");
        } else {
            System.setProperty("java.specification.version", originalVersion);
        }
    }

}
