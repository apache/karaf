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
import org.apache.felix.framework.util.VersionRange;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IRequirement;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

public class Requirement implements IRequirement
{
    private String m_namespace = null;
    private R4Directive[] m_dirs = null;
    private R4Attribute[] m_attrs = null;
    private Filter m_filter = null;

    public Requirement(String namespace, R4Directive[] dirs, R4Attribute[] attrs)
    {
        m_namespace = namespace;
        m_dirs = dirs;
        m_attrs = attrs;
        m_filter = convertToFilter();
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
        return getFilter().toString();
    }

    private Filter convertToFilter()
    {
        String filterStr = null;

        StringBuffer sb = new StringBuffer("(&");

        for (int i = 0; (m_attrs != null) && (i < m_attrs.length); i++)
        {
            // If this is a package import, then convert wild-carded
            // dynamically imported package names to an OR comparison.
            if (m_namespace.equals(ICapability.PACKAGE_NAMESPACE) &&
                m_attrs[i].getName().equals(ICapability.PACKAGE_PROPERTY) &&
                m_attrs[i].getValue().toString().endsWith(".*"))
            {
                int idx = m_attrs[i].getValue().toString().indexOf(".*");
                sb.append("(|(package=");
                sb.append(m_attrs[i].getValue().toString().substring(0, idx));
                sb.append(")(package=");
                sb.append(m_attrs[i].getValue().toString());
                sb.append("))");
            }
            else if (m_attrs[i].getValue() instanceof VersionRange)
            {
                VersionRange vr = (VersionRange) m_attrs[i].getValue();
                if (vr.isLowInclusive())
                {
                    sb.append("(version>=");
                    sb.append(vr.getLow().toString());
                    sb.append(")");
                }
                else
                {
                    sb.append("(!(version<=");
                    sb.append(vr.getLow().toString());
                    sb.append("))");
                }

                if (vr.getHigh() != null)
                {
                    if (vr.isHighInclusive())
                    {
                        sb.append("(version<=");
                        sb.append(vr.getHigh().toString());
                        sb.append(")");
                    }
                    else
                    {
                        sb.append("(!(version>=");
                        sb.append(vr.getHigh().toString());
                        sb.append("))");
                    }
                }
            }
            else
            {
                sb.append("(");
                sb.append(m_attrs[i].getName());
                sb.append("=");
                sb.append(m_attrs[i].getValue().toString());
                sb.append(")");
            }
        }

        sb.append(")");

        try
        {
            return new FilterImpl(sb.toString());
        }
        catch (InvalidSyntaxException ex)
        {
            // This should never happen, so we can safely ignore.
        }

        return null;
    }
}