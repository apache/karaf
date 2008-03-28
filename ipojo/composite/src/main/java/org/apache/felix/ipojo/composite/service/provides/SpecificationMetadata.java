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
package org.apache.felix.ipojo.composite.service.provides;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.BundleContext;

/**
 * Represent a service specification.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class SpecificationMetadata {

    /**
     * Name of the specification, i.e. name of the interface.
     */
    private String m_name;

    /**
     * List of the method contained in the specification.
     */
    private List/* <MethodMetadata> */m_methods = new ArrayList/* <MethodMetadata> */();

    /**
     * Is the specification an aggregate?
     */
    private boolean m_isAggregate;

    /**
     * Is the specification optional?
     */
    private boolean m_isOptional = false;

    /**
     * Is the specification an interface?
     */
    private boolean m_isInterface = true;

    /**
     * Component Type.
     */
    private String m_componentType = null;

    /**
     * Reference on the handler.
     */
    private ProvidedServiceHandler m_handler;

    /**
     * Constructor.
     * @param name : specification name.
     * @param context : bundle context.
     * @param isAggregate : is the specification aggregate.
     * @param isOptional : is the specification optional.
     * @param psd : the handler.
     */
    public SpecificationMetadata(String name, BundleContext context, boolean isAggregate, boolean isOptional, ProvidedServiceHandler psd) {
        m_name = name;
        m_handler = psd;

        // Populate methods :
        try {
            Class clazz = context.getBundle().loadClass(name);
            Method[] methods = clazz.getMethods();
            for (int i = 0; i < methods.length; i++) {
                MethodMetadata method = new MethodMetadata(methods[i]);
                m_methods.add(method);
            }
        } catch (ClassNotFoundException e) {
            m_handler.error("Cannot open " + name + " : " + e.getMessage());
            return;
        }

        m_isAggregate = isAggregate;
        m_isOptional = isOptional;
    }

    /**
     * Constructor.
     * @param clazz : class
     * @param type : component type
     * @param psd : the parent handler
     */
    public SpecificationMetadata(Class clazz, String type, ProvidedServiceHandler psd) {
        m_handler = psd;
        m_isAggregate = false;
        m_isOptional = false;
        m_componentType = type;
        m_name = clazz.getName();
        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            MethodMetadata method = new MethodMetadata(methods[i]);
            m_methods.add(method);
        }
        m_isInterface = false;
    }

    public String getName() {
        return m_name;
    }

    public List/* <MethodMetadata> */getMethods() {
        return m_methods;
    }

    /**
     * Get a method by its name.
     * @param name : method name
     * @return the method metadata contained in the current specification with the given name. Null if the method is not found.
     */
    public MethodMetadata getMethodByName(String name) {
        for (int i = 0; i < m_methods.size(); i++) {
            MethodMetadata met = (MethodMetadata) m_methods.get(i);
            if (met.getMethod().getName().equals(name)) { return met; }
        }
        return null;
    }

    public boolean isAggregate() {
        return m_isAggregate;
    }

    public boolean isOptional() {
        return m_isOptional;
    }

    public boolean isInterface() {
        return m_isInterface;
    }

    public void setIsOptional(boolean optional) {
        m_isOptional = optional;
    }

    public String getComponentType() {
        return m_componentType;
    }

}
