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

import org.apache.felix.framework.FilterImpl;
import org.apache.felix.framework.util.MapToDictionary;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IRequirement;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class Requirement implements IRequirement
{
    private String m_namespace = null;
    private Filter m_filter = null;

    public Requirement(String namespace, String filterStr) throws InvalidSyntaxException
    {
        m_namespace = namespace;
        m_filter = new FilterImpl(filterStr);
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public Filter getFilter()
    {
        return m_filter;
    }

    public boolean isMultiple()
    {
        return false;
    }

    public boolean isOptional()
    {
        return false;
    }

    public String getComment()
    {
        return null;
    }

    public boolean isSatisfied(ICapability capability)
    {
        return m_filter.match(new MapToDictionary(capability.getProperties()));
    }

    public String toString()
    {
        return m_filter.toString();
    }
}