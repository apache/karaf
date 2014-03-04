/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.karaf.tooling.semantic;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.logging.Logger;
import org.junit.Test;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;

public class GenerateSemanticMojoTest {

	@Test
	public void dependency1() throws Exception {

		final Logger logger = UnitHelp.logger();

		logger.info("===");

		final String uri = "com.carrotgarden.osgi:carrot-osgi-anno-scr-make:pom:1.1.3";

		final MavenProject project = UnitHelp.newProject(uri);

		Set<String> scopeIncluded;
		{
			scopeIncluded = new HashSet<String>();
			scopeIncluded.add("compile");
			scopeIncluded.add("runtime");
		}

		Set<String> scopeExcluded;
		{
			scopeExcluded = new HashSet<String>();
			scopeExcluded.add("provided");
			scopeExcluded.add("system");
			scopeExcluded.add("test");
		}

		final RepositorySystem system = UnitHelp.newSystem();
		final RepositorySystemSession session = UnitHelp.newSession(system);

		final Map<String, String> resolverSettings = new HashMap<String, String>();

		final Set<String> packagingIncluded = new HashSet<String>();
		{
			packagingIncluded.add("bundle");
		}

		final Set<String> typeIncluded = new HashSet<String>();
		{
			typeIncluded.add("jar");
		}

		final MojoContext context = new MojoContext(logger, project, scopeIncluded,
				scopeExcluded, system, session, UnitHelp.newRepoRemoteList(),
				resolverSettings, packagingIncluded, typeIncluded, true);

		final Map<Artifact, String> dependencyMap = GenerateSemanticMojo
				.prepare(context);

		for (final Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			final Artifact artifact = entry.getKey();
			final String scope = entry.getValue();
			logger.info(artifact + " @ " + scope);
		}

		assertEquals(1, dependencyMap.size());

	}

	@Test
	public void dependency2() throws Exception {

		final Logger logger = UnitHelp.logger();

		logger.info("===");

		final String uri = "com.carrotgarden.osgi:carrot-osgi-anno-scr-make:pom:1.1.3";

		final MavenProject project = UnitHelp.newProject(uri);

		Set<String> scopeIncluded;
		{
			scopeIncluded = new HashSet<String>();
			scopeIncluded.add("provided");
			scopeIncluded.add("runtime");
		}

		Set<String> scopeExcluded;
		{
			scopeExcluded = new HashSet<String>();
			scopeExcluded.add("compile");
			scopeExcluded.add("system");
			scopeExcluded.add("test");
		}

		final RepositorySystem system = UnitHelp.newSystem();
		final RepositorySystemSession session = UnitHelp.newSession(system);

		final Map<String, String> resolverSettings = new HashMap<String, String>();

		final Set<String> packagingIncluded = new HashSet<String>();
		{
			packagingIncluded.add("bundle");
		}

		final Set<String> typeIncluded = new HashSet<String>();
		{
			typeIncluded.add("jar");
		}

		final MojoContext context = new MojoContext(logger, project, scopeIncluded,
				scopeExcluded, system, session, UnitHelp.newRepoRemoteList(),
				resolverSettings, packagingIncluded, typeIncluded, true);

		final Map<Artifact, String> dependencyMap = GenerateSemanticMojo
				.prepare(context);

		for (final Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			final Artifact artifact = entry.getKey();
			final String scope = entry.getValue();
			logger.info(artifact + " @ " + scope);
		}

		assertEquals(5, dependencyMap.size());

	}

