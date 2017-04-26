package org.apache.karaf.tooling.assembly;

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
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.util.ArrayList;
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
import static org.junit.Assume.assumeTrue;
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

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Spy
    private Builder builder = Builder.newInstance();

    private AssemblyMojo mojo;

    private AssemblyMojoExec execMojo;

    private Set<Artifact> dependencyArtifacts;

    @Mock
    private Log log;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    @Captor
    private ArgumentCaptor<Map<String, String>> mapArgumentCaptor;

    private BuilderFactory builderFactory;

    private AssemblyOutfitter assemblyOutfitter;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        doNothing().when(builder)
                   .generateAssembly();
        dependencyArtifacts = new HashSet<>();
        mojo = getAssemblyMojo();
        builderFactory = new BuilderFactory(log, builder);
        assemblyOutfitter = new AssemblyOutfitter(mojo);
        execMojo = new AssemblyMojoExec(log, builderFactory, assemblyOutfitter);
        mojo.setConfig(new HashMap<>());
        mojo.setSystem(new HashMap<>());
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
        mojo.setFramework(framework);
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageKarsAdded(Builder.Stage.Startup, new String[]{expectedFrameworkKar}, 2);
    }

    @Test
    public void executeMojoWithInvalidFrameworkShouldThrowException() throws Exception {
        //given
        mojo.setFramework("unknown");
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unsupported framework: unknown");
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithStartupProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithBootProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setBootProfiles(Collections.singletonList("boot profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithInstalledProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithProfilesAndProfileDirectoryShouldBeOkay() throws Exception {
        //given
        mojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        mojo.setProfilesUri("profiles uri");
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithCompileKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "kar", ""));
        final String expected = "mvn:org.apache/test-compile-kar-/0.1.0/kar";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageKarsAdded(Builder.Stage.Startup, new String[]{DEFAULT_FRAMEWORK_KAR, expected}, 2);
    }

    @Test
    public void executeMojoWithRuntimeKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "kar", ""));
        final String expected = "mvn:org.apache/test-runtime-kar-/0.1.0/kar";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageKarsAdded(Builder.Stage.Boot, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithProvidedKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "kar", ""));
        final String expected = "mvn:org.apache/test-provided-kar-/0.1.0/kar";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageKarsAdded(Builder.Stage.Installed, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithCompileJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", ""));
        final String expected = "mvn:org.apache/test-compile-jar-/0.1.0";
        //when
        execMojo.doExecute(mojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithUnknownTypeDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "unknown", ""));
        //when
        execMojo.doExecute(mojo);
        //then
        assertBundlesAdded(new String[]{});
    }

    @Test
    public void executeMojoWithRuntimeJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", ""));
        final String expected = "mvn:org.apache/test-runtime-jar-/0.1.0";
        //when
        execMojo.doExecute(mojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", ""));
        final String expected = "mvn:org.apache/test-provided-jar-/0.1.0";
        //when
        execMojo.doExecute(mojo);
        //then
        assertBundlesAdded(new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileBundleDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "bundle", ""));
        final String expected = "mvn:org.apache/test-compile-bundle-/0.1.0/bundle";
        //when
        execMojo.doExecute(mojo);
        //then
        assertBundlesAdded(new String[]{expected,});
    }

    @Test
    public void executeMojoWithCompileJarFeatureDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "features"));
        final String expected = "mvn:org.apache/test-compile-jar-features/0.1.0/jar/features";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageRepoAdded(Builder.Stage.Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-compile-jar-karaf/0.1.0/jar/karaf";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageRepoAdded(Builder.Stage.Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithRuntimeJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-runtime-jar-karaf/0.1.0/jar/karaf";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageRepoAdded(Builder.Stage.Boot, new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-provided-jar-karaf/0.1.0/jar/karaf";
        //when
        execMojo.doExecute(mojo);
        //then
        assertStageRepoAdded(Builder.Stage.Installed, new String[]{expected});
    }

    @Test
    public void executeMojoWithUnknownScope() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("unknown", "jar", ""));
        //when
        execMojo.doExecute(mojo);
        //then
        stringArgumentCaptor.getAllValues()
                            .clear();
        then(builder).should(times(3))
                     .bundles(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()
                                       .stream()
                                       .filter(value -> value.contains("unknown"))
                                       .findAny()).isNotPresent();
    }

    @Test
    public void executeMojoWithNoDependencies() throws Exception {
        //given
        dependencyArtifacts.clear();
        //when
        execMojo.doExecute(mojo);
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

    private DefaultArtifact getDependency(final String scope, final String type, final String classifier)
            throws IOException {
        final String groupId = "org.apache";
        final String artifactId = String.format("test-%s-%s-%s", scope, type, classifier == null ? "" : classifier);
        final String version = "0.1.0";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact artifact =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        artifact.setFile(new File(resources.getBasedir(TEST_PROJECT), artifactId));
        return artifact;
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
        assemblyMojo.setTranslatedUrls(new HashMap<>());
        return assemblyMojo;
    }

    private MavenSession getMavenSession(final MavenProject mavenProject) {
        return mojoRule.newMavenSession(mavenProject);
    }

    private ArtifactRepository getLocalRepository(final File baseDir) {
        return new StubArtifactRepository(baseDir.getAbsolutePath());
    }

    private MavenProject getMavenProject(final File pom) throws IOException {
        final MavenProject mavenProject = new MavenProject();
        mavenProject.setFile(pom);
        mavenProject.setArtifact(getProjectArtifact());
        mavenProject.setDependencyArtifacts(dependencyArtifacts);
        mavenProject.setRemoteArtifactRepositories(getArtifactRepositories());
        return mavenProject;
    }

    private DefaultArtifact getProjectArtifact() throws IOException {
        final String groupId = "org.apache";
        final String artifactId = TEST_PROJECT;
        final String version = "0.1.0";
        final String compile = "compile";
        final String type = "jar";
        final String classifier = "";
        final ArtifactHandler artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact defaultArtifact =
                new DefaultArtifact(groupId, artifactId, version, compile, type, classifier, artifactHandler);
        defaultArtifact.setFile(new File(resources.getBasedir(TEST_PROJECT), "artifact-file"));
        return defaultArtifact;
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
        mojo.setFeatureRepositories(featuresRepositories);
        //when
        execMojo.doExecute(mojo);
        //then
        then(log).should()
                 .warn(anyString());
        assertThat(mojo.getStartupRepositories()).containsAll(featuresRepositories);
        assertThat(mojo.getBootRepositories()).containsAll(featuresRepositories);
        assertThat(mojo.getInstalledRepositories()).containsAll(featuresRepositories);
    }

    @Test
    public void executeMojoDoesntUseDeprecatedFeatureRepositories() throws Exception {
        //given
        mojo.setFeatureRepositories(Collections.emptyList());
        //when
        execMojo.doExecute(mojo);
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
        mojo.getProject()
            .setRemoteArtifactRepositories(repositories);
        //when
        execMojo.doExecute(mojo);
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
        mojo.setConfig(null);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(never())
                     .config(any(), any());
    }

    @Test
    public void executeMojoWithNullSystem() throws Exception {
        //given
        mojo.setSystem(null);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(never())
                     .system(any(), any());
    }

    @Test
    public void executeMojoWithPropertyFileEdits() throws Exception {
        //given
        final String propertyFileEdits =
                new File(resources.getBasedir(TEST_PROJECT), "property-file-edits").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .propertyEdits(any());
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsMissing() throws Exception {
        //given
        final String propertyFileEdits = new File(resources.getBasedir(TEST_PROJECT), "missing-file").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(never())
                     .propertyEdits(any());
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsDirectory() throws Exception {
        //given
        final String propertyFileEdits = resources.getBasedir(TEST_PROJECT)
                                                  .getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        exception.expect(FileNotFoundException.class);
        //when
        execMojo.doExecute(mojo);
    }

    @Test
    public void executeMojoWithProjectArtifactFile() throws Exception {
        //given
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0/jar/";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactFileIsNull() throws Exception {
        //given
        mojo.getProject()
            .getArtifact()
            .setFile(null);
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0/jar/";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).doesNotContainKeys(expected);
    }

    @Test
    public void executeMojoWithMissingProjectArtifactFile() throws Exception {
        //given
        mojo.getProject()
            .getArtifact()
            .setFile(new File(resources.getBasedir(TEST_PROJECT), "missing-file"));
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0/jar/";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).doesNotContainKeys(expected);
    }

    @Test
    public void executeMojoWithProjectArtifactIsBundle() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "bundle", "");
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "bundle-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-bundle-/0.1.0/jar/";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactIsJarAndHasClassifier() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "jar", "extra");
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "extra-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-jar-extra/0.1.0/jar/extra";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactIsJarAndHasNullClassifier() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "jar", null);
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "extra-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-jar-/0.1.0";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactIsNonJarAndHasClassifier() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "kar", "extra");
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "extra-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-kar-extra/0.1.0/kar/extra";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactIsNonJarAndHasNullClassifier() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "kar", null);
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "extra-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-kar-/0.1.0/kar";
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()).containsKey(expected);
    }

    @Test
    public void executeMojoWithNullTranslatedUrls() throws Exception {
        //given
        mojo.setTranslatedUrls(null);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .translatedUrls(mapArgumentCaptor.capture());
        assertThat(mapArgumentCaptor.getValue()
                                    .entrySet()).hasSize(1);
    }

    @Test
    public void executeMojoWithLibraries() throws Exception {
        //given
        mojo.setLibraries(Collections.singletonList("library"));
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .libraries(stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsSubsequence("library");
    }

    @Test
    public void executeMojoDetectFrameworkKarForFrameworkWhenAlreadySet() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "framework";
        final String version = "version";
        final String scope = "compile";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        mojo.setFramework("framework");
        //when
        execMojo.doExecute(mojo);
        //then
        assertThat(mojo.getFramework()).isEqualTo("framework");
        then(builder).should()
                     .kars(eq(Builder.Stage.Startup), eq(false), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsOnly(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoDetectFrameworkKarForFramework() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "framework";
        final String version = "version";
        final String scope = "compile";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        mojo.setFramework(null);
        //when
        execMojo.doExecute(mojo);
        //then
        assertThat(mojo.getFramework()).isEqualTo("framework");
        then(builder).should()
                     .kars(eq(Builder.Stage.Startup), eq(false), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsOnly(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoDetectFrameworkKarForStaticFramework() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "static";
        final String version = "version";
        final String scope = "compile";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        mojo.setFramework(null);
        //when
        execMojo.doExecute(mojo);
        //then
        assertThat(mojo.getFramework()).isEqualTo("static-framework");
        then(builder).should()
                     .kars(eq(Builder.Stage.Startup), eq(false), stringArgumentCaptor.capture());
        assertThat(stringArgumentCaptor.getAllValues()).containsOnly(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoWhenStartupFeaturesIncludesFrameworkDoNotAddInjectAsMandatory() throws Exception {
        //given
        mojo.setStartupFeatures(Arrays.asList("framework", "other feature"));
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(never())
                     .features(Builder.Stage.Startup, "framework");
    }

    @Test
    public void executeMojoWhenStartupFeaturesDoesNotIncludeFrameworkThenInjectAsMandatory() throws Exception {
        //given
        mojo.setStartupFeatures(Collections.singletonList("other feature"));
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should()
                     .features(Builder.Stage.Startup, "framework");
    }

    @Test
    public void executeMojoWithProfilesShouldNotAddAllRepositories() throws Exception {
        //given
        mojo.setProfilesUri("url");
        mojo.setStartupRepositories(Collections.emptyList());
        mojo.setStartupProfiles(Collections.singletonList("value"));
        mojo.setBootProfiles(Collections.singletonList("value"));
        mojo.setInstalledProfiles(Collections.singletonList("value"));
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(times(3))
                     .repositories(eq(false));
    }

    @Test
    public void executeMojoWithFeaturesShouldNotAddAllRepositories() throws Exception {
        //given
        mojo.setStartupFeatures(Collections.singletonList("value"));
        mojo.setBootFeatures(Collections.singletonList("value"));
        mojo.setInstalledFeatures(Collections.singletonList("value"));
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(times(3))
                     .repositories(eq(false));
    }

    @Test
    public void executeMojoWhenNotInstallAllFeatureByDefault() throws Exception {
        //given
        mojo.setInstallAllFeaturesByDefault(false);
        //when
        execMojo.doExecute(mojo);
        //then
        then(builder).should(times(3))
                     .repositories(eq(false));
    }

    @Test
    public void executeMojoWhenNotIncludeBuildOutputDirectory() throws Exception {
        //given
        final String outputDirectory = new File(resources.getBasedir(TEST_PROJECT), "output").getAbsolutePath();
        mojo.getProject()
            .getBuild()
            .setOutputDirectory(outputDirectory);
        mojo.setIncludeBuildOutputDirectory(false);
        //when
        execMojo.doExecute(mojo);
        //then
        final File copyMeFile = new File(mojo.getWorkDirectory(), "copy-me");
        assertThat(copyMeFile).doesNotExist();
    }

    @Test
    public void executeMojoWhenIncludeBuildOutputDirectory() throws Exception {
        //given
        final String outputDirectory = new File(resources.getBasedir(TEST_PROJECT), "output").getAbsolutePath();
        mojo.getProject()
            .getBuild()
            .setOutputDirectory(outputDirectory);
        mojo.setIncludeBuildOutputDirectory(true);
        //when
        execMojo.doExecute(mojo);
        //then
        final File copyMeFile = new File(mojo.getWorkDirectory(), "copy-me");
        assertThat(copyMeFile).exists();
        assertThat(Files.readAllLines(copyMeFile.toPath())).containsExactly("copy-me content");
    }

    @Test
    public void executeMojoWhenNotOverwriteAssemblyFromSource() throws Exception {
        //given
        final File sourceDirectory = new File(resources.getBasedir(TEST_PROJECT), "missing-dir");
        mojo.setSourceDirectory(sourceDirectory);
        //when
        execMojo.doExecute(mojo);
        //then
        final File[] files = mojo.getWorkDirectory()
                                 .listFiles();
        assertThat(files).isEmpty();
    }

    @Test
    public void executeMojoWhenOverwriteAssemblyFromSource() throws Exception {
        //given
        final File sourceDirectory = new File(resources.getBasedir(TEST_PROJECT), "source");
        mojo.setSourceDirectory(sourceDirectory);
        //when
        execMojo.doExecute(mojo);
        //then
        assertThat(new File(mojo.getWorkDirectory(), "source-file")).exists();
    }

    @Test
    public void executeMojoSetsBinFilesToExecutable() throws Exception {
        //given
        final File workDirectory = mojo.getWorkDirectory();
        final FileSystem fileSystem = workDirectory.toPath()
                                                   .getFileSystem();
        assumeTrue(fileSystem.supportedFileAttributeViews()
                             .contains("posix"));
        //when
        execMojo.doExecute(mojo);
        //then
        final File executableFile = new File(new File(workDirectory, "bin"), "executable-file");
        assertThat(executableFile.canExecute()).isTrue();
    }

    @Test
    public void executeMojoSetsBinFilesToExecutableUnlessBatFile() throws Exception {
        //given
        final File workDirectory = mojo.getWorkDirectory();
        final FileSystem fileSystem = workDirectory.toPath()
                                                   .getFileSystem();
        assumeTrue(fileSystem.supportedFileAttributeViews()
                             .contains("posix"));
        //when
        execMojo.doExecute(mojo);
        //then
        final File executableFile = new File(new File(workDirectory, "bin"), "batch.bat");
        assertThat(executableFile.canExecute()).isFalse();
    }

}
