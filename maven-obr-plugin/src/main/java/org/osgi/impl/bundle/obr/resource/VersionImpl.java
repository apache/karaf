/*
 * $Header: /cvshome/bundles/bundles.obr/src/bundles/obr/resource/VersionImpl.java,v 1.3 2006/02/15 16:36:57 pkriens Exp $
 * 
 * Copyright (c) OSGi Alliance (2004, 2005). All Rights Reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this 
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html.
 */

package org.osgi.impl.bundle.obr.resource;

import org.osgi.framework.Version;

/**
 * Version identifier for bundles and packages.
 * 
 * <p>
 * Version identifiers have four components.
 * <ol>
 * <li>Major version. A non-negative integer.</li>
 * <li>Minor version. A non-negative integer.</li>
 * <li>Micro version. A non-negative integer.</li>
 * <li>Qualifier. A text string. See <code>Version(String)</code> for the
 * format of the qualifier string.</li>
 * </ol>
 * 
 * <p>
 * <code>Version</code> objects are immutable.
 * 
 * @version $Revision: 1.3 $
 * @since 1.3
 */

public class VersionImpl extends Version {
	VersionRange	range;

	/**
	 * Creates a version identifier from the specified numerical components.
	 * 
	 * <p>
	 * The qualifier is set to the empty string.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @throws IllegalArgumentException If the numerical components are
	 *         negative.
	 */
	public VersionImpl(int major, int minor, int micro) {
		this(major, minor, micro, null);
	}

	/**
	 * Creates a version identifier from the specifed components.
	 * 
	 * @param major Major component of the version identifier.
	 * @param minor Minor component of the version identifier.
	 * @param micro Micro component of the version identifier.
	 * @param qualifier Qualifier component of the version identifier. If
	 *        <code>null</code> is specified, then the qualifier will be set
	 *        to the empty string.
	 * @throws IllegalArgumentException If the numerical components are negative
	 *         or the qualifier string is invalid.
	 */
	public VersionImpl(int major, int minor, int micro, String qualifier) {
		super(major, minor, micro, qualifier);
	}

	// TODO Ugly!
	public VersionImpl(String string) {
		super(
				string.indexOf("[") >= 0 || string.indexOf("(") >= 0 ? new VersionRange(
						string).getMinimum().toString()
						: string);
		if ( string.indexOf("[") >= 0 || string.indexOf("(") >= 0 )
			range = new VersionRange(string);
	}

	VersionRange getRange() {
		return range;
	}
	/**
	 * 	this	other		0		1		-1
	 * 	
	 * @param o
	 * @return
	 * @see org.osgi.framework.Version#compareTo(java.lang.Object)
	 */
	public int compareTo(Object o) {
		if ( o instanceof VersionImpl ) {
			VersionImpl	other = (VersionImpl) o;
			int cs = 0;
			if ( range != null )
				cs++;
			if ( other.range!=null)
				cs+=2;
			switch (cs ) {
				case 0:	// V1 V2
					return super.compareTo(other);
					
				case 1:	// R1 V2
					return range.isIncluded(other) ? 0 : 1;
					
				case 2:	// V1 R2
					return other.range.isIncluded(this) ? 0 : 1;
	
					// TODO experimental
				case 3:	// R1 R2
					return range.isIncluded(other.range.getMinimum()) && range.isIncluded(other.range.getMaximum()) ? 0 : 1;			
			}
			return -1;
		} else {
			return super.compareTo(o);
		}
	}

	public boolean equals(Object other) {
		return compareTo(other) == 0;
	}
}