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

public class CapabilityImpl implements Capability
{
    private ResourceImpl m_resource;
    private String m_name = null;
    private final Map m_map = new HashMap();

    public CapabilityImpl()
    {
    }

    public ResourceImpl getResource()
    {
        return m_resource;
    }

    public void setResource(ResourceImpl resource)
    {
        m_resource = resource;
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name.intern();
    }

    public Map getProperties()
    {
        return m_map;
    }

    protected void addP(PropertyImpl prop)
    {
        addP(prop.getN(), prop.getV());
    }

    protected void addP(String name, Object value)
    {
        m_map.put(name.toLowerCase(), value);
    }

    public String toString()
    {
        return m_name  + ":" + m_map.toString();
    }
}