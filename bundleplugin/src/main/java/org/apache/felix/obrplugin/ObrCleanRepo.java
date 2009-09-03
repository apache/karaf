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
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Clean an OBR repository by finding and removing missing resources.
 * 
 * @requiresProject false
 * @goal clean
 * @phase clean
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ObrCleanRepo extends AbstractMojo
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


    public void execute()
    {
        if ( "NONE".equalsIgnoreCase( obrRepository ) || "false".equalsIgnoreCase( obrRepository ) )
        {
            getLog().info( "Local OBR clean disabled (enable with -DobrRepository)" );
            return;
        }

        try
        {
            // Compute local repository location
            URI repositoryXml = ObrUtils.findRepositoryXml( localRepository.getBasedir(), obrRepository );
            if ( !"file".equals( repositoryXml.getScheme() ) )
            {
                getLog().error( "The repository URI " + repositoryXml + " is not a local file" );
                return;
            }

            File repositoryFile = new File( repositoryXml );

            // Check if the file exist
            if ( !repositoryFile.exists() )
            {
                getLog().error( "The repository file " + repositoryFile + " does not exist" );
                return;
            }

            getLog().info( "Cleaning..." );

            Document doc = parseFile( repositoryFile, initConstructor() );
            Node finalDocument = cleanDocument( doc.getDocumentElement() ); // Analyze existing repository.

            if ( finalDocument == null )
            {
                getLog().info( "Nothing to clean in " + repositoryFile );
            }
            else
            {
                writeToFile( repositoryXml, finalDocument ); // Write the new file
                getLog().info( "Repository " + repositoryFile + " cleaned" );
            }
        }
        catch ( Exception e )
        {
            getLog().error( "Exception while cleaning local OBR: " + e.getLocalizedMessage(), e );
        }
    }


    /**
     * Analyze the given XML tree (DOM of the repository file) and remove missing resources.
     * 
     * @param elem : the input XML tree
     * @return the cleaned XML tree
     */
    private Element cleanDocument( Element elem )
    {
        String localRepoPath = localRepository.getBasedir();
        NodeList nodes = elem.getElementsByTagName( "resource" );
        List toRemove = new ArrayList();

        // First, look for missing resources
        for ( int i = 0; i < nodes.getLength(); i++ )
        {
            Element n = ( Element ) nodes.item( i );
            String value = n.getAttribute( "uri" );

            File file = new File( localRepoPath, value );
            if ( !file.exists() )
            {
                getLog().info(
                    "The bundle " + n.getAttribute( "presentationname" ) + " - " + n.getAttribute( "version" )
                        + " will be removed" );
                toRemove.add( n );
            }
        }

        Date d = new Date();
        if ( toRemove.size() > 0 )
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

        File fichier = null;
        FileOutputStream flux = null;
        try
        {
            fichier = File.createTempFile( "repository", ".xml" );
            flux = new FileOutputStream( fichier );
        }
        catch ( IOException e )
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

            FileUtils.rename( fichier, new File( outputFilename ) );
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "IOException when closing file : " + e.getMessage() );
        }
    }
}
