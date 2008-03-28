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

/**
 * Information on Method for the composition.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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
     * Method Object.
     */
    private Method m_method;
    
    /**
     * Delegation field.
     */
    private FieldMetadata m_delegation;

    /**
     * Delegation policy (default = ONE).
     */
    private int m_policy = ONE_POLICY;

    /**
     * Constructor.
     * @param method : method object.
     */
    public MethodMetadata(Method method) {
        m_method = method;
    }

    public Method getMethod() {
        return m_method;
    }

    public void setDelegation(FieldMetadata field) {
        m_delegation = field;
    }

    public FieldMetadata getDelegation() {
        return m_delegation;
    }
    
    /**
     * Equals method.
     * This method check if two MethodMetadata are equals or if the current MemethodMetadata is equals with a Method object. 
     * @param object : object.
     * @return true if the current object and the given object are equals.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object object) {
        if (object instanceof MethodMetadata) {
            Method met = ((MethodMetadata) object).getMethod();
            return equals(met);
        }
        
        if (object instanceof Method) {
            Method met = (Method) object;
            if (! met.getName().equals(m_method.getName()) || met.getParameterTypes().length != m_method.getParameterTypes().length) {
                return false;
            }

            for (int i = 0; i < m_method.getParameterTypes().length; i++) {
                if (!m_method.getParameterTypes()[i].getName().equals(met.getParameterTypes()[i].getName())) {
                    return false;
                }
            }

            return true;
        }
        
        return false;
    }
    
    /**
     * Hash code method.
     * @return the parent hash code.
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return super.hashCode();
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
    
    /**
     * Check if the method can throw UnsupportedOperationException.
     * @return true if the method has declared the UnsupportedOperationException.
     */
    protected boolean throwsUnsupportedOperationException() {
        for (int i = 0; i < m_method.getExceptionTypes().length; i++) {
            if (m_method.getExceptionTypes()[i].getName().equals(UnsupportedOperationException.class.getName())) {
                return true;
            }
        }
        return false;
    }

}
