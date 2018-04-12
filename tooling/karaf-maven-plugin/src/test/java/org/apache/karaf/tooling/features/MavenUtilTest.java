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
package org.apache.karaf.tooling.features;

import org.apache.karaf.tooling.utils.Dependency31Helper;
import org.junit.Test;

import static org.apache.karaf.tooling.utils.MavenUtil.aetherToMvn;
import static org.apache.karaf.tooling.utils.MavenUtil.mvnToAether;
import static org.junit.Assert.assertEquals;

public class MavenUtilTest {

    @Test
    public void testMvnToAether() throws Exception {
        assertEquals("org.foo:org.foo.bar:1.0-SNAPSHOT", mvnToAether("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT"));
        assertEquals("org.foo:org.foo.bar:kar:1.0-SNAPSHOT", mvnToAether("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/kar"));
        assertEquals("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT", mvnToAether("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/xml/features"));
        assertEquals("org.foo:org.foo.bar:1.0-SNAPSHOT", mvnToAether("wrap:mvn:org.foo/org.foo.bar/1.0-SNAPSHOT"));
        assertEquals("org.foo:org.foo.bar:kar:1.0-SNAPSHOT", mvnToAether("wrap:mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/kar"));
        assertEquals("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT", mvnToAether("wrap:mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/xml/features"));
    }

    @Test
    public void testAetherToMvn() throws Exception {
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT", aetherToMvn("org.foo:org.foo.bar:1.0-SNAPSHOT"));
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/kar", aetherToMvn("org.foo:org.foo.bar:kar:1.0-SNAPSHOT"));
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/xml/features", aetherToMvn("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT"));
    }

    @Test
    public void testPathFromMvnEclipse() throws Exception {
        Dependency31Helper helper = new Dependency31Helper(null, null, null);
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT.jar", helper.pathFromMaven("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT"));
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT.kar", helper.pathFromMaven("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/kar"));
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT-features.xml", helper.pathFromMaven("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/xml/features"));
    }

    @Test
    public void testPathFromAetherEclipse() throws Exception {
        Dependency31Helper helper = new Dependency31Helper(null, null, null);
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT.jar", helper.pathFromAether("org.foo:org.foo.bar:1.0-SNAPSHOT"));
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT.kar", helper.pathFromAether("org.foo:org.foo.bar:kar:1.0-SNAPSHOT"));
        assertEquals("org/foo/org.foo.bar/1.0-SNAPSHOT/org.foo.bar-1.0-SNAPSHOT-features.xml", helper.pathFromAether("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT"));
    }

    @Test
    public void testEclipseArtifactToMvn() throws Exception {
        Dependency31Helper helper = new Dependency31Helper(null, null, null);
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:1.0-SNAPSHOT"), "1.0-SNAPSHOT"));
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/kar", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:kar:1.0-SNAPSHOT"), "1.0-SNAPSHOT"));
        assertEquals("mvn:org.foo/org.foo.bar/1.0-SNAPSHOT/xml/features", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT"), "1.0-SNAPSHOT"));
    }

    @Test
    public void testEclipseArtifactToMvnWithRange() throws Exception {
        Dependency31Helper helper = new Dependency31Helper(null, null, null);
        assertEquals("mvn:org.foo/org.foo.bar/[1.0,2.0)", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:1.0-SNAPSHOT"), "[1.0,2.0)"));
        assertEquals("mvn:org.foo/org.foo.bar/[1.0,2.0)/kar", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:kar:1.0-SNAPSHOT"), "[1.0,2.0)"));
        assertEquals("mvn:org.foo/org.foo.bar/[1.0,2.0)/xml/features", helper.artifactToMvn(new org.eclipse.aether.artifact.DefaultArtifact("org.foo:org.foo.bar:xml:features:1.0-SNAPSHOT"), "[1.0,2.0)"));
    }
}
