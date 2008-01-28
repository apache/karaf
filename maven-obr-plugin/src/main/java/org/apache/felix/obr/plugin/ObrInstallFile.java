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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;


/**
 * construct the repository.xml from a compiled bundle.
 * @description construct the repository.xml from a compiled bundle.
 * @goal install-file
 * @requiresProject false
 * @phase install
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrInstallFile extends AbstractMojo
{
    /**
     * Component factory for Maven artifacts
     * 
     * @component
     */
    private ArtifactFactory m_factory;

    /**
     * The local Maven repository.
     * 
     * @parameter expression="${localRepository}"
     * @required
     * @readonly
     */
    private ArtifactRepository m_localRepo;

    /**
     * path to the repository.xml.
     * 
     * @parameter expression="${repository-path}"
     */
    private String m_repositoryPath;

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
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        m_project = new MavenProject();
        m_project.setArtifactId( m_artifactId );
        m_project.setGroupId( m_groupId );
        m_project.setVersion( m_version );
        m_project.setPackaging( m_packaging );

        if ( m_groupId == null )
        {
            getLog().error( "-DgroupId=VALUE is required" );
            return;
        }
        if ( m_artifactId == null )
        {
            getLog().error( "-Dartifactid=VALUE is required" );
            return;
        }
        if ( m_version == null )
        {
            getLog().error( "-Dversion=VALUE is required" );
            return;
        }
        if ( m_packaging == null )
        {
            getLog().error( "-Dpackaging=VALUE is required" );
            return;
        }

        // locate the obr.xml file
        URI obrXml = ObrUtils.toFileURI( m_obrFile );
        if ( null == obrXml )
        {
            getLog().info( "obr.xml is not present, use default" );
        }

        Artifact bundleArtifact = m_factory.createBuildArtifact( m_groupId, m_artifactId, m_version, m_packaging );

        // get the path to local maven repository
        String mavenRepository = m_localRepo.getBasedir();

        URI repoXml = ObrUtils.findRepositoryXml( mavenRepository, m_repositoryPath );
        URI bundleJar = ObrUtils.findBundleJar( m_localRepo, bundleArtifact );

        if ( !new File( bundleJar ).exists() )
        {
            getLog().error( "file doesn't exist: " + bundleJar );
            return;
        }

        // use default configuration
        Config userConfig = new Config();

        ObrUpdate update = new ObrUpdate( repoXml, obrXml, m_project, bundleJar, null, userConfig, getLog() );

        update.updateRepository();
    }
}
