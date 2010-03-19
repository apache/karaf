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
package org.apache.felix.bundlerepository.impl;

import java.util.*;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Property;

public class CapabilityImpl implements Capability
{
    private String m_name = null;
    private final Map m_map = new HashMap();
    private final List m_list = new ArrayList();

    public CapabilityImpl()
    {
    }

    public CapabilityImpl(String name)
    {
        setName(name);
    }

    public CapabilityImpl(String name, PropertyImpl[] properties)
    {
        setName(name);
        for (int i = 0; properties != null && i < properties.length; i++)
        {
            addProperty(properties[i]);
        }
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name.intern();
    }

    public Map getPropertiesAsMap()
    {
        return m_map;
    }

    public Property[] getProperties()
    {
        return (Property[]) m_list.toArray(new Property[m_list.size()]);
    }

    public void addProperty(Property prop)
    {
        m_map.put(prop.getName().toLowerCase(), prop.getConvertedValue());
        m_list.add(prop);
    }

    public void addProperty(String name, String value)
    {
        addProperty(name, null, value);
    }

    public void addProperty(String name, String type, String value)
    {
        addProperty(new PropertyImpl(name, type, value));
    }

    public String toString()
    {
        return m_name  + ":" + m_map.toString();
    }
}