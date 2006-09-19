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
package org.apache.felix.framework.searchpolicy;

import java.util.*;

import org.apache.felix.framework.util.Util;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class R4Export extends R4Package
{
    private String[] m_uses = null;
    private String[][] m_includeFilter = null;
    private String[][] m_excludeFilter = null;

    public R4Export(R4Package pkg)
    {
        this(pkg.getName(), pkg.getDirectives(), pkg.getAttributes());
    }

    public R4Export(String name, R4Directive[] directives, R4Attribute[] attrs)
    {
        super(name, directives, attrs);

        // Find all export directives: uses, mandatory, include, and exclude.
        String mandatory = "", uses = "";
        for (int i = 0; i < m_directives.length; i++)
        {
            if (m_directives[i].getName().equals(Constants.USES_DIRECTIVE))
            {
                uses = m_directives[i].getValue();
            }
            else if (m_directives[i].getName().equals(Constants.MANDATORY_DIRECTIVE))
            {
                mandatory = m_directives[i].getValue();
            }
            else if (m_directives[i].getName().equals(Constants.INCLUDE_DIRECTIVE))
            {
                String[] ss = Util.parseDelimitedString(m_directives[i].getValue(), ",");
                m_includeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    m_includeFilter[filterIdx] = parseSubstring(ss[filterIdx]);
                }
            }
            else if (m_directives[i].getName().equals(Constants.EXCLUDE_DIRECTIVE))
            {
                String[] ss = Util.parseDelimitedString(m_directives[i].getValue(), ",");
                m_excludeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    m_excludeFilter[filterIdx] = parseSubstring(ss[filterIdx]);
                }
            }
        }

        // Parse these uses directive.
        StringTokenizer tok = new StringTokenizer(uses, ",");
        m_uses = new String[tok.countTokens()];
        for (int i = 0; i < m_uses.length; i++)
        {
            m_uses[i] = tok.nextToken().trim();
        }

        // Parse mandatory directive and mark specified
        // attributes as mandatory.
        tok = new StringTokenizer(mandatory, ",");
        while (tok.hasMoreTokens())
        {
            // Get attribute name.
            String attrName = tok.nextToken().trim();
            // Find attribute and mark it as mandatory.
            boolean found = false;
            for (int i = 0; (!found) && (i < m_attrs.length); i++)
            {
                if (m_attrs[i].getName().equals(attrName))
                {
                    m_attrs[i] = new R4Attribute(
                        m_attrs[i].getName(),
                        m_attrs[i].getValue(), true);
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

        // Find the version, if present, and convert to Version.
        // The version attribute value may be a String or a Version,
        // since the value may be coming from an R4Export that already
        // converted it to Version.
        m_version = Version.emptyVersion;
        for (int i = 0; i < m_attrs.length; i++)
        {
            if (m_attrs[i].getName().equals(Constants.VERSION_ATTRIBUTE))
            {
                String versionStr = (m_attrs[i].getValue() instanceof Version)
                    ? ((Version) m_attrs[i].getValue()).toString()
                    : (String) m_attrs[i].getValue();
                m_version = Version.parseVersion(versionStr);
                m_attrs[i] = new R4Attribute(
                    m_attrs[i].getName(),
                    m_version,
                    m_attrs[i].isMandatory());
                break;
            }
        }
    }

    public String[] getUses()
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
        for (;;)
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
                break;
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
        int index = 0;

        for (int i = 0; i < len; i++)
        {
            String piece = (String) pieces[i];
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
                break;
            }
            // initial non-star; assert index == 0
            else if (i == 0)
            {
                if (!s.startsWith(piece))
                {
                    result = false;
                    break;
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
                    break;
                }
            }
            // start beyond the matching piece
            index += piece.length();
        }

        return result;
    }
}