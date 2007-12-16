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
package org.apache.felix.bundleplugin;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.obr.plugin.Config;
import org.apache.felix.obr.plugin.ObrUpdate;
import org.apache.felix.obr.plugin.PathFile;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

/**
 * Installs bundle details in the local OBR repository
 * 
 * @goal install
 * @phase install
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class OBRInstall extends AbstractMojo
{
    /**
     * OBR Repository.
     * 
     * @parameter expression="${obrRepository}"
     */
    private String obrRepository;

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

    public void execute()
        throws MojoExecutionException
    {
        if( "NONE".equalsIgnoreCase( obrRepository ) )
        {
            return;
        }

        Log log = getLog();
        ObrUpdate update;

        try
        {
            String localRepoPath = localRepository.getBasedir();
            String artifactPath = localRepository.pathOf( project.getArtifact() );
            String bundlePath = localRepoPath + File.separator + artifactPath;
            bundlePath = bundlePath.replace( '\\', '/' );

            PathFile repositoryXml = normalizeRepositoryPath( obrRepository, localRepoPath );
            String extensionXml = findOBRExtensions( project.getResources() );

            Config user = new Config();

            update = new ObrUpdate( repositoryXml, extensionXml, project, bundlePath, localRepoPath, user, log );

            repositoryXml.createPath();
            update.updateRepository();
        }
        catch( Exception e )
        {
            log.warn( "Exception while updating OBR: " + e.getLocalizedMessage(), e );
        }
    }

    private static PathFile normalizeRepositoryPath( String obrPath, String mavenPath )
    {
        if( null == obrPath || obrPath.length() == 0 )
        {
            obrPath = mavenPath + File.separatorChar + "repository.xml";
        }
        else if( !obrPath.endsWith( ".xml" ) )
        {
            obrPath = obrPath + File.separatorChar + "repository.xml";
        }

        URI uri;
        try
        {
            uri = new URI( obrPath );
        }
        catch( URISyntaxException e )
        {
            uri = null;
        }

        if( null == uri || !uri.isAbsolute() )
        {
            File file = new File( obrPath );
            if( !file.isAbsolute() )
            {
                file = new File( mavenPath, obrPath );
            }

            uri = file.toURI();
        }

        // PathFile workaround: for now provide decoded strings to maven-obr-plugin
        return new PathFile( uri.getScheme() + ':' + uri.getSchemeSpecificPart() );
    }

    private static String findOBRExtensions( List resources )
    {
        for( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = (Resource) i.next();
            File obrFile = new File( resource.getDirectory(), "obr.xml" );
            if( obrFile.exists() )
            {
                return obrFile.getPath();
            }
        }
        return null;
    }
}
