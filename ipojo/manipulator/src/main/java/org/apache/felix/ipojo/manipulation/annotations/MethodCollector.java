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
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * This class collects method annotations, and give them to the metadata collector. 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodCollector extends EmptyVisitor {

    /**
     * Parent collector.
     */
    private MetadataCollector m_collector;
    
    /**
     * Method name. 
     */
    private String m_name;

    /**
     * Constructor.
     * @param name : name of the method.
     * @param collector : parent collector.
     */
    public MethodCollector(String name, MetadataCollector collector) {
        m_collector = collector;
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
        
        if (CustomAnnotationVisitor.isCustomAnnotation(arg0)) {
            Element elem = CustomAnnotationVisitor.buildElement(arg0);
            elem.addAttribute(new Attribute("method", m_name));
            return new CustomAnnotationVisitor(elem, m_collector, true);
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
        m_collector.getElements().put(cb, null);
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
        m_collector.getElements().put(cb, null);
        return null;
    }

    /**
     * Process @serviceProperty annotation.
     * @return the visitor parsing the visited annotation.
     */
    private AnnotationVisitor processServiceProperty() {
        if (! m_collector.getIds().containsKey("provides")) {
            System.err.println("The component does not provide services, skipping ServiceProperty for " + m_name);
            return null;
        } else {
            Element provides = (Element) m_collector.getIds().get("provides");
            return new PropertyAnnotationParser(provides, m_name);
        }
    }

    /**
     * Process @property annotation.
     * @return the visitor parsing the visited annotation.
     */
    private AnnotationVisitor processProperty() {
        Element prop = null;
        if (! m_collector.getIds().containsKey("properties")) {
            prop = new Element("Properties", "");
            m_collector.getIds().put("properties", prop);
            m_collector.getElements().put(prop, null);
        } else {
            prop = (Element) m_collector.getIds().get("properties");
        }
        return new PropertyAnnotationParser(prop, m_name);
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
         * Binding policy.
         */
        private String m_policy;
        
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
            if (arg0.equals("policy")) {
                m_policy = arg1.toString();
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
            Element req = (Element) m_collector.getIds().get(m_id);
            if (req == null) {
                // Add the complete requires
                req = new Element("requires", "");
                if (m_specification != null) {
                    req.addAttribute(new Attribute("specification", m_specification));
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
            } else {
                String itf = req.getAttribute("specification");
                String aggregate = req.getAttribute("aggregate");
                String optional = req.getAttribute("optional");
                String filter = req.getAttribute("filter");
                String policy = req.getAttribute("policy");
                String comparator = req.getAttribute("comparator");
                String from = req.getAttribute("from");
                if (m_specification != null) {
                    if (itf == null) {
                        req.addAttribute(new Attribute("specification", m_specification));
                    } else if (! m_specification.equals(itf)) {
                        System.err.println("The required specification is not the same as previouly : " + m_specification + " & " + itf);
                        return;
                    }
                }
                
                if (m_optional != null) {
                    if (optional == null) {
                        req.addAttribute(new Attribute("optional", m_optional));
                    } else if (! m_optional.equals(optional)) {
                        System.err.println("The optional attribute is not always the same");
                        return;
                    }
                }
                
                if (m_aggregate != null) {
                    if (aggregate == null) {
                        req.addAttribute(new Attribute("aggregate", m_aggregate));
                    } else if (! m_aggregate.equals(aggregate)) {
                        System.err.println("The aggregate attribute is not always the same");
                        return;
                    }
                }
                
                if (m_filter != null) {
                    if (filter == null) {
                        req.addAttribute(new Attribute("filter", m_filter));
                    } else if (! m_filter.equals(filter)) {
                        System.err.println("The filter attribute is not always the same");
                        return;
                    }
                }
                
                if (m_policy != null) {
                    if (policy == null) {
                        req.addAttribute(new Attribute("policy", m_policy));
                    } else if (! m_policy.equals(policy)) {
                        System.err.println("The policy attribute is not always the same");
                        return;
                    }
                }
                
                if (m_comparator != null) {
                    if (comparator == null) {
                        req.addAttribute(new Attribute("comparator", m_comparator));
                    } else if (! m_comparator.equals(comparator)) {
                        System.err.println("The comparator attribute is not always the same");
                        return;
                    }
                }
                
                if (m_from != null) {
                    if (from == null) {
                        req.addAttribute(new Attribute("from", m_from));
                    } else if (! m_from.equals(from)) {
                        System.err.println("The from attribute is not always the same");
                        return;
                    }
                    
                }
                
            }
            Element method = new Element("callback", "");
            method.addAttribute(new Attribute("method", m_name));
            method.addAttribute(new Attribute("type", m_type));
            req.addElement(method);
            m_collector.getIds().put(m_id, req);
            m_collector.getElements().put(req, null);
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
            for (int i = 0; props != null && prop == null && i < props.length; i++) {
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

            prop.addAttribute(new Attribute("method", m_method));
            if (m_value != null) {
                prop.addAttribute(new Attribute("value", m_value));
            }

        }
    }
}
