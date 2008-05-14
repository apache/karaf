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
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class Capability implements ICapability
{
    private String m_namespace;
    private R4Directive[] m_directives;
    private R4Attribute[] m_attributes;
    private Map m_attrMap;
    private String[] m_uses = new String[0];
    private String[][] m_includeFilter;
    private String[][] m_excludeFilter;

    // Cached properties for performance reasons.
    private String m_pkgName;
    private Version m_pkgVersion = Version.emptyVersion;

    public Capability(String namespace, R4Directive[] dirs, R4Attribute[] attrs)
    {
        m_namespace = namespace;
        m_directives = dirs;
        m_attributes = attrs;

        // Find all export directives: uses, mandatory, include, and exclude.
        String mandatory = "";
        for (int dirIdx = 0; (m_directives != null) && (dirIdx < m_directives.length); dirIdx++)
        {
            if (m_directives[dirIdx].getName().equals(Constants.USES_DIRECTIVE))
            {
                // Parse these uses directive.
                StringTokenizer tok = new StringTokenizer(m_directives[dirIdx].getValue(), ",");
                m_uses = new String[tok.countTokens()];
                for (int i = 0; i < m_uses.length; i++)
                {
                    m_uses[i] = tok.nextToken().trim();
                }
            }
            else if (m_directives[dirIdx].getName().equals(Constants.MANDATORY_DIRECTIVE))
            {
                mandatory = m_directives[dirIdx].getValue();
            }
            else if (m_directives[dirIdx].getName().equals(Constants.INCLUDE_DIRECTIVE))
            {
                String[] ss = ManifestParser.parseDelimitedString(m_directives[dirIdx].getValue(), ",");
                m_includeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    m_includeFilter[filterIdx] = parseSubstring(ss[filterIdx]);
                }
            }
            else if (m_directives[dirIdx].getName().equals(Constants.EXCLUDE_DIRECTIVE))
            {
                String[] ss = ManifestParser.parseDelimitedString(m_directives[dirIdx].getValue(), ",");
                m_excludeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    m_excludeFilter[filterIdx] = parseSubstring(ss[filterIdx]);
                }
            }
        }

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
        for (int i = 0; i < m_attributes.length; i++)
        {
            if (m_attributes[i].getName().equals(ICapability.PACKAGE_PROPERTY))
            {
                m_pkgName = (String) m_attributes[i].getValue();
            }
            else if (m_attributes[i].getName().equals(ICapability.VERSION_PROPERTY))
            {
                m_pkgVersion = (Version) m_attributes[i].getValue();
            }
        }
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
            included = checkSubstring(m_includeFilter[i], className);
        }

        // If there are no exclude filters then no classes are excluded
        // by default, otherwise try to find one match.
        boolean excluded = false;
        for (int i = 0;
            (!excluded) && (m_excludeFilter != null) && (i < m_excludeFilter.length);
            i++)
        {
            excluded = checkSubstring(m_excludeFilter[i], className);
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

// TODO: RB - Remove or simplify toString() for final version.
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

    //
    // The following substring-related code was lifted and modified
    // from the LDAP parser code.
    //

    private static String[] parseSubstring(String target)
    {
        List pieces = new ArrayList();
        StringBuffer ss = new StringBuffer();
        // int kind = SIMPLE; // assume until proven otherwise
        boolean wasStar = false; // indicates last piece was a star
        boolean leftstar = false; // track if the initial piece is a star
        boolean rightstar = false; // track if the final piece is a star

        int idx = 0;

        // We assume (sub)strings can contain leading and trailing blanks
loop:   for (;;)
        {
            if (idx >= target.length())
            {
                if (wasStar)
                {
                    // insert last piece as "" to handle trailing star
                    rightstar = true;
                }
                else
                {
                    pieces.add(ss.toString());
                    // accumulate the last piece
                    // note that in the case of
                    // (cn=); this might be
                    // the string "" (!=null)
                }
                ss.setLength(0);
                break loop;
            }

            char c = target.charAt(idx++);
            if (c == '*')
            {
                if (wasStar)
                {
                    // encountered two successive stars;
                    // I assume this is illegal
                    throw new IllegalArgumentException("Invalid filter string: " + target);
                }
                if (ss.length() > 0)
                {
                    pieces.add(ss.toString()); // accumulate the pieces
                    // between '*' occurrences
                }
                ss.setLength(0);
                // if this is a leading star, then track it
                if (pieces.size() == 0)
                {
                    leftstar = true;
                }
                ss.setLength(0);
                wasStar = true;
            }
            else
            {
                wasStar = false;
                ss.append(c);
            }
        }
        if (leftstar || rightstar || pieces.size() > 1)
        {
            // insert leading and/or trailing "" to anchor ends
            if (rightstar)
            {
                pieces.add("");
            }
            if (leftstar)
            {
                pieces.add(0, "");
            }
        }
        return (String[]) pieces.toArray(new String[pieces.size()]);
    }

    private static boolean checkSubstring(String[] pieces, String s)
    {
        // Walk the pieces to match the string
        // There are implicit stars between each piece,
        // and the first and last pieces might be "" to anchor the match.
        // assert (pieces.length > 1)
        // minimal case is <string>*<string>

        boolean result = false;
        int len = pieces.length;

loop:   for (int i = 0; i < len; i++)
        {
            String piece = pieces[i];
            int index = 0;
            if (i == len - 1)
            {
                // this is the last piece
                if (s.endsWith(piece))
                {
                    result = true;
                }
                else
                {
                    result = false;
                }
                break loop;
            }
            // initial non-star; assert index == 0
            else if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break loop;
                }
            }
            // assert i > 0 && i < len-1
            else
            {
                // Sure wish stringbuffer supported e.g. indexOf
                index = s.indexOf(piece, index);
                if (index < 0)
                {
                    result = false;
                    break loop;
                }
            }
            // start beyond the matching piece
            index += piece.length();
        }

        return result;
    }
}