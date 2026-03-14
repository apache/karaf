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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MavenResolverImplTest {

    private File tempDir;
    private File localRepo;

    @Before
    public void setUp() throws Exception {
        tempDir = new File(System.getProperty("java.io.tmpdir"), "karaf-url-test-" + System.nanoTime());
        tempDir.mkdirs();
        localRepo = new File(tempDir, "local-repo");
        localRepo.mkdirs();
    }

    @After
    public void tearDown() {
        deleteRecursive(tempDir);
    }

    @Test
    public void testResolveFromLocalRepository() throws Exception {
        // Create a fake artifact in the local repo
        File artifactDir = new File(localRepo, "org/apache/karaf/test-artifact/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "test-artifact-1.0.0.jar");
        createFile(artifactFile, "fake-jar-content");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:org.apache.karaf/test-artifact/1.0.0");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveFromLocalRepositoryWithoutMvnPrefix() throws Exception {
        File artifactDir = new File(localRepo, "org/apache/karaf/test-artifact/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "test-artifact-1.0.0.jar");
        createFile(artifactFile, "fake-jar-content");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("org.apache.karaf/test-artifact/1.0.0");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
    }

    @Test
    public void testResolveFromDefaultRepository() throws Exception {
        File defaultRepo = new File(tempDir, "system-repo");
        File artifactDir = new File(defaultRepo, "com/example/mylib/2.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "mylib-2.0.0.jar");
        createFile(artifactFile, "default-repo-content");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(defaultRepo.getAbsolutePath()),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/mylib/2.0.0");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveFromDefaultRepositoryWithFlags() throws Exception {
        File defaultRepo = new File(tempDir, "system-repo");
        File artifactDir = new File(defaultRepo, "com/example/mylib/2.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "mylib-2.0.0.jar");
        createFile(artifactFile, "default-repo-content");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(defaultRepo.getAbsolutePath() + "@id=system@snapshots"),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/mylib/2.0.0");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
    }

    @Test
    public void testResolveFromFileUriDefaultRepository() throws Exception {
        File defaultRepo = new File(tempDir, "system-repo");
        File artifactDir = new File(defaultRepo, "com/example/mylib/2.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "mylib-2.0.0.jar");
        createFile(artifactFile, "file-uri-content");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(defaultRepo.toURI().toString() + "@id=system"),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/mylib/2.0.0");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
    }

    @Test
    public void testDefaultRepoCheckedBeforeLocalRepo() throws Exception {
        // Create artifact in both default and local repos with different content
        File defaultRepo = new File(tempDir, "default-repo");
        File defaultArtifactDir = new File(defaultRepo, "com/example/mylib/1.0.0");
        defaultArtifactDir.mkdirs();
        File defaultArtifact = new File(defaultArtifactDir, "mylib-1.0.0.jar");
        createFile(defaultArtifact, "from-default");

        File localArtifactDir = new File(localRepo, "com/example/mylib/1.0.0");
        localArtifactDir.mkdirs();
        File localArtifact = new File(localArtifactDir, "mylib-1.0.0.jar");
        createFile(localArtifact, "from-local");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(defaultRepo.getAbsolutePath()),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/mylib/1.0.0");

        // Default repository should be checked first
        assertEquals(defaultArtifact.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testResolveNullUrlThrowsException() throws Exception {
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        resolver.resolve((String) null);
    }

    @Test(expected = IOException.class)
    public void testResolveNonExistentArtifactThrowsException() throws Exception {
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        resolver.resolve("mvn:com.nonexistent/artifact/1.0.0");
    }

    @Test
    public void testResolveByCoordinates() throws Exception {
        File artifactDir = new File(localRepo, "org/example/coords-test/3.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "coords-test-3.0.0.jar");
        createFile(artifactFile, "coords-content");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("org.example", "coords-test", "3.0.0", null, null);

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveWithType() throws Exception {
        File artifactDir = new File(localRepo, "org/example/typed/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "typed-1.0.0.xml");
        createFile(artifactFile, "<features/>");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:org.example/typed/1.0.0/xml");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveWithTypeAndClassifier() throws Exception {
        File artifactDir = new File(localRepo, "org/example/classified/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "classified-1.0.0-features.xml");
        createFile(artifactFile, "<features/>");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:org.example/classified/1.0.0/xml/features");

        assertNotNull(resolved);
        assertTrue(resolved.exists());
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testGetLocalRepository() {
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        assertEquals(localRepo, resolver.getLocalRepository());
    }

    @Test
    public void testResolveMultipleDefaultRepositories() throws Exception {
        File repo1 = new File(tempDir, "repo1");
        File repo2 = new File(tempDir, "repo2");

        // Artifact only in second repo
        File artifactDir = new File(repo2, "com/example/lib/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "lib-1.0.0.jar");
        createFile(artifactFile, "repo2-content");

        MavenConfiguration config = createConfig(
                localRepo,
                Arrays.asList(repo1.getAbsolutePath(), repo2.getAbsolutePath()),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/lib/1.0.0");

        assertNotNull(resolved);
        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testStripRepositoryFlags() {
        assertEquals("https://repo1.maven.org/maven2",
                MavenResolverImpl.stripRepositoryFlags("https://repo1.maven.org/maven2@id=central"));
        assertEquals("https://repo.apache.org",
                MavenResolverImpl.stripRepositoryFlags("https://repo.apache.org@id=apache@snapshots@noreleases"));
        assertEquals("/opt/karaf/system",
                MavenResolverImpl.stripRepositoryFlags("/opt/karaf/system@id=system@snapshots"));
        assertEquals("https://repo.example.com",
                MavenResolverImpl.stripRepositoryFlags("https://repo.example.com"));
        assertEquals("",
                MavenResolverImpl.stripRepositoryFlags("@id=test"));
    }

    // --- helpers ---

    private MavenConfiguration createConfig(File localRepo, java.util.List<String> defaultRepos, java.util.List<String> remoteRepos) {
        MavenConfiguration config = createMock(MavenConfiguration.class);
        expect(config.getLocalRepository()).andReturn(localRepo).anyTimes();
        expect(config.getDefaultRepositories()).andReturn(defaultRepos).anyTimes();
        expect(config.getRepositories()).andReturn(remoteRepos).anyTimes();
        expect(config.getChecksumPolicy()).andReturn("ignore").anyTimes();
        expect(config.getConnectionTimeout()).andReturn(5000).anyTimes();
        expect(config.getReadTimeout()).andReturn(30000).anyTimes();
        expect(config.isCertificateCheck()).andReturn(true).anyTimes();
        expect(config.getRetryCount()).andReturn(0).anyTimes();
        replay(config);
        return config;
    }

    private void createFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
        }
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

}
