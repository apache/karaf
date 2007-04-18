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

import java.util.ArrayList;

import org.apache.felix.ipojo.handlers.dependency.nullable.MethodSignature;

/**
 * Information on Method for the composition.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class MethodMetadata {

    /**
     * ONE POLICY.
     */
    public static final int ONE_POLICY = 1;

    /**
     * ALL POLICY. 
     */
    public static final int ALL_POLICY = 2;

    /**
     * Method Name.
     */
    private String m_methodName;

    /**
     * Internal Descriptor.
     */
    private String m_descriptor;

    /**
     * List of arguments. 
     */
    private ArrayList/* <String> */m_arguments = new ArrayList/* <String> */();

    /**
     * List of exceptions.
     */
    private ArrayList/* <String> */m_exceptions = new ArrayList/* <String> */();
    
    /**
     * Delegator field.
     */
    private FieldMetadata m_delegation;

    /**
     * Delegation policy (default = ONE).
     */
    private int m_policy = ONE_POLICY;

    /**
     * Constructor.
     * @param name : name of the method.
     * @param desc : description of the method.
     */
    public MethodMetadata(String name, String desc) {
        m_methodName = name;
        m_descriptor = desc;
    }

    /**
     * Add an argument.
     * @param type : type of the argument.
     */
    public void addArgument(String type) {
        m_arguments.add(type);
    }

    /**
     * Add an exception.
     * @param exception : name of the exception.
     */
    public void addException(String exception) {
        m_exceptions.add(exception);
    }

    public ArrayList/* <String> */getArguments() {
        return m_arguments;
    }

    public ArrayList/* <String> */getExceptions() {
        return m_exceptions;
    }

    public String getMethodName() {
        return m_methodName;
    }

    

    public void setDelegation(FieldMetadata dm) {
        m_delegation = dm;
    }

    public FieldMetadata getDelegation() {
        return m_delegation;
    }

    /**
     * Check if two method metadata are equals.
     * @param mm : the method metadata to compare with the current method metadata.
     * @return true if the two method are equals
     */
    public boolean equals(MethodMetadata mm) {
        // Test if the name are the same, #args and #exception are the same.
        if (!mm.getMethodName().equals(m_methodName) || mm.getArguments().size() != m_arguments.size()) {
            return false;
        }

        for (int i = 0; i < m_arguments.size(); i++) {
            if (!m_arguments.get(i).equals(mm.getArguments().get(i))) {
                return false;
            }
        }

//        for (int i = 0; i < m_exceptions.size(); i++) {
//            if (! mm.getExceptions().contains(m_exceptions.get(i))) { return false; }
//        }

        return true;
    }

    /**
     * Equals method for Method Signature.
     * @param ms : the method signatur to compare.
     * @return true if the given method signature is equals to the current method metadata.
     */
    public boolean equals(MethodSignature ms) {
        // the method is equals to the method signature if the name and the desc are similar.
        if (!m_methodName.equals(ms.getName())) {
            return false;
        }
        if (!m_descriptor.equals(ms.getDesc())) {
            return false;
        }
        
        return true;
    }

    public int getPolicy() {
        return m_policy;
    }

    /**
     * Activate the all policy for this method.
     */
    public void setAllPolicy() {
        m_policy = ALL_POLICY;
    }
    
    public String getDescription() { return m_descriptor; }

}
