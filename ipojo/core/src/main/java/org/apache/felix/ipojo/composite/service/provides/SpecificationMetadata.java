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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.handlers.dependency.nullable.MethodSignature;
import org.apache.felix.ipojo.handlers.dependency.nullable.MethodSignatureVisitor;
import org.apache.felix.ipojo.util.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
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
     * Componenet Type.
     */
    private String m_componentType = null;

    /**
     * Reference on the handler.
     */
    private ProvidedServiceHandler m_handler;

    /**
     * Constructor.
     * @param name : specification name.
     * @param bc : bundle context.
     * @param isAggregate : is the specification aggregate.
     * @param isOptional : is the specification optional.
     * @param psd : the handler.
     */
    public SpecificationMetadata(String name, BundleContext bc, boolean isAggregate, boolean isOptional, ProvidedServiceHandler psd) {
        m_name = name;
        m_handler = psd;
    
        // Populate methods :
        URL url = bc.getBundle().getResource(name.replace('.', '/') + ".class");
        InputStream is = null;
        ClassReader cr = null;
        MethodSignatureVisitor msv = null;
        try {
            is = url.openStream();
            cr = new ClassReader(is);
            msv = new MethodSignatureVisitor();
            cr.accept(msv, ClassReader.SKIP_FRAMES);
            is.close();
        } catch (IOException e) {
            m_handler.getManager().getFactory().getLogger().log(Logger.ERROR, "Cannot open " + name + " : " + e.getMessage());
            return;
        }
    
        MethodSignature[] containsMethods = msv.getMethods();
        for (int i = 0; i < containsMethods.length; i++) {
            MethodSignature met = containsMethods[i];
            String desc = met.getDesc();
            MethodMetadata method = new MethodMetadata(met.getName(), desc);
    
            Type[] args = Type.getArgumentTypes(desc);
            String[] exceptionClasses = met.getException();
            for (int j = 0; j < args.length; j++) {
                method.addArgument(args[j].getClassName());
            }
            for (int j = 0; j < exceptionClasses.length; j++) {
                method.addException(exceptionClasses[j]);
            }
    
            addMethod(method);
        }
    
        m_isAggregate = isAggregate;
        m_isOptional = isOptional;
    }

    /**
     * Constructor.
     * @param c : class
     * @param type : componenet type
     * @param psd : the parent handler
     */
    public SpecificationMetadata(Class c, String type, ProvidedServiceHandler psd) {
        m_handler = psd;
        m_isAggregate = false;
        m_isOptional = false;
        m_componentType = type;
        m_name = c.getName();
        Method[] methods = c.getMethods();
        for (int i = 0; i < methods.length; i++) {
            String desc = Type.getMethodDescriptor(methods[i]);
            MethodMetadata method = new MethodMetadata(methods[i].getName(), desc);
            Type[] args = Type.getArgumentTypes(desc);
            Class[] exceptionClasses = methods[i].getExceptionTypes();
            for (int j = 0; j < args.length; j++) {
                method.addArgument(args[j].getClassName());
            }
            for (int j = 0; j < exceptionClasses.length; j++) {
                method.addException(exceptionClasses[j].getName());
            }
    
            addMethod(method);
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
     * Add a method metadata to the current specification.
     * @param mm : the method metadata to add.
     */
    public void addMethod(MethodMetadata mm) {
        m_methods.add(mm);
    }

    /**
     * Get a method by its name.
     * @param name : method name
     * @return the method metadata contained in the current specification with the given name. Null if the method is not found.
     */
    public MethodMetadata getMethodByName(String name) {
        for (int i = 0; i < m_methods.size(); i++) {
            MethodMetadata met = (MethodMetadata) m_methods.get(i);
            if (met.getMethodName().equals(name)) {
                return met;
            }
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
