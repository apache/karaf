/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.features.internal.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.transform.sax.SAXSource;

import org.apache.karaf.features.FeaturesNamespaces;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLFilter;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

public class JaxbUtil {

    public static final XMLInputFactory XMLINPUT_FACTORY = XMLInputFactory.newInstance();
    private static final JAXBContext FEATURES_CONTEXT;
    static {
        try {
            FEATURES_CONTEXT = JAXBContext.newInstance(Features.class);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    public static void marshal(Features features, OutputStream out) throws JAXBException {
        Marshaller marshaller = FEATURES_CONTEXT.createMarshaller();

        marshaller.setProperty("jaxb.formatted.output", true);

        marshaller.marshal(features, out);
    }

    public static void marshal(Features features, Writer out) throws JAXBException {
        Marshaller marshaller = FEATURES_CONTEXT.createMarshaller();

        marshaller.setProperty("jaxb.formatted.output", true);

        marshaller.marshal(features, out);
    }


    /**
     * Read in a Features from the input stream.
     *
     * @param in       input stream to read
     * @param validate whether to validate the input.
     * @return a Features read from the input stream
     * @throws ParserConfigurationException is the SAX parser can not be configured
     * @throws SAXException                 if there is an xml problem
     * @throws JAXBException                if the xml cannot be marshalled into a T.
     */
    public static Features unmarshal(InputStream in, boolean validate) {
        InputSource inputSource = new InputSource(in);

        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setValidating(validate);
        SAXParser parser;
        try {
            parser = factory.newSAXParser();
        

        Unmarshaller unmarshaller = FEATURES_CONTEXT.createUnmarshaller();
        unmarshaller.setEventHandler(new ValidationEventHandler() {
            public boolean handleEvent(ValidationEvent validationEvent) {
                System.out.println(validationEvent);
                return false;
            }
        });

        XMLFilter xmlFilter = new NoSourceAndNamespaceFilter(parser.getXMLReader());
        xmlFilter.setContentHandler(unmarshaller.getUnmarshallerHandler());

        SAXSource source = new SAXSource(xmlFilter, inputSource);

        return (Features)unmarshaller.unmarshal(source);
        
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Provides an empty inputsource for the entity resolver.
     * Converts all elements with empty namespace to the features namespace to make old feature files 
     * compatible to the new format
     */
    public static class NoSourceAndNamespaceFilter extends XMLFilterImpl {
        private static final InputSource EMPTY_INPUT_SOURCE = new InputSource(new ByteArrayInputStream(new byte[0]));

        public NoSourceAndNamespaceFilter(XMLReader xmlReader) {
            super(xmlReader);
        }

        @Override
        public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
            return EMPTY_INPUT_SOURCE;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
            super.startElement(FeaturesNamespaces.URI_CURRENT, localName, qName, atts);
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            super.endElement(FeaturesNamespaces.URI_CURRENT, localName, qName);
        }
    }


}
