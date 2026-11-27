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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.features.spi.MavenResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.internal.impl.DefaultLocalPathComposer;
import org.eclipse.aether.internal.impl.DefaultLocalPathPrefixComposerFactory;
import org.eclipse.aether.internal.impl.DefaultRepositoryKeyFunctionFactory;
import org.eclipse.aether.internal.impl.EnhancedLocalRepositoryManagerFactory;
import org.eclipse.aether.internal.impl.LegacyTrackingFileManager;
import org.eclipse.aether.repository.LocalArtifactRegistration;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Checks {@link ReactorMavenResolver} against the real Maven Resolver enhanced local repository manager, with the
 * split local repository layouts of KARAF-7667.
 */
public class ReactorMavenResolverLocalRepositoryTest {

    private static final RemoteRepository CENTRAL =
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build();

    private static final List<RemoteRepository> REPOSITORIES = Collections.singletonList(CENTRAL);

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final FailingFallback fallback = new FailingFallback();

    /**
     * Maven Resolver 2.x (Maven 3.10, Maven 4) property names.
     */
    @Test
    public void testSplitRepository() throws Exception {
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.");
        File installed = install(session);

        assertTrue(installed.getPath(), installed.getPath().replace('\\', '/')
                .contains("/installed/snapshots/org/example/foo/1.0-SNAPSHOT/"));
        assertEquals(installed, resolver(session).resolve("mvn:org.example/foo/1.0-SNAPSHOT"));
    }

    /**
     * Maven Resolver 1.9 (Maven 3.9) property names, still read by Maven Resolver 2.x.
     */
    @Test
    public void testSplitRepositoryWithLegacyNames() throws Exception {
        DefaultRepositorySystemSession session = session("aether.enhancedLocalRepository.");
        File installed = install(session);

        assertTrue(installed.getPath(), installed.getPath().replace('\\', '/')
                .contains("/installed/snapshots/org/example/foo/1.0-SNAPSHOT/"));
        assertEquals(installed, resolver(session).resolve("mvn:org.example/foo/1.0-SNAPSHOT"));
    }

    @Test
    public void testSplitRepositoryWithPrefixes() throws Exception {
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.",
                "localPrefix", "mine", "snapshotsPrefix", "snaps");
        File installed = install(session);

