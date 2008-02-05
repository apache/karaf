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
import java.util.Collection;
import java.util.Iterator;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Resource;


/**
 * Various OBR utility methods
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrUtils
{
    private static final String DOT_XML = ".xml";
    private static final String REPO_XML = "repository.xml";
    private static final String OBR_XML = "obr.xml";


    /**
     * @param mavenRepository path to local maven repository
     * @param obrRepository path to specific repository.xml
     * @return URI pointing to correct repository.xml
     */
    public static URI findRepositoryXml( String mavenRepository, String obrRepository )
    {
        String targetPath = obrRepository;

        // Combine location settings into a single repository location
        if ( null == targetPath || targetPath.trim().length() == 0 || "true".equalsIgnoreCase( targetPath ) )
        {
            targetPath = mavenRepository + '/' + REPO_XML;
        }
        else if ( !targetPath.toLowerCase().endsWith( DOT_XML ) )
        {
            targetPath = targetPath + '/' + REPO_XML;
        }

        URI uri;
        try
        {
            uri = new URI( targetPath );
            uri.toURL(); // check protocol
        }
        catch ( Exception e )
        {
            uri = null;
        }

        // fall-back to file-system approach
        if ( null == uri || !uri.isAbsolute() )
        {
            uri = new File( targetPath ).toURI();
        }

        return uri;
    }


    /**
     * @param resources collection of resource locations
     * @return URI pointing to correct obr.xml, null if not found
     */
    public static URI findObrXml( Collection resources )
    {
        for ( Iterator i = resources.iterator(); i.hasNext(); )
        {
            Resource resource = ( Resource ) i.next();
            File obrFile = new File( resource.getDirectory(), OBR_XML );
            if ( obrFile.exists() )
            {
                return obrFile.toURI();
            }
        }
        return null;
    }


    /**
     * @param repository maven repository
     * @param artifact maven artifact
     * @return file URI pointing to artifact in repository
     */
    public static URI findBundleJar( ArtifactRepository repository, Artifact artifact )
    {
        String baseDir = repository.getBasedir();
        String artifactPath = repository.pathOf( artifact );

        return toFileURI( baseDir + '/' + artifactPath );
    }


    /**
     * @param path filesystem path
     * @return file URI for the path
     */
    public static URI toFileURI( String path )
    {
        if ( null == path )
        {
            return null;
        }
        else if ( path.startsWith( "file:" ) )
        {
            return URI.create( path );
        }
        else
        {
            return new File( path ).toURI();
        }
    }


    /**
     * @param repositoryXml URI pointing to repository.xml, or directory containing it
     * @param bundleJar URI pointing to bundle jarfile
     * @return relative URI to bundle jarfile
     */
    public static URI getRelativeURI( URI repositoryXml, URI bundleJar )
    {
        try
        {
            String repositoryPath = repositoryXml.getPath();
            if ( repositoryPath.toLowerCase().endsWith( DOT_XML ) )
            {
                // remove filename to get containing directory
                int dirnameIndex = repositoryPath.lastIndexOf( '/' );
                repositoryPath = repositoryPath.substring( 0, dirnameIndex );
            }

            URI rootURI = new URI( null, repositoryPath, null );
            URI localURI = new URI( null, bundleJar.getPath(), null );

            return rootURI.relativize( localURI );
        }
        catch ( Exception e )
        {
            return bundleJar;
        }
    }
}
