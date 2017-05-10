package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.easymock.EasyMock.contains;
import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.core.Is.isA;

/**
 * Tests for {@link AssemblyMojo}.
 */
public class AssemblyMojoTest extends EasyMockSupport {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public TestResources resources = new TestResources();

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Mock
    private Log log;

    private Builder builder;

    @Mock
    private BuilderConfiguration builderConfiguration;

    @Mock
    private AssemblyOutfitter assemblyOutfitter;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Before
    public void setUp() throws Exception {
        mojo.setLog(log);
        builder = new BuilderSpy();
        mojo.setBuilder(builder);
        givenBasicConfiguration();
    }

    private void givenBasicConfiguration() throws IOException {
        mojo.setSourceDirectory(new File(AssemblyMother.getProjectDirectory(TEST_PROJECT, resources), "source"));
        final MavenProject mavenProject = AssemblyMother.getProject(TEST_PROJECT, resources, Collections.emptySet());
        mojo.setProject(mavenProject);
        mojo.setMavenSession(AssemblyMother.getSession(mavenProject, mojoRule));
        mojo.setLocalRepo(AssemblyMother.createArtifactRepository());
        mojo.setJavase("javase version");
        mojo.setConfig(Collections.singletonMap("config key", "config value"));
        mojo.setSystem(Collections.singletonMap("system key", "system value"));
        mojo.setBlacklistedBundles(Collections.singletonList("blacklist bundle"));
        mojo.setBlacklistedFeatures(Collections.singletonList("blacklist feature"));
        mojo.setBlacklistedProfiles(Collections.singletonList("blacklist profile"));
        mojo.setBlacklistedRepositories(Collections.singletonList("blacklist repository"));
        mavenProject.setRemoteArtifactRepositories(
                Collections.singletonList(AssemblyMother.createArtifactRepository()));
        mojo.setWorkDirectory(new File(AssemblyMother.getProjectDirectory(TEST_PROJECT, resources), "output"));
        mojo.setFramework("framework");
    }

    @Test
    public void shouldExecuteOkay() throws Exception {
        //given
        log.info(contains("Using repositories: " + "default-url@id=default-id"));
        expectLastCall();
        log.info("Creating work directory");
        expectLastCall();
        log.info("Loading kar and features repositories dependencies");
        expectLastCall();
        //should
        replayAll();
        //when
        doExecuteMojo();
        verifyAll();
    }

    private void doExecuteMojo() throws Exception {
        mojo.execute();
    }

    @Test
    public void executeShouldRethrowMojoExecutionException() throws Exception {
        //should
        exception.expect(MojoExecutionException.class);
        exception.expectMessage("exception message");
        //given
        builderThrows(new MojoExecutionException("exception message"));
        //when
        doExecuteMojo();
    }

    public void builderThrows(final Exception exceptionToThrow) {
        ((BuilderSpy) builder).willThrow(exceptionToThrow);
    }

    @Test
    public void executeShouldRethrowMojoFailureException() throws Exception {
        //should
        exception.expect(MojoFailureException.class);
        exception.expectMessage("exception message");
        //given
        builderThrows(new MojoFailureException("exception message"));
        //when
        doExecuteMojo();
    }

    @Test
    public void executeShouldWrapOtherExceptionsInMojoExecutionException() throws Exception {
        //should
        exception.expect(MojoExecutionException.class);
        exception.expectMessage("Unable to build assembly");
        exception.expectCause(isA(IOException.class));
        //given
        builderThrows(new IOException("exception message"));
        //when
        doExecuteMojo();
    }

    @Test
    public void doExecuteCallsDependencies() throws Exception {
        //given
        builder = mock(Builder.class);
        mojo.setBuilderConfiguration(builderConfiguration);
        mojo.setBuilder(builder);
        mojo.setAssemblyOutfitter(assemblyOutfitter);
        //should
        builderConfiguration.configure(builder, mojo);
        builder.generateAssembly();
        assemblyOutfitter.outfit();
        replayAll();
        //when
        mojo.doExecute();
        verifyAll();
    }

    @Test
    public void throwExceptionWhenProfileUriMissingAndProfileIsUsed() throws Exception {
        //given
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        //should
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("profilesDirectory must be specified");
        //when
        mojo.doExecute();
    }

    @Test
    public void shouldBeOkayWhenProfileUriSuppliedAndNoProfileIsUsed() throws Exception {
        //given
        mojo.setProfilesUri("profiles uri");
        //when
        doExecuteMojo();
    }

    @Test
    public void shouldBeOkayWhenProfileUriSuppliedAndProfilesAreUsed() throws Exception {
        //given
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        mojo.setProfilesUri("profiles uri");
        //when
        doExecuteMojo();
    }

    @Test
    public void shouldUpdateDeprecatedConfigurationWhenUsingFeatureRepositories() throws Exception {
        //given
        mojo.setFeatureRepositories(Collections.singletonList("feature repository"));
        //should
        log.warn("Use of featuresRepositories is deprecated");
        //when
        doExecuteMojo();
    }

}
