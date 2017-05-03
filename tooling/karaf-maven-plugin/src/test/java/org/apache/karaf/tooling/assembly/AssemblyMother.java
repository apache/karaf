/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Paul Campbell
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies
 * or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.apache.karaf.tooling.assembly;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Object Mother for Maven objects.
 */
class AssemblyMother {

    static MavenSession getSession(
            final MavenProject mavenProject, final MojoRule mojoRule
                                  ) {
        return mojoRule.newMavenSession(mavenProject);
    }

    static MavenProject getProject(
            final String testProject, final TestResources resources,
            final Set<Artifact> dependencyArtifacts
                                  ) throws IOException {
        final File pom = new File(getProjectDirectory(testProject, resources), "pom.xml");
        final MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(pom);
        mavenProject.setArtifact(getProjectArtifact(testProject, resources));
        mavenProject.setDependencyArtifacts(dependencyArtifacts);
        mavenProject.setRemoteArtifactRepositories(getArtifactRepositories());
        return mavenProject;
    }

    private static DefaultArtifact getProjectArtifact(final String testProject, final TestResources resources)
            throws IOException {
        final String groupId = "org.apache";
        final String version = "0.1.0";
        final String compile = "compile";
        final String type = "jar";
        final String classifier = "";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact defaultArtifact =
                new DefaultArtifact(groupId, testProject, version, compile, type, classifier, artifactHandler);
        defaultArtifact.setFile(new File(resources.getBasedir(testProject), "artifact-file"));
        return defaultArtifact;
    }

    private static List<ArtifactRepository> getArtifactRepositories() {
        final ArtifactRepository artifactRepository = createArtifactRepository();
        return Collections.singletonList(artifactRepository);
    }

    static ArtifactRepository createArtifactRepository() {
        final ArtifactRepository artifactRepository = new MavenArtifactRepository();
        artifactRepository.setId("default-id");
        artifactRepository.setUrl("default-url");
        artifactRepository.setLayout(new DefaultRepositoryLayout());
        final ArtifactRepositoryPolicy enabledPolicy = new ArtifactRepositoryPolicy(true, "", "");
        final ArtifactRepositoryPolicy disabledPolicy = new ArtifactRepositoryPolicy(false, "", "");
        artifactRepository.setReleaseUpdatePolicy(disabledPolicy);
        artifactRepository.setSnapshotUpdatePolicy(enabledPolicy);
        return artifactRepository;
    }

    static File getProjectDirectory(final String testProject, final TestResources resources) throws IOException {
        return resources.getBasedir(testProject);
    }
}
