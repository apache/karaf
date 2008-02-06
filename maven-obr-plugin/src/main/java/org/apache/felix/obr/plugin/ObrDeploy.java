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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;


/**
 * Deploys bundle details to a remote OBR repository (life-cycle goal)
 * 
 * @goal deploy
 * @phase deploy
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ObrDeploy extends AbstractMojo
{
    /**
     * When true, ignore remote locking.
     * 
     * @parameter expression="${ignoreLock}"
     */
    private boolean ignoreLock;

    /**
     * OBR Repository.
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
     * @parameter expression="${project.distributionManagementArtifactRepository}"
     * @readonly
     */
    private ArtifactRepository deploymentRepository;

    /**
     * Alternative deployment repository. Format: id::layout::url
     * 
     * @parameter expression="${altDeploymentRepository}"
     */
    private String altDeploymentRepository;

    /**
     * Local Repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository localRepository;

    /**
     * The Maven project.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

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
        if ( !supportedProjectTypes.contains( project.getPackaging() ) )
        {
            getLog().info( "Ignoring packaging type " + project.getPackaging() );
            return;
        }
        else if ( "NONE".equalsIgnoreCase( obrRepository ) )
        {
            getLog().info( "OBR update disabled (enable with -DobrRepository)" );
            return;
        }

        URI tempURI = ObrUtils.findRepositoryXml( "", obrRepository );
        String repositoryName = new File( tempURI.getPath() ).getName();

        Log log = getLog();
        ObrUpdate update;

        RemoteFileManager remoteFile = new RemoteFileManager( m_wagonManager, settings, log );
        openRepositoryConnection( remoteFile );

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
            URI obrXmlFile = ObrUtils.findObrXml( project.getResources() );
            URI bundleJar = ObrUtils.findBundleJar( localRepository, project.getArtifact() );

            Config userConfig = new Config();
            userConfig.setPathRelative( true );
            userConfig.setRemoteFile( true );

            update = new ObrUpdate( repositoryXml, obrXmlFile, project, bundleJar, mavenRepository, userConfig, log );

            update.updateRepository();

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

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" );


    private void openRepositoryConnection( RemoteFileManager remoteFile ) throws MojoExecutionException
    {
        if ( deploymentRepository == null && altDeploymentRepository == null )
        {
            String msg = "Deployment failed: repository element was not specified in the pom inside"
                + " distributionManagement element or in -DaltDeploymentRepository=id::layout::url parameter";

            throw new MojoExecutionException( msg );
        }

        if ( altDeploymentRepository != null )
        {
            getLog().info( "Using alternate deployment repository " + altDeploymentRepository );

            Matcher matcher = ALT_REPO_SYNTAX_PATTERN.matcher( altDeploymentRepository );
            if ( !matcher.matches() )
            {
                throw new MojoExecutionException( "Invalid syntax for alternative repository \""
                    + altDeploymentRepository + "\". Use \"id::layout::url\"." );
            }

            remoteFile.connect( matcher.group( 1 ).trim(), matcher.group( 3 ).trim() );
        }
        else
        {
            remoteFile.connect( deploymentRepository.getId(), deploymentRepository.getUrl() );
        }
    }
}
