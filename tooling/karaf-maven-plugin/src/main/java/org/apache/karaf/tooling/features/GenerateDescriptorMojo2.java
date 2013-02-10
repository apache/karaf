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
package org.apache.karaf.tooling.features;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.RepositoryUtils;
import org.apache.maven.project.MavenProject;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.collection.CollectResult;
import org.sonatype.aether.collection.DependencySelector;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.DefaultRepositorySystemSession;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.sonatype.aether.util.graph.selector.AndDependencySelector;
import org.sonatype.aether.util.graph.selector.ExclusionDependencySelector;
import org.sonatype.aether.util.graph.selector.OptionalDependencySelector;
import org.sonatype.aether.util.graph.selector.ScopeDependencySelector;

/**
 * Generates the features XML file for packaging=bundle
 * 
 * @goal features-generate-descriptor2
 * @phase compile
 * @requiresDependencyResolution runtime
 * @inheritByDefault true
 * @description Generates the features XML file starting with an optional source
 *              feature.xml and adding project dependencies as bundles and
 *              feature/kar dependencies
 */
public class GenerateDescriptorMojo2 extends GenerateDescriptorMojo {

	/**
	 * Dependency scope to include. Default include: compile, runtime.
	 * 
	 * @parameter
	 */
	protected Set<String> scopeIncluded;
	{
		scopeIncluded = new HashSet<String>();
		scopeIncluded.add("compile");
		scopeIncluded.add("runtime");
	}

	/**
	 * Dependency scope to exclude. Default exclude: provided, system, test.
	 * 
	 * @parameter
	 */
	protected Set<String> scopeExcluded;
	{
		scopeExcluded = new HashSet<String>();
		scopeExcluded.add("provided");
		scopeExcluded.add("system");
		scopeExcluded.add("test");
	}

	/**
	 * Find transitive dependencies with given filters.
	 */
	public static Map<Artifact, String> prepare( //
			MavenProject project, //
			Set<String> scopeIncluded, //
			Set<String> scopeExcluded, //
			RepositorySystem system, //
			RepositorySystemSession session //
	) throws Exception {

		// [dependency:scope]
		final Map<Artifact, String> localDependencies = new HashMap<Artifact, String>();

		final DefaultRepositorySystemSession localSession = new DefaultRepositorySystemSession(
				session);

		final Artifact artifact = RepositoryUtils.toArtifact(project
				.getArtifact());

		final Dependency dependency = new Dependency(artifact, "compile");

		final CollectRequest collectRequest = new CollectRequest(dependency,
				null);

		final DependencySelector selector = new AndDependencySelector(//
				new OptionalDependencySelector(), //
				new ScopeDependencySelector(scopeIncluded, scopeExcluded), //
				new ExclusionDependencySelector());

		localSession.setDependencySelector(selector);

		final CollectResult collectResult = system.collectDependencies(
				localSession, collectRequest);

		final DependencyNode collectNode = collectResult.getRoot();

		final DependencyRequest dependencyRequest = new DependencyRequest(
				collectNode, null);

		final DependencyResult result = system.resolveDependencies(
				localSession, dependencyRequest);

		final DependencyNode resolveNode = result.getRoot();

		final PreorderNodeListGenerator generator = new PreorderNodeListGenerator();

		resolveNode.accept(generator);

		final List<Dependency> dependencyList = generator.getDependencies(true);

		for (Dependency dependencyItem : dependencyList) {
			localDependencies.put(dependencyItem.getArtifact(),
					dependencyItem.getScope());
		}

		return localDependencies;

	}

	@Override
	protected void prepare() throws Exception {

		this.localDependencies = prepare(project, scopeIncluded, scopeExcluded,
				repoSystem, repoSession);

		this.treeListing = "not available";

	}

}
