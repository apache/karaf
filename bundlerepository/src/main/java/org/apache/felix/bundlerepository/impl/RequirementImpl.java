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

import java.util.regex.Pattern;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.utils.filter.FilterImpl;
import org.osgi.framework.InvalidSyntaxException;

public class RequirementImpl implements Requirement
{
    private static final Pattern REMOVE_LT = Pattern.compile("\\(([^<>=~()]*)<([^*=]([^\\\\\\*\\(\\)]|\\\\|\\*|\\(|\\))*)\\)");
    private static final Pattern REMOVE_GT = Pattern.compile("\\(([^<>=~()]*)>([^*=]([^\\\\\\*\\(\\)]|\\\\|\\*|\\(|\\))*)\\)");
    private static final Pattern REMOVE_NV = Pattern.compile("\\(version>=0.0.0\\)");

    private String m_name = null;
    private boolean m_extend = false;
    private boolean m_multiple = false;
    private boolean m_optional = false;
    private FilterImpl m_filter = null;
    private String m_comment = null;

    public RequirementImpl()
    {
    }

    public RequirementImpl(String name) 
    {
        setName(name);
    }

    public String getName()
    {
        return m_name;
    }

    public void setName(String name)
    {
        // Name of capabilities and requirements are interned for performances
        // (with a very low memory consumption as there are only a handful of values)
        m_name = name.intern();
    }

    public String getFilter()
    {
        return m_filter.toString();
    }

    public void setFilter(String filter)
    {
        try
        {
            String nf = REMOVE_LT.matcher(filter).replaceAll("(!($1>=$2))");
            nf = REMOVE_GT.matcher(nf).replaceAll("(!($1<=$2))");
            nf = REMOVE_NV.matcher(nf).replaceAll("");
            m_filter = FilterImpl.newInstance(nf, true);
        }
        catch (InvalidSyntaxException e)
        {
            IllegalArgumentException ex = new IllegalArgumentException();
            ex.initCause(e);
            throw ex;
        }
    }

    public boolean isSatisfied(Capability capability)
    {
        return m_name.equals(capability.getName()) && m_filter.matchCase(capability.getPropertiesAsMap())
                && (m_filter.toString().indexOf("(mandatory:<*") >= 0 || capability.getPropertiesAsMap().get("mandatory:") == null);
    }

    public boolean isExtend()
    {
        return m_extend;
    }

    public void setExtend(boolean extend)
    {
        m_extend = extend;
    }

    public boolean isMultiple()
    {
        return m_multiple;
    }

    public void setMultiple(boolean multiple)
    {
        m_multiple = multiple;
    }

    public boolean isOptional()
    {
        return m_optional;
    }

    public void setOptional(boolean optional)
    {
        m_optional = optional;
    }

    public String getComment()
    {
        return m_comment;
    }

    public void addText(String s)
    {
        m_comment = s;
    }

    public boolean equals(Object o)
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

    public int hashCode()
    {
        return m_filter.toString().hashCode();
    }

    public String toString()
    {
        return m_name + ":" + getFilter();
    }
}