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
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.features.spi.MavenResolver;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

/**
 * Cases of {@link ReactorMavenResolver} that the real local repository manager cannot easily produce. The split
 * repository layouts are covered by {@link ReactorMavenResolverLocalRepositoryTest}.
 */
public class ReactorMavenResolverTest {

    private static final String URL = "mvn:org.example/foo/1.0-SNAPSHOT";

    private static final List<RemoteRepository> REPOSITORIES = Collections.singletonList(
            new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2").build());

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private File fallbackFile;
    private int fallbackCalls;
    private final List<LocalArtifactRequest> requests = new ArrayList<>();

    private final MavenResolver fallback = new MavenResolver() {
        @Override
        public File resolve(String url) {
            fallbackCalls++;
            return fallbackFile;
        }

        @Override
        public File resolve(String url, Exception previousException) {
            fallbackCalls++;
            return fallbackFile;
        }

        @Override
        public File resolve(String groupId, String artifactId, String classifier, String extension,
                            String version) {
            fallbackCalls++;
            return fallbackFile;
        }
    };

    @Before
    public void setUp() throws IOException {
        fallbackFile = tmp.newFile("fallback.jar");
    }

    /**
     * All three resolve methods look up the local repository with the project repositories and context.
     */
    @Test
    public void testResolveInstalledArtifact() throws Exception {
        File installed = file("installed/foo-1.0-SNAPSHOT.jar");
        ReactorMavenResolver resolver = resolver(null, manager("enhanced", installed, true, null));

        assertEquals(installed, resolver.resolve(URL));
        assertEquals(installed, resolver.resolve(URL, new IOException()));
        assertEquals(installed, resolver.resolve("org.example", "foo", "", "jar", "1.0-SNAPSHOT"));
        assertEquals(0, fallbackCalls);

        LocalArtifactRequest request = requests.get(0);
        assertEquals("org.example:foo:jar:1.0-SNAPSHOT", request.getArtifact().toString());
        assertEquals(REPOSITORIES, request.getRepositories());
        assertEquals("project", request.getContext());
    }

    @Test
    public void testFallbackWhenNotAvailable() throws Exception {
        File cached = file("cached/foo-1.0.jar");
        ReactorMavenResolver resolver = resolver(null, manager("enhanced", cached, false, REPOSITORIES.get(0)));

        assertSame(fallbackFile, resolver.resolve("mvn:org.example/foo/1.0"));
        assertEquals(1, fallbackCalls);
    }

    @Test
    public void testFallbackWhenFileIsMissing() throws Exception {
        File missing = new File(tmp.getRoot(), "missing/foo-1.0-SNAPSHOT.jar");
        ReactorMavenResolver resolver = resolver(null, manager("enhanced", missing, true, null));

        assertSame(fallbackFile, resolver.resolve(URL));
        assertEquals(1, fallbackCalls);
    }

    /**
     * The simple local repository (mvn -llr) says that every file is available, whatever its origin. It only has the
     * default layout, which the fallback resolver reads directly.
     */
    @Test
    public void testFallbackForSimpleLocalRepository() throws Exception {
        File file = file("org/example/foo/1.0/foo-1.0.jar");
        ReactorMavenResolver resolver = resolver(null, manager("simple", file, true, null));

        assertSame(fallbackFile, resolver.resolve("mvn:org.example/foo/1.0"));
        assertEquals(0, requests.size());
        assertEquals(1, fallbackCalls);
    }

    @Test
    public void testFallbackForUrlWithRepository() throws Exception {
        File installed = file("installed/foo-1.0.jar");
        ReactorMavenResolver resolver = resolver(null, manager("enhanced", installed, true, null));

        assertSame(fallbackFile, resolver.resolve("mvn:https://repo.example.org/maven2!org.example/foo/1.0"));
        assertEquals(0, requests.size());
        assertEquals(1, fallbackCalls);
    }

    @Test
    public void testReactorBeforeLocalRepository() throws Exception {
        File inReactor = file("reactor/foo-1.0-SNAPSHOT.jar");
        File installed = file("installed/foo-1.0-SNAPSHOT.jar");
        ReactorMavenResolver resolver = resolver(reactor(inReactor), manager("enhanced", installed, true, null));

        assertEquals(inReactor, resolver.resolve(URL));
        assertEquals(0, requests.size());
        assertEquals(0, fallbackCalls);
    }

    @Test
    public void testWithoutSession() throws Exception {
        ReactorMavenResolver resolver = new ReactorMavenResolver(reactor(null), null, null, fallback);

        assertSame(fallbackFile, resolver.resolve(URL));
        assertEquals(1, fallbackCalls);
    }

    private File file(String path) throws IOException {
        File file = new File(tmp.getRoot(), path);
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(), new byte[] { 1 });
        return file;
    }

    private ReactorMavenResolver resolver(WorkspaceReader reactor, LocalRepositoryManager manager) {
        DefaultRepositorySystemSession session = new DefaultRepositorySystemSession(h -> false);
        session.setLocalRepositoryManager(manager);
        return new ReactorMavenResolver(reactor, session, REPOSITORIES, fallback);
    }

    /**
     * A local repository manager that records the requests and returns the given result.
     *
     * @param repository the repository the artifact was cached from, {@code null} for an installed artifact
     */
    private LocalRepositoryManager manager(String contentType, File file, boolean available,
                                           RemoteRepository repository) {
        return (LocalRepositoryManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[] { LocalRepositoryManager.class }, (proxy, method, args) -> {
                    if (method.getName().equals("getRepository")) {
                        return new LocalRepository(tmp.getRoot(), contentType);
                    }
                    if (method.getName().equals("find") && args[1] instanceof LocalArtifactRequest) {
                        LocalArtifactRequest request = (LocalArtifactRequest) args[1];
                        requests.add(request);
                        return new LocalArtifactResult(request).setPath(file.toPath())
                                .setAvailable(available).setRepository(repository);
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

    private static WorkspaceReader reactor(File file) {
        return (WorkspaceReader) Proxy.newProxyInstance(ReactorMavenResolverTest.class.getClassLoader(),
                new Class<?>[] { WorkspaceReader.class }, (proxy, method, args) -> {
                    if (method.getName().equals("findArtifact")) {
                        return file;
                    }
                    throw new UnsupportedOperationException(method.toString());
                });
    }

}
