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
package org.cauldron.sigil.build;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

public class SiteInsertFeatures extends Task {

	private File siteXmlFile;
	private String features;
	private String versionPropPrefix;
	private String categoryPropPrefix;
	
	public File getSiteXmlFile() {
		return siteXmlFile;
	}
	public void setSiteXmlFile(File siteXmlFile) {
		this.siteXmlFile = siteXmlFile;
	}
	public String getFeatures() {
		return features;
	}
	public void setFeatures(String features) {
		this.features = features;
	}
	public String getVersionPropPrefix() {
		return versionPropPrefix;
	}
	public void setVersionPropPrefix(String versionPropPrefix) {
		this.versionPropPrefix = versionPropPrefix;
	}
	public String getCategoryPropPrefix() {
		return categoryPropPrefix;
	}
	public void setCategoryPropPrefix(String categoryPropPrefix) {
		this.categoryPropPrefix = categoryPropPrefix;
	}
	
	@Override
	public void execute() throws BuildException {
		Project project = getProject();
		
		List<Feature> featureList = new ArrayList<Feature>(); 
		StringTokenizer tokenizer = new StringTokenizer(features, ",");
		while(tokenizer.hasMoreTokens()) {
			Feature feature = new Feature();
			feature.id = tokenizer.nextToken().trim();
			
			// Find the version property
			String versionProp;
			if(versionPropPrefix == null) {
				versionProp = feature.id;
			} else {
				versionProp = versionPropPrefix + "." + feature.id;
			}
			feature.version = project.getProperty(versionProp);
			
			// Find the categories for this feature
			feature.categories = new String[0];
			if(categoryPropPrefix != null) {
				String categoriesStr = project.getProperty(categoryPropPrefix + "." + feature.id);
				if(categoriesStr != null) {
					StringTokenizer categoriesTokenizer = new StringTokenizer(categoriesStr, ",");
					feature.categories = new String[categoriesTokenizer.countTokens()];
					for(int i=0; i<feature.categories.length; i++) {
						feature.categories[i] = categoriesTokenizer.nextToken();
					}
				}
			}

			if(feature.version != null) {
				feature.url = "features/" + feature.id + "_" + feature.version + ".jar";
				featureList.add(feature);
			} else {
				System.out.println("Skipping feature " + feature.id);
			}
		}
		
		if(!siteXmlFile.isFile()) {
			throw new BuildException(siteXmlFile + " does not exist or is not a normal file");
		}
		try {
			// Generate new XML into a temporary file
			File tempFile = File.createTempFile("tmp", ".xml", siteXmlFile.getParentFile());
			tempFile.deleteOnExit();

			SAXTransformerFactory transformerFactory = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			TransformerHandler transformerHandler = transformerFactory.newTransformerHandler();
			transformerHandler.setResult(new StreamResult(tempFile));
			
			SAXParserFactory parserFactory = SAXParserFactory.newInstance();
			SAXParser parser = parserFactory.newSAXParser();
			
			SiteInsertFeatureContentHandler contentHandler = new SiteInsertFeatureContentHandler(transformerHandler, featureList);
			
			XMLReader reader = parser.getXMLReader();
			reader.setContentHandler(contentHandler);
			reader.parse(new InputSource(new FileInputStream(siteXmlFile)));
			
			// Backup original file
			File backup = new File(siteXmlFile.getParentFile(), siteXmlFile.getName() + ".bak");
			copyFile(siteXmlFile, backup);
			
			// Replace original file
			copyFile(tempFile, siteXmlFile);
			
		} catch (IOException e) {
			throw new BuildException(e);
		} catch (TransformerConfigurationException e) {
			throw new BuildException(e);
		} catch (IllegalArgumentException e) {
			throw new BuildException(e);
		} catch (TransformerFactoryConfigurationError e) {
			throw new BuildException(e);
		} catch (ParserConfigurationException e) {
			throw new BuildException(e);
		} catch (SAXException e) {
			throw new BuildException(e);
		}
	}
	
	private void copyFile(File source, File dest) throws IOException {
		FileInputStream in = null;
		FileOutputStream out = null;
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);
			
			byte[] buffer = new byte[1024];
			
			int read;
			while((read = in.read(buffer, 0, 1024)) > -1) {
				out.write(buffer, 0, read);
			}
		} finally {
			try { if(in != null) in.close(); } catch(IOException e) {}
			try { if(out != null) out.close(); } catch(IOException e) {}
		}
		
	}
}
