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

package org.apache.karaf.tooling.ap;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;

import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.collection.CollectRequest;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.graph.DependencyFilter;
import org.sonatype.aether.graph.DependencyNode;
import org.sonatype.aether.repository.LocalRepository;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactRequest;
import org.sonatype.aether.resolution.ArtifactResult;
import org.sonatype.aether.resolution.DependencyRequest;
import org.sonatype.aether.resolution.DependencyResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.filter.ScopeDependencyFilter;
import org.sonatype.aether.util.graph.PreorderNodeListGenerator;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.internal.MavenRepositorySystemSession;

public class UnitHelp {

	/**
	 * Verify operation manually.
	 */
	public static void main(String[] args) throws Exception {

		final RepositorySystem system = newSystem();

		final RepositorySystemSession session = newSession(system);

		// String uri = "jmock:jmock:pom:1.1.0";
		String uri = "org.apache.maven:maven-profile:2.2.1";

		final Artifact artifact = new DefaultArtifact(uri);

		final Dependency dependency = new Dependency(artifact, "compile");

		final RemoteRepository central = newRepoCentral();

		final CollectRequest collectRequest = new CollectRequest();
		collectRequest.setRoot(dependency);
		collectRequest.addRepository(central);

		final DependencyNode collectNode = system.collectDependencies(session,
				collectRequest).getRoot();

		final List<String> include = new ArrayList<String>();
		final List<String> exclude = new ArrayList<String>();

		final DependencyFilter filter = new ScopeDependencyFilter(include,
				exclude);

		final DependencyRequest dependencyRequest = new DependencyRequest(
				collectNode, filter);

		final DependencyResult result = system.resolveDependencies(session,
				dependencyRequest);

		final DependencyNode resolveNode = result.getRoot();

		final PreorderNodeListGenerator generator = new PreorderNodeListGenerator();

		resolveNode.accept(generator);

		final String[] pathArray = generator.getClassPath().split(
				File.pathSeparator);

		for (String path : pathArray) {
			System.out.println("path = " + path);
		}

		//

		final MavenProject project = newProject("org.apache.maven:maven-model:pom:3.0");

		System.out.println("project = " + project);

	}

	/**
	 * Remote central repository.
	 */
	public static RemoteRepository newRepoCentral() throws Exception {
		final RemoteRepository central = new RemoteRepository("central",
				"default", "http://repo1.maven.org/maven2/");
		return central;
	}

	/**
	 * Local user repository.
	 */
	public static LocalRepository newRepoLocal() throws Exception {
		final File home = new File(System.getProperty("user.home"));
		final File repo = new File(home, ".m2/repository");
		final LocalRepository localRepo = new LocalRepository(repo);
		return localRepo;
	}

	/**
	 * Resolve maven URI into maven project.
	 * <p>
	 * Provides only model and artifact.
	 */
	public static MavenProject newProject(String mavenURI) throws Exception {

		final RepositorySystem system = newSystem();
		final RepositorySystemSession session = newSession(system);
		final RemoteRepository central = newRepoCentral();

		final ArtifactRequest request = new ArtifactRequest();
		request.setArtifact(new DefaultArtifact(mavenURI));
		request.addRepository(central);

		final ArtifactResult result = system.resolveArtifact(session, request);

		final Artifact artifact = result.getArtifact();

		final File input = artifact.getFile();

		final ModelReader reader = new DefaultModelReader();

		final Model model = reader.read(input, null);

		final MavenProject project = new MavenProject(model);

		project.setArtifact(RepositoryUtils.toArtifact(artifact));

		return project;

	}

	/**
	 * Default repository session.
	 */
	public static RepositorySystemSession newSession() throws Exception {
		return newSession(newSystem());
	}

	/**
	 * Default repository session.
	 */
	public static MavenRepositorySystemSession newSession(
			RepositorySystem system) throws Exception {

		final LocalRepository localRepo = newRepoLocal();

		final MavenRepositorySystemSession session = new MavenRepositorySystemSession();

		session.setLocalRepositoryManager(system
				.newLocalRepositoryManager(localRepo));

		return session;

	}

	/**
	 * Default repository system.
	 */
	public static RepositorySystem newSystem() throws Exception {
		return new DefaultPlexusContainer().lookup(RepositorySystem.class);
	}

}
