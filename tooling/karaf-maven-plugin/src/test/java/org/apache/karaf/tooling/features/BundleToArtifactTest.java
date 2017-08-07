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

import org.apache.karaf.tooling.utils.MojoSupport;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
import org.apache.maven.artifact.handler.manager.ArtifactHandlerManager;
import org.apache.maven.artifact.handler.manager.DefaultArtifactHandlerManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.junit.Assert;
import org.junit.Test;


public class BundleToArtifactTest extends MojoSupport {

    @SuppressWarnings("rawtypes")
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
        Artifact artifact = resourceToArtifact("mvn:org.foo/bar/1.0/kar", false);
        assert artifact.getGroupId().equals("org.foo");
        assert artifact.getArtifactId().equals("bar");
        assert artifact.getBaseVersion().equals("1.0");
        assert artifact.getType().equals("kar");
        assert artifact.getRepository() == null;
        assert artifact.getClassifier() == null;
    }

    @Test
    public void testURLWithClassifier() throws Exception {
        Artifact artifact = resourceToArtifact("mvn:org.foo/bar/1.0/kar/type", false);
        Assert.assertEquals("org.foo", artifact.getGroupId());
        Assert.assertEquals("bar", artifact.getArtifactId());
        Assert.assertEquals("1.0", artifact.getBaseVersion());
        Assert.assertEquals("kar", artifact.getType());
        Assert.assertNull(artifact.getRepository());
        Assert.assertEquals("type", artifact.getClassifier());
    }

    @Test
    public void testRemoteRepoURL() throws Exception {
        Artifact artifact = resourceToArtifact("mvn:http://baz.com!org.foo/bar/1.0/kar", false);
        Assert.assertEquals("org.foo", artifact.getGroupId());
        Assert.assertEquals("bar", artifact.getArtifactId());
        Assert.assertEquals("1.0", artifact.getBaseVersion());
        Assert.assertEquals("kar", artifact.getType());
        Assert.assertEquals("http://baz.com", artifact.getRepository().getUrl());
        Assert.assertNull(artifact.getClassifier());
    }
    
    @Test
    public void testRemoteRepoURLWithId() throws Exception {
        Artifact artifact = resourceToArtifact("mvn:http://baz.com@id=baz!org.foo/bar/1.0/kar", false);
        Assert.assertEquals("org.foo", artifact.getGroupId());
        Assert.assertEquals("bar", artifact.getArtifactId());
        Assert.assertEquals("1.0", artifact.getBaseVersion());
        Assert.assertEquals("kar", artifact.getType());
        Assert.assertEquals("http://baz.com", artifact.getRepository().getUrl());
        Assert.assertNull(artifact.getClassifier());
    }
}
