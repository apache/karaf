package org.apache.karaf.tooling.assembly;

import org.apache.maven.plugin.testing.resources.TestResources;
import org.apache.maven.project.MavenProject;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AssemblyOutfitter}.
 */
public class AssemblyOutfitterTest extends EasyMockSupport {

    private static final String TEST_PROJECT = "assembly-execute-mojo";

    @Rule
    public EasyMockRule rule = new EasyMockRule(this);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Rule
    public TestResources resources = new TestResources();

    private AssemblyOutfitter subject;

    private AssemblyMojo mojo = new AssemblyMojo();

    @Mock
    private ExecutableFile executableFile;

    private File sourceDirectory;

    private File workDirectory;

    private File outputDirectory;

    private MavenProject mavenProject;


    @Before
    public void setUp() throws Exception {
        subject = new AssemblyOutfitter(mojo, executableFile);
        mavenProject = AssemblyMother.getProject(TEST_PROJECT, resources, Collections.emptySet());
        mojo.setProject(mavenProject);
    }

    @Test
    public void sourceFilesCopiedToWork() throws IOException {
        //given
        sourceDirExists();
        workDirExists();
        folder.newFile("source/source-file-to-copy");
        //when
        subject.outfit();
        //then
        checkThatFilesInSourceWasCopiedToWork(sourceDirectory);
    }

    private void workDirExists() throws IOException {
        workDirectory = folder.newFolder("work");
        mojo.setWorkDirectory(workDirectory);
    }

    private void sourceDirExists() throws IOException {
        sourceDirectory = folder.newFolder("source");
        mojo.setSourceDirectory(sourceDirectory);
    }

    private void checkThatFilesInSourceWasCopiedToWork(final File source) {
        assertThat(workDirectory.list()).containsAll(Arrays.asList(source.list()));
    }

    @Test
    public void sourceFileDoesNotExists() throws IOException {
        //given
        sourceDirMissing();
        workDirExists();
        //when
        subject.outfit();
        //then
        checkThatWorkDirIsEmpty();
    }

    private void sourceDirMissing() throws IOException {
        sourceDirExists();
        Assume.assumeTrue("Delete temp directory", sourceDirectory.delete());
    }

    private void checkThatWorkDirIsEmpty() {
        assertThat(workDirectory.list()).isEmpty();
    }

    @Test
    public void includeBuildOutput() throws Exception {
        //given
        sourceDirExists();
        workDirExists();
        outputDirectoryExists();
        folder.newFile("output/output-file-to-copy");
        mojo.setIncludeBuildOutputDirectory(true);
        //when
        subject.outfit();
        //then
        checkThatFilesInSourceWasCopiedToWork(outputDirectory);
    }

    private void outputDirectoryExists() throws IOException {
        outputDirectory = folder.newFolder("output");
        mavenProject.getBuild()
                    .setOutputDirectory(String.valueOf(outputDirectory));

    }

    @Test
    public void markBinFilesAsExecutable() throws Exception {
        //given
        workDirExists();
        //assuming
        AssemblyMother.assumePosixFilesystem(workDirectory.toPath());
        //given
        sourceDirExists();
        final File binDirectory = binDirExists();
        final String fileBin = "file.bin";
        final String fileBat = "file.bat";
        final File binFile = fileExists(binDirectory, fileBin);
        final File batFile = fileExists(binDirectory, fileBat);
        executableFile.make(binFile.toPath());
        replayAll();
        //when
        subject.outfit();
        //then
        verifyAll();
    }

    private File fileExists(final File binDirectory, final String filename) throws IOException {
        final File binFile = new File(binDirectory, filename);
        assertThat(binFile.createNewFile()).isTrue();
        return binFile;
    }

    private File binDirExists() {
        final File binDirectory = new File(workDirectory, "bin");
        assertThat(binDirectory.mkdir()).isTrue();
        return binDirectory;
    }

}
