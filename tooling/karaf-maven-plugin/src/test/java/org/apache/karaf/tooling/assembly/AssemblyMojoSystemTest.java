package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.plugin.testing.stubs.DefaultArtifactHandlerStub;
import org.apache.maven.plugin.testing.stubs.StubArtifactRepository;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
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

import static org.apache.karaf.profile.assembly.Builder.Stage.Boot;
import static org.apache.karaf.profile.assembly.Builder.Stage.Installed;
import static org.apache.karaf.profile.assembly.Builder.Stage.Startup;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assume.assumeTrue;

/**
 * Tests for {@link AssemblyMojo} and it's attendant classes.
 */
public class AssemblyMojoSystemTest extends EasyMockSupport {

    // WILDCARD is used in place of the framework version as the test shouldn't be updated for every release
    private static final String DEFAULT_FRAMEWORK_KAR = "mvn:org.apache.karaf.features/framework/WILDCARD/xml/features";

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private BuilderSpy builder = new BuilderSpy();

    private AssemblyMojo mojo;

    private AssemblyMojoExec execMojo;

    private Set<Artifact> dependencyArtifacts;

    @Mock
    private Log log;

    @Mock
    private KarafPropertyInstructionsModelStaxReader profileEditsReader;

    @Before
    public void setUp() throws Exception {
        dependencyArtifacts = new HashSet<>();
        final MavenUriParser mavenUriParser = new MavenUriParser();
        final ProfileEditsParser profileEditsParser = new ProfileEditsParser(profileEditsReader);
        final ArtifactFrameworkParser frameworkParser = new ArtifactFrameworkParser();
        final StartupArtifactParser startupArtifactParser = new StartupArtifactParser();
        final BootArtifactParser bootArtifactParser = new BootArtifactParser();
        final InstalledArtifactParser installedArtifactParser = new InstalledArtifactParser();
        final ArtifactParser artifactParser =
                new ArtifactParser(mavenUriParser, builder, frameworkParser, startupArtifactParser, bootArtifactParser,
                                   installedArtifactParser
                );
        final BuilderConfiguration builderConfiguration =
                new BuilderConfiguration(log, mavenUriParser, profileEditsParser, artifactParser);
        mojo = getAssemblyMojo();
        mojo.setConfig(new HashMap<>());
        mojo.setSystem(new HashMap<>());
        final ExecutableFile executableFile = new ExecutableFile();
        final AssemblyOutfitter assemblyOutfitter = new AssemblyOutfitter(mojo, executableFile);
        execMojo = new AssemblyMojoExec(log, builder, builderConfiguration, assemblyOutfitter);
        mojo.setMojoExec(execMojo);
    }

