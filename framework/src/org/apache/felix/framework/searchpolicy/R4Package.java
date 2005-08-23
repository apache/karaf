/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.framework.searchpolicy;

import java.util.*;

import org.apache.felix.framework.util.FelixConstants;
import org.apache.felix.framework.util.Util;

public class R4Package
{
    private String m_id = "";
    private R4Directive[] m_directives = null;
    private R4Attribute[] m_attrs = null;
    private R4Version m_versionLow = null;
    private R4Version m_versionHigh = null;
    private String[] m_uses = null;
    private boolean m_isOptional = false;
    private String[][] m_includeFilter = null;
    private String[][] m_excludeFilter = null;

    protected R4Package(R4Package pkg)
    {
        m_id = pkg.m_id;
        m_directives = pkg.m_directives;
        m_attrs = pkg.m_attrs;
        m_versionLow = pkg.m_versionLow;
        m_versionHigh = pkg.m_versionHigh;
        m_uses = pkg.m_uses;
        m_isOptional = pkg.m_isOptional;
        m_includeFilter = pkg.m_includeFilter;
        m_excludeFilter = pkg.m_excludeFilter;
    }

    public R4Package(String id, R4Directive[] directives, R4Attribute[] attrs)
    {
        m_id = id;
        m_directives = (directives == null) ? new R4Directive[0] : directives;
        m_attrs = (attrs == null) ? new R4Attribute[0] : attrs;

        // Find all directives: uses, mandatory, resolution, include, and exclude.
        String mandatory = "", uses = "";
        for (int i = 0; i < m_directives.length; i++)
        {
            if (m_directives[i].getName().equals(FelixConstants.USES_DIRECTIVE))
            {
                uses = m_directives[i].getValue();
            }
            else if (m_directives[i].getName().equals(FelixConstants.MANDATORY_DIRECTIVE))
            {
                mandatory = m_directives[i].getValue();
            }
            else if (m_directives[i].getName().equals(FelixConstants.RESOLUTION_DIRECTIVE))
            {
                m_isOptional = m_directives[i].getValue().equals(FelixConstants.RESOLUTION_OPTIONAL);
            }
            else if (m_directives[i].getName().equals(FelixConstants.INCLUDE_DIRECTIVE))
            {
                String[] ss = Util.parseDelimitedString(m_directives[i].getValue(), ",");
                m_includeFilter = new String[ss.length][];
                for (int filterIdx = 0; filterIdx < ss.length; filterIdx++)
                {
                    m_includeFilter[filterIdx] = parseSubstring(ss[filterIdx]);
                }
            }
            else if (m_directives[i].getName().equals(FelixConstants.EXCLUDE_DIRECTIVE))
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
                        m_attrs[i].getName(), m_attrs[i].getValue(), true);
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

        // Find and parse version attribute, if present.
        String versionInterval = "0.0.0";
        for (int i = 0; i < m_attrs.length; i++)
        {
            if (m_attrs[i].getName().equals(FelixConstants.VERSION_ATTRIBUTE) ||
                m_attrs[i].getName().equals(FelixConstants.PACKAGE_SPECIFICATION_VERSION))
            {
                // Normalize version attribute name.
                m_attrs[i] = new R4Attribute(
                    FelixConstants.VERSION_ATTRIBUTE, m_attrs[i].getValue(),
                    m_attrs[i].isMandatory());
                versionInterval = m_attrs[i].getValue();
                break;
            }
        }
        
        R4Version[] versions = parseVersionInterval(versionInterval);
        m_versionLow = versions[0];
        if (versions.length == 2)
        {
            m_versionHigh = versions[1];
        }
    }

    public String getId()
    {
        return m_id;
    }

    public R4Directive[] getDirectives()
    {
        return m_directives;
    }

    public R4Attribute[] getAttributes()
    {
        return m_attrs;
    }

    public R4Version getVersionLow()
    {
        return m_versionLow;
    }

    public R4Version getVersionHigh()
    {
        return m_versionHigh;
    }

    public String[] getUses()
    {
        return m_uses;
    }

    public boolean isOptional()
    {
        return m_isOptional;
    }

