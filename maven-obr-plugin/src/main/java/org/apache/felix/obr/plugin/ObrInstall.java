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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;


/**
 * construct the repository.xml with a project Maven compiled
 *
 * @goal install
 * @phase install
 * @requiresDependencyResolution compile
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrInstall extends AbstractMojo
{
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
     * Project in use.
     * 
     * @parameter expression="${project}"
     * @require
     */

    private MavenProject m_project;


    /**
     * main method for this goal.
     * @implements org.apache.maven.plugin.Mojo.execute 
     * @throws MojoExecutionException if the plugin failed
     */
    public void execute() throws MojoExecutionException
    {
        // locate the obr.xml file
        URI obrXml = ObrUtils.findObrXml( m_project.getResources() );
        if ( null == obrXml )
        {
            getLog().info( "obr.xml is not present, use default" );
        }

        // get the path to local maven repository
        String mavenRepository = m_localRepo.getBasedir();

        URI repoXml = ObrUtils.findRepositoryXml( mavenRepository, m_repositoryPath );
        URI bundleJar = ObrUtils.findBundleJar( m_localRepo, m_project.getArtifact() );

        if ( !new File( bundleJar ).exists() )
        {
            getLog().error( "file not found in local repository: " + bundleJar );
            return;
        }

        // use default configuration
        Config userConfig = new Config();

        ObrUpdate update = new ObrUpdate( repoXml, obrXml, m_project, bundleJar, null, userConfig, getLog() );

        update.updateRepository();
    }
}
