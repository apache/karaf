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
package org.apache.felix.ipojo.manipulation;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.manipulation.ClassChecker.AnnotationDescriptor;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.objectweb.asm.Type;

/**
 * Method Descriptor describe a method.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodDescriptor {

    /**
     * Method name.
     */
    private String m_name;

    /**
     * Returned type.
     */
    private String m_returnType;

    /**
     * Argument types.
     */
    private String[] m_arguments;
    
    /**
     * The descriptor of the method.
     */
    private String m_desc;
    
    
    /**
     * The list of {@link AnnotationDescriptor} attached to this
     * method. 
     */
    private List m_annotations;

    /**
     * Constructor.
     * @param name : name of the method.
     * @param desc : descriptor of the method.
     */
    public MethodDescriptor(String name, String desc) {
        m_name = name;
        m_desc = desc;
        Type ret = Type.getReturnType(desc);
        Type[] args = Type.getArgumentTypes(desc);

        m_returnType = getType(ret);
        m_arguments = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            m_arguments[i] = getType(args[i]);
        }
    }
    
    /**
     * Add an annotation to the current method.
     * @param ann annotation to add
     */
    public void addAnnotation(AnnotationDescriptor ann) {
        if (m_annotations == null) {
            m_annotations = new ArrayList();
        }
        m_annotations.add(ann);
    }
    
    public List getAnnotations() {
        return m_annotations;
    }
    
    public String getDescriptor() {
        return m_desc;
    }

    /**
     * Compute method manipulation metadata.
     * @return the element containing metadata about this method.
     */
    public Element getElement() {
        Element method = new Element("method", "");
        method.addAttribute(new Attribute("name", m_name));

        // Add return
        if (!m_returnType.equals("void")) {
            method.addAttribute(new Attribute("return", m_returnType));
        }

        // Add arguments
        if (m_arguments.length > 0) {
            StringBuffer args = new StringBuffer("{");
            args.append(m_arguments[0]);
            for (int i = 1; i < m_arguments.length; i++) {
                args.append("," + m_arguments[i]);
            }
            args.append("}");
            method.addAttribute(new Attribute("arguments", args.toString()));
        }

        return method;
    }

    /**
     * Get the iPOJO internal type for the given type.
     * @param type : type.
     * @return the iPOJO internal type.
     */
    private String getType(Type type) {
        switch (type.getSort()) {
            case Type.ARRAY:
                Type elemType = type.getElementType();
                return getType(elemType) + "[]";
            case Type.BOOLEAN:
                return "boolean";
            case Type.BYTE:
                return "byte";
            case Type.CHAR:
                return "char";
            case Type.DOUBLE:
                return "double";
            case Type.FLOAT:
                return "float";
            case Type.INT:
                return "int";
            case Type.LONG:
                return "long";
            case Type.OBJECT:
                return type.getClassName();
            case Type.SHORT:
                return "short";
            case Type.VOID:
                return "void";
            default:
                return "unknown";
        }
    }

    public String getName() {
        return m_name;
    }

}
