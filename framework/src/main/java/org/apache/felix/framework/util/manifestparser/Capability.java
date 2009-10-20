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

import java.util.*;

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.ICapability;
import org.apache.felix.moduleloader.IModule;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class Capability implements ICapability, Comparable
{
    private final IModule m_module;
    private final String m_namespace;
    private final R4Directive[] m_directives;
    private final R4Attribute[] m_attributes;
    private final String[] m_uses;
    private final String[][] m_includeFilter;
    private final String[][] m_excludeFilter;
    private volatile Map m_attrMap;

    // Cached properties for performance reasons.
    private final String m_pkgName;
    private final Version m_pkgVersion;

    public Capability(IModule module, String namespace, R4Directive[] dirs, R4Attribute[] attrs)
    {
        m_module = module;
        m_namespace = namespace;
        m_directives = dirs;
        m_attributes = attrs;

        // Find all export directives: uses, mandatory, include, and exclude.
        String mandatory = "";
        String[] uses = new String[0];
        String[][] includeFilter = null, excludeFilter = null;
        for (int dirIdx = 0; (m_directives != null) && (dirIdx < m_directives.length); dirIdx++)
        {
            if (m_directives[dirIdx].getName().equals(Constants.USES_DIRECTIVE))
            {
                // Parse these uses directive.
                StringTokenizer tok = new StringTokenizer(m_directives[dirIdx].getValue(), ",");
                uses = new String[tok.countTokens()];
                for (int i = 0; i < uses.length; i++)
                {
                    uses[i] = tok.nextToken().trim();
                }
            }
            else if (m_directives[dirIdx].getName().equals(Constants.MANDATORY_DIRECTIVE))
            {
                mandatory = m_directives[dirIdx].getValue();
            }
            else if (m_directives[dirIdx].getName().equals(Constants.INCLUDE_DIRECTIVE))
            {
                String[] ss = ManifestParser.parseDelimitedString(m_directives[dirIdx].getValue(), ",");
                includeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    includeFilter[filterIdx] = Util.parseSubstring(ss[filterIdx]);
                }
            }
            else if (m_directives[dirIdx].getName().equals(Constants.EXCLUDE_DIRECTIVE))
            {
                String[] ss = ManifestParser.parseDelimitedString(m_directives[dirIdx].getValue(), ",");
                excludeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    excludeFilter[filterIdx] = Util.parseSubstring(ss[filterIdx]);
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
            for (int i = 0; (!found) && (i < m_attributes.length); i++)
            {
                if (m_attributes[i].getName().equals(attrName))
                {
                    m_attributes[i] = new R4Attribute(
                        m_attributes[i].getName(),
                        m_attributes[i].getValue(), true);
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

        // For performance reasons, find the package name and version properties.
        String pkgName = null;
        Version pkgVersion = Version.emptyVersion;
        for (int i = 0; i < m_attributes.length; i++)
        {
            if (m_attributes[i].getName().equals(ICapability.PACKAGE_PROPERTY))
            {
                pkgName = (String) m_attributes[i].getValue();
            }
            else if (m_attributes[i].getName().equals(ICapability.VERSION_PROPERTY))
            {
                pkgVersion = (Version) m_attributes[i].getValue();
            }
        }

        // Set final values.
        m_pkgName = pkgName;
        m_pkgVersion = pkgVersion;
    }

    public IModule getModule()
    {
        return m_module;
    }

    public String getNamespace()
    {
        return m_namespace;
    }

// TODO: RB - Determine how to eliminate these non-generic methods;
//            at least make sure they are not used in the generic resolver.
    public String getPackageName()
    {
        return m_pkgName;
    }

    public Version getPackageVersion()
    {
        return m_pkgVersion;
    }

    public R4Directive[] getDirectives()
    {
        // TODO: RB - We should return copies of the arrays probably.
        return m_directives;
    }

    public R4Attribute[] getAttributes()
    {
        // TODO: RB - We should return copies of the arrays probably.
        return m_attributes;
    }

    public String[] getUses()
    {
        // TODO: RB - We should return copies of the arrays probably.
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
            (!included) && (m_includeFilter != null) && (i < m_includeFilter.length);
            i++)
        {
            included = Util.checkSubstring(m_includeFilter[i], className);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_excludeFilter != null) && (i < m_excludeFilter.length);
            i++)
        {
            excluded = Util.checkSubstring(m_excludeFilter[i], className);
        }
        return included && !excluded;
    }

// TODO: RB - Terminology mismatch property vs. attribute.
    public Map getProperties()
    {
        if (m_attrMap == null)
        {
            m_attrMap = new Map() {

                public int size()
                {
                    // A name and version attribute is always present, since it has a
                    // default value.
                    return m_attributes.length + 2;
                }

                public boolean isEmpty()
                {
                    // A version attribute is always present, since it has a
                    // default value.
                    return false;
                }

                public boolean containsKey(Object key)
                {
                    return (get(key) != null);
                }

                public boolean containsValue(Object value)
                {
                    // Check the package name.
                    if (m_pkgName.equals(value))
                    {
                        return true;
                    }

                    // Check the package version.
                    if (m_pkgVersion.equals(value))
                    {
                        return true;
                    }

                    // Check all attributes.
                    for (int i = 0; i < m_attributes.length; i++)
                    {
                        if (m_attributes[i].getValue().equals(value))
                        {
                            return true;
                        }
                    }

                    return false;
                }

                public Object get(Object key)
                {
                    if (ICapability.PACKAGE_PROPERTY.equals(key))
                    {
                        return m_pkgName;
                    }
                    else if (ICapability.VERSION_PROPERTY.equals(key))
                    {
                        return m_pkgVersion;
                    }

                    for (int i = 0; i < m_attributes.length; i++)
                    {
                        if (m_attributes[i].getName().equals(key))
                        {
                            return m_attributes[i].getValue();
                        }
                    }

                    return null;
                }

                public Object put(Object key, Object value)
                {
                    throw new UnsupportedOperationException("Map.put() not implemented.");
                }

                public Object remove(Object key)
                {
                    throw new UnsupportedOperationException("Map.remove() not implemented.");
                }

                public void putAll(Map t)
                {
                    throw new UnsupportedOperationException("Map.putAll() not implemented.");
                }

                public void clear()
                {
                    throw new UnsupportedOperationException("Map.clear() not implemented.");
                }

                public Set keySet()
                {
                    Set set = new HashSet();
                    set.add(ICapability.PACKAGE_PROPERTY);
                    set.add(ICapability.VERSION_PROPERTY);
                    for (int i = 0; i < m_attributes.length; i++)
                    {
                        set.add(m_attributes[i].getName());
                    }
                    return set;
                }

                public Collection values()
                {
                    throw new UnsupportedOperationException("Map.values() not implemented.");
                }

                public Set entrySet()
                {
                    throw new UnsupportedOperationException("Map.entrySet() not implemented.");
                }
            };
        }
        return m_attrMap;
    }

    public int compareTo(Object o)
    {
        Capability cap = (Capability) o;
        Version thisVersion = null;
        Version version = null;
        if (getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
        {
            thisVersion = getPackageVersion();
            version = cap.getPackageVersion();
        }
        else if (getNamespace().equals(ICapability.MODULE_NAMESPACE))
        {
            thisVersion = (Version) getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
            version = (Version) cap.getProperties().get(Constants.BUNDLE_VERSION_ATTRIBUTE);
        }
        if ((thisVersion != null) && (version != null))
        {
            int cmp = thisVersion.compareTo(version);
            if (cmp < 0)
            {
                return 1;
            }
            else if (cmp > 0)
            {
                return -1;
            }
            else
            {
                long thisId = m_module.getBundle().getBundleId();
                long id = cap.getModule().getBundle().getBundleId();
                if (thisId < id)
                {
                    return -1;
                }
                else if (thisId > id)
                {
                    return 1;
                }
                return 0;
            }
        }
        else
        {
            return -1;
        }
    }

    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append(getNamespace());
        for (int i = 0; (m_directives != null) && (i < m_directives.length); i++)
        {
            sb.append(";");
            sb.append(m_directives[i].getName());
            sb.append(":=\"");
            sb.append(m_directives[i].getValue());
            sb.append("\"");
        }
        for (int i = 0; (m_attributes != null) && (i < m_attributes.length); i++)
        {
            sb.append(";");
            sb.append(m_attributes[i].getName());
            sb.append("=\"");
            sb.append(m_attributes[i].getValue());
            sb.append("\"");
        }
        return sb.toString();
    }
}