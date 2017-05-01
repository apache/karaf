package org.apache.karaf.tooling.assembly;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ExecutableFile}.
 */
@Ignore
public class ExecutableFileMockitoTest {

    @Test
    public void setExecutableIgnoresIOException() throws Exception {
        //given
        final ExecutableFile executableFile = new ExecutableFile();
        final String filename = "/dev/null";
        //when
        executableFile.make(Paths.get(filename));
        //then
        assertThat(true).as("No exception thrown")
                        .isTrue();
    }

}
