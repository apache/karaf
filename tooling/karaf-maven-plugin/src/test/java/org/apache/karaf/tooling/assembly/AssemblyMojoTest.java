package org.apache.karaf.tooling.assembly;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.easymock.EasyMock.expectLastCall;
import static org.hamcrest.core.Is.isA;

/**
 * Tests for {@link AssemblyMojo}.
 */
public class AssemblyMojoTest extends EasyMockSupport {

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Mock
    private AssemblyMojoExec mojoExec;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Before
    public void setUp() throws Exception {
        mojo.setMojoExec(mojoExec);
    }

    @Test
    public void shouldExecuteOkay() throws Exception {
        //should
        mojoExec.doExecute(mojo);
        replayAll();
        //when
        mojo.execute();
        verifyAll();
    }

    @Test
    public void executeShouldRethrowMojoExecutionException() throws Exception {
        //should
        exception.expect(MojoExecutionException.class);
        exception.expectMessage("exception message");
        mojoExec.doExecute(mojo);
        expectLastCall().andThrow(new MojoExecutionException("exception message"));
        replayAll();
        //when
        mojo.execute();
        verifyAll();
    }

    @Test
    public void executeShouldRethrowMojoFailureException() throws Exception {
        //should
        exception.expect(MojoFailureException.class);
        exception.expectMessage("exception message");
        mojoExec.doExecute(mojo);
        expectLastCall().andThrow(new MojoFailureException("exception message"));
        replayAll();
        //when
        mojo.execute();
        verifyAll();
    }

    @Test
    public void executeShouldWrapOtherExceptionsInMojoExecutionException() throws Exception {
        //should
        exception.expect(MojoExecutionException.class);
        exception.expectMessage("Unable to build assembly");
        exception.expectCause(isA(IOException.class));
        mojoExec.doExecute(mojo);
        expectLastCall().andThrow(new IOException("exception message"));
        replayAll();
        //when
        mojo.execute();
        verifyAll();
    }

}
