package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

/**
 * Tests for {@link ArtifactParser}.
 */
public class ArtifactParserTest extends EasyMockSupport {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public TestResources resources = new TestResources();

    private ArtifactParser subject;

    @Mock
    private MavenUriParser mavenUriParser;

    @Mock
    private Builder builder;

    @Mock
    private ArtifactFrameworkParser frameworkParser;

    @Mock
    private StartupArtifactParser startupArtifactParser;

    @Mock
    private BootArtifactParser bootArtifactParser;

    @Mock
    private InstalledArtifactParser installedArtifactParser;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Before
    public void setUp() throws Exception {
        subject =
                new ArtifactParser(mavenUriParser, builder, frameworkParser, startupArtifactParser, bootArtifactParser,
                                   installedArtifactParser
                );
    }

    @Test
    public void okay() throws IOException {
        //given
        mojo.setStartupBundles(Arrays.asList("startup bundle 1", "startup bundle 2"));
        mojo.setBootBundles(Arrays.asList("boot bundle 1", "boot bundle 2"));
        mojo.setInstalledBundles(Arrays.asList("installed bundle 1", "installed bundle 2"));
        mojo.setStartupRepositories(Arrays.asList("startup repository 1", "startup repository 2"));
        mojo.setBootRepositories(Arrays.asList("boot repository 1", "boot repository 2"));
        mojo.setInstalledRepositories(Arrays.asList("installed repository 1", "installed repository 2"));
        mojo.setLibraries(Arrays.asList("library 1", "library 2"));

        final Artifact artifactKar = AssemblyMother.createArtifact("kar", "compile", "kar", "");
        final Artifact artifactRepositoryFeatures =
                AssemblyMother.createArtifact("repo features", "compile", "jar", "features");
        final Artifact artifactRepositoryKaraf = AssemblyMother.createArtifact("repo karaf", "compile", "jar", "karaf");
        final Artifact artifactJar = AssemblyMother.createArtifact("jar", "compile", "jar", "");
        final Artifact artifactBundle = AssemblyMother.createArtifact("bundle", "compile", "bundle", "");
        final Artifact artifactOtherScope = AssemblyMother.createArtifact("other scope", "other", "jar", "");
        final Artifact artifactOtherType = AssemblyMother.createArtifact("other type", "compile", "other", "");

        final Set<Artifact> artifacts =
                Stream.of(artifactKar, artifactRepositoryFeatures, artifactRepositoryKaraf, artifactJar, artifactBundle,
                          artifactOtherScope, artifactOtherType
                         )
                      .collect(Collectors.toSet());
        mojo.setProject(AssemblyMother.getProject(TEST_PROJECT, resources, artifacts));
        frameworkParser.parse(anyObject(Builder.class), eq(mojo), anyObject(ArtifactLists.class));
        startupArtifactParser.parse(anyObject(Builder.class), eq(mojo), anyObject(ArtifactLists.class));
        bootArtifactParser.parse(anyObject(Builder.class), eq(mojo), anyObject(ArtifactLists.class));
        installedArtifactParser.parse(anyObject(Builder.class), eq(mojo), anyObject(ArtifactLists.class));
        expectLastCall();
        expect(builder.libraries("library 1", "library 2")).andReturn(builder);
        expect(mavenUriParser.artifactToMvnUri(artifactJar)).andReturn("jar artifact uri");
        expect(mavenUriParser.artifactToMvnUri(artifactBundle)).andReturn("bundle artifact uri");
        expect(mavenUriParser.artifactToMvnUri(artifactKar)).andReturn("kar artifact uri");
        expect(mavenUriParser.artifactToMvnUri(artifactRepositoryFeatures)).andReturn("repo features artifact uri");
        expect(mavenUriParser.artifactToMvnUri(artifactRepositoryKaraf)).andReturn("repo karaf artifact uri");
        replayAll();
        //when
        subject.parse(mojo);
        //then
        verifyAll();
    }

}
