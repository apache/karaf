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
package org.apache.karaf.tooling.utils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.List;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.util.maven.Parser;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalArtifactRequest;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.WorkspaceReader;

/**
 * {@link MavenResolver} that looks up artifacts inside the Maven reactor first, then in the local repository of the
 * running Maven session, and only then delegates to the fallback resolver.
 * <p>
 * The local repository lookup goes through the {@link LocalRepositoryManager} of the Maven session, so it finds the
 * artifacts whatever local repository layout Maven uses (for example a split local repository, see KARAF-7667).
 * It is only done with the enhanced local repository (the Maven default), which records where each artifact comes
 * from. It applies to every layout of that repository, not only to the split one. The following artifacts are taken
 * from there:
 * <ul>
 *     <li>artifacts installed by a local build, like the reactor does for the modules of the current build;</li>
 *     <li>release artifacts cached from one of the project repositories;</li>
 *     <li>SNAPSHOT artifacts cached from one of the project repositories, when Maven runs offline.</li>
 * </ul>
 * Everything else (a cached SNAPSHOT when online, a version range or the LATEST and RELEASE keywords, which have no
 * file of their own in the local repository, or a URL that names its own repository) is left to the fallback resolver, which applies its own repositories and update policy.
 */
public class ReactorMavenResolver implements MavenResolver {

    /**
     * Request context that Maven uses for the dependencies of a project. The enhanced local repository only uses the
     * context for a repository manager. An artifact cached there in another context (for example "plugin") is not
     * found, so it goes to the fallback resolver, as before.
     */
    private static final String PROJECT_CONTEXT = "project";

    /**
     * Content type of the Maven Resolver local repository that tracks where each artifact comes from.
     */
    private static final String ENHANCED = "enhanced";

    private final WorkspaceReader reactor;
    private final RepositorySystemSession session;
    private final List<RemoteRepository> repositories;
    private final MavenResolver fallback;

    /**
     * @param reactor      the Maven reactor, may be {@code null}
     * @param session      the repository session whose local repository is used, may be {@code null}
     * @param repositories the remote repositories of the project; a cached artifact is only used when it was
     *                     downloaded from one of them
     * @param fallback     the resolver used when the artifact is neither in the reactor nor in the local repository
     */
    public ReactorMavenResolver(WorkspaceReader reactor, RepositorySystemSession session,
                                List<RemoteRepository> repositories, MavenResolver fallback) {
        this.reactor = reactor;
        this.session = session;
        this.repositories = repositories != null ? repositories : Collections.emptyList();
        this.fallback = fallback;
    }

    private static Parser parse(String url) throws MalformedURLException {
        return new Parser(url.startsWith("mvn:") ? url.substring(4) : url);
    }

    private static Artifact toArtifact(Parser parser) {
        return new DefaultArtifact(parser.getGroup(), parser.getArtifact(), parser.getClassifier(),
                parser.getType(), parser.getVersion());
    }

    /**
     * Returns the file of the artifact from the reactor or, when {@code useLocalRepository} is set, from the local
     * repository, or {@code null}.
     */
    private File find(Artifact artifact, boolean useLocalRepository) {
        if (reactor != null) {
            File file = reactor.findArtifact(artifact);
            if (file != null) {
                return file;
            }
        }
        if (!useLocalRepository || session == null) {
            return null;
        }
        LocalRepositoryManager manager = session.getLocalRepositoryManager();
        // The simple local repository (mvn -llr) says that every file is available, whatever its origin. It only has
        // the default layout, which the fallback resolver reads directly.
        if (manager == null || !ENHANCED.equals(manager.getRepository().getContentType())) {
            return null;
        }
        LocalArtifactResult result =
                manager.find(session, new LocalArtifactRequest(artifact, repositories, PROJECT_CONTEXT));
        if (!result.isAvailable() || result.getFile() == null || !result.getFile().isFile()) {
            return null;
        }
        if (!artifact.isSnapshot() || session.isOffline()) {
            return result.getFile();
        }
        // The enhanced local repository records installed artifacts in _remote.repositories with an empty repository
        // id, and then returns a result without a repository. A SNAPSHOT downloaded from a remote repository is left
        // to the fallback resolver, which applies its update policy.
        return result.getRepository() == null ? result.getFile() : null;
    }

    /**
     * A URL that names its own repository is left to the fallback resolver after the reactor.
     */
    private File find(String url) throws MalformedURLException {
        Parser parser = parse(url);
        return find(toArtifact(parser), parser.getRepositoryURL() == null);
    }

    @Override
    public File resolve(String url) throws IOException {
        File file = find(url);
        return file == null ? fallback.resolve(url) : file;
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        File file = find(url);
        return file == null ? fallback.resolve(url, previousException) : file;
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        File file = find(new DefaultArtifact(groupId, artifactId, classifier, extension, version), true);
        return file == null ? fallback.resolve(groupId, artifactId, classifier, extension, version) : file;
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        return fallback.isRetryableException(exception);
    }

    @Override
    public void close() throws IOException {
        fallback.close();
    }

}
