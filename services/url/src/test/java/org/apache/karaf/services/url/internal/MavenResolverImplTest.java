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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.BasicAuthenticator;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import org.apache.karaf.features.spi.MavenResolver;
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
    public void testResolveFromMultiDefaultRepository() throws Exception {
        File karRepo = new File(tempDir, "kar");
        File artifactDir = new File(karRepo, "my-kar-1.0/com/example/mylib/2.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "mylib-2.0.0.jar");
        createFile(artifactFile, "kar-repo-content");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(karRepo.toURI() + "@id=kar.repository@multi@snapshots"),
                Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("mvn:com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test(expected = IOException.class)
    public void testMultiDefaultRepositoryIsNotARepositoryItself() throws Exception {
        File karRepo = new File(tempDir, "kar");
        File artifactDir = new File(karRepo, "com/example/mylib/2.0.0");
        artifactDir.mkdirs();
        createFile(new File(artifactDir, "mylib-2.0.0.jar"), "content");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(karRepo.getAbsolutePath() + "@id=kar.repository@multi"),
                Collections.emptyList());

        new MavenResolverImpl(config).resolve("mvn:com.example/mylib/2.0.0");
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

        File resolved = resolver.resolve("org.example", "coords-test", null, null, "3.0.0");

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
    public void testResolveByCoordinatesWithClassifierAndExtension() throws Exception {
        File artifactDir = new File(localRepo, "org/example/classified/1.0.0");
        artifactDir.mkdirs();
        File artifactFile = new File(artifactDir, "classified-1.0.0-features.xml");
        createFile(artifactFile, "<features/>");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        File resolved = resolver.resolve("org.example", "classified", "features", "xml", "1.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testFailedResolutionIsNotRetryable() {
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());
        MavenResolverImpl resolver = new MavenResolverImpl(config);

        assertEquals(MavenResolver.RetryChance.NEVER, resolver.isRetryableException(new IOException("not found")));
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
    public void testParseRepository() throws Exception {
        Repository repository =
                Repository.parse("https://repo1.maven.org/maven2@id=central");
        assertEquals("https://repo1.maven.org/maven2", repository.url);
        assertNull(repository.directory);
        assertNull(repository.authenticator);
        assertEquals("https://repo.apache.org",
                Repository.parse("https://repo.apache.org@id=apache@snapshots@noreleases").url);
        assertEquals("https://repo.example.com", Repository.parse("https://repo.example.com").url);

        repository = Repository.parse("/opt/karaf/system@id=system@snapshots");
        assertEquals(new File("/opt/karaf/system"), repository.directory);
        assertFalse(repository.multi);
        assertTrue(Repository.parse("file:/opt/karaf/data/kar@id=kar@multi").multi);
    }

    @Test
    public void testParseRepositoryKeepsTheAtSignsOfTheUrl() throws Exception {
        assertEquals(new File("/var/jenkins/workspace/job@2/karaf/system"), Repository.parse(
                "file:/var/jenkins/workspace/job@2/karaf/system@id=system.repository@snapshots").directory);

        Repository repository =
                Repository.parse("https://user:secret@repo.example.com/maven2@id=private");
        assertEquals("https://repo.example.com/maven2", repository.url);
        assertNotNull(repository.authenticator);
    }

    @Test
    public void testParseHandWrittenFileRepository() throws Exception {
        assertEquals(new File("/opt/my karaf/system"),
                Repository.parse("file:/opt/my karaf/system@id=system").directory);
        assertEquals(new File("system"), Repository.parse("file:system@id=system").directory);
    }

    @Test(expected = MalformedURLException.class)
    public void testParseRepositoryWithoutUrl() throws Exception {
        Repository.parse("@id=test");
    }

    @Test
    public void testMaskCredentials() {
        assertEquals("https://***@repo.example.com/maven2@id=private",
                Repository.maskCredentials("https://user:secret@repo.example.com/maven2@id=private"));
        assertEquals("https://repo.example.com/maven2@id=central",
                Repository.maskCredentials("https://repo.example.com/maven2@id=central"));
    }

    @Test
    public void testResolveFromDefaultRepositoryWithAtSignInPath() throws Exception {
        File defaultRepo = new File(tempDir, "job@2/system");
        File artifactFile = createArtifact(defaultRepo, "com/example/mylib/2.0.0/mylib-2.0.0.jar");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList(defaultRepo.toURI() + "@id=system.repository@snapshots"),
                Collections.emptyList());

        File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testInvalidDefaultRepositoryDoesNotPreventResolution() throws Exception {
        File defaultRepo = new File(tempDir, "system-repo");
        File artifactFile = createArtifact(defaultRepo, "com/example/mylib/2.0.0/mylib-2.0.0.jar");

        MavenConfiguration config = createConfig(
                localRepo,
                Arrays.asList("file:/opt/100%/system@id=invalid", defaultRepo.toURI() + "@id=system"),
                Collections.emptyList());

        File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveFromHandWrittenFileUriDefaultRepository() throws Exception {
        File defaultRepo = new File(tempDir, "my karaf/system");
        File artifactFile = createArtifact(defaultRepo, "com/example/mylib/2.0.0/mylib-2.0.0.jar");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.singletonList("file:" + defaultRepo.getAbsolutePath() + "@id=system"),
                Collections.emptyList());

        File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveFromFileRemoteRepository() throws Exception {
        File sharedRepo = new File(tempDir, "shared-repo");
        File artifactFile = createArtifact(sharedRepo, "com/example/mylib/2.0.0/mylib-2.0.0.jar");

        MavenConfiguration config = createConfig(
                localRepo,
                Collections.emptyList(),
                Collections.singletonList(sharedRepo.toURI() + "@id=shared"));

        File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testResolveFromRepositoryOfTheUrl() throws Exception {
        File repo = new File(tempDir, "url-repo");
        File artifactFile = createArtifact(repo, "com/example/mylib/2.0.0/mylib-2.0.0.jar");

        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());

        File resolved = new MavenResolverImpl(config).resolve("mvn:" + repo.toURI() + "!com.example/mylib/2.0.0");

        assertEquals(artifactFile.getAbsolutePath(), resolved.getAbsolutePath());
    }

    @Test
    public void testArtifactPathOutsideTheRepositoriesIsRejected() throws Exception {
        // mvn:a/../.. is the a/../../..-...jar path: the parent directory of the local repository
        File outside = createArtifact(tempDir, "..-...jar");
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());

        try {
            new MavenResolverImpl(config).resolve("mvn:a/../..");
            fail("Resolved " + outside + " outside of the local repository");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().startsWith("Invalid Maven artifact path"));
        }
    }

    @Test
    public void testResolveByCoordinatesKeepsTheVersionSlot() throws Exception {
        // without version, the extension must not be taken as the version (g/a/xml)
        createArtifact(localRepo, "org/example/typed/xml/typed-xml.jar");
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(), Collections.emptyList());

        try {
            new MavenResolverImpl(config).resolve("org.example", "typed", null, "xml", "");
            fail("Resolved the xml version");
        } catch (IOException e) {
            // expected
        }
    }

    @Test
    public void testDownloadWithCredentials() throws Exception {
        byte[] content = "remote-content".getBytes(StandardCharsets.UTF_8);
        try (TestServer server = new TestServer("user", "p@ss:word")) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", content);
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar.sha1", sha1(content));
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList("http://user:p%40ss%3Aword@127.0.0.1:" + server.port()
                            + "/maven2@id=private"), "fail");

            File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");

            assertEquals(new File(localRepo, "com/example/lib/1.0.0/lib-1.0.0.jar"), resolved);
            assertArrayEquals(content, Files.readAllBytes(resolved.toPath()));
            assertArrayEquals(new String[] { "lib-1.0.0.jar" }, resolved.getParentFile().list());
        }
    }

    @Test(expected = IOException.class)
    public void testDownloadWithoutCredentialsIsUnauthorized() throws Exception {
        try (TestServer server = new TestServer("user", "password")) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url()));

            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");
        }
    }

    @Test
    public void testFailChecksumPolicyRejectsADownloadWithoutChecksum() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            assertChecksumFailure(server);
        }
    }

    @Test
    public void testFailChecksumPolicyRejectsAChecksumMismatch() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar.sha1", sha1("other".getBytes(StandardCharsets.UTF_8)));
            assertChecksumFailure(server);
        }
    }

    private void assertChecksumFailure(TestServer server) throws Exception {
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                Collections.singletonList(server.url()), "fail");

        try {
            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");
            fail("Resolved an artifact which could not be verified");
        } catch (IOException e) {
            // nothing is left in the local repository, not even a temporary file
            String[] downloaded = new File(localRepo, "com/example/lib/1.0.0").list();
            assertTrue(Arrays.toString(downloaded), downloaded == null || downloaded.length == 0);
        }
    }

    @Test
    public void testServerErrorIsRetried() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            server.fail("com/example/lib/1.0.0/lib-1.0.0.jar", 503);
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url()), "ignore", null, 1, MavenSettings.EMPTY);

            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");

            assertEquals(2, server.count("com/example/lib/1.0.0/lib-1.0.0.jar"));
        }
    }

    @Test
    public void testClientErrorIsNotRetried() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            server.fail("com/example/lib/1.0.0/lib-1.0.0.jar", 403);
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=forbidden"), "ignore", null, 3, MavenSettings.EMPTY);

            try {
                new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");
                fail("Resolved a forbidden artifact");
            } catch (IOException e) {
                // the cause is in the message
                assertTrue(e.getMessage(), e.getMessage().contains("forbidden"));
                assertTrue(e.getMessage(), e.getMessage().contains("HTTP 403"));
            }
            assertEquals(1, server.count("com/example/lib/1.0.0/lib-1.0.0.jar"));
        }
    }

    @Test
    public void testUnreachableRepositoryIsNotRetried() throws Exception {
        String url;
        try (TestServer server = new TestServer(null, null)) {
            url = server.url();
        }
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                Collections.singletonList(url), "ignore", null, 3, MavenSettings.EMPTY);

        long start = System.currentTimeMillis();
        try {
            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");
            fail("Resolved an artifact from an unreachable repository");
        } catch (IOException e) {
            // no new attempt, so no delay between attempts
            assertTrue(System.currentTimeMillis() - start < 500);
            assertEquals(1, e.getSuppressed().length);
        }
    }

    @Test
    public void testSnapshotIsNotResolvedFromAReleaseRepository() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            putSnapshot(server, "20240101120000");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=releases"));

            try {
                new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT");
                fail("Resolved a snapshot from a release repository");
            } catch (IOException e) {
                assertEquals(0, server.requests.size());
            }
        }
    }

    @Test
    public void testReleaseIsNotResolvedFromASnapshotRepository() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots@noreleases"));

            try {
                new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");
                fail("Resolved a release from a snapshot repository");
            } catch (IOException e) {
                assertEquals(0, server.requests.size());
            }
        }
    }

    @Test
    public void testResolveRemoteSnapshot() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            byte[] content = putSnapshot(server, "20240101120000");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots@noreleases"));
            MavenResolverImpl resolver = new MavenResolverImpl(config);

            File resolved = resolver.resolve("mvn:com.example/lib/1.0-SNAPSHOT");

            assertEquals(new File(localRepo, "com/example/lib/1.0-SNAPSHOT/lib-1.0-SNAPSHOT.jar"), resolved);
            assertArrayEquals(content, Files.readAllBytes(resolved.toPath()));
            assertTrue(new File(resolved.getParentFile(), "maven-metadata-snapshots.xml").isFile());

            // the daily update policy: the remote repository is not checked again
            assertEquals(resolved, resolver.resolve("mvn:com.example/lib/1.0-SNAPSHOT"));
            assertEquals(1, server.count(SNAPSHOT_METADATA));
            assertEquals(1, server.count(SNAPSHOT_FILE));
        }
    }

    @Test
    public void testResolveRemoteSnapshotWithMaven2Metadata() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            byte[] content = "snapshot-content".getBytes(StandardCharsets.UTF_8);
            server.put(SNAPSHOT_FILE, content);
            server.put(SNAPSHOT_METADATA, ("<metadata><versioning><snapshot><timestamp>20240101.120000</timestamp>"
                    + "<buildNumber>3</buildNumber></snapshot><lastUpdated>20240101120000</lastUpdated></versioning>"
                    + "</metadata>").getBytes(StandardCharsets.UTF_8));
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots"));

            File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT");

            assertArrayEquals(content, Files.readAllBytes(resolved.toPath()));
        }
    }

    @Test
    public void testAlwaysUpdatePolicyChecksTheRemoteRepositoryAgain() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            putSnapshot(server, "20240101120000");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots"), "ignore", "always", 0,
                    MavenSettings.EMPTY);
            MavenResolverImpl resolver = new MavenResolverImpl(config);

            resolver.resolve("mvn:com.example/lib/1.0-SNAPSHOT");
            resolver.resolve("mvn:com.example/lib/1.0-SNAPSHOT");

            assertEquals(2, server.count(SNAPSHOT_METADATA));
            // the local copy is newer than the remote snapshot
            assertEquals(1, server.count(SNAPSHOT_FILE));
        }
    }

    @Test
    public void testNewerRemoteSnapshotReplacesTheLocalCopy() throws Exception {
        File localCopy = createArtifact(localRepo, "com/example/lib/1.0-SNAPSHOT/lib-1.0-SNAPSHOT.jar");
        localCopy.setLastModified(1000);
        try (TestServer server = new TestServer(null, null)) {
            byte[] content = putSnapshot(server, "20240101120000");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots"));

            File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT");

            assertEquals(localCopy, resolved);
            assertArrayEquals(content, Files.readAllBytes(resolved.toPath()));
        }
    }

    @Test
    public void testLocalSnapshotNewerThanTheRemoteOneIsKept() throws Exception {
        File localCopy = createArtifact(localRepo, "com/example/lib/1.0-SNAPSHOT/lib-1.0-SNAPSHOT.jar");
        try (TestServer server = new TestServer(null, null)) {
            putSnapshot(server, "20240101120000");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=snapshots@snapshots"));

            File resolved = new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT");

            assertEquals(localCopy, resolved);
            assertEquals(0, server.count(SNAPSHOT_FILE));
        }
    }

    @Test
    public void testLocalSnapshotIsUsedWhenTheRemoteRepositoryIsUnreachable() throws Exception {
        File localCopy = createArtifact(localRepo, "com/example/lib/1.0-SNAPSHOT/lib-1.0-SNAPSHOT.jar");
        String url;
        try (TestServer server = new TestServer(null, null)) {
            url = server.url();
        }
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                Collections.singletonList(url + "@id=snapshots@snapshots"));

        assertEquals(localCopy, new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT"));
    }

    @Test
    public void testResolveSnapshotDeployedInAFileRepository() throws Exception {
        File fileRepo = new File(tempDir, "file-repo");
        File deployed = createArtifact(fileRepo, "com/example/lib/1.0-SNAPSHOT/lib-1.0-20240101.120000-3.jar");
        Files.write(new File(fileRepo, SNAPSHOT_METADATA).toPath(), snapshotMetadata("20240101120000"));
        MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                Collections.singletonList(fileRepo.toURI() + "@id=file@snapshots"));

        assertEquals(deployed, new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0-SNAPSHOT"));
    }

    @Test
    public void testMirrorFromTheSettings() throws Exception {
        try (TestServer server = new TestServer(null, null)) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            MavenSettings settings = settings("<mirrors><mirror><id>mirror</id><mirrorOf>central</mirrorOf><url>"
                    + server.url() + "</url></mirror></mirrors>");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList("http://central.invalid/maven2@id=central"), "ignore", null, 0, settings);

            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");

            assertEquals(1, server.count("com/example/lib/1.0.0/lib-1.0.0.jar"));
        }
    }

    @Test
    public void testServerCredentialsFromTheSettings() throws Exception {
        try (TestServer server = new TestServer("user", "password")) {
            server.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            MavenSettings settings = settings("<servers><server><id>private</id><username>user</username>"
                    + "<password>password</password></server></servers>");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList(server.url() + "@id=private"), "ignore", null, 0, settings);

            assertTrue(new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0").isFile());
        }
    }

    @Test
    public void testProxyFromTheSettings() throws Exception {
        try (TestServer proxy = new TestServer(null, null)) {
            proxy.put("com/example/lib/1.0.0/lib-1.0.0.jar", "remote-content".getBytes(StandardCharsets.UTF_8));
            MavenSettings settings = settings("<proxies><proxy><protocol>http</protocol><host>127.0.0.1</host><port>"
                    + proxy.port() + "</port></proxy></proxies>");
            MavenConfiguration config = createConfig(localRepo, Collections.emptyList(),
                    Collections.singletonList("http://repo.invalid/maven2@id=remote"), "ignore", null, 0, settings);

            new MavenResolverImpl(config).resolve("mvn:com.example/lib/1.0.0");

            // the proxy got the request for the repository host
            assertTrue(proxy.requests.toString(),
                    proxy.requests.contains("http://repo.invalid/maven2/com/example/lib/1.0.0/lib-1.0.0.jar"));
        }
    }

    // --- helpers ---

    private static final String SNAPSHOT_METADATA = "com/example/lib/1.0-SNAPSHOT/maven-metadata.xml";
    private static final String SNAPSHOT_FILE = "com/example/lib/1.0-SNAPSHOT/lib-1.0-20240101.120000-3.jar";

    /**
     * Deploys the com.example/lib/1.0-SNAPSHOT snapshot, with Maven 3 metadata.
     */
    private static byte[] putSnapshot(TestServer server, String updated) {
        byte[] content = "snapshot-content".getBytes(StandardCharsets.UTF_8);
        server.put(SNAPSHOT_FILE, content);
        server.put(SNAPSHOT_METADATA, snapshotMetadata(updated));
        return content;
    }

    private static byte[] snapshotMetadata(String updated) {
        return ("<metadata><groupId>com.example</groupId><artifactId>lib</artifactId><version>1.0-SNAPSHOT</version>"
                + "<versioning><snapshot><timestamp>20240101.120000</timestamp><buildNumber>3</buildNumber></snapshot>"
                + "<lastUpdated>" + updated + "</lastUpdated><snapshotVersions>"
                + "<snapshotVersion><classifier>sources</classifier><extension>jar</extension>"
                + "<value>1.0-20240101.120000-2</value><updated>" + updated + "</updated></snapshotVersion>"
                + "<snapshotVersion><extension>jar</extension>"
                + "<value>1.0-20240101.120000-3</value><updated>" + updated + "</updated></snapshotVersion>"
                + "</snapshotVersions></versioning></metadata>").getBytes(StandardCharsets.UTF_8);
    }

    private MavenSettings settings(String content) throws IOException {
        File file = new File(tempDir, "settings.xml");
        Files.write(file.toPath(), ("<settings>" + content + "</settings>").getBytes(StandardCharsets.UTF_8));
        return MavenSettings.load(file);
    }

    private MavenConfiguration createConfig(File localRepo, java.util.List<String> defaultRepos, java.util.List<String> remoteRepos) {
        return createConfig(localRepo, defaultRepos, remoteRepos, "ignore");
    }

    private MavenConfiguration createConfig(File localRepo, java.util.List<String> defaultRepos, java.util.List<String> remoteRepos,
                                            String checksumPolicy) {
        return createConfig(localRepo, defaultRepos, remoteRepos, checksumPolicy, null, 0, MavenSettings.EMPTY);
    }

    private MavenConfiguration createConfig(File localRepo, java.util.List<String> defaultRepos, java.util.List<String> remoteRepos,
                                            String checksumPolicy, String updatePolicy, int retryCount,
                                            MavenSettings settings) {
        MavenConfiguration config = createMock(MavenConfiguration.class);
        expect(config.getLocalRepository()).andReturn(localRepo).anyTimes();
        expect(config.getDefaultRepositories()).andReturn(defaultRepos).anyTimes();
        expect(config.getRepositories()).andReturn(remoteRepos).anyTimes();
        expect(config.getChecksumPolicy()).andReturn(checksumPolicy).anyTimes();
        expect(config.getUpdatePolicy()).andReturn(updatePolicy).anyTimes();
        expect(config.getSettings()).andReturn(settings).anyTimes();
        expect(config.getConnectionTimeout()).andReturn(5000).anyTimes();
        expect(config.getReadTimeout()).andReturn(30000).anyTimes();
        expect(config.isCertificateCheck()).andReturn(true).anyTimes();
        expect(config.getRetryCount()).andReturn(retryCount).anyTimes();
        replay(config);
        return config;
    }

    private void createFile(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
        }
    }

    private File createArtifact(File repository, String path) throws IOException {
        File file = new File(repository, path);
        file.getParentFile().mkdirs();
        createFile(file, path);
        return file;
    }

    private static byte[] sha1(byte[] content) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (byte b : MessageDigest.getInstance("SHA-1").digest(content)) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * A Maven repository (/maven2), with the Basic authentication when a user is given. It also serves as HTTP proxy.
     */
    private static final class TestServer implements AutoCloseable {

        private final Map<String, byte[]> files = new ConcurrentHashMap<>();
        private final Map<String, Deque<Integer>> errors = new ConcurrentHashMap<>();
        /**
         * The URIs of the requests, absolute for the proxy requests.
         */
        final List<String> requests = Collections.synchronizedList(new ArrayList<>());
        private final HttpServer server;

        TestServer(String user, String password) throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            HttpContext context = server.createContext("/maven2", exchange -> {
                requests.add(exchange.getRequestURI().toString());
                String path = exchange.getRequestURI().getPath().substring("/maven2/".length());
                Deque<Integer> pathErrors = errors.get(path);
                Integer error = pathErrors != null ? pathErrors.poll() : null;
                byte[] body = files.get(path);
                if (error != null) {
                    exchange.sendResponseHeaders(error, -1);
                } else if (body == null) {
                    exchange.sendResponseHeaders(404, -1);
                } else {
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
                exchange.close();
            });
            if (user != null) {
                context.setAuthenticator(new BasicAuthenticator("maven") {
                    @Override
                    public boolean checkCredentials(String username, String pwd) {
                        return user.equals(username) && password.equals(pwd);
                    }
                });
            }
            server.start();
        }

        int port() {
            return server.getAddress().getPort();
        }

        String url() {
            return "http://127.0.0.1:" + port() + "/maven2";
        }

        void put(String path, byte[] content) {
            files.put(path, content);
        }

        /**
         * The next requests of the path get these errors.
         */
        void fail(String path, Integer... statuses) {
            errors.put(path, new ArrayDeque<>(Arrays.asList(statuses)));
        }

        int count(String path) {
            synchronized (requests) {
                return (int) requests.stream().filter(uri -> uri.endsWith("/maven2/" + path)).count();
            }
        }

        @Override
        public void close() {
            server.stop(0);
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
