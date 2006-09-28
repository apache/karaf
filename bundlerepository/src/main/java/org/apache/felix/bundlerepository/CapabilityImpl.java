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

import java.util.*;

import org.osgi.service.obr.Capability;

public class CapabilityImpl implements Capability
{
    private String m_name = null;
    private Map m_map = null;

    public CapabilityImpl()
    {
        m_map = new TreeMap(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        m_name = name;
    }

    public Map getProperties()
    {
        return m_map;
    }

    protected void addP(PropertyImpl prop)
    {
        m_map.put(prop.getN(), prop.getV());
    }
}