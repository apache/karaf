/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.deployer.war;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.felix.fileinstall.ArtifactUrlTransformer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * A deployment listener that listens for war deployements.
 */
public class WarDeploymentListener implements ArtifactUrlTransformer {

	private static final String PATH_SEPERATOR = "/";

	private static final Log LOGGER = LogFactory
			.getLog(WarDeploymentListener.class);

	private DocumentBuilderFactory dbf;

	public boolean canHandle(File artifact) {
		try {
			JarFile jar = new JarFile(artifact);
			JarEntry entry = jar.getJarEntry("WEB-INF/web.xml");
			// Only handle WAR artifacts
			if (entry == null) {
				return false;
			}
			// Only handle non OSGi bundles
			Manifest m = jar.getManifest();
			if (m!= null && m.getMainAttributes().getValue(
					new Attributes.Name("Bundle-SymbolicName")) != null
					&& m.getMainAttributes().getValue(
							new Attributes.Name("Bundle-Version")) != null) {
				return false;
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public URL transform(URL artifact) throws Exception {

		String path = artifact.getPath();
		String protocol = artifact.getProtocol();

		// match the last slash to retrieve the name of the archive
		int lastSlash = path.lastIndexOf(PATH_SEPERATOR);
		// match the suffix so we get rid of it for displaying
		int suffixPos = path.lastIndexOf(".war");

		// Fall back if there is no display-name set in the web.xml or if the
		// web.xml can't be read.
		String displayName = path.substring(lastSlash + 1, suffixPos);
		try {
			// step through the jar to find the web.xml
			JarInputStream jar = new JarInputStream(artifact.openStream());
			JarEntry nextJarEntry = jar.getNextJarEntry();
			boolean found = false;
			while (nextJarEntry != null) {
				if (nextJarEntry.getName().indexOf("web.xml") != -1
						&& !nextJarEntry.isDirectory()) {
					Document doc = parse(jar); // found the web.xml
					NodeList nodeList = doc.getDocumentElement()
							.getChildNodes(); // getElementsByTagName("display-name");
					for (int i = 0; i < nodeList.getLength(); i++) {
						Node item = nodeList.item(i);
						String nodeName = item.getNodeName();
						if ("display-name".equalsIgnoreCase(nodeName)) {
							String nodeValue = item.getFirstChild()
									.getNodeValue();
							if (nodeValue != null) {
								displayName = nodeValue;
							}
							found = true;
							jar.close();
							break;
						}
					}
				}
				if (found)
					break;
				else
					nextJarEntry = jar.getNextJarEntry();
			}
			// alter the original URL artifact
		} catch (Exception e) {
			LOGGER.warn("Unable to create Webapp-Context from web.xml", e);
		}
		return new URL("war", null, protocol + ":" + path + "?Webapp-Context="
				+ displayName);
	}

	private Document parse(InputStream inputStream)
			throws ParserConfigurationException, SAXException, IOException {
		if (dbf == null) {
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
		}

		DocumentBuilder db = dbf.newDocumentBuilder();
		db.setErrorHandler(new ErrorHandler() {

			public void warning(SAXParseException exception)
					throws SAXException {
				// ignore waring
			}

			public void fatalError(SAXParseException exception)
					throws SAXException {
				throw exception;
			}

			public void error(SAXParseException exception) throws SAXException {
				throw exception;
			}
		});

		return db.parse(inputStream);
	}

}
