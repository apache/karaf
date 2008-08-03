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


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Clean a remote repository file.
 * It just looks for every resources and check that pointed file exists.
 * 
 * @requiresProject false
 * @goal remote-clean
 * @phase clean
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ObrRemoteClean extends AbstractMojo
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

        RemoteFileManager remoteFile = new RemoteFileManager( m_wagonManager, settings, log );
        openRepositoryConnection( remoteFile );
        if ( null == prefixUrl )
        {
            prefixUrl = remoteFile.toString();
        }

        // ======== LOCK REMOTE OBR ========
        log.info( "LOCK " + remoteFile + '/' + repositoryName );
        remoteFile.lockFile( repositoryName, ignoreLock );
        File downloadedRepositoryXml = null;

        try
        {
            // ======== DOWNLOAD REMOTE OBR ========
            log.info( "Downloading " + repositoryName );
            downloadedRepositoryXml = remoteFile.get( repositoryName, ".xml" );

            URI repositoryXml = downloadedRepositoryXml.toURI();

            Config userConfig = new Config();
            userConfig.setRemoteFile( true );

            // Clean the downloaded file.
            Document doc = parseFile( new File( repositoryXml ), initConstructor() );
            Node finalDocument = cleanDocument( doc.getDocumentElement() );

            if ( finalDocument == null )
            {
                getLog().info( "Nothing to clean in " + repositoryName );
            }
            else
            {
                writeToFile( repositoryXml, finalDocument ); // Write the new file
                getLog().info( "Repository " + repositoryName + " cleaned" );
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


    /**
     * Analyze the given XML tree (DOM of the repository file) and remove missing resources.
     * This method ask the user before deleting the resources from the repository.
     * @param elem : the input XML tree
     * @return the cleaned XML tree
     */
    private Element cleanDocument( Element elem )
    {
        NodeList nodes = elem.getElementsByTagName( "resource" );
        List toRemove = new ArrayList();

        // First, look for missing resources
        for ( int i = 0; i < nodes.getLength(); i++ )
        {
            Element n = ( Element ) nodes.item( i );
            String value = n.getAttribute( "uri" );

            URL url;
            try
            {
                url = new URL( new URL( prefixUrl + '/' ), value );
            }
            catch ( MalformedURLException e )
            {
                getLog().error( "Malformed URL when creating the resource absolute URI : " + e.getMessage() );
                return null;
            }

            try
            {
                url.openConnection().getContent();
            }
            catch ( IOException e )
            {
                getLog().info(
                    "The bundle " + n.getAttribute( "presentationname" ) + " - " + n.getAttribute( "version" )
                        + " will be removed : " + e.getMessage() );
                toRemove.add( n );
            }
        }

        Date d = new Date();
        if ( toRemove.size() > 0 )
        {
            System.out.println( "Do you want to remove these bundles from the repository file [y/N]:" );
            BufferedReader br = new BufferedReader( new InputStreamReader( System.in ) );
            String answer = null;

            try
            {
                answer = br.readLine();
            }
            catch ( IOException ioe )
            {
                getLog().error( "IO error trying to read the user confirmation" );
                return null;
            }

            if ( answer != null && answer.trim().equalsIgnoreCase( "y" ) )
            {
                // Then remove missing resources.
                for ( int i = 0; i < toRemove.size(); i++ )
                {
                    elem.removeChild( ( Node ) toRemove.get( i ) );
                }

                // If we have to remove resources, we need to update 'lastmodified' attribute
                SimpleDateFormat format = new SimpleDateFormat( "yyyyMMddHHmmss.SSS" );
                d.setTime( System.currentTimeMillis() );
                elem.setAttribute( "lastmodified", format.format( d ) );
                return elem;
            }
            else
            {
                return null;
            }
        }

        return null;
    }


    /**
     * Initialize the document builder from Xerces.
     * 
     * @return DocumentBuilder ready to create new document
     * @throws MojoExecutionException : occurs when the instantiation of the document builder fails
     */
    private DocumentBuilder initConstructor() throws MojoExecutionException
    {
        DocumentBuilder constructor = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try
        {
            constructor = factory.newDocumentBuilder();
        }
        catch ( ParserConfigurationException e )
        {
            getLog().error( "Unable to create a new xml document" );
            throw new MojoExecutionException( "Cannot create the Document Builder : " + e.getMessage() );
        }
        return constructor;
    }


    /**
     * Open an XML file.
     * 
     * @param filename : XML file path
     * @param constructor DocumentBuilder get from xerces
     * @return Document which describes this file
     * @throws MojoExecutionException occurs when the given file cannot be opened or is a valid XML file.
     */
    private Document parseFile( File file, DocumentBuilder constructor ) throws MojoExecutionException
    {
        if ( constructor == null )
        {
            return null;
        }
        // The document is the root of the DOM tree.
        File targetFile = file.getAbsoluteFile();
        getLog().info( "Parsing " + targetFile );
        Document doc = null;
        try
        {
            doc = constructor.parse( targetFile );
        }
        catch ( SAXException e )
        {
            getLog().error( "Cannot parse " + targetFile + " : " + e.getMessage() );
            throw new MojoExecutionException( "Cannot parse " + targetFile + " : " + e.getMessage() );
        }
        catch ( IOException e )
        {
            getLog().error( "Cannot open " + targetFile + " : " + e.getMessage() );
            throw new MojoExecutionException( "Cannot open " + targetFile + " : " + e.getMessage() );
        }
        return doc;
    }


    /**
     * write a Node in a xml file.
     * 
     * @param outputFilename URI to the output file
     * @param treeToBeWrite Node root of the tree to be write in file
     * @throws MojoExecutionException if the plugin failed
     */
    private void writeToFile( URI outputFilename, Node treeToBeWrite ) throws MojoExecutionException
    {
        // init the transformer
        Transformer transformer = null;
        TransformerFactory tfabrique = TransformerFactory.newInstance();
        try
        {
            transformer = tfabrique.newTransformer();
        }
        catch ( TransformerConfigurationException e )
        {
            getLog().error( "Unable to write to file: " + outputFilename.toString() );
            throw new MojoExecutionException( "Unable to write to file: " + outputFilename.toString() + " : "
                + e.getMessage() );
        }
        Properties proprietes = new Properties();
        proprietes.put( "method", "xml" );
        proprietes.put( "version", "1.0" );
        proprietes.put( "encoding", "ISO-8859-1" );
        proprietes.put( "standalone", "yes" );
        proprietes.put( "indent", "yes" );
        proprietes.put( "omit-xml-declaration", "no" );
        transformer.setOutputProperties( proprietes );

        DOMSource input = new DOMSource( treeToBeWrite );

        File fichier = new File( outputFilename );
        FileOutputStream flux = null;
        try
        {
            flux = new FileOutputStream( fichier );
        }
        catch ( FileNotFoundException e )
        {
            getLog().error( "Unable to write to file: " + fichier.getName() );
            throw new MojoExecutionException( "Unable to write to file: " + fichier.getName() + " : " + e.getMessage() );
        }
        Result output = new StreamResult( flux );
        try
        {
            transformer.transform( input, output );
        }
        catch ( TransformerException e )
        {
            throw new MojoExecutionException( "Unable to write to file: " + outputFilename.toString() + " : "
                + e.getMessage() );
        }

        try
        {
            flux.flush();
            flux.close();
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException when closing file : " + e.getMessage() );
        }
    }
}
