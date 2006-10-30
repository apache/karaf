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
package org.apache.felix.ipojo.handlers.dependency.nullable;

/**
 * Reprensent of method description.
 * The goal of this class, is to be able to generate a proxy class, or a nullable class from an interface.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class MethodSignature {

    /**
     * Nmae of the method.
     */
    private String m_name;

    /**
     * Descriptor of the method.
     */
    private String m_desc;

    /**
     * Signature of the method.
     */
    private String m_signature;

    /**
     * Exception thored by the method.
     */
    private String[] m_exception;

    /**
     * MethodSignature constructor.
     * Describe a method.
     * @param name : name of the method
     * @param desc : descriptor of the method
     * @param sign : signature of the method
     * @param exc : exception throwed by the method
     */
    public MethodSignature(String name, String desc, String sign, String[] exc) {
        m_name = name;
        m_desc = desc;
        m_signature = sign;
        m_exception = exc;
    }

    /**
     * @return the description of the method.
     */
    public String getDesc() {
        return m_desc;
    }

    /**
     * @return the String array of exception throwed by the method.
     */
    public String[] getException() {
        return m_exception;
    }

    /**
     * @return the name of the method.
     */
    public String getName() {
        return m_name;
    }

    /**
     * @return the signature of the method.
     */
    public String getSignature() {
        return m_signature;
    }
}
