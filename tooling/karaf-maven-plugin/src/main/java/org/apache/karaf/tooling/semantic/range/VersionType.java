package org.apache.karaf.tooling.semantic.range;

import org.sonatype.aether.version.VersionConstraint;

/**
 * Version type resolver
 */
public enum VersionType {

	/**
	 * Version is range, i.e. [1,2)
	 */
	RANGE, //

	/**
	 * Version is value, i.e. 1.0.0
	 */
	VALUE, //

	/**
	 * Unexpected input.
	 */
	WRONG, //

	;

	public static VersionType form(final VersionConstraint constraint) {
		if (constraint == null) {
			return WRONG;
		}
		final boolean hasRange = !constraint.getRanges().isEmpty();
		final boolean hasValue = constraint.getVersion() != null;
		if (hasRange && !hasValue) {
			return RANGE;
		}
		if (!hasRange && hasValue) {
			return VALUE;
		}
		return WRONG;
	}

}
