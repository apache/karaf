/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.instance.command;

import org.apache.karaf.instance.command.completers.InstanceCompleter;
import org.apache.karaf.instance.core.Instance;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Command(scope = "instance", name = "package", description = "Create a .zip archive of the instance.")
@Service
public class PackageCommand extends InstanceCommandSupport {

    @Argument(index = 0, name = "name", description = "The name of the container instance", required = true)
    @Completion(InstanceCompleter.class)
    private String name;
    @Argument(index = 1, name = "destination", description = "Destination path for the archive", required = true)
    private String destination;

    protected Object doExecute() throws Exception {
        Instance instance = getExistingInstance(name);
        Path location = Paths.get(instance.getLocation());
        Path destinationPath = Paths.get(destination);
        createZip(location, destinationPath);
        return "Archive available at " + destinationPath;
    }

    private void createZip(Path source, Path destination) throws IOException {
        Path normalizedDestination = destination.normalize();
        try (ZipOutputStream zOut = new ZipOutputStream(new FileOutputStream(destination.toString()))) {
            Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
                    if (attributes.isSymbolicLink()) {
                        return FileVisitResult.CONTINUE;
                    }

                    String entryName = source.relativize(file).toString();

                    if (Paths.get(entryName).normalize().equals(normalizedDestination)) {
                        // Prevent the zip from trying to zip itself when ran in the instance directory,
                        // e.g. when executing instance:package root ./archives/root.zip
                        return FileVisitResult.CONTINUE;
                    }

                    ZipEntry zipEntry = new ZipEntry(entryName);
                    System.out.println(entryName);
                    zOut.putNextEntry(zipEntry);
                    Files.copy(file, zOut);
                    zOut.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
            });
        }
    }
}
