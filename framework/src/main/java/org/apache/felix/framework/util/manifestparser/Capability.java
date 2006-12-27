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
package org.apache.felix.framework.util.manifestparser;

import java.util.*;
import org.apache.felix.moduleloader.ICapability;

public class Capability implements ICapability
{
    private String m_namespace = null;
    private R4Directive[] m_dirs = null;
    private R4Attribute[] m_attrs = null;
    private Map m_attrMap = null;

    public Capability(String namespace, R4Directive[] dirs, R4Attribute[] attrs)
    {
        m_namespace = namespace;
        m_dirs = dirs;
        m_attrs = attrs;
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public R4Directive[] getDirectives()
    {
        return m_dirs;
    }

    public Map getProperties()
    {
        if (m_attrMap == null)
        {
            m_attrMap = new Map() {

                public int size()
                {
                    return m_attrs.length;
                }

                public boolean isEmpty()
                {
                    return false;
                }

                public boolean containsKey(Object key)
                {
                    return (get(key) != null);
                }

                public boolean containsValue(Object value)
                {
                    for (int i = 0; i < m_attrs.length; i++)
                    {
                        if (m_attrs[i].getValue().equals(value))
                        {
                            return true;
                        }
                    }
                    return false;
                }

                public Object get(Object key)
                {
                    for (int i = 0; i < m_attrs.length; i++)
                    {
                        if (m_attrs[i].getName().equals(key))
                        {
                            return m_attrs[i].getValue();
                        }
                    }
                    return null;
                }

                public Object put(Object key, Object value)
                {
                    throw new UnsupportedOperationException("Map.put() not implemented.");
                }

                public Object remove(Object key)
                {
                    throw new UnsupportedOperationException("Map.remove() not implemented.");
                }

                public void putAll(Map t)
                {
                    throw new UnsupportedOperationException("Map.putAll() not implemented.");
                }

                public void clear()
                {
                    throw new UnsupportedOperationException("Map.clear() not implemented.");
                }

                public Set keySet()
                {
                    Set set = new HashSet();
                    for (int i = 0; i < m_attrs.length; i++)
                    {
                        set.add(m_attrs[i].getName());
                    }
                    return set;
                }

                public Collection values()
                {
                    throw new java.lang.UnsupportedOperationException("Map.values() not implemented.");
                }

                public Set entrySet()
                {
                    throw new java.lang.UnsupportedOperationException("Map.entrySet() not implemented.");
                }
            };
        }
        return m_attrMap;
    }

// TODO: RB - Remove or simplify toString() for final version.
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(m_namespace);
        for (int i = 0; (m_dirs != null) && (i < m_dirs.length); i++)
        {
            sb.append(";");
            sb.append(m_dirs[i].getName());
            sb.append(":=");
            sb.append(m_dirs[i].getValue());
        }
        for (int i = 0; (m_attrs != null) && (i < m_attrs.length); i++)
        {
            sb.append(";");
            sb.append(m_attrs[i].getName());
            sb.append("=");
            sb.append(m_attrs[i].getValue());
        }
        return sb.toString();
    }
}