	@Test
	public void dependency3() throws Exception {

		final Logger logger = UnitHelp.logger();

		logger.info("===");

		final String uri = "com.barchart.version.tester:tester-one-zoo:pom:1.0.7";

		final MavenProject project = UnitHelp.newProject(uri);

		Set<String> scopeIncluded;
		{
			scopeIncluded = new HashSet<String>();
			scopeIncluded.add("compile");
		}

		Set<String> scopeExcluded;
		{
			scopeExcluded = new HashSet<String>();
			scopeExcluded.add("runtime");
			scopeExcluded.add("provided");
			scopeExcluded.add("system");
			scopeExcluded.add("test");
		}

		final RepositorySystem system = UnitHelp.newSystem();
		final RepositorySystemSession session = UnitHelp.newSession(system);

		final Map<String, String> resolverSettings = new HashMap<String, String>();

		final Set<String> packagingIncluded = new HashSet<String>();
		{
			packagingIncluded.add("bundle");
		}

		final Set<String> typeIncluded = new HashSet<String>();
		{
			typeIncluded.add("jar");
		}

		final MojoContext context = new MojoContext(logger, project, scopeIncluded,
				scopeExcluded, system, session, UnitHelp.newRepoRemoteList(),
				resolverSettings, packagingIncluded, typeIncluded, true);

		final Map<Artifact, String> dependencyMap = GenerateSemanticMojo
				.prepare(context);

		for (final Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			final Artifact artifact = entry.getKey();
			final String scope = entry.getValue();
			logger.info(artifact + " @ " + scope);
		}

		assertEquals(3, dependencyMap.size());

	}

	@Test
	public void dependency4() throws Exception {

		final Logger logger = UnitHelp.logger();

		logger.info("===");

		final String uri = "org.apache.karaf.kar:org.apache.karaf.kar.core:pom:3.0.0";

		final MavenProject project = UnitHelp.newProject(uri);

		Set<String> scopeIncluded;
		{
			scopeIncluded = new HashSet<String>();
			scopeIncluded.add("compile");
		}

		Set<String> scopeExcluded;
		{
			scopeExcluded = new HashSet<String>();
			scopeExcluded.add("provided");
			scopeExcluded.add("system");
			scopeExcluded.add("test");
		}

		final RepositorySystem system = UnitHelp.newSystem();
		final RepositorySystemSession session = UnitHelp.newSession(system);

		final Map<String, String> resolverSettings = new HashMap<String, String>();

		final Set<String> packagingIncluded = new HashSet<String>();
		{
			packagingIncluded.add("bundle");
		}

		final Set<String> typeIncluded = new HashSet<String>();
		{
			typeIncluded.add("jar");
		}

		final MojoContext context = new MojoContext(logger, project, scopeIncluded,
				scopeExcluded, system, session, UnitHelp.newRepoRemoteList(),
				resolverSettings, packagingIncluded, typeIncluded, false);

		final Map<Artifact, String> dependencyMap = GenerateSemanticMojo
				.prepare(context);

		for (final Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			final Artifact artifact = entry.getKey();
			final String scope = entry.getValue();
			logger.info(artifact + " @ " + scope);
		}

		assertEquals(4, dependencyMap.size());

	}

	@Test
	public void dependency5() throws Exception {

		final Logger logger = UnitHelp.logger();

		logger.info("===");

		final String uri = "org.apache.karaf.kar:org.apache.karaf.kar.core:pom:3.0.0";

		final MavenProject project = UnitHelp.newProject(uri);

		Set<String> scopeIncluded;
		{
			scopeIncluded = new HashSet<String>();
			scopeIncluded.add("compile");
		}

		Set<String> scopeExcluded;
		{
			scopeExcluded = new HashSet<String>();
			scopeExcluded.add("provided");
			scopeExcluded.add("system");
			scopeExcluded.add("test");
		}

		final RepositorySystem system = UnitHelp.newSystem();
		final RepositorySystemSession session = UnitHelp.newSession(system);

		final Map<String, String> resolverSettings = new HashMap<String, String>();

		final Set<String> packagingIncluded = new HashSet<String>();
		{
			packagingIncluded.add("bundle");
		}

		final Set<String> typeIncluded = new HashSet<String>();
		{
			typeIncluded.add("jar");
		}

		final MojoContext context = new MojoContext(logger, project, scopeIncluded,
				scopeExcluded, system, session, UnitHelp.newRepoRemoteList(),
				resolverSettings, packagingIncluded, typeIncluded, true);

		final Map<Artifact, String> dependencyMap = GenerateSemanticMojo
				.prepare(context);

		for (final Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			final Artifact artifact = entry.getKey();
			final String scope = entry.getValue();
			logger.info(artifact + " @ " + scope);
		}

		assertEquals(11, dependencyMap.size());

	}

}
