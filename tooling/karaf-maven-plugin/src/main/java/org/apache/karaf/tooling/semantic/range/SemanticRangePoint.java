package org.apache.karaf.tooling.semantic.range;

import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionRange;

public class SemanticRangePoint extends SemanticRangeBase {

	private final Version version;

	public SemanticRangePoint(Version version) {
		this.version = version;
	}

	@Override
	public boolean containsVersion(Version version) {
		return this.version.equals(version);
	}

	@Override
	public Bound getLowerBound() {
		return new Bound(version, true);
	}

	@Override
	public Bound getUpperBound() {
		return new Bound(version, true);
	}

	public String toString() {
		return "[" + version + "," + version + "]";
	}

}
