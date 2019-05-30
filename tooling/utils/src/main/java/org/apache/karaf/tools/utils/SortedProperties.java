/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tools.utils;

import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

/**
 * Sort properties for better readability.
 */
public class SortedProperties extends Properties {

    private static final long serialVersionUID = 1L;

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(new TreeSet<>(super.keySet()));
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        Set<Map.Entry<Object, Object>> origSet = super.entrySet();
        Set<Map.Entry<Object, Object>> sortedSet = new LinkedHashSet<Map.Entry<Object, Object>>(origSet.size());

        Iterator<Map.Entry<Object, Object>> iterator = origSet.stream().sorted(new Comparator<Map.Entry<Object, Object>>() {

            @Override
            public int compare(java.util.Map.Entry<Object, Object> o1, java.util.Map.Entry<Object, Object> o2) {
                return o1.getKey().toString().compareTo(o2.getKey().toString());
            }
        }).iterator();

        while (iterator.hasNext())
            sortedSet.add(iterator.next());

        return sortedSet;
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(new TreeSet<>(super.keySet()));
    }
    
    
    
}
