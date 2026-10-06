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
package org.apache.karaf.tooling.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.MavenResolvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MojoSupportTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Test
    public void testLocalRepositoryWithoutSplit() {
        assertEquals("/repo", MojoSupport.getPaxUrlLocalRepository("/repo", Collections.emptyMap()));

        Map<String, Object> config = new HashMap<>();
        config.put("aether.enhancedLocalRepository.split", "false");
        config.put("aether.enhancedLocalRepository.splitLocal", "true");
        assertEquals("/repo", MojoSupport.getPaxUrlLocalRepository("/repo", config));
    }

    @Test
    public void testLocalRepositoryWithSplit() {
        Map<String, Object> config = new HashMap<>();
        config.put("aether.enhancedLocalRepository.split", "true");
        assertEquals("/repo@id=local@split", MojoSupport.getPaxUrlLocalRepository("/repo", config));

        config.put("aether.enhancedLocalRepository.splitLocal", true);
        config.put("aether.enhancedLocalRepository.splitRemote", "true");
        config.put("aether.enhancedLocalRepository.splitRemoteRepository", "true");
        config.put("aether.enhancedLocalRepository.splitRemoteRepositoryLast", "false");
        config.put("aether.enhancedLocalRepository.localPrefix", "mine");
        config.put("aether.enhancedLocalRepository.snapshotsPrefix", "snaps");
        assertEquals("/repo@id=local@split@splitLocal@splitRemote@splitRemoteRepository"
                        + "@splitLocalPrefix=mine@splitSnapshotsPrefix=snaps",
                MojoSupport.getPaxUrlLocalRepository("/repo", config));
    }

    /**
     * A SNAPSHOT installed by Maven in a split local repository must be resolvable by pax-url.
     */
    @Test
    public void testResolveSnapshotFromSplitLocalRepository() throws Exception {
        File repository = tmp.newFolder("repository");
        // layout of Maven 3.9 with -Daether.enhancedLocalRepository.split -Daether.enhancedLocalRepository.splitLocal
        Path dir = repository.toPath().resolve("installed/snapshots/org/example/foo/1.0-SNAPSHOT");
        Files.createDirectories(dir);
        Files.write(dir.resolve("foo-1.0-SNAPSHOT.jar"), "content".getBytes(StandardCharsets.UTF_8));
        Files.write(dir.resolve("_remote.repositories"), "foo-1.0-SNAPSHOT.jar>=\n".getBytes(StandardCharsets.UTF_8));

        // without the split options pax-url looks in the root of the repository only
        try {
            createOfflineResolver(repository.getAbsolutePath()).resolve("mvn:org.example/foo/1.0-SNAPSHOT");
            fail("Artifact in split repository should not be found without split options");
        } catch (IOException expected) {
            // expected
        }

        Map<String, Object> config = new HashMap<>();
        config.put("aether.enhancedLocalRepository.split", "true");
        config.put("aether.enhancedLocalRepository.splitLocal", "true");

        File resolved = createOfflineResolver(MojoSupport.getPaxUrlLocalRepository(repository.getAbsolutePath(), config))
                .resolve("mvn:org.example/foo/1.0-SNAPSHOT");
        assertEquals(dir.resolve("foo-1.0-SNAPSHOT.jar").toFile().getCanonicalFile(), resolved.getCanonicalFile());
    }

    private MavenResolver createOfflineResolver(String localRepository) throws IOException {
        File settings = new File(tmp.getRoot(), "settings.xml");
        Files.write(settings.toPath(), "<settings/>".getBytes(StandardCharsets.UTF_8));
        Dictionary<String, String> props = new Hashtable<>();
        props.put("org.ops4j.pax.url.mvn.localRepository", localRepository);
        props.put("org.ops4j.pax.url.mvn.settings", settings.getAbsolutePath());
        props.put("org.ops4j.pax.url.mvn.repositories", "");
        props.put("org.ops4j.pax.url.mvn.defaultRepositories", "");
        props.put("org.ops4j.pax.url.mvn.offline", "true");
        return MavenResolvers.createMavenResolver(props, "org.ops4j.pax.url.mvn");
    }

}
