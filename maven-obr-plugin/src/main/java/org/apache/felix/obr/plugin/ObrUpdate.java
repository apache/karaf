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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * this class parse the old repository.xml file build the bundle resource description and update the repository.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */

public class ObrUpdate {
    /**
     * generate the date format to insert it in repository descriptor file.
     */
    static SimpleDateFormat m_format = new SimpleDateFormat("yyyyMMddHHmmss.SSS");

    /**
     * logger for this plugin.
     */
    private Log m_logger;

    /**
     * name and path to the repository descriptor file.
     */
    private URI m_repoFilename;

    /**
     * name and path to the obr.xml file.
     */
    private String m_obrXml;

    /**
     * name and path to the bundle jar file.
     */
    private String m_outputFile;

    /**
     * maven project description.
     */
    private MavenProject m_project;

    /**
     * user configuration information.
     */
    private Config m_userConfig;

    /**
     * used to build another xml document.
     */
    private DocumentBuilder m_constructor;

    /**
     * root on parent document.
     */
    private Document m_repoDoc;

    /**
     * first Node on repository descriptor tree.
     */
    private Node m_root;

    /**
     * used to extract bindex bundle information.
     */
    private ExtractBindexInfo m_ebi;

    /**
     * used to store bundle information.
     */
    private ResourcesBundle m_resourceBundle;

    /**
     * pathfile to the repository descriptor file.
     */
    private PathFile m_repo;

    /**
     * initialize information.
     * @param repoFilename path to the repository descriptor file
     * @param obrXml path and filename to the obr.xml file
     * @param project maven project description
     * @param outputFile path to the bundle jar file
     * @param localRepo only path to the local repository
     * @param userConfig user information
     * @param log plugin logger
     */
    public ObrUpdate(PathFile repoFilename, String obrXml, MavenProject project, String outputFile, String localRepo, Config userConfig, Log log) {
        // this.m_localRepo = localRepo;
        this.m_outputFile = outputFile;
        this.m_repoFilename = repoFilename.getUri();
        this.m_obrXml = obrXml;
        this.m_project = project;
        this.m_logger = log;

        this.m_userConfig = userConfig;

        m_resourceBundle = new ResourcesBundle(log);

        if (userConfig.isRemotely()) {
            this.m_repo = new PathFile(localRepo);
        } else {
            this.m_repo = repoFilename;
        }
    }

    /**
     * update the repository descriptor file. parse the old repository descriptor file, get the old reference of the bundle or determine the id for a new bundle, extract information from bindex set the new information in descriptor file and save it.
     * @throws MojoExecutionException if the plugin failed
     */
    public void updateRepository() throws MojoExecutionException {
    	
    	m_logger.debug(" (f) m_obrXml = " + m_obrXml);
    	m_logger.debug(" (f) m_outputFile = " + m_outputFile);
    	m_logger.debug(" (f) m_repoFilename = " + m_repoFilename);

        m_constructor = initConstructor();

        if (m_constructor == null)  {
            return;
        }

        // get the file size
        PathFile pf = new PathFile(m_outputFile);
        File fout = pf.getFile();
        pf.setBaseDir(m_repo.getOnlyAbsolutePath());
        if (fout.exists()) {
            m_resourceBundle.setSize(String.valueOf(fout.length()));
            if (m_userConfig.isPathRelative()) {
                m_resourceBundle.setUri(pf.getOnlyRelativeFilename().replace('\\', '/'));
            } else {
                m_resourceBundle.setUri("file:" + m_outputFile);
            }
        } else {
            m_logger.error("file doesn't exist: " + m_outputFile);
            return;
        }

        // parse repository
        if (parseRepositoryXml() == -1) { return; }

        // parse the obr.xml file
        if (m_obrXml != null) {
            // URL url = getClass().getResource("/SchemaObr.xsd");
            // TODO validate obr.xml file

            Document obrXmlDoc = parseFile(m_obrXml, m_constructor);
            if (obrXmlDoc == null) { return; }
            Node obrXmlRoot = (Element) obrXmlDoc.getDocumentElement();
            // sort the obr file
            sortObrXml(obrXmlRoot);
        }

        // use bindex to extract bundle information
        try {
            m_ebi = new ExtractBindexInfo(m_repoFilename, m_outputFile);
        } catch (MojoExecutionException e) {
            m_logger.error("unable to build Bindex informations");
            e.printStackTrace();
            throw new MojoExecutionException("MojoFailureException");
        }

        m_resourceBundle.construct(m_project, m_ebi);

        if (!walkOnTree(m_root)) {
            // the correct resource node was not found, we must create it
            // we compute the new id
            String id = m_resourceBundle.getId();
            searchRepository(m_root, id);
        }

        // the repository.xml file have been modified, so we save it
        m_logger.info("Writing OBR metadata");
        writeToFile(m_repoFilename, m_root);
    }

