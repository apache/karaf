package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tooling.utils.IoUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Finishes outfitting the generated assembly, adding project specific files and setting permissions.
 */
class AssemblyOutfitter {

    private static final Set<PosixFilePermission> EXECUTABLE_PERMISSIONS = PosixFilePermissions.fromString("rwxr-xr-x");

    private final AssemblyMojo mojo;

    AssemblyOutfitter(final AssemblyMojo mojo) {
        this.mojo = mojo;
    }

    void outfit() throws IOException {
        addProjectBuildOutputToAssembly();
        overlayAssemblyFromProjectFiles();
        markAssemblyBinFilesAsExecutable();
    }

    private void addProjectBuildOutputToAssembly() throws IOException {
        if (mojo.getIncludeBuildOutputDirectory()) {
            IoUtils.copyDirectory(new File(mojo.getProject()
                                               .getBuild()
                                               .getOutputDirectory()), mojo.getWorkDirectory());
        }
    }

    private void overlayAssemblyFromProjectFiles() throws IOException {
        if (mojo.getSourceDirectory()
                .exists()) {
            IoUtils.copyDirectory(mojo.getSourceDirectory(), mojo.getWorkDirectory());
        }
    }

    private void markAssemblyBinFilesAsExecutable() {
        whereIsPosix(mojo.getWorkDirectory()).map(workDirectory -> new File(workDirectory, "bin"))
                                             .map(binDirectory -> binDirectory.listFiles(nonBatchFiles()))
                                             .map(Stream::of)
                                             .ifPresent(files -> files.map(File::getAbsolutePath)
                                                                      .map(Paths::get)
                                                                      .forEach(this::setFilePermissions));
    }

    private Optional<File> whereIsPosix(final File directory) {
        return directory.toPath()
                        .getFileSystem()
                        .supportedFileAttributeViews()
                        .stream()
                        .filter("posix"::matches)
                        .findAny()
                        .map(v -> directory);
    }

    private FileFilter nonBatchFiles() {
        return pathname -> !pathname.toString()
                                    .endsWith(".bat");
    }

    private void setFilePermissions(final Path filename) {
        try {
            Files.setPosixFilePermissions(filename, EXECUTABLE_PERMISSIONS);
        } catch (IOException e) {
            // non-posix filesystem should never have gotten this far
        }
    }

}
