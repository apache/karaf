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
package org.apache.felix.ipojo.xml.parser;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

/**
 * XML Metadata parser.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class XMLMetadataParser implements ContentHandler {

    /**
     * Element of the metadata.
     */
    private Element[] m_elements = new Element[0];

    /**
     * Get parsed metadata.
     * The document must be parsed before calling this method. 
     * @return : all the metadata.
     * @throws ParseException : occurs if an error occurs during the parsing.
     */
    public Element[] getMetadata() throws ParseException {
        return m_elements[0].getElements();
    }


    /**
     * Characters.
     * @param ch : character
     * @param start : start
     * @param length : length
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException {
        // NOTHING TO DO

    }


    /**
     * End the document.
     * @throws SAXException : can never occrus.
     * @see org.xml.sax.ContentHandler#endDocument()
     */
    public void endDocument() throws SAXException {
    }


    /**
     * End of an element.
     * @param namespaceURI : element namespace
     * @param localName : local name
     * @param qName : qualified name
     * @throws SAXException : occurs when the element is malformed
     * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        // Get the last element of the list
        Element lastElement = removeLastElement();

        // Check if the name is consistent with the name of this end tag
        if (!lastElement.getName().equalsIgnoreCase(qName) && !lastElement.getNameSpace().equalsIgnoreCase(namespaceURI)) {
            throw new SAXException("Parse error when ending an element : " + qName + " [" + namespaceURI + "]");
        }

        // The name is consistent
        // Add this element last element with if it is not the root
        if (m_elements.length != 0) {
            Element newQueue = m_elements[m_elements.length - 1];
            newQueue.addElement(lastElement);
        } else {
            // It is the last element
            addElement(lastElement);
        }

    }

    /**
     * End prefix mapping.
     * @param prefix : ended prefix
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
     */
    public void endPrefixMapping(String prefix) throws SAXException {
        // NOTHING TO DO
    }


    /**
     * Ignore whitespace.
     * @param ch : character
     * @param start : start
     * @param length : length
     * @throws SAXException : can never occurs. 
     * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
     */
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        // NOTHING TO DO
    }

    /**
     * Process an instruction.
     * @param target : target
     * @param data : data
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction(String target, String data) throws SAXException {
        // DO NOTHING
    }

    /**
     * Set Document locator.
     * @param locator : new locator.
     * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
     */
    public void setDocumentLocator(Locator locator) {
        // NOTHING TO DO

    }

    /**
     * Skipped entity.
     * @param name : name.
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
     */
    public void skippedEntity(String name) throws SAXException {
        // NOTHING TO DO

    }

    /**
     * Start a document.
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#startDocument()
     */
    public void startDocument() throws SAXException {
    }


    /**
     * Start an element.
     * @param namespaceURI : element namespace.
     * @param localName : local element.
     * @param qName : qualified name.
     * @param atts : attribute
     * @throws SAXException : occurs if the element cannot be parsed correctly.
     * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException {
        Element elem = new Element(localName, namespaceURI);
        for (int i = 0; i < atts.getLength(); i++) {
            String name = (String) atts.getLocalName(i);
            String ns = (String) atts.getURI(i);
            String value = (String) atts.getValue(i);
            Attribute att = new Attribute(name, ns, value);
            elem.addAttribute(att);
        }

        addElement(elem);

    }

    /**
     * Start a prefix mapping.
     * @param prefix : prefix.
     * @param uri : uri.
     * @throws SAXException : can never occurs.
     * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
     */
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        // NOTHING TO DO

    }

    /**
     * Add an element.
     * @param elem : the element to add
     */
    private void addElement(Element elem) {
        for (int i = 0; (m_elements != null) && (i < m_elements.length); i++) {
            if (m_elements[i] == elem) {
                return;
            }
        }

        if (m_elements != null) {
            Element[] newElementsList = new Element[m_elements.length + 1];
            System.arraycopy(m_elements, 0, newElementsList, 0, m_elements.length);
            newElementsList[m_elements.length] = elem;
            m_elements = newElementsList;
        } else {
            m_elements = new Element[] { elem };
        }
    }

    /**
     * Remove an element.
     * @return : the removed element.
     */
    private Element removeLastElement() {
        int idx = -1;
        idx = m_elements.length - 1;
        Element last = m_elements[idx];
        if (idx >= 0) {
            if ((m_elements.length - 1) == 0) {
                // It is the last element of the list;
                m_elements = new Element[0];
            } else {
                // Remove the last element of the list :
                Element[] newElementsList = new Element[m_elements.length - 1];
                System.arraycopy(m_elements, 0, newElementsList, 0, idx);
                m_elements = newElementsList;
            }
        }
        return last;
    }
}
