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
package org.apache.felix.obrplugin;


import java.io.File;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;


/**
 * Deploys bundle details to a remote OBR repository (command-line goal)
 * 
 * @requiresProject false
 * @goal deploy-file
 * @phase deploy
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ObrDeployFile extends AbstractFileMojo
{
    /**
     * When true, ignore remote locking.
     * 
     * @parameter expression="${ignoreLock}"
     */
    private boolean ignoreLock;

    /**
     * Remote OBR Repository.
     * 
     * @parameter expression="${remoteOBR}"
     */
    private String remoteOBR;

    /**
     * Local OBR Repository.
     * 
     * @parameter expression="${obrRepository}"
     */
    private String obrRepository;

    /**
     * Project types which this plugin supports.
     *
     * @parameter
     */
    private List supportedProjectTypes = Arrays.asList( new String[]
        { "jar", "bundle" } );

    /**
     * Remote repository id, used to lookup authentication settings.
     *
     * @parameter expression="${repositoryId}" default-value="remote-repository"
     * @required
     */
    private String repositoryId;

    /**
     * Remote OBR repository URL, where the bundle details are to be uploaded.
     *
     * @parameter expression="${url}"
     * @required
     */
    private String url;

    /**
     * Optional public URL where the bundle has been deployed.
     *
     * @parameter expression="${bundleUrl}"
     */
    private String bundleUrl;

    /**
     * Local Repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * Local Maven settings.
     * 
     * @parameter expression="${settings}"
     * @required
     * @readonly
     */
    private Settings settings;

    /**
     * The Wagon manager.
     * 
     * @component
     */
    private WagonManager m_wagonManager;


    public void execute() throws MojoExecutionException
    {
        MavenProject project = getProject();
        String projectType = project.getPackaging();

        // ignore unsupported project types, useful when bundleplugin is configured in parent pom
        if ( !supportedProjectTypes.contains( projectType ) )
        {
            getLog().warn(
                "Ignoring project type " + projectType + " - supportedProjectTypes = " + supportedProjectTypes );
            return;
        }
        else if ( "NONE".equalsIgnoreCase( remoteOBR ) || "false".equalsIgnoreCase( remoteOBR ) )
        {
            getLog().info( "Remote OBR update disabled (enable with -DremoteOBR)" );
            return;
        }

        // if the user doesn't supply an explicit name for the remote OBR file, use the local name instead
        if ( null == remoteOBR || remoteOBR.trim().length() == 0 || "true".equalsIgnoreCase( remoteOBR ) )
        {
            remoteOBR = obrRepository;
        }

        URI tempURI = ObrUtils.findRepositoryXml( "", remoteOBR );
        String repositoryName = new File( tempURI.getSchemeSpecificPart() ).getName();

        Log log = getLog();
        ObrUpdate update;

        RemoteFileManager remoteFile = new RemoteFileManager( m_wagonManager, settings, log );
        remoteFile.connect( repositoryId, url );

        // ======== LOCK REMOTE OBR ========
        log.info( "LOCK " + remoteFile + '/' + repositoryName );
        remoteFile.lockFile( repositoryName, ignoreLock );
        File downloadedRepositoryXml = null;

        try
        {
            // ======== DOWNLOAD REMOTE OBR ========
            log.info( "Downloading " + repositoryName );
            downloadedRepositoryXml = remoteFile.get( repositoryName, ".xml" );

            String mavenRepository = localRepository.getBasedir();

            URI repositoryXml = downloadedRepositoryXml.toURI();
            URI obrXmlFile = ObrUtils.toFileURI( obrXml );
            URI bundleJar;

            if ( null == file )
            {
                bundleJar = ObrUtils.getArtifactURI( localRepository, project.getArtifact() );
            }
            else
            {
                bundleJar = file.toURI();
            }

            Config userConfig = new Config();
            userConfig.setRemoteFile( true );

            if ( null != bundleUrl )
            {
                // public URL differs from the bundle file location
                URI uri = URI.create( bundleUrl );
                log.info("Computed bundle uri: " + uri);
                userConfig.setRemoteBundle( uri );
            }
            else if ( null != file )
            {
                // assume file will be deployed in remote repository, so find the remote relative location
                URI uri = URI.create( localRepository.pathOf( project.getArtifact() ) );
                log.info("Computed bundle uri: " + uri);
                userConfig.setRemoteBundle( uri );
            }

            update = new ObrUpdate( repositoryXml, obrXmlFile, project, mavenRepository, userConfig, log );
            update.parseRepositoryXml();

            update.updateRepository( bundleJar, null, null );

            update.writeRepositoryXml();

            if ( downloadedRepositoryXml.exists() )
            {
                // ======== UPLOAD MODIFIED OBR ========
                log.info( "Uploading " + repositoryName );
                remoteFile.put( downloadedRepositoryXml, repositoryName );
            }
        }
        catch ( Exception e )
        {
            log.warn( "Exception while updating remote OBR: " + e.getLocalizedMessage(), e );
        }
        finally
        {
            // ======== UNLOCK REMOTE OBR ========
            log.info( "UNLOCK " + remoteFile + '/' + repositoryName );
            remoteFile.unlockFile( repositoryName );
            remoteFile.disconnect();

            if ( null != downloadedRepositoryXml )
            {
                downloadedRepositoryXml.delete();
            }
        }
    }
}
