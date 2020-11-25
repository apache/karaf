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
package org.apache.karaf.features.internal.service;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamResult;

import org.apache.karaf.features.internal.model.processing.FeaturesProcessing;
import org.apache.karaf.features.internal.model.processing.ObjectFactory;
import org.apache.karaf.util.xml.IndentingXMLEventWriter;
import org.ops4j.pax.swissbox.property.BundleContextPropertyResolver;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.ops4j.util.property.PropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.AttributesImpl;

/**
 * A class to help serialize {@link org.apache.karaf.features.internal.model.processing.FeaturesProcessing} model
 * but with added template comments for main sections of <code>org.apache.karaf.features.xml</code> file.
 */
public class FeaturesProcessingSerializer {

    public static Logger LOG = LoggerFactory.getLogger(FeaturesProcessingSerializer.class);

    private final BundleContext bundleContext;
    private JAXBContext FEATURES_PROCESSING_CONTEXT;

    public FeaturesProcessingSerializer() {
        Bundle bundle = FrameworkUtil.getBundle(this.getClass());
        this.bundleContext = bundle == null ? null : bundle.getBundleContext();
        try {
            FEATURES_PROCESSING_CONTEXT = JAXBContext.newInstance(ObjectFactory.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Reads {@link FeaturesProcessing features processing model} from input stream
     * @param stream
     * @return
     */
    public FeaturesProcessing read(InputStream stream) throws Exception {
        return this.read(stream, null);
    }

    /**
     * Reads {@link FeaturesProcessing features processing model} from input stream
     * @param stream
     * @param versions additional properties to resolve placeholders in features processing XML
     * @return
     */
    public FeaturesProcessing read(InputStream stream, Properties versions) throws Exception {
        Unmarshaller unmarshaller = FEATURES_PROCESSING_CONTEXT.createUnmarshaller();
        UnmarshallerHandler handler = unmarshaller.getUnmarshallerHandler();

        // BundleContextPropertyResolver gives access to e.g., ${karaf.base}
        final PropertyResolver resolver = bundleContext == null ? new DictionaryPropertyResolver(versions)
                : new DictionaryPropertyResolver(versions, new BundleContextPropertyResolver(bundleContext));

        // indirect unmarshaling with property resolution inside XML attribute values and CDATA
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        spf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        spf.setNamespaceAware(true);
        XMLReader xmlReader = spf.newSAXParser().getXMLReader();
        xmlReader.setContentHandler(new ResolvingContentHandler(new Properties() {
            @Override
            public String getProperty(String key) {
                return resolver.get(key);
            }

            @Override
            public String getProperty(String key, String defaultValue) {
                String value = resolver.get(key);
                return value == null ? defaultValue : value;
            }
        }, handler));
        xmlReader.parse(new InputSource(stream));

        return (FeaturesProcessing) handler.getResult();
    }

    /**
     * Writes the model to output stream and adds comments for main sections.
     * @param model
     * @param output
     */
    public void write(FeaturesProcessing model, OutputStream output) {
        try {
            // JAXB model as stream which is next parsed as XMLEvents
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = FEATURES_PROCESSING_CONTEXT.createMarshaller();
            marshaller.marshal(model, new StreamResult(baos));

            Map<String, Boolean> emptyElements = new HashMap<>();
            emptyElements.put("blacklistedRepositories", model.getBlacklistedRepositories().size() == 0);
            emptyElements.put("blacklistedFeatures", model.getBlacklistedFeatures().size() == 0);
            emptyElements.put("blacklistedBundles", model.getBlacklistedBundles().size() == 0);
            emptyElements.put("overrideBundleDependency", model.getOverrideBundleDependency().getRepositories().size()
                    + model.getOverrideBundleDependency().getFeatures().size()
                    + model.getOverrideBundleDependency().getBundles().size() == 0);
            emptyElements.put("bundleReplacements", model.getBundleReplacements().getOverrideBundles().size() == 0);
            emptyElements.put("featureReplacements", model.getFeatureReplacements().getReplacements().size() == 0);

            // A mix of direct write and stream of XML events. It's not easy (without knowing StAX impl) to
            // output self closed tags for example.
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!--\n");
            writer.write("    Configuration generated by Karaf Assembly Builder\n");
            writer.write("-->\n");
            writer.flush();

            Properties props = new Properties();
            props.load(getClass().getResourceAsStream("feature-processing-comments.properties"));

            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            XMLEventReader xmlEventReader = factory.createXMLEventReader(new ByteArrayInputStream(baos.toByteArray()));
            XMLEventWriter xmlEventWriter = new IndentingXMLEventWriter(XMLOutputFactory.newFactory().createXMLEventWriter(writer), "    ");
            XMLEventFactory evFactory = XMLEventFactory.newFactory();
            int depth = 0;
            boolean skipClose = false;
            while (xmlEventReader.hasNext()) {
                XMLEvent ev = xmlEventReader.nextEvent();
                int type = ev.getEventType();
                if (type != XMLEvent.START_DOCUMENT && type != XMLEvent.END_DOCUMENT) {
                    if (type == XMLEvent.START_ELEMENT) {
                        skipClose = false;
                        depth++;
                        if (depth == 2) {
                            String tag = ev.asStartElement().getName().getLocalPart();
                            String comment = props.getProperty(tag);
                            xmlEventWriter.add(evFactory.createCharacters("\n    "));
                            xmlEventWriter.add(evFactory.createComment(" " + comment + " "));
                        }
                    } else if (type == XMLEvent.END_ELEMENT) {
                        skipClose = false;
                        depth--;
                        if (depth == 1) {
                            String tag = ev.asEndElement().getName().getLocalPart();
                            String comment = props.getProperty(tag);
                        }
                    }
                    if (type == XMLEvent.END_ELEMENT && depth == 0) {
                        xmlEventWriter.add(evFactory.createCharacters("\n"));
                    }
                    if (!skipClose) {
                        xmlEventWriter.add(ev);
                    }
                    if (type == XMLEvent.START_ELEMENT && depth == 1) {
                        xmlEventWriter.add(evFactory.createCharacters("\n"));
                    }
                }
            }
            xmlEventWriter.add(evFactory.createEndDocument());
            writer.flush();
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    private static class ResolvingContentHandler implements ContentHandler {

        public static Logger LOG = LoggerFactory.getLogger(ResolvingContentHandler.class);

        private Properties properties;
        private ContentHandler target;

        private boolean inElement = false;
        private StringWriter sw = new StringWriter();

        public ResolvingContentHandler(Properties properties, ContentHandler target) {
            this.properties = properties;
            this.target = target;
        }

        @Override
        public void setDocumentLocator(Locator locator) {
            target.setDocumentLocator(locator);
        }

        @Override
        public void startDocument() throws SAXException {
            target.startDocument();
        }

        @Override
        public void endDocument() throws SAXException {
            target.endDocument();
        }

        @Override
        public void startPrefixMapping(String prefix, String uri) throws SAXException {
            target.startPrefixMapping(prefix, uri);
        }

        @Override
        public void endPrefixMapping(String prefix) throws SAXException {
            target.endPrefixMapping(prefix);
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            AttributesImpl resolvedAttributes = new AttributesImpl(atts);
            for (int i = 0; i < atts.getLength(); i++) {
                resolvedAttributes.setAttribute(i, atts.getURI(i), atts.getLocalName(i), atts.getQName(i),
                        atts.getType(i), resolve(atts.getValue(i)));
            }
            if (inElement) {
                flushBuffer(false);
            }
            inElement = true;
            target.startElement(uri, localName, qName, resolvedAttributes);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (inElement) {
                flushBuffer(true);
                inElement = false;
            }
            target.endElement(uri, localName, qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (inElement) {
                sw.append(new String(ch, start, length));
            } else {
                target.characters(ch, start, length);
            }
        }

        @Override
        public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
            // only elements without PCDATA in DTD have whitespace passed to this method. so ignore
            target.ignorableWhitespace(ch, start, length);
        }

        @Override
        public void processingInstruction(String target, String data) throws SAXException {
            this.target.processingInstruction(target, data);
        }

        @Override
        public void skippedEntity(String name) throws SAXException {
            target.skippedEntity(name);
        }

        /**
         * Pass collected characters to target {@link ContentHandler}
         * @param resolve whether to expect placeholders in collected text
         */
        private void flushBuffer(boolean resolve) throws SAXException {
            String value = sw.toString();
            String resolved = resolve ? resolve(value) : value;

            target.characters(resolved.toCharArray(), 0, resolved.length());
            sw = new StringWriter();
        }

        private String resolve(String value) {
            String resolved = org.ops4j.util.collections.PropertyResolver.resolve(properties, value);
            if (resolved.contains("${")) {
                // there are still unresolved properties - just log warning
                LOG.warn("Value {} has unresolved properties, please check configuration.", value);
            }
            return resolved;
        }

    }

}
