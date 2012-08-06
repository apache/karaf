/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.karaf.region.persist.internal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.framework.Version;

public final class VersionRange {

    /** A string representation of the version. */
    private String version;

    /** The minimum desired version for the bundle */
    private Version minimumVersion;

    /** The maximum desired version for the bundle */
    private Version maximumVersion;

    /** True if the match is exclusive of the minimum version */
    private boolean minimumExclusive;

    /** True if the match is exclusive of the maximum version */
    private boolean maximumExclusive;

    /** A regexp to select the version */
    private static final Pattern versionCapture = Pattern.compile("\"?(.*?)\"?$");

    /**
     *
     * @param version
     *            version for the verioninfo
     */
    public VersionRange(String version) {
        this.version = version;
        processVersionAttribute(version);
    }

    /**
     * This method should be used to create a version range from a single
     * version string.
     * @param version
     *            version for the versioninfo
     * @param exactVersion
     *            whether this is an exact version {@code true} or goes to infinity
     *            {@code false}
     */
    public VersionRange(String version, boolean exactVersion) {

        if (exactVersion) {
            // Do not store this string as it might be just a version, or a range!
            processExactVersionAttribute(version);
        } else {
            this.version = version;
            processVersionAttribute(this.version);
        }

        assertInvariants();
    }

