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
    private final Version m_low;
    private final boolean m_isLowInclusive;
    private final Version m_high;
    private final boolean m_isHighInclusive;
    public static final VersionRange infiniteRange = new VersionRange(Version.emptyVersion, true, null, true);

    public VersionRange(
        Version low, boolean isLowInclusive,
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

    public boolean intersects(VersionRange vr)
    {
        // Check to see if the passed in floor is less than or equal to
        // this ceiling and the passed in ceiling is greater than or
        // equal to this floor.
        boolean isFloorLessThanCeiling = false;
        if ((m_high == null)
            || (m_high.compareTo(vr.getLow()) > 0)
            || ((m_high.compareTo(vr.getLow()) == 0)
                && m_isHighInclusive && vr.isLowInclusive()))
        {
            isFloorLessThanCeiling = true;
        }
        boolean isCeilingGreaterThanFloor = false;
        if ((vr.getHigh() == null)
            || (m_low.compareTo(vr.getHigh()) < 0)
            || ((m_low.compareTo(vr.getHigh()) == 0)
                && m_isLowInclusive && vr.isHighInclusive()))
        {
            isCeilingGreaterThanFloor = true;
        }
        return isFloorLessThanCeiling && isCeilingGreaterThanFloor;
    }

    public VersionRange intersection(VersionRange vr)
    {
        if (!intersects(vr))
        {
            return null;
        }

        VersionRange floor = (m_low.compareTo(vr.getLow()) > 0) ? this : vr;
        boolean floorInclusive = (getLow().equals(vr.getLow()))
            ? (isLowInclusive() & vr.isLowInclusive())
            : floor.isLowInclusive();

        VersionRange ceiling;
        boolean ceilingInclusive;
        if (vr.getHigh() == null)
        {
            ceiling = this;
            ceilingInclusive = ceiling.isHighInclusive();
        }
        else if (m_high == null)
        {
            ceiling = vr;
            ceilingInclusive = ceiling.isHighInclusive();
        }
        else if (m_high.compareTo(vr.getHigh()) > 0)
        {
            ceiling = vr;
            ceilingInclusive = ceiling.isHighInclusive();
        }
        else if (m_high.compareTo(vr.getHigh()) < 0)
        {
            ceiling = this;
            ceilingInclusive = ceiling.isHighInclusive();
        }
        else
        {
            ceiling = this;
            ceilingInclusive = (isHighInclusive() & vr.isHighInclusive());
        }

        return new VersionRange(
            floor.getLow(), floorInclusive, ceiling.getHigh(), ceilingInclusive);
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
        if (m_low != other.m_low && (m_low == null || !m_low.equals(other.m_low)))
        {
            return false;
        }
        if (m_isLowInclusive != other.m_isLowInclusive)
        {
            return false;
        }
        if (m_high != other.m_high && (m_high == null || !m_high.equals(other.m_high)))
        {
            return false;
        }
        if (m_isHighInclusive != other.m_isHighInclusive)
        {
            return false;
        }
        return true;
    }

    public int hashCode()
    {
        int hash = 5;
        hash = 97 * hash + (m_low != null ? m_low.hashCode() : 0);
        hash = 97 * hash + (m_isLowInclusive ? 1 : 0);
        hash = 97 * hash + (m_high != null ? m_high.hashCode() : 0);
        hash = 97 * hash + (m_isHighInclusive ? 1 : 0);
        return hash;
    }

    public String toString()
    {
        if (m_high != null)
        {
            StringBuffer sb = new StringBuffer();
            sb.append(m_isLowInclusive ? '[' : '(');
            sb.append(m_low.toString());
            sb.append(',');
            sb.append(m_high.toString());
            sb.append(m_isHighInclusive ? ']' : ')');
            return sb.toString();
        }
        else
        {
            return m_low.toString();
        }
    }
}