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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class implements a least recently used cache map. It will hold 
 * a given size of key-value pairs and drop the least recently used entry once this
 * size is reached. This class is thread safe.
 * 
 * @see org.apache.felix.eventadmin.impl.util.CacheMap
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class LeastRecentlyUsedCacheMap implements CacheMap
{
    // The internal lock for this object used instead synchronized(this)
    private final Object m_lock = new Object();
    
    // The max number of entries in the cache. Once reached entries are replaced
    private final int m_maxSize;
    
    // The cache 
    private final Map m_cache;
    
    // The history used to determine the least recently used entries. The end of the
    // list is the most recently used key. In other words m_history.get(0) returns
    // the least recently used key.
    private final List m_history;
    
    /**
     * The constructor of the cache. The given max size will be used to determine the
     * size of the cache that triggers replacing least recently used entries with 
     * new ones.
     * 
     * @param maxSize The max number of entries in the cache
     */
    public LeastRecentlyUsedCacheMap(final int maxSize)
    {
        if(0 >= maxSize)
        {
            throw new IllegalArgumentException("Size must be positive");
        }
        
        m_maxSize = maxSize;
        
        // We need one more entry then m_maxSize in the cache and a HashMap is
        // expanded when it reaches 3/4 of its size hence, the funny numbers.
        m_cache = new HashMap(m_maxSize + 1 + ((m_maxSize + 1) * 3) / 4);
        
        // This is like above but assumes a list is expanded when it reaches 1/2 of
        // its size. Not much harm if this is not the case. 
        m_history  = new ArrayList(m_maxSize + 1 + ((m_maxSize + 1) / 2));
    }
    
    /**
     * Returns the value for the key in case there is one. Additionally, the 
     * LRU counter for the key is updated.
     * 
     * @param key The key for the value to return
     * 
     * @return The value of the key in case there is one, <tt>null</tt> otherwise
     * 
     * @see org.apache.felix.eventadmin.impl.util.CacheMap#get(java.lang.Object)
     */
    public Object get(final Object key)
    {
        synchronized(m_lock)
        {
            final Object result = m_cache.get(key);
            
            if(null != result)
            {
                m_history.remove(key);
                
                m_history.add(key);
            }
            
            return result;
        }
    }

    /**
     * Add the key-value pair to the cache. The key will be come the most recently
     * used entry. In case max size is (or has been) reached this will remove the
     * least recently used entry in the cache. In case that the cache already 
     * contains this specific key-value pair it LRU counter is updated only.
     * 
     * @param key The key for the value
     * @param value The value for the key
     * 
     * @see org.apache.felix.eventadmin.impl.util.CacheMap#add(java.lang.Object, java.lang.Object)
     */
    public void add(final Object key, final Object value)
    {
        synchronized(m_lock)
        {
            final Object result = m_cache.put(key, value);
            
            if(null != result)
            {
                m_history.remove(key);
            }
            
            m_history.add(key);
            
            if(m_maxSize < m_cache.size())
            {
                m_cache.remove(m_history.remove(0));
            }
        }
    }

    /**
     * Remove the entry denoted by key from the cache and return its value.
     * 
     * @param key The key of the entry to be removed
     * 
     * @return The value of the entry removed, <tt>null</tt> if none
     * 
     * @see org.apache.felix.eventadmin.impl.util.CacheMap#remove(java.lang.Object)
     */
    public Object remove(final Object key)
    {
        synchronized(m_lock)
        {
            final Object result = m_cache.remove(key);
            
            if(null != result)
            {
                m_history.remove(key);
            }
            
            return result;
        }
    }

    /**
     * Return the current size of the cache.
     * 
     * @return The number of entries currently in the cache.
     * 
     * @see org.apache.felix.eventadmin.impl.util.CacheMap#size()
     */
    public int size()
    {
        synchronized (m_lock)
        {
            return m_cache.size();
        }
    }

    /**
     * Remove all entries from the cache.
     * 
     * @see org.apache.felix.eventadmin.impl.util.CacheMap#clear()
     */
    public void clear()
    {
        synchronized (m_lock)
        {
            m_cache.clear();
            
            m_history.clear();
        }
    }

}