    private AssemblyMojo getAssemblyMojo() throws Exception {
        final File baseDir = resources.getBasedir(TEST_PROJECT);
        final File pom = new File(baseDir, "pom.xml");
        final MavenProject mavenProject = AssemblyMother.getProject(TEST_PROJECT, resources, dependencyArtifacts);
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

    @Test
    public void shouldExecuteMojoForFramework() throws Exception {
        //given
        final String framework = "framework";
        //when
        executeMojoCheckingForFrameworkKar(framework, DEFAULT_FRAMEWORK_KAR);
        //then
        assertFeaturesAddedToStage(Startup, new String[]{framework});
    }

    private void executeMojoCheckingForFrameworkKar(final String framework, final String expectedFrameworkKar)
            throws Exception {
        //given
        mojo.setFramework(framework);
        //when
        executeMojo();
        //then
        assertStageKarsAdded(Startup, new String[]{expectedFrameworkKar}, 2);
    }

    private void executeMojo() throws Exception {
        mojo.execute();
    }

    private void assertStageKarsAdded(final Builder.Stage stage, final String[] kars, final int calls) {
        // convert plain mvn urls into regex patterns
        final List<Pattern> patterns = Arrays.stream(kars)
                                             .map(pattern -> "^" + pattern + "$")
                                             .map(pattern -> pattern.replace(".", "\\."))
                                             .map(pattern -> pattern.replace("WILDCARD", ".*"))
                                             .map(Pattern::compile)
                                             .collect(Collectors.toList());
        final List<String> karsByStage = builder.getAllKarsForStage(stage);
        assertThat(karsByStage).hasSize(kars.length);
        assertThat(karsByStage.stream()
                              .filter(kar -> patterns.stream()
                                                     .anyMatch(pattern -> pattern.matcher(kar)
                                                                                 .matches()))
                              .collect(Collectors.toList())).hasSize(kars.length);
    }

    @Test
    public void shouldExecuteMojoForFrameworkLogBack() throws Exception {
        //given
        final String framework = "framework-logback";
        //when
        executeMojoCheckingForFrameworkKar(framework, DEFAULT_FRAMEWORK_KAR);
        //then
        assertFeaturesAddedToStage(Startup, new String[]{framework});
    }

    private void assertFeaturesAddedToStage(final Builder.Stage stage, final String[] features) {
        List<String> allFeaturesForStage = builder.getAllFeaturesForStage(stage);
        assertThat(allFeaturesForStage).as("Add features to Stage " + stage)
                                       .containsExactlyInAnyOrder(features);
    }

    @Test
    public void shouldExecuteMojoForStaticFramework() throws Exception {
        //given
        final String framework = "static-framework";
        final String expectedFrameworkKar = "mvn:org.apache.karaf.features/static/WILDCARD/xml/features";
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertFeaturesAddedToStage(Startup, new String[]{framework});
    }

    @Test
    public void shouldExecuteMojoForStaticFrameworkLogBack() throws Exception {
        //given
        final String framework = "static-framework-logback";
        final String expectedFrameworkKar = "mvn:org.apache.karaf.features/static/WILDCARD/xml/features";
        //when
        executeMojoCheckingForFrameworkKar(framework, expectedFrameworkKar);
        //then
        assertFeaturesAddedToStage(Startup, new String[]{framework});
    }

    @Test
    public void executeMojoWithInvalidFrameworkShouldThrowException() throws Exception {
        //given
        mojo.setFramework("unknown");
        //when
        executeMojoAndExpect(IllegalArgumentException.class, "Unsupported framework: unknown");
    }

    private void executeMojoAndExpect(final Class<IllegalArgumentException> expectedCause, final String message)
            throws Exception {
        try {
            executeMojo();
            fail("MojoExecutionException not thrown");
        } catch (MojoExecutionException e) {
            final Throwable cause = e.getCause();
            assertThat(cause).isInstanceOf(expectedCause);
            assertThat(cause.getMessage()).contains(message);
        }
    }

    @Test
    public void executeMojoWithStartupProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        //when
        executeMojoAndExpect(IllegalArgumentException.class, "profilesDirectory must be specified");
    }

