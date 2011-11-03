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
package org.apache.karaf.main.util;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys by
 * extending <tt>TreeMap</tt>. This map can be set to use case-sensitive
 * or case-insensitive comparison when searching for the key.
 * Any keys put into this map will be converted to
 * a <tt>String</tt> using the <tt>toString()</tt> method,
 * since it is only intended to compare strings.
**/
public class StringMap extends TreeMap<String, String>
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public StringMap()
    {
        this(true);
    }

    public StringMap(boolean caseSensitive)
    {
        super(new StringComparator(caseSensitive));
    }

    @SuppressWarnings("unchecked")
	public StringMap(Map<?, ?> map, boolean caseSensitive)
    {
        this(caseSensitive);
        putAll((Map<? extends String, ? extends String>) map);
    }

    public String put(String key, String value)
    {
        return super.put(key.toString(), value);
    }

    public boolean isCaseSensitive()
    {
        return ((StringComparator) comparator()).isCaseSensitive();
    }

    public void setCaseSensitive(boolean b)
    {
        ((StringComparator) comparator()).setCaseSensitive(b);
    }

    private static class StringComparator implements Comparator<String>
    {
        private boolean m_isCaseSensitive = true;

        public StringComparator(boolean b)
        {
            m_isCaseSensitive = b;
        }

        public int compare(String o1, String o2)
        {
            if (m_isCaseSensitive)
            {
                return o1.toString().compareTo(o2.toString());
            }
            else
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        }

        public boolean isCaseSensitive()
        {
            return m_isCaseSensitive;
        }

        public void setCaseSensitive(boolean b)
        {
            m_isCaseSensitive = b;
        }
    }
}
