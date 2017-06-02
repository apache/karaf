/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.tooling.assembly;

import org.apache.karaf.tooling.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Finishes outfitting the generated assembly, adding project specific files and setting permissions.
 */
class AssemblyOutfitter {

    private final AssemblyMojo mojo;

    private final ExecutableFile executableFile;

    AssemblyOutfitter(final AssemblyMojo mojo, final ExecutableFile executableFile) {
        this.mojo = mojo;
        this.executableFile = executableFile;
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
                                             .map(binDirectory -> binDirectory.listFiles(this::nonBatchFile))
                                             .map(Stream::of)
                                             .ifPresent(files -> files.map(File::getAbsolutePath)
                                                                      .map(Paths::get)
                                                                      .forEach(executableFile::make));
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

    private boolean nonBatchFile(final File pathname) {
        return !batchFile(pathname);
    }

    private boolean batchFile(final File pathname) {
        return pathname.toString()
                       .endsWith(".bat");
    }

}
