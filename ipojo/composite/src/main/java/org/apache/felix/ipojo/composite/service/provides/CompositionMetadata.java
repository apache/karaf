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
package org.apache.felix.ipojo.composite.service.provides;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.manipulation.Manipulator;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Check and build a composition, i.e. a POJO containing the composition.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositionMetadata {

    /**
     * Implemented composition.
     */
    private SpecificationMetadata m_specification;

    /**
     * Name of the composition.
     */
    private String m_name;

    /**
     * Bundle Context.
     */
    private BundleContext m_context;

    /**
     * Manipulation Metadata.
     */
    private Element m_manipulation;

    /**
     * Reference on the handler.
     */
    private ProvidedServiceHandler m_handler;

    /**
     * List of Mappings.
     */
    private List m_mappings = new ArrayList();

    /**
     * Constructor.
     * @param context : bundle context
     * @param description : 'provides' element
     * @param psh : parent handler 
     * @param name : name of the composition.
     */
    public CompositionMetadata(BundleContext context, Element description, ProvidedServiceHandler psh, String name) {
        m_context = context;
        m_handler = psh;
        // Get the composition name
        m_name = description.getAttribute("specification") + name;

        // Get implemented service specification
        String spec = description.getAttribute("specification");
        m_specification = new SpecificationMetadata(spec, m_context, false, false, m_handler);

        Element[] mappings = description.getElements("delegation");
        for (int i = 0; mappings != null && i < mappings.length; i++) {
            String methodName = mappings[i].getAttribute("method");
            MethodMetadata method = m_specification.getMethodByName(methodName);
            if (method == null) {
                m_handler.error("The method " + methodName + " does not exist in the specicifation " + spec);
                return;
            }

            if (mappings[i].getAttribute("policy").equalsIgnoreCase("All")) {
                method.setAllPolicy();
            }
        }
    }

    protected BundleContext getBundleContext() {
        return m_context;
    }

    public String getName() {
        return m_name;
    }

    public SpecificationMetadata getSpecificationMetadata() {
        return m_specification;
    }

    /**
     * Build Available Mappings.
     * @throws CompositionException : a factory is not available, the composition cannot be checked.
     */
    private void buildAvailableMappingList() throws CompositionException {
        int index = 0;

        for (int i = 0; i < m_handler.getInstanceType().size(); i++) {
            String type = (String) m_handler.getInstanceType().get(i);
            try {
                ServiceReference[] refs = m_context.getServiceReferences(Factory.class.getName(), "(factory.name=" + type + ")");
                if (refs == null) {
                    m_handler.error("The factory " + type + " is not available, cannot check the composition");
                    throw new CompositionException("The factory " + type + " needs to be available to check the composition");
                } else {
                    String className = (String) refs[0].getProperty("component.class");
                    Class impl = m_context.getBundle().loadClass(className);
                    SpecificationMetadata spec = new SpecificationMetadata(impl, type, m_handler);
                    FieldMetadata field = new FieldMetadata(spec);
                    field.setName("_field" + index);
                    Mapping map = new Mapping(spec, field);
                    m_mappings.add(map);
                    index++;
                }
            } catch (InvalidSyntaxException e) {
                m_handler.error("A LDAP filter is not valid : " + e.getMessage());
            } catch (ClassNotFoundException e) {
                m_handler.error("The implementation class of a component cannot be loaded : " + e.getMessage());
            }
        }

        for (int i = 0; i < m_handler.getSpecifications().size(); i++) {
            SpecificationMetadata spec = (SpecificationMetadata) m_handler.getSpecifications().get(i);
            FieldMetadata field = new FieldMetadata(spec);
            field.setName("_field" + index);
            if (spec.isOptional()) {
                field.setOptional(true);
            }
            if (spec.isAggregate()) {
                field.setAggregate(true);
            }
            Mapping map = new Mapping(spec, field);
            m_mappings.add(map);
            index++;
        }
    }

    /**
     * Build the delegation mapping.
     * @throws CompositionException : occurs when the mapping cannot be inferred correctly
     */
    protected void buildMapping() throws CompositionException {
        buildAvailableMappingList();

        // Dependency closure is OK, now look for method delegation
        Map/* <MethodMetadata, Mapping> */svcMethods = new HashMap();
        Map/* <MethodMetadata, Mapping> */instMethods = new HashMap();

        for (int i = 0; i < m_mappings.size(); i++) {
            Mapping map = (Mapping) m_mappings.get(i);
            SpecificationMetadata spec = map.getSpecification();
            for (int j = 0; j < spec.getMethods().size(); j++) {
                MethodMetadata method = (MethodMetadata) spec.getMethods().get(j);
                if (spec.isInterface()) {
                    svcMethods.put(method, map);
                } else {
                    instMethods.put(method, map);
                }
            }
        }

        // For each needed method, search if available and store the mapping
        for (int j = 0; j < m_specification.getMethods().size(); j++) {
            MethodMetadata method = (MethodMetadata) m_specification.getMethods().get(j);
            Set keys = instMethods.keySet(); // Look first in methods contained in the glue code.
            Iterator iterator = keys.iterator();
            boolean found = false;
            while (iterator.hasNext() & !found) {
                MethodMetadata met = (MethodMetadata) iterator.next();
                if (met.equals(method)) {
                    found = true;
                    FieldMetadata field = ((Mapping) instMethods.get(met)).getField();
                    field.setUseful(true);
                    method.setDelegation(field);
                }
            }
            if (!found) { // If not found looks inside method contained in services.
                keys = svcMethods.keySet(); // Look first in methods contained in the glue code
                iterator = keys.iterator();
                while (!found && iterator.hasNext()) {
                    MethodMetadata met = (MethodMetadata) iterator.next();
                    if (met.equals(method)) {
                        found = true;
                        FieldMetadata field = ((Mapping) svcMethods.get(met)).getField();
                        field.setUseful(true);
                        method.setDelegation(field);
                        // Test optional
                        if (field.isOptional() && !method.throwsUnsupportedOperationException()) {
                            m_handler.warn("The method " + method.getMethod().getName() + " could not be provided correctly : the specification " + field.getSpecification().getName() + " is optional");
                        }
                    }
                }
            }
            if (!found) { throw new CompositionException("Inconsistent composition - the method " + method.getMethod() + " could not be delegated"); }
        }
    }

    /**
     * Build a service implementation.
     * @return the byte[] of the POJO.
     */
    protected byte[] buildPOJO() {
        Class clazz = null;
        try {
            clazz = getBundleContext().getBundle().loadClass(m_specification.getName());
        } catch (ClassNotFoundException e1) {
            // The class has already be loaded.
            return null;
        }
        byte[] pojo = POJOWriter.dump(clazz, m_name, getFieldList(), getMethodList());
        Manipulator manipulator = new Manipulator();
        try {
            byte[] newclazz = manipulator.manipulate(pojo);
            m_manipulation = manipulator.getManipulationMetadata();
            return newclazz;
        } catch (IOException e) {
            m_handler.error("An error occurs during the composite implementation creation : " + e.getMessage(), e);
        }
        return null;
    }

    /**
     * Build service implementation metadata.
     * @param name : name of the future instance (used to avoid cycle)
     * @return Component Type metadata. 
     */
    protected Element buildMetadata(String name) {
        Element elem = new Element("component", "");
        Attribute className = new Attribute("classname", m_name);
        Attribute factory = new Attribute("factory", "false");
        elem.addAttribute(className);
        elem.addAttribute(factory);

        // Add architecture for debug
        elem.addAttribute(new Attribute("architecture", "true"));

        // Provides
        Element provides = new Element("provides", "");
        provides.addAttribute(new Attribute("specification", m_specification.getName()));
        elem.addElement(provides);

        // Dependencies
        List fields = getFieldList();
        for (int i = 0; i < fields.size(); i++) {
            FieldMetadata field = (FieldMetadata) fields.get(i);
            if (field.isUseful() && field.getSpecification().isInterface()) {
                Element dep = new Element("requires", "");
                dep.addAttribute(new Attribute("field", field.getName()));
                dep.addAttribute(new Attribute("scope", "composite"));
                if (field.getSpecification().isOptional()) {
                    dep.addAttribute(new Attribute("optional", "true"));
                }
                dep.addAttribute(new Attribute("filter", "(!(instance.name=" + name + "))"));
                elem.addElement(dep);
            }
        }

        Element properties = new Element("properties", "");
        for (int i = 0; i < fields.size(); i++) {
            FieldMetadata field = (FieldMetadata) fields.get(i);
            if (field.isUseful() && !field.getSpecification().isInterface()) {
                Element prop = new Element("Property", "");
                prop.addAttribute(new Attribute("field", field.getName()));
                properties.addElement(prop);
            }
        }
        if (properties.getElements().length != 0) {
            elem.addElement(properties);
        }

        // Insert information to metadata
        elem.addElement(m_manipulation);

        return elem;
    }

    /**
     * Get the field list to use for the delegation.
     * @return the field list.
     */
    public List getFieldList() {
        List list = new ArrayList();
        for (int i = 0; i < m_mappings.size(); i++) {
            Mapping map = (Mapping) m_mappings.get(i);
            list.add(map.getField());
        }
        return list;
    }

    /**
     * Get the method list contained in the implemented specification.
     * @return the List of implemented method.
     */
    private List getMethodList() {
        return m_specification.getMethods();
    }

    /**
     * Store links between Field and pointed Specification.
     */
    private class Mapping {

        /**
         * Specification.
         */
        private SpecificationMetadata m_spec;

        /**
         * Field.
         */
        private FieldMetadata m_field;

        /**
         * Constructor.
         * @param spec : specification metadata.
         * @param field : the field.
         */
        public Mapping(SpecificationMetadata spec, FieldMetadata field) {
            m_spec = spec;
            m_field = field;
        }

        public SpecificationMetadata getSpecification() {
            return m_spec;
        }

        public FieldMetadata getField() {
            return m_field;
        }

    }

}
