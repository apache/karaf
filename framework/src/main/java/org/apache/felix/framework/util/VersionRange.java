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
package org.apache.felix.framework.util;

import org.osgi.framework.Version;

public class VersionRange
{
    private final Version m_floor;
    private final boolean m_isFloorInclusive;
    private final Version m_ceiling;
    private final boolean m_isCeilingInclusive;
    public static final VersionRange infiniteRange
        = new VersionRange(Version.emptyVersion, true, null, true);

    public VersionRange(
        Version low, boolean isLowInclusive,
        Version high, boolean isHighInclusive)
    {
        m_floor = low;
        m_isFloorInclusive = isLowInclusive;
        m_ceiling = high;
        m_isCeilingInclusive = isHighInclusive;
    }

    public Version getFloor()
    {
        return m_floor;
    }

    public boolean isFloorInclusive()
    {
        return m_isFloorInclusive;
    }

    public Version getCeiling()
    {
        return m_ceiling;
    }

    public boolean isCeilingInclusive()
    {
        return m_isCeilingInclusive;
    }

    public boolean isInRange(Version version)
    {
        // We might not have an upper end to the range.
        if (m_ceiling == null)
        {
            return (version.compareTo(m_floor) >= 0);
        }
        else if (isFloorInclusive() && isCeilingInclusive())
        {
            return (version.compareTo(m_floor) >= 0) && (version.compareTo(m_ceiling) <= 0);
        }
        else if (isCeilingInclusive())
        {
            return (version.compareTo(m_floor) > 0) && (version.compareTo(m_ceiling) <= 0);
        }
        else if (isFloorInclusive())
        {
            return (version.compareTo(m_floor) >= 0) && (version.compareTo(m_ceiling) < 0);
        }
        return (version.compareTo(m_floor) > 0) && (version.compareTo(m_ceiling) < 0);
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

    public boolean equals(Object obj)
    {
        if (obj == null)
        {
            return false;
        }
        if (getClass() != obj.getClass())
        {
            return false;
        }
        final VersionRange other = (VersionRange) obj;
        if (m_floor != other.m_floor && (m_floor == null || !m_floor.equals(other.m_floor)))
        {
            return false;
        }
        if (m_isFloorInclusive != other.m_isFloorInclusive)
        {
            return false;
        }
        if (m_ceiling != other.m_ceiling && (m_ceiling == null || !m_ceiling.equals(other.m_ceiling)))
        {
            return false;
        }
        if (m_isCeilingInclusive != other.m_isCeilingInclusive)
        {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int hash = 5;
        hash = 97 * hash + (m_floor != null ? m_floor.hashCode() : 0);
        hash = 97 * hash + (m_isFloorInclusive ? 1 : 0);
        hash = 97 * hash + (m_ceiling != null ? m_ceiling.hashCode() : 0);
        hash = 97 * hash + (m_isCeilingInclusive ? 1 : 0);
        return hash;
    }

    public String toString()
    {
        if (m_ceiling != null)
        {
            StringBuffer sb = new StringBuffer();
            sb.append(m_isFloorInclusive ? '[' : '(');
            sb.append(m_floor.toString());
            sb.append(',');
            sb.append(m_ceiling.toString());
            sb.append(m_isCeilingInclusive ? ']' : ')');
            return sb.toString();
        }
        else
        {
            return m_floor.toString();
        }
    }
}