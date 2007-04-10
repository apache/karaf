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
package org.apache.felix.ipojo.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.plugin.IPojoPluginConfiguration;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public class XMLMetadataParser implements ContentHandler {
	
	
	/**
	 * Element of the metadata.
	 */
	private Element[] m_elements = new Element[0];

    /**
     * @return a components metadata
     * @throws ParseException when an error occurs in the xml parsing
     */
    public Element[] getComponentsMetadata() throws ParseException {
    	Element[] comp = m_elements[0].getElements("Component");
    	Element[] compo = m_elements[0].getElements("Composite");
    	Element[] metadata = new Element[comp.length + compo.length];
    	int l = 0;
    	for(int i = 0; i < comp.length; i++) { metadata[l] = comp[i]; l++;}
    	for(int i = 0; i < compo.length; i++) { metadata[l] = compo[i]; l++;}
    	return metadata;
    }
    
    public List getReferredPackages() {
    	List referred = new ArrayList();
    	Element[] compo = m_elements[0].getElements("Composite");
    	for(int i= 0; i < compo.length; i++) {
    		for(int j = 0; j < compo[i].getElements().length; j++) {
    			if(compo[i].getElements()[j].containsAttribute("specification")) {
    				String p = compo[i].getElements()[j].getAttribute("specification");
    				int last = p.lastIndexOf('.');
    				if(last != -1) { referred.add(p.substring(0, last)); }
    			}
    		}
    	}
    	return referred;
    }
    
    public Element[] getMetadata() throws ParseException {
    	Element[] comp = m_elements[0].getElements("Component");
    	Element[] compo = m_elements[0].getElements("Composite");
    	Element[] conf = m_elements[0].getElements("Instance");
    	Element[] metadata = new Element[comp.length + conf.length + compo.length];
    	int l = 0;
    	for(int i = 0; i < comp.length; i++) { metadata[l] = comp[i]; l++;}
    	for(int i = 0; i < compo.length; i++) { metadata[l] = compo[i]; l++;}
    	for(int i = 0; i < conf.length; i++) { metadata[l] = conf[i]; l++;}
    	return metadata;
    }
    
    

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length)
			throws SAXException {
		// NOTHING TO DO

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endDocument()
	 */
	public void endDocument() throws SAXException {
		IPojoPluginConfiguration.getLogger().log(Level.INFO, "End of the XML parsing, " + m_elements.length + " primary elements found");

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String localName, String qName)
			throws SAXException {
	   	// Get the last element of the list
        Element lastElement = removeLastElement();

        // Check if the name is consitent with the name of this end tag
        if (!lastElement.getName().equalsIgnoreCase(qName) && !lastElement.getNameSpace().equals(namespaceURI)) {
        	IPojoPluginConfiguration.getLogger().log(Level.SEVERE, "Parse error when ending an element : " + qName + " [" + namespaceURI + "]");
        	throw new SAXException("Parse error when ending an element : " + qName + " [" + namespaceURI + "]");
        }

        // The name is consitent
        // Add this element last element with if it is not the root
        if (m_elements.length != 0) {
        	Element newQueue = m_elements[m_elements.length - 1];
        	newQueue.addElement(lastElement);
        }
        else {
        	// It is the last element
        	addElement(lastElement);
        }

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endPrefixMapping(java.lang.String)
	 */
	public void endPrefixMapping(String prefix) throws SAXException {
		//NOTHING TO DO

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#ignorableWhitespace(char[], int, int)
	 */
	public void ignorableWhitespace(char[] ch, int start, int length)
			throws SAXException {
		// NOTHING TO DO
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#processingInstruction(java.lang.String, java.lang.String)
	 */
	public void processingInstruction(String target, String data)
			throws SAXException {
		// DO NOTHING
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#setDocumentLocator(org.xml.sax.Locator)
	 */
	public void setDocumentLocator(Locator locator) {
		// NOTHING TO DO

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#skippedEntity(java.lang.String)
	 */
	public void skippedEntity(String name) throws SAXException {
		// NOTHING TO DO

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startDocument()
	 */
	public void startDocument() throws SAXException {
		IPojoPluginConfiguration.getLogger().log(Level.INFO, "Start of the XML parsing");

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String localName,
			String qName, Attributes atts) throws SAXException {
		IPojoPluginConfiguration.getLogger().log(Level.INFO, "An XML tag was openend : " + localName + " [" + namespaceURI + "]");

        Element elem = new Element(localName, namespaceURI);     
        
        for (int i = 0; i < atts.getLength(); i++) {
        	String name = (String)atts.getLocalName(i);
        	String ns = (String) atts.getURI(i);
        	String value = (String)atts.getValue(i);
        	Attribute att = new Attribute(name, ns, value);
        	elem.addAttribute(att);
        }

        addElement(elem);

	}

	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startPrefixMapping(java.lang.String, java.lang.String)
	 */
	public void startPrefixMapping(String prefix, String uri)
			throws SAXException {
		// NOTHING TO DO

	}
	
	private void addElement(Element elem) {
    	for (int i = 0; (m_elements != null) && (i < m_elements.length); i++) {
            if (m_elements[i] == elem) { return; }
        }

        if (m_elements != null) {
            Element[] newElementsList = new Element[m_elements.length + 1];
            System.arraycopy(m_elements, 0, newElementsList, 0, m_elements.length);
            newElementsList[m_elements.length] = elem;
            m_elements = newElementsList;
        }
        else { m_elements = new Element[] {elem}; }
	}
	
	private Element removeLastElement() {
		int idx = -1;
		idx = m_elements.length - 1;
		Element last = m_elements[idx];
        if (idx >= 0) {
            if ((m_elements.length - 1) == 0) {
            	// It is the last element of the list;
            	m_elements = new Element[0];
            	}
            else {
            	// Remove the last element of the list :
                Element[] newElementsList = new Element[m_elements.length - 1];
                System.arraycopy(m_elements, 0, newElementsList, 0, idx);
                m_elements = newElementsList;
            }
        }
        return last;
	}

}
