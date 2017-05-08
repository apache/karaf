package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.easymock.EasyMock.expect;

/**
 * Tests for {@link StartupArtifactParser}.
 */
public class StartupArtifactParserTest extends EasyMockSupport {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    private StartupArtifactParser subject;

    @Mock
    private Builder builder;

    private AssemblyMojo mojo = new AssemblyMojo();

    private ArtifactLists artifactLists = new ArtifactLists();

    @Before
    public void setUp() throws Exception {
        subject = new StartupArtifactParser();
        artifactLists.getStartupKars()
                     .addAll(Arrays.asList("kar 1", "kar 2"));
        artifactLists.addStartupRepositories(Collections.singletonList("repository"));
        artifactLists.addStartupBundles(Collections.singletonList("bundle"));
        expect(builder.defaultStage(Builder.Stage.Startup)).andReturn(builder);
        expect(builder.kars("kar 1", "kar 2")).andReturn(builder);
        expect(builder.bundles("bundle")).andReturn(builder);
        mojo.setInstallAllFeaturesByDefault(false);
    }

    @Test
    public void parseWithFramework() {
        //given
        withFrameworkInStartupFeatures();
        withProfile();
        withRepository();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withFrameworkInStartupFeatures() {
        final String framework = "framework";
        mojo.setFramework(framework);
        mojo.setStartupFeatures(Collections.singletonList(framework));
        expect(builder.features(framework)).andReturn(builder);
    }

    private void withProfile() {
        mojo.setStartupProfiles(Collections.singletonList("profile"));
        expect(builder.profiles("profile")).andReturn(builder);
    }

    private void withRepository() {
        expect(builder.repositories(false, "repository")).andReturn(builder);
    }

    @Test
    public void parseWithNoFramework() {
        //given
        withFrameworkNotInStartupFeatures();
        withProfile();
        withRepository();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withFrameworkNotInStartupFeatures() {
        final String framework = "framework";
        mojo.setFramework(framework);
        mojo.setStartupFeatures(Collections.emptyList());
        expect(builder.features(Builder.Stage.Startup, framework)).andReturn(builder);
        expect(builder.features()).andReturn(builder);
    }

    @Test
    public void parseWithNoFrameworkOrProfiles() {
        //given
        withFrameworkNotInStartupFeatures();
        withNoProfile();
        withRepository();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withNoProfile() {
        mojo.setStartupProfiles(Collections.emptyList());
        expect(builder.profiles()).andReturn(builder);
    }

    @Test
    public void parseWithNoFrameworkOrProfilesAndInstallByDefault() {
        //given
        withFrameworkNotInStartupFeatures();
        withNoProfile();
        withInstallAllFeaturesByDefault();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withInstallAllFeaturesByDefault() {
        mojo.setInstallAllFeaturesByDefault(true);
        expect(builder.repositories(true, "repository")).andReturn(builder);
    }

}
