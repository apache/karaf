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
package org.apache.karaf.log.core.internal;

import org.apache.karaf.log.core.Level;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class LogServiceLog4j2XmlImpl implements LogServiceInternal {

    private static final String ELEMENT_LOGGERS = "Loggers";
    private static final String ELEMENT_ROOT = "Root";
    private static final String ELEMENT_LOGGER = "Logger";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_LEVEL = "level";
    private static final String ELEMENT_CONFIGURATION = "Configuration";

    private final Path path;

    LogServiceLog4j2XmlImpl(String file) {
        this.path = Paths.get(file);
    }

    public Map<String, String> getLevel(String logger) {
        try {
            Document doc = loadConfig(path);
            Map<String, Element> loggers = getLoggers(doc);

            Map<String, String> levels = new TreeMap<>();
            for (Map.Entry<String, Element> e : loggers.entrySet()) {
                String level = e.getValue().getAttribute(ATTRIBUTE_LEVEL);
                if (level != null && !level.isEmpty()) {
                    levels.put(e.getKey(), level);
                }
            }

            if (ALL_LOGGER.equals(logger)) {
                return levels;
            }
            String l = logger;
            String val;
            for (; ; ) {
                val = levels.get(l != null ? l : ROOT_LOGGER);
                if (val != null || l == null) {
                    return Collections.singletonMap(logger, val);
                }
                int idx = l.lastIndexOf('.');
                if (idx < 0) {
                    l = null;
                } else {
                    l = l.substring(0, idx);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve level for logger", e);
        }
    }

    public void setLevel(String logger, String level) {
        try {
            Document doc = loadConfig(path);
            Map<String, Element> loggers = getLoggers(doc);

            Element element = loggers.get(logger);
            if (element != null) {
                if (Level.isDefault(level)) {
                    element.removeAttribute(ATTRIBUTE_LEVEL);
                } else {
                    element.setAttribute(ATTRIBUTE_LEVEL, level);
                }
            }
            else if (!Level.isDefault(level)) {
                Element docE = doc.getDocumentElement();
                Element docLoggers = (Element) docE.getElementsByTagName(ELEMENT_LOGGERS).item(0);
                boolean root = ROOT_LOGGER.equals(logger);
                if (root) {
                    element = doc.createElement(ELEMENT_ROOT);
                    element.setAttribute(ATTRIBUTE_LEVEL, level);
                } else {
                    element = doc.createElement(ELEMENT_LOGGER);
                    element.setAttribute(ATTRIBUTE_NAME, logger);
                    element.setAttribute(ATTRIBUTE_LEVEL, level);
                }
                insertIndented(docLoggers, element, root);
            } else {
                return;
            }
            try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                TransformerFactory tFactory = TransformerFactory.newInstance();
                tFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
                try {
                    tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
                    tFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
                } catch (IllegalArgumentException e) {
                    // ignore
                }

                Transformer transformer = tFactory.newTransformer();
                transformer.transform(new DOMSource(doc), new StreamResult(os));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to set level for logger", e);
        }
    }

    /**
     * Insert the given node at beginning or end of the given node,
     * indenting it as needed.
     */
    static void insertIndented(Element loggers, Element element, boolean atBeginning) {
        NodeList loggerElements = loggers.getElementsByTagName("*");
        if (atBeginning && loggerElements.getLength() > 0) {
            Node insertBefore = loggers.getFirstChild();
            if (insertBefore != null) {
                if (insertBefore.getNodeType() == Node.TEXT_NODE) {
                    String indent = loggers.getFirstChild().getTextContent();
                    Node node = loggers.getOwnerDocument().createTextNode(indent);
                    loggers.insertBefore(node, insertBefore);
                }
                loggers.insertBefore(element, insertBefore);
            } else {
                loggers.appendChild(element);
            }
        } else {
            Node insertAfter = loggerElements.getLength() > 0 ? loggerElements.item(loggerElements.getLength() - 1) : null;
            if (insertAfter != null) {
                if (insertAfter.getPreviousSibling() != null && insertAfter.getPreviousSibling().getNodeType() == Node.TEXT_NODE) {
                    String indent = insertAfter.getPreviousSibling().getTextContent();
                    Node node = loggers.getOwnerDocument().createTextNode(indent);
                    if (insertAfter.getNextSibling() != null) {
                        loggers.insertBefore(node, insertAfter.getNextSibling());
                        insertAfter = node;
                    } else {
                        loggers.appendChild(node);
                    }
                }
                if (insertAfter.getNextSibling() != null) {
                    loggers.insertBefore(element, insertAfter.getNextSibling());
                } else {
                    loggers.appendChild(element);
                }
            } else {
                if (loggers.getPreviousSibling() != null && loggers.getPreviousSibling().getNodeType() == Node.TEXT_NODE) {
                    String indent = loggers.getPreviousSibling().getTextContent();
                    String prev = indent;
                    if (indent.endsWith("\t")) {
                        indent += "\t";
                    } else {
                        int nl = indent.lastIndexOf('\n');
                        if (nl >= 0) {
                            indent = indent + indent.substring(nl + 1);
                        } else {
                            indent += "\t";
                        }
                    }
                    if (loggers.getFirstChild() != null && loggers.getPreviousSibling().getNodeType() == Node.TEXT_NODE) {
                        loggers.removeChild(loggers.getFirstChild());
                    }
                    loggers.appendChild(loggers.getOwnerDocument().createTextNode(indent));
                    loggers.appendChild(element);
                    loggers.appendChild(loggers.getOwnerDocument().createTextNode(prev));
                } else {
                    loggers.appendChild(element);
                }
            }
        }

    }

    static Document loadConfig(Path path) throws Exception {
        try (InputStream is = Files.newInputStream(path)) {
            return loadConfig(path.toString(), is);
        }
    }

    static Document loadConfig(String id, InputStream is) throws ParserConfigurationException, SAXException, IOException {
        final InputSource source = new InputSource(is);
        source.setPublicId(id);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(false);
        factory.setExpandEntityReferences(false);

        setFeature(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
        setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
        setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
        setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        setFeature(factory, "http://apache.org/xml/features/xinclude/fixup-base-uris", true);
        setFeature(factory, "http://apache.org/xml/features/xinclude/fixup-language", true);
        tryCall(() -> factory.setXIncludeAware(true));
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        return documentBuilder.parse(source);
    }

    private static void setFeature(DocumentBuilderFactory factory, String name, boolean b) {
        tryCall(() -> factory.setFeature(name, b));
    }

    interface RunnableWithException {
        void run() throws Exception;
    }

    private static void tryCall(RunnableWithException c) {
        try {
            c.run();
        } catch (Exception e) {
            // Ignore
        }
    }

    private Map<String, Element> getLoggers(Document doc) {
        Map<String, Element> loggers = new TreeMap<>();
        Element docE = doc.getDocumentElement();
        if (!ELEMENT_CONFIGURATION.equals(docE.getLocalName())) {
            throw new IllegalArgumentException("Xml root document should be " + ELEMENT_CONFIGURATION);
        }
        NodeList children = docE.getElementsByTagName(ELEMENT_LOGGERS);
        if (children.getLength() != 1) {
            return Collections.emptyMap();
        }
        NodeList loggersList = children.item(0).getChildNodes();
        for (int i = 0; i < loggersList.getLength(); i++) {
            Node n = loggersList.item(i);
            if (n instanceof Element) {
                Element e = (Element) n;
                if (ELEMENT_ROOT.equals(e.getLocalName())) {
                    loggers.put(ROOT_LOGGER, e);
                } else if (ELEMENT_LOGGER.equals(e.getLocalName())) {
                    String name = e.getAttribute(ATTRIBUTE_NAME);
                    if (name != null) {
                        loggers.put(name, e);
                    }
                }
            }
        }
        return loggers;
    }

}