        assertTrue(installed.getPath(), installed.getPath().replace('\\', '/')
                .contains("/mine/snaps/org/example/foo/1.0-SNAPSHOT/"));
        assertEquals(installed, resolver(session).resolve("mvn:org.example/foo/1.0-SNAPSHOT"));
    }

    @Test
    public void testCachedReleaseInSplitRepository() throws Exception {
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.");
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        Artifact artifact = new DefaultArtifact("org.example", "bar", "jar", "1.0");
        Path path = lrm.getAbsolutePathForRemoteArtifact(artifact, CENTRAL, "");
        write(path);
        lrm.add(session, new LocalArtifactRegistration(artifact, CENTRAL, Collections.singleton("")));

        assertTrue(path.toString(), path.toString().replace('\\', '/').contains("/cached/"));
        assertEquals(path.toFile(), resolver(session).resolve("mvn:org.example/bar/1.0"));
    }

    /**
     * Version ranges and the LATEST and RELEASE keywords have no file of their own in the local repository, so they
     * are left to the fallback resolver.
     */
    @Test
    public void testVersionRangesAndKeywords() throws Exception {
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.");
        install(session);
        ReactorMavenResolver resolver = resolver(session);

        for (String url : new String[] { "mvn:org.example/foo/[1.0,2.0)", "mvn:org.example/foo/LATEST",
                "mvn:org.example/foo/RELEASE" }) {
            try {
                resolver.resolve(url);
                fail(url + " should be left to the fallback resolver");
            } catch (IOException expected) {
                // expected
            }
        }
        assertEquals(3, fallback.calls);
    }

    /**
     * A SNAPSHOT that Maven downloaded is only taken from the split repository when Maven runs offline.
     */
    @Test
    public void testCachedSnapshotInSplitRepository() throws Exception {
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.");
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        Artifact artifact = new DefaultArtifact("org.example", "baz", "jar", "1.0-SNAPSHOT");
        Path path = lrm.getAbsolutePathForRemoteArtifact(artifact, CENTRAL, "");
        write(path);
        lrm.add(session, new LocalArtifactRegistration(artifact, CENTRAL, Collections.singleton("")));

        try {
            resolver(session).resolve("mvn:org.example/baz/1.0-SNAPSHOT");
            fail("A cached SNAPSHOT should be left to the fallback resolver when online");
        } catch (IOException expected) {
            // expected
        }
        assertEquals(1, fallback.calls);

        session.setOffline(true);
        assertEquals(path.toFile(), resolver(session).resolve("mvn:org.example/baz/1.0-SNAPSHOT"));
    }

    /**
     * For a repository manager, Maven Resolver tracks the request context, and Maven uses "project" for the
     * dependencies of a project.
     */
    @Test
    public void testCachedReleaseFromRepositoryManager() throws Exception {
        RemoteRepository mirror = new RemoteRepository.Builder("mirror", "default", "https://repo.example.org/maven2")
                .setRepositoryManager(true).build();
        DefaultRepositorySystemSession session = session("aether.lrm.enhanced.");
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        Artifact artifact = new DefaultArtifact("org.example", "bar", "jar", "1.0");
        Path path = lrm.getAbsolutePathForRemoteArtifact(artifact, mirror, "project");
        write(path);
        lrm.add(session, new LocalArtifactRegistration(artifact, mirror, Collections.singleton("project")));

        ReactorMavenResolver resolver = new ReactorMavenResolver(null, session, Collections.singletonList(mirror),
                fallback);
        assertEquals(path.toFile(), resolver.resolve("mvn:org.example/bar/1.0"));
    }

    /**
     * @param prefix     the prefix of the split repository property names
     * @param properties more property names (without the prefix) and values, set before the manager is created
     */
    private DefaultRepositorySystemSession session(String prefix, String... properties) throws Exception {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false);
        session.setConfigProperty(prefix + "split", "true");
        session.setConfigProperty(prefix + "splitLocal", "true");
        for (int i = 0; i < properties.length; i += 2) {
            session.setConfigProperty(prefix + properties[i], properties[i + 1]);
        }
        EnhancedLocalRepositoryManagerFactory factory = new EnhancedLocalRepositoryManagerFactory(
                new DefaultLocalPathComposer(), new LegacyTrackingFileManager(),
                new DefaultLocalPathPrefixComposerFactory(new DefaultRepositoryKeyFunctionFactory()),
                new DefaultRepositoryKeyFunctionFactory());
        session.setLocalRepositoryManager(factory.newInstance(session, new LocalRepository(tmp.newFolder("repo"))));
        return session;
    }

    /**
     * Installs org.example:foo:1.0-SNAPSHOT the way the Maven install goal does.
     */
    private File install(DefaultRepositorySystemSession session) throws IOException {
        LocalRepositoryManager lrm = session.getLocalRepositoryManager();
        Artifact artifact = new DefaultArtifact("org.example", "foo", "jar", "1.0-SNAPSHOT");
        Path path = lrm.getAbsolutePathForLocalArtifact(artifact);
        write(path);
        lrm.add(session, new LocalArtifactRegistration(artifact));
        return path.toFile();
    }

    private static void write(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, new byte[] { 1 });
    }

    private ReactorMavenResolver resolver(DefaultRepositorySystemSession session) {
        return new ReactorMavenResolver(null, session, REPOSITORIES, fallback);
    }

    private static class FailingFallback implements MavenResolver {
        private int calls;

        @Override
        public File resolve(String url) throws IOException {
            calls++;
            throw new IOException("Not found in the local repository: " + url);
        }
    }

}
