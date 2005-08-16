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
package org.apache.osgi.bundle.bundlerepository;

import java.util.*;

import org.apache.osgi.service.bundlerepository.*;
import org.osgi.framework.Constants;

//
// This class is essentially the same as the R4Package class in Felix,
// except that I had to add the parseDelimitedString() method. These
// two classes should be unified.
//

/**
 * This is a simple class to encapsulate a package declaration for
 * bundle imports and exports for the bundle repository.
**/
public class R4Package implements IPackage
{
    private String m_id = "";
    private IDirective[] m_directives = null;
    private IAttribute[] m_attrs = null;
    private IVersion m_versionLow = null;
    private IVersion m_versionHigh = null;
    private boolean m_isOptional = false;

    protected R4Package(R4Package pkg)
    {
        m_id = pkg.m_id;
        m_directives = pkg.m_directives;
        m_attrs = pkg.m_attrs;
        m_versionLow = pkg.m_versionLow;
        m_versionHigh = pkg.m_versionHigh;
        m_isOptional = pkg.m_isOptional;
    }

    public R4Package(String id, IDirective[] directives, IAttribute[] attrs)
    {
        m_id = id;
        m_directives = (directives == null) ? new IDirective[0] : directives;
        m_attrs = (attrs == null) ? new IAttribute[0] : attrs;

        // Find mandatory and resolution directives, if present.
        String mandatory = "";
        for (int i = 0; i < m_directives.length; i++)
        {
            if (m_directives[i].getName().equals(Constants.MANDATORY_DIRECTIVE))
            {
                mandatory = m_directives[i].getValue();
            }
            else if (m_directives[i].getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                m_isOptional = m_directives[i].getValue().equals(Constants.RESOLUTION_OPTIONAL);
            }
        }

        // Parse mandatory directive and mark specified
        // attributes as mandatory.
        StringTokenizer tok = new StringTokenizer(mandatory, "");
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
            if (m_attrs[i].getName().equals(Constants.VERSION_ATTRIBUTE) ||
                m_attrs[i].getName().equals(Constants.PACKAGE_SPECIFICATION_VERSION))
            {
                // Normalize version attribute name.
                m_attrs[i] = new R4Attribute(
                    Constants.VERSION_ATTRIBUTE, m_attrs[i].getValue(),
                    m_attrs[i].isMandatory());
                versionInterval = m_attrs[i].getValue();
                break;
            }
        }
        
        IVersion[] versions = parseVersionInterval(versionInterval);
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

    public IDirective[] getDirectives()
    {
        return m_directives;
    }

    public IAttribute[] getAttributes()
    {
        return m_attrs;
    }

    public IVersion getVersionLow()
    {
        return m_versionLow;
    }

    public IVersion getVersionHigh()
    {
        return m_versionHigh;
    }

    public boolean isOptional()
    {
        return m_isOptional;
    }

    // PREVIOUSLY PART OF COMPATIBILITY POLICY.
    public boolean doesSatisfy(IPackage pkg)
    {
        // For packages to be compatible, they must have the
        // same name.
        if (!m_id.equals(pkg.getId()))
        {
            return false;
        }
        
        return isVersionInRange(m_versionLow, pkg.getVersionLow(), pkg.getVersionHigh())
            && doAttributesMatch(pkg);
    }

    // PREVIOUSLY PART OF COMPATIBILITY POLICY.
    public static boolean isVersionInRange(IVersion version, IVersion low, IVersion high)
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

