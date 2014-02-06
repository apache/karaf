/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.semantic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.tooling.features.GenerateDescriptorMojo;
import org.apache.karaf.tooling.semantic.eclipse.ConflictIdSorter;
import org.apache.karaf.tooling.semantic.eclipse.ConflictMarker;
import org.apache.karaf.tooling.semantic.eclipse.ConflictResolver;
import org.apache.karaf.tooling.semantic.eclipse.JavaScopeDeriver;
import org.apache.karaf.tooling.semantic.eclipse.JavaScopeSelector;
import org.apache.karaf.tooling.semantic.eclipse.NearestVersionSelector;
import org.apache.karaf.tooling.semantic.eclipse.SimpleOptionalitySelector;
import org.apache.karaf.tooling.semantic.xform.SnapshotTransformer;
import org.apache.maven.RepositoryUtils;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.graph.PostorderNodeListGenerator;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;
import org.sonatype.aether.util.graph.transformer.ChainedDependencyGraphTransformer;

/**
 * Generates the semantic features XML file for packaging=pom
 * 
 * @goal features-generate-semantic
 * @phase package
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description TODO
 * 
 * @author Andrei Pozolotin
 */
public class GenerateSemanticMojo extends GenerateDescriptorMojo {

	/**
	 * FIXME normal equals does not work.
	 */
	public static boolean equals(Dependency one, Dependency two) {
		return one.toString().equals(two.toString());
	}

	/**
	 * Dependency scope to include. Default include: compile.
	 * 
	 * @parameter
	 */
	protected Set<String> scopeIncluded;
	{
		scopeIncluded = new HashSet<String>();
		scopeIncluded.add("compile");
	}

	/**
	 * Root artifact packaging to include. Default include: bundle.
	 * 
	 * @parameter
	 */
	protected Set<String> packagingIncluded;
	{
		packagingIncluded = new HashSet<String>();
		packagingIncluded.add("bundle");
	}

	/**
	 * Dependency scope to exclude. Default exclude: runtime, provided, system,
	 * test.
	 * 
	 * @parameter
	 */
	protected Set<String> scopeExcluded;
	{
		scopeExcluded = new HashSet<String>();
		scopeExcluded.add("runtime");
		scopeExcluded.add("provided");
		scopeExcluded.add("system");
		scopeExcluded.add("test");
	}

	/**
	 * 
	 * @parameter
	 */
	protected Map<String, String> resolverSettings = new HashMap<String, String>();

	/**
	 */
	public static Map<Artifact, String> prepare(MojoContext context)
			throws Exception {

		Artifact artifact = RepositoryUtils.toArtifact(context.project
				.getArtifact());

		Dependency root = new Dependency(artifact, "compile");

		CollectRequest collectRequest = new CollectRequest(root,
				context.projectRepos);

		DependencySelector selector = new AndDependencySelector(
				new DependencySelector[] {
						new OptionalDependencySelector(),
						new ScopeDependencySelector(context.scopeIncluded,
								context.scopeExcluded),
						new ExclusionDependencySelector() });

		DefaultRepositorySystemSession localSession = new DefaultRepositorySystemSession(
				context.session);

		localSession.setDependencySelector(selector);

		//

		ConflictMarker marker = new ConflictMarker();
		ConflictIdSorter sorter = new ConflictIdSorter();

		SnapshotTransformer snapper = new SnapshotTransformer(
				context.resolverSettings);

		ConflictResolver.VersionSelector versionSelector = new NearestVersionSelector();
		ConflictResolver.ScopeSelector scopeSelector = new JavaScopeSelector();
		ConflictResolver.OptionalitySelector optionalitySelector = new SimpleOptionalitySelector();
		ConflictResolver.ScopeDeriver scopeDeriver = new JavaScopeDeriver();

		ConflictResolver resolver = new ConflictResolver(versionSelector,
				scopeSelector, optionalitySelector, scopeDeriver);

		ChainedDependencyGraphTransformer transformer = new ChainedDependencyGraphTransformer(
				marker, sorter, snapper, resolver);

		localSession.setDependencyGraphTransformer(transformer);

		//

		CollectResult collectResult = context.system.collectDependencies(
				localSession, collectRequest);

		DependencyNode collectNode = collectResult.getRoot();

		DependencyRequest dependencyRequest = new DependencyRequest(
				collectNode, null);

		DependencyResult resolveResult = context.system.resolveDependencies(
				localSession, dependencyRequest);

		DependencyNode resolveNode = resolveResult.getRoot();

		PostorderNodeListGenerator generator = new PostorderNodeListGenerator();

		resolveNode.accept(generator);

		List<Dependency> dependencyList = generator.getDependencies(false);

		Map<Artifact, String> dependencyMap = new LinkedHashMap<Artifact, String>();

		for (Dependency dependency : dependencyList) {

			boolean isRootNode = equals(root, dependency);

			boolean isPackagingIncluded = context.packagingIncluded
					.contains(context.project.getPackaging());

			if (isRootNode) {
				if (!isPackagingIncluded) {
					context.logger.info("Excluded: " + dependency);
					continue;
				}
			}

			context.logger.info("\t " + dependency);

			dependencyMap.put(dependency.getArtifact(), dependency.getScope());

		}

		return dependencyMap;
	}

	@Override
	protected void prepare() throws Exception {

		MojoContext context = new MojoContext(getLogger(), project,
				scopeIncluded, scopeExcluded, repoSystem, repoSession,
				projectRepos, resolverSettings, packagingIncluded);

		this.localDependencies = prepare(context);

		this.treeListing = "not available";

	}

}
