/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.parser;

import java.util.Dictionary;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Manifest Metadata parser.
 * Read a manifest file and construct metadata
 * @author Clement Escoffier
 */
public class ManifestMetadataParser {

	/**
	 * Manifest Headers.
	 */
	private Dictionary m_headers;

	/**
	 * Element list.
	 */
	private Element[] m_elements = new Element[0];

    /**
     * @return the component metadata.
     * @throws ParseException when a parsing error occurs
     */
    public Element[] getComponentsMetadata() throws ParseException {
    	return m_elements[0].getElements("Component");
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

	/**
	 * Parse the given dictionnary and create the components manager.
	 * @param dict : the given headers of the manifest file
	 * @throws ParseException : if any error occurs
	 */
	public void parse(Dictionary dict) throws ParseException {
		m_headers = dict;
		String componentClassesStr = (String)m_headers.get("iPOJO-Components");
		//Add the ipojo element inside the element list
		addElement(new Element("iPOJO", ""));
		parseElements(componentClassesStr.trim());

	}

	/**
	 * Parse the metadata from the string given in argument.
	 * @param metadata : the metadata to parse
	 * @return Element : the root element resulting of the parsing
	 * @throws ParseException : if any error occurs
	 */
	public static Element parse(String metadata) throws ParseException  {
		ManifestMetadataParser parser = new ManifestMetadataParser();
		parser.parseElements(metadata);
		if (parser.m_elements.length != 1) { throw new ParseException("Error in parsing, root element not found : " + metadata); }
		return parser.m_elements[0];
	}

	private void parseElements(String s) {
		char[] string = s.toCharArray();

		for (int i = 0; i < string.length; i++) {
			char c = string[i];

			switch(c) {

			case '$' :
				String attName = "";
				String attValue = "";
				String attNs = "";
				i++;
				c = string[i];
				while (c != '=') {
					if (c == ':') {
						attNs = attName;
						attName = "";
					} else { attName = attName + c; }
					i = i + 1;
					c = string[i];
				}
				i++; // skip =
				c = string[i];
				while (c != ' ') {
					attValue = attValue + c;
					i++;
					c = string[i];
				}
				Attribute att = new Attribute(attName, attNs , attValue);
				m_elements[m_elements.length - 1].addAttribute(att);
				break;

			case '}' :
				Element lastElement = removeLastElement();
				if (m_elements.length != 0) {
					Element newQueue = m_elements[m_elements.length - 1];
					newQueue.addElement(lastElement);
				}
				else {
					addElement(lastElement);
				}
				break;
			case ' ' : break; // do nothing;
			default :
					String name = "";
					String ns = "";
					c = string[i];
					while (c != ' ') {
						if (c == ':') {
							ns = name;
							name = "";
							i++;
							c = string[i];
						}
						else {
							name = name + c;
							i++;
							c = string[i];
						}
					}
					// Skip spaces
					while (string[i] == ' ') { i = i + 1; }
					i = i + 1; // skip {
				    Element elem = new Element(name, ns);
					addElement(elem);
				break;
			}
			}
		}

}
