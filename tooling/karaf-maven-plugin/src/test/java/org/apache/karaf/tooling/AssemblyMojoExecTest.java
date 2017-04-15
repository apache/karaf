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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;

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

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

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
        dependencyArtifacts.addAll(
                Arrays.asList(getDependency("compile", "jar", ""), getDependency("runtime", "jar", ""),
                              getDependency("provided", "jar", ""), getDependency("other", "jar", ""),
                              getDependency("compile", "bundle", ""), getDependency("compile", "other", ""),
                              getDependency("compile", "kar", ""), getDependency("runtime", "kar", ""),
                              getDependency("provided", "kar", ""), getDependency("compile", "jar", "features"),
                              getDependency("compile", "jar", "karaf"), getDependency("runtime", "jar", "karaf"),
                              getDependency("provided", "jar", "karaf")
                             ));

        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageKarsAdded(Builder.Stage.Startup, new String[]{
                // WILDCARD is used in place of the framework version as the test shouldn't be updated for every release
                "mvn:org.apache.karaf.features/framework/WILDCARD/xml/features",
                "mvn:org.apache/test-compile-kar-/0.1.0/kar"
        });
        assertStageKarsAdded(Builder.Stage.Boot, new String[]{"mvn:org.apache/test-runtime-kar-/0.1.0/kar"});
        assertStageKarsAdded(Builder.Stage.Installed, new String[]{"mvn:org.apache/test-provided-kar-/0.1.0/kar"});

        assertStageRepoAdded(Builder.Stage.Startup, new String[]{
                "mvn:org.apache/test-compile-jar-features/0.1.0/jar/features",
                "mvn:org.apache/test-compile-jar-karaf/0.1.0/jar/karaf"
        });
        assertStageRepoAdded(Builder.Stage.Boot, new String[]{"mvn:org.apache/test-runtime-jar-karaf/0.1.0/jar/karaf"});
        assertStageRepoAdded(
                Builder.Stage.Installed, new String[]{"mvn:org.apache/test-provided-jar-karaf/0.1.0/jar/karaf"});

        assertFeaturesAdded(new String[]{});
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{"framework"}, 2);
        assertStageFeaturesAdded(Builder.Stage.Boot, new String[]{}, 1);
        assertStageFeaturesAdded(Builder.Stage.Installed, new String[]{}, 1);
        assertBundlesAdded(new String[]{
                "mvn:org.apache/test-compile-jar-/0.1.0", "mvn:org.apache/test-compile-bundle-/0.1.0/bundle",
                "mvn:org.apache/test-runtime-jar-/0.1.0", "mvn:org.apache/test-provided-jar-/0.1.0"
        });
        assertProfilesAdded(new String[]{});
    }

    private void assertProfilesAdded(final String[] profiles) {
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(3))
                     .profiles(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).as("Add profiles")
                                                       .containsExactlyInAnyOrder(profiles);
    }

    private void assertBundlesAdded(final String[] bundles) {
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(3))
                     .bundles(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).as("Add bundles")
                                                       .containsExactlyInAnyOrder(bundles);
    }

    private void assertFeaturesAdded(final String[] features) {
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(3))
                     .features(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).as("Add features")
                                                       .containsExactlyInAnyOrder(features);
    }

    private void assertStageFeaturesAdded(final Builder.Stage stage, final String[] features, final int calls) {
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(calls))
                     .features(eq(stage), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).as("Add features to Stage " + stage)
                                                       .containsExactlyInAnyOrder(features);
    }

    private void assertStageRepoAdded(final Builder.Stage stage, final String[] repos) {
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should()
                     .repositories(eq(stage), anyBoolean(), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).as("Add repositories to Stage " + stage)
                                                       .containsExactlyInAnyOrder(repos);
    }

    private void assertStageKarsAdded(final Builder.Stage stage, final String[] kars) {
        // convert plain mvn urls into regex patterns
        final List<Pattern> patterns = Arrays.stream(kars)
                                             .map(pattern -> "^" + pattern + "$")
                                             .map(pattern -> pattern.replace(".", "\\."))
                                             .map(pattern -> pattern.replace("WILDCARD", ".*"))
                                             .map(Pattern::compile)
                                             .collect(Collectors.toList());
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(this.builder).should(times(kars.length))
                          .kars(eq(stage), anyBoolean(), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).hasSize(kars.length);
        assertThat(stringArgumentCaptor.getAllValues()
                                       .stream()
                                       .filter(kar -> patterns.stream()
                                                              .anyMatch(pattern -> pattern.matcher(kar)
                                                                                          .matches()))
                                       .collect(Collectors.toList())).hasSize(kars.length);
    }

    private DefaultArtifact getDependency(final String scope, final String type, final String classifier) {
        final String groupId = "org.apache";
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
        final String groupId = "org.apache";
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
