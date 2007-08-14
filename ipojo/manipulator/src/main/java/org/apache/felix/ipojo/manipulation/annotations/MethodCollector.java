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
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * This class collects method annotations, and give them to the metadata collector. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodCollector extends EmptyVisitor {

    /**
     * Parent element (component).
     */
    private Element m_element;
    
    /**
     * Method name. 
     */
    private String m_name;

    /**
     * Constructor.
     * @param name : name of the method.
     * @param element : parent element.
     */
    public MethodCollector(String name, Element element) {
        m_element = element;
        m_name = name;
    }

    /**
     * Visit method annotations.
     * @param arg0 : annotation name.
     * @param arg1 : is the annotation visible at runtime.
     * @return the visitor paring the visited annotation.
     * @see org.objectweb.asm.commons.EmptyVisitor#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String arg0, boolean arg1) {
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Property;")) {
            return processProperty();
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/ServiceProperty;")) {
            return processServiceProperty();
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Validate;")) {
            return processValidate();
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Invalidate;")) {
            return processInvalidate();
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Bind;")) {
            return processBind("bind");
        }
        if (arg0.equals("Lorg/apache/felix/ipojo/annotations/Unbind;")) {
            return processBind("unbind");
        }
        return null;
    }

    /**
     * Process @bind & @unbind.
     * @param type : bind or unbind
     * @return the visitor parsing @bind & @unbind annotations.
     */
    private AnnotationVisitor processBind(String type) {
        return new BindAnnotationParser(m_name, type);
    }

    /**
     * Process @validate annotation.
     * @return null.
     */
    private AnnotationVisitor processValidate() {
        Element cb = new Element("callback", "");
        cb.addAttribute(new org.apache.felix.ipojo.metadata.Attribute("transition", "validate"));
        cb.addAttribute(new org.apache.felix.ipojo.metadata.Attribute("method", m_name));
        m_element.addElement(cb);
        return null;
    }

    /**
     * Process @invalidate annotation.
     * @return null.
     */
    private AnnotationVisitor processInvalidate() {
        Element cb = new Element("callback", "");
        cb.addAttribute(new org.apache.felix.ipojo.metadata.Attribute("transition", "invalidate"));
        cb.addAttribute(new org.apache.felix.ipojo.metadata.Attribute("method", m_name));
        m_element.addElement(cb);
        return null;
    }

    /**
     * Process @serviceProperty annotation.
     * @return the visitor parsing the visited annotation.
     */
    private AnnotationVisitor processServiceProperty() {
        if (m_element.getElements("Provides", "").length == 0) {
            System.err.println("the component does not provide services, skip ServiceProperty for " + m_name);
            return null;
        }
        Element provides = m_element.getElements("Provides", "")[0];
        return new PropertyAnnotationParser(provides, m_name);
    }

    /**
     * Process @property annotation.
     * @return the visitor parsing the visited annotation.
     */
    private AnnotationVisitor processProperty() {
        if (m_element.getElements("Properties", "").length == 0) {
            m_element.addElement(new Element("Properties", ""));
        }
        Element props = m_element.getElements("Properties", "")[0];
        return new PropertyAnnotationParser(props, m_name);
    }

    /**
     * Parse @bind & @unbind annotations.
     */
    private final class BindAnnotationParser extends EmptyVisitor implements AnnotationVisitor {

        /**
         * Method name.
         */
        private String m_name;

        /**
         * Requirement filter.
         */
        private String m_filter;

        /**
         * Is the requirement optional?
         */
        private String m_optional;

        /**
         * Is the requirement aggregate?
         */
        private String m_aggregate;

        /**
         * Required specification. 
         */
        private String m_specification;

        /**
         * Requirement id.
         */
        private String m_id;

        /**
         * Bind or Unbind method?
         */
        private String m_type;

        /**
         * Constructor.
         * @param bind : method name.
         * @param type : is the callback a bind or an unbind method.
         */
        private BindAnnotationParser(String bind, String type) {
            m_name = bind;
            m_type = type;
        }

        /**
         * Visit annotation attribute.
         * @param arg0 : annotation name
         * @param arg1 : annotation value
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
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
            if (arg0.equals("aggregate")) {
                m_aggregate = arg1.toString();
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
        }

        /**
         * End of the visit.
         * Create or append the requirement info to a created or already existing "requires" element.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() {
            if (m_id == null) {
                if (m_name.startsWith("bind")) {
                    m_id = m_name.substring("bind".length());
                } else if (m_name.startsWith("unbind")) {
                    m_id = m_name.substring("unbind".length());
                } else {
                    System.err.println("Cannot determine the id of the bind method : " + m_name);
                    return;
                }
            }
            // Check if it is a full-determined requirement
            Element req = null;
            Element[] reqs = m_element.getElements("requires");
            for (int i = 0; i < reqs.length; i++) {
                if (reqs[i].containsAttribute("id") && reqs[i].getAttribute("id").equals(m_id)) {
                    req = reqs[i];
                    break;
                }
                if (reqs[i].containsAttribute("field") && reqs[i].getAttribute("field").equals(m_id)) {
                    req = reqs[i];
                    break;
                }
            }
            if (req == null) {
                // Add the complete requires
                req = new Element("requires", "");
                if (m_specification != null) {
                    req.addAttribute(new Attribute("interface", m_specification));
                }
                if (m_aggregate != null) {
                    req.addAttribute(new Attribute("aggregate", m_aggregate));
                }
                if (m_filter != null) {
                    req.addAttribute(new Attribute("filter", m_filter));
                }
                if (m_optional != null) {
                    req.addAttribute(new Attribute("optional", m_optional));
                }
                if (m_id != null) {
                    req.addAttribute(new Attribute("id", m_id));
                }
            }
            Element method = new Element("callback", "");
            method.addAttribute(new Attribute("method", m_name));
            method.addAttribute(new Attribute("type", m_type));
            req.addElement(method);
            m_element.addElement(req);
            return;
        }
    }

    private final class PropertyAnnotationParser extends EmptyVisitor implements AnnotationVisitor {

        /**
         * Parent element.
         */
        private Element m_parent;
        
        /**
         * Attached method.
         */
        private String m_method;
        
        /**
         * Property name. 
         */
        private String m_name;
        
        /**
         * Property value. 
         */
        private String m_value;

        /**
         * Constructor.
         * @param parent : parent element.
         * @param method : attached method.
         */
        private PropertyAnnotationParser(Element parent, String method) {
            m_parent = parent;
            m_method = method;
        }

        /**
         * Visit annotation attributes.
         * @param arg0 : annotation name
         * @param arg1 : annotation value
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
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
        }

        /**
         * End of the visit.
         * Append the computed element to the parent element.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() {
            if (m_name == null && m_method.startsWith("set")) {
                m_name = m_method.substring("set".length());
            }
            Element[] props = m_parent.getElements("Property");
            Element prop = null;
            for (int i = 0; prop == null && i < props.length; i++) {
                if (props[i].containsAttribute("name") && props[i].getAttribute("name").equals(m_name)) {
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

            prop.addAttribute(new Attribute("method", m_method));
            if (m_value != null) {
                prop.addAttribute(new Attribute("value", m_value));
            }

        }
    }
}
