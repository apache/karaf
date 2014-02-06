package org.apache.karaf.tooling.semantic.range;

import org.sonatype.aether.version.VersionConstraint;

public enum VersionType {

	/***/
	RANGE, //

	/***/
	VALUE, //

	/***/
	WRONG, //

	;

	public static VersionType form(final VersionConstraint constraint) {
		if (constraint == null) {
			return WRONG;
		}
		boolean hasRange = !constraint.getRanges().isEmpty();
		boolean hasValue = constraint.getVersion() != null;
		if (hasRange && !hasValue) {
			return RANGE;
		}
		if (!hasRange && hasValue) {
			return VALUE;
		}
		return WRONG;
	}

}