    /**
     * Constructor designed for internal use only.
     *
     * @param maximumVersion
     * @param maximumExclusive
     * @param minimumVersion
     * @param minimumExclusive
     * @throws IllegalArgumentException
     *             if parameters are not valid.
     */
    private VersionRange(Version maximumVersion,
                         boolean maximumExclusive,
                         Version minimumVersion,
                         boolean minimumExclusive) {
        this.maximumVersion = maximumVersion;
        this.maximumExclusive = maximumExclusive;
        this.minimumVersion = minimumVersion;
        this.minimumExclusive = minimumExclusive;

        assertInvariants();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.apache.aries.application.impl.VersionRange#toString()
     */
    @Override
    public String toString() {
        // Some constructors don't take in a string that we can return directly,
        // so construct one if needed
        if (version == null) {
            if (maximumVersion == null) {
                version = minimumVersion.toString();
            } else {
                version = (minimumExclusive ? "(" : "[") + minimumVersion + "," + maximumVersion
                          + (maximumExclusive ? ")" : "]");
            }
        }
        return this.version;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + minimumVersion.hashCode();
        result = 31 * result + (minimumExclusive ? 1 : 0);
        result = 31 * result + (maximumVersion != null ? maximumVersion.hashCode() : 0);
        result = 31 * result + (maximumExclusive ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (this == other) {
            result = true;
        } else if (other instanceof VersionRange) {
            VersionRange vr = (VersionRange) other;
            result = minimumVersion.equals(vr.minimumVersion)
                     && minimumExclusive == vr.minimumExclusive
                     && (maximumVersion == null ? vr.maximumVersion == null : maximumVersion
                             .equals(vr.maximumVersion)) && maximumExclusive == vr.maximumExclusive;
        }

        return result;
    }

    /**
     * this method returns the exact version from the versionInfo obj.
     * this is used for DeploymentContent only to return a valid exact version
     * otherwise, null is returned.
     * @return the exact version
     */
    public Version getExactVersion() {
        Version v = null;
        if (isExactVersion()) {
            v = getMinimumVersion();
        }
        return v;
    }

    /**
     * get the maximum version
     * @return    the maximum version
     */
    public Version getMaximumVersion() {
        return maximumVersion;
    }

    /**
     * get the minimum version
     * @return    the minimum version
     */
    public Version getMinimumVersion() {
        return minimumVersion;
    }

    /**
     * is the maximum version exclusive
     * @return is the max version in the range.
     */
    public boolean isMaximumExclusive() {
        return maximumExclusive;
    }

    /**
     * is the maximum version unbounded
     * @return true if no upper bound was specified.
     */
    public boolean isMaximumUnbounded() {
        boolean unbounded = maximumVersion == null;
        return unbounded;
    }

    /**
     * is the minimum version exclusive
     * @return true if the min version is in range.
     */
    public boolean isMinimumExclusive() {
        return minimumExclusive;
    }

    /**
     * this is designed for deployed-version as that is the exact version.
     *
     * @param version
     * @return
     * @throws IllegalArgumentException
     */
    private boolean processExactVersionAttribute(String version) throws IllegalArgumentException {
        boolean success = processVersionAttribute(version);

        if (maximumVersion == null) {
            maximumVersion = minimumVersion;
        }

        if (!minimumVersion.equals(maximumVersion)) {
            throw new IllegalArgumentException("Version is not exact: " + version);
        }

        if (!!!isExactVersion()) {
            throw new IllegalArgumentException("Version is not exact: " + version);
        }

        return success;
    }

    /**
     * process the version attribute,
     *
     * @param version
     *            the value to be processed
     * @return
     * @throws IllegalArgumentException
     */
    private boolean processVersionAttribute(String version) throws IllegalArgumentException {
        boolean success = false;

        if (version == null) {
            throw new IllegalArgumentException("Version is null");
        }

        Matcher matches = versionCapture.matcher(version);

        if (matches.matches()) {
            String versions = matches.group(1);

            if ((versions.startsWith("[") || versions.startsWith("("))
                && (versions.endsWith("]") || versions.endsWith(")"))) {
                if (versions.startsWith("["))
                    minimumExclusive = false;
                else if (versions.startsWith("("))
                    minimumExclusive = true;

                if (versions.endsWith("]"))
                    maximumExclusive = false;
                else if (versions.endsWith(")"))
                    maximumExclusive = true;

                int index = versions.indexOf(',');
                String minVersion = versions.substring(1, index);
                String maxVersion = versions.substring(index + 1, versions.length() - 1);

                try {
                    minimumVersion = new Version(minVersion.trim());
                    maximumVersion = new Version(maxVersion.trim());
                    success = true;
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Version cannot be decoded: " + version, nfe);
                }
            } else {
                try {
                    if (versions.trim().length() == 0)
                        minimumVersion = new Version(0, 0, 0);
                    else
                        minimumVersion = new Version(versions.trim());
                    success = true;
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException("Version cannot be decoded: " + version, nfe);
                }
            }
        } else {
            throw new IllegalArgumentException("Version cannot be decoded: " + version);
        }

        return success;
    }

    /**
     * Assert object invariants. Called by constructors to verify that arguments
     * were valid.
     *
     * @throws IllegalArgumentException
     *             if invariants are violated.
     */
    private void assertInvariants() {
        if (minimumVersion == null
            || !isRangeValid(minimumVersion, minimumExclusive, maximumVersion, maximumExclusive)) {
            IllegalArgumentException e = new IllegalArgumentException();
            throw e;
        }
    }

    /**
     * Check if the supplied parameters describe a valid version range.
     *
     * @param min
     *            the minimum version.
     * @param minExclusive
     *            whether the minimum version is exclusive.
     * @param max
     *            the maximum version.
     * @param maxExclusive
     *            whether the maximum version is exclusive.
     * @return true is the range is valid; otherwise false.
     */
    private boolean isRangeValid(Version min,
                                 boolean minExclusive,
                                 Version max,
                                 boolean maxExclusive) {
        boolean result;

        // A null maximum version is unbounded so means that minimum is smaller
        // than
        // maximum.
        int minMaxCompare = (max == null ? -1 : min.compareTo(max));
        if (minMaxCompare > 0) {
            // Minimum larger than maximum is invalid.
            result = false;
        } else if (minMaxCompare == 0 && (minExclusive || maxExclusive)) {
            // If min and max are the same, and either are exclusive, no valid
            // range
            // exists.
            result = false;
        } else {
            // Range is valid.
            result = true;
        }

        return result;
    }

    /**
     * This method checks that the provided version matches the desired version.
     *
     * @param version
     *            the version.
     * @return true if the version matches, false otherwise.
     */
    public boolean matches(Version version) {
        boolean result;
        if (this.getMaximumVersion() == null) {
            result = this.getMinimumVersion().compareTo(version) <= 0;
        } else {
            int minN = this.isMinimumExclusive() ? 0 : 1;
            int maxN = this.isMaximumExclusive() ? 0 : 1;

            result = (this.getMinimumVersion().compareTo(version) < minN)
                     && (version.compareTo(this.getMaximumVersion()) < maxN);
        }
        return result;
    }

    /**
     * check if the versioninfo is the exact version
     * @return true if the range will match 1 exact version.
     */
    public boolean isExactVersion() {
        return minimumVersion.equals(maximumVersion) && minimumExclusive == maximumExclusive
               && !!!minimumExclusive;
    }

    /**
     * Create a new version range that is the intersection of {@code this} and the argument.
     * In other words, the largest version range that lies within both {@code this} and
     * the parameter.
     * @param r a version range to be intersected with {@code this}.
     * @return a new version range, or {@code null} if no intersection is possible.
     */
    public VersionRange intersect(VersionRange r) {
        // Use the highest minimum version.
        final Version newMinimumVersion;
        final boolean newMinimumExclusive;
        int minCompare = minimumVersion.compareTo(r.getMinimumVersion());
        if (minCompare > 0) {
            newMinimumVersion = minimumVersion;
            newMinimumExclusive = minimumExclusive;
        } else if (minCompare < 0) {
            newMinimumVersion = r.getMinimumVersion();
            newMinimumExclusive = r.isMinimumExclusive();
        } else {
            newMinimumVersion = minimumVersion;
            newMinimumExclusive = (minimumExclusive || r.isMinimumExclusive());
        }

        // Use the lowest maximum version.
        final Version newMaximumVersion;
        final boolean newMaximumExclusive;
        // null maximum version means unbounded, so the highest possible value.
        if (maximumVersion == null) {
            newMaximumVersion = r.getMaximumVersion();
            newMaximumExclusive = r.isMaximumExclusive();
        } else if (r.getMaximumVersion() == null) {
            newMaximumVersion = maximumVersion;
            newMaximumExclusive = maximumExclusive;
        } else {
            int maxCompare = maximumVersion.compareTo(r.getMaximumVersion());
            if (maxCompare < 0) {
                newMaximumVersion = maximumVersion;
                newMaximumExclusive = maximumExclusive;
            } else if (maxCompare > 0) {
                newMaximumVersion = r.getMaximumVersion();
                newMaximumExclusive = r.isMaximumExclusive();
            } else {
                newMaximumVersion = maximumVersion;
                newMaximumExclusive = (maximumExclusive || r.isMaximumExclusive());
            }
        }

        VersionRange result;
        if (isRangeValid(newMinimumVersion, newMinimumExclusive, newMaximumVersion,
                newMaximumExclusive)) {
            result = new VersionRange(newMaximumVersion, newMaximumExclusive, newMinimumVersion,
                    newMinimumExclusive);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Parse a version range..
     *
     * @param s
     * @return VersionRange object.
     * @throws IllegalArgumentException
     *             if the String could not be parsed as a VersionRange
     */
    public static VersionRange parseVersionRange(String s) throws IllegalArgumentException {
        return new VersionRange(s);
    }

    /**
     * Parse a version range and indicate if the version is an exact version
     *
     * @param s
     * @param exactVersion
     * @return VersionRange object.
     * @throws IllegalArgumentException
     *             if the String could not be parsed as a VersionRange
     */
    public static VersionRange parseVersionRange(String s, boolean exactVersion)
            throws IllegalArgumentException {
        return new VersionRange(s, exactVersion);
    }
}
