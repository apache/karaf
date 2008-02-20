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
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


/**
 * Installs bundle details in the local OBR repository (command-line goal)
 * 
 * @requiresProject false
 * @goal install-file
 * @phase install
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ObrInstallFile extends AbstractFileMojo
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


    public void execute() throws MojoExecutionException
    {
        MavenProject project = getProject();

        if ( !supportedProjectTypes.contains( project.getPackaging() ) )
        {
            getLog().info( "Ignoring packaging type " + project.getPackaging() );
            return;
        }
        else if ( "NONE".equalsIgnoreCase( obrRepository ) || "false".equalsIgnoreCase( obrRepository ) )
        {
            getLog().info( "Local OBR update disabled (enable with -DobrRepository)" );
            return;
        }

        Log log = getLog();
        ObrUpdate update;

        String mavenRepository = localRepository.getBasedir();

        URI repositoryXml = ObrUtils.findRepositoryXml( mavenRepository, obrRepository );
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

        update = new ObrUpdate( repositoryXml, obrXmlFile, project, mavenRepository, userConfig, log );
        update.parseRepositoryXml();

        update.updateRepository( bundleJar, null, null );

        update.writeRepositoryXml();
    }
}
