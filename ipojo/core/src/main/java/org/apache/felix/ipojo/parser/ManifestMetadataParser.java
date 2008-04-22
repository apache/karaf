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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Manifest Metadata parser. Read a manifest file and construct metadata
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManifestMetadataParser {

    /**
     * Element list.
     */
    private Element[] m_elements = new Element[0];

    /**
     * Get the array of component type metadata.
     * @return the component metadata (composite & component).
     * @throws ParseException when a parsing error occurs
     */
    public Element[] getComponentsMetadata() throws ParseException {
        Element[] elems = m_elements[0].getElements();
        List list = new ArrayList();
        for (int i = 0; i < elems.length; i++) {
            if (!"instance".equals(elems[i].getName())) {
                list.add(elems[i]);
            }
        }
        return (Element[]) list.toArray(new Element[list.size()]);
    }

    /**
     * Get the array of instance configuration described in the metadata.
     * @return the instances list.
     * @throws ParseException : if the metadata cannot be parsed successfully
     */
    public Dictionary[] getInstances() throws ParseException {
        Element[] configs = m_elements[0].getElements("instance");
        if (configs == null) {
            return null;
        }
        Dictionary[] dicts = new Dictionary[configs.length];
        for (int i = 0; i < configs.length; i++) {
            dicts[i] = parseInstance(configs[i]);
        }
        return dicts;
    }

    /**
     * Parse an Element to get a dictionary.
     * 
     * @param instance : the Element describing an instance.
     * @return : the resulting dictionary
     * @throws ParseException : occurs when a configuration cannot be parse correctly.
     */
    private Dictionary parseInstance(Element instance) throws ParseException {
        Dictionary dict = new Properties();
        String name = instance.getAttribute("name");
        String comp = instance.getAttribute("component");
        if (name != null) {
            dict.put("name", instance.getAttribute("name"));
        }

        if (comp == null) {
            throw new ParseException("An instance does not have the 'component' attribute");
        }

        dict.put("component", comp);
        Element[] props = instance.getElements("property");

        for (int i = 0; props != null && i < props.length; i++) {
            parseProperty(props[i], dict);
        }

        return dict;
    }

    /**
     * Parse a property.
     * @param prop : the current element to parse
     * @param dict : the dictionary to populate
     * @throws ParseException : occurs if the proeprty cannot be parsed correctly
     */
    private void parseProperty(Element prop, Dictionary dict) throws ParseException {
        // Check that the property has a name
        String name = prop.getAttribute("name");
        String value = prop.getAttribute("value");
        if (name == null) {
            throw new ParseException("A property does not have the 'name' attribute");
        }
        //case : the property element has a 'value' attribute
        if (value == null) {
            // Recursive case
            // Check if there is 'property' element
            Element[] subProps = prop.getElements("property");
            if (subProps.length == 0) {
                throw new ParseException("A complex property must have at least one 'property' sub-element");
            }
            Dictionary dict2 = new Properties();
            for (int i = 0; i < subProps.length; i++) {
                parseProperty(subProps[i], dict2);
                dict.put(name, dict2);
            }
        } else {
            dict.put(prop.getAttribute("name"), prop.getAttribute("value"));
        }
    }

    /**
     * Add an element to the list.
     * 
     * @param elem : the element to add
     */
    private void addElement(Element elem) {
        if (m_elements == null) {
            m_elements = new Element[] { elem };
        } else {
            Element[] newElementsList = new Element[m_elements.length + 1];
            System.arraycopy(m_elements, 0, newElementsList, 0, m_elements.length);
            newElementsList[m_elements.length] = elem;
            m_elements = newElementsList;
        }
    }

    /**
     * Remove an element to the list.
     * 
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
     * Parse the given dictionary and create the instance managers.
     * 
     * @param dict : the given headers of the manifest file
     * @throws ParseException : if any error occurs
     */
    public void parse(Dictionary dict) throws ParseException {
        String componentClassesStr = (String) dict.get("iPOJO-Components");
        // Add the ipojo element inside the element list
        addElement(new Element("iPOJO", ""));
        parseElements(componentClassesStr.trim());
    }

    /**
     * Parse the given header and create the instance managers.
     * 
     * @param header : the given header of the manifest file
     * @throws ParseException : if any error occurs
     */
    public void parseHeader(String header) throws ParseException {
        // Add the ipojo element inside the element list
        addElement(new Element("iPOJO", ""));
        parseElements(header.trim());
    }

    /**
     * Parse the metadata from the string given in argument.
     * 
     * @param metadata : the metadata to parse
     * @return Element : the root element resulting of the parsing
     * @throws ParseException : if any error occurs
     */
    public static Element parse(String metadata) throws ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.parseElements(metadata);
        if (parser.m_elements.length != 1) {
            throw new ParseException("Error in parsing, root element not found : " + metadata);
        }
        return parser.m_elements[0];
    }
    
    /**
     * Parse the metadata from the given header string.
     * @param header : the header to parse
     * @return Element : the root element resulting of the parsing
     * @throws ParseException : if any error occurs
     */
    public static Element parseHeaderMetadata(String header) throws ParseException {
        ManifestMetadataParser parser = new ManifestMetadataParser();
        parser.addElement(new Element("iPOJO", ""));
        parser.parseElements(header);
        if (parser.m_elements.length != 1) {
            throw new ParseException("Error in parsing, root element not found : " + header);
        }
        return parser.m_elements[0];
    }

    /**
     * Parse the given string.
     * 
     * @param elems : the string to parse
     */
    private void parseElements(String elems) {
        char[] string = elems.toCharArray();

        for (int i = 0; i < string.length; i++) {
            char current = string[i];

            switch (current) { //NOPMD
                // Beginning of an attribute.
                case '$':
                    StringBuffer attName = new StringBuffer();
                    StringBuffer attValue = new StringBuffer();
                    StringBuffer attNs = null;
                    i++;
                    current = string[i]; // Increment and get the new current char.
                    while (current != '=') {
                        if (current == ':') {
                            attNs = attName;
                            attName = new StringBuffer();
                        } else {
                            attName.append(current);
                        }
                        i++;
                        current = string[i];
                    }
                    i = i + 2; // skip ="
                    current = string[i];
                    while (current != '"') {
                        attValue.append(current);
                        i++;
                        current = string[i]; // Increment and get the new current char.
                    }
                    i++; // skip "
                    current = string[i];

                    Attribute att = null;
                    if (attNs == null) {
                        att = new Attribute(attName.toString(), attValue.toString());
                    } else {
                        att = new Attribute(attName.toString(), attNs.toString(), attValue.toString());
                    }
                    m_elements[m_elements.length - 1].addAttribute(att);
                    break;

                // End of an element
                case '}':
                    Element lastElement = removeLastElement();
                    if (m_elements.length == 0) {
                        addElement(lastElement);
                    } else {
                        Element newQueue = m_elements[m_elements.length - 1];
                        newQueue.addElement(lastElement);
                    }
                    break;

                // Space
                case ' ':
                    break; // do nothing;

                // Default case
                default:
                    StringBuffer name = new StringBuffer();
                    StringBuffer namespace = null;
                    current = string[i];
                    while (current != ' ') {
                        if (current == ':') {
                            namespace = name;
                            name = new StringBuffer();
                            i++;
                            current = string[i];
                        } else {
                            name.append(current);
                            i++;
                            current = string[i]; // Increment and get the new current char.
                        }
                    }
                    // Skip spaces
                    while (string[i] == ' ') {
                        i = i + 1;
                    }
                    i = i + 1; // skip {
                    
                    Element elem = null;
                    if (namespace == null) {
                        elem = new Element(name.toString(), null);
                    } else {
                        elem = new Element(name.toString(), namespace.toString());
                    }
                    addElement(elem);
                    
                    break;
            }
        }
    }

}
