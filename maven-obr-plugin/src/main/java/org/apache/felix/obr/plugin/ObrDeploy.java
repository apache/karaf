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
import java.net.URI;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;


/**
 * Deploy bundle metadata to remote OBR.
 * 
 * @goal deploy
 * @phase deploy
 * @requiresDependencyResolution compile
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrDeploy extends AbstractMojo
{
    /**
     * Maven settings.
     * 
     * @parameter expression="${settings}"
     * @require
     */
    private Settings m_settings;

    /**
     * name of the repository xml descriptor file.
     * 
     * @parameter expression="${repository-name}" default-value="repository.xml" alias="repository-name"
     */
    private String m_repositoryName;

    /**
     * The local Maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
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
     * When true, ignore remote locking.
     * 
     * @parameter expression="${ignore-lock}" alias="ignore-lock"
     */
    private boolean m_ignoreLock;


    /**
     * main method for this goal.
     * @implements org.apache.maven.plugin.Mojo.execute 
     * @throws MojoExecutionException
     */
    public void execute() throws MojoExecutionException
    {
        ArtifactRepository ar = m_project.getDistributionManagementArtifactRepository();

        // locate the obr.xml file
        URI obrXml = ObrUtils.findObrXml( m_project.getResources() );
        if ( null == obrXml )
        {
            getLog().info( "obr.xml is not present, use default" );
        }

        File repoDescriptorFile = null;

        // init the wagon connection
        RemoteFileManager remoteFile = new RemoteFileManager( ar, m_wagonManager, m_settings, getLog() );
        remoteFile.connect();

        if ( !m_ignoreLock )
        {
            int countError = 0;
            while ( remoteFile.isLockedFile( m_repositoryName ) && countError < 2 )
            {
                countError++;
                getLog().warn( "OBR is locked, retry in 10s" );
                try
                {
                    Thread.sleep( 10000 );
                }
                catch ( InterruptedException e )
                {
                    getLog().warn( "Sleep interrupted" );
                }
            }

            if ( countError == 2 )
            {
                getLog().error( "OBR " + m_repositoryName + " is locked. Use -Dignore-lock to force uploading" );
                throw new MojoExecutionException( "OBR locked" );
            }
        }

        // ======== LOCK REMOTE OBR ========
        remoteFile.lockFile( m_repositoryName );

        // ======== DOWNLOAD REMOTE OBR ========
        repoDescriptorFile = remoteFile.get( m_repositoryName, ".xml" );

        // get the path to local maven repository
        String mavenRepository = m_localRepo.getBasedir();

        URI repoXml = repoDescriptorFile.toURI();
        URI bundleJar = ObrUtils.findBundleJar( m_localRepo, m_project.getArtifact() );

        if ( !new File( bundleJar ).exists() )
        {
            getLog().error( "file not found in local repository: " + bundleJar );
            return;
        }

        Config userConfig = new Config();
        userConfig.setPathRelative( true );
        userConfig.setRemotely( true );

        ObrUpdate update = new ObrUpdate( repoXml, obrXml, m_project, bundleJar, mavenRepository, userConfig, getLog() );

        update.updateRepository();

        // ======== UPLOAD MODIFIED OBR ========
        remoteFile.put( repoDescriptorFile, m_repositoryName );
        repoDescriptorFile.delete();

        // ======== UNLOCK REMOTE OBR ========
        remoteFile.unlockFile( m_repositoryName );

        remoteFile.disconnect();
    }
}
