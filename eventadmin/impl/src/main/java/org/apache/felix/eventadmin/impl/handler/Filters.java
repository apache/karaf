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

import java.util.Dictionary;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * The factory for <tt>Filter</tt> objects. Additionally, two null filter objects
 * are provided that either always return <tt>true</tt> or <tt>false</tt>,
 * respectively.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Filters
{
    /**
     * A null filter object that matches any given service reference.
     */
    public static final Filter TRUE_FILTER = new Filter()
    {

        /**
         * This is a null object that always returns <tt>true</tt>.
         * 
         * @param reference An unused service reference
         * @return <tt>true</tt>
         */
        public boolean match(final ServiceReference reference)
        {
            return true;
        }

        /**
         * This is a null object that always returns <tt>true</tt>.
         * 
         * @param dictionary An unused dictionary
         * @return <tt>true</tt>
         */
        public boolean match(final Dictionary dictionary)
        {
            return true;
        }

        /**
         * This is a null object that always returns <tt>true</tt>.
         * 
         * @param dictionary An unused dictionary.
         * @return <tt>true</tt>
         */
        public boolean matchCase(final Dictionary dictionary)
        {
            return true;
        }
    };

    /**
     * A null filter object that does not match any given service reference. 
     */
    public static final Filter FALSE_FILTER = new Filter()
    {

        /**
         * This is a null object that always returns <tt>false</tt>.
         * 
         * @param reference An unused reference.
         * @return <tt>false</tt>
         */
        public boolean match(final ServiceReference reference)
        {
           return false;
        }

        /**
         * This is a null object that always returns <tt>false</tt>.
         * 
         * @param dictionary An unused dictionary
         * @return <tt>false</tt>
         */
        public boolean match(final Dictionary dictionary)
        {
            return false;
        }

        /**
         * This is a null object that always returns <tt>false</tt>.
         * 
         * @param dictionary An unused dictionary.
         * @return <tt>false</tt>
         */
        public boolean matchCase(final Dictionary dictionary)
        {
            return false;
        }
    };

    /**
     * Create a filter for the given filter string or return the nullFilter in case
     * the string is <tt>null</tt>.
     * 
     * @param filter The filter as a string
     * @param nullFilter The default value to return if filter is <tt>null</tt>
     * @return The <tt>Filter</tt> of the filter string or the nullFilter if the 
     *      filter string was null
     * @throws InvalidSyntaxException if <tt>BundleContext.createFilter()</tt>
     *      throws an <tt>InvalidSyntaxException</tt>
     */
    public Filter createFilter(final String filter, final Filter nullFilter)
        throws InvalidSyntaxException;
}
