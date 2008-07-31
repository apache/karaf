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
package org.apache.felix.bundlerepository.metadataparser;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.*;

import org.xml.sax.*;
import org.apache.felix.bundlerepository.Logger;

/**
 * handles the metadata in XML format
 */
public class XmlMetadataHandler extends MetadataHandler
{
    public XmlMetadataHandler(Logger logger)
    {
        super(logger);
    }

    /**
     * Called to parse the InputStream and set bundle list and package hash map
     */
    public void parse(InputStream istream) throws ParserConfigurationException, IOException, SAXException
    {
        // Parse the Meta-Data

        ContentHandler contenthandler = (ContentHandler) m_handler;

        InputSource is = new InputSource(istream);

        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false);

        SAXParser saxParser = spf.newSAXParser();

        XMLReader xmlReader = null;
        xmlReader = saxParser.getXMLReader();
        xmlReader.setContentHandler(contenthandler);
        xmlReader.parse(is);
    }
}