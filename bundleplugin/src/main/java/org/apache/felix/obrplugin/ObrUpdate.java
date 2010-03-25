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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;

import org.apache.felix.bundlerepository.Resource;
import org.apache.felix.bundlerepository.impl.DataModelHelperImpl;
import org.apache.felix.bundlerepository.impl.PullParser;
import org.apache.felix.bundlerepository.impl.RepositoryImpl;
import org.apache.felix.bundlerepository.impl.RepositoryParser;
import org.apache.felix.bundlerepository.impl.ResourceImpl;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;


/**
 * this class parse the old repository.xml file build the bundle resource description and update the repository.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrUpdate
{
    /**
     * logger for this plugin.
     */
    private Log m_logger;

    /**
     * name and path to the repository descriptor file.
     */
    private URI m_repositoryXml;

    /**
     * name and path to the obr.xml file.
     */
    private URI m_obrXml;

    /**
     * maven project description.
     */
    private MavenProject m_project;

    /**
     * user configuration information.
     */
    private Config m_userConfig;

    /**
     * root on parent document.
     */
    private RepositoryImpl m_repository;

    /**
     * used to store bundle information.
     */
    private ResourceImpl m_resourceBundle;

    /**
     * base URI used to relativize bundle URIs.
     */
    private URI m_baseURI;


    /**
     * initialize information.
     * @param repositoryXml path to the repository descriptor file
     * @param obrXml path and filename to the obr.xml file
     * @param project maven project description
     * @param mavenRepositoryPath path to the local maven repository
     * @param userConfig user information
     * @param logger plugin logger
     */
    public ObrUpdate( URI repositoryXml, URI obrXml, MavenProject project, String mavenRepositoryPath,
        Config userConfig, Log logger )
    {
        m_repositoryXml = repositoryXml;
        m_obrXml = obrXml;
        m_project = project;
        m_logger = logger;

        m_userConfig = userConfig;

        if ( userConfig.isRemoteFile() )
        {
            m_baseURI = ObrUtils.toFileURI( mavenRepositoryPath );
        }
        else
        {
            m_baseURI = m_repositoryXml;
        }
    }


    /**
     * update the repository descriptor file. parse the old repository descriptor file,
     * get the old reference of the bundle or determine the id for a new bundle, extract
     * information from bindex set the new information in descriptor file and save it.
     * 
     * @param bundleJar path to the bundle jar file
     * @param sourceJar path to the source jar file
     * @param docJar path to the docs jar file
     * 
     * @throws MojoExecutionException if the plugin failed
     */
    public void updateRepository( URI bundleJar, URI sourceJar, URI docJar ) throws MojoExecutionException
    {
        m_logger.debug( " (f) repositoryXml = " + m_repositoryXml );
        m_logger.debug( " (f) bundleJar = " + bundleJar );
        m_logger.debug( " (f) sourceJar = " + sourceJar );
        m_logger.debug( " (f) obrXml = " + m_obrXml );

        if ( m_repository == null )
        {
            return;
        }

        // get the file size
        File bundleFile = new File( bundleJar );
        if ( bundleFile.exists() )
        {
            URI resourceURI = m_userConfig.getRemoteBundle();
            if ( null == resourceURI )
            {
                resourceURI = bundleJar;
                if ( m_userConfig.isPathRelative() )
                {
                    resourceURI = ObrUtils.getRelativeURI( m_baseURI, resourceURI );
                }
            }

            if ( m_userConfig.isRemoteFile() )
            {
                m_logger.info( "Deploying " + resourceURI );
            }
            else
            {
                m_logger.info( "Installing " + resourceURI );
            }

            try
            {
                m_resourceBundle = (ResourceImpl) new DataModelHelperImpl().createResource( bundleJar.toURL() );
                if (m_resourceBundle == null)
                {
                    return;
                }
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Unable to load resource information", e);
            }

            m_resourceBundle.put( Resource.SIZE, String.valueOf( bundleFile.length() ) );
            m_resourceBundle.put( Resource.URI, resourceURI.toASCIIString() );
        }
        else
        {
            m_logger.error( "file doesn't exist: " + bundleJar );
            return;
        }

        // parse the obr.xml file
        if ( m_obrXml != null )
        {
            m_logger.info( "Adding " + m_obrXml );

            // URL url = getClass().getResource("/SchemaObr.xsd");
            // TODO validate obr.xml file

            // add contents to resource bundle
            parseObrXml();
        }

        String sourcePath = relativisePath( sourceJar );
        String docPath = relativisePath( docJar );

//        m_resourceBundle.construct( m_project, bindexExtractor, sourcePath, docPath );
//         TODO: rebuild wrt m_project

        m_repository.addResource( m_resourceBundle );
        m_repository.setLastModified(System.currentTimeMillis());
    }


    private String relativisePath( URI uri )
    {
        if ( null != uri )
        {
            if ( m_userConfig.isPathRelative() )
            {
                return ObrUtils.getRelativeURI( m_baseURI, uri ).toASCIIString();
            }

            return uri.toASCIIString();
        }

        return null;
    }


    public void writeRepositoryXml() throws MojoExecutionException
    {
        m_logger.info( "Writing OBR metadata" );

        File file = null;
        Writer writer;
        try
        {
            file = File.createTempFile( "repository", ".xml" );
            writer = new OutputStreamWriter( new FileOutputStream( file ) );
        }
        catch ( IOException e )
        {
            m_logger.error( "Unable to write to file: " + file.getName() );
            e.printStackTrace();
            throw new MojoExecutionException( "Unable to write to file: " + file.getName() + " : " + e.getMessage() );
        }

        try
        {
            new DataModelHelperImpl().writeRepository(m_repository, writer);
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Unable to write repository xml", e );
        }

        try
        {
            writer.flush();
            writer.close();

            File outputFile = new File( m_repositoryXml );
            outputFile.getParentFile().mkdirs();
            FileUtils.rename( file, outputFile );
        }
        catch ( IOException e )
        {
            e.printStackTrace();
            throw new MojoExecutionException( "IOException" );
        }

    }


   /**
     * Parse the repository descriptor file.
     *
     * @throws MojoExecutionException if the plugin failed
     */
    public void parseRepositoryXml() throws MojoExecutionException
    {
        File fout = new File( m_repositoryXml );
        if ( !fout.exists() )
        {
            m_repository = new RepositoryImpl();
            writeRepositoryXml();
        }
        else
        {
            try
            {
                m_repository = (RepositoryImpl) new DataModelHelperImpl().repository( m_repositoryXml.toURL() );
            }
            catch ( Exception e )
            {
                throw new MojoExecutionException( "Unable to read repository xml: " + m_repositoryXml, e );
            }
        }
    }


    /**
     * put the information from obr.xml into ressourceBundle object.
     */
    private void parseObrXml() throws MojoExecutionException {
        try
        {
            InputStream is = new FileInputStream(new File(m_obrXml));
            try
            {
                KXmlParser kxp = new KXmlParser();
                kxp.setInput(is, null);
                kxp.nextTag(); // skip top level element
                kxp.nextTag(); // go to first child element
                parseObrXml(kxp);
            }
            finally
            {
                is.close();
            }
        }
        catch (Exception e)
        {
            throw new MojoExecutionException("Unable to parse obr xml: " + m_obrXml, e);
        }
    }

    private void parseObrXml(KXmlParser kxp) throws Exception
    {
        PullParser parser = new PullParser();
        while (kxp.getEventType() == XmlPullParser.START_TAG)
        {
            if (RepositoryParser.CATEGORY.equals(kxp.getName()))
            {
                m_resourceBundle.addCategory(parser.parseCategory(kxp));
            }
            else if (RepositoryParser.REQUIRE.equals(kxp.getName()))
            {
                m_resourceBundle.addRequire(parser.parseRequire(kxp));
            }
            else if (RepositoryParser.CAPABILITY.equals(kxp.getName()))
            {
                m_resourceBundle.addCapability(parser.parseCapability(kxp));
            }
            else
            {
                kxp.nextTag();
                parseObrXml(kxp);
            }
            kxp.nextTag();
        }
    }


}
