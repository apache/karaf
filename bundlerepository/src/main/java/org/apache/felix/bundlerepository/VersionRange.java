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

import org.osgi.framework.Version;

public class VersionRange
{
    private Version m_low = null;
    private boolean m_isLowInclusive = false;
    private Version m_high = null;
    private boolean m_isHighInclusive = false;
    private String m_toString = null;
    public static final VersionRange infiniteRange = new VersionRange(Version.emptyVersion, true, null, true);

    public VersionRange(Version low, boolean isLowInclusive,
        Version high, boolean isHighInclusive)
    {
        m_low = low;
        m_isLowInclusive = isLowInclusive;
        m_high = high;
        m_isHighInclusive = isHighInclusive;
    }

    public Version getLow()
    {
        return m_low;
    }

    public boolean isLowInclusive()
    {
        return m_isLowInclusive;
    }

    public Version getHigh()
    {
        return m_high;
    }

    public boolean isHighInclusive()
    {
        return m_isHighInclusive;
    }

    public boolean isInRange(Version version)
    {
        // We might not have an upper end to the range.
        if (m_high == null)
        {
            return (version.compareTo(m_low) >= 0);
        }
        else if (isLowInclusive() && isHighInclusive())
        {
            return (version.compareTo(m_low) >= 0) && (version.compareTo(m_high) <= 0);
        }
        else if (isHighInclusive())
        {
            return (version.compareTo(m_low) > 0) && (version.compareTo(m_high) <= 0);
        }
        else if (isLowInclusive())
        {
            return (version.compareTo(m_low) >= 0) && (version.compareTo(m_high) < 0);
        }
        return (version.compareTo(m_low) > 0) && (version.compareTo(m_high) < 0);
    }

    public static VersionRange parse(String range)
    {
        // Check if the version is an interval.
        if (range.indexOf(',') >= 0)
        {
            String s = range.substring(1, range.length() - 1);
            String vlo = s.substring(0, s.indexOf(',')).trim();
            String vhi = s.substring(s.indexOf(',') + 1, s.length()).trim();
            return new VersionRange (
                new Version(vlo), (range.charAt(0) == '['),
                new Version(vhi), (range.charAt(range.length() - 1) == ']'));
        }
        else
        {
            return new VersionRange(new Version(range), true, null, false);
        }
    }

    public String toString()
    {
        if (m_toString == null)
        {
            if (m_high != null)
            {
                StringBuffer sb = new StringBuffer();
                sb.append(m_isLowInclusive ? '[' : '(');
                sb.append(m_low.toString());
                sb.append(',');
                sb.append(m_high.toString());
                sb.append(m_isHighInclusive ? ']' : ')');
                m_toString = sb.toString();
            }
            else
            {
                m_toString = m_low.toString();
            }
        }
        return m_toString;
    }
}
