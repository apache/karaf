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

import java.lang.reflect.Method;

import org.apache.felix.ipojo.metadata.Element;

/**
 * A Method Metadata represents a method from the implementation class.
 * This class allows getting information about a method : name, arguments, return type...
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodMetadata {
    
    /**
     * Empty Constructor Method Id.
     */
    public static final String EMPTY_CONSTRUCTOR_ID = "$init";

    /**
     * Bundle Context Constructor Method Id.
     */
    public static final String BC_CONSTRUCTOR_ID = "$init$org_osgi_framework_BundleContext";
    
    /**
     * Constructor Prefix.
     */
    public static final String CONSTRUCTOR_PREFIX = "$init";

    /**
     * The name of the method.
     */
    private String m_name;

    /**
     * The argument type array. 
     */
    private String[] m_arguments = new String[0];

    /**
     * The returned type. 
     */
    private String m_return = "void";

    /**
     * Creates a Method Metadata.
     * @param metadata the method manipulation element.
     */
    MethodMetadata(Element metadata) {
        m_name = metadata.getAttribute("name");
        String arg = metadata.getAttribute("arguments");
        String result = metadata.getAttribute("return");
        if (arg != null) {
            m_arguments = ParseUtils.parseArrays(arg);
        }
        if (result != null) {
            m_return = result;
        }
    }

    public String getMethodName() {
        return m_name;
    }

    public String[] getMethodArguments() {
        return m_arguments;
    }

    public String getMethodReturn() {
        return m_return;
    }

    /**
     * Gets the method unique identifier. For internal usage only.
     * A method identifier is a unique string that can be a java field
     * that identify the method.
     * @return the method identifier.
     */
    public String getMethodIdentifier() {
        StringBuffer identifier = new StringBuffer(m_name);
        for (int i = 0; i < m_arguments.length; i++) {
            String arg = m_arguments[i];
            identifier.append('$');
            if (arg.endsWith("[]")) {
                arg = arg.substring(0, arg.length() - 2);
                identifier.append(arg.replace('.', '_'));
                identifier.append("__"); // Replace [] by __
            } else {
                identifier.append(arg.replace('.', '_'));
            }
        }
        return identifier.toString();
    }

    /**
     * Computes the method id for the given Method object. 
     * @param method the Method object.
     * @return the method id.
     */
    public static String computeMethodId(Method method) {
        StringBuffer identifier = new StringBuffer(method.getName());
        Class[] args = method.getParameterTypes();
        for (int i = 0; i < args.length; i++) {
            identifier.append('$'); // Argument separator.
            if (args[i].isArray()) {
                if (args[i].getComponentType().isPrimitive()) {
                    // Primitive array
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i].getComponentType()));
                } else {
                    // Object array
                    identifier.append(args[i].getComponentType().getName().replace('.', '_')); // Replace '.' by '_'
                }
                identifier.append("__"); // Add __ (array)
            } else {
                if (args[i].isPrimitive()) {
                    // Primitive type
                    identifier.append(FieldMetadata.getPrimitiveTypeByClass(args[i]));
                } else {
                    // Object type
                    identifier.append(args[i].getName().replace('.', '_')); // Replace '.' by '_'
                }
            }
        }
        return identifier.toString();
    }
}
