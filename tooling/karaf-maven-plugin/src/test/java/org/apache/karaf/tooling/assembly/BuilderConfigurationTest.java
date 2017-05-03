package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
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

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import static org.easymock.EasyMock.contains;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests for {@link BuilderConfiguration}.
 */
public class BuilderConfigurationTest extends EasyMockSupport {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    private BuilderConfiguration subject;

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public MojoRule mojoRule = new MojoRule();

    @Rule
    public TestResources resources = new TestResources();

    @Mock
    private Log log;

    @Mock
    private MavenUriParser mavenUriParser;

    @Mock
    private ProfileEditsParser profileEditsParser;

    @Mock
    private ArtifactParser artifactParser;

    @Mock
    private Builder builder;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Before
    public void setUp() throws Exception {
        subject = new BuilderConfiguration(log, mavenUriParser, profileEditsParser, artifactParser);
    }

    @Test
    public void configureWithAProfilesUri() throws IOException, XMLStreamException {
        //given
        givenBasicConfiguration();
        final String profilesUri = "profiles uri";
        givenProfilesUri(profilesUri);
        replayAll();
        //when
        subject.configure(builder, mojo);
        //then
        verifyAll();
    }

    @Test
    public void configureWithNullProfilesUri() throws IOException, XMLStreamException {
        //given
        givenBasicConfiguration();
        final String profilesUri = null;
        givenProfilesUri(profilesUri);
        replayAll();
        //when
        subject.configure(builder, mojo);
        //then
        verifyAll();
    }

    private void givenBasicConfiguration() throws IOException {
        final MavenProject mavenProject = AssemblyMother.getProject(TEST_PROJECT, resources, Collections.emptySet());
        mojo.setProject(mavenProject);
        mojo.setMavenSession(AssemblyMother.getSession(mavenProject, mojoRule));
        mojo.setLocalRepo(AssemblyMother.createArtifactRepository());
        expect(builder.offline(false)).andReturn(builder);
        expect(builder.localRepository("/")).andReturn(builder);
        mojo.setJavase("javase version");
        expect(builder.javase("javase version")).andReturn(builder);
        mojo.setConfig(Collections.singletonMap("config key", "config value"));
        expect(builder.config("config key", "config value")).andReturn(builder);
        mojo.setSystem(Collections.singletonMap("system key", "system value"));
        expect(builder.system("system key", "system value")).andReturn(builder);
        expect(builder.karafVersion(Builder.KarafVersion.v4x)).andReturn(builder);
        expect(builder.useReferenceUrls(false)).andReturn(builder);
        expect(builder.defaultAddAll(true)).andReturn(builder);
        expect(builder.ignoreDependencyFlag(false)).andReturn(builder);
        final KarafPropertyEdits propertyEdits = new KarafPropertyEdits();
        expect(profileEditsParser.parse(mojo)).andReturn(Optional.of(propertyEdits));
        expect(builder.propertyEdits(propertyEdits)).andReturn(builder);
        expect(builder.pidsToExtract(Collections.singletonList("*"))).andReturn(builder);
        expect(builder.blacklistPolicy(Builder.BlacklistPolicy.Discard)).andReturn(builder);
        mojo.setBlacklistedBundles(Collections.singletonList("blacklist bundle"));
        expect(builder.blacklistBundles(Collections.singletonList("blacklist bundle"))).andReturn(builder);
        mojo.setBlacklistedFeatures(Collections.singletonList("blacklist feature"));
        expect(builder.blacklistFeatures(Collections.singletonList("blacklist feature"))).andReturn(builder);
        mojo.setBlacklistedProfiles(Collections.singletonList("blacklist profile"));
        expect(builder.blacklistProfiles(Collections.singletonList("blacklist profile"))).andReturn(builder);
        mojo.setBlacklistedRepositories(Collections.singletonList("blacklist repository"));
        expect(builder.blacklistRepositories(Collections.singletonList("blacklist repository"))).andReturn(builder);
        mavenProject.setRemoteArtifactRepositories(
                Collections.singletonList(AssemblyMother.createArtifactRepository()));
        final String removeRepositoryAsString = "default-url@id=default-id";
        log.info(contains("Using repositories: " + removeRepositoryAsString));
        expectLastCall();
        expect(builder.mavenRepositories(contains(removeRepositoryAsString))).andReturn(builder);
        expect(mavenUriParser.getTranslatedUris(mavenProject, null)).andReturn(new HashMap<>());
        expect(builder.translatedUrls(Collections.emptyMap())).andReturn(builder);
        log.info("Creating work directory");
        expectLastCall();
        final File workDirectory = new File(AssemblyMother.getProjectDirectory(TEST_PROJECT, resources), "output");
        mojo.setWorkDirectory(workDirectory);
        expect(builder.homeDirectory(workDirectory.toPath())).andReturn(builder);
        log.info("Loading kar and features repositories dependencies");
        expectLastCall();
        artifactParser.parse(mojo);
        expectLastCall();
    }

    private void givenProfilesUri(final String profilesUri) {
        mojo.setProfilesUri(profilesUri);
        if (profilesUri != null) {
            expect(builder.profilesUris(profilesUri)).andReturn(builder);
        }
    }

}
