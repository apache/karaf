/*
 *   Copyright 2006 The Apache Software Foundation
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
import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class R4Package
{
    private String m_name = "";
    protected R4Directive[] m_directives = null;
    protected R4Attribute[] m_attrs = null;
    protected Version m_version = new Version("0.0.0");

    public R4Package(String name, R4Directive[] directives, R4Attribute[] attrs)
    {
        m_name = name;
        m_directives = (directives == null) ? new R4Directive[0] : directives;
        m_attrs = (attrs == null) ? new R4Attribute[0] : attrs;
    }

    public String getName()
    {
        return m_name;
    }

    public R4Directive[] getDirectives()
    {
        return m_directives;
    }

    public R4Attribute[] getAttributes()
    {
        return m_attrs;
    }

    public Version getVersion()
    {
        return m_version;
    }


    public String toString()
    {
        String msg = getName();
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
            Map dirsMap = new HashMap();
            Map attrsMap = new HashMap();
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
                    // Check for duplicates.
                    if (dirsMap.get(key) != null)
                    {
                        throw new IllegalArgumentException(
                            "Duplicate directive: " + key);
                    }
                    dirsMap.put(key, new R4Directive(key, value));
                }
                else
                {
                    // Check for duplicates.
                    if (attrsMap.get(key) != null)
                    {
                        throw new IllegalArgumentException(
                            "Duplicate attribute: " + key);
                    }
                    attrsMap.put(key, new R4Attribute(key, value, false));
                }
            }

            // Check for "version" and "specification-version" attributes
            // and verify they are the same if both are specified.
            R4Attribute v = (R4Attribute) attrsMap.get(Constants.VERSION_ATTRIBUTE);
            R4Attribute sv = (R4Attribute) attrsMap.get(Constants.PACKAGE_SPECIFICATION_VERSION);
            if ((v != null) && (sv != null))
            {
                // Verify they are equal.
                if (!v.getValue().trim().equals(sv.getValue().trim()))
                {
                    throw new IllegalArgumentException(
                        "Both version and specificat-version are specified, but they are not equal.");
                }
                // Remove spec-version since it isn't needed.
                attrsMap.remove(Constants.PACKAGE_SPECIFICATION_VERSION);
            }

            // Create directive array.
            R4Directive[] dirs = (R4Directive[])
                dirsMap.values().toArray(new R4Directive[dirsMap.size()]);

            // Create attribute array.
            R4Attribute[] attrs = (R4Attribute[])
                attrsMap.values().toArray(new R4Attribute[attrsMap.size()]);

            // Create package attributes for each package and
            // set directives/attributes. Add each package to
            // completel list of packages.
            R4Package[] pkgs = new R4Package[pkgCount];
            for (int pkgIdx = 0; pkgIdx < pkgCount; pkgIdx++)
            {
                pkgs[pkgIdx] = new R4Package(pieces[pkgIdx], dirs, attrs);
                completeList.add(pkgs[pkgIdx]);
            }
        }
    
        R4Package[] pkgs = (R4Package[])
            completeList.toArray(new R4Package[completeList.size()]);
        return pkgs;
    }
}