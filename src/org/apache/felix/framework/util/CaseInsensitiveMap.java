/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.util;

import java.util.*;

/**
 * Simple utility class that creates a case-insensitive map by
 * extending <tt>TreeMap</tt> and to use a case-insensitive
 * comparator. Any keys put into this map will be converted to
 * a <tt>String</tt> using the <tt>toString()</tt> method,
 * since it is intended to compare strings.
**/
public class CaseInsensitiveMap extends TreeMap
{
    public CaseInsensitiveMap()
    {
        super(new Comparator() {
            public int compare(Object o1, Object o2)
            {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }
        });
    }
    
    public CaseInsensitiveMap(Map map)
    {
        this();
        putAll(map);
    }
    
    public Object put(Object key, Object value)
    {
        return super.put(key.toString(), value);
    }
}