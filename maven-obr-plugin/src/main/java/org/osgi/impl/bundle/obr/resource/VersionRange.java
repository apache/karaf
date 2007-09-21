/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.osgi.impl.bundle.obr.resource;

import org.osgi.framework.Version;

/**
 * This class represents a version range.
 * @since 3.1
 */
public class VersionRange {
	private static final Version versionMax = new Version(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
	/**
	 * An empty version
	 */
	public static final VersionRange emptyRange = new VersionRange(null);

	private Version minVersion;
	private boolean includeMin; 
	private Version maxVersion;
	private boolean includeMax;

	/**
	 * Constructs a VersionRange with the specified minVersion and maxVersion.
	 * @param minVersion the minimum version of the range
	 * @param maxVersion the maximum version of the range
	 */
	public VersionRange(Version minVersion, boolean includeMin, Version maxVersion, boolean includeMax) {
		this.minVersion = minVersion;
		this.includeMin = includeMin;
		this.maxVersion = maxVersion;
		this.includeMax = includeMax;
	}

	/**
	 * Constructs a VersionRange from the given versionRange String.
	 * @param versionRange a version range String that specifies a range of
	 * versions.
	 */
	public VersionRange(String versionRange) {
		if (versionRange == null || versionRange.length() == 0) {
			minVersion = Version.emptyVersion;
			includeMin = true;
			maxVersion = VersionRange.versionMax;
			includeMax = true;
			return;
		}
		versionRange = versionRange.trim();
		if (versionRange.charAt(0) == '[' || versionRange.charAt(0) == '(') {
			int comma = versionRange.indexOf(',');
			if (comma < 0)
				throw new IllegalArgumentException();
			char last = versionRange.charAt(versionRange.length() - 1);
			if (last != ']' && last != ')')
				throw new IllegalArgumentException();

			minVersion = Version.parseVersion(versionRange.substring(1, comma).trim());
			includeMin = versionRange.charAt(0) == '[';
			maxVersion = Version.parseVersion(versionRange.substring(comma + 1, versionRange.length() - 1).trim());
			includeMax = last == ']';
		} else {
			minVersion = Version.parseVersion(versionRange.trim());
			includeMin = true;
			maxVersion = VersionRange.versionMax;
			includeMax = true;
		}
	}

	/**
	 * Returns the minimum Version of this VersionRange
	 * @return the minimum Version of this VersionRange
	 */
	public Version getMinimum() {
		return minVersion;
	}

	/**
	 * Indicates if the minimum version is included in the version range.
	 * @return true if the minimum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMinimum() {
		return includeMin;
	}

	/**
	 * Returns the maximum Version of this VersionRange
	 * @return the maximum Version of this VersionRange
	 */
	public Version getMaximum() {
		return maxVersion;
	}

	/**
	 * Indicates if the maximum version is included in the version range.
	 * @return true if the maximum version is included in the version range;
	 * otherwise false is returned
	 */
	public boolean getIncludeMaximum() {
		return includeMax;
	}

	/**
	 * Returns whether the given version is included in this VersionRange.
	 * This will depend on the minimum and maximum versions of this VersionRange
	 * and the given version.
	 * 
	 * @param version a version to be tested for inclusion in this VersionRange. 
	 * (may be <code>null</code>)
	 * @return <code>true</code> if the version is include, 
	 * <code>false</code> otherwise 
	 */
	public boolean isIncluded(Version version) {
		Version minRequired = getMinimum();
		if (minRequired == null)
			return true;
		if (version == null)
			return false;
		Version maxRequired = getMaximum() == null ? VersionRange.versionMax : getMaximum();
		int minCheck = includeMin ? 0 : 1;
		int maxCheck = includeMax ? 0 : -1;
		return version.compareTo(minRequired) >= minCheck && version.compareTo(maxRequired) <= maxCheck;

	}

	public boolean equals(Object object) {
		if (!(object instanceof VersionRange))
			return false;
		VersionRange vr = (VersionRange) object;
		if (minVersion != null && vr.getMinimum() != null) {
			if (minVersion.equals(vr.getMinimum()) && includeMin == vr.includeMin)
				if (maxVersion != null && vr.getMaximum() != null) {
					if (maxVersion.equals(vr.getMaximum()) && includeMax == vr.includeMax)
						return true;
				}
				else
					return maxVersion == vr.getMaximum();
		}
		else {
			return minVersion == vr.getMinimum();
		}
		return false;
	}

	public String toString() {
		if (minVersion == null)
			return Version.emptyVersion.toString();
		if (VersionRange.versionMax.equals(maxVersion))
			return minVersion.toString();
		StringBuffer result = new StringBuffer();
		result.append(includeMin ? '[' : '(');
		result.append(minVersion);
		result.append(',');
		result.append(maxVersion);
		result.append(includeMax ? ']' : ')');
		return result.toString();
	}
}
