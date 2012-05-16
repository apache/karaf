/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import junit.framework.Assert;

import org.junit.Test;

public class SimpleMavenResolverTest {
    private static final String ARTIFACT_COORDS = "mvn:org.apache.karaf.features/framework/1.0.0/xml/features";

    @Test
    public void mavenToPath() throws URISyntaxException {
        String resolvedPath = SimpleMavenResolver.fromMaven(new URI(ARTIFACT_COORDS));
        Assert.assertEquals("org/apache/karaf/features/framework/1.0.0/framework-1.0.0-features.xml", resolvedPath);
    }
    
    @Test
    public void testResolve() throws URISyntaxException {
        File basedir = new File(getClass().getClassLoader().getResource("foo").getPath()).getParentFile();
        File home = new File(basedir, "test-karaf-home");
        File system = new File(home, "system");
        SimpleMavenResolver resolver = new SimpleMavenResolver(Collections.singletonList(system));
        resolver.resolve(new URI(ARTIFACT_COORDS)); // Will throw exception if the artifact can not be resolved
    }
}
