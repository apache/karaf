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
package org.apache.felix.bundlerepository.impl.kxmlsax;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import org.kxml.Attribute;
import org.kxml.Xml;
import org.kxml.parser.ParseEvent;
import org.kxml.parser.XmlParser;

/**
 * The KXmlSAXParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 * @version 	1.0 08 Nov 2002
 * @version 	1.1 24 Apr 2004
 * @author 	Humberto Cervantes, Didier Donsez
 */
public class KXmlSAXParser extends XmlParser {
	/**
	* The constructor for a parser, it receives a java.io.Reader.
	*
	* @param   reader  The reader
	* @exception   IOException thrown by the superclass
	*/
	public KXmlSAXParser(Reader r) throws IOException {
		super(r);
	}

	/**
	* Parser from the reader provided in the constructor, and call
	* the startElement and endElement in a KxmlHandler
	*
	* @param   reader  The reader
	* @exception   Exception thrown by the superclass
	*/
	public void parseXML(KXmlSAXHandler handler) throws Exception {
		ParseEvent evt = null;
		do {
			evt = read();
			if (evt.getType() == Xml.START_TAG) {
				Properties props = new Properties();
				for (int i = 0; i < evt.getAttributeCount(); i++) {
					Attribute attr = evt.getAttribute(i);
					props.put(attr.getName(), attr.getValue());
				}
				handler.startElement(
					"uri",
					evt.getName(),
					evt.getName(),
					props);
			} else if (evt.getType() == Xml.END_TAG) {
				handler.endElement("uri", evt.getName(), evt.getName());
			} else if (evt.getType() == Xml.TEXT) {
				String text = evt.getText();
				handler.characters(text.toCharArray(),0,text.length());
			} else {
				// do nothing
			}
		} while (evt.getType() != Xml.END_DOCUMENT);
	}
}
