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
package org.apache.felix.framework.util;

import java.util.*;

/**
 * Simple utility class that creates a map for string-based keys.
 * This map can be set to use case-sensitive or case-insensitive
 * comparison when searching for the key.  Any keys put into this
 * map will be converted to a <tt>String</tt> using the
 * <tt>toString()</tt> method, since it is only intended to
 * compare strings.
**/
public class StringMap implements Map
{
    private TreeMap m_map;

    public StringMap()
    {
        this(true);
    }

    public StringMap(boolean caseSensitive)
    {
        m_map = new TreeMap(new StringComparator(caseSensitive));
    }

    public StringMap(Map map, boolean caseSensitive)
    {
        this(caseSensitive);
        putAll(map);
    }

    public boolean isCaseSensitive()
    {
        return ((StringComparator) m_map.comparator()).isCaseSensitive();
    }

    public void setCaseSensitive(boolean b)
    {
        if (isCaseSensitive() != b)
        {
            TreeMap map = new TreeMap(new StringComparator(b));
            map.putAll(m_map);
            m_map = map;
        }
    }

    public int size()
    {
        return m_map.size();
    }

    public boolean isEmpty()
    {
        return m_map.isEmpty();
    }

    public boolean containsKey(Object arg0)
    {
        return m_map.containsKey(arg0);
    }

    public boolean containsValue(Object arg0)
    {
        return m_map.containsValue(arg0);
    }

    public Object get(Object arg0)
    {
        return m_map.get(arg0);
    }

    public Object put(Object key, Object value)
    {
        return m_map.put(key.toString(), value);
    }

    public void putAll(Map map)
    {
        for (Iterator it = map.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry) it.next();
            put(entry.getKey(), entry.getValue());
        }
    }

    public Object remove(Object arg0)
    {
        return m_map.remove(arg0);
    }

    public void clear()
    {
        m_map.clear();
    }

    public Set keySet()
    {
        return m_map.keySet();
    }

    public Collection values()
    {
        return m_map.values();
    }

    public Set entrySet()
    {
        return m_map.entrySet();
    }

    public String toString()
    {
        return m_map.toString();
    }
}