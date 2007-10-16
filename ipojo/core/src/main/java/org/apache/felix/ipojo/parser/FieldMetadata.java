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
 * A Field Metadata represent a field of an implementation class.
 * This class allow to avoid reflection to get the type and the name of a field.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class FieldMetadata {
    
    /**
     * Name of the field.
     */
    private String m_name;
    
    /**
     * Type of the field. 
     */
    private String m_type;
    
    /**
     * Constructor.
     * @param metadata : field manipulation element.
     */
    FieldMetadata(Element metadata) {
        m_name = metadata.getAttribute("name");
        m_type = metadata.getAttribute("type");
    }
    
    /**
     * Constructor.
     * @param field : field name.
     * @param type : type of the field.
     */
    public FieldMetadata(String field, String type) {
        m_name = field;
        m_type = type;
    }
    
    public String getFieldName() { return m_name; }
    
    public String getFieldType() { return m_type; }
    
    /**
     * Get the 'reflective' type of the field.
     * The reflective type is the type used by the Java Reflection API.
     * @return : the reflective type corresponding to this field.
     */
    public String getReflectionType() {
        // Primitive Array 
        if (m_type.endsWith("[]") && m_type.indexOf(".") == -1) {
            String arr = "";
            for (int i = 0; i < m_type.length(); i++) {
                if (m_type.charAt(i) == '[') { arr += '['; }
            }
            int index = m_type.indexOf('[');
            String t = m_type.substring(0, index);
            return arr + getInternalPrimitiveType(t);
        }
        // Non-Primitive Array 
        if (m_type.endsWith("[]") && m_type.indexOf(".") != -1) {
            String arr = "";
            for (int i = 0; i < m_type.length(); i++) {
                if (m_type.charAt(i) == '[') { arr += '['; }
            }
            int index = m_type.indexOf('[');
            String t = m_type.substring(0, index);
            return arr + "L" + t + ";";
        }
        // Simple type 
        if (!m_type.endsWith("[]")) {
            return m_type;
        }
        
        return null;
    }
    
    /**
     * Get the internal notation for primitive type.
     * @param string : Stringform of the type
     * @return the internal notation or null if not found
     */
    private String getInternalPrimitiveType(String string) {
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
        System.err.println("No primitive type found for " + m_type);
        return null;
    }

}
