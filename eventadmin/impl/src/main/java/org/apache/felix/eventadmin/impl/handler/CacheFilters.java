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
package org.apache.felix.eventadmin.impl.handler;

import org.apache.felix.eventadmin.impl.util.CacheMap;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This is an implementation of the <tt>Filters</tt> factory that uses a cache in
 * order to speed-up filter creation.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CacheFilters implements Filters
{
    // The cache to use
    private final CacheMap m_cache;

    // The context of the bundle used to create the Filter objects
    private final BundleContext m_context;

    /**
     * The constructor of this factory. The cache is used to speed-up filter 
     * creation.
     * 
     * @param cache The cache to use
     * @param context The context of the bundle used to create the <tt>Filter</tt>
     *      objects
     */
    public CacheFilters(final CacheMap cache, final BundleContext context)
    {
        if(null == cache)
        {
            throw new NullPointerException("Cache may not be null");
        }
        
        if(null == context)
        {
            throw new NullPointerException("Context may not be null");
        }
        
        m_cache = cache;

        m_context = context;
    }

    /**
     * Create a filter for the given filter string or return the nullFilter in case
     * the string is <tt>null</tt>.
     * 
     * @param filter The filter as a string
     * @param nullFilter The default value to return if filter is <tt>null</tt>
     * @return The <tt>Filter</tt> of the filter string or the nullFilter if the 
     *      filter string was <tt>null</tt>
     * @throws InvalidSyntaxException if <tt>BundleContext.createFilter()</tt>
     *      throws an <tt>InvalidSyntaxException</tt>
     *      
     * @see org.apache.felix.eventadmin.impl.handler.Filters#createFilter(java.lang.String, org.osgi.framework.Filter)
     */
    public Filter createFilter(String filter, Filter nullFilter)
        throws InvalidSyntaxException
    {
        Filter result = (Filter) ((null != filter) ? m_cache.get(filter)
            : nullFilter);
        
        if (null == result)
        {
            result = m_context.createFilter(filter);

            m_cache.add(filter, result);
        }
        
        return result;
    }

}
