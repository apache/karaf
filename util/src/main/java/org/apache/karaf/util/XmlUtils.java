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
package org.apache.karaf.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 * Utils class to manipulate XML document in a thread safe way.
 */
public class XmlUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlUtils.class);

    private static final ThreadLocal<DocumentBuilderFactory> DOCUMENT_BUILDER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<TransformerFactory> TRANSFORMER_FACTORY = new ThreadLocal<>();
    private static final ThreadLocal<SAXParserFactory> SAX_PARSER_FACTORY = new ThreadLocal<>();

    public static Document parse(String uri) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(uri);
        } finally {
            db.reset();
        }
    }

    public static Document parse(InputStream stream) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(stream);
        } finally {
            db.reset();
        }
    }

    public static Document parse(File f) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        try {
            return db.parse(f);
        } finally {
            db.reset();
        }
    }

    public static Document parse(File f, ErrorHandler errorHandler) throws TransformerException, IOException, SAXException, ParserConfigurationException {
        DocumentBuilder db = documentBuilder();
        db.setErrorHandler(errorHandler);
        try {
            return db.parse(f);
        } finally {
            db.reset();
        }
    }

    public static void transform(Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer();
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static void transform(Source xsltSource, Source xmlSource, Result outputTarget) throws TransformerException {
        Transformer t = transformer(xsltSource);
        try {
            t.transform(xmlSource, outputTarget);
        } finally {
            t.reset();
        }
    }

    public static XMLReader xmlReader() throws ParserConfigurationException, SAXException {
        SAXParserFactory spf = SAX_PARSER_FACTORY.get();
        if (spf == null) {
            spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            spf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            spf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            spf.setXIncludeAware(false);
            SAX_PARSER_FACTORY.set(spf);
        }
        return spf.newSAXParser().getXMLReader();
    }

    public static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DOCUMENT_BUILDER_FACTORY.get();
        if (dbf == null) {
            dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            DOCUMENT_BUILDER_FACTORY.set(dbf);
        }
        return dbf.newDocumentBuilder();
    }

    public static Transformer transformer() throws TransformerConfigurationException {
        TransformerFactory tf = TRANSFORMER_FACTORY.get();
        if (tf == null) {
            tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            } catch (IllegalArgumentException e) {
                LOGGER.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_DTD);
            }
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (IllegalArgumentException e) {
                LOGGER.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_STYLESHEET);
            }
            TRANSFORMER_FACTORY.set(tf);
        }
        return tf.newTransformer();
    }

    private static Transformer transformer(Source xsltSource) throws TransformerConfigurationException {
        TransformerFactory tf = TRANSFORMER_FACTORY.get();
        if (tf == null) {
            tf = TransformerFactory.newInstance();
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            } catch (IllegalArgumentException e) {
                LOGGER.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_DTD);
            }
            try {
                tf.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            } catch (IllegalArgumentException e) {
                LOGGER.warn("XSL transformer implementation doesn't support {} feature", XMLConstants.ACCESS_EXTERNAL_STYLESHEET);
            }
            TRANSFORMER_FACTORY.set(tf);
        }
        return tf.newTransformer(xsltSource);
    }

}