    private boolean doAttributesMatch(IPackage pkg)
    {
        // Cycle through all attributes of the specified package
        // and make sure their values match the attribute values
        // of this package.
        for (int attrIdx = 0; attrIdx < pkg.getAttributes().length; attrIdx++)
        {
            // Get current attribute from specified package.
            IAttribute attr = pkg.getAttributes()[attrIdx];

            // Ignore version attribute, since it is a special case that
            // has already been compared using isVersionInRange() before
            // the call to this method was made.
            if (attr.getName().equals(Constants.VERSION_ATTRIBUTE))
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
                IAttribute thisAttr = m_attrs[thisAttrIdx];
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
            IAttribute thisAttr = m_attrs[thisAttrIdx];
            
            // If the attribute is mandatory, then make sure
            // the specified package has the attribute.
            if (thisAttr.isMandatory())
            {
                boolean found = false;
                for (int attrIdx = 0;
                    (!found) && (attrIdx < pkg.getAttributes().length);
                    attrIdx++)
                {
                    // Get current attribute from specified package.
                    IAttribute attr = pkg.getAttributes()[attrIdx];
        
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
            msg = msg + " [" + m_directives[i].getName() + ":="+ m_directives[i].getName() + "]";
        }
        for (int i = 0; (m_attrs != null) && (i < m_attrs.length); i++)
        {
            msg = msg + " [" + m_attrs[i].getValue() + "="+ m_attrs[i].getValue() + "]";
        }
        return msg;
    }

    // Like this: pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2,
    //            pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static IPackage[] parseImportOrExportHeader(String s)
    {
        IPackage[] pkgs = null;
        if (s != null)
        {
            if (s.length() == 0)
            {
                throw new IllegalArgumentException(
                    "The import and export headers cannot be an empty string.");
            }
            String[] ss = parseDelimitedString(s, ","); // FelixConstants.CLASS_PATH_SEPARATOR
            pkgs = parsePackageStrings(ss);
        }
        return (pkgs == null) ? new IPackage[0] : pkgs;
    }

    // Like this: pkg1; pkg2; dir1:=dirval1; dir2:=dirval2; attr1=attrval1; attr2=attrval2
    public static IPackage[] parsePackageStrings(String[] ss)
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
            String[] pieces = parseDelimitedString(
                ss[ssIdx], ";"); // FelixConstants.PACKAGE_SEPARATOR

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
            IDirective[] dirs = new IDirective[pieces.length - pkgCount];
            IAttribute[] attrs = new IAttribute[pieces.length - pkgCount];
            int dirCount = 0, attrCount = 0;
            int idx = -1;
            String sep = null;
            for (int pieceIdx = pkgCount; pieceIdx < pieces.length; pieceIdx++)
            {
                // Check if it is a directive.
                if ((idx = pieces[pieceIdx].indexOf(":=")) >= 0) // FelixConstants.DIRECTIVE_SEPARATOR
                {
                    sep = ":="; // FelixConstants.DIRECTIVE_SEPARATOR
                }
                // Check if it is an attribute.
                else if ((idx = pieces[pieceIdx].indexOf("=")) >= 0) // FelixConstants.ATTRIBUTE_SEPARATOR
                {
                    sep = "="; // FelixConstants.ATTRIBUTE_SEPARATOR
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
                if (sep.equals(":=")) // FelixConstants.DIRECTIVE_SEPARATOR
                {
                    dirs[dirCount++] = new R4Directive(key, value);
                }
                else
                {
                    attrs[attrCount++] = new R4Attribute(key, value, false);
                }
            }

            // Shrink directive array.
            IDirective[] dirsFinal = new IDirective[dirCount];
            System.arraycopy(dirs, 0, dirsFinal, 0, dirCount);
            // Shrink attribute array.
            IAttribute[] attrsFinal = new IAttribute[attrCount];
            System.arraycopy(attrs, 0, attrsFinal, 0, attrCount);

            // Create package attributes for each package and
            // set directives/attributes. Add each package to
            // completel list of packages.
            IPackage[] pkgs = new IPackage[pkgCount];
            for (int pkgIdx = 0; pkgIdx < pkgCount; pkgIdx++)
            {
                pkgs[pkgIdx] = new R4Package(pieces[pkgIdx], dirsFinal, attrsFinal);
                completeList.add(pkgs[pkgIdx]);
            }
        }
    
        IPackage[] ips = (IPackage[])
            completeList.toArray(new IPackage[completeList.size()]);
        return ips;
    }

    public static IVersion[] parseVersionInterval(String interval)
    {
        // Check if the version is an interval.
        if (interval.indexOf(',') >= 0)
        {
            String s = interval.substring(1, interval.length() - 1);
            String vlo = s.substring(0, s.indexOf(','));
            String vhi = s.substring(s.indexOf(',') + 1, s.length());
            return new IVersion[] {
                new R4Version(vlo, (interval.charAt(0) == '[')),
                new R4Version(vhi, (interval.charAt(interval.length() - 1) == ']'))
            };
        }
        else
        {
            return new IVersion[] { new R4Version(interval, true) };
        }
    }

    /**
     * Parses delimited string and returns an array containing the tokens. This
     * parser obeys quotes, so the delimiter character will be ignored if it is
     * inside of a quote. This method assumes that the quote character is not
     * included in the set of delimiter characters.
     * @param value the delimited string to parse.
     * @param delim the characters delimiting the tokens.
     * @return an array of string tokens or null if there were no tokens.
    **/
    public static String[] parseDelimitedString(String value, String delim)
    {
        if (value == null)
        {
           value = "";
        }

        List list = new ArrayList();

        int CHAR = 1;
        int DELIMITER = 2;
        int STARTQUOTE = 4;
        int ENDQUOTE = 8;

        StringBuffer sb = new StringBuffer();

        int expecting = (CHAR | DELIMITER | STARTQUOTE);
        
        for (int i = 0; i < value.length(); i++)
        {
            char c = value.charAt(i);

            boolean isDelimiter = (delim.indexOf(c) >= 0);
            boolean isQuote = (c == '"');

            if (isDelimiter && ((expecting & DELIMITER) > 0))
            {
                list.add(sb.toString().trim());
                sb.delete(0, sb.length());
                expecting = (CHAR | DELIMITER | STARTQUOTE);
            }
            else if (isQuote && ((expecting & STARTQUOTE) > 0))
            {
                sb.append(c);
                expecting = CHAR | ENDQUOTE;
            }
            else if (isQuote && ((expecting & ENDQUOTE) > 0))
            {
                sb.append(c);
                expecting = (CHAR | STARTQUOTE | DELIMITER);
            }
            else if ((expecting & CHAR) > 0)
            {
                sb.append(c);
            }
            else
            {
                throw new IllegalArgumentException("Invalid delimited string: " + value);
            }
        }

        if (sb.length() > 0)
        {
            list.add(sb.toString().trim());
        }

        return (String[]) list.toArray(new String[list.size()]);
    }
}