    public boolean isIncluded(String name)
    {
        if ((m_includeFilter == null) && (m_excludeFilter == null))
        {
            return true;
        }

        // Get the class name portion of the target class.
        String className = org.apache.felix.moduleloader.Util.getClassName(name);

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

    // PREVIOUSLY PART OF COMPATIBILITY POLICY.
    public boolean doesSatisfy(R4Package pkg)
    {
        // For packages to be compatible, they must have the
        // same name.
        if (!m_id.equals(pkg.m_id))
        {
            return false;
        }
        
        return isVersionInRange(m_versionLow, pkg.m_versionLow, pkg.m_versionHigh)
            && doAttributesMatch(pkg);
    }

    // PREVIOUSLY PART OF COMPATIBILITY POLICY.
    public static boolean isVersionInRange(R4Version version, R4Version low, R4Version high)
    {
        // We might not have an upper end to the range.
        if (high == null)
        {
            return (version.compareTo(low) >= 0);
        }
        else if (low.isInclusive() && high.isInclusive())
        {
            return (version.compareTo(low) >= 0) && (version.compareTo(high) <= 0);
        }
        else if (high.isInclusive())
        {
            return (version.compareTo(low) > 0) && (version.compareTo(high) <= 0);
        }
        else if (low.isInclusive())
        {
            return (version.compareTo(low) >= 0) && (version.compareTo(high) < 0);
        }

        return (version.compareTo(low) > 0) && (version.compareTo(high) < 0);
    }

    private boolean doAttributesMatch(R4Package pkg)
    {
        // Cycle through all attributes of the specified package
        // and make sure their values match the attribute values
        // of this package.
        for (int attrIdx = 0; attrIdx < pkg.m_attrs.length; attrIdx++)
        {
            // Get current attribute from specified package.
            R4Attribute attr = pkg.m_attrs[attrIdx];

            // Ignore version attribute, since it is a special case that
            // has already been compared using isVersionInRange() before
            // the call to this method was made.
            if (attr.getName().equals(FelixConstants.VERSION_ATTRIBUTE))
            {
                continue;
            }

            // Check if this package has the same attribute.
            boolean found = false;
            for (int thisAttrIdx = 0;
                (!found) && (thisAttrIdx < m_attrs.length);
                thisAttrIdx++)
            {
                // Get current attribute for this package.
                R4Attribute thisAttr = m_attrs[thisAttrIdx];
                // Check if the attribute names are equal.
                if (attr.getName().equals(thisAttr.getName()))
                {
                    // If the values are not equal, then return false immediately.
                    // We should not compare version values here, since they are
                    // a special case and have already been compared by a call to
                    // isVersionInRange() before getting here; however, it is
                    // possible for version to be mandatory, so make sure it is
                    // present below.
                    if (!attr.getValue().equals(thisAttr.getValue()))
                    {
                        return false;
                    }
                    found = true;
                }
            }
            // If the attribute was not found, then return false.
            if (!found)
            {
                return false;
            }
        }

        // Now, cycle through all attributes of this package and verify that
        // all mandatory attributes are present in the speceified package.
        for (int thisAttrIdx = 0; thisAttrIdx < m_attrs.length; thisAttrIdx++)
        {
            // Get current attribute for this package.
            R4Attribute thisAttr = m_attrs[thisAttrIdx];
            
            // If the attribute is mandatory, then make sure
            // the specified package has the attribute.
            if (thisAttr.isMandatory())
            {
                boolean found = false;
                for (int attrIdx = 0;
                    (!found) && (attrIdx < pkg.m_attrs.length);
                    attrIdx++)
                {
                    // Get current attribute from specified package.
                    R4Attribute attr = pkg.m_attrs[attrIdx];
        
                    // Check if the attribute names are equal
                    // and set found flag.
                    if (thisAttr.getName().equals(attr.getName()))
                    {
                        found = true;
                    }
                }
                // If not found, then return false.
                if (!found)
                {
                    return false;
                }
            }
        }

        return true;
    }

    public String toString()
    {
        String msg = getId();
        for (int i = 0; (m_directives != null) && (i < m_directives.length); i++)
        {
            msg = msg + " [" + m_directives[i].getName() + ":="+ m_directives[i].getValue() + "]";
        }
        for (int i = 0; (m_attrs != null) && (i < m_attrs.length); i++)
        {
            msg = msg + " [" + m_attrs[i].getName() + "="+ m_attrs[i].getValue() + "]";
        }
        return msg;
    }

    // Like this: pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2,
    //            pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static R4Package[] parseImportOrExportHeader(String s)
    {
        R4Package[] pkgs = null;
        if (s != null)
        {
            if (s.length() == 0)
            {
                throw new IllegalArgumentException(
                    "The import and export headers cannot be an empty string.");
            }
            String[] ss = Util.parseDelimitedString(
                s, FelixConstants.CLASS_PATH_SEPARATOR);
            pkgs = parsePackageStrings(ss);
        }
        return (pkgs == null) ? new R4Package[0] : pkgs;
    }

    // Like this: pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static R4Package[] parsePackageStrings(String[] ss)
        throws IllegalArgumentException
    {
        if (ss == null)
        {
            return null;
        }

        List completeList = new ArrayList();
        for (int ssIdx = 0; ssIdx < ss.length; ssIdx++)
        {
            // Break string into semi-colon delimited pieces.
            String[] pieces = Util.parseDelimitedString(
                ss[ssIdx], FelixConstants.PACKAGE_SEPARATOR);

            // Count the number of different packages; packages
            // will not have an '=' in their string. This assumes
            // that packages come first, before directives and
            // attributes.
            int pkgCount = 0;
            for (int pieceIdx = 0; pieceIdx < pieces.length; pieceIdx++)
            {
                if (pieces[pieceIdx].indexOf('=') >= 0)
                {
                    break;
                }
                pkgCount++;
            }

            // Error if no packages were specified.
            if (pkgCount == 0)
            {
                throw new IllegalArgumentException(
                    "No packages specified on import: " + ss[ssIdx]);
            }

            // Parse the directives/attributes.
            R4Directive[] dirs = new R4Directive[pieces.length - pkgCount];
            R4Attribute[] attrs = new R4Attribute[pieces.length - pkgCount];
            int dirCount = 0, attrCount = 0;
            int idx = -1;
            String sep = null;
            for (int pieceIdx = pkgCount; pieceIdx < pieces.length; pieceIdx++)
            {
                // Check if it is a directive.
                if ((idx = pieces[pieceIdx].indexOf(FelixConstants.DIRECTIVE_SEPARATOR)) >= 0)
                {
                    sep = FelixConstants.DIRECTIVE_SEPARATOR;
                }
                // Check if it is an attribute.
                else if ((idx = pieces[pieceIdx].indexOf(FelixConstants.ATTRIBUTE_SEPARATOR)) >= 0)
                {
                    sep = FelixConstants.ATTRIBUTE_SEPARATOR;
                }
                // It is an error.
                else
                {
                    throw new IllegalArgumentException(
                        "Not a directive/attribute: " + ss[ssIdx]);
                }

                String key = pieces[pieceIdx].substring(0, idx).trim();
                String value = pieces[pieceIdx].substring(idx + sep.length()).trim();

                // Remove quotes, if value is quoted.
                if (value.startsWith("\"") && value.endsWith("\""))
                {
                    value = value.substring(1, value.length() - 1);
                }

                // Save the directive/attribute in the appropriate array.
                if (sep.equals(FelixConstants.DIRECTIVE_SEPARATOR))
                {
                    dirs[dirCount++] = new R4Directive(key, value);
                }
                else
                {
                    attrs[attrCount++] = new R4Attribute(key, value, false);
                }
            }

            // Shrink directive array.
            R4Directive[] dirsFinal = new R4Directive[dirCount];
            System.arraycopy(dirs, 0, dirsFinal, 0, dirCount);
            // Shrink attribute array.
            R4Attribute[] attrsFinal = new R4Attribute[attrCount];
            System.arraycopy(attrs, 0, attrsFinal, 0, attrCount);

            // Create package attributes for each package and
            // set directives/attributes. Add each package to
            // completel list of packages.
            R4Package[] pkgs = new R4Package[pkgCount];
            for (int pkgIdx = 0; pkgIdx < pkgCount; pkgIdx++)
            {
                pkgs[pkgIdx] = new R4Package(pieces[pkgIdx], dirsFinal, attrsFinal);
                completeList.add(pkgs[pkgIdx]);
            }
        }
    
        R4Package[] ips = (R4Package[])
            completeList.toArray(new R4Package[completeList.size()]);
        return ips;
    }

    public static R4Version[] parseVersionInterval(String interval)
    {
        // Check if the version is an interval.
        if (interval.indexOf(',') >= 0)
        {
            String s = interval.substring(1, interval.length() - 1);
            String vlo = s.substring(0, s.indexOf(','));
            String vhi = s.substring(s.indexOf(',') + 1, s.length());
            return new R4Version[] {
                new R4Version(vlo, (interval.charAt(0) == '[')),
                new R4Version(vhi, (interval.charAt(interval.length() - 1) == ']'))
            };
        }
        else
        {
            return new R4Version[] { new R4Version(interval, true) };
        }
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
            String piece = (String) pieces[i];
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