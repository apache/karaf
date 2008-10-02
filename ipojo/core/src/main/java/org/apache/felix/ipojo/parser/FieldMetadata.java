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
package org.apache.felix.ipojo.parser;

import org.apache.felix.ipojo.metadata.Element;

/**
 * A Field Metadata represents a field of the implementation class.
 * This class avoids using reflection to get the type and the name of a field.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldMetadata {
    
    /**
     * The name of the field.
     */
    private String m_name;
    
    /**
     * The yype of the field. 
     */
    private String m_type;
    
    /**
     * Creates a field metadata.
     * This constructor is used when creating the {@link PojoMetadata}
     * object.
     * @param metadata the field manipulation element from Manipulation
     * Metadata.
     */
    FieldMetadata(Element metadata) {
        m_name = metadata.getAttribute("name");
        m_type = metadata.getAttribute("type");
    }
    
    /**
     * Creates a field metadata.
     * This constructor can be used to avoid using {@link PojoMetadata}.
     * Be care that creating such {@link FieldMetadata} does not assert its 
     * presence in the implementation class.
     * @param field the field name.
     * @param type the type of the field.
     */
    public FieldMetadata(String field, String type) {
        m_name = field;
        m_type = type;
    }
    
    public String getFieldName() { return m_name; }
    
    public String getFieldType() { return m_type; }
    
    /**
     * Gets the 'reflective' type of the given type.
     * The reflective type is the type used by the Java Reflection API.
     * More precisely this method handles the array cases 
     * @param type the type to analyze to find the Java reflective type.
     * @return the reflective type corresponding to this field.
     */
    public static String getReflectionType(String type) {
        // Primitive Array 
        if (type.endsWith("[]") && type.indexOf('.') == -1) {
            int index = type.indexOf('[');
            return '[' + getInternalPrimitiveType(type.substring(0, index));
        }
        // Non-Primitive Array 
        if (type.endsWith("[]") && type.indexOf('.') != -1) {
            int index = type.indexOf('[');
            return "[L" + type.substring(0, index) + ";";
        }
        // The type is not an array.
        return type;
    }
    
    /**
     * Gets the internal notation for primitive type.
     * @param string the String form of the type
     * @return the internal notation or <code>null</code> if not found
     */
    public static String getInternalPrimitiveType(String string) {
        if (string.equalsIgnoreCase("boolean")) {
            return "Z";
        }
        if (string.equalsIgnoreCase("char")) {
            return "C";
        }
        if (string.equalsIgnoreCase("byte")) {
            return "B";
        }
        if (string.equalsIgnoreCase("short")) {
            return "S";
        }
        if (string.equalsIgnoreCase("int")) {
            return "I";
        }
        if (string.equalsIgnoreCase("float")) {
            return "F";
        }
        if (string.equalsIgnoreCase("long")) {
            return "J";
        }
        if (string.equalsIgnoreCase("double")) {
            return "D";
        }
        return null;
    }
    
    /**
     * Gets the iPOJO primitive type from the given primitive class.
     * @param clazz the class of the primitive type
     * @return the iPOJO primitive type name or <code>null</code> if
     * not found.
     */
    public static String getPrimitiveTypeByClass(Class clazz) {
        if (clazz.equals(Boolean.TYPE)) {
            return "boolean";
        }
        if (clazz.equals(Character.TYPE)) {
            return "char";
        }
        if (clazz.equals(Byte.TYPE)) {
            return "byte";
        }
        if (clazz.equals(Short.TYPE)) {
            return "short";
        }
        if (clazz.equals(Integer.TYPE)) {
            return "int";
        }
        if (clazz.equals(Float.TYPE)) {
            return "float";
        }
        if (clazz.equals(Long.TYPE)) {
            return "long";
        }
        if (clazz.equals(Double.TYPE)) {
            return "double";
        }
        return null;
    }

}
