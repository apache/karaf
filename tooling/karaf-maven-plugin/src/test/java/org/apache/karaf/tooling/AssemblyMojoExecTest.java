package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Set<Artifact> dependencyArtifacts;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(builder)
                   .generateAssembly();
        dependencyArtifacts = new HashSet<>();
        assemblyMojo = getAssemblyMojo();
        assemblyMojoExec = new AssemblyMojoExec(assemblyMojo.getLog(), () -> builder);
        final Map<String, String> config = new HashMap<>();
        assemblyMojo.setConfig(config);
        final Map<String, String> system = new HashMap<>();
        assemblyMojo.setSystem(system);
    }

    @Test
    public void shouldExecuteMojo() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", ""));
        dependencyArtifacts.add(getDependency("runtime", "jar", ""));
        dependencyArtifacts.add(getDependency("provided", "jar", ""));
        dependencyArtifacts.add(getDependency("other", "jar", ""));
        dependencyArtifacts.add(getDependency("compile", "bundle", ""));
        dependencyArtifacts.add(getDependency("compile", "other", ""));

        dependencyArtifacts.add(getDependency("compile", "kar", ""));
        dependencyArtifacts.add(getDependency("runtime", "kar", ""));
        dependencyArtifacts.add(getDependency("provided", "kar", ""));

        dependencyArtifacts.add(getDependency("compile", "jar", "features"));
        dependencyArtifacts.add(getDependency("compile", "jar", "karaf"));
        dependencyArtifacts.add(getDependency("runtime", "jar", "karaf"));
        dependencyArtifacts.add(getDependency("provided", "jar", "karaf"));
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        // ?
    }

    private DefaultArtifact getDependency(final String scope, final String type, final String classifier) {
        final String groupId = "net.kemitix";
        final String artifactId = String.format("test-%s-%s-%s", scope, type, classifier);
        final String version = "0.1.0";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        return new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
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
        mavenProject.setDependencyArtifacts(dependencyArtifacts);
        mavenProject.setRemoteArtifactRepositories(getArtifactRepositories());
        return mavenProject;
    }

    private DefaultArtifact getProjectArtifact() {
        final String groupId = "net.kemitix";
        final String artifactId = "assembly-execute-mojo";
        final String version = "0.1.0";
        final String compile = "compile";
        final String type = "jar";
        final String classifier = "";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        return new DefaultArtifact(groupId, artifactId, version, compile, type, classifier, artifactHandler);
    }

    private List<ArtifactRepository> getArtifactRepositories() {
        final ArtifactRepository artifactRepository = new MavenArtifactRepository();
        artifactRepository.setLayout(new DefaultRepositoryLayout());
        return Collections.singletonList(artifactRepository);
    }

}
