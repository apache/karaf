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


package org.apache.karaf.tooling.features;

import java.io.File;
import java.io.IOException;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.MatchingTask;
import org.apache.tools.ant.taskdefs.Tar;
import org.apache.tools.ant.taskdefs.Zip;
import org.apache.tools.ant.types.TarFileSet;
import org.apache.tools.ant.types.ZipFileSet;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Package a server archive from an assembled server
 *
 * @version $Revision: 1.1 $
 * @goal archive-server
 * @phase package
 * @execute phase="package"
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Package a server archive from an assembled server
 */
public class ArchiveServerMojo extends MojoSupport {

    /**
     * The target directory of the project.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     * @readonly
     */
    private File destDir;


    /**
     * The location of the server repository.
     *
     * @parameter expression="${project.build.directory}/assembly"
     * @required
     */
    private File targetServerDirectory;

    /**
     * The target file to set as the project's artifact.
     *
     * @parameter expression="${project.file}"
     * @required
     */
    private File targetFile;


    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().debug("Setting artifact file: " + targetFile);
        org.apache.maven.artifact.Artifact artifact = project.getArtifact();
        artifact.setFile(targetFile);
        try {
            //now pack up the server.
            archive("tar.gz");
            archive("zip");
        } catch (Exception e) {
            throw new MojoExecutionException("Could not archive plugin", e);
        }
    }

    private void archive(String type) throws IOException {
        Artifact artifact1 = factory.createArtifactWithClassifier(project.getArtifact().getGroupId(), project.getArtifact().getArtifactId(), project.getArtifact().getVersion(), type, "bin");
        File target1 = archive(targetServerDirectory, destDir, artifact1);
        projectHelper.attachArtifact( project, artifact1.getType(), "bin", target1 );
    }

    public File archive(File source, File dest, Artifact artifact) throws //ArchiverException,
            IOException {
        String serverName = artifact.getArtifactId() + "-" + artifact.getVersion();
        dest = new File(dest, serverName + "-bin." + artifact.getType());
        Project project = new Project();
        MatchingTask archiver;
        if ("tar.gz".equals(artifact.getType())) {
            Tar tar = new Tar();
            Tar.TarCompressionMethod tarCompressionMethod = new Tar.TarCompressionMethod();
            tarCompressionMethod.setValue("gzip");
            tar.setCompression(tarCompressionMethod);
            Tar.TarLongFileMode fileMode = new Tar.TarLongFileMode();
            fileMode.setValue(Tar.TarLongFileMode.GNU);
            tar.setLongfile(fileMode);
            tar.setDestFile(dest);
            TarFileSet rc = new TarFileSet();
            rc.setDir(source);
            rc.setPrefix(serverName);
            rc.setProject(project);
            rc.setExcludes("bin/");
            tar.add(rc);

            rc = new TarFileSet();
            rc.setDir(source);
            rc.setPrefix(serverName);
            rc.setProject(project);
            rc.setIncludes("bin/");
            rc.setExcludes("bin/*.bat");
            rc.setFileMode("755");
            tar.add(rc);

            rc = new TarFileSet();
            rc.setDir(source);
            rc.setPrefix(serverName);
            rc.setProject(project);
            rc.setIncludes("bin/*.bat");
            tar.add(rc);

            archiver = tar;
        } else if ("zip".equals(artifact.getType())) {
            Zip zip = new Zip();
            zip.setDestFile(dest);
            ZipFileSet fs = new ZipFileSet();
            fs.setDir(source);
            fs.setPrefix(serverName);
            fs.setProject(project);
            zip.addFileset(fs);
            archiver = zip;
        } else {
            throw new IllegalArgumentException("Unknown target type: " + artifact.getType());
        }
        archiver.setProject(project);
        archiver.execute();
        return dest;
    }

}
