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

import java.lang.reflect.Field;
import java.util.HashMap;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Test;

/**
 * @version $Rev:$ $Date:$
 */
public class BundleToArtifactTest extends MojoSupport {

    public BundleToArtifactTest() throws NoSuchFieldException, IllegalAccessException {
        factory = new DefaultArtifactFactory();
        ArtifactHandlerManager artifactHandlerManager = new DefaultArtifactHandlerManager();
        Field f = factory.getClass().getDeclaredField("artifactHandlerManager");
        f.setAccessible(true);
        f.set(factory, artifactHandlerManager);
        f.setAccessible(false);

        f = artifactHandlerManager.getClass().getDeclaredField("artifactHandlers");
        f.setAccessible(true);
        f.set(artifactHandlerManager, new HashMap());
        f.setAccessible(false);
    }

    public void execute() throws MojoExecutionException, MojoFailureException {
    }
    
    @Test
    public void testSimpleURL() throws Exception {
        Artifact artifact = bundleToArtifact("mvn:org.foo/bar/1.0/kar", false);
        assert artifact.getGroupId().equals("org.foo");
        assert artifact.getArtifactId().equals("bar");
        assert artifact.getBaseVersion().equals("1.0");
        assert artifact.getType().equals("kar");
        assert artifact.getRepository() == null;
        assert artifact.getClassifier() == null;
    }

    @Test
    public void testURLWithClassifier() throws Exception {
        Artifact artifact = bundleToArtifact("mvn:org.foo/bar/1.0/kar/type", false);
        assert artifact.getGroupId().equals("org.foo");
        assert artifact.getArtifactId().equals("bar");
        assert artifact.getBaseVersion().equals("1.0");
        assert artifact.getType().equals("kar");
        assert artifact.getRepository() == null;
        assert artifact.getClassifier().equals("type");
    }

    @Test
    public void testRemoteRepoURL() throws Exception {
        Artifact artifact = bundleToArtifact("mvn:http://baz.com!org.foo/bar/1.0/kar", false);
        assert artifact.getGroupId().equals("org.foo");
        assert artifact.getArtifactId().equals("bar");
        assert artifact.getBaseVersion().equals("1.0");
        assert artifact.getType().equals("kar");
        assert artifact.getRepository().getUrl().equals("http://baz.com");
        assert artifact.getClassifier() == null;
    }
}
