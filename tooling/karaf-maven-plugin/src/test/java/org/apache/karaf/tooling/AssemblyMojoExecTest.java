package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AssemblyMojoExec}.
 */
public class AssemblyMojoExecTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    private Builder builder;

    @Test
    public void shouldExecuteMojo() throws Exception {
        //given
        final AssemblyMojo assemblyMojo = getAssemblyMojo();
        builder = new MyBuilder();
        //when
        new AssemblyMojoExec(assemblyMojo.getLog(), () -> builder).doExecute(assemblyMojo);
        //then
        // ?
    }

    private AssemblyMojo getAssemblyMojo() throws Exception {
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
        final DefaultRepositoryLayout repositoryLayout = new DefaultRepositoryLayout();
        artifactRepository.setLayout(repositoryLayout);
        mavenProject.setRemoteArtifactRepositories(Collections.singletonList(artifactRepository));
        assemblyMojo.setProject(mavenProject);

        final MavenSession mavenSession = mojoRule.newMavenSession(mavenProject);
        assemblyMojo.setMavenSession(mavenSession);

        final ArtifactRepository localRepo = new StubArtifactRepository(baseDir.getAbsolutePath());
        assemblyMojo.setLocalRepo(localRepo);

        assemblyMojo.setWorkDirectory(new File(baseDir, "assembly"));
        assemblyMojo.setSourceDirectory(new File(baseDir, "source"));
        assemblyMojo.setFramework("framework");
        assemblyMojo.setJavase("1.8");
        return assemblyMojo;
    }

    private class MyBuilder extends Builder {

        @Override
        public void generateAssembly() throws Exception {
            // do nothing
        }

    }
}
