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

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.karaf.features.FeaturesNamespaces;
import org.apache.karaf.features.internal.model.Features;
import org.apache.karaf.features.internal.model.Feature;
import org.apache.karaf.features.internal.model.JaxbUtil;
import org.apache.karaf.tooling.features.GenerateDescriptorMojo2;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.xml.sax.SAXException;

import org.codehaus.plexus.DefaultPlexusContainer;
import org.sonatype.aether.RepositorySystem;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;

public class GenerateDescriptorMojo2Test {

	@Test
	public void dependency0() throws Exception {

		System.out.println("---");

		String uri = "com.carrotgarden.osgi:carrot-osgi-anno-scr-make:pom:1.1.3";

		MavenProject project = UnitHelp.newProject(uri);

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

		RepositorySystem system = UnitHelp.newSystem();
		RepositorySystemSession session = UnitHelp.newSession(system);

		Map<Artifact, String> dependencyMap = GenerateDescriptorMojo2.prepare(
				project, scopeIncluded, scopeExcluded, system, session);

		for (Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			Artifact artifact = entry.getKey();
			String scope = entry.getValue();
			System.out.println( artifact + " @ " + scope);
		}

		assertEquals(1, dependencyMap.size());

	}

	@Test
	public void dependency1() throws Exception {

		System.out.println("---");

		String uri = "com.carrotgarden.osgi:carrot-osgi-anno-scr-make:pom:1.1.3";

		MavenProject project = UnitHelp.newProject(uri);

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

		RepositorySystem system = UnitHelp.newSystem();
		RepositorySystemSession session = UnitHelp.newSession(system);

		Map<Artifact, String> dependencyMap = GenerateDescriptorMojo2.prepare(
				project, scopeIncluded, scopeExcluded, system, session);

		for (Map.Entry<Artifact, String> entry : dependencyMap.entrySet()) {
			Artifact artifact = entry.getKey();
			String scope = entry.getValue();
			System.out.println( artifact + " @ " + scope);
		}
		
		assertEquals(5, dependencyMap.size());

	}

}
