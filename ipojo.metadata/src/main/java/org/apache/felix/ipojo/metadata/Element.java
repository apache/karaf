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

/**
 * Element.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Element {

	/**
	 * Name of the element.
	 */
	private String m_name;

    /**
     * Name space of the element.
     */
    private String m_nameSpace;

	/**
	 * List of the attributes of the element.
	 */
	private Attribute[] m_attributes = new Attribute[0];

	/**
	 * List of the subelement of the element.
	 */
	private Element[] m_elements = new Element[0];

	/**
     * Constructor.
	 * @param name : the name of the element
     * @pram ns : the namespace of the element
	 */
	public Element(String name, String ns) {
        m_name = name.toLowerCase();
        m_nameSpace = ns;
    }

	/**
	 * @return the sub elements
	 */
	public Element[] getElements() { return m_elements; }

	/**
	 * @return the attributes
	 */
	public Attribute[] getAttributes() { return m_attributes; }

	/**
	 * @return the name of the element
	 */
	public String getName() { return m_name; }

    /**
     * @return the namespace of the element
     */
    public String getNameSpace() { return m_nameSpace; }

	/**
     * Return the value of the attribute given in parameter.
	 * @param name : the name of the searched attribute
	 * @return the value of the attrbitue given in parameter, null if the attribute does not exist
	 */
	public String getAttribute(String name) {
		name = name.toLowerCase();
		for (int i = 0; i < m_attributes.length; i++) {
			if (m_attributes[i].getName().equals(name)) {
				return m_attributes[i].getValue();
			}
		}
		System.err.println("[Error in Metadata] The attribute " + name + " does not exist in " + m_name + " [" + m_nameSpace + "]");
		return null;
	}

	/**
	 * Return the value of the attrbitue "name" of the namespace "ns".
	 * @param name : name of the attribute to find
	 * @param ns : namespace of the attribute to find
	 * @return the String value of the attribute, or null if the attribute is not found.
	 */
	public String getAttribute(String name, String ns) {
		name = name.toLowerCase();
		for (int i = 0; i < m_attributes.length; i++) {
			if (m_attributes[i].getName().equals(name) && m_attributes[i].getNameSpace().equals(ns)) {
				return m_attributes[i].getValue();
			}
		}
		System.err.println("[Error in Metadata] The attribute " + name + "[" + ns + "] does not exist in " + m_name + " [" + m_nameSpace + "]");
		return null;
	}

	/**
     * Add a subelement.
	 * @param elem : the element to add
	 */
	public void addElement(Element elem) {
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

	private static Element[] addElement(Element[] list, Element elem) {
        if (list != null) {
            Element[] newElementsList = new Element[list.length + 1];
            System.arraycopy(list, 0, newElementsList, 0, list.length);
            newElementsList[list.length] = elem;
            return newElementsList;
        }
        else { return new Element[] {elem}; }
	}

	/**
     * Remove a subelement.
	 * @param elem : the element to remove
	 */
	public void removeElement(Element elem) {
        int idx = -1;
        for (int i = 0; i < m_elements.length; i++) {
            if (m_elements[i] == elem) { idx = i; break; }
        }

        if (idx >= 0) {
            if ((m_elements.length - 1) == 0) { m_elements = new Element[0]; }
            else {
                Element[] newElementsList = new Element[m_elements.length - 1];
                System.arraycopy(m_elements, 0, newElementsList, 0, idx);
                if (idx < newElementsList.length) {
                    System.arraycopy(m_elements, idx + 1, newElementsList, idx, newElementsList.length - idx); }
                m_elements = newElementsList;
            }
        }
	}

	/**
     * Add a attribute.
	 * @param att : the attribute to add
	 */
	public void addAttribute(Attribute att) {
        for (int i = 0; (m_attributes != null) && (i < m_attributes.length); i++) {
            if (m_attributes[i] == att) { return; }
        }

        if (m_attributes != null) {
            Attribute[] newAttributesList = new Attribute[m_attributes.length + 1];
            System.arraycopy(m_attributes, 0, newAttributesList, 0, m_attributes.length);
            newAttributesList[m_attributes.length] = att;
            m_attributes = newAttributesList;
        }
        else { m_attributes = new Attribute[] {att}; }
	}

	/**
     * Remove an attribute.
	 * @param att : the attribute to remove
	 */
	public void removeAttribute(Attribute att) {
        int idx = -1;
        for (int i = 0; i < m_attributes.length; i++) {
            if (m_attributes[i] == att) { idx = i; break; }
        }

        if (idx >= 0) {
            if ((m_attributes.length - 1) == 0) { m_attributes = new Attribute[0]; }
            else {
                Attribute[] newAttributesList = new Attribute[m_attributes.length - 1];
                System.arraycopy(m_attributes, 0, newAttributesList, 0, idx);
                if (idx < newAttributesList.length) {
                    System.arraycopy(m_attributes, idx + 1, newAttributesList, idx, newAttributesList.length - idx); }
                m_attributes = newAttributesList;
            }
        }
	}

	/**
     * Get the elements array of the element type given in parameter.
     * This method look for an empty namespace.
	 * @param name : the type of the element to find (element name)
	 * @return the resulting element array (empty if the search failed)
	 */
	public Element[] getElements(String name) {
		name = name.toLowerCase();
		Element[] list = new Element[0];
		for (int i = 0; i < m_elements.length; i++) {
			if (m_elements[i].getName().equals(name) && m_elements[i].getNameSpace().equals("")) {
				list = Element.addElement(list, m_elements[i]);
			}
		}
		return list;
	}

    /**
     * Get the elements array of the element type given in parameter.
     * @param name : the type of the element to find (element name)
     * @param ns : the namespace of the element
     * @return the resulting element array (empty if the search failed)
     */
    public Element[] getElements(String name, String ns) {
    	name = name.toLowerCase();
        Element[] list = new Element[0];
        for (int i = 0; i < m_elements.length; i++) {
            if (m_elements[i].getName().equals(name) && m_elements[i].getNameSpace().equals(ns)) {
                list = Element.addElement(list, m_elements[i]);
            }
        }
        return list;
    }

	/**
     * Is the element contains a subelement of the type given in parameter.
     * This method does not manage the namespace
	 * @param name : type of the element to check.
	 * @return true if the element contains an element of the type "name"
	 */
	public boolean containsElement(String name) {
		name = name.toLowerCase();
		for (int i = 0; i < m_elements.length; i++) {
			if (m_elements[i].getName().equals(name)) { return true; }
        }
		return false;
	}

    /**
     * Is the element contains a subelement of the type given in parameter.
     * This method does not manage the namespace
     * @param name : type of the element to check.
     * @return true if the element contains an element of the type "name"
     */
    public boolean containsElement(String name, String ns) {
    	name = name.toLowerCase();
    	ns = ns.toLowerCase();
        for (int i = 0; i < m_elements.length; i++) {
            if (m_elements[i].getName().equals(name) && m_elements[i].getNameSpace().equals(ns)) { return true; }
        }
        return false;
    }

	/**
     * Is the element contains an attribute of the name given in parameter.
	 * @param name : name of the element
	 * @return true if the element contains an attribute of the type "name"
	 */
	public boolean containsAttribute(String name) {
		name = name.toLowerCase();
		for (int i = 0; i < m_attributes.length; i++) {
			if (m_attributes[i].getName().equals(name)) { return true; }
        }
		return false;
	}

	/**
	 * @return the first-order namespaces list of the current element. First-order namespace are namespace of the element attribute and namespaces of its direct sub-element.
	 */
	public String[] getNamespaces() {
		String[] ns = new String[0];

		// Look for each direct sub-element
		for (int i = 0; i < m_elements.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < ns.length; j++) {
				if (ns[j].equals(m_elements[i].getNameSpace())) { found = true; }
			}
			if (!found) {
				String[] newNSList = new String[ns.length + 1];
				System.arraycopy(ns, 0, newNSList, 0, ns.length);
				newNSList[ns.length] = m_elements[i].getNameSpace();
				ns = newNSList;
			}
		}

		// Look for each attribute
		for (int i = 0; i < m_attributes.length; i++) {
			boolean found = false;
			for (int j = 0; !found && j < ns.length; j++) {
				if (ns[j].equals(m_attributes[i].getNameSpace())) { found = true; }
			}
			if (!found) {
				String[] newNSList = new String[ns.length + 1];
				System.arraycopy(ns, 0, newNSList, 0, ns.length);
				newNSList[ns.length] = m_attributes[i].getNameSpace();
				ns = newNSList;
			}
		}

		return ns;
	}

}
