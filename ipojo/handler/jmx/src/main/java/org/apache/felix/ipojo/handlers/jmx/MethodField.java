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
 * this class build a method JMX description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class MethodField {

    /**
     * m_description : store the method descritpion.
     */
    private String m_description;
    /**
     * m_method : store the method properties.
     */
    private MethodMetadata m_method;

    /**
     * MethodField : constructor.
     * 
     * @param method
     *            : the metod properties
     * @param description
     *            : thes method description
     */
    public MethodField(MethodMetadata method, String description) {
        this.m_method = method;
        this.m_description = description;

    }

    public MethodMetadata getMethod() {
        return m_method;
    }

    public String getDescription() {
        return m_description;
    }

    public String getName() {
        return m_method.getMethodName();
    }

    /**
     * getParams : get the parameter in JMX format.
     * 
     * @return MBeanParameterInfo : return info on JMX format
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
