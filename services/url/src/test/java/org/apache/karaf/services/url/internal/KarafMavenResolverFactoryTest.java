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
import java.nio.file.Files;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.features.spi.MavenResolverFactory;
import org.apache.karaf.features.spi.MavenResolvers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class KarafMavenResolverFactoryTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testFactoryIsFoundWithServiceLoader() {
        MavenResolverFactory factory = MavenResolvers.factory();

        assertTrue(factory instanceof KarafMavenResolverFactory);
        assertEquals(MavenConfiguration.PID, factory.getConfigurationPid());
    }

    @Test
    public void testCreateWithoutBundleContext() throws Exception {
        File systemRepo = tempFolder.newFolder("system");
        File artifact = createArtifact(systemRepo);

        Dictionary<String, String> configuration = new Hashtable<>();
        configuration.put(MavenConfiguration.PID + ".localRepository", tempFolder.newFolder("local").getAbsolutePath());
        configuration.put(MavenConfiguration.PID + ".defaultRepositories", systemRepo.toURI() + "@id=system.repository");

        try (MavenResolver resolver = new KarafMavenResolverFactory().create(configuration)) {
            assertEquals(artifact.getAbsolutePath(), resolver.resolve("mvn:org.example/lib/1.0.0").getAbsolutePath());
        }
    }

    @Test
    public void testCreateWithPropertyPrefix() throws Exception {
        File systemRepo = tempFolder.newFolder("system");
        File artifact = createArtifact(systemRepo);

        Dictionary<String, String> configuration = new Hashtable<>();
        configuration.put("custom.localRepository", tempFolder.newFolder("local").getAbsolutePath());
        configuration.put("custom.defaultRepositories", systemRepo.getAbsolutePath());

        try (MavenResolver resolver = new KarafMavenResolverFactory().create(configuration, "custom")) {
            assertEquals(artifact.getAbsolutePath(), resolver.resolve("org.example", "lib", null, null, "1.0.0").getAbsolutePath());
        }
    }

    private static File createArtifact(File repository) throws Exception {
        File artifact = new File(repository, "org/example/lib/1.0.0/lib-1.0.0.jar");
        artifact.getParentFile().mkdirs();
        Files.write(artifact.toPath(), "content".getBytes());
        return artifact;
    }

}
