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
package org.apache.karaf.tooling.utils;

import org.eclipse.aether.artifact.DefaultArtifact;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class Dependency31HelperTest {

    @Test
    public void testIsFeatureWithStandardClassifier() {
        // groupId:artifactId:extension:classifier:version
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:xml:features:1.0");
        assertTrue(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithNonStandardClassifier() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:xml:features-core:1.0");
        assertTrue(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithAnotherNonStandardClassifier() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:xml:features-extra:1.0");
        assertTrue(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithKarExtension() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:kar:1.0");
        assertTrue(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithJarExtensionNoClassifier() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:jar:1.0");
        assertFalse(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithUnrelatedClassifier() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:xml:sources:1.0");
        assertFalse(Dependency31Helper.isFeature(artifact));
    }

    @Test
    public void testIsFeatureWithEmptyClassifier() {
        DefaultArtifact artifact = new DefaultArtifact("org.foo:bar:xml:1.0");
        assertFalse(Dependency31Helper.isFeature(artifact));
    }
}