    /**
     * init the document builder from xerces.
     * @return DocumentBuilder ready to create new document
     */
    private DocumentBuilder initConstructor() {
        DocumentBuilder constructor = null;
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        try {
            constructor = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            m_logger.error("unable to create a new xml document");
            e.printStackTrace();
        }
        return constructor;

    }

    /**
     * Parse the repository descriptor file.
     * 
     * @return 0 if the bundle is already in the descriptor, else -1
     * @throws MojoExecutionException if the plugin failed
     */
    private int parseRepositoryXml() throws MojoExecutionException {

        File fout = new File(m_repoFilename);
        if (!fout.exists()) {
            // create the repository.xml
            try {
            	fout.createNewFile();
                m_logger.info("Created "+fout.getAbsolutePath());
            } catch (IOException e) {
                m_logger.error("Cannot create file " + fout.getAbsolutePath());
                e.printStackTrace();
                return -1;
            }

            Document doc = m_constructor.newDocument();
            // create xml tree
            Date d = new Date();
            d.setTime(System.currentTimeMillis());
            Element root = doc.createElement("repository");
            root.setAttribute("lastmodified", m_format.format(d));
            root.setAttribute("name", "MyRepository");
            try {
                writeToFile(m_repoFilename, root);
            } catch (MojoExecutionException e) {
                e.printStackTrace();
                throw new MojoExecutionException("MojoExecutionException");
            }
        }

        // now we parse the repository.xml file
        m_repoDoc = parseFile(fout.getAbsolutePath(), m_constructor);
        if (m_repoDoc == null) { return -1; }

        m_root = (Element) m_repoDoc.getDocumentElement();
        return 0;
    }

    /**
     * transform a xml file to a xerces Document.
     * @param filename path to the xml file
     * @param constructor DocumentBuilder get from xerces
     * @return Document which describe this file
     */
    private Document parseFile(String filename, DocumentBuilder constructor) {
        if (constructor == null) { return null; }
        // The document is the root of the DOM tree.
        m_logger.info("Parsing " + filename);
        Document doc = null;
        try {
            doc = constructor.parse(new File(filename));
        } catch (SAXException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            m_logger.error("cannot open file: " + filename);
            e.printStackTrace();
            return null;
        }
        return doc;
    }

