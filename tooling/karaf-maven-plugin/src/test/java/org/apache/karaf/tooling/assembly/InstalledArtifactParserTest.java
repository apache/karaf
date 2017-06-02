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
 * Tests for {@link InstalledArtifactParser}.
 */
public class InstalledArtifactParserTest extends EasyMockSupport {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    private InstalledArtifactParser subject;

    @Mock
    private Builder builder;

    private AssemblyMojo mojo = new AssemblyMojo();

    private ArtifactLists artifactLists = new ArtifactLists();

    @Before
    public void setUp() throws Exception {
        subject = new InstalledArtifactParser();
        expect(builder.defaultStage(Builder.Stage.Installed)).andReturn(builder);
        mojo.setInstallAllFeaturesByDefault(false);
        artifactLists.getInstalledKars()
                     .addAll(Arrays.asList("kar 1", "kar 2"));
        expect(builder.kars("kar 1", "kar 2")).andReturn(builder);
        artifactLists.addInstalledBundles(Arrays.asList("bundle 1", "bundle 2"));
        expect(builder.bundles("bundle 1", "bundle 2")).andReturn(builder);
    }

    @Test
    public void parse() {
        //given
        withFeatures();
        withProfiles();
        withRepositories();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withRepositories() {
        artifactLists.addInstalledRepositories(Arrays.asList("repo 1", "repo 2"));
        expect(builder.repositories(false, "repo 1", "repo 2")).andReturn(builder);
    }

    private void withProfiles() {
        mojo.setInstalledProfiles(Arrays.asList("profile 1", "profile 2"));
        expect(builder.profiles("profile 1", "profile 2")).andReturn(builder);
    }

    private void withFeatures() {
        mojo.setInstalledFeatures(Arrays.asList("feature 1", "feature 2"));
        expect(builder.features("feature 1", "feature 2")).andReturn(builder);
    }

    @Test
    public void parseWithNoFeatures() {
        //given
        withNoFeatures();
        withProfiles();
        withRepositories();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withNoFeatures() {
        mojo.setInstalledFeatures(Collections.emptyList());
        expect(builder.features()).andReturn(builder);
    }

    @Test
    public void parseWithNoFeaturesOrProfiles() {
        //given
        withNoFeatures();
        withNoProfiles();
        withRepositories();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withNoProfiles() {
        mojo.setInstalledProfiles(Collections.emptyList());
        expect(builder.profiles()).andReturn(builder);
    }

    @Test
    public void parseWithNoFeaturesOrProfilesAndAllFeatureByDefault() {
        //given
        withNoFeatures();
        withNoProfiles();
        withAllFeaturesByDefault();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withAllFeaturesByDefault() {
        mojo.setInstallAllFeaturesByDefault(true);
        artifactLists.addInstalledRepositories(Arrays.asList("repo 1", "repo 2"));
        expect(builder.repositories(true, "repo 1", "repo 2")).andReturn(builder);
    }

}
