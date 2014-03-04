package org.apache.karaf.tooling.semantic.range;

import java.util.Collection;
import java.util.Iterator;

import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;
import org.sonatype.aether.version.VersionRange;

public class SemanticRangeFactory {

	public static SemanticRange from(DependencyNode node) {

		VersionConstraint constraint = node.getVersionConstraint();

		VersionType versionType = VersionType.form(constraint);

		switch (versionType) {

		case VALUE:
			Version version = constraint.getVersion();
			return new SemanticRangePoint(version);

		case RANGE:
			Collection<VersionRange> rangeList = constraint.getRanges();
			Iterator<VersionRange> iterator = rangeList.iterator();
			VersionRange range = iterator.next();
			if (iterator.hasNext()) {
				throw new IllegalStateException("Unexpected multiple ranges = "
						+ rangeList);
			}
			return new SemanticRangeSpan(range);

		default:
			throw new IllegalStateException("Wrong version type = "
					+ versionType);

		}

	}

}
