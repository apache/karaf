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
package org.apache.felix.ipojo.api.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.ipojo.api.HandlerConfiguration;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Allows defining a contained instance. A contained instance is like
 * an instance declaration but the instance is created in the composite.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Instance implements HandlerConfiguration {
    
    /**
     * Targeted component type.
     */
    private String m_type;
    
    /**
     * Configuration.
     */
    private List m_conf = new ArrayList();

    /**
     * Creates a Instance.
     * @param type the targeted type.
     */
    public Instance(String type) {
        m_type = type;
    }

    /**
     * Adds the string property.
     * @param name property name
     * @param value property value
     * @return the current instance
     */
    public Instance addProperty(String name, String value) {
        Element elem = new Element("property", "");
        m_conf.add(elem);
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("value", value));
        return this;
    }

    /**
     * Adds a list property.
     * @param name property name
     * @param values the list
     * @return the current instance
     */
    public Instance addProperty(String name, List values) {
        Element elem = new Element("property", "");
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("type", "list"));

        m_conf.add(elem);

        for (int i = 0; i < values.size(); i++) {
            Object obj = values.get(i);
            Element e = new Element("property", "");
            elem.addElement(e);
            if (obj instanceof String) {
                e.addAttribute(new Attribute("value", obj.toString()));
            } else {
                // TODO
                throw new UnsupportedOperationException(
                        "Complex properties are not supported yet");
            }
        }

        return this;
    }

    /**
     * Adds an array property.
     * @param name the property name
     * @param values the array
     * @return the current instance
     */
    public Instance addProperty(String name, String[] values) {
        Element elem = new Element("property", "");
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("type", "array"));

        m_conf.add(elem);

        for (int i = 0; i < values.length; i++) {
            Object obj = values[i];
            Element e = new Element("property", "");
            elem.addElement(e);
            e.addAttribute(new Attribute("value", obj.toString()));
        }

        return this;
    }

    /**
     * Adds a vector property.
     * @param name the property name
     * @param values the vector
     * @return the current instance
     */
    public Instance addProperty(String name, Vector values) {
        Element elem = new Element("property", "");
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("type", "vector"));

        m_conf.add(elem);

        for (int i = 0; i < values.size(); i++) {
            Object obj = values.get(i);
            Element e = new Element("property", "");
            elem.addElement(e);
            if (obj instanceof String) {
                e.addAttribute(new Attribute("value", obj.toString()));
            } else {
                // TODO
                throw new UnsupportedOperationException(
                        "Complex properties are not supported yet");
            }
        }

        return this;
    }

    /**
     * Adds a map property.
     * @param name the property name
     * @param values the map
     * @return the current instance
     */
    public Instance addProperty(String name, Map values) {
        Element elem = new Element("property", "");
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("type", "map"));

        m_conf.add(elem);
        Set entries = values.entrySet();
        Iterator it = entries.iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Entry) it.next();
            Element e = new Element("property", "");
            elem.addElement(e);

            String n = (String) entry.getKey();
            Object v = entry.getValue();
            if (v instanceof String) {
                e.addAttribute(new Attribute("name", n));
                e.addAttribute(new Attribute("value", v.toString()));
            } else {
                // TODO
                throw new UnsupportedOperationException(
                        "Complex properties are not supported yet");
            }
        }

        return this;
    }

    /**
     * Adds a dictionary property.
     * @param name the property name
     * @param values the dictionary
     * @return the current instance
     */
    public Instance addProperty(String name, Dictionary values) {
        Element elem = new Element("property", "");
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("type", "dictionary"));

        m_conf.add(elem);
        Enumeration e = values.keys();
        while (e.hasMoreElements()) {
            Element el = new Element("property", "");
            elem.addElement(el);

            String n = (String) e.nextElement();
            Object v = values.get(n);
            if (v instanceof String) {
                el.addAttribute(new Attribute("name", n));
                el.addAttribute(new Attribute("value", v.toString()));
            } else {
                // TODO
                throw new UnsupportedOperationException(
                        "Complex properties are not supported yet");
            }
        }

        return this;
    }

    /**
     * Ensures the validity of the instance description.
     */
    private void ensureValidity() {
        if (m_type == null) {
            throw new IllegalStateException(
                    "Invalid containted instance configuration : the component type is not set");
        }
    }

    /**
     * Gets the instance description in the Element-Attribute form.
     * @return the root Element of the instance description 
     * @see org.apache.felix.ipojo.api.HandlerConfiguration#getElement()
     */
    public Element getElement() {
        ensureValidity();
        Element instance = new Element("instance", "");
        instance.addAttribute(new Attribute("component", m_type));
        for (int i = 0; i < m_conf.size(); i++) {
            Element elem = (Element) m_conf.get(i);
            instance.addElement(elem);
        }
        return instance;
    }
}
