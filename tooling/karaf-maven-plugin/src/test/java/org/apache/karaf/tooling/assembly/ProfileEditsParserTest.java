package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tools.utils.model.KarafPropertyEdits;
import org.apache.karaf.tools.utils.model.io.stax.KarafPropertyInstructionsModelStaxReader;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

/**
 * Tests for {@link ProfileEditsParser}.
 */
public class ProfileEditsParserTest extends EasyMockSupport {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public TestResources resources = new TestResources();

    private ProfileEditsParser subject;

    @Mock
    private KarafPropertyInstructionsModelStaxReader profileEditsReader;

    private AssemblyMojo mojo = new AssemblyMojo();

    private File projectDir;

    @Before
    public void setUp() throws IOException {
        subject = new ProfileEditsParser(profileEditsReader);
        projectDir = resources.getBasedir(TEST_PROJECT);
    }

    @Test
    public void parseNormalFile() throws Exception {
        //given
        givenPropertyEditsExist();
        final KarafPropertyEdits edits = new KarafPropertyEdits();
        //should
        expect(profileEditsReader.read(anyObject(InputStream.class), eq(true))).andReturn(edits);
        replayAll();
        //when
        final Optional<KarafPropertyEdits> parse = subject.parse(mojo);
        //then
        verifyAll();
        assertThat(parse).contains(edits);
    }

    private void givenPropertyEditsExist() throws IOException {
        final String propertyFileEdits =
                new File(resources.getBasedir(TEST_PROJECT), "property-file-edits").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
    }

    @Test
    public void parseMissingFile() throws Exception {
        //given
        givenPropertyEditsFileDoesNotExist();
        //should
        replayAll();
        //when
        final Optional<KarafPropertyEdits> parse = subject.parse(mojo);
        //then
        verifyAll();
        assertThat(parse).isEmpty();
    }

    private void givenPropertyEditsFileDoesNotExist() throws IOException {
        final String propertyFileEdits = new File(resources.getBasedir(TEST_PROJECT), "missing-file").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
    }

    @Test
    public void parseFileIsDirectory() throws Exception {
        //should
        exception.expect(RuntimeException.class);
        exception.expectCause(CoreMatchers.isA(FileNotFoundException.class));
        //given
        givenPropertyEditsFileIsDirectory();
        //when
        subject.parse(mojo);
    }

    private void givenPropertyEditsFileIsDirectory() throws IOException {
        final String propertyFileEdits = new File(resources.getBasedir(TEST_PROJECT), "output").getAbsolutePath();
        mojo.setPropertyFileEdits(propertyFileEdits);
    }

    @Test
    public void parseFileReadThrowsIOException() throws Exception {
        //should
        expect(profileEditsReader.read(EasyMock.isA(InputStream.class), eq(true))).andThrow(
                new IOException("io-exception"));
        replayAll();
        //given
        givenPropertyEditsExist();
        //when
        final Optional<KarafPropertyEdits> parse = subject.parse(mojo);
        //then
        verifyAll();
        assertThat(parse).isEmpty();
    }

}
