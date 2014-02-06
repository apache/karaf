package org.apache.karaf.tooling.semantic.xform;

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
 * "Object" of "Conflict ID" is an artifact key, that is, artifact w/o version.
 * <p>
 * equality == group:artifact:classifier:extensions
 * <p>
 * identity == unique instance
 * <p>
 * Conflict list and map must be produced by previous transformers.
 * 
 * @author Andrei Pozolotin
 */
public abstract class BaseTransformer implements DependencyGraphTransformer {

	/**
	 * Topologically sorted artifact keys.
	 */
	@SuppressWarnings("unchecked")
	public List<Object> conflictList(
			DependencyGraphTransformationContext context) {
		final List<Object> conflictList = (List<Object>) context
				.get(TransformationContextKeys.SORTED_CONFLICT_IDS);
		return conflictList;
	}

	/**
	 * Mapping from versioned artifact into artifact keys.
	 */
	@SuppressWarnings("unchecked")
	public Map<DependencyNode, Object> conflictMap(
			DependencyGraphTransformationContext context) {
		final Map<DependencyNode, Object> conflictMap = (Map<DependencyNode, Object>) context
				.get(TransformationContextKeys.CONFLICT_IDS);
		return conflictMap;
	}

	protected void log(String text) {
		System.err.println(text);
	}

}
