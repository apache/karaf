/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.bundlerepository.impl.metadataparser;

import java.io.*;

import org.apache.felix.bundlerepository.impl.kxmlsax.KXmlSAXParser;


/**
 * handles the metadata in XML format
 * (use kXML (http://kxml.enhydra.org/) a open-source very light weight XML parser
 * @version 	1.00 11 Nov 2003
 * @author 	Didier Donsez
 */
public class KXmlMetadataHandler /*implements MetadataHandler*/ {

	private XmlCommonHandler handler;

	public KXmlMetadataHandler() {
		handler = new XmlCommonHandler();
	}

	/**
	* Called to parse the InputStream and set bundle list and package hash map
	*/
	public void parse(InputStream is) throws Exception {
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		KXmlSAXParser parser;
		parser = new KXmlSAXParser(br);
		parser.parseXML(handler);
	}

	/**
	 * return the metadata
	 * @return a Objet
	 */
	public Object getMetadata() {
		return handler.getRoot();
	}

	public void addType(String qname, Class clazz) {
		handler.addType(qname, clazz);
	}

	public void setDefaultType(Class clazz) {
		handler.setDefaultType(clazz);
	}
}
