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
package org.apache.felix.scr.impl.parser;

import java.util.Properties;

/**
 * Interface for a SAX like handler with kXML
 */
public interface KXml2SAXHandler {

   /**
	* Method called when parsing text
	*
	* @param   text
	* @exception   ParseException
	*/
   void characters(String text) throws ParseException;

   /**
	* Method called when a tag opens
	*
	* @param   uri
	* @param   localName
	* @param   attrib
	* @exception   ParseException
	*/
	void startElement(
		String uri,
		String localName,
		Properties attrib)
		throws ParseException;

   /**
	* Method called when a tag closes
	*
	* @param   uri
	* @param   localName
	* @exception   ParseException
	*/
    void endElement(
		String uri,
		String localName)
		throws ParseException;

    void processingInstruction(String target,
									  String data)
							   throws Exception;

	void setLineNumber(int lineNumber);

	void setColumnNumber(int columnNumber);
}
