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
package org.apache.felix.bundlerepository.metadataparser.kxmlsax;

import java.util.Properties;

/**
 * Interface for SAX handler with kXML
 */
public interface KXml2SAXHandler
{
    /**
     * Method called when parsing text
     *
     * @param   ch
     * @param   offset
     * @param   length
     * @exception   SAXException
     */
    public void characters(char[] ch, int offset, int length) throws Exception;

    /**
     * Method called when a tag opens
     *
     * @param   uri
     * @param   localName
     * @param   qName
     * @param   attrib
     * @exception   SAXException
     **/
    public void startElement(
        String uri,
        String localName,
        String qName,
        Properties attrib)
        throws Exception;

    /**
     * Method called when a tag closes
     *
     * @param   uri
     * @param   localName
     * @param   qName
     * @exception   SAXException
     */
    public void endElement(
        java.lang.String uri,
        java.lang.String localName,
        java.lang.String qName)
        throws Exception;

    public void processingInstruction(String target,
        String data)
        throws Exception;

    public void setLineNumber(int lineNumber);

    public void setColumnNumber(int columnNumber);
}