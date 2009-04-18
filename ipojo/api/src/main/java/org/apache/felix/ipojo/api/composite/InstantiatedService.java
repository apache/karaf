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
import org.apache.felix.ipojo.util.DependencyModel;

/**
 * Allows defining an instantiated sub-service. An instantiated sub-
 * service will be reified by instances publishing the required service. 
 * Those instances are created from public factories
 * inside the composite.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstantiatedService implements HandlerConfiguration {

    /**
     * The required specification.
     */
    private String m_specification;

    /**
     * The LDAP filter of the dependency.
     */
    private String m_filter;

    /**
     * Is the dependency optional?
     */
    private boolean m_optional;

    /**
     * Is the dependency aggregate?
     */
    private boolean m_aggregate;

    /**
     * The dependency binding policy. Default: Dynamic policy.
     */
    private int m_policy = DependencyModel.DYNAMIC_BINDING_POLICY;

    /**
     * The dependency comparator. (used to compare service providers)
     */
    private String m_comparator;

    /**
     * Instance configuration List of Element (Property).
     */
    private List m_conf = new ArrayList();

    /**
     * Gets the dependency metadata.
     * @return the 'subservice' element describing the current 
    * instantiated service.
     */
    public Element getElement() {
        ensureValidity();

        Element dep = new Element("subservice", "");
        dep.addAttribute(new Attribute("action", "instantiate"));

        dep.addAttribute(new Attribute("specification", m_specification));

        if (m_filter != null) {
            dep.addAttribute(new Attribute("filter", m_filter));
        }
        if (m_comparator != null) {
            dep.addAttribute(new Attribute("comparator", m_comparator));
        }

        if (m_optional) {
            dep.addAttribute(new Attribute("optional", "true"));
        }
        if (m_aggregate) {
            dep.addAttribute(new Attribute("aggregate", "true"));
        }

        if (m_policy == DependencyModel.DYNAMIC_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "dynamic"));
        } else if (m_policy == DependencyModel.STATIC_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "static"));
        } else if (m_policy == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY) {
            dep.addAttribute(new Attribute("policy", "dynamic-priority"));
        }

        for (int i = 0; i < m_conf.size(); i++) {
            Element elem = (Element) m_conf.get(i);
            dep.addElement(elem);
        }

        return dep;
    }

    /**
     * Adds a string property.
     * @param name the property name
     * @param value the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, String value) {
        Element elem = new Element("property", "");
        m_conf.add(elem);
        elem.addAttribute(new Attribute("name", name));
        elem.addAttribute(new Attribute("value", value));
        return this;
    }

    /**
     * Adds a list property.
     * @param name the property name
     * @param values the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, List values) {
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
     * @param values the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, String[] values) {
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
     * @param values the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, Vector values) {
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
     * @param values the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, Map values) {
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
     * @param values the property value
     * @return the current instantiated sub-service
     */
    public InstantiatedService addProperty(String name, Dictionary values) {
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
     * Sets the required service specification.
     * @param spec the specification
     * @return the current instantiated sub-service
     */
    public InstantiatedService setSpecification(String spec) {
        m_specification = spec;
        return this;
    }

    /**
     * Sets the dependency filter.
     * @param filter the LDAP filter
     * @return the current instantiated sub-service
     */
    public InstantiatedService setFilter(String filter) {
        m_filter = filter;
        return this;
    }

    /**
     * Sets the dependency optionality.
     * @param opt <code>true</code> to set the dependency to optional.
     * @return the current instantiated sub-service
     */
    public InstantiatedService setOptional(boolean opt) {
        m_optional = opt;
        return this;
    }

    /**
     * Sets the dependency cardinality.
     * @param agg <code>true</code> to set the dependency to aggregate.
     * @return the current instantiated sub-service
     */
    public InstantiatedService setAggregate(boolean agg) {
        m_aggregate = agg;
        return this;
    }

    /**
     * Sets the dependency binding policy.
     * @param policy the binding policy
     * @return the current instantiated sub-service
     */
    public InstantiatedService setBindingPolicy(int policy) {
        m_policy = policy;
        return this;
    }

    /**
     * Sets the dependency comparator.
     * @param cmp the comparator class name
     * @return the current instantiated sub-service
     */
    public InstantiatedService setComparator(String cmp) {
        m_comparator = cmp;
        return this;
    }

    /**
     * Checks dependency configuration validity.
     */
    private void ensureValidity() {
        // Check specification
        if (m_specification == null) {
            throw new IllegalStateException(
                    "The specification of the instantiated service must be set");
        }

        // Check binding policy.
        if (!(m_policy == DependencyModel.DYNAMIC_BINDING_POLICY
                || m_policy == DependencyModel.STATIC_BINDING_POLICY || m_policy == DependencyModel.DYNAMIC_PRIORITY_BINDING_POLICY)) {
            throw new IllegalStateException("Unknown binding policy : "
                    + m_policy);
        }
    }

}
