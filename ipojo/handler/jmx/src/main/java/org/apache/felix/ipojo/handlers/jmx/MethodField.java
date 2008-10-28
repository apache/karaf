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

package org.apache.felix.ipojo.handlers.jmx;

import javax.management.MBeanParameterInfo;

import org.apache.felix.ipojo.parser.MethodMetadata;

/**
 * This class builds a method JMX description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodField {

    /**
     * Stores the method description.
     */
    private String m_description;
    /**
     * Stores the method properties.
     */
    private MethodMetadata m_method;

    /**
     * Constructor.
     * 
     * @param method the method properties
     * @param description the method description
     */
    public MethodField(MethodMetadata method, String description) {
        this.m_method = method;
        this.m_description = description;

    }

    /**
     * Gets the method.
     * @return the method
     */
    public MethodMetadata getMethod() {
        return m_method;
    }

    /**
     * Gets the description.
     * @return the description
     */
    public String getDescription() {
        return m_description;
    }

    /**
     * Gets the name.
     * @return the name
     */
    public String getName() {
        return m_method.getMethodName();
    }

    /**
     * Gets the parameter in JMX format.
     * 
     * @return info on JMX format
     */
    public MBeanParameterInfo[] getParams() {
        MBeanParameterInfo[] mbean = new MBeanParameterInfo[m_method
            .getMethodArguments().length];
        for (int i = 0; i < m_method.getMethodArguments().length; i++) {
            mbean[i] = new MBeanParameterInfo("arg" + i, m_method
                .getMethodArguments()[i], null);
        }
        return mbean;
    }

    public String[] getSignature() {
        return m_method.getMethodArguments();
    }

    public String getReturnType() {
        return m_method.getMethodReturn();
    }

}
