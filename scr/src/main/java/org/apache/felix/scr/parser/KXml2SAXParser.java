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
package org.apache.felix.scr.parser;

import java.io.Reader;
import java.util.Properties;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The KXml2SAXParser extends the XmlParser from kxml. This is a very
 * simple parser that does not take into account the DTD
 *
 */
public class KXml2SAXParser extends KXmlParser {

    /**
	* The constructor for a parser, it receives a java.io.Reader.
	*
	* @param   reader  The reader
	* @throws XmlPullParserException
	*/
	public KXml2SAXParser(Reader reader) throws XmlPullParserException {
		super();
	    this.setInput(reader);
	    this.setFeature(FEATURE_PROCESS_NAMESPACES, true);
	}

	/**
	* Parser from the reader provided in the constructor, and call
	* the startElement and endElement in a KxmlHandler
	*
	* @param   reader  The reader
	* @exception   Exception thrown by the superclass
	*/
	public void parseXML(KXml2SAXHandler handler) throws Exception {

		while (this.next() != XmlPullParser.END_DOCUMENT) {
			handler.setLineNumber(this.getLineNumber());
			handler.setColumnNumber(this.getColumnNumber());
			if (this.getEventType() == XmlPullParser.START_TAG) {
				Properties props = new Properties();
				for (int i = 0; i < this.getAttributeCount(); i++) {
					props.put(this.getAttributeName(i), this.getAttributeValue(i));
				}
				handler.startElement(
					this.getNamespace(),
					this.getName(),
					props);
			} else if (this.getEventType() == XmlPullParser.END_TAG) {
				handler.endElement(this.getNamespace(), this.getName());
			} else if (this.getEventType() == XmlPullParser.TEXT) {
				String text = this.getText();
				handler.characters(text);
			} else if (this.getEventType() == XmlPullParser.PROCESSING_INSTRUCTION) {
				// TODO extract the target from the evt.getText()
				handler.processingInstruction(null,this.getText());
			} else {
				// do nothing
			}
		}
	}
}
