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
package org.apache.felix.ipojo.manipulation.annotations;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Collect field annotations. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldCollector extends EmptyVisitor implements FieldVisitor {
    
    /**
     * Collected element.
     */
    private MetadataCollector m_collector;
    
    /**
     * Field name. 
     */
    private String m_field;
    
    /**
     * Constructor.
     * @param fieldName : field name
     * @param collector : metadata collector.
     */
    public FieldCollector(String fieldName, MetadataCollector collector) {
        m_collector = collector;
        m_field = fieldName;
    }

    /**
     * Visit annotations on the current field.
     * @param arg0 : annotation name
     * @param arg1 : is the annotation a runtime annotation.
     * @return the annotation visitor visiting the annotation
     * @see org.objectweb.asm.FieldVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Requires;")) {
            return new RequiresAnnotationParser(m_field);
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Controller;")) {
            Element elem = new Element("controller", "");
            elem.addAttribute(new Attribute("field", m_field));
            m_collector.getElements().put(elem, null);
            return null;
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/ServiceProperty;")) {
            if (! m_collector.getIds().containsKey("provides")) { // The provides annotation is already computed.
                System.err.println("The component does not provide services, skip ServiceProperty for " + m_field);
                return null;
            } else {
                // Get the provides element
                Element parent = (Element) m_collector.getIds().get("provides");
                return new PropertyAnnotationParser(m_field, parent);
            }
            
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Property;")) {
            Element parent = null;
            if (! m_collector.getIds().containsKey("properties")) {
                parent = new Element("Properties", "");
                m_collector.getIds().put("properties", parent);
                m_collector.getElements().put(parent, null);
            } else {
                parent = (Element) m_collector.getIds().get("properties");
            }
            return new PropertyAnnotationParser(m_field, parent);
        }
        
        if (CustomAnnotationVisitor.isCustomAnnotation(arg0)) {
            Element elem = CustomAnnotationVisitor.buildElement(arg0);
            elem.addAttribute(new Attribute("field", m_field)); // Add a field attribute
            return new CustomAnnotationVisitor(elem, m_collector, true, false);
        }
        
        return null;
       
    }
    
    /**
     * AnnotationVisitor parsing the @requires annotation.
     */
    private final class RequiresAnnotationParser extends EmptyVisitor implements AnnotationVisitor {
        
        /**
         * Dependency field.
         */
        private String m_field;
        
        /**
         * Dependency filter.
         */
        private String m_filter;
        
        /**
         * Is the dependency optional ?
         */
        private String m_optional;
        
        /**
         * Dependency specification.
         */
        private String m_specification;
        
        /**
         * Dependency id.
         */
        private String m_id;
        
        /**
         * Binding policy.
         */
        private String m_policy;
        
        /**
         * Default-Implementation attribute.
         */
        private String m_defaultImplementation;
        
        /**
         * Enable or Disable Nullable pattern. 
         */
        private String m_nullable;
        
        /**
         * Comparator.
         */
        private String m_comparator;
        
        /**
         * From attribute.
         */
        private String m_from;
        
        /**
         * Constructor.
         * @param name : field name.
         */
        private RequiresAnnotationParser(String name) {
            m_field = name;
        }

        /**
         * Visit one "simple" annotation.
         * @param arg0 : annotation name
         * @param arg1 : annotation value
         * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("filter")) {
                m_filter = arg1.toString();
                return;
            }
            if (arg0.equals("optional")) {
                m_optional = arg1.toString();
                return;
            }
            if (arg0.equals("nullable")) {
                m_nullable = arg1.toString();
                return;
            }
            if (arg0.equals("policy")) {
                m_policy = arg1.toString();
                return;
            }
            if (arg0.equals("defaultimplementation")) {
                Type type = Type.getType(arg1.toString());
                m_defaultImplementation = type.getClassName();
                return;
            }
            if (arg0.equals("specification")) {
                m_specification = arg1.toString();
                return;
            }
            if (arg0.equals("id")) {
                m_id = arg1.toString();
                return;
            }
            if (arg0.equals("comparator")) {
                Type type = Type.getType(arg1.toString());
                m_comparator = type.getClassName();
                return;
            }
            if (arg0.equals("from")) {
                m_from = arg1.toString();
                return;
            }
        }

        /**
         * End of the annotation.
         * Create a "requires" element
         * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
         */
        public void visitEnd() {
            Element req = null;
            if (m_id == null) {
                req = (Element) m_collector.getIds().get(m_field);
            } else {
                req = (Element) m_collector.getIds().get(m_id);
            }

            if (req == null) {
                req = new Element("requires", "");
            }

            req.addAttribute(new Attribute("field", m_field));
            if (m_specification != null) {
                req.addAttribute(new Attribute("specification", m_specification));
            }
            if (m_filter != null) {
                req.addAttribute(new Attribute("filter", m_filter));
            }
            if (m_optional != null) {
                req.addAttribute(new Attribute("optional", m_optional));
            }
            if (m_nullable != null) {
                req.addAttribute(new Attribute("nullable", m_nullable));
            }
            if (m_defaultImplementation != null) {
                req.addAttribute(new Attribute("default-implementation", m_defaultImplementation));
            }
            if (m_policy != null) {
                req.addAttribute(new Attribute("policy", m_policy));
            }
            if (m_id != null) {
                req.addAttribute(new Attribute("id", m_id));
            }
            if (m_comparator != null) {
                req.addAttribute(new Attribute("comparator", m_comparator));
            }
            if (m_from != null) {
                req.addAttribute(new Attribute("from", m_from));
            }
            
            if (m_id != null) { 
                m_collector.getIds().put(m_id, req);
            } else {
                m_collector.getIds().put(m_field, req);
            }
            
            m_collector.getElements().put(req, null);
                
            return;
        }
    }
    
    /**
     * Parses a Property annotation.
     */
    private static final class PropertyAnnotationParser extends EmptyVisitor implements AnnotationVisitor {
        
        /**
         * Parent element element.
         */
        private Element m_parent;
        
        /**
         * Field name. 
         */
        private String m_field;
        
        /**
         * Property name. 
         */
        private String m_name;
        
        /**
         * Property value.  
         */
        private String m_value;
        
        /**
         * Property mandatory aspect.
         */
        private String m_mandatory;
        
        
        /**
         * Constructor.
         * @param parent : parent element.
         * @param field : field name.
         */
        private PropertyAnnotationParser(String field, Element parent) {
            m_parent = parent;
            m_field = field;
        }

        /**
         * Visit one "simple" annotation.
         * @param arg0 : annotation name
         * @param arg1 : annotation value
         * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("name")) {
                m_name = arg1.toString();
                return;
            }
            if (arg0.equals("value")) {
                m_value = arg1.toString();
                return;
            }
            if (arg0.equals("mandatory")) {
                m_mandatory = arg1.toString();
                return;
            }
        }

        /**
         * End of the annotation.
         * Create a "property" element
         * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
         */
        public void visitEnd() {
            if (m_name == null) {
                m_name = m_field;
            }
            
            Element[] props = m_parent.getElements("Property");
            Element prop = null;
            for (int i = 0; prop == null && props != null && i < props.length; i++) {
                String name = props[i].getAttribute("name");
                if (name != null && name.equals(m_name)) {
                    prop = props[i];
                }
            }
            
            if (prop == null) {
                prop = new Element("property", "");
                m_parent.addElement(prop);
                if (m_name != null) { 
                    prop.addAttribute(new Attribute("name", m_name));
                }
            }
            
            prop.addAttribute(new Attribute("field", m_field));
            if (m_value != null) {
                prop.addAttribute(new Attribute("value", m_value));
            }
            if (m_mandatory != null) {
                prop.addAttribute(new Attribute("mandatory", m_mandatory));
            }
            
        }
    }
}
