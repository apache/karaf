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
package org.apache.karaf.tooling;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Package a server archive from an assembled server
 */
@Mojo(name = "archive", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class ArchiveMojo extends MojoSupport {

    /**
     * The target directory of the project.
     */
    @Parameter(defaultValue="${project.build.directory}")
    private File destDir;

    /**
     * The location of the server repository.
     */
    @Parameter(defaultValue="${project.build.directory}/assembly")
    private File targetServerDirectory;

    /**
     * Path prefix of files in the created archive.
     */
    @Parameter(defaultValue="${project.artifactId}-${project.version}")
    private String pathPrefix;

    /**
     * Use a path prefix of files in the created archive
     */
    @Parameter
    private boolean usePathPrefix = true;

    /**
     * The target file to set as the project's artifact.
     */
    @Parameter(defaultValue="${project.artifactId}-${project.version}")
    private File targetFile;

    /**
     * pack a assembly as a tar.gz archive
     */
    @Parameter
    private boolean archiveTarGz = true;

    /**
     * pack a assembly as a zip archive
     */
    @Parameter
    private boolean archiveZip = true;

    /**
     * Whether to attach the resulting assembly to the project as an artifact.
     */
    @Parameter(defaultValue="true")
    private boolean attach = true;

    /**
     * If supplied, the classifier for the artifact when attached.
     */
    @Parameter
    private String classifier;

    /**
     * use symbolic links in tar.gz or zip archives
     *
     * Symbolic links are not very well supported by windows Platform.
     * At least, is does not work on WinXP + NTFS, so do not include them
     * for now. So the default is false.
     */
    @Parameter
    private boolean useSymLinks = false;

    public void execute() throws MojoExecutionException, MojoFailureException {
        org.apache.maven.artifact.Artifact artifact = project.getArtifact();
        artifact.setFile(targetFile);

        // abort if there are no archives to be created
        if (!archiveTarGz && !archiveZip) {
            return;
        }

        try {
            if (project.getPackaging().equals("karaf-assembly")) {
                if (archiveZip) {
                    archive("zip", false, true);
                    if (archiveTarGz) {
                        archive("tar.gz", true, false);
                    }
                } else {
                    archive("tar.gz", false, true);
                }
            } else {
                if (archiveTarGz) {
                    archive("tar.gz", true, false);
                }
                if (archiveZip) {
                    archive("zip", true, false);
                }
            }
        } catch (Exception e) {
            throw new MojoExecutionException("Could not archive plugin", e);
        }
    }

    @SuppressWarnings("deprecation")
	private void archive(String type, boolean attachToProject, boolean setProjectFile) throws IOException {
        Artifact artifact = factory.createArtifact(project.getArtifact().getGroupId(), project.getArtifact().getArtifactId(), project.getArtifact().getVersion(), project.getArtifact().getScope(), type);
        File target = archive(targetServerDirectory, destDir, artifact);

        if (attachToProject && attach) {
            projectHelper.attachArtifact(project, artifact.getType(), classifier, target);
        }

        if (setProjectFile) {
            artifact.setFile(target);
            project.setArtifact(artifact);
        }
    }

    public File archive(File source, File dest, Artifact artifact) throws //ArchiverException,
            IOException {
        String serverName = null;
        if (targetFile != null) {
            serverName = targetFile.getName();
        } else {
           serverName = artifact.getArtifactId() + "-" + artifact.getVersion();
        }
        dest = new File(dest, serverName + "." + artifact.getType());
        
        String prefix = "";
        if (usePathPrefix) {
        	prefix = pathPrefix.trim();
	        if( prefix.length() > 0 && !prefix.endsWith("/") ) {
	            prefix += "/";
	        }
        }

        if ("tar.gz".equals(artifact.getType())) {
            try (
                    OutputStream fOut = Files.newOutputStream(dest.toPath());
                    OutputStream bOut = new BufferedOutputStream(fOut);
                    OutputStream gzOut = new GzipCompressorOutputStream(bOut);
                    TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut);
                    DirectoryStream<Path> children = Files.newDirectoryStream(source.toPath())

            ) {
                tOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
                tOut.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);
                for (Path child : children) {
                    addFileToTarGz(tOut, child, prefix);
                }
            }
        } else if ("zip".equals(artifact.getType())) {
            try (
                    OutputStream fOut = Files.newOutputStream(dest.toPath());
                    OutputStream bOut = new BufferedOutputStream(fOut);
                    ZipArchiveOutputStream tOut = new ZipArchiveOutputStream(bOut);
                    DirectoryStream<Path> children = Files.newDirectoryStream(source.toPath())

            ) {
                for (Path child : children) {
                    addFileToZip(tOut, child, prefix);
                }
            }
        } else {
            throw new IllegalArgumentException("Unknown target type: " + artifact.getType());
        }

        return dest;
    }

    private void addFileToTarGz(TarArchiveOutputStream tOut, Path f, String base) throws IOException {
        if (Files.isDirectory(f)) {
            String entryName = base + f.getFileName().toString() + "/";
            TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
            tOut.putArchiveEntry(tarEntry);
            tOut.closeArchiveEntry();
            try (DirectoryStream<Path> children = Files.newDirectoryStream(f)) {
                for (Path child : children) {
                    addFileToTarGz(tOut, child, entryName);
                }
            }
        } else if (useSymLinks && Files.isSymbolicLink(f)) {
            String entryName = base + f.getFileName().toString();
            TarArchiveEntry tarEntry = new TarArchiveEntry(entryName, TarConstants.LF_SYMLINK);
            tarEntry.setLinkName(Files.readSymbolicLink(f).toString());
            tOut.putArchiveEntry(tarEntry);
            tOut.closeArchiveEntry();
        }  else {
            String entryName = base + f.getFileName().toString();
            TarArchiveEntry tarEntry = new TarArchiveEntry(entryName);
            tarEntry.setSize(Files.size(f));
            if (entryName.contains("/bin/") || (!usePathPrefix && entryName.startsWith("bin/"))) {
                if (entryName.endsWith(".bat")) {
                    tarEntry.setMode(0644);
                } else {
                    tarEntry.setMode(0755);
                }
            }
            tOut.putArchiveEntry(tarEntry);
            Files.copy(f, tOut);
            tOut.closeArchiveEntry();
        }
    }

    private void addFileToZip(ZipArchiveOutputStream tOut, Path f, String base) throws IOException {
        if (Files.isDirectory(f)) {
            String entryName = base + f.getFileName().toString() + "/";
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName);
            tOut.putArchiveEntry(zipEntry);
            tOut.closeArchiveEntry();
            try (DirectoryStream<Path> children = Files.newDirectoryStream(f)) {
                for (Path child : children) {
                    addFileToZip(tOut, child, entryName);
                }
            }
        } else if (useSymLinks && Files.isSymbolicLink(f)) {
            String entryName = base + f.getFileName().toString();
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName);
            zipEntry.setUnixMode(UnixStat.LINK_FLAG | UnixStat.DEFAULT_FILE_PERM);
            tOut.putArchiveEntry(zipEntry);
            tOut.write(Files.readSymbolicLink(f).toString().getBytes());
            tOut.closeArchiveEntry();
        }  else {
            String entryName = base + f.getFileName().toString();
            ZipArchiveEntry zipEntry = new ZipArchiveEntry(entryName);
            zipEntry.setSize(Files.size(f));
            if (entryName.contains("/bin/") || (!usePathPrefix && entryName.startsWith("bin"))) {
                if (!entryName.endsWith(".bat")) {
                    zipEntry.setUnixMode(0755);
                } else {
                    zipEntry.setUnixMode(0644);
                }
            }
            tOut.putArchiveEntry(zipEntry);
            Files.copy(f, tOut);
            tOut.closeArchiveEntry();
        }
    }

}
