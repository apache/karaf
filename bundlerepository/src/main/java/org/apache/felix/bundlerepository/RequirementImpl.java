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
package org.apache.felix.bundlerepository;

import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Requirement;

public class RequirementImpl implements Requirement
{
    private String m_name = null;
    private boolean m_extend = false;
    private boolean m_multiple = false;
    private boolean m_optional = false;
    private Filter m_filter = null;
    private String m_comment = null;
    private MapToDictionary m_dict = new MapToDictionary(null);

    public RequirementImpl()
    {
    }

    public String getName()
    {
        return m_name;
    }

    public synchronized void setName(String name)
    {
        m_name = name;
    }

    public String getFilter()
    {
        return m_filter.toString();
    }

    public synchronized void setFilter(String filter)
    {
        try
        {
            m_filter = RepositoryAdminImpl.m_context.createFilter(filter);
        }
        catch (InvalidSyntaxException ex)
        {
            m_filter = null;
            System.err.println(ex);
        }
    }

    public synchronized boolean isSatisfied(Capability capability)
    {
        m_dict.setSourceMap(capability.getProperties());
        return m_filter.match(m_dict);
    }

    public boolean isExtend()
    {
        return m_extend;
    }

    public synchronized void setExtend(String s)
    {
        m_extend = Boolean.valueOf(s).booleanValue();
    }

    public boolean isMultiple()
    {
        return m_multiple;
    }

    public synchronized void setMultiple(String s)
    {
        m_multiple = Boolean.valueOf(s).booleanValue();
    }

    public boolean isOptional()
    {
        return m_optional;
    }

    public synchronized void setOptional(String s)
    {
        m_optional = Boolean.valueOf(s).booleanValue();
    }

    public String getComment()
    {
        return m_comment;
    }

    public synchronized void addText(String s)
    {
        m_comment = s;
    }

    public synchronized boolean equals(Object o)
    {
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

    public int hashCode()
    {
        return m_filter.toString().hashCode();
    }
}