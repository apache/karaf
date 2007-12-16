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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authorization.AuthorizationException;

/**
 * deploy the bundle to a ftp site.
 * this goal is used when you upload a jar file (in command line)
 * @goal deploy-file
 * @phase deploy
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrDeployFile extends AbstractMojo {

    /**
     * setting of maven.
     * 
     * @parameter expression="${settings}"
     * @require
     */

    private Settings m_settings;

    /**
     * name of the repository xml descriptor file.
     * 
     * @parameter expression="${repository-name}" default-value="repository.xml"
     */
    private String m_repositoryName;

    /**
     * The local Maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     */
    private ArtifactRepository m_localRepo;

    /**
     * Project in use.
     * 
     * @parameter expression="${project}"
     * @require
     */
    private MavenProject m_project;

    /**
     * Wagon Manager.
     * @component
     */
    private WagonManager m_wagonManager;

    /**
     * obr file define by the user.
     * 
     * @parameter expression="${obr-file}"
     * 
     */
    private String m_obrFile;

    /**
     * obr file define by the user.
     * 
     * @parameter expression="${ignore-lock}"
     * 
     */
    private boolean m_ignoreLock;

    /**
     * used to store pathfile in local repo.
     */
    private String m_fileInLocalRepo;

    /**
     * main method for this goal.
     * @implements org.apache.maven.plugin.Mojo.execute 
     * @throws MojoExecutionException if the plugin failed
     * @throws MojoFailureException if the plugin failed
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Obr-deploy-file start:");

        ArtifactRepository ar = m_project.getDistributionManagementArtifactRepository();

        // locate the obr.xml file
        PathFile fileObrXml = new PathFile(m_obrFile);
        if (!fileObrXml.isExists()) {
            getLog().warn("obr.xml file not found, use default");
        }

        File repoDescriptorFile = null;

        RemoteFileManager remoteFile = new RemoteFileManager(ar, m_wagonManager, m_settings, getLog());

        remoteFile.connect();

        // create a non-empty file used to lock the repository descriptor file
        File lockFile = null;
        Writer output = null;
        try {
            lockFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), null);
            output = new BufferedWriter(new FileWriter(lockFile));
            output.write("locked");
            output.close();
        } catch (IOException e) {
            getLog().error("Unable to create temporary file");
            throw new MojoFailureException("IOException");
        }

        if (m_ignoreLock) {
            try {
                remoteFile.put(lockFile, m_repositoryName + ".lock");
            } catch (TransferFailedException e) {
                getLog().error("Transfer failed");
                e.printStackTrace();
                throw new MojoFailureException("TransferFailedException");

            } catch (ResourceDoesNotExistException e) {
                throw new MojoFailureException("ResourceDoesNotExistException");
            } catch (AuthorizationException e) {
                getLog().error("Authorization failed");
                e.printStackTrace();
                throw new MojoFailureException("AuthorizationException");
            }

        } else {
            int countError = 0;
            while (remoteFile.isLockedFile(remoteFile, m_repositoryName) && countError < 2) {
                countError++;
                getLog().warn("File is locked, retry in 10s");
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    getLog().warn("Sleep Interupted");
                }
            }

            if (countError == 2) {
                getLog().error("File: " + m_repositoryName + " is locked. Try -Dignore-lock=true if you want to force uploading");
                throw new MojoFailureException("fileLocked");
            }
        }

        // file is not locked, so we lock it now
        try {
            remoteFile.put(lockFile, m_repositoryName + ".lock");
        } catch (TransferFailedException e) {
            getLog().error("Transfer failed");
            e.printStackTrace();
            throw new MojoFailureException("TransferFailedException");

        } catch (ResourceDoesNotExistException e) {
            throw new MojoFailureException("ResourceDoesNotExistException");
        } catch (AuthorizationException e) {
            getLog().error("Authorization failed");
            e.printStackTrace();
            throw new MojoFailureException("AuthorizationException");
        }

        try {
            repoDescriptorFile = remoteFile.get(m_repositoryName);
        } catch (TransferFailedException e) {
            getLog().error("Transfer failed");
            e.printStackTrace();
            throw new MojoFailureException("TransferFailedException");

        } catch (ResourceDoesNotExistException e) {
            // file doesn't exist! create a new one
            getLog().warn("file specified does not exist: " + m_repositoryName);
            getLog().warn("Create a new repository descriptor file " + m_repositoryName);
            try {
                File f = File.createTempFile(String.valueOf(System.currentTimeMillis()), null);
                repoDescriptorFile = new File(f.getParent() + File.separator + String.valueOf(System.currentTimeMillis()) + ".xml");
            } catch (IOException e1) {
                getLog().error("canno't create temporary file");
                e1.printStackTrace();
                return;
            }
        } catch (AuthorizationException e) {
            getLog().error("Authorization failed");
            e.printStackTrace();
            throw new MojoFailureException("AuthorizationException");
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoFailureException("IOException");
        }

        Config userConfig = new Config();
        userConfig.setPathRelative(true);
        userConfig.setRemotely(true);

        PathFile file = null;

        // get the path to local maven repository
        file = new PathFile(PathFile.uniformSeparator(m_settings.getLocalRepository()) + File.separator + PathFile.uniformSeparator(m_localRepo.pathOf(m_project.getArtifact())));
        if (file.isExists()) {
            m_fileInLocalRepo = file.getOnlyAbsoluteFilename();
        } else {
            getLog().error("file not found in local repository: " + m_settings.getLocalRepository() + File.separator + m_localRepo.pathOf(m_project.getArtifact()));
            return;
        }

        file = new PathFile("file:/" + repoDescriptorFile.getAbsolutePath());

        ObrUpdate obrUpdate = new ObrUpdate(file, fileObrXml.getOnlyAbsoluteFilename(), m_project, m_fileInLocalRepo, PathFile.uniformSeparator(m_settings.getLocalRepository()), userConfig, getLog());

        obrUpdate.updateRepository();

        // the reposiroty descriptor file is modified, we upload it on the remote repository
        try {
            remoteFile.put(repoDescriptorFile, m_repositoryName);
        } catch (TransferFailedException e) {
            getLog().error("Transfer failed");
            e.printStackTrace();
            throw new MojoFailureException("TransferFailedException");
        } catch (ResourceDoesNotExistException e) {
            getLog().error("Resource does not exist:" + repoDescriptorFile.getName());
            e.printStackTrace();
            throw new MojoFailureException("ResourceDoesNotExistException");
        } catch (AuthorizationException e) {
            getLog().error("Authorization failed");
            e.printStackTrace();
            throw new MojoFailureException("AuthorizationException");
        }
        repoDescriptorFile.delete();

        // we remove lockFile activation
        lockFile = null;
        try {
            lockFile = File.createTempFile(String.valueOf(System.currentTimeMillis()), null);
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoFailureException("IOException");
        }
        try {
            remoteFile.put(lockFile, m_repositoryName + ".lock");
        } catch (TransferFailedException e) {
            getLog().error("Transfer failed");
            e.printStackTrace();
            throw new MojoFailureException("TransferFailedException");
        } catch (ResourceDoesNotExistException e) {
            e.printStackTrace();
            throw new MojoFailureException("ResourceDoesNotExistException");
        } catch (AuthorizationException e) {
            getLog().error("Authorization failed");
            e.printStackTrace();
            throw new MojoFailureException("AuthorizationException");
        }
        remoteFile.disconnect();
    }
}
