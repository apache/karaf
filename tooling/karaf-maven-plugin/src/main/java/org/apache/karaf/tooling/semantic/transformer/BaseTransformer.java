package org.apache.karaf.tooling.semantic.transformer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.sonatype.aether.collection.DependencyGraphTransformationContext;
import org.sonatype.aether.collection.DependencyGraphTransformer;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.util.graph.transformer.TransformationContextKeys;

/**
 * "Object" of "Conflict ID" is an integer key.
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
	 * Topologically sorted integer keys.
	 */
	@SuppressWarnings("unchecked")
	public List<Object> conflictList(
			final DependencyGraphTransformationContext context) {
		final List<Object> conflictList = (List<Object>) context
				.get(TransformationContextKeys.SORTED_CONFLICT_IDS);
		return conflictList;
	}

	/**
	 * Verify presence of cycles.
	 */
	@SuppressWarnings("unchecked")
	public Collection<Collection<Object>> cycles(
			final DependencyGraphTransformationContext context) {
		Collection<Collection<Object>> cycles = (Collection<Collection<Object>>) context
				.get(TransformationContextKeys.CYCLIC_CONFLICT_IDS);
		return cycles;
	}

	/**
	 * Mapping from versioned artifact into integer key.
	 */
	@SuppressWarnings("unchecked")
	public Map<DependencyNode, Object> conflictMap(
			final DependencyGraphTransformationContext context) {
		final Map<DependencyNode, Object> conflictMap = (Map<DependencyNode, Object>) context
				.get(TransformationContextKeys.CONFLICT_IDS);
		return conflictMap;
	}

	protected void log(final String text) {
		System.err.println(text);
	}

}
