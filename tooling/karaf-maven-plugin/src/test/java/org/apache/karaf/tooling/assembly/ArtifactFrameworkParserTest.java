package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.startsWith;

/**
 * Tests for {@link ArtifactFrameworkParser}.
 */
public class ArtifactFrameworkParserTest extends EasyMockSupport {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    private ArtifactFrameworkParser subject;

    @Mock
    private Builder builder;

    private AssemblyMojo mojo = new AssemblyMojo();

    private ArtifactLists artifactLists = new ArtifactLists();

    @Before
    public void setUp() throws Exception {
        subject = new ArtifactFrameworkParser();
    }

    @Test
    public void parseWithFramework() {
        //given
        withFramework();
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withFramework() {
        final String framework = "framework";
        mojo.setFramework(framework);
        expect(builder.kars(eq(Builder.Stage.Startup), eq(false),
                            startsWith("mvn:org.apache.karaf.features/framework/")
                           )).andReturn(builder);
    }

    @Test
    public void parseWithUnknownFramework() {
        //should
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("Unsupported framework: unsupported");
        //given
        withUnknownFramework("unsupported");
        //when
        subject.parse(builder, mojo, artifactLists);
    }

    private void withUnknownFramework(final String framework) {
        mojo.setFramework(framework);
    }

    @Test
    public void parseWithDynamicFrameworkInStartupKar() {
        //given
        withFrameworkInStartupKars("mvn:org.apache.karaf.features/framework/");
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    private void withFrameworkInStartupKars(final String mvnUri) {
        artifactLists.getStartupKars()
                     .add(mvnUri);
        expect(builder.kars(Builder.Stage.Startup, false, mvnUri)).andReturn(builder);
    }

    @Test
    public void parseWithStaticFrameworkInStartupKar() {
        //given
        withFrameworkInStartupKars("mvn:org.apache.karaf.features/static/");
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

    @Test
    public void parseWithNonFrameworkInStartupKar() {
        //given
        withFramework();
        final String mvnUri = "mvn:org.apache.karaf.features/other/";
        artifactLists.getStartupKars()
                     .add(mvnUri);
        replayAll();
        //when
        subject.parse(builder, mojo, artifactLists);
        //then
        verifyAll();
    }

}
