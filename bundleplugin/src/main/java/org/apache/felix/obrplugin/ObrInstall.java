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


import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


/**
 * Installs bundle details in the local OBR repository (life-cycle goal)
 * 
 * @goal install
 * @phase install
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class  ObrInstall extends AbstractMojo
{
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
     * Attached source artifact
     */
    private Artifact m_sourceArtifact;

    /**
     * Attached doc artifact
     */
    private Artifact m_docArtifact;


    public void execute()
    {
        String projectType = project.getPackaging();

        // ignore unsupported project types, useful when bundleplugin is configured in parent pom
        if ( !supportedProjectTypes.contains( projectType ) )
        {
            getLog().warn(
                "Ignoring project type " + projectType + " - supportedProjectTypes = " + supportedProjectTypes );
            return;
        }
        else if ( "NONE".equalsIgnoreCase( obrRepository ) || "false".equalsIgnoreCase( obrRepository ) )
        {
            getLog().info( "Local OBR update disabled (enable with -DobrRepository)" );
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

        Log log = getLog();
        ObrUpdate update;

        try
        {
            String mavenRepository = localRepository.getBasedir();

            URI repositoryXml = ObrUtils.findRepositoryXml( mavenRepository, obrRepository );
            URI obrXmlFile = ObrUtils.findObrXml( project.getResources() );

            Config userConfig = new Config();

            update = new ObrUpdate( repositoryXml, obrXmlFile, project, mavenRepository, userConfig, log );
            update.parseRepositoryXml();

            updateLocalBundleMetadata( project.getArtifact(), update );
            for ( Iterator i = attachedArtifacts.iterator(); i.hasNext(); )
            {
                updateLocalBundleMetadata( ( Artifact ) i.next(), update );
            }

            update.writeRepositoryXml();
        }
        catch ( Exception e )
        {
            log.warn( "Exception while updating local OBR: " + e.getLocalizedMessage(), e );
        }
    }


    private void updateLocalBundleMetadata( Artifact artifact, ObrUpdate update ) throws MojoExecutionException
    {
        if ( !supportedProjectTypes.contains( artifact.getType() ) )
        {
            return;
        }
        else if ( null == artifact.getFile() || artifact.getFile().isDirectory() )
        {
            getLog().error( "No artifact found, try \"mvn install bundle:install\"" );
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
