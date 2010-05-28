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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.Requirement;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.util.VersionRange;
import org.osgi.framework.Constants;

public class RequirementImpl implements Requirement
{
    private final Module m_module;
    private final String m_namespace;
    private final SimpleFilter m_filter;
    private final boolean m_optional;
    private final List<Directive> m_dirs;
    private final List<Directive> m_dirsConst;

    public RequirementImpl(
        Module module, String namespace,
        List<Directive> dirs, List<Attribute> attrs)
    {
        m_module = module;
        m_namespace = namespace;
        m_dirs = dirs;
        m_dirsConst = Collections.unmodifiableList(m_dirs);
        m_filter = convertToFilter(attrs);

        // Find resolution import directives.
        boolean optional = false;
        for (int dirIdx = 0; dirIdx < m_dirs.size(); dirIdx++)
        {
            if (m_dirs.get(dirIdx).getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                optional = m_dirs.get(dirIdx).getValue().equals(Constants.RESOLUTION_OPTIONAL);
            }
        }
        m_optional = optional;
    }

    public Module getModule()
    {
        return m_module;
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public SimpleFilter getFilter()
    {
        return m_filter;
    }

    public boolean isOptional()
    {
        return m_optional;
    }

    public Directive getDirective(String name)
    {
        for (int i = 0; i < m_dirs.size(); i++)
        {
            if (m_dirs.get(i).getName().equals(name))
            {
                return m_dirs.get(i);
            }
        }
        return null;
    }

    public List<Directive> getDirectives()
    {
        return m_dirsConst;
    }

    public String toString()
    {
        return "[" + m_module + "] " + m_namespace + "; " + getFilter().toString();
    }

    private static SimpleFilter convertToFilter(List<Attribute> attrs)
    {
        // Rather than building a filter string to be parsed into a SimpleFilter,
        // we will just create the parsed SimpleFilter directly.

        List<SimpleFilter> filters = new ArrayList<SimpleFilter>();

        for (Attribute attr : attrs)
        {
            if (attr.getValue() instanceof VersionRange)
            {
                VersionRange vr = (VersionRange) attr.getValue();
                if (vr.isFloorInclusive())
                {
                    filters.add(
                        new SimpleFilter(
                            attr.getName(),
                            vr.getFloor().toString(),
                            SimpleFilter.GTE));
                }
                else
                {
                    SimpleFilter not =
                        new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT);
                    ((List) not.getValue()).add(
                        new SimpleFilter(
                            attr.getName(),
                            vr.getFloor().toString(),
                            SimpleFilter.LTE));
                    filters.add(not);
                }

                if (vr.getCeiling() != null)
                {
                    if (vr.isCeilingInclusive())
                    {
                        filters.add(
                            new SimpleFilter(
                                attr.getName(),
                                vr.getCeiling().toString(),
                                SimpleFilter.LTE));
                    }
                    else
                    {
                        SimpleFilter not =
                            new SimpleFilter(null, new ArrayList(), SimpleFilter.NOT);
                        ((List) not.getValue()).add(
                            new SimpleFilter(
                                attr.getName(),
                                vr.getCeiling().toString(),
                                SimpleFilter.GTE));
                        filters.add(not);
                    }
                }
            }
            else
            {
                List<String> values = SimpleFilter.parseSubstring(attr.getValue().toString());
                if (values.size() > 1)
                {
                    filters.add(
                        new SimpleFilter(
                            attr.getName(),
                            values,
                            SimpleFilter.SUBSTRING));
                }
                else
                {
                    filters.add(
                        new SimpleFilter(
                            attr.getName(),
                            values.get(0),
                            SimpleFilter.EQ));
                }
            }
        }

        SimpleFilter sf = null;

        if (filters.size() == 1)
        {
            sf = filters.get(0);
        }
        else if (attrs.size() > 1)
        {
            sf = new SimpleFilter(null, filters, SimpleFilter.AND);
        }

        return sf;
    }
}