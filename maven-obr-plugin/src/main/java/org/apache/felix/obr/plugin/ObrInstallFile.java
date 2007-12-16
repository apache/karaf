/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.obr.plugin;

import java.io.File;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * construct the repository.xml from a compiled bundle.
 * @description construct the repository.xml from a compiled bundle.
 * @goal install-file
 * @requiresProject false
 * @phase install
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrInstallFile extends AbstractMojo {
    /**
     * The local Maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository m_localRepo;

    /**
     * path to the repository.xml.
     * 
     * @parameter expression="${repository-path}"
     * @require
     */
    private String m_repositoryPath;

    /**
     * setting of maven.
     * 
     * @parameter expression="${settings}"
     * @require
     */
    private Settings m_settings;

    /**
     * Artifact Id.
     * @description symbolic name define by the user
     * @parameter expression="${artifactId}"
     */
    private String m_artifactId;

    /**
     * Group Id.
     * @description groupId define by the user
     * @parameter expression="${groupId}"
     */
    private String m_groupId;

    /**
     * Version.
     * @description version define by the user
     * @parameter expression="${version}"
     */
    private String m_version;

    /**
     * Packaging.
     * @description packaging define by the user
     * @parameter expression="${packaging}"
     */
    private String m_packaging;

    /**
     * OBR File.
     * @description obr file define by the user
     * @parameter expression="${obr-file}"
     */
    private String m_obrFile;

    /**
     * store user information in a project.
     */
    private MavenProject m_project;

    /**
     * main method for this goal.
     * @implements org.apache.maven.plugin.Mojo.execute 
     * @throws MojoExecutionException if the plugin failed
     * @throws MojoFailureException if the plugin failed
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Install-File Obr starts:");

        m_project = new MavenProject();
        m_project.setArtifactId(m_artifactId);
        m_project.setGroupId(m_groupId);
        m_project.setVersion(m_version);
        m_project.setPackaging(m_packaging);

        PathFile fileOut;

        if (m_groupId == null) {
            getLog().error("-DgroupId=VALUE is required");
            return;
        }
        if (m_artifactId == null) {
            getLog().error("-Dartifactid=VALUE is required");
            return;
        }
        if (m_version == null) {
            getLog().error("-Dversion=VALUE is required");
            return;
        }
        if (m_packaging == null) {
            getLog().error("-Dpackaging=VALUE is required");
            return;
        }

        // copy the file to the local repository
        PathFile repoLocal = new PathFile(m_localRepo.getBasedir());

        // get the target file in mvn repo
        fileOut = new PathFile(PathFile.uniformSeparator(m_settings.getLocalRepository()) + File.separator + m_groupId.replace('.', File.separatorChar) + File.separator + m_artifactId + File.separator + m_version + File.separator + m_artifactId
                + "-" + m_version + "." + m_packaging);

        if (!fileOut.isExists()) {
            getLog().error("file doesn't exist: " + fileOut.getAbsoluteFilename());
            return;
        } else {
            getLog().info("Target file: " + fileOut.getAbsoluteFilename());
        }

        if (m_repositoryPath == null) {
            m_repositoryPath = "file:" + repoLocal.getOnlyAbsoluteFilename() + "repository.xml";
            getLog().warn("-DpathRepo is not define, use default repository: " + m_repositoryPath);
        }

        PathFile fileRepo = new PathFile(m_repositoryPath);
        if (fileRepo.isRelative()) {
            fileRepo.setBaseDir(m_settings.getLocalRepository());
        }

        // create the folder to the repository
        PathFile repoExist = new PathFile(fileRepo.getAbsolutePath());
        if (!repoExist.isExists()) {
            fileRepo.createPath();
        }

        PathFile fileObrXml = new PathFile(m_obrFile);
        if (!fileObrXml.isExists()) {
            getLog().warn("obr.xml file not found, use default");
        }

        // build the user config
        Config userConfig = new Config();

        ObrUpdate obrUpdate = new ObrUpdate(fileRepo, fileObrXml.getOnlyAbsoluteFilename(), m_project, fileOut.getOnlyAbsoluteFilename(), m_localRepo.getBasedir(), userConfig, getLog());
        obrUpdate.updateRepository();

    }

}
