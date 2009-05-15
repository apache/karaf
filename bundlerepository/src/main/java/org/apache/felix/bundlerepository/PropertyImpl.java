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
package org.apache.felix.bundlerepository;

import java.net.MalformedURLException;
import java.net.URL;

import org.osgi.framework.Version;
import org.osgi.service.obr.Resource;

public class PropertyImpl
{
    private String m_name = null;
    private String m_type = null;
    private Object m_value = null;

    public PropertyImpl()
    {
    }

    public PropertyImpl(String name, String type, String value)
    {
        setN(name);
        setT(type);
        setV(value);
    }

    public void setN(String name)
    {
        m_name = name;
    }

    public String getN()
    {
        return m_name;
    }

    public void setT(String type)
    {
        m_type = type;

        // If there is an existing value, then convert
        // it based on the new type.
        if (m_value != null)
        {
            m_value = convertType(m_value.toString());
        }
    }

    public String getT()
    {
        return m_type;
    }

    public void setV(String value)
    {
        m_value = convertType(value);
    }

    public Object getV()
    {
        return m_value;
    }

    private Object convertType(String value)
    {
        if ((m_type != null) && m_type.equalsIgnoreCase(Resource.VERSION))
        {
            return new Version(value);
        }
        else if ((m_type != null) && (m_type.equalsIgnoreCase(Resource.URL)))
        {
            try
            {
                return new URL(value);
            }
            catch (MalformedURLException ex)
            {
                ex.printStackTrace();
            }
        }
        return value;
    }
}