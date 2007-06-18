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
package org.apache.felix.eventadmin.impl.util;

/**
 * This is the interface of a simple cache map. 
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface CacheMap
{
    /**
     * Return the value for the key in case there is one in the cache.
     * 
     * @param key The key to look-up
     * 
     * @return The value for the given key in case there is one in the cache, 
     *      <tt>null</tt> otherwise
     */
    public Object get(final Object key);
    
    /**
     * Add a value for the key to this cache.
     * 
     * @param key The key for the value
     * @param value The value to add to the cache 
     */
    public void add(final Object key, final Object value);
    
    /**
     * Remove a key and its value from the cache.
     * 
     * @param key The key to remove
     * 
     * @return The value of the key in case there is one in the cache, <tt>null</tt>
     *      otherwise
     */
    public Object remove(final Object key);
    
    /**
     * Returns the number of key-value pairs in this cache.
     * 
     * @return The number of key-value pairs in this cache
     */
    public int size();
    
    /**
     * Remove all entries of the cache.
     */
    public void clear();
}
