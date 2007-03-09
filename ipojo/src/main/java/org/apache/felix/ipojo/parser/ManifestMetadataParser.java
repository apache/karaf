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

import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Manifest Metadata parser.
 * Read a manifest file and construct metadata
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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

    /**
     * @return the instances list.
     * @throws ParseException : if the metadata cannot be parsed successfully
     */
    public Dictionary[] getInstances() throws ParseException {
        Element[] configs = m_elements[0].getElements("Instance");
        Dictionary[] dicts = new Dictionary[configs.length];
        for (int i = 0; i < configs.length; i++) {
            dicts[i] = parseInstance(configs[i]);
        }
        return dicts;
    }
    
    /**
     * Parse an Element to get a dictionary.
     * @param instance : the Element describing an instance.
     * @return : the resulting dictionary
     * @throws ParseException
     */
    private Dictionary parseInstance(Element instance) throws ParseException {
    	Dictionary dict = new Properties();
    	if (!instance.containsAttribute("name")) { throw new ParseException("An instance does not have the 'name' attribute"); }
    	if (!instance.containsAttribute("component")) { throw new ParseException("An instance does not have the 'component' attribute"); }
    	dict.put("name", instance.getAttribute("name"));
    	dict.put("component", instance.getAttribute("component"));
    	
    	for (int i = 0; i < instance.getElements("property").length; i++) {
    		parseProperty(instance.getElements("property")[i], dict);
    	}
    	
    	return dict;
    }
    
    /**
     * @param prop
     * @param dict
     * @throws ParseException
     */
    private void parseProperty(Element prop, Dictionary dict) throws ParseException {
    	// Check that the property has a name 
    	if (!prop.containsAttribute("name")) { throw new ParseException("A property does not have the 'name' attribute"); }
    	// Final case : the property element has a 'value' attribute
    	if (prop.containsAttribute("value")) { 
    		dict.put(prop.getAttribute("name"), prop.getAttribute("value")); 
    	} else {
    		// Recursive case
    		// Check if there is 'property' element
    		Element[] subProps = prop.getElements("property");
    		if (subProps.length == 0) { throw new ParseException("A complex property must have at least one 'property' sub-element"); }
    		Dictionary dict2 = new Properties();
    		for (int i = 0; i < subProps.length; i++) {
    			parseProperty(subProps[i], dict2);
    			dict.put(prop.getAttribute("name"), dict2);
    		}
    	}
    }

    /**
     * Add an element to the list.
     * @param elem : the element to add
     */
    private void addElement(Element elem) {
        if (m_elements != null) {
            Element[] newElementsList = new Element[m_elements.length + 1];
            System.arraycopy(m_elements, 0, newElementsList, 0, m_elements.length);
            newElementsList[m_elements.length] = elem;
            m_elements = newElementsList;
        } else { m_elements = new Element[] {elem}; }
    }

    /**
     * Remove an element to the list.
     * @return an element to remove
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

    /**
     * Parse the given dictionnary and create the instance managers.
     * @param dict : the given headers of the manifest file
     * @throws ParseException : if any error occurs
     */
    public void parse(Dictionary dict) throws ParseException {
        m_headers = dict;
        String componentClassesStr = (String) m_headers.get("iPOJO-Components");
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

    /**
     * Paser the given string.
     * @param s : the string to parse
     */
    private void parseElements(String s) {
        char[] string = s.toCharArray();

        for (int i = 0; i < string.length; i++) {
            char c = string[i];

            switch(c) {
            	// Beginning of an attribute.
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
                    i++; // skip "
                    c = string[i];
                    while (c != '"') {
                        attValue = attValue + c;
                        i++;
                        c = string[i];
                    }
                    i++; // skip "
                    c = string[i];

                    Attribute att = new Attribute(attName, attNs , attValue);
                    m_elements[m_elements.length - 1].addAttribute(att);
                    break;

                // End of an element    
                case '}' : 
                    Element lastElement = removeLastElement();
                    if (m_elements.length != 0) {
                        Element newQueue = m_elements[m_elements.length - 1];
                        newQueue.addElement(lastElement);
                    } else {
                        addElement(lastElement);
                    }
                    break;

                // Space
                case ' ' : 
                	break; // do nothing;
                
                // Default case
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
                		} else {
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
