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

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Manipulation Metadata allows getting information about the implementation class
 * without using reflection such as implemented interfaces, super class,
 * methods and fields. 
 * This method allows getting object to register {@link org.apache.felix.ipojo.FieldInterceptor} and
 * {@link org.apache.felix.ipojo.MethodInterceptor}.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class PojoMetadata {
    
    /**
     * The list of implemented interfaces.
     */
    private String[] m_interfaces = new String[0];
    
    /**
     * The list of fields.
     */
    private FieldMetadata[] m_fields = new FieldMetadata[0];
    
    /**
     * The list of methods. 
     */
    private MethodMetadata[] m_methods = new MethodMetadata[0];

    /**
     * The Super class (if <code>null</code> for {@link Object}).
     */
    private String m_super;
    
    
    /**
     * Creates Pojo metadata.
     * Manipulation Metadata object are created from component type metadata by
     * parsing manipulation metadata.
     * @param metadata the component type metadata
     * @throws ConfigurationException if the manipulation metadata cannot be found
     */
    public PojoMetadata(Element metadata) throws ConfigurationException {
        Element[] elems = metadata.getElements("manipulation", "");
        if (elems == null) {
            throw new ConfigurationException("The component " + metadata.getAttribute("classname") + " has no manipulation metadata"); 
        }
        Element manip = elems[0];
        m_super = manip.getAttribute("super");
        Element[] fields = manip.getElements("field");
        for (int i = 0; fields != null && i < fields.length; i++) {
            FieldMetadata field = new FieldMetadata(fields[i]);
            addField(field);
        }
        Element[] methods = manip.getElements("method");
        for (int i = 0; methods != null && i < methods.length; i++) {
            MethodMetadata method = new MethodMetadata(methods[i]);
            addMethod(method);
        }
        Element[] itfs = manip.getElements("interface");
        for (int i = 0; itfs != null && i < itfs.length; i++) {
            addInterface(itfs[i].getAttribute("name"));
        }
    }
    
    public MethodMetadata[] getMethods() { return m_methods; }
    
    public FieldMetadata[] getFields() { return m_fields; }
    
    public String[] getInterfaces() { return m_interfaces; }
    
    /**
     * Gets the field metadata for the given name. 
     * @param name : the name of the field
     * @return the corresponding field metadata or <code>null</code> if not found
     */
    public FieldMetadata getField(String name) { 
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name)) { return m_fields[i]; }
        }
        return null;
    }
    
    /**
     * Gets the field metadata for the given name and type. 
     * @param name : the name of the field
     * @param type : the type of the field
     * @return the corresponding field metadata or <code>null</code> if not found
     */
    public FieldMetadata getField(String name, String type) { 
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name) && m_fields[i].getFieldType().equalsIgnoreCase(type)) { return m_fields[i]; }
        }
        return null;
    }
    
    /**
     * Checks if the given interface name is implemented.
     * This methods checks on interface directly implemented
     * by the implementation class.
     * @param itf the interface to check.
     * @return <code>true</code> if the implementation class implements
     * the given interface.
     */
    public boolean isInterfaceImplemented(String itf) {
        for (int i = 0; i < m_interfaces.length; i++) {
            if (m_interfaces[i].equals(itf)) { return true; }
        }
        return false;
    }
    
    /**
     * Gets the MethodMetadata corresponding to the method
     * (contained in the implementation class) with
     * the given name.
     * If several methods match, the first one is returned.
     * @param name the name of the method to find.
     * @return the method metadata object or <code>null</code> if not found
     */
    public MethodMetadata getMethod(String name) {
        for (int i = 0; i < m_methods.length; i++) {
            if (m_methods[i].getMethodName().equalsIgnoreCase(name)) { return m_methods[i]; }
        }
        return null;
    }
    
    /**
     * Gets the MethodMetadata list corresponding to the method
     * (contained in the implementation class) to given name.
     * All methods contained in the implementation class matching 
     * with the name are in the returned list.
     * @param name the name of the method to look for.
     * @return the Method Metadata array or an empty array if not found
     */
    public MethodMetadata[] getMethods(String name) {
        MethodMetadata[] mms = new MethodMetadata[0];
        for (int i = 0; i < m_methods.length; i++) {
            if (m_methods[i].getMethodName().equalsIgnoreCase(name)) { 
                if (mms.length > 0) {
                    MethodMetadata[] newInstances = new MethodMetadata[mms.length + 1];
                    System.arraycopy(mms, 0, newInstances, 0, mms.length);
                    newInstances[mms.length] = m_methods[i];
                    mms = newInstances;
                } else {
                    mms = new MethodMetadata[] { m_methods[i] };
                }
            }
        }
        return mms;
    }
    
    /**
     * Gets the MethodMetadata corresponding to the method
     * (contained in the implementation class) to given name 
     * and argument types.
     * @param name the name of the method to look for.
     * @param types the array of the argument types of the method 
     * @return the Method Metadata or <code>null</code> if not found
     */
    public MethodMetadata getMethod(String name, String[] types) {
        for (int i = 0; i < m_methods.length; i++) {
            if (m_methods[i].getMethodName().equalsIgnoreCase(name) && m_methods[i].getMethodArguments().length == types.length) {
                int argIndex = 0;
                for (; argIndex < types.length; argIndex++) {
                    if (! types[argIndex].equals(m_methods[i].getMethodArguments()[argIndex])) {
                        break;
                    }
                }
                if (argIndex == types.length) { return m_methods[i]; } // No mismatch detected.
            }
        }
        return null;
    }
        
     /**
     * Adds a method to the list.
     * This method is used during the creation of the {@link PojoMetadata}
     * object.
     * @param method the Method Metadata to add.
     */
    private void addMethod(MethodMetadata method) {
        for (int i = 0; (m_methods != null) && (i < m_methods.length); i++) {
            if (m_methods[i] == method) {
                return;
            }
        }

        if (m_methods.length > 0) {
            MethodMetadata[] newInstances = new MethodMetadata[m_methods.length + 1];
            System.arraycopy(m_methods, 0, newInstances, 0, m_methods.length);
            newInstances[m_methods.length] = method;
            m_methods = newInstances;
        } else {
            m_methods = new MethodMetadata[] { method };
        }
    }
        
     /**
     * Adds a field to the list.
     * This method is used during the creation of the {@link PojoMetadata}
     * object.
     * @param field the Field Metadata to add.
     */
    private void addField(FieldMetadata field) {
        for (int i = 0; (m_fields != null) && (i < m_fields.length); i++) {
            if (m_fields[i] == field) {
                return;
            }
        }

        if (m_fields.length > 0) {
            FieldMetadata[] newInstances = new FieldMetadata[m_fields.length + 1];
            System.arraycopy(m_fields, 0, newInstances, 0, m_fields.length);
            newInstances[m_fields.length] = field;
            m_fields = newInstances;
        } else {
            m_fields = new FieldMetadata[] { field };
        }
    }
        
    /**
     * Adds the interface to the list.
     * This method is used during the creation of the {@link PojoMetadata}
     * object.
     * @param itf the interface name to add.
     */
    private void addInterface(String itf) {
        for (int i = 0; (m_interfaces != null) && (i < m_interfaces.length); i++) {
            if (m_interfaces[i] == itf) {
                return;
            }
        }

        if (m_interfaces.length > 0) {
            String[] newInstances = new String[m_interfaces.length + 1];
            System.arraycopy(m_interfaces, 0, newInstances, 0, m_interfaces.length);
            newInstances[m_interfaces.length] = itf;
            m_interfaces = newInstances;
        } else {
            m_interfaces = new String[] { itf };
        }
    }

    public String getSuperClass() {
        return m_super;
    }

}
