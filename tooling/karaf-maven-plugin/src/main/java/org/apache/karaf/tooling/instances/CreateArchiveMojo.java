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
package org.apache.karaf.tooling.instances;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.ArchiveFileSet;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.tools.ant.util.FileUtils;

/**
 * Package a server archive from an assembled server
 *
 * @goal instance-create-archive
 * @phase package
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Package a server archive from an assembled server
 * @threadSafe
 */
public class CreateArchiveMojo extends MojoSupport {

    /**
     * The target directory of the project.
     *
     * @parameter default-value="${project.build.directory}"
     * @required
     * @readonly
     */
    private File destDir;

    /**
     * The location of the server repository.
     *
     * @parameter default-value="${project.build.directory}/assembly"
     * @required
     */
    private File targetServerDirectory;

    /**
     * The target file to set as the project's artifact.
     *
     * @parameter default-value="${project.artifactId}-${project.version}"
     * @required
     */
    private File targetFile;

    /**
     * pack a assembly as a tar.gz archive
     *
     * @parameter
     */
    private boolean archiveTarGz = true;

    /**
     * pack a assembly as a zip archive
     *
     * @parameter
     */
    private boolean archiveZip = true;

    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Preparing assembly");
        File outputDirectory = new File(this.project.getBuild().getOutputDirectory());
        if (outputDirectory.exists()) {
            try {
                org.apache.commons.io.FileUtils.copyDirectory(outputDirectory, targetServerDirectory);
            } catch (Exception e) {
                throw new MojoExecutionException("Can't prepare assembly", e);
            }
        }
        getLog().debug("Setting artifact file: " + targetFile);
        org.apache.maven.artifact.Artifact artifact = project.getArtifact();
        artifact.setFile(targetFile);
        try {
            //now pack up the server.
            if (archiveTarGz) {
                archive("tar.gz");
            }
            if (archiveZip) {
                archive("zip");
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not archive plugin", e);
        }
    }

    @SuppressWarnings("deprecation")
    private void archive(String type) throws IOException {
        Artifact artifact1 = factory.createArtifactWithClassifier(project.getArtifact().getGroupId(), project.getArtifact().getArtifactId(), project.getArtifact().getVersion(), type, "bin");
        File target1 = archive(targetServerDirectory, destDir, artifact1);
        projectHelper.attachArtifact(project, artifact1.getType(), null, target1);
    }

    public File archive(File source, File dest, Artifact artifact) throws //ArchiverException,
            IOException {
        String serverName = null;
        if (targetFile != null && project.getPackaging().equals("karaf-assembly")) {
            serverName = targetFile.getName();
        } else {
            serverName = artifact.getArtifactId() + "-" + artifact.getVersion();
        }
        dest = new File(dest, serverName + "." + artifact.getType());
        Archiver archiver;
        if ("tar.gz".equals(artifact.getType())) {
            archiver = new TarArchiver(dest);
        } else if ("zip".equals(artifact.getType())) {
            archiver = new ZipArchiver(dest);
        } else {
            throw new IllegalArgumentException("Unknown target type: " + artifact.getType());
        }
        populateArchive(archiver, source, serverName);
        return dest;
    }

    private <T extends ArchiveFileSet> void populateArchive(Archiver<T> archiver, File source, String serverName) {

        System.out.println("Source " + source.getAbsolutePath());

        Project project = new Project();
        T fileSet = archiver.createFileSet();
        fileSet.setDir(source);
        fileSet.setPrefix(serverName);
        fileSet.setProject(project);
        fileSet.setExcludes("bin/");
        archiver.add(fileSet);

        fileSet = archiver.createFileSet();
        fileSet.setDir(source);
        fileSet.setPrefix(serverName);
        fileSet.setProject(project);
        fileSet.setIncludes("bin/");
        fileSet.setExcludes("bin/*.bat");
        fileSet.setFileMode("755");
        archiver.add(fileSet);

        fileSet = archiver.createFileSet();
        fileSet.setDir(source);
        fileSet.setPrefix(serverName);
        fileSet.setProject(project);
        fileSet.setIncludes("bin/*.bat");
        archiver.add(fileSet);

        MatchingTask task = archiver.getTask();
        task.setProject(project);
        task.execute();

    }

    private class ZipArchiver implements Archiver<ZipFileSet> {

        private final Zip zip;

        public ZipArchiver(File dest) {
            zip = new Zip();
            zip.setDestFile(dest);
        }

        @Override
        public ZipFileSet createFileSet() {
            return new ZipFileSet();
        }

        @Override
        public void add(ZipFileSet fileSet) {
            zip.add(fileSet);
        }

        @Override
        public MatchingTask getTask() {
            return zip;
        }
    }

    private class TarArchiver implements Archiver<TarFileSet> {

        private final Tar tar;

        public TarArchiver(File dest) {
            tar = new Tar();
            Tar.TarCompressionMethod tarCompressionMethod = new Tar.TarCompressionMethod();
            tarCompressionMethod.setValue("gzip");
            tar.setCompression(tarCompressionMethod);
            Tar.TarLongFileMode fileMode = new Tar.TarLongFileMode();
            fileMode.setValue(Tar.TarLongFileMode.GNU);
            tar.setLongfile(fileMode);
            tar.setDestFile(dest);
        }

        @Override
        public TarFileSet createFileSet() {
            return new TarFileSet();
        }

        @Override
        public void add(TarFileSet fileSet) {
            tar.add(fileSet);
        }

        @Override
        public MatchingTask getTask() {
            return tar;
        }
    }

    private interface Archiver<T extends ArchiveFileSet> {

        MatchingTask getTask();

        T createFileSet();

        void add(T fileSet);
    }

}
