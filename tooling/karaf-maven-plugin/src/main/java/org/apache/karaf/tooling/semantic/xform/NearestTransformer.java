package org.apache.karaf.tooling.semantic.xform;

import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.collection.UnsolvableVersionConflictException;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.transformer.ConflictIdSorter;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;

/**
 * A dependency graph transformer that resolves version conflicts using the
 * nearest-wins strategy. For a given set of conflicting nodes, one node will be
 * chosen as the winner and the other nodes are removed from the dependency
 * graph.
 * 
 * @author Benjamin Bentmann
 */
public class NearestTransformer extends BaseTransformer {

	static final class ConflictGroup {

		final Map<DependencyNode, Position> candidates = new IdentityHashMap<DependencyNode, Position>(
				32);

		final Collection<VersionConstraint> constraints = new HashSet<VersionConstraint>();

		final Object key;

		Position position;

		final Collection<Position> positions = new LinkedHashSet<Position>();

		boolean pruned;

		Version version;

		public ConflictGroup(Object key) {
			this.key = key;
			this.position = new Position(null, Integer.MAX_VALUE);
		}

		@Override
		public String toString() {
			return key + " > " + version;
		}

	}

	static final class Position {

		final int depth;

		final int hash;

		final DependencyNode parent;

		public Position(DependencyNode parent, int depth) {
			this.parent = parent;
			this.depth = depth;
			hash = 31 * System.identityHashCode(parent) + depth;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			} else if (!(obj instanceof Position)) {
				return false;
			}
			Position that = (Position) obj;
			return this.parent == that.parent && this.depth == that.depth;
		}

		@Override
		public int hashCode() {
			return hash;
		}

		@Override
		public String toString() {
			return depth + " > " + parent;
		}

	}

	private void backtrack(ConflictGroup group)
			throws UnsolvableVersionConflictException {
		group.version = null;

		for (Iterator<Map.Entry<DependencyNode, Position>> it = group.candidates
				.entrySet().iterator(); it.hasNext();) {
			Map.Entry<DependencyNode, Position> entry = it.next();

			Version version = entry.getKey().getVersion();
			Position pos = entry.getValue();

			if (!isAcceptable(group, version)) {
				it.remove();
			} else if (group.version == null
					|| isNearer(pos, version, group.position, group.version)) {
				group.version = version;
				group.position = pos;
			}
		}

		if (group.version == null) {
			throw newFailure(group);
		}

	}

	private boolean isAcceptable(ConflictGroup group, Version version) {
		for (VersionConstraint constraint : group.constraints) {
			if (!constraint.containsVersion(version)) {
				return false;
			}
		}
		return true;
	}

	private boolean isNearer(Position pos1, Version ver1, Position pos2,
			Version ver2) {
		if (pos1.depth < pos2.depth) {
			return true;
		} else if (pos1.depth == pos2.depth && pos1.parent == pos2.parent
				&& ver1.compareTo(ver2) > 0) {
			return true;
		}
		return false;
	}

	private UnsolvableVersionConflictException newFailure(ConflictGroup group) {

		Collection<String> versions = new LinkedHashSet<String>();

		for (VersionConstraint constraint : group.constraints) {
			versions.add(constraint.toString());
		}

		return new UnsolvableVersionConflictException(group.key, versions);

	}

	private void pruneNonSelectedVersions(ConflictGroup group,
			Map<DependencyNode, Object> conflictMap) {

		for (Position pos : group.positions) {
			for (Iterator<DependencyNode> iterator = pos.parent.getChildren()
					.iterator(); iterator.hasNext();) {

				DependencyNode node = iterator.next();

				Object key = conflictMap.get(node);

				if (group.key.equals(key)) {
					if (!group.pruned && group.position.depth == pos.depth
							&& group.version.equals(node.getVersion())) {
						group.pruned = true;
					} else {
						iterator.remove();
					}
				}
			}
		}
	}

	private void selectVersion(//
			DependencyNode node, //
			DependencyNode root, //
			int depth, //
			Map<DependencyNode, Integer> depthMap, //
			ConflictGroup group, //
			Map<DependencyNode, Object> conflictMap //
	) throws RepositoryException {

		Integer smallestDepth = depthMap.get(node);

		if (smallestDepth == null || smallestDepth.intValue() > depth) {
			depthMap.put(node, Integer.valueOf(depth));
		} else {
			return;
		}

		Object key = conflictMap.get(node);

		if (group.key.equals(key)) {

			Position pos = new Position(root, depth);

			if (root != null) {
				group.positions.add(pos);
			}

			VersionConstraint constraint = node.getVersionConstraint();

			boolean backtrack = false;
			boolean hardConstraint = !constraint.getRanges().isEmpty();

			if (hardConstraint) {
				if (group.constraints.add(constraint)) {
					if (group.version != null
							&& !constraint.containsVersion(group.version)) {
						backtrack = true;
					}
				}
			}

			if (isAcceptable(group, node.getVersion())) {
				group.candidates.put(node, pos);

				if (backtrack) {
					backtrack(group);
				} else if (group.version == null
						|| isNearer(pos, node.getVersion(), group.position,
								group.version)) {
					group.version = node.getVersion();
					group.position = pos;
				}
			} else {
				if (backtrack) {
					backtrack(group);
				}
				return;
			}
		}

		depth++;

		for (DependencyNode child : node.getChildren()) {
			selectVersion(child, node, depth, depthMap, group, conflictMap);
		}
	}

	public DependencyNode transformGraph(//
			DependencyNode node, //
			DependencyGraphTransformationContext context //
	) throws RepositoryException {

		List<Object> conflictList = conflictList(context);

		Map<DependencyNode, Object> conflictMap = conflictMap(context);

		Map<DependencyNode, Integer> depthMap = new IdentityHashMap<DependencyNode, Integer>(
				conflictMap.size());

		for (Object key : conflictList) {
			ConflictGroup group = new ConflictGroup(key);
			depthMap.clear();
			selectVersion(node, null, 0, depthMap, group, conflictMap);
			pruneNonSelectedVersions(group, conflictMap);
		}

		return node;
	}

}
