package org.apache.karaf.tooling;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link AssemblyMojoExec}.
 */
public class AssemblyMojoExecTest {

    // WILDCARD is used in place of the framework version as the test shouldn't be updated for every release
    private static final String DEFAULT_FRAMEWORK_KAR = "mvn:org.apache.karaf.features/framework/WILDCARD/xml/features";

    public static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Spy
    private Builder builder = Builder.newInstance();

    private AssemblyMojo assemblyMojo;

    private AssemblyMojoExec assemblyMojoExec;

    private Set<Artifact> dependencyArtifacts;

    @Mock
    private Log log;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(builder)
                   .generateAssembly();
        dependencyArtifacts = new HashSet<>();
        assemblyMojo = getAssemblyMojo();
        assemblyMojoExec = new AssemblyMojoExec(log, () -> builder);
        assemblyMojo.setConfig(new HashMap<>());
        assemblyMojo.setSystem(new HashMap<>());
    }

    @Test
    public void shouldExecuteMojoForFramework() throws Exception {
        //given
        final String framework = "framework";
        final String expectedFrameworkKar = DEFAULT_FRAMEWORK_KAR;
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{framework}, 2);
    }

    @Test
    public void shouldExecuteMojoForFrameworkLogBack() throws Exception {
        //given
        final String framework = "framework-logback";
        final String expectedFrameworkKar = DEFAULT_FRAMEWORK_KAR;
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{framework}, 2);
    }

    @Test
    public void shouldExecuteMojoForStaticFramework() throws Exception {
        //given
        final String framework = "static-framework";
        final String expectedFrameworkKar = "mvn:org.apache.karaf.features/static/WILDCARD/xml/features";
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{framework}, 2);
    }

    @Test
    public void shouldExecuteMojoForStaticFrameworkLogBack() throws Exception {
        //given
        final String framework = "static-framework-logback";
        final String expectedFrameworkKar = "mvn:org.apache.karaf.features/static/WILDCARD/xml/features";
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{framework}, 2);
    }

    private void executeMojoCheckingForFrameworkKar(final String framework, final String expectedFrameworkKar)
            throws Exception {
        //given
        assemblyMojo.setFramework(framework);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageKarsAdded(Builder.Stage.Startup, new String[]{expectedFrameworkKar}, 2);
    }

    @Test
    public void executeMojoWithInvalidFrameworkShouldThrowException() throws Exception {
        //given
        assemblyMojo.setFramework("unknown");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unsupported framework: unknown");
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

    @Test
    public void executeMojoWithStartupProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        assemblyMojo.setStartupProfiles(Collections.singletonList("startup profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

    @Test
    public void executeMojoWithBootProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        assemblyMojo.setBootProfiles(Collections.singletonList("boot profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

    @Test
    public void executeMojoWithInstalledProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        assemblyMojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

    @Test
    public void executeMojoWithProfilesAndProfileDirectoryShouldBeOkay() throws Exception {
        //given
        assemblyMojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        assemblyMojo.setProfilesUri("profiles uri");
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

    @Test
    public void executeMojoWithCompileKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "kar", ""));
        final String expected = "mvn:org.apache/test-compile-kar-/0.1.0/kar";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageKarsAdded(Builder.Stage.Startup, new String[]{DEFAULT_FRAMEWORK_KAR, expected}, 2);
    }

    @Test
    public void executeMojoWithRuntimeKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "kar", ""));
        final String expected = "mvn:org.apache/test-runtime-kar-/0.1.0/kar";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageKarsAdded(Builder.Stage.Boot, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithProvidedKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "kar", ""));
        final String expected = "mvn:org.apache/test-provided-kar-/0.1.0/kar";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageKarsAdded(Builder.Stage.Installed, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithCompileJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", ""));
        final String expected = "mvn:org.apache/test-compile-jar-/0.1.0";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithRuntimeJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", ""));
        final String expected = "mvn:org.apache/test-runtime-jar-/0.1.0";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", ""));
        final String expected = "mvn:org.apache/test-provided-jar-/0.1.0";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileBundleDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "bundle", ""));
        final String expected = "mvn:org.apache/test-compile-bundle-/0.1.0/bundle";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertBundlesAdded(new String[]{expected,});
    }

    @Test
    public void executeMojoWithCompileJarFeatureDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "features"));
        final String expected = "mvn:org.apache/test-compile-jar-features/0.1.0/jar/features";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageRepoAdded(Builder.Stage.Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-compile-jar-karaf/0.1.0/jar/karaf";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageRepoAdded(Builder.Stage.Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithRuntimeJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-runtime-jar-karaf/0.1.0/jar/karaf";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageRepoAdded(Builder.Stage.Boot, new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-provided-jar-karaf/0.1.0/jar/karaf";
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertStageRepoAdded(Builder.Stage.Installed, new String[]{expected});
    }

    @Test
    public void executeMojoWithNoDependencies() throws Exception {
        //given
        dependencyArtifacts.clear();
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        assertFeaturesAdded(new String[]{});
        assertStageFeaturesAdded(Builder.Stage.Startup, new String[]{"framework"}, 2);
        assertStageFeaturesAdded(Builder.Stage.Boot, new String[]{}, 1);
        assertStageFeaturesAdded(Builder.Stage.Installed, new String[]{}, 1);
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

    private void assertStageKarsAdded(final Builder.Stage stage, final String[] kars, final int calls) {
        // convert plain mvn urls into regex patterns
        final List<Pattern> patterns = Arrays.stream(kars)
                                             .map(pattern -> "^" + pattern + "$")
                                             .map(pattern -> pattern.replace(".", "\\."))
                                             .map(pattern -> pattern.replace("WILDCARD", ".*"))
                                             .map(Pattern::compile)
                                             .collect(Collectors.toList());
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(calls))
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
        final File baseDir = resources.getBasedir(TEST_PROJECT);
        final File pom = new File(baseDir, "pom.xml");
        final MavenProject mavenProject = getMavenProject(pom);

        final AssemblyMojo assemblyMojo = (AssemblyMojo) mojoRule.lookupMojo("assembly", pom);
        assemblyMojo.setLog(log);
        assemblyMojo.setProject(mavenProject);
        assemblyMojo.setMavenSession(getMavenSession(mavenProject));
        assemblyMojo.setLocalRepo(getLocalRepository(baseDir));
        assemblyMojo.setWorkDirectory(new File(baseDir, "assembly"));
        assemblyMojo.setSourceDirectory(new File(baseDir, "source"));
        assemblyMojo.setFramework("framework");
        assemblyMojo.setJavase("1.8");
        assemblyMojo.setStartupRepositories(new ArrayList<>());
        assemblyMojo.setBootRepositories(new ArrayList<>());
        assemblyMojo.setInstalledRepositories(new ArrayList<>());
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
        final String artifactId = TEST_PROJECT;
        final String version = "0.1.0";
        final String compile = "compile";
        final String type = "jar";
        final String classifier = "";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        return new DefaultArtifact(groupId, artifactId, version, compile, type, classifier, artifactHandler);
    }

    private List<ArtifactRepository> getArtifactRepositories() {
        final ArtifactRepository artifactRepository = new MavenArtifactRepository();
        artifactRepository.setId("default-id");
        artifactRepository.setUrl("default-url");
        artifactRepository.setLayout(new DefaultRepositoryLayout());
        return Collections.singletonList(artifactRepository);
    }

    @Test
    public void executeMojoUsesDeprecatedFeatureRepositories() throws Exception {
        //given
        final List<String> featuresRepositories = Collections.singletonList("feature repository");
        assemblyMojo.setFeatureRepositories(featuresRepositories);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(log).should()
                 .warn(anyString());
        assertThat(assemblyMojo.getStartupRepositories()).containsAll(featuresRepositories);
        assertThat(assemblyMojo.getBootRepositories()).containsAll(featuresRepositories);
        assertThat(assemblyMojo.getInstalledRepositories()).containsAll(featuresRepositories);
    }

    @Test
    public void executeMojoDoesntUseDeprecatedFeatureRepositories() throws Exception {
        //given
        assemblyMojo.setFeatureRepositories(Collections.emptyList());
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(log).should(never())
                 .warn(anyString());
    }

    @Test
    public void executeMojoAddsMavenRepositories() throws Exception {
        //given
        final ArtifactRepositoryPolicy enabledPolicy = new ArtifactRepositoryPolicy(true, "", "");
        final ArtifactRepositoryPolicy disabledPolicy = new ArtifactRepositoryPolicy(false, "", "");
        final DefaultRepositoryLayout layout = new DefaultRepositoryLayout();
        final List<ArtifactRepository> repositories = Arrays.asList(
                new MavenArtifactRepository("snapshot-id", "snapshot-url", layout, enabledPolicy, disabledPolicy),
                new MavenArtifactRepository("release-id", "release-url", layout, disabledPolicy, enabledPolicy),
                new MavenArtifactRepository("both-id", "both-url", layout, enabledPolicy, enabledPolicy),
                new MavenArtifactRepository("neither-id", "neither-url", layout, disabledPolicy, disabledPolicy)
                                                                   );
        assemblyMojo.getProject()
                    .setRemoteArtifactRepositories(repositories);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should()
                     .mavenRepositories(stringArgumentCaptor.capture());
        final String repos = stringArgumentCaptor.getValue();
        assertThat(repos).as("default repo")
                         .contains("snapshot-url@id=snapshot-id@noreleases@snapshots")
                         .contains("release-url@id=release-id")
                         .contains("both-url@id=both-id@snapshots")
                         .contains("neither-url@id=neither-id@noreleases");
    }

    @Test
    public void executeMojoWithNullConfig() throws Exception {
        //given
        assemblyMojo.setConfig(null);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(builder).should(never())
                     .config(any(), any());
    }

    @Test
    public void executeMojoWithNullSystem() throws Exception {
        //given
        assemblyMojo.setSystem(null);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(builder).should(never())
                     .system(any(), any());
    }

    @Test
    public void executeMojoWithPropertyFileEdits() throws Exception {
        //given
        final String propertyFileEdits =
                new File(resources.getBasedir(TEST_PROJECT), "property-file-edits").getAbsolutePath();
        assemblyMojo.setPropertyFileEdits(propertyFileEdits);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(builder).should()
                     .propertyEdits(any());
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsMissing() throws Exception {
        //given
        final String propertyFileEdits = new File(resources.getBasedir(TEST_PROJECT), "missing-file").getAbsolutePath();
        assemblyMojo.setPropertyFileEdits(propertyFileEdits);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
        //then
        then(builder).should(never())
                     .propertyEdits(any());
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsDirectory() throws Exception {
        //given
        final String propertyFileEdits = resources.getBasedir(TEST_PROJECT).getAbsolutePath();
        assemblyMojo.setPropertyFileEdits(propertyFileEdits);
        exception.expect(FileNotFoundException.class);
        //when
        assemblyMojoExec.doExecute(assemblyMojo);
    }

}
