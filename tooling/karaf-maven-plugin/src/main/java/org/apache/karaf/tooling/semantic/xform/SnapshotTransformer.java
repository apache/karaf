package org.apache.karaf.tooling.semantic.xform;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.karaf.tooling.semantic.range.VersionType;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;

public class SnapshotTransformer extends BaseTransformer {

	public static final String ENABLE_RANGE_SNAPSHOT = "enableRangeSnapshot";

	private boolean enableRangeSnapshot;

	public SnapshotTransformer(Map<String, String> resolverSettings) {

		this.enableRangeSnapshot = Boolean
				.parseBoolean((String) resolverSettings
						.get("enableRangeSnapshot"));

	}

	public DependencyNode transformGraph(DependencyNode node,
			DependencyGraphTransformationContext context)
			throws RepositoryException {

		if (!this.enableRangeSnapshot) {
			removeRangeSnapshot(node, context);
		}

		return node;
	}

	protected void removeRangeSnapshot(DependencyNode root,
			DependencyGraphTransformationContext context) {

		Iterator<DependencyNode> iterator = root.getChildren().iterator();

		while (iterator.hasNext()) {
			DependencyNode node = (DependencyNode) iterator.next();

			VersionType versionType = VersionType.form(node
					.getVersionConstraint());

			switch (versionType) {
			case RANGE:
				if (node.getDependency().getArtifact().isSnapshot()) {
					iterator.remove();
				}
				break;
			}

			removeRangeSnapshot(node, context);
		}
	}
}