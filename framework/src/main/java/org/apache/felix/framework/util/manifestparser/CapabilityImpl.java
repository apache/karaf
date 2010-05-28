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
import java.util.StringTokenizer;
import org.apache.felix.framework.capabilityset.Attribute;
import org.apache.felix.framework.capabilityset.Capability;
import org.apache.felix.framework.capabilityset.Directive;
import org.apache.felix.framework.capabilityset.SimpleFilter;
import org.apache.felix.framework.resolver.Module;
import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;

public class CapabilityImpl implements Capability
{
    private final Module m_module;
    private final String m_namespace;
    private final List<Directive> m_dirs;
    private final List<Directive> m_dirsConst;
    private final List<Attribute> m_attrs;
    private final List<Attribute> m_attrsConst;
    private final List<String> m_uses;
    private final List<List<String>> m_includeFilter;
    private final List<List<String>> m_excludeFilter;

    public CapabilityImpl(Module module, String namespace,
        List<Directive> dirs, List<Attribute> attrs)
    {
        m_namespace = namespace;
        m_module = module;
        m_dirs = dirs;
        m_dirsConst = Collections.unmodifiableList(m_dirs);
        m_attrs = attrs;
        m_attrsConst = Collections.unmodifiableList(m_attrs);

        // Find all export directives: uses, mandatory, include, and exclude.
        String mandatory = "";
        List<String> uses = new ArrayList(0);
        List<List<String>> includeFilter = null, excludeFilter = null;
        for (int dirIdx = 0; dirIdx < m_dirs.size(); dirIdx++)
        {
            if (m_dirs.get(dirIdx).getName().equals(Constants.USES_DIRECTIVE))
            {
                // Parse these uses directive.
                StringTokenizer tok = new StringTokenizer(
                    (String) m_dirs.get(dirIdx).getValue(), ",");
                uses = new ArrayList<String>(tok.countTokens());
                while (tok.hasMoreTokens())
                {
                    uses.add(tok.nextToken().trim());
                }
            }
            else if (m_dirs.get(dirIdx).getName().equals(Constants.MANDATORY_DIRECTIVE))
            {
                mandatory = (String) m_dirs.get(dirIdx).getValue();
            }
            else if (m_dirs.get(dirIdx).getName().equals(Constants.INCLUDE_DIRECTIVE)
                || m_dirs.get(dirIdx).getName().equals(Constants.EXCLUDE_DIRECTIVE))
            {
                List<List<String>> filterList = null;

                List<String> filters = ManifestParser.parseDelimitedString(
                    (String) m_dirs.get(dirIdx).getValue(), ",");
                filterList = new ArrayList<List<String>>(filters.size());

                for (int filterIdx = 0; filterIdx < filters.size(); filterIdx++)
                {
                    List<String> substrings = SimpleFilter.parseSubstring(filters.get(filterIdx));
                    filterList.add(substrings);
                }

                if (m_dirs.get(dirIdx).getName().equals(Constants.INCLUDE_DIRECTIVE))
                {
                    includeFilter = filterList;
                }
                else
                {
                    excludeFilter = filterList;
                }
            }
        }

        // Set final values.
        m_uses = uses;
        m_includeFilter = includeFilter;
        m_excludeFilter = excludeFilter;

        // Parse mandatory directive and mark specified
        // attributes as mandatory.
        StringTokenizer tok = new StringTokenizer(mandatory, ", ");
        while (tok.hasMoreTokens())
        {
            // Get attribute name.
            String attrName = tok.nextToken().trim();
            // Find attribute and mark it as mandatory.
            boolean found = false;
            for (int i = 0; (!found) && (i < m_attrs.size()); i++)
            {
                if (m_attrs.get(i).getName().equals(attrName))
                {
                    m_attrs.set(i, new Attribute(
                        m_attrs.get(i).getName(),
                        m_attrs.get(i).getValue(), true));
                    found = true;
                }
            }
            // If a specified mandatory attribute was not found,
            // then error.
            if (!found)
            {
                throw new IllegalArgumentException(
                    "Mandatory attribute '" + attrName + "' does not exist.");
            }
        }
    }

    public Module getModule()
    {
        return m_module;
    }

    public String getNamespace()
    {
        return m_namespace;
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

    public Attribute getAttribute(String name)
    {
        for (int i = 0; i < m_attrs.size(); i++)
        {
            if (m_attrs.get(i).getName().equals(name))
            {
                return m_attrs.get(i);
            }
        }
        return null;
    }

    public List<Attribute> getAttributes()
    {
        return m_attrsConst;
    }

    public List<String> getUses()
    {
        return m_uses;
    }

    public boolean isIncluded(String name)
    {
        if ((m_includeFilter == null) && (m_excludeFilter == null))
        {
            return true;
        }

        // Get the class name portion of the target class.
        String className = Util.getClassName(name);

        // If there are no include filters then all classes are included
        // by default, otherwise try to find one match.
        boolean included = (m_includeFilter == null);
        for (int i = 0;
            (!included) && (m_includeFilter != null) && (i < m_includeFilter.size());
            i++)
        {
            included = SimpleFilter.compareSubstring(m_includeFilter.get(i), className);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_excludeFilter != null) && (i < m_excludeFilter.size());
            i++)
        {
            excluded = SimpleFilter.compareSubstring(m_excludeFilter.get(i), className);
        }
        return included && !excluded;
    }

    public String toString()
    {
        if (m_module == null)
        {
            return m_attrs.toString();
        }
        if (m_namespace.equals(Capability.PACKAGE_NAMESPACE))
        {
            return "[" + m_module + "] "
                + m_namespace + "; " + getAttribute(Capability.PACKAGE_ATTR);
        }
        return "[" + m_module + "] " + m_namespace + "; " + m_attrs;
    }
}