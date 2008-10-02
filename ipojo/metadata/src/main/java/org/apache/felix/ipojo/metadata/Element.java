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
package org.apache.felix.ipojo.metadata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An element represents an XML Element.
 * It contains a name, a namepace, {@link Attribute} objects
 * and sub-elements. This class is used to parse iPOJO metadata.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Element {

    /**
     * The name of the element.
     */
    private String m_name;

    /**
     * The namespace of the element.
     */
    private String m_nameSpace;

    /**
     * The map of attributes of the element (attribute name -> {@link Attribute}).
     * The map key is the qualified name of the attribute (<code>ns:name</code>)
     * The value is the attribute object.
     */
    private Map m_attributes = new HashMap();

    /**
     * The map of the sub-element of the element (element name -> {@link Element}.
     * The map key is the element qualified name (ns:name).
     * The value is the array of element of this name.
     */
    private Map m_elements = new HashMap();

    /**
     * Creates an Element.
     * @param name the name of the element
     * @param ns the namespace of the element
     */
    public Element(String name, String ns) {
        m_name = name.toLowerCase();
        if (ns != null && ns.length() > 0) {
            m_nameSpace = ns.toLowerCase();
        }
    }

    /**
     * Gets sub-elements.
     * If no sub-elements, an empty array is returned.
     * @return the sub elements
     */
    public Element[] getElements() {
        Collection col = m_elements.values();
        Iterator it = col.iterator();
        List list = new ArrayList();
        while (it.hasNext()) {
            Element[] v = (Element[]) it.next();
            for (int i = 0; i < v.length; i++) {
                list.add(v[i]);
            }
        }
        return (Element[]) list.toArray(new Element[list.size()]);
    }

    /**
     * Gets element attributes.
     * If no attributes, an empty array is returned.
     * @return the attributes
     */
    public Attribute[] getAttributes() {
        return (Attribute[]) m_attributes.values().toArray(new Attribute[0]);
    }

    /**
     * Gets element name.
     * @return the name of the element
     */
    public String getName() {
        return m_name;
    }

    /**
     * Gets element namespace.
     * @return the namespace of the element
     */
    public String getNameSpace() {
        return m_nameSpace;
    }

    /**
     * Returns the value of the attribute given in parameter.
     * @param name the name of the searched attribute
     * @return the value of the attribute given in parameter,
     * <code>null</code> if the attribute does not exist
     */
    public String getAttribute(String name) {
        name = name.toLowerCase();
        Attribute att = (Attribute) m_attributes.get(name);
        if (att == null) {
            return null;
        } else {
            return att.getValue();
        }
    }

    /**
     * Returns the value of the attribute "name" of the namespace "ns".
     * @param name the name of the attribute to find
     * @param ns the namespace of the attribute to find
     * @return the String value of the attribute, or 
     * <code>null</code> if the attribute is not found.
     */
    public String getAttribute(String name, String ns) {
        name = ns.toLowerCase() + ":" + name.toLowerCase();
        return getAttribute(name);
    }
    
    /**
     * Gets the qualified name of the current element.
     * @return the qualified name of the current element.
     */
    private String getQualifiedName() {
        if (m_nameSpace == null) {
            return m_name;
        } else {
            return m_nameSpace + ":" + m_name;
        }
    }

    /**
     * Adds a sub-element.
     * @param elem the element to add
     */
    public void addElement(Element elem) {
        Element[] array = (Element[]) m_elements.get(elem.getQualifiedName());
        if (array == null) {
            m_elements.put(elem.getQualifiedName(), new Element[] {elem});
        } else {
            Element[] newElementsList = new Element[array.length + 1];
            System.arraycopy(array, 0, newElementsList, 0, array.length);
            newElementsList[array.length] = elem;
            m_elements.put(elem.getQualifiedName(), newElementsList);
        }
    }

    /**
     * Removes a sub-element.
     * @param elem the element to remove
     */
    public void removeElement(Element elem) {
        Element[] array = (Element[]) m_elements.get(elem.getQualifiedName());
        if (array == null) {
            return;
        } else {
            int idx = -1;
            for (int i = 0; i < array.length; i++) {
                if (array[i] == elem) {
                    idx = i;
                    break;
                }
            }

            if (idx >= 0) {
                if ((array.length - 1) == 0) {
                    m_elements.remove(elem.getQualifiedName());
                } else {
                    Element[] newElementsList = new Element[array.length - 1];
                    System.arraycopy(array, 0, newElementsList, 0, idx);
                    if (idx < newElementsList.length) {
                        System.arraycopy(array, idx + 1, newElementsList, idx, newElementsList.length - idx);
                    }
                    m_elements.put(elem.getQualifiedName(), newElementsList); // Update the stored list.
                }
            }
        }
    }

    /**
     * Adds a attribute.
     * @param att the attribute to add
     */
    public void addAttribute(Attribute att) {
        String name = att.getName().toLowerCase();
        if (att.getNameSpace() != null) {
            name = att.getNameSpace().toLowerCase() + ":" + name;
        }
        m_attributes.put(name, att);
    }

    /**
     * Removes an attribute.
     * @param att the attribute to remove
     */
    public void removeAttribute(Attribute att) {
        String name = att.getName();
        if (att.getNameSpace() != null) {
            name = att.getNameSpace() + ":" + name;
        }
        m_attributes.remove(name);
    }

    /**
     * Gets the elements array of the element type given in parameter. 
     * This method look for an empty namespace.
     * @param name the type of the element to find (element name)
     * @return the resulting element array (<code>null</code> if the search failed)
     */
    public Element[] getElements(String name) {
        Element[] elems = (Element[]) m_elements.get(name.toLowerCase());
        return elems;
    }

    /**
     * Gets the elements array of the element type given in parameter.
     * @param name the type of the element to find (element name)
     * @param ns the namespace of the element
     * @return the resulting element array (<code>null</code> if the search failed)
     */
    public Element[] getElements(String name, String ns) {
        if (ns == null || ns.length() == 0) {
            return getElements(name);
        }
        name = ns + ":" + name;
        return getElements(name);
    }

    /**
     * Does the element contain a sub-element of the type given in parameter.
     * @param name the type of the element to check.
     * @return <code>true</code> if the element contains an element of the type "name"
     */
    public boolean containsElement(String name) {
        return m_elements.containsKey(name.toLowerCase());
    }

    /**
     * Does the element contain a sub-element of the type given in parameter. 
     * @param name the type of the element to check.
     * @param ns the namespace of the element to check.
     * @return <code>true</code> if the element contains an element of the type "name"
     */
    public boolean containsElement(String name, String ns) {
        if (ns != null && ns.length() != 0) {
            name = ns + ":" + name;
        }
        return containsElement(name);
    }

    /**
     * Does the element contain an attribute of the name given in parameter.
     * @param name the name of the element
     * @return <code>true</code> if the element contains an attribute of the type "name"
     */
    public boolean containsAttribute(String name) {
        return m_attributes.containsKey(name.toLowerCase());
    }
    
    /**
     * Gets the XML form of this element.
     * @return the XML snippet representing this element.
     */
    public String toXMLString() {
        return toXMLString(0);
    }

    /**
     * Internal method to get XML form of an element.
     * @param indent the indentation to used.
     * @return the XML snippet representing this element.
     */
    private String toXMLString(int indent) {
        String xml = "";

        String tabs = "";
        for (int j = 0; j < indent; j++) {
            tabs += "\t";
        }

        if (m_nameSpace == null) {
            xml = tabs + "<" + m_name;
        } else {
            xml = tabs + "<" + m_nameSpace + ":" + m_name;
        }

        Set keys = m_attributes.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            Attribute current = (Attribute) m_attributes.get(it.next());
            if (current.getNameSpace() == null) {
                xml += " " + current.getName() + "=\"" + current.getValue() + "\"";
            } else {
                xml += " " + current.getNameSpace() + ":" + current.getName() + "=\"" + current.getValue() + "\"";
            }
        }

        if (m_elements.size() == 0) {
            xml += "/>";
            return xml;
        } else {
            xml += ">";
            keys = m_elements.keySet();
            it = keys.iterator();
            while (it.hasNext()) {
                Element[] e = (Element[]) m_elements.get(it.next());
                for (int i = 0; i < e.length; i++) {
                    xml += "\n";
                    xml += e[i].toXMLString(indent + 1);
                }
            }
            xml += "\n" + tabs + "</" + m_name + ">";
            return xml;
        }
    }

    /**
     * To String method.
     * @return the String form of this element.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return toString(0);
    }

    /**
     * Internal method to compute the toString method.
     * @param indent the indentation to use.
     * @return the String form of this element.
     */
    private String toString(int indent) {
        String xml = "";

        String tabs = "";
        for (int j = 0; j < indent; j++) {
            tabs += "\t";
        }

        if (m_nameSpace == null) {
            xml = tabs + m_name;
        } else {
            xml = tabs + m_nameSpace + ":" + m_name;
        }

        Set keys = m_attributes.keySet();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            Attribute current = (Attribute) m_attributes.get(it.next());
            if (current.getNameSpace() == null) {
                xml += " " + current.getName() + "=\"" + current.getValue() + "\"";
            } else {
                xml += " " + current.getNameSpace() + ":" + current.getName() + "=\"" + current.getValue() + "\"";
            }
        }

        if (m_elements.size() == 0) {
            return xml;
        } else {
            keys = m_elements.keySet();
            it = keys.iterator();
            while (it.hasNext()) {
                Element[] e = (Element[]) m_elements.get(it.next());
                for (int i = 0; i < e.length; i++) {
                    xml += "\n";
                    xml += e[i].toString(indent + 1);
                }
            }
            return xml;
        }
    }

}
