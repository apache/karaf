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

import java.lang.reflect.Array;

import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Collect metadata from custom annotation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CustomAnnotationVisitor extends EmptyVisitor implements AnnotationVisitor {

    //TODO manage enum annotations.
    
    /**
     * Parent element.
     */
    private Element m_elem;

    /**
     * Id attribute (if found)
     * else use the annotation package name.
     */
    private String m_id;

    /**
     * Parent attribute (if found)
     * else use the annotation package name.
     */
    private String m_parent;

    /**
     * Is the custom annotation a first-order annotation.
     */
    private boolean m_root;
    
    /**
     * Is the visit annotation a class annotation?
     */
    private boolean m_classAnnotation;

    /**
     * Metadata collector.
     */
    private MetadataCollector m_collector;
    
    /**
     * Constructor.
     * @param elem the parent element
     * @param collector the metadata collector
     * @param root is the annotation a root
     * @param clazz the annotation is a class annotation.
     */
    public CustomAnnotationVisitor(Element elem, MetadataCollector collector, boolean root, boolean clazz) {
        m_elem = elem;
        m_root = root;
        m_collector = collector;
        m_classAnnotation = clazz;
    }
    
    /**
     * Check if the given annotation descriptor is an iPOJO custom annotation.
     * A valid iPOJO custom annotation must contains 'ipojo' or 'handler' in its qualified name.
     * @param desc : annotation descriptor
     * @return : true if the given descriptor is an iPOJO custom annotation
     */
    public static boolean isCustomAnnotation(String desc) {
        desc = desc.toLowerCase();
        if (desc.indexOf("ipojo") != -1 || desc.indexOf("handler") != -1) {
            return true;
        }
        return false;
    }
    
    /**
     * Build the element object from the given descriptor.
     * @param desc : annotation descriptor
     * @return : the element
     */
    public static Element buildElement(String desc) {
        String s = (desc.replace('/', '.')).substring(1, desc.length() - 1);
        int index = s.lastIndexOf('.');
        String name = s.substring(index + 1);
        String namespace = s.substring(0, index);
        return new Element(name, namespace);
    }


    /**
     * Visit a 'simple' annotation attribute.
     * This method is used for primitive arrays too. 
     * @param arg0 : attribute name
     * @param arg1 : attribute value
     * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
     */
    public void visit(String arg0, Object arg1) {
        if (arg1.getClass().isArray()) {
            // Primitive arrays case
            String v = null;
            int index = Array.getLength(arg1);
            for (int i = 0; i < index; i++) {
                if (v == null) {
                    v = "{" + Array.get(arg1, i);
                } else {
                    v += "," + Array.get(arg1, i);
                }
            }
            v += "}";
            m_elem.addAttribute(new Attribute(arg0, v));
            return;
        }
        // Attributes are added as normal attributes
        m_elem.addAttribute(new Attribute(arg0, arg1.toString()));
        if (m_root) {
            if (arg0.equals("id")) {
                m_id = arg1.toString();
            } else if (arg0.equals("parent")) {
                m_parent = arg1.toString();
            }
        }
    }

    /**
     * Visit a sub-annotation.
     * @param arg0 : attribute name.
     * @param arg1 : annotation description
     * @return an annotation visitor which will visit the given annotation
     * @see org.objectweb.asm.commons.EmptyVisitor#visitAnnotation(java.lang.String, java.lang.String)
     */
    public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
        // Sub annotations are mapped to sub-elements
        Element elem = buildElement(arg1);
        m_elem.addElement(elem);
        return new CustomAnnotationVisitor(elem, m_collector, false, false);
    }

    /**
     * Visit an array attribute.
     * @param arg0 : attribute name
     * @return a visitor which will visit each element of the array
     * @see org.objectweb.asm.commons.EmptyVisitor#visitArray(java.lang.String)
     */
    public AnnotationVisitor visitArray(String arg0) {
        return new SubArrayVisitor(m_elem, arg0);
    }
    
    /**
     * Visits an enumeration attribute.
     * @param arg0 the attribute name
     * @param arg1 the enumeration descriptor
     * @param arg2 the attribute value
     * @see org.objectweb.asm.AnnotationVisitor#visitEnum(java.lang.String, java.lang.String, java.lang.String)
     */
    public void visitEnum(String arg0, String arg1, String arg2) {
        m_elem.addAttribute(new Attribute(arg0, arg2));
    }

    /**
     * End of the visit.
     * All attribute was visited, we can update collectors data.
     * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
     */
    public void visitEnd() {
        if (m_root) {
            if (m_id != null) {
                m_collector.getIds().put(m_id, m_elem);
            } else {
                if (! m_collector.getIds().containsKey(m_elem.getNameSpace()) && m_classAnnotation) {
                    // If the namespace is not already used, add the annotation as the
                    // root element of this namespace.
                    m_collector.getIds().put(m_elem.getNameSpace(), m_elem);
                } else {
                    // Already used, the element is the parent.
                    if (m_parent == null) {
                        m_parent = m_elem.getNameSpace();
                    }
                }
            }
            
            m_collector.getElements().put(m_elem, m_parent);
        }
    }

    private class SubArrayVisitor extends EmptyVisitor implements AnnotationVisitor {
        /**
         * Parent element.
         */
        private Element m_elem;

        /**
         * Attribute name.
         */
        private String m_name;

        /**
         * Attribute value.
         * (accumulator)
         */
        private String m_acc;

        /**
         * Constructor.
         * @param elem : parent element.
         * @param name : attribute name.
         */
        public SubArrayVisitor(Element elem, String name) {
            m_elem = elem;
            m_name = name;
        }

        /**
         * Visit a 'simple' element of the visited array.
         * @param arg0 : null
         * @param arg1 : element value.
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (m_acc == null) {
                m_acc = "{" + arg1.toString();
            } else {
                m_acc = m_acc + "," + arg1.toString();
            }
        }

        /**
         * Visit an annotation element of the visited array.
         * @param arg0 : null
         * @param arg1 : annotation to visit
         * @return the visitor which will visit the annotation
         * @see org.objectweb.asm.commons.EmptyVisitor#visitAnnotation(java.lang.String, java.lang.String)
         */
        public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
            // Sub annotations are map to sub-elements
            Element elem = buildElement(arg1);
            m_elem.addElement(elem);
            return new CustomAnnotationVisitor(elem, m_collector, false, false);
        }

        /**
         * End of the visit.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() {
            if (m_acc != null) {
                // We have analyzed an attribute
                m_elem.addAttribute(new Attribute(m_name, m_acc + "}"));
            }
        }

    }
}
