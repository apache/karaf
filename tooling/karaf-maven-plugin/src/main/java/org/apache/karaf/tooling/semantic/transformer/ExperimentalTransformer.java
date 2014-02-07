package org.apache.karaf.tooling.semantic.transformer;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.tooling.semantic.range.SemanticRange;
import org.apache.karaf.tooling.semantic.range.SemanticRangeFactory;
import org.apache.karaf.tooling.semantic.range.SemanticRangeList;
import org.apache.karaf.tooling.semantic.range.VersionType;
import org.sonatype.aether.RepositoryException;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;
import org.sonatype.aether.version.Version;
import org.sonatype.aether.version.VersionConstraint;

/**
 * @author Andrei Pozolotin
 */
public class ExperimentalTransformer extends BaseTransformer {

	/** FIXME */
	public static boolean isSnapshot(Version version) {
		return version.toString().endsWith("-SNAPSHOT");
	}

	// private Map<DependencyNode, SemanticRange> rangeMap = new
	// HashMap<DependencyNode, SemanticRange>();

	/**
	 * "Object" is a "conflict" or a wrapper for maven artifact.
	 * <p>
	 * equality == group:artifact:classifier:extensions
	 * <p>
	 * identity == unique instance
	 * <p>
	 * Conflict list and map are produced by previous transformers.
	 */

	/**
	 * [conflict-key : range-list ]
	 */
	private Map<Object, SemanticRangeList> rangeMap = new HashMap<Object, SemanticRangeList>();

	public SemanticRangeList list(Object key) {
		SemanticRangeList list = rangeMap.get(key);
		if (list == null) {
			list = new SemanticRangeList();
			rangeMap.put(key, list);
		}
		return list;
	}

	/**
	 * Remove snapshots.
	 */
	public void listClean(DependencyNode root,
			DependencyGraphTransformationContext context) {

		Iterator<DependencyNode> iterator = root.getChildren().iterator();

		while (iterator.hasNext()) {

			DependencyNode node = iterator.next();

			VersionType versionType = VersionType.form(node
					.getVersionConstraint());

			boolean isSnapshot = node.getDependency().getArtifact()
					.isSnapshot();

			switch (versionType) {
			case RANGE:
				break;
			case VALUE:
				break;
			default:
				throw new IllegalStateException("Wrong version type = "
						+ versionType);
			}

			if (isSnapshot) {
				iterator.remove();
			}

			listClean(node, context);
		}

	}

	public void listCreate(DependencyNode root,
			DependencyGraphTransformationContext context) {

		Map<DependencyNode, Object> conflictMap = conflictMap(context);

		for (DependencyNode node : root.getChildren()) {

			Object key = conflictMap.get(node);

			SemanticRange range = SemanticRangeFactory.from(node);

			SemanticRangeList list = list(key);

			list.addIfAbsent(range);

			listCreate(node, context);

		}

	}

	/**
	 * Sort by lower bound.
	 */
	public void listSort(DependencyNode root,
			DependencyGraphTransformationContext context) {

		Map<DependencyNode, Object> conflictMap = conflictMap(context);

		for (DependencyNode node : root.getChildren()) {

			Object key = conflictMap.get(node);

			SemanticRangeList list = list(key);

		}

	}

	public DependencyNode transformGraph(DependencyNode root,
			DependencyGraphTransformationContext context)
			throws RepositoryException {

		listClean(root, context);

		listCreate(root, context);

		listSort(root, context);

		// for (Map.Entry<DependencyNode, Object> entry :
		// conflictMap.entrySet()) {
		// listCreate(entry.getKey(), context);
		// }

		log("rangeMap=" + rangeMap);

		return root;

	}

	public DependencyNode transformGraphXXX(DependencyNode node,
			DependencyGraphTransformationContext context)
			throws RepositoryException {

		log("node=" + node);

		final List<Object> conflictList = conflictList(context);

		for (Object key : conflictList) {
			log("\t 1 key=" + key);
		}

		@SuppressWarnings("unchecked")
		final Map<DependencyNode, Object> conflictMap = (Map<DependencyNode, Object>) context
				.get(TransformationContextKeys.CONFLICT_IDS);

		final Object key = conflictMap.get(node);
		log("\t 2 key=" + key);
		final Dependency dependency = node.getDependency();

		for (Map.Entry<DependencyNode, Object> entry : conflictMap.entrySet()) {
			log("\t 3 node=" + entry.getKey() + " key=" + entry.getValue());
		}

		final VersionConstraint constraint = node.getVersionConstraint();
		log("\t 4 constraint=" + constraint);

		final Artifact artifact = dependency.getArtifact();
		log("\t 5 artifact=" + artifact);

		Iterator<DependencyNode> iterator = node.getChildren().iterator();
		while (iterator.hasNext()) {

			DependencyNode item = iterator.next();
			log("\t 6 item=" + item);

			VersionConstraint itemConstraint = item.getVersionConstraint();
			log("\t 7 constraint=" + itemConstraint);

			Version itemVersion = item.getVersion();
			log("\t 8 version=" + itemVersion);

			switch (VersionType.form(itemConstraint)) {
			case RANGE:
				log("\t 9 ranges=" + itemConstraint.getRanges());
				break;
			case VALUE:
				log("\t 10 version=" + itemVersion);
				break;
			}

			if (item.getDependency().getArtifact().isSnapshot()) {
				iterator.remove();
			}
		}

		return node;

	}

}
