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
package org.apache.felix.bundlerepository.impl;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.osgi.framework.InvalidSyntaxException;

public class RequirementImpl implements Requirement
{
    private String m_name = null;
    private boolean m_extend = false;
    private boolean m_multiple = false;
    private boolean m_optional = false;
    private FilterImpl m_filter = null;
    private String m_comment = null;

    public RequirementImpl()
    {
    }

    public synchronized String getName()
    {
        return m_name;
    }

    public synchronized void setName(String name)
    {
        // Name of capabilities and requirements are interned for performances
        // (with a very slow inter consumption as there are only a handful of values)
        m_name = name.intern();
    }

    public synchronized String getFilter()
    {
        return m_filter.toString();
    }

    public synchronized void setFilter(String filter) throws InvalidSyntaxException
    {
        m_filter = FilterImpl.newInstance(filter, true);
    }

    public synchronized boolean isSatisfied(Capability capability)
    {
        return m_name.equals(capability.getName()) && m_filter.matchCase(capability.getProperties());
    }

    public synchronized boolean isExtend()
    {
        return m_extend;
    }

    public synchronized void setExtend(String s)
    {
        m_extend = Boolean.valueOf(s).booleanValue();
    }

    public synchronized boolean isMultiple()
    {
        return m_multiple;
    }

    public synchronized void setMultiple(String s)
    {
        m_multiple = Boolean.valueOf(s).booleanValue();
    }

    public synchronized boolean isOptional()
    {
        return m_optional;
    }

    public synchronized void setOptional(String s)
    {
        m_optional = Boolean.valueOf(s).booleanValue();
    }

    public synchronized String getComment()
    {
        return m_comment;
    }

    public synchronized void addText(String s)
    {
        m_comment = s;
    }

    public synchronized boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o instanceof Requirement)
        {
            Requirement r = (Requirement) o;
            return m_name.equals(r.getName()) &&
                (m_optional == r.isOptional()) &&
                (m_multiple == r.isMultiple()) &&
                m_filter.toString().equals(r.getFilter()) &&
                ((m_comment == r.getComment()) ||
                    ((m_comment != null) && (m_comment.equals(r.getComment()))));
        }
        return false;
    }

    public synchronized int hashCode()
    {
        return m_filter.toString().hashCode();
    }

    public synchronized String toString()
    {
        return m_name + ":" + getFilter();
    }
}