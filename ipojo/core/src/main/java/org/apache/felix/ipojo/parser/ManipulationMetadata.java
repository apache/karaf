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
 * Manipulation Metadata allows getting information about the implementation class
 * whithout doing reflection. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ManipulationMetadata {
    
    /**
     * List of implemented interfaces.
     */
    private String[] m_interfaces = new String[0];
    
    /**
     * List of fields.
     */
    private FieldMetadata[] m_fields = new FieldMetadata[0];
    
    /**
     * List of methods. 
     */
    private MethodMetadata[] m_methods = new MethodMetadata[0];
    
    
    /**
     * Constructor.
     * Manipulation Metadata object are created from component type metadata by
     * parsing manipulation metadata.
     * @param metadata : component type metadata
     */
    public ManipulationMetadata(Element metadata) {
        Element manip = metadata.getElements("manipulation", "")[0];
        for (int i = 0; i < manip.getElements().length; i++) {
            if (manip.getElements()[i].getName().equals("field")) {
                FieldMetadata fm = new FieldMetadata(manip.getElements()[i]);
                addField(fm);
            } else if (manip.getElements()[i].getName().equals("method")) {
                MethodMetadata fm = new MethodMetadata(manip.getElements()[i]);
                addMethod(fm);
            } else if (manip.getElements()[i].getName().equals("interface")) {
                addInterface(manip.getElements()[i].getAttribute("name"));
            }
        }
    }
    
    public MethodMetadata[] getMethods() { return m_methods; }
    
    public FieldMetadata[] getFields() { return m_fields; }
    
    public String[] getInterfaces() { return m_interfaces; }
    
    /**
     * Get the field metadata for the given name. 
     * @param name : name of the field
     * @return the corresponding field metadata or null if not found
     */
    public FieldMetadata getField(String name) { 
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name)) { return m_fields[i]; }
        }
        return null;
    }
    
    /**
     * Get the field metadata for the given name and type. 
     * @param name : name of the field
     * @param type : type of the field
     * @return the corresponding field metadata or null if not found
     */
    public FieldMetadata getField(String name, String type) { 
        for (int i = 0; i < m_fields.length; i++) {
            if (m_fields[i].getFieldName().equalsIgnoreCase(name) && m_fields[i].getFieldType().equalsIgnoreCase(type)) { return m_fields[i]; }
        }
        return null;
    }
    
    /**
     * Check if the given interface name is implemented.
     * @param itf : interface to check.
     * @return true if the implementation class implement the given interface.
     */
    public boolean isInterfaceImplemented(String itf) {
        for (int i = 0; i < m_interfaces.length; i++) {
            if (m_interfaces[i].equals(itf)) { return true; }
        }
        return false;
    }
    
    /**
     * Get the MethodMetadata corresponding to the method
     * (contained in the implementation class) to given name.
     * If several method match, the first one is returned.
     * @param name : name of the method to look for.
     * @return the Method Metadate or null if not found
     */
    public MethodMetadata getMethod(String name) {
        for (int i = 0; i < m_methods.length; i++) {
            if (m_methods[i].getMethodName().equalsIgnoreCase(name)) { return m_methods[i]; }
        }
        return null;
    }
    
    /**
     * Get the MethodMetadata list corresponding to the method
     * (contained in the implementation class) to given name.
     * All methods contained in the implementation class matching 
     * with the name are in the returned list.
     * @param name : name of the method to look for.
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
     * Get the MethodMetadata corresponding to the method
     * (contained in the implementation class) to given name 
     * and argument types.
     * @param name : name of the method to look for.
     * @param types : array of the argument types of the method 
     * @return the Method Metadate or null if not found
     */
    public MethodMetadata getMethod(String name, String[] types) {
        for (int i = 0; i < m_methods.length; i++) {
            if (m_methods[i].getMethodName().equalsIgnoreCase(name) && m_methods[i].getMethodArguments().length == types.length) {
                boolean ok = true;
                for (int j = 0; ok && j < types.length; j++) {
                    if (! types[j].equals(m_methods[i].getMethodArguments()[j])) {
                        ok = false;
                    }
                }
                if (ok) { return m_methods[i]; }
            }
        }
        return null;
    }
        
     /**
      * Add a method to the list.
     * @param mm : Method Metadata to add.
     */
    private void addMethod(MethodMetadata mm) {
        for (int i = 0; (m_methods != null) && (i < m_methods.length); i++) {
            if (m_methods[i] == mm) {
                return;
            }
        }

        if (m_methods.length > 0) {
            MethodMetadata[] newInstances = new MethodMetadata[m_methods.length + 1];
            System.arraycopy(m_methods, 0, newInstances, 0, m_methods.length);
            newInstances[m_methods.length] = mm;
            m_methods = newInstances;
        } else {
            m_methods = new MethodMetadata[] { mm };
        }
    }
        
     /**
      * Add a field to the list.
     * @param mm : the Field Metadata to add.
     */
    private void addField(FieldMetadata mm) {
        for (int i = 0; (m_fields != null) && (i < m_fields.length); i++) {
            if (m_fields[i] == mm) {
                return;
            }
        }

        if (m_fields.length > 0) {
            FieldMetadata[] newInstances = new FieldMetadata[m_fields.length + 1];
            System.arraycopy(m_fields, 0, newInstances, 0, m_fields.length);
            newInstances[m_fields.length] = mm;
            m_fields = newInstances;
        } else {
            m_fields = new FieldMetadata[] { mm };
        }
    }
        
    /**
     * Add the interface to the list.
     * @param mm : the interface name to add.
     */
    private void addInterface(String mm) {
        for (int i = 0; (m_interfaces != null) && (i < m_interfaces.length); i++) {
            if (m_interfaces[i] == mm) {
                return;
            }
        }

        if (m_interfaces.length > 0) {
            String[] newInstances = new String[m_interfaces.length + 1];
            System.arraycopy(m_interfaces, 0, newInstances, 0, m_interfaces.length);
            newInstances[m_interfaces.length] = mm;
            m_interfaces = newInstances;
        } else {
            m_interfaces = new String[] { mm };
        }
    }

}
