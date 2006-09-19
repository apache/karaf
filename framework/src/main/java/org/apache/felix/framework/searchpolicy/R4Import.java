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

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class R4Import extends R4Package
{
    private VersionRange m_versionRange = null;
    private boolean m_isOptional = false;

    public R4Import(R4Package pkg)
    {
        this(pkg.getName(), pkg.getDirectives(), pkg.getAttributes());
    }

    public R4Import(String name, R4Directive[] directives, R4Attribute[] attrs)
    {
        super(name, directives, attrs);

        // Find all import directives: resolution.
        for (int i = 0; i < m_directives.length; i++)
        {
            if (m_directives[i].getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                m_isOptional = m_directives[i].getValue().equals(Constants.RESOLUTION_OPTIONAL);
            }
        }

        // Convert version and bundle version attributes to VersionRange.
        // The attribute value may be a String or a Version, since the
        // value may be coming from an R4Export that already converted
        // it to Version.
        m_versionRange = VersionRange.parse(Version.emptyVersion.toString());
        m_version = m_versionRange.getLow();
        for (int i = 0; i < m_attrs.length; i++)
        {
            if (m_attrs[i].getName().equals(Constants.VERSION_ATTRIBUTE))
            {
                String versionStr = (m_attrs[i].getValue() instanceof Version)
                    ? ((Version) m_attrs[i].getValue()).toString()
                    : (String) m_attrs[i].getValue();
                m_versionRange = VersionRange.parse(versionStr);
                m_version = m_versionRange.getLow();
                m_attrs[i] = new R4Attribute(
                    m_attrs[i].getName(),
                    m_versionRange,
                    m_attrs[i].isMandatory());
            }
            else if (m_attrs[i].getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE))
            {
                String versionStr = (m_attrs[i].getValue() instanceof Version)
                    ? ((Version) m_attrs[i].getValue()).toString()
                    : (String) m_attrs[i].getValue();
                m_attrs[i] = new R4Attribute(
                    m_attrs[i].getName(),
                    VersionRange.parse(versionStr),
                    m_attrs[i].isMandatory());
            }
        }
    }

    public Version getVersionHigh()
    {
        return m_versionRange.getHigh();
    }

    public boolean isLowInclusive()
    {
        return m_versionRange.isLowInclusive();
    }

    public boolean isHighInclusive()
    {
        return m_versionRange.isHighInclusive();
    }

    public boolean isOptional()
    {
        return m_isOptional;
    }

    public boolean isSatisfied(R4Export export)
    {
        // For packages to be compatible, they must have the
        // same name.
        if (!getName().equals(export.getName()))
        {
            return false;
        }
        
        return m_versionRange.isInRange(export.getVersion())
            && doAttributesMatch(export);
    }

    private boolean doAttributesMatch(R4Export export)
    {
        // Cycle through all attributes of this import package
        // and make sure its values match the attribute values
        // of the specified export package.
        for (int impAttrIdx = 0; impAttrIdx < getAttributes().length; impAttrIdx++)
        {
            // Get current attribute from this import package.
            R4Attribute impAttr = getAttributes()[impAttrIdx];

            // Ignore version attribute, since it is a special case that
            // has already been compared using isVersionInRange() before
            // the call to this method was made.
            if (impAttr.getName().equals(Constants.VERSION_ATTRIBUTE))
            {
                continue;
            }

            // Check if the export package has the same attribute.
            boolean found = false;
            for (int expAttrIdx = 0;
                (!found) && (expAttrIdx < export.getAttributes().length);
                expAttrIdx++)
            {
                // Get current attribute for the export package.
                R4Attribute expAttr = export.getAttributes()[expAttrIdx];
                // Check if the attribute names are equal.
                if (impAttr.getName().equals(expAttr.getName()))
                {
                    // We only recognize version types. If the value of the
                    // attribute is a version/version range, then we use the
                    // "in range" comparison, otherwise we simply use equals().
                    if (expAttr.getValue() instanceof Version)
                    {
                        if (!((VersionRange) impAttr.getValue()).isInRange((Version) expAttr.getValue()))
                        {
                            return false;
                        }
                    }
                    else if (!impAttr.getValue().equals(expAttr.getValue()))
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

        // Now, cycle through all attributes of the export package and verify that
        // all mandatory attributes are present in this import package.
        for (int expAttrIdx = 0; expAttrIdx < export.getAttributes().length; expAttrIdx++)
        {
            // Get current attribute for this package.
            R4Attribute expAttr = export.getAttributes()[expAttrIdx];
            
            // If the export attribute is mandatory, then make sure
            // this import package has the attribute.
            if (expAttr.isMandatory())
            {
                boolean found = false;
                for (int impAttrIdx = 0;
                    (!found) && (impAttrIdx < getAttributes().length);
                    impAttrIdx++)
                {
                    // Get current attribute from specified package.
                    R4Attribute impAttr = getAttributes()[impAttrIdx];
        
                    // Check if the attribute names are equal
                    // and set found flag.
                    if (expAttr.getName().equals(impAttr.getName()))
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
}