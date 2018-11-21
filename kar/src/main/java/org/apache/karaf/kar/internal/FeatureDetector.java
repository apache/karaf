/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.kar.internal;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Simple helper to determine if a file is a feature repo
 */
class FeatureDetector {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureDetector.class);
    
    private DocumentBuilderFactory dbf;

    FeatureDetector() {
        dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (ParserConfigurationException ex) {
            //
        }
        dbf.setNamespaceAware(true);
    }
    /**
     * Check if a file is a features XML.
     *
     * @param artifact the file to check.
     * @return true if the artifact is a features XML, false else.
     */
    boolean isFeaturesRepository(File artifact) {
        try {
            if (artifact.isFile() && artifact.getName().endsWith(".xml") && !artifact.getName().startsWith("maven-metadata")) {
                Document doc = parse(artifact);
                String name = doc.getDocumentElement().getLocalName();
                String uri  = doc.getDocumentElement().getNamespaceURI();
                if ("features".equals(name) && (uri == null || "".equals(uri) || uri.startsWith("http://karaf.apache.org/xmlns/features/v"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("File '{}' is not a features file.", artifact.getName(), e);
        }
        return false;
    }
    
    /**
     * Parse a features XML.
     *
     * @param artifact the features XML to parse.
     * @return the parsed document.
     * @throws Exception in case of parsing failure.
     */
    private Document parse(File artifact) throws Exception {
        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setErrorHandler(new ErrorHandler() {
            public void warning(SAXParseException exception) throws SAXException {
            }
            public void error(SAXParseException exception) throws SAXException {
            }
            public void fatalError(SAXParseException exception) throws SAXException {
                throw exception;
            }
        });
        return db.parse(artifact);
    }

}
