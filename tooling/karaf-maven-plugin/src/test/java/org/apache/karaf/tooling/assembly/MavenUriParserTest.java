package org.apache.karaf.tooling.assembly;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MavenUriParser}.
 */
public class MavenUriParserTest {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public TestResources resources = new TestResources();

    private MavenUriParser subject;

    private MavenProject mavenProject;

    private Map<String, String> translatedUris;

    private Set<Artifact> dependencyArtifacts;

    @Before
    public void setUp() throws Exception {
        subject = new MavenUriParser();
    }

    @Test
    public void getTranslatedUrisForProjectAlone() throws Exception {
        //given
        givenEmptyProject();
        //when
        final Map<String, String> result = subject.getTranslatedUris(mavenProject, translatedUris);
        //then
        final String projectKey = "mvn:org.apache/assembly-execute-mojo/0.1.0";
        assertThat(result).hasSize(1)
                          .containsOnlyKeys(projectKey);
        final String projectFile = result.get(projectKey);
        assertThat(projectFile).endsWith("/artifact-file");
    }

    private void givenEmptyProject() throws IOException {
        dependencyArtifacts = Collections.emptySet();
        mavenProject = AssemblyMother.getProject(TEST_PROJECT, resources, dependencyArtifacts);
        translatedUris = Collections.emptyMap();
    }

    @Test
    public void getTranslatedUrisWhereArtifactFileIsNull() throws Exception {
        //given
        givenEmptyProject();
        mavenProject.getArtifact()
                    .setFile(null);
        //when
        final Map<String, String> result = subject.getTranslatedUris(mavenProject, translatedUris);
        //then
        assertThat(result).hasSize(0);
    }

    @Test
    public void artifactToMvnUriForPlainValues() throws Exception {
        //given
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("extension");
        final Artifact artifact =
                new DefaultArtifact("groupId", "artifactId", "version", "scope", "type", "classifier", artifactHandler);
        //when
        final String result = subject.artifactToMvnUri(artifact);
        //then
        assertThat(result).isEqualTo("mvn:groupId/artifactId/version/extension/classifier");
    }

    @Test
    public void artifactToMvnUriForJar() throws Exception {
        //given
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        final Artifact artifact =
                new DefaultArtifact("groupId", "artifactId", "version", "scope", "jar", "", artifactHandler);
        //when
        final String result = subject.artifactToMvnUri(artifact);
        //then
        assertThat(result).isEqualTo("mvn:groupId/artifactId/version");
    }

    @Test
    public void artifactToMvnUriForBundle() throws Exception {
        //given
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("bundle");
        final Artifact artifact =
                new DefaultArtifact("groupId", "artifactId", "version", "scope", "jar", "", artifactHandler);
        //when
        final String result = subject.artifactToMvnUri(artifact);
        //then
        assertThat(result).isEqualTo("mvn:groupId/artifactId/version");
    }

    @Test
    public void artifactToMvnUriForJarWithClassifier() throws Exception {
        //given
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("jar");
        final Artifact artifact =
                new DefaultArtifact("groupId", "artifactId", "version", "scope", "jar", "classifier", artifactHandler);
        //when
        final String result = subject.artifactToMvnUri(artifact);
        //then
        assertThat(result).isEqualTo("mvn:groupId/artifactId/version/jar/classifier");
    }
    @Test
    public void artifactToMvnUriForKar() throws Exception {
        //given
        final ArtifactHandler artifactHandler = new DefaultArtifactHandler("kar");
        final Artifact artifact =
                new DefaultArtifact("groupId", "artifactId", "version", "scope", "kar", "", artifactHandler);
        //when
        final String result = subject.artifactToMvnUri(artifact);
        //then
        assertThat(result).isEqualTo("mvn:groupId/artifactId/version/kar");
    }

}
