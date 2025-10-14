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
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LogServiceLogbackXmlImpl implements LogServiceInternal {

    private static final String ELEMENT_ROOT = "root";
    private static final String ELEMENT_LOGGER = "logger";
    private static final String ELEMENT_PROPERTY = "property";
    private static final String ELEMENT_VARIABLE = "variable";
    private static final String ATTRIBUTE_NAME = "name";
    private static final String ATTRIBUTE_LEVEL = "level";
    private static final String ATTRIBUTE_VALUE = "value";
    private static final String ELEMENT_CONFIGURATION = "configuration";

    private final Path path;

    LogServiceLogbackXmlImpl(String file) {
        this.path = Paths.get(file);
    }

    public Map<String, String> getLevel(String logger) {
        try {
            Document doc = loadConfig(path);
            Map<String, String> properties = getProperties(doc);
            Map<String, Element> loggers = getLoggers(doc);

            Map<String, String> levels = new TreeMap<>();
            for (Map.Entry<String, Element> e : loggers.entrySet()) {

                String level = e.getValue().getAttribute(ATTRIBUTE_LEVEL);
                if (!level.isEmpty()) {
                    levels.put(e.getKey(), resolveValue(level, properties, new Properties(System.getProperties()), new HashMap<>(System.getenv())));
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

    static String resolveValue(String value, Map<String, String> properties, Properties systemProperties, Map<String, String> envVariables) {
        // Pattern to match ${variable:-default}
        // At this moment only basic substitution is supported
        // i.e D${my.param:-EBUG} is not supported
        Pattern pattern = Pattern.compile("\\$\\{(.+?)(?::-(.+?))?}");
        Matcher matcher = pattern.matcher(value);

        if (matcher.matches()) {
            String variable = matcher.group(1);
            String defaultValue = matcher.group(2);
            // Remove found property to prevent cyclic loops
            // Check properties
            String resolved = properties.remove(variable);
            if (resolved == null) {
                // Check system properties
                resolved = systemProperties.getProperty(variable);
                systemProperties.remove(variable);
            }
            if (resolved == null) {
                // Check environment variables
                resolved = envVariables.remove(variable);
            }

            if (resolved != null) {
                // Check resolved variable again to susbstitute
                return resolveValue(resolved, properties, systemProperties, envVariables);
            } else {
                return defaultValue;
            }
        } else {
            return value;
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
                boolean root = ROOT_LOGGER.equals(logger);
                if (root) {
                    element = doc.createElement(ELEMENT_ROOT);
                    element.setAttribute(ATTRIBUTE_LEVEL, level);
                } else {
                    element = doc.createElement(ELEMENT_LOGGER);
                    element.setAttribute(ATTRIBUTE_NAME, logger);
                    element.setAttribute(ATTRIBUTE_LEVEL, level);
                }
                insertIndented(docE, element);
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
     * Insert the given node into the parent element,
     * indenting it as needed.
     */
    static void insertIndented(Element parent, Element element) {
        NodeList taggedElements = parent.getElementsByTagName("*");
        //only use direct descendants of parent element to insert next to
        ArrayList <Node> childElements = new ArrayList<>();
        for (int i = 0;i < taggedElements.getLength(); i++ ){
            if(taggedElements.item(i).getParentNode().equals(parent)){
                childElements.add(taggedElements.item(i));
            }
        }
        Node insertAfter = !childElements.isEmpty() ? childElements.get(childElements.size() - 1) : null;
            if (insertAfter != null) {
                if (insertAfter.getPreviousSibling() != null && insertAfter.getPreviousSibling().getNodeType() == Node.TEXT_NODE) {
                    String indent = insertAfter.getPreviousSibling().getTextContent();
                    Node node = parent.getOwnerDocument().createTextNode(indent);
                    if (insertAfter.getNextSibling() != null) {
                        parent.insertBefore(node, insertAfter.getNextSibling());
                        insertAfter = node;
                    } else {
                        parent.appendChild(node);
                    }
                }
                if (insertAfter.getNextSibling() != null ) {
                    parent.insertBefore(element, insertAfter.getNextSibling());
                } else {
                    parent.appendChild(element);
                }
            } else {
                String indent;
                String prev;
                if (parent.getPreviousSibling() != null && parent.getPreviousSibling().getNodeType() == Node.TEXT_NODE) {
                    indent = parent.getPreviousSibling().getTextContent();
                    prev = indent;
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
                    if (parent.getFirstChild() != null) {
                        parent.removeChild(parent.getFirstChild());
                    }
                } else {
                    indent = "\t";
                    prev = "\n";
                }
                parent.appendChild(parent.getOwnerDocument().createTextNode(indent));
                parent.appendChild(element);
                parent.appendChild(parent.getOwnerDocument().createTextNode(prev));
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
        NodeList loggersList = docE.getElementsByTagName(ELEMENT_LOGGER);
        for (int i = 0; i < loggersList.getLength(); i++) {
            Node n = loggersList.item(i);
            if (n instanceof Element) {
                Element e = (Element) n;
                if (ELEMENT_LOGGER.equals(e.getLocalName())) {
                    String name = e.getAttribute(ATTRIBUTE_NAME);
                    if (!name.isEmpty()) {
                        loggers.put(name, e);
                    }
                }
            }
        }
        // Handle root separately
        Node n = docE.getElementsByTagName(ELEMENT_ROOT).item(0);
        if (n instanceof Element) {
            Element e = (Element) n;
            if (ELEMENT_ROOT.equals(e.getLocalName())) {
                loggers.put(ROOT_LOGGER, e);
            }
        }
        return loggers;
    }

    private Map<String, String> getProperties(Document doc) {
        Map<String, String> properties = new TreeMap<>();
        Element docE = doc.getDocumentElement();
        if (!ELEMENT_CONFIGURATION.equals(docE.getLocalName())) {
            throw new IllegalArgumentException("Xml root document should be " + ELEMENT_CONFIGURATION);
        }
        NodeList propertyList = docE.getElementsByTagName(ELEMENT_PROPERTY);
        extractProperties(propertyList, ELEMENT_PROPERTY, properties);
        NodeList variableList = docE.getElementsByTagName(ELEMENT_VARIABLE);
        extractProperties(variableList, ELEMENT_VARIABLE, properties);
        return properties;
    }

    private static void extractProperties(NodeList propertyList, String elementName, Map<String, String> properties) {
        for (int i = 0; i < propertyList.getLength(); i++) {
            Node n = propertyList.item(i);
            if (n instanceof Element) {
                Element e = (Element) n;
                if (elementName.equals(e.getLocalName())) {
                    String name = e.getAttribute(ATTRIBUTE_NAME);
                    String value = e.getAttribute(ATTRIBUTE_VALUE);
                    if (!name.isEmpty()) {
                        properties.put(name, value);
                    }
                }
            }
        }
    }


}
