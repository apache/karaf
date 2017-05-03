package org.apache.karaf.tooling.assembly;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutableFile}.
 */
public class ExecutableFileTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private ExecutableFile executableFile;

    @Before
    public void setUp() throws Exception {
        executableFile = new ExecutableFile();
    }

    @Test
    public void setExecutableOkay() throws Exception {
        //given
        //assume Posix filesystem
        final Path filePath = folder.newFile("executable-file")
                                    .toPath();
        AssemblyMother.assumePosixFilesystem(filePath);
        //when
        executableFile.make(filePath);
        //then
        assertThat(filePath).isExecutable();
    }

    @Test
    public void setExecutableIgnoresIOException() throws Exception {
        //given
        final String filename = "/dev/null";
        AssemblyMother.assumePosixFilesystem(Paths.get(filename));
        //when
        executableFile.make(Paths.get(filename));
        //then
        assertThat(true).as("No exception thrown")
                        .isTrue();
    }

}
