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
import org.osgi.framework.*;

public class Requirement implements IRequirement
{
    private final String m_namespace;
    private final R4Directive[] m_directives;
    private final R4Attribute[] m_attributes;
    private final boolean m_isOptional;

    private final String m_targetName;
    private final VersionRange m_targetVersionRange;
    private volatile Filter m_filter;

    public Requirement(String namespace, String filterStr) throws InvalidSyntaxException
    {
        m_namespace = namespace;
        m_filter = new FilterImpl(filterStr);
        m_directives = null;
        m_attributes = null;
        m_isOptional = false;
        m_targetName = null;
        m_targetVersionRange = null;
    }

    public Requirement(String namespace, R4Directive[] directives, R4Attribute[] attributes)
    {
        m_namespace = namespace;
        m_directives = directives;
        m_attributes = attributes;
        m_filter = null;

        // Find all import directives: resolution.
        boolean optional = false;
        for (int i = 0; (m_directives != null) && (i < m_directives.length); i++)
        {
            if (m_directives[i].getName().equals(Constants.RESOLUTION_DIRECTIVE))
            {
                optional = m_directives[i].getValue().equals(Constants.RESOLUTION_OPTIONAL);
            }
        }
        m_isOptional = optional;

        String targetName = null;
        VersionRange targetVersionRange = VersionRange.infiniteRange;
        for (int i = 0; i < m_attributes.length; i++)
        {
            if (m_namespace.equals(ICapability.MODULE_NAMESPACE))
            {
                if (m_attributes[i].getName().equals(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    targetName = (String) m_attributes[i].getValue();
                }
                else if (m_attributes[i].getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE))
                {
                    targetVersionRange = (VersionRange) m_attributes[i].getValue();
                }
            }
            else if (m_namespace.equals(ICapability.PACKAGE_NAMESPACE))
            {
                if (m_attributes[i].getName().equals(ICapability.PACKAGE_PROPERTY))
                {
                    targetName = (String) m_attributes[i].getValue();
                }
                else if (m_attributes[i].getName().equals(ICapability.VERSION_PROPERTY))
                {
                    targetVersionRange = (VersionRange) m_attributes[i].getValue();
                }
            }
            else if (m_namespace.equals(ICapability.HOST_NAMESPACE))
            {
                if (m_attributes[i].getName().equals(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE))
                {
                    targetName = (String) m_attributes[i].getValue();
                }
                else if (m_attributes[i].getName().equals(Constants.BUNDLE_VERSION_ATTRIBUTE))
                {
                    targetVersionRange = (VersionRange) m_attributes[i].getValue();
                }
            }
        }
        m_targetName = targetName;
        m_targetVersionRange = targetVersionRange;
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public Filter getFilter()
    {
        if (m_filter == null)
        {
            m_filter = convertToFilter();
        }
        return m_filter;
    }

// TODO: RB - We need to verify that the resolver code does not
// touch these implementation-specific methods.

    public String getTargetName()
    {
        return m_targetName;
    }

    public VersionRange getTargetVersionRange()
    {
        return m_targetVersionRange;
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

    public boolean isMultiple()
    {
        return false;
    }

    public boolean isOptional()
    {
        return m_isOptional;
    }

    public String getComment()
    {
        return "Comment for " + toString();
    }

    public boolean isSatisfied(ICapability capability)
    {
        // If the requirement was constructed with a filter, then
        // we must use that filter for evaluation.
        if ((m_attributes == null) && (m_filter != null))
        {
            return m_namespace.equals(capability.getNamespace()) &&
                getFilter().match(new MapToDictionary(capability.getProperties()));
        }
        // Otherwise, if the requirement was constructed with attributes, then
        // perform the evaluation manually instead of using the filter for
        // performance reasons.
        else if (m_attributes != null)
        {
            return capability.getNamespace().equals(getNamespace()) &&
                doAttributesMatch((Capability) capability);
        }

        return false;
    }

    private boolean doAttributesMatch(Capability ec)
    {
        // Grab the capability's attributes.
        R4Attribute[] capAttrs = ec.getAttributes();

        // Cycle through all attributes of this import package
        // and make sure its values match the attribute values
        // of the specified export package.
        for (int reqAttrIdx = 0; reqAttrIdx < m_attributes.length; reqAttrIdx++)
        {
            // Get current attribute from this import package.
            R4Attribute reqAttr = m_attributes[reqAttrIdx];

            // Check if the export package has the same attribute.
            boolean found = false;
            for (int capAttrIdx = 0;
                (!found) && (capAttrIdx < capAttrs.length);
                capAttrIdx++)
            {
                // Get current attribute for the export package.
                R4Attribute capAttr = capAttrs[capAttrIdx];
                // Check if the attribute names are equal.
                if (reqAttr.getName().equals(capAttr.getName()))
                {
                    // We only recognize version types. If the value of the
                    // attribute is a version/version range, then we use the
                    // "in range" comparison, otherwise we simply use equals().
                    if (capAttr.getValue() instanceof Version)
                    {
                        if (!((VersionRange) reqAttr.getValue()).isInRange((Version) capAttr.getValue()))
                        {
                            return false;
                        }
                    }
                    else if (capAttr.getValue() instanceof Object[])
                    {
                        Object[] values = (Object[]) capAttr.getValue();
                        boolean matched = false;
                        for (int valIdx = 0; !matched && (valIdx < values.length); valIdx++)
                        {
                            if (reqAttr.getValue().equals(values[valIdx]))
                            {
                                matched = true;
                            }
                        }
                        if (!matched)
                        {
                            return false;
                        }
                    }
                    else if (!reqAttr.getValue().equals(capAttr.getValue()))
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
        for (int capAttrIdx = 0; capAttrIdx < capAttrs.length; capAttrIdx++)
        {
            // Get current attribute for this package.
            R4Attribute capAttr = capAttrs[capAttrIdx];

            // If the export attribute is mandatory, then make sure
            // this import package has the attribute.
            if (capAttr.isMandatory())
            {
                boolean found = false;
                for (int reqAttrIdx = 0;
                    (!found) && (reqAttrIdx < m_attributes.length);
                    reqAttrIdx++)
                {
                    // Get current attribute from specified package.
                    R4Attribute reqAttr = m_attributes[reqAttrIdx];

                    // Check if the attribute names are equal
                    // and set found flag.
                    if (capAttr.getName().equals(reqAttr.getName()))
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

    private Filter convertToFilter()
    {
        StringBuffer sb = new StringBuffer();
        if ((m_attributes != null) && (m_attributes.length > 1))
        {
            sb.append("(&");
        }
        for (int i = 0; (m_attributes != null) && (i < m_attributes.length); i++)
        {
            // If this is a package import, then convert wild-carded
            // dynamically imported package names to an OR comparison.
            if (m_namespace.equals(ICapability.PACKAGE_NAMESPACE) &&
                m_attributes[i].getName().equals(ICapability.PACKAGE_PROPERTY) &&
                m_attributes[i].getValue().toString().endsWith(".*"))
            {
                int idx = m_attributes[i].getValue().toString().indexOf(".*");
                sb.append("(|(package=");
                sb.append(m_attributes[i].getValue().toString().substring(0, idx));
                sb.append(")(package=");
                sb.append(m_attributes[i].getValue().toString());
                sb.append("))");
            }
            else if (m_attributes[i].getValue() instanceof VersionRange)
            {
                VersionRange vr = (VersionRange) m_attributes[i].getValue();
                if (vr.isLowInclusive())
                {
                    sb.append("(");
                    sb.append(m_attributes[i].getName());
                    sb.append(">=");
                    sb.append(vr.getLow().toString());
                    sb.append(")");
                }
                else
                {
                    sb.append("(!(");
                    sb.append(m_attributes[i].getName());
                    sb.append("<=");
                    sb.append(vr.getLow().toString());
                    sb.append("))");
                }

                if (vr.getHigh() != null)
                {
                    if (vr.isHighInclusive())
                    {
                        sb.append("(");
                        sb.append(m_attributes[i].getName());
                        sb.append("<=");
                        sb.append(vr.getHigh().toString());
                        sb.append(")");
                    }
                    else
                    {
                        sb.append("(!(");
                        sb.append(m_attributes[i].getName());
                        sb.append(">=");
                        sb.append(vr.getHigh().toString());
                        sb.append("))");
                    }
                }
            }
            else
            {
                sb.append("(");
                sb.append(m_attributes[i].getName());
                sb.append("=");
                sb.append(m_attributes[i].getValue().toString());
                sb.append(")");
            }
        }

        if ((m_attributes != null) && (m_attributes.length > 1))
        {
            sb.append(")");
        }

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

    public String toString()
    {
        return getNamespace() + "; " + getFilter().toString();
    }
}