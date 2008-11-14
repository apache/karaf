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
package org.apache.felix.ipojo.context;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple utility class that creates a map for string-based keys by extending
 * <tt>TreeMap</tt>. This map can be set to use case-sensitive or
 * case-insensitive comparison when searching for the key. Any keys put into
 * this map will be converted to a <tt>String</tt> using the
 * <tt>toString()</tt> method, since it is only intended to compare strings.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class StringMap extends TreeMap {

    /**
     * serialVersionUID.
     */
    private static final long serialVersionUID = 6948801857034259744L;

    /**
     * Constructor.
     */
    public StringMap() {
        this(true);
    }

    /**
     * Constructor.
     * 
     * @param caseSensitive : fix if the map if case sensitive or not.
     */
    public StringMap(boolean caseSensitive) {
        super(new StringComparator(caseSensitive));
    }

    /**
     * Constructor.
     * 
     * @param map : initial properties.
     * @param caseSensitive : fix if the map if case sensitive or not.
     */
    public StringMap(Map map, boolean caseSensitive) {
        this(caseSensitive);
        putAll(map);
    }

    /**
     * Put a record in the map.
     * @param key : key
     * @param value : value
     * @return an object.
     * @see java.util.TreeMap#put(K, V)
     */
    public Object put(Object key, Object value) {
        return super.put(key.toString(), value);
    }

    /**
     * Check if the map is case-sensitive.
     * @return true if the map is case sensitive.
     */
    public boolean isCaseSensitive() {
        return ((StringComparator) comparator()).isCaseSensitive();
    }

    /**
     * Set the case sensitivity.
     * 
     * @param flag : the new case sensitivity.
     */
    public void setCaseSensitive(boolean flag) {
        ((StringComparator) comparator()).setCaseSensitive(flag);
    }

    private static class StringComparator implements Comparator, Serializable {
        /**
         * Id.
         */
        private static final long serialVersionUID = 1L;
        
        /**
         * Is the map case sensitive?
         */
        private boolean m_isCaseSensitive = true;

        /**
         * Constructor.
         * 
         * @param flag : true to enable the case sensitivity.
         */
        public StringComparator(boolean flag) {
            m_isCaseSensitive = flag;
        }

        /**
         * Compare to object.
         * @param object1 : first object to compare
         * @param object2 : second object to compare
         * @return the comparison result
         * @see java.util.Comparator#compare(T, T)
         */
        public int compare(Object object1, Object object2) {
            if (m_isCaseSensitive) {
                return object1.toString().compareTo(object2.toString());
            } else {
                return object1.toString().compareToIgnoreCase(object2.toString());
            }
        }

        /**
         * Check if the comparator is case sensitive.
         * @return true if the map is case sensitive.
         */
        public boolean isCaseSensitive() {
            return m_isCaseSensitive;
        }

        /**
         * Set the case sensitivity.
         * 
         * @param flag : true to enable the case sensitivity
         */
        public void setCaseSensitive(boolean flag) {
            m_isCaseSensitive = flag;
        }
    }
}
