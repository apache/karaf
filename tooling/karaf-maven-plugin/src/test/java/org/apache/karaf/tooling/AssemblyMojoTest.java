package org.apache.karaf.tooling;

import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AssemblyMojo}.
 */
public class AssemblyMojoTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Test
    @Ignore
    public void shouldExecuteMojo() throws Exception {
        //given
        final File baseDir = resources.getBasedir("assembly-execute-mojo");
        final File pom = new File(baseDir, "pom.xml");
        assertThat(pom).isNotNull();
        assertThat(pom).exists();

        final AssemblyMojo assemblyMojo = (AssemblyMojo) mojoRule.lookupMojo("assembly", pom);

        final MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(pom);
        mavenProject.setArtifact(
                new DefaultArtifact("net.kemitix", "assembly-execute-mojo", "0.1.0", "compile", "jar", "", null));
        mavenProject.setDependencyArtifacts(Collections.emptySet());
        final ArtifactRepository artifactRepository = new MavenArtifactRepository();
        //artifactRepository.setUrl("file:///home/pcampbell/.m2/repository");
        final DefaultRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();
        artifactRepository.setLayout(repositoryLayout);
        mavenProject.setRemoteArtifactRepositories(Collections.singletonList(artifactRepository));
        mojoRule.setVariableValueToObject(assemblyMojo, "project", mavenProject);

        final MavenSession mavenSession = mojoRule.newMavenSession(mavenProject);
        //mavenSession.getRequest().setOffline(true);
        mojoRule.setVariableValueToObject(assemblyMojo, "mavenSession", mavenSession);

        final ArtifactRepository localRepo = new StubArtifactRepository(baseDir.getAbsolutePath());
        mojoRule.setVariableValueToObject(assemblyMojo, "localRepo", localRepo);

        mojoRule.setVariableValueToObject(assemblyMojo, "workDirectory", new File(baseDir, "assembly"));
        mojoRule.setVariableValueToObject(assemblyMojo, "framework", "framework");
        mojoRule.setVariableValueToObject(assemblyMojo, "javase", "1.8");
        //when
        assemblyMojo.execute();
        //then
        // ?
    }

}
