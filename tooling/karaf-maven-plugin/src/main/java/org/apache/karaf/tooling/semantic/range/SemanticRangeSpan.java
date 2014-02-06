package org.apache.karaf.tooling.semantic.range;

import org.apache.karaf.tooling.semantic.ReflectUtil;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionRange;

public class SemanticRangeSpan extends SemanticRangeBase {

	private final VersionRange range;

	public SemanticRangeSpan(VersionRange range) {
		this.range = range;
	}

	@Override
	public boolean containsVersion(Version version) {
		return range.containsVersion(version);
	}

	@Override
	public Bound getLowerBound() {
		Version version = ReflectUtil.readField(range, "lowerBound");
		boolean inclusive = ReflectUtil.readField(range, "lowerBoundInclusive");
		return new Bound(version, inclusive);
	}

	@Override
	public Bound getUpperBound() {
		Version version = ReflectUtil.readField(range, "upperBound");
		boolean inclusive = ReflectUtil.readField(range, "upperBoundInclusive");
		return new Bound(version, inclusive);
	}

	public String toString() {
		return range.toString();
	}

}
