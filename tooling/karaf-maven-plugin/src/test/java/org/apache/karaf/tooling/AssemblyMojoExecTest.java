package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.doNothing;

/**
 * Tests for {@link AssemblyMojoExec}.
 */
public class AssemblyMojoExecTest {

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Spy
    private Builder builder = Builder.newInstance();

    private AssemblyMojo assemblyMojo;

    private AssemblyMojoExec assemblyMojoExec;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        assemblyMojo = getAssemblyMojo();
        doNothing().when(builder)
                   .generateAssembly();
        assemblyMojoExec = new AssemblyMojoExec(assemblyMojo.getLog(), () -> builder);
    }

    @Test
    public void shouldExecuteMojo() throws Exception {
        //given
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        // ?
    }

    private AssemblyMojo getAssemblyMojo() throws Exception {
        final File baseDir = resources.getBasedir("assembly-execute-mojo");
        final File pom = new File(baseDir, "pom.xml");
        final MavenProject mavenProject = getMavenProject(pom);

        final AssemblyMojo assemblyMojo = (AssemblyMojo) mojoRule.lookupMojo("assembly", pom);
        assemblyMojo.setProject(mavenProject);
        assemblyMojo.setMavenSession(getMavenSession(mavenProject));
        assemblyMojo.setLocalRepo(getLocalRepository(baseDir));
        assemblyMojo.setWorkDirectory(new File(baseDir, "assembly"));
        assemblyMojo.setSourceDirectory(new File(baseDir, "source"));
        assemblyMojo.setFramework("framework");
        assemblyMojo.setJavase("1.8");
        return assemblyMojo;
    }

    private MavenSession getMavenSession(final MavenProject mavenProject) {
        return mojoRule.newMavenSession(mavenProject);
    }

    private ArtifactRepository getLocalRepository(final File baseDir) {
        return new StubArtifactRepository(baseDir.getAbsolutePath());
    }

    private MavenProject getMavenProject(final File pom) {
        final MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(pom);
        mavenProject.setArtifact(getProjectArtifact());
        mavenProject.setDependencyArtifacts(Collections.emptySet());
        mavenProject.setRemoteArtifactRepositories(getArtifactRepositories());
        return mavenProject;
    }

    private DefaultArtifact getProjectArtifact() {
        final String groupId = "net.kemitix";
        final String artifactId = "assembly-execute-mojo";
        final String version = "0.1.0";
        final String compile = "compile";
        final String jar = "jar";
        final String classifier = "";
        final ArtifactHandler artifactHandler = null;
        return new DefaultArtifact(groupId, artifactId, version, compile, jar, classifier, artifactHandler);
    }

    private List<ArtifactRepository> getArtifactRepositories() {
        final ArtifactRepository artifactRepository = new MavenArtifactRepository();
        artifactRepository.setLayout(new DefaultRepositoryLayout());
        return Collections.singletonList(artifactRepository);
    }

}
