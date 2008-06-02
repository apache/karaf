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
package org.apache.felix.deploymentadmin;

import org.osgi.framework.Version;

/**
 * This class represents a version range as defined in section 3.2.5 of the OSGi r4 specification.
 */
public class VersionRange {

    public static final VersionRange infiniteRange = new VersionRange(Version.emptyVersion, true, null, true);

    private Version m_low = null;
    private boolean m_isLowInclusive = false;
    private Version m_high = null;
    private boolean m_isHighInclusive = false;
    private String m_toString = null;

    /**
     * Create an instance of the VersionRange class.
     *
     * @param low Lower bound version
     * @param isLowInclusive True if lower bound should be included in the range
     * @param high Upper bound version
     * @param isHighInclusive True if upper bound should be included in the range
     */
    public VersionRange(Version low, boolean isLowInclusive, Version high, boolean isHighInclusive) {
        m_low = low;
        m_isLowInclusive = isLowInclusive;
        m_high = high;
        m_isHighInclusive = isHighInclusive;
    }

    /**
     * Creates an instance of the VersionRange class which resembles [version,*)
     *
     * @param version The lower boundary of the version range
     */
    public VersionRange(Version version) {
        this(version, true, null, false);
    }

    /**
     * Get the lower boundary of the version range, the boundary being inclusive or not is not taken into account.
     *
     * @return Version resembling the lower boundary of the version range
     */
    public Version getLow() {
        return m_low;
    }

    /**
     * Determines whether the lower boundary is inclusive or not.
     *
     * @return True if the lower boundary is inclusive, false otherwise.
     */
    public boolean isLowInclusive() {
        return m_isLowInclusive;
    }

    /**
     * Get the upper boundary of the version range, the boundary being inclusive or not is not taken in to account.
     *
     * @return Version resembling the upper boundary of the version range.
     */
    public Version getHigh() {
        return m_high;
    }

    /**
     * Determines whether the upper boundary is inclusive or not.
     *
     * @return True if the upper boundary is inclusive, false otherwise.
     */
    public boolean isHighInclusive() {
        return m_isHighInclusive;
    }

    /**
     * Determine if the specified version is part of the version range or not.
     *
     * @param version The version to verify
     * @return True if the specified version is included in this version range, false otherwise.
     */
    public boolean isInRange(Version version) {
        // We might not have an upper end to the range.
        if (m_high == null) {
            return (version.compareTo(m_low) >= 0);
        }
        else if (isLowInclusive() && isHighInclusive()) {
            return (version.compareTo(m_low) >= 0) && (version.compareTo(m_high) <= 0);
        }
        else if (isHighInclusive()) {
            return (version.compareTo(m_low) > 0) && (version.compareTo(m_high) <= 0);
        }
        else if (isLowInclusive()) {
            return (version.compareTo(m_low) >= 0) && (version.compareTo(m_high) < 0);
        }
        return (version.compareTo(m_low) > 0) && (version.compareTo(m_high) < 0);
    }

    /**
     * Parses a version range from the specified string.
     *
     * @param range String representation of the version range.
     * @return A <code>VersionRange</code> object representing the version range.
     * @throws IllegalArgumentException If <code>range</code> is improperly formatted.
     */
    public static VersionRange parse(String range) throws IllegalArgumentException {
        // Check if the version is an interval.
        if (range.indexOf(',') >= 0) {
            String s = range.substring(1, range.length() - 1);
            String vlo = s.substring(0, s.indexOf(',')).trim();
            String vhi = s.substring(s.indexOf(',') + 1, s.length()).trim();
            return new VersionRange(new Version(vlo), (range.charAt(0) == '['), new Version(vhi), (range.charAt(range.length() - 1) == ']'));
        }
        else {
            return new VersionRange(new Version(range), true, null, false);
        }
    }

    public String toString() {
        if (m_toString == null) {
            if (m_high != null) {
                StringBuffer sb = new StringBuffer();
                sb.append(m_isLowInclusive ? '[' : '(');
                sb.append(m_low.toString());
                sb.append(',');
                sb.append(m_high.toString());
                sb.append(m_isHighInclusive ? ']' : ')');
                m_toString = sb.toString();
            }
            else {
                m_toString = m_low.toString();
            }
        }
        return m_toString;
    }
}
