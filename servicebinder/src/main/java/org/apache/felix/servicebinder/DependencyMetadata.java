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
package org.apache.felix.servicebinder;

/**
 * Metadata of a dependency
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyMetadata
{
    private String m_serviceName = "";
    private String m_filter = "";
    private String m_bindMethod = "";
    private String m_unbindMethod = "";
    private String m_cardinality = "";
    private String m_policy = "";

    private boolean m_isStatic = true;
    private boolean m_isOptional = false;
    private boolean m_isMultiple = false;

    /**
     * Constructor
     *
    **/
    DependencyMetadata(String servicename,String cardinality,String policy,String filter,String bindmethod,String unbindmethod)
    {
        m_serviceName = servicename;

        String classnamefilter = "(objectClass="+servicename+")";

        if(filter.equals("") == false)
        {
            m_filter = "(&"+classnamefilter+filter+")";
        }
        else
        {
            m_filter = classnamefilter;
        }

        m_bindMethod = bindmethod;
        m_unbindMethod = unbindmethod;
        m_cardinality = cardinality;
        m_policy = policy;

        if(policy.equals("static") == false)
        {
            m_isStatic = false;
        }

        if(cardinality.equals("0..1") || cardinality.equals("0..n"))
        {
            m_isOptional = true;
        }

        if(cardinality.equals("0..n") || cardinality.equals("1..n"))
        {
            m_isMultiple = true;
        }
    }

    /**
     * Returns the name of the required service
     *
     * @return the name of the required service
    **/
    public String getServiceName()
    {
        return m_serviceName;
    }

    /**
     * Returns the filter
     *
     * @return A string with the filter
    **/
    public String getFilter()
    {
        return m_filter;
    }

    /**
     * Get the name of the Bind method
     *
     * @return a String with the name of the BindMethod
    **/
    public String getBindMethodName()
    {
        return m_bindMethod;
    }

    /**
     * Get the name of the Unbind method
     *
     * @return a String with the name of the Unbind method
    **/
    public String getUnbindMethodName()
    {
        return m_unbindMethod;
    }


    /**
     * Test if dependency's binding policy is static
     *
     * @return true if static
    **/
    public boolean isStatic()
    {
        return m_isStatic;
    }

    /**
     * Test if dependency is optional (0..1 or 0..n)
     *
     * @return true if the dependency is optional
    **/
    public boolean isOptional()
    {
        return m_isOptional;
    }

    /**
     * Test if dependency is multiple (0..n or 1..n)
     *
     * @return true if the dependency is multiple
    **/
    public boolean isMultiple()
    {
        return m_isMultiple;
    }

    /**
     * Get the cardinality as a string
     *
     * @return the cardinality
    **/
    public String getCardinality()
    {
        return m_cardinality;
    }

    /**
     * Get the policy as a string
     *
     * @return the policy
    **/
    public String getPolicy()
    {
        return m_policy;
    }
}
