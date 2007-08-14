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
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.EmptyVisitor;

/**
 * Collect metadata from classes by parsing annotation.
 * This class collects type (i.e.) annotations and create method & field collectors.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MetadataCollector extends EmptyVisitor implements Opcodes {

    /**
     * Class name.
     */
    private String m_className;
    
    /**
     * Root element of computed metadata.
     */
    private Element m_elem = new Element("component", "");
    
    /**
     * True if the visited class is a component type declaration (i.e. contains the @component annotation).
     */
    private boolean m_containsAnnotation = false;
    
    public Element getElem() {
        return m_elem;
    }
    
    public boolean isAnnotated() {
        return m_containsAnnotation;
    }

    /**
     * Start visiting a class.
     * Initialize the getter/setter generator, add the _cm field, add the pojo interface.
     * @param version : class version
     * @param access : class access
     * @param name : class name 
     * @param signature : class signature
     * @param superName : class super class
     * @param interfaces : implemented interfaces
     * @see org.objectweb.asm.ClassAdapter#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        m_className = name;
    }

    
    /**
     * Visit class annotations.
     * This method detects @component, @provides and @Element annotations.
     * @param desc : annotation descriptor.
     * @param visible : is the annotation visible at runtime.
     * @return the annotation visitor.
     * @see org.objectweb.asm.ClassAdapter#visitAnnotation(java.lang.String, boolean)
     */
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        // @Component
        if (desc.equals("Lorg/apache/felix/ipojo/annotations/Component;")) {
            // It is a component
            m_containsAnnotation = true;
            m_elem.addAttribute(new Attribute("className", m_className.replace('/', '.')));
            return new ComponentVisitor();
        }
        
        // @Provides
        if (desc.equals("Lorg/apache/felix/ipojo/annotations/Provides;")) {
            return new ProvidesVisitor();
        }
        
        //@Element
        if (desc.equals("Lorg/apache/felix/ipojo/annotations/Element;")) {
            return new ElementVisitor(m_elem);
        }
        
        return null;
    }

    /**
     * Visit a field.
     * Call the field collector visitor.
     * @param access : field access.
     * @param name : field name
     * @param desc : field descriptor
     * @param signature : field signature
     * @param value : field value (static field only)
     * @return the field visitor.
     * @see org.objectweb.asm.ClassAdapter#visitField(int, java.lang.String, java.lang.String, java.lang.String, java.lang.Object)
     */
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new FieldCollector(name, m_elem);
    }

    /**
     * Visit a method.
     * Call the method collector visitor.
     * @param access : method access
     * @param name : method name
     * @param desc : method descriptor
     * @param signature : method signature
     * @param exceptions : method exceptions
     * @return the Method Visitor.
     * @see org.objectweb.asm.ClassAdapter#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
     */
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new MethodCollector(name, m_elem);
    }
    
    /**
     * Parse the @provides annotation.
     */
    private class ProvidesVisitor extends EmptyVisitor implements AnnotationVisitor {
        /**
         * Provides element.
         */
        Element m_prov = new Element("provides", "");

        /**
         * Visit @provides annotation attributes.
         * @param arg0 : annotation attribute name
         * @param arg1 : annotation attribute value
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("specifications")) {
                m_prov.addAttribute(new Attribute("interface", arg1.toString()));
            }
            if (arg0.equals("factory")) {
                m_prov.addAttribute(new Attribute("factory", arg1.toString()));
            }
        }
        
        /**
         * End of the visit.
         * Append to the parent element the computed "provides" element.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() {
            m_elem.addElement(m_prov);            
        }
        
    }
    
    /**
     * Parse the @component annotation.
     */
    private class ComponentVisitor extends EmptyVisitor implements AnnotationVisitor {
        
        /**
         * Factory attribute.
         */
        private String m_factory;

        /**
         * Is the component an immediate component? 
         */
        private String m_immediate;
        
        /**
         * Component name (cannot be null). 
         */
        private String m_name;
        
        /**
         * Does the component exposes its architecture?
         */
        private String m_architecture;

        /**
         * Does the component propagate configuration to provided services?
         */
        private String m_propagation;

        /**
         * Visit @component annotation attribute.
         * @param arg0 : attribute name
         * @param arg1 : attribute value
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("factory")) {
                m_factory = arg1.toString();
                return;
            }
            if (arg0.equals("name")) {
                m_name = arg1.toString();
                return;
            }
            if (arg0.equals("immediate")) {
                m_immediate = arg1.toString();
                return;
            }
            if (arg0.equals("architecture")) {
                m_architecture = arg1.toString();
                return;
            }
            if (arg0.equals("propagation")) {
                m_propagation = arg1.toString();
                return;
            }
        }

        /**
         * End of the visit.
         * Append to the "component" element computed attribute.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() { 
            if (m_name == null) {
                m_name = m_className.replace('/', '.');
            }
            m_elem.addAttribute(new Attribute("name", m_name));
            if (m_factory == null) {
                m_elem.addAttribute(new Attribute("factory", "false"));
            } else {
                m_elem.addAttribute(new Attribute("factory", m_factory)); 
            }
            if (m_architecture != null) {
                m_elem.addAttribute(new Attribute("architecture", m_architecture));
            }
            if (m_immediate != null) {
                m_elem.addAttribute(new Attribute("immediate", m_immediate));
            }
            if (m_propagation != null) {
                Element props = new Element("properties", "");
                props.addAttribute(new Attribute("propagation", m_propagation));
                m_elem.addElement(props);
            }
        }        
    }

    /**
     * Parse the @Element & @SubElement annotations. 
     */
    private class ElementVisitor extends EmptyVisitor implements AnnotationVisitor {
        
        /**
         * Element name.
         */
        private String m_name;
        
        /**
         * Element namespace. 
         */
        private String m_namespace;
        
        /**
         * Parent Element. 
         */
        private Element m_parent;
        
        /**
         * Accumulator element to store temporary attributes and sub-elements.
         */
        private Element m_accu = new Element("accu", "");
        
        /**
         * Constructor.
         * @param parent : parent element.
         */
        public ElementVisitor(Element parent) {
            m_parent = parent;
        }

        /**
         * Visit annotation attribute.
         * @param arg0 : name of the attribute
         * @param arg1 : value of the attribute
         * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("name")) {
                m_name = arg1.toString();
            }
            if (arg0.equals("namespace")) {
                m_namespace = arg1.toString();
            }
        }

        
        /**
         * Visit array annotation attribute (attributes & elements).
         * @param arg0 : attribute name
         * @return the annotation visitor which will visit the content of the array
         * @see org.objectweb.asm.AnnotationVisitor#visitArray(java.lang.String)
         */
        public AnnotationVisitor visitArray(String arg0) {
            if (arg0.equals("attributes")) {
                return new EmptyVisitor() {
                    public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
                        return new AttributeVisitor(m_accu);
                    }
                    
                };
            }
            if (arg0.equals("elements")) {
                return new EmptyVisitor() {
                    public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
                        return new ElementVisitor(m_accu);
                    }
                    
                };
            }
            return null;
        }

        /**
         * End of the visit.
         * Append computed element to the parent element.
         * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
         */
        public void visitEnd() {
            Element elem = null;
            if (m_namespace != null) {
                elem = new Element(m_name, m_namespace);
            } else {
                elem = new Element(m_name, "");
            }

            Attribute[] atts = m_accu.getAttributes();
            for (int i = 0; i < atts.length; i++) {
                elem.addAttribute(atts[i]);
            }

            Element[] elems = m_accu.getElements();
            for (int i = 0; i < elems.length; i++) {
                elem.addElement(elems[i]);
            }

            m_parent.addElement(elem);
        }
        
    }
    
    /**
     * Parse an @attribute annotation.
     */
    private class AttributeVisitor extends EmptyVisitor implements AnnotationVisitor {
        /**
         * Parent element.
         */
        private Element m_parent;

        /**
         * Attribute name.
         */
        private String m_name;
        
        /**
         * Attribute value. 
         */
        private String m_value;
        
        /**
         * Constructor.
         * @param parent : parent element.
         */
        public AttributeVisitor(Element parent) {
            m_parent = parent;
        }

        /**
         * Visit attributes. 
         * @param arg0 : attribute name
         * @param arg1 : attribute value
         * @see org.objectweb.asm.commons.EmptyVisitor#visit(java.lang.String, java.lang.Object)
         */
        public void visit(String arg0, Object arg1) {
            if (arg0.equals("name")) {
                m_name = arg1.toString();
                return;
            }
            if (arg0.equals("value")) {
                m_value = arg1.toString();
            }
        }

        /**
         * End of the visit.
         * Append this current attribute to the parent element.
         * @see org.objectweb.asm.commons.EmptyVisitor#visitEnd()
         */
        public void visitEnd() {
            m_parent.addAttribute(new Attribute(m_name, m_value));
        }        
    }
}