    /**
     * put the information from obr.xml into ressourceBundle object.
     * @param node Node to the OBR.xml file
     */
    private void sortObrXml(Node node) {
        if (node.getNodeName().compareTo("require") == 0) {
            Require newRequireNode = new Require();
            NamedNodeMap list = node.getAttributes();
            try {
                newRequireNode.setExtend(list.getNamedItem("extend").getNodeValue());
                newRequireNode.setMultiple(list.getNamedItem("multiple").getNodeValue());
                newRequireNode.setOptional(list.getNamedItem("optional").getNodeValue());
                newRequireNode.setFilter(list.getNamedItem("filter").getNodeValue());
                newRequireNode.setName(list.getNamedItem("name").getNodeValue());
            } catch (NullPointerException e) {
                m_logger.error("the obr.xml file seems to be invalid in a \"require\" tag (one or more attributes are missing)");
                // e.printStackTrace();
            }
            newRequireNode.setValue(XmlHelper.getTextContent(node));
            m_resourceBundle.addRequire(newRequireNode);
        } else if (node.getNodeName().compareTo("capability") == 0) {
            Capability newCapability = new Capability();
            try {
                newCapability.setName(node.getAttributes().getNamedItem("name").getNodeValue());
            } catch (NullPointerException e) {
                m_logger.error("attribute \"name\" is missing in obr.xml in a \"capability\" tag");
                e.printStackTrace();
            }
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                PElement p = new PElement();
                Node n = list.item(i);
                Node item = null;
                // System.err.println(n.getNodeName());
                if (n.getNodeName().compareTo("p") == 0) {

                    p.setN(n.getAttributes().getNamedItem("n").getNodeValue());
                    item = n.getAttributes().getNamedItem("t");
                    if (item != null) { p.setT(item.getNodeValue()); }
                    item = n.getAttributes().getNamedItem("v");
                    if (item != null) { p.setV(item.getNodeValue()); }

                    newCapability.addP(p);
                }
            }
            m_resourceBundle.addCapability(newCapability);
        } else if (node.getNodeName().compareTo("category") == 0) {
            Category newCategory = new Category();
            newCategory.setId(node.getAttributes().getNamedItem("id").getNodeValue());
            m_resourceBundle.addCategory(newCategory);
        } else {
            NodeList list = node.getChildNodes();
            for (int i = 0; i < list.getLength(); i++) {
                sortObrXml(list.item(i));
            }
        }
    }

    /**
     * write a Node in a xml file.
     * @param outputFilename URI to the output file
     * @param treeToBeWrite Node root of the tree to be write in file
     * @throws MojoExecutionException if the plugin failed
     */
    private void writeToFile(URI outputFilename, Node treeToBeWrite) throws MojoExecutionException {
        // init the transformer
        Transformer transformer = null;
        TransformerFactory tfabrique = TransformerFactory.newInstance();
        try {
            transformer = tfabrique.newTransformer();
        } catch (TransformerConfigurationException e) {
            m_logger.error("Unable to write to file: " + outputFilename.toString());
            e.printStackTrace();
            throw new MojoExecutionException("TransformerConfigurationException");
        }
        Properties proprietes = new Properties();
        proprietes.put("method", "xml");
        proprietes.put("version", "1.0");
        proprietes.put("encoding", "ISO-8859-1");
        proprietes.put("standalone", "yes");
        proprietes.put("indent", "yes");
        proprietes.put("omit-xml-declaration", "no");
        transformer.setOutputProperties(proprietes);

        DOMSource input = new DOMSource(treeToBeWrite);

        File fichier = new File(outputFilename);
        FileOutputStream flux = null;
        try {
            flux = new FileOutputStream(fichier);
        } catch (FileNotFoundException e) {
            m_logger.error("Unable to write to file: " + fichier.getName());
            e.printStackTrace();
            throw new MojoExecutionException("FileNotFoundException");
        }
        Result output = new StreamResult(flux);
        try {
            transformer.transform(input, output);
        } catch (TransformerException e) {
            e.printStackTrace();
            throw new MojoExecutionException("TransformerException");
        }

        try {
            flux.flush();
            flux.close();
        } catch (IOException e) {
            e.printStackTrace();
            throw new MojoExecutionException("IOException");
        }

    }

    /**
     * walk on the tree until the targeted node was found.
     * @param node targeted node
     * @return true if the requiered node was found else false.
     */
    private boolean walkOnTree(Node node) {

        if (node.getNodeName().compareTo("resource") == 0) {
            return resource(node);
        } else { // look at the repository node (first in the file)
            if (node.getNodeName().compareTo("repository") == 0) {
                Date d = new Date();
                d.setTime(System.currentTimeMillis());
                NamedNodeMap nList = node.getAttributes();
                Node n = nList.getNamedItem("lastmodified");
                n.setNodeValue(m_format.format(d));
            }
            NodeList list = node.getChildNodes();
            if (list.getLength() > 0) {
                for (int i = 0; i < list.getLength(); i++) {
                    if (walkOnTree(list.item(i))) { return true; }
                }
            }
            return false;
        }

    }

    /**
     * put the resource bundle in the tree.
     * @param node Node on the xml file
     * @param id id of the bundle ressource
     */
    private void searchRepository(Node node, String id) {
        if (node.getNodeName().compareTo("repository") == 0) {
            node.appendChild(m_resourceBundle.getNode(m_repoDoc));
            return;
        } else {
            System.out.println("Second branch...");
            NodeList list = node.getChildNodes();
            if (list.getLength() > 0) {
                for (int i = 0; i < list.getLength(); i++) {
                    searchRepository(list.item(i), id);
                }
            }
        }

    }

    /**
     * compare two node and update the array which compute the smallest free id.
     * @param node : node
     * @return true if the node is the same bundle than the ressourceBundle, else false.
     */
    private boolean resource(Node node) {

        // this part save all the id free if we need to add resource
        String id = node.getAttributes().getNamedItem("id").getNodeValue();
        NamedNodeMap map = node.getAttributes();

        if (m_resourceBundle.isSameBundleResource(map.getNamedItem("symbolicname").getNodeValue(), map.getNamedItem("version").getNodeValue())) {
            m_resourceBundle.setId(String.valueOf(id));
            node.getParentNode().replaceChild(m_resourceBundle.getNode(m_repoDoc), node);
            return true;
        }
        return false;
    }
}
