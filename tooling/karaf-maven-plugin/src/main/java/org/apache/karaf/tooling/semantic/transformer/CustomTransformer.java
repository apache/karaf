package org.apache.karaf.tooling.semantic.transformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.karaf.tooling.semantic.range.VersionType;
import org.apache.mina.util.IdentityHashSet;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.graph.DependencyVisitor;
import org.sonatype.aether.util.artifact.ArtifacIdUtils;

import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver;

/**
 * Customer transform.
 * <p>
 * Supported features
 * <li>remove all range snapshots
 * <li>remove all but regex-match range snapshots
 */
public class CustomTransformer extends BaseTransformer {

	/**
	 * Identity of original pom.xml
	 */
	static class Project {

		/**
		 * Aether uses children list object as a proxy-id to the original
		 * pom.xml.
		 * <p>
		 * {@link ConflictResolver.ConflictItem}
		 * "unique owner of a child node which is the child list"
		 */
		static Object key(DependencyNode node) {
			return node.getChildren();
		}

		/** Unique id of the original pom.xml */
		final Object key;

		final List<DependencyNode> nodeList;

		/** Nodes which represent the same pom.xml */
		final Set<DependencyNode> pomSet = new IdentityHashSet<DependencyNode>();

		Project(List<DependencyNode> nodeList) {
			this.key = nodeList;
			this.nodeList = nodeList;
		}

	}

	/**
	 * Enable all snapshot dependencies which come form the range.
	 */
	public static final String ENABLE_RANGE_SNAPSHOT = "enableRangeSnapshot";

	/**
	 * Enable only matching snapshot dependencies which come form the range.
	 * <p>
	 * {@code <groupId>:<artifactId>:<extension>[:<classifier>]:<version>}
	 */
	public static final String ENABLE_RANGE_SNAPSHOT_REGEX = "enableRangeSnapshotRegex";

	private final boolean enableRangeSnapshot;

	private final Pattern enableRangeSnapshotRegex;

	public CustomTransformer(final Map<String, String> resolverSettings) {

		final String enableText = resolverSettings.get(ENABLE_RANGE_SNAPSHOT);
		enableRangeSnapshot = enableText == null ? false : Boolean
				.parseBoolean(enableText);

		final String regexText = resolverSettings
				.get(ENABLE_RANGE_SNAPSHOT_REGEX);
		enableRangeSnapshotRegex = regexText == null ? null : Pattern
				.compile(regexText);

	}

	/**
	 * Find all project roots.
	 */
	protected void discoverProjects(final DependencyNode root,
			final Map<Object, List<DependencyNode>> pomMap) {

		final List<DependencyNode> nodeList = root.getChildren();

		/**
		 * Aether uses children list object as a proxy-id to the original
		 * pom.xml.
		 * <p>
		 * {@link ConflictResolver.ConflictItem}
		 * "unique owner of a child node which is the child list"
		 */
		final Object parent = nodeList;

		if (pomMap.containsKey(parent)) {
			/** Already discovered. */
			return;
		} else {
			/** New - remember now. */
			pomMap.put(parent, nodeList);
		}

		for (final DependencyNode node : nodeList) {
			discoverProjects(node, pomMap);
		}

	}

	/**
	 * Find nodes to be removed.
	 */
	protected void discoverRemoval(
			final Map<DependencyNode, Object> conflictMap,
			final Map<DependencyNode, Object> removalMap) {

		final Set<Entry<DependencyNode, Object>> entrySet = conflictMap
				.entrySet();

		for (final Entry<DependencyNode, Object> entry : entrySet) {

			final Object key = entry.getValue();
			final DependencyNode node = entry.getKey();
			final Artifact artifact = node.getDependency().getArtifact();
			final VersionType type = VersionType.form(node
					.getVersionConstraint());
			final boolean isSnapshot = artifact.isSnapshot();

			if (!isSnapshot) {
				continue;
			}

			if (type != VersionType.RANGE) {
				continue;
			}

			if (enableRangeSnapshotRegex == null) {
				/** Remove all snapshots. */
				removalMap.put(node, key);
			} else {
				/** Remove non-matching snapshots. */
				final String artifactGUID = ArtifacIdUtils.toId(artifact);
				if (enableRangeSnapshotRegex.matcher(artifactGUID).matches()) {
					/** Keep. */
				} else {
					removalMap.put(node, key);
				}
			}

		}

	}

	/**
	 * Remove nodes from dependency list.
	 */
	protected void remove(final Map<Object, List<DependencyNode>> pomMap,
			Map<DependencyNode, Object> removalMap) {

		final Collection<List<DependencyNode>> dependencyList = pomMap.values();

		final Set<DependencyNode> removalSet = removalMap.keySet();

		for (final DependencyNode node : removalSet) {
			for (final List<DependencyNode> nodeList : dependencyList) {
				nodeList.remove(node);
			}
		}

	}

	/**
	 * Remove range-bound snapshots from collected resolver structures.
	 * <p>
	 * Does not affect non-range snapshots.
	 */
	protected void removeRangeSnapshot(final DependencyNode root,
			final DependencyGraphTransformationContext context) {

		final long timeStart = System.currentTimeMillis();

		/**
		 * Collected dependency graph.
		 */
		final Map<DependencyNode, Object> conflictMap = conflictMap(context);
		log("### conflictMap.size()=" + conflictMap.size());

		/**
		 * Dependency removal filter.
		 */
		final Map<DependencyNode, Object> removalMap = new HashMap<DependencyNode, Object>();
		discoverRemoval(conflictMap, removalMap);
		log("### removalMap.size()=" + removalMap.size());

		/**
		 * Discovered dependency roots.
		 */
		final Map<Object, List<DependencyNode>> parentMap = new IdentityHashMap<Object, List<DependencyNode>>();
		discoverProjects(root, parentMap);
		log("### parentMap.size()=" + parentMap.size());

		remove(parentMap, removalMap);

		final long timeFinish = System.currentTimeMillis();

		final long timeDiff = timeFinish - timeStart;

		log("### " + String.format("removal process time: %,d ms", timeDiff));

	}

	@Override
	public DependencyNode transformGraph(final DependencyNode node,
			final DependencyGraphTransformationContext context)
			throws RepositoryException {

		if (!this.enableRangeSnapshot) {
			removeRangeSnapshot(node, context);
		}

		return node;
	}

	/**
	 * Do not use.
	 */
	protected void zzzRemoveRangeSnapshot(final DependencyNode root,
			final DependencyGraphTransformationContext context, final int level) {

		final Iterator<DependencyNode> iterator = root.getChildren().iterator();

		while (iterator.hasNext()) {

			final DependencyNode node = iterator.next();

			final VersionType versionType = VersionType.form(node
					.getVersionConstraint());

			final Artifact artifact = node.getDependency().getArtifact();

			log("level=" + level + " artifact=" + artifact);

			switch (versionType) {
			default:

				/** Non-range snapshots are not affected. */
				break;

			case RANGE:

				/** Process only range snapshots. */
				final boolean isSnapshot = artifact.isSnapshot();
				if (enableRangeSnapshotRegex == null) {
					/** Remove all snapshots. */
					if (isSnapshot) {
						iterator.remove();
					}
				} else {
					/** Remove non-matching snapshots. */
					if (isSnapshot) {
						final String artifactGUID = ArtifacIdUtils
								.toId(artifact);
						if (enableRangeSnapshotRegex.matcher(artifactGUID)
								.matches()) {
							/** Keep. */
						} else {
							iterator.remove();
						}
					}
				}
				break;

			}

			zzzRemoveRangeSnapshot(node, context, level + 1);

		}
	}

}