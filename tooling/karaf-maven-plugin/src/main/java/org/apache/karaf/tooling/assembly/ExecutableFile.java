package org.apache.karaf.tooling.assembly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * Set permissions on file to enable execution.
 */
class ExecutableFile {

    private static final Set<PosixFilePermission> EXECUTABLE_PERMISSIONS = PosixFilePermissions.fromString("rwxr-xr-x");

    void make(final Path filename) {
        try {
            Files.setPosixFilePermissions(filename, EXECUTABLE_PERMISSIONS);
        } catch (IOException e) {
            // ignored
        }
    }
}
