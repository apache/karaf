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
package org.apache.felix.bundlerepository;

import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class R4Package
{
    private String m_name = "";
    protected R4Directive[] m_directives = null;
    protected R4Attribute[] m_attrs = null;
    protected Version m_version = null;

    public R4Package(String name, R4Directive[] directives, R4Attribute[] attrs)
    {
        m_name = name;
        m_directives = (directives == null) ? new R4Directive[0] : directives;
        m_attrs = (attrs == null) ? new R4Attribute[0] : attrs;

        // Find and parse version attribute, if present.
        String rangeStr = "0.0.0";
        for (int i = 0; i < m_attrs.length; i++)
        {
            if (m_attrs[i].getName().equals(Constants.VERSION_ATTRIBUTE) ||
                m_attrs[i].getName().equals(Constants.PACKAGE_SPECIFICATION_VERSION))
            {
                // Normalize version attribute name.
                m_attrs[i] = new R4Attribute(
                    Constants.VERSION_ATTRIBUTE, m_attrs[i].getValue(),
                    m_attrs[i].isMandatory());
                rangeStr = m_attrs[i].getValue();
                break;
            }
        }
        
        VersionRange range = VersionRange.parse(rangeStr);
        // For now, ignore if we have a version range.
        m_version = range.getLow();
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
            String[] ss = Util.parseDelimitedString(s, ",");
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
            String[] pieces = Util.parseDelimitedString(ss[ssIdx], ";");

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
                if ((idx = pieces[pieceIdx].indexOf(":=")) >= 0)
                {
                    sep = ":=";
                }
                // Check if it is an attribute.
                else if ((idx = pieces[pieceIdx].indexOf("=")) >= 0)
                {
                    sep = "=";
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
                if (sep.equals(":="))
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
    
        R4Package[] pkgs = (R4Package[])
            completeList.toArray(new R4Package[completeList.size()]);
        return pkgs;
    }
}