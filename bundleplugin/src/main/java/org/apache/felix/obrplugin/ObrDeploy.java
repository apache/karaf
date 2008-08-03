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
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.artifact.Artifact;
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
     * Optional public URL prefix for the remote repository.
     *
     * @parameter expression="${prefixUrl}"
     */
    private String prefixUrl;

    /**
     * Optional public URL where the bundle has been deployed.
     *
     * @parameter expression="${bundleUrl}"
     */
    private String bundleUrl;

    /**
     * Remote OBR Repository.
     * 
     * @parameter expression="${remoteOBR}" default-value="NONE"
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
     * OBR specific deployment repository. Format: id::layout::url
     * 
     * @parameter expression="${obrDeploymentRepository}"
     */
    private String obrDeploymentRepository;

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
     * @parameter expression="${project.attachedArtifacts}
     * @required
     * @readonly
     */
    private List attachedArtifacts;

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

    /**
     * Attached source artifact
     */
    private Artifact m_sourceArtifact;

    /**
     * Attached doc artifact
     */
    private Artifact m_docArtifact;


    public void execute() throws MojoExecutionException
    {
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

        // check for any attached sources or docs
        for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
        {
            Artifact artifact = ( Artifact ) i.next();
            if ( "sources".equals( artifact.getClassifier() ) )
            {
                m_sourceArtifact = artifact;
            }
            else if ( "javadoc".equals( artifact.getClassifier() ) )
            {
                m_docArtifact = artifact;
            }
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

            Config userConfig = new Config();
            userConfig.setRemoteFile( true );

            if ( bundleUrl != null )
            {
                // public URL differs from the bundle file location
                userConfig.setRemoteBundle( URI.create( bundleUrl ) );
            }
            else if ( prefixUrl != null )
            {
                // support absolute bundle URLs based on given prefix
                URI bundleJar = ObrUtils.getArtifactURI( localRepository, project.getArtifact() );
                String relative = ObrUtils.getRelativeURI( ObrUtils.toFileURI( mavenRepository ), bundleJar )
                    .toASCIIString();
                URL resourceURL = new URL( new URL( prefixUrl + '/' ), relative );
                userConfig.setRemoteBundle( URI.create( resourceURL.toString() ) );
            }

            update = new ObrUpdate( repositoryXml, obrXmlFile, project, mavenRepository, userConfig, log );
            update.parseRepositoryXml();

            updateRemoteBundleMetadata( project.getArtifact(), update );
            for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
            {
                updateRemoteBundleMetadata( ( Artifact ) i.next(), update );
            }

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

    private static final Pattern ALT_REPO_SYNTAX_PATTERN = Pattern.compile( "(.+)::(.+)::(.+)" );


    private void openRepositoryConnection( RemoteFileManager remoteFile ) throws MojoExecutionException
    {
        // use OBR specific deployment location?
        if ( obrDeploymentRepository != null )
        {
            altDeploymentRepository = obrDeploymentRepository;
        }

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


    private void updateRemoteBundleMetadata( Artifact artifact, ObrUpdate update ) throws MojoExecutionException
    {
        if ( !supportedProjectTypes.contains( artifact.getType() ) )
        {
            return;
        }
        else if ( null == artifact.getFile() || artifact.getFile().isDirectory() )
        {
            getLog().error( "No artifact found, try \"mvn install bundle:deploy\"" );
            return;
        }

        URI bundleJar = ObrUtils.getArtifactURI( localRepository, artifact );

        URI sourceJar = null;
        if ( null != m_sourceArtifact )
        {
            sourceJar = ObrUtils.getArtifactURI( localRepository, m_sourceArtifact );
        }

        URI docJar = null;
        if ( null != m_docArtifact )
        {
            docJar = ObrUtils.getArtifactURI( localRepository, m_docArtifact );
        }

        update.updateRepository( bundleJar, sourceJar, docJar );
    }
}