    @Test
    public void executeMojoWithBootProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setBootProfiles(Collections.singletonList("boot profile"));
        //when
        executeMojoAndExpect(IllegalArgumentException.class, "profilesDirectory must be specified");
    }

    @Test
    public void executeMojoWithInstalledProfilesAndNoProfileDirectoryShouldThrowException() throws Exception {
        //given
        mojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        //when
        executeMojoAndExpect(IllegalArgumentException.class, "profilesDirectory must be specified");
    }

    @Test
    public void executeMojoWithProfilesAndProfileDirectoryShouldBeOkay() throws Exception {
        //given
        mojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        mojo.setProfilesUri("profiles uri");
        //when
        executeMojo();
    }

    @Test
    public void executeMojoWithCompileKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "kar", ""));
        final String expected = "mvn:org.apache/test-compile-kar-/0.1.0/kar";
        //when
        executeMojo();
        //then
        assertStageKarsAdded(Startup, new String[]{DEFAULT_FRAMEWORK_KAR, expected}, 2);
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

    @Test
    public void executeMojoWithRuntimeKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "kar", ""));
        final String expected = "mvn:org.apache/test-runtime-kar-/0.1.0/kar";
        //when
        executeMojo();
        //then
        assertStageKarsAdded(Boot, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithProvidedKarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "kar", ""));
        final String expected = "mvn:org.apache/test-provided-kar-/0.1.0/kar";
        //when
        executeMojo();
        //then
        assertStageKarsAdded(Builder.Stage.Installed, new String[]{expected}, 1);
    }

    @Test
    public void executeMojoWithCompileJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", ""));
        final String expected = "mvn:org.apache/test-compile-jar-/0.1.0";
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Startup, new String[]{expected});
    }

    private void assertBundlesAddedToStage(final Builder.Stage stage, final String[] bundles) {
        assertThat(builder.getAllBundlesForStage(stage)).as("Add bundles to stage " + stage)
                                                        .containsExactlyInAnyOrder(bundles);
    }

    @Test
    public void executeMojoWithUnknownTypeDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "unknown", ""));
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Startup, new String[]{});
    }

    @Test
    public void executeMojoWithRuntimeJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", ""));
        final String expected = "mvn:org.apache/test-runtime-jar-/0.1.0";
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Boot, new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", ""));
        final String expected = "mvn:org.apache/test-provided-jar-/0.1.0";
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Installed, new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileBundleDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "bundle", ""));
        final String expected = "mvn:org.apache/test-compile-bundle-/0.1.0";
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithCompileJarFeatureDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "features"));
        final String expected = "mvn:org.apache/test-compile-jar-features/0.1.0/jar/features";
        //when
        executeMojo();
        //then
        assertRepoAddedToStage(Startup, new String[]{expected});
    }

    private void assertRepoAddedToStage(final Builder.Stage stage, final String[] repos) {
        assertThat(builder.getAllRepositoriesForStage(stage)).as("Add repositories to Stage " + stage)
                                                             .containsExactlyInAnyOrder(repos);
    }

    @Test
    public void executeMojoWithCompileJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("compile", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-compile-jar-karaf/0.1.0/jar/karaf";
        //when
        executeMojo();
        //then
        assertRepoAddedToStage(Startup, new String[]{expected});
    }

    @Test
    public void executeMojoWithRuntimeJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("runtime", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-runtime-jar-karaf/0.1.0/jar/karaf";
        //when
        executeMojo();
        //then
        assertRepoAddedToStage(Boot, new String[]{expected});
    }

    @Test
    public void executeMojoWithProvidedJarKarafDependency() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("provided", "jar", "karaf"));
        final String expected = "mvn:org.apache/test-provided-jar-karaf/0.1.0/jar/karaf";
        //when
        executeMojo();
        //then
        assertRepoAddedToStage(Builder.Stage.Installed, new String[]{expected});
    }

    @Test
    public void executeMojoWithUnknownScope() throws Exception {
        //given
        dependencyArtifacts.add(getDependency("unknown", "jar", ""));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllBundlesForStage(Startup)).isEmpty();
        assertThat(builder.getAllBundlesForStage(Boot)).isEmpty();
        assertThat(builder.getAllBundlesForStage(Installed)).isEmpty();
    }

    @Test
    public void executeMojoWithNoDependencies() throws Exception {
        //given
        dependencyArtifacts.clear();
        //when
        executeMojo();
        //then
        assertBundlesAddedToStage(Startup, new String[]{});
        assertBundlesAddedToStage(Boot, new String[]{});
        assertBundlesAddedToStage(Installed, new String[]{});
        assertFeaturesAddedToStage(Startup, new String[]{"framework"});
        assertFeaturesAddedToStage(Boot, new String[]{});
        assertFeaturesAddedToStage(Installed, new String[]{});
        assertProfilesAddedToStage(Startup, new String[]{});
        assertProfilesAddedToStage(Boot, new String[]{});
        assertProfilesAddedToStage(Installed, new String[]{});
    }

    private void assertProfilesAddedToStage(final Builder.Stage stage, final String[] profiles) {
        assertThat(builder.getAllProfilesForStage(stage)).as("Add profiles to stage " + stage)
                                                         .containsExactlyInAnyOrder(profiles);
    }

    @Test
    public void executeMojoUsesDeprecatedFeatureRepositories() throws Exception {
        //given
        final List<String> featuresRepositories = Collections.singletonList("feature repository");
        mojo.setFeatureRepositories(featuresRepositories);
        log.warn("Use of featureRepositories is deprecated, use startupRepositories, bootRepositories or "
                 + "installedRepositories instead");
        withDefaultLogging();
        replayAll();
        //when
        executeMojo();
        //then
        verifyAll();
        assertThat(mojo.getStartupRepositories()).containsAll(featuresRepositories);
        assertThat(mojo.getBootRepositories()).containsAll(featuresRepositories);
        assertThat(mojo.getInstalledRepositories()).containsAll(featuresRepositories);
    }

    @Test
    public void executeMojoDoesntUseDeprecatedFeatureRepositories() throws Exception {
        //given
        mojo.setFeatureRepositories(Collections.emptyList());
        withDefaultLogging();
        replayAll();
        //when
        executeMojo();
        //then
        verifyAll();
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
        executeMojo();
        //then
        final String repos = builder.getAllMavenRepositories();
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
        executeMojo();
        //then
        assertThat(builder.isConfigCalled()).isFalse();
    }

    @Test
    public void executeMojoWithNullSystem() throws Exception {
        //given
        mojo.setSystem(null);
        //when
        executeMojo();
        //then
        assertThat(builder.isSystemCalled()).isFalse();
    }

    @Test
    public void executeMojoWithPropertyFileEdits() throws Exception {
        //given
        final KarafPropertyEdits edit = givenPropertyEditsExist();
        expect(profileEditsReader.read(anyObject(InputStream.class), eq(true))).andReturn(edit);
        withDefaultLogging();
        replayAll();
        //when
        executeMojo();
        //then
        verifyAll();
        assertThat(builder.isPropertyEditsCalled()).isTrue();
    }

    private KarafPropertyEdits givenPropertyEditsExist() throws IOException {
        final KarafPropertyEdits edit = new KarafPropertyEdits();
        final String propertyFileEdits =
                new File(resources.getBasedir(TEST_PROJECT), "property-file-edits").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        return edit;
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsMissing() throws Exception {
        //given
        final String propertyFileEdits = new File(resources.getBasedir(TEST_PROJECT), "missing-file").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        //when
        executeMojo();
        //then
        assertThat(builder.isPropertyEditsCalled()).isFalse();
    }

    @Test
    public void executeMojoWithPropertyFileEditsWhenFileIsDirectory() throws Exception {
        //given
        final String propertyFileEdits = resources.getBasedir(TEST_PROJECT)
                                                  .getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
        //when
        try {
            executeMojo();
            fail("MojoExecutionException not thrown");
        } catch (MojoExecutionException e) {
            assertThat(e.getCause()
                        .getCause()).isInstanceOf(FileNotFoundException.class);
        }
    }

    @Test
    public void executeMojoWithProjectArtifactFile() throws Exception {
        //given
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0";
        //when
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
    }

    @Test
    public void executeMojoWhenProjectArtifactFileIsNull() throws Exception {
        //given
        mojo.getProject()
            .getArtifact()
            .setFile(null);
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0/jar/";
        //when
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).doesNotContainKeys(expected);
    }

    @Test
    public void executeMojoWithMissingProjectArtifactFile() throws Exception {
        //given
        mojo.getProject()
            .getArtifact()
            .setFile(new File(resources.getBasedir(TEST_PROJECT), "missing-file"));
        final String expected = "mvn:org.apache/assembly-execute-mojo/0.1.0/jar/";
        //when
        executeMojo();
        //then
        final Map<String, String> translatedUrls = builder.getAllTranslatedUrls();
        assertThat(translatedUrls).doesNotContainKeys(expected);
    }

    @Test
    public void executeMojoWithProjectArtifactIsBundle() throws Exception {
        //given
        final DefaultArtifact bundle = getDependency("compile", "bundle", "");
        bundle.setFile(new File(resources.getBasedir(TEST_PROJECT), "bundle-file"));
        mojo.getProject()
            .addAttachedArtifact(bundle);
        final String expected = "mvn:org.apache/test-compile-bundle-/0.1.0";
        //when
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
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
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
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
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
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
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
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
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()).containsKey(expected);
    }

    @Test
    public void executeMojoWithNullTranslatedUrls() throws Exception {
        //given
        mojo.setTranslatedUrls(null);
        //when
        executeMojo();
        //then
        assertThat(builder.getAllTranslatedUrls()
                          .entrySet()).hasSize(1);
    }

    @Test
    public void executeMojoWithLibraries() throws Exception {
        //given
        mojo.setLibraries(Collections.singletonList("library"));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllLibraries()).containsExactly("library");
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
        executeMojo();
        //then
        assertThat(mojo.getFramework()).isEqualTo("framework");
        assertThat(builder.getAllKarsForStage(Startup)).containsOnly(
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
        executeMojo();
        //then
        assertThat(mojo.getFramework()).isEqualTo("framework");
        assertThat(builder.getAllKarsForStage(Startup)).containsOnly(
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
        executeMojo();
        //then
        assertThat(mojo.getFramework()).isEqualTo("static-framework");
        assertThat(builder.getAllKarsForStage(Startup)).containsOnly(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoKarCompileDependencyIsAStartupKar() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "test";
        final String version = "version";
        final String scope = "compile";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        //when
        executeMojo();
        //then
        assertThat(builder.getAllKarsForStage(Startup)).contains(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoKarRuntimeDependencyIsABootKar() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "test";
        final String version = "version";
        final String scope = "runtime";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        //when
        executeMojo();
        //then
        assertThat(builder.getAllKarsForStage(Boot)).containsOnly(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoKarProvidedDependencyIsAnInstalledKar() throws Exception {
        //given
        final String groupId = "org.apache.karaf.features";
        final String artifactId = "test";
        final String version = "version";
        final String scope = "provided";
        final String type = "kar";
        final String classifier = "";
        final DefaultArtifactHandlerStub artifactHandler = new DefaultArtifactHandlerStub(type, classifier);
        final DefaultArtifact dependency =
                new DefaultArtifact(groupId, artifactId, version, scope, type, classifier, artifactHandler);
        dependencyArtifacts.add(dependency);
        //when
        executeMojo();
        //then
        assertThat(builder.getAllKarsForStage(Installed)).contains(
                String.format("mvn:%s/%s/%s/%s", groupId, artifactId, version, type));
    }

    @Test
    public void executeMojoWhenStartupFeaturesIncludesFrameworkThenAddFrameworkAsKar() throws Exception {
        //given
        mojo.setStartupFeatures(Arrays.asList("framework", "other feature"));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllFeaturesForStage(Startup)).contains("framework", "other feature");
        final List<String> startupKars = builder.getAllKarsForStage(Startup);
        assertThat(startupKars).hasSize(1);
        assertThat(startupKars.get(0)).startsWith("mvn:org.apache.karaf.features/framework/");
    }

    @Test
    public void executeMojoWhenStartupFeaturesDoesNotIncludeFrameworkThenInjectAsMandatory() throws Exception {
        //given
        mojo.setStartupFeatures(Collections.singletonList("other feature"));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllFeaturesForStage(Startup)).containsExactlyInAnyOrder("framework", "other feature");
    }

    @Test
    public void executeMojoWithProfilesShouldNotAddAllRepositories() throws Exception {
        //given
        mojo.setProfilesUri("url");
        mojo.setStartupRepositories(Collections.emptyList());
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        mojo.setBootProfiles(Collections.singletonList("boot profile"));
        mojo.setInstalledProfiles(Collections.singletonList("installed profile"));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllRepositoriesForStage(Startup)).isEmpty();
        assertThat(builder.getAllProfilesForStage(Startup)).containsExactly("startup profile");
        assertThat(builder.getAllProfilesForStage(Boot)).containsExactly("boot profile");
        assertThat(builder.getAllProfilesForStage(Installed)).containsExactly("installed profile");
    }

    @Test
    public void executeMojoWithFeaturesShouldNotAddAllRepositories() throws Exception {
        //given
        mojo.setStartupFeatures(Collections.singletonList("value"));
        mojo.setBootFeatures(Collections.singletonList("value"));
        mojo.setInstalledFeatures(Collections.singletonList("value"));
        //when
        executeMojo();
        //then
        assertThat(builder.getAllRepositoriesForStage(Startup)).isEmpty();
        assertThat(builder.getAllRepositoriesForStage(Boot)).isEmpty();
        assertThat(builder.getAllRepositoriesForStage(Builder.Stage.Installed)).isEmpty();
    }

    @Test
    public void executeMojoWhenNotInstallAllFeatureByDefault() throws Exception {
        //given
        mojo.setInstallAllFeaturesByDefault(false);
        //when
        executeMojo();
        //then
        assertThat(builder.getAllRepositoriesForStage(Startup)).isEmpty();
        assertThat(builder.getAllRepositoriesForStage(Boot)).isEmpty();
        assertThat(builder.getAllRepositoriesForStage(Builder.Stage.Installed)).isEmpty();
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
        executeMojo();
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
        executeMojo();
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
        executeMojo();
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
        executeMojo();
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
        executeMojo();
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
        executeMojo();
        //then
        final File executableFile = new File(new File(workDirectory, "bin"), "batch.bat");
        assertThat(executableFile.canExecute()).isFalse();
    }

    @Test
    public void executeMojoRethrowsMojoExecutionExceptions() throws Exception {
        //given
        final AssemblyMojoExec mojoExec = mock(AssemblyMojoExec.class);
        mojo.setMojoExec(mojoExec);
        mojoExec.doExecute(mojo);
        expectLastCall().andThrow(new MojoExecutionException(""));
        replayAll();
        exception.expect(MojoExecutionException.class);
        //when
        executeMojo();
        //then
        verifyAll();
    }

    @Test
    public void executeMojoRethrowsMojoFailureExceptions() throws Exception {
        //given
        final AssemblyMojoExec mojoExec = mock(AssemblyMojoExec.class);
        mojo.setMojoExec(mojoExec);
        mojoExec.doExecute(mojo);
        expectLastCall().andThrow(new MojoFailureException(""));
        replayAll();
        exception.expect(MojoFailureException.class);
        //when
        executeMojo();
        //then
        verifyAll();
    }

    @Test
    public void executeMojoHandlesIOExceptionReadingProfileEdits() throws Exception {
        //given
        givenPropertyEditsExist();
        expect(profileEditsReader.read(anyObject(InputStream.class), eq(true))).andThrow(new IOException());
        withDefaultLogging();
        replayAll();
        //when
        executeMojo();
        //then
        assertThat(builder.isPropertyEditsCalled()).isFalse();
        verifyAll();
    }

    @Test
    public void executeMojoHandlesXMLStreamExceptionReadingProfileEdits() throws Exception {
        //given
        givenPropertyEditsExist();
        expect(profileEditsReader.read(anyObject(FileInputStream.class), eq(true))).andThrow(new XMLStreamException());
        withDefaultLogging();
        replayAll();
        //when
        executeMojo();
        //then
        assertThat(builder.isPropertyEditsCalled()).isFalse();
        verifyAll();
    }

    private void withDefaultLogging() {
        log.info("Using repositories: default-url@id=default-id@noreleases@snapshots");
        log.info("Creating work directory");
        log.info("Loading kar and features repositories dependencies");
        expectLastCall();
    }

}
