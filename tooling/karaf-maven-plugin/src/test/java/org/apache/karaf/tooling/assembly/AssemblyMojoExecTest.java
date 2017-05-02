package org.apache.karaf.tooling.assembly;

import org.apache.karaf.profile.assembly.Builder;
import org.apache.maven.plugin.logging.Log;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;

/**
 * Tests for {@link AssemblyMojoExec}.
 */
public class AssemblyMojoExecTest extends EasyMockSupport {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private Log log;

    @Mock
    private Builder builder;

    @Mock
    private BuilderConfiguration builderConfiguration;

    @Mock
    private AssemblyOutfitter assemblyOutfitter;

    private AssemblyMojoExec mojoExec;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Before
    public void setUp() throws Exception {
        mojo.setWorkDirectory(folder.newFolder());
        mojoExec = new AssemblyMojoExec(log, builder, builderConfiguration, assemblyOutfitter);
    }

    @Test
    public void doExecuteCallsDependencies() throws Exception {
        //should
        builderConfiguration.configure(builder, mojo);
        builder.generateAssembly();
        assemblyOutfitter.outfit();
        replayAll();
        //when
        mojoExec.doExecute(mojo);
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
        mojoExec.doExecute(mojo);
    }

    @Test
    public void shouldBeOkayWhenProfileUriSuppliedAndNoProfileIsUsed() throws Exception {
        //given
        mojo.setProfilesUri("profiles uri");
        //when
        mojoExec.doExecute(mojo);
    }

    @Test
    public void shouldBeOkayWhenProfileUriSuppliedAndProfilesAreUsed() throws Exception {
        //given
        mojo.setStartupProfiles(Collections.singletonList("startup profile"));
        mojo.setProfilesUri("profiles uri");
        //when
        mojoExec.doExecute(mojo);
    }

    @Test
    public void shouldUpdateDeprecatedConfigurationWhenUsingFeatureRepositories() throws Exception {
        //given
        mojo.setFeatureRepositories(Collections.singletonList("feature repository"));
        //should
        log.warn("Use of featuresRepositories is deprecated");
        //when
        mojoExec.doExecute(mojo);
    }

}
