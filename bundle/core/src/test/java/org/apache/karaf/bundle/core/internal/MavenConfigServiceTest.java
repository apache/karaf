/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.bundle.core.internal;

import java.io.File;
import java.util.Hashtable;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MavenConfigServiceTest {

    @Test
    public void testLocalRepoEmpty() throws Exception {
        Hashtable<String, Object> config = new Hashtable<>();
        assertEquals(null, MavenConfigService.getLocalRepoFromConfig(config));
    }

    @Test
    public void testLocalRepoExplicit() throws Exception {
        Hashtable<String, Object> config = new Hashtable<>();
        config.put("org.ops4j.pax.url.mvn.localRepository", "foo/bar");
        assertEquals("foo/bar", MavenConfigService.getLocalRepoFromConfig(config));
    }

    @Test
    public void testLocalRepoFromSettings() throws Exception {
        Hashtable<String, Object> config = new Hashtable<>();
        config.put("org.ops4j.pax.url.mvn.settings", getClass().getResource("/settings.xml").getPath());
        assertEquals("foo/bar", MavenConfigService.getLocalRepoFromConfig(config));
    }

    @Test
    public void testLocalRepoFromSettingsNs() throws Exception {
        Hashtable<String, Object> config = new Hashtable<>();
        config.put("org.ops4j.pax.url.mvn.settings", getClass().getResource("/settings2.xml").getPath());
        assertEquals("foo/bar", MavenConfigService.getLocalRepoFromConfig(config));
    }

    @Test
    public void testLocalRepoOfTheKarafUrlService() throws Exception {
        Hashtable<String, Object> config = new Hashtable<>();
        config.put("org.apache.karaf.url.mvn.localRepository", "foo/bar");
        assertEquals("foo/bar", MavenConfigService.getLocalRepoFromConfig(config, "org.apache.karaf.url.mvn"));
        assertEquals(null, MavenConfigService.getLocalRepoFromConfig(config));
    }

    @Test
    public void testRelativeLocalRepo() throws Exception {
        File base = new File("karaf-home").getAbsoluteFile();
        assertEquals(new File(base, "foo/bar"), MavenConfigService.toFile("foo/bar", base.getPath()));
        assertEquals(new File("foo/bar").getAbsoluteFile(), MavenConfigService.toFile("foo/bar", null));
        File absolute = new File("m2").getAbsoluteFile();
        assertEquals(absolute, MavenConfigService.toFile(absolute.getPath(), base.getPath()));
    }

    @Test
    public void testStripOptions() throws Exception {
        assertEquals("/opt/m2", MavenConfigService.stripOptions("/opt/m2@id=local"));
        assertEquals("/var/jenkins/workspace/job@2/m2",
                MavenConfigService.stripOptions("/var/jenkins/workspace/job@2/m2@id=local@snapshots"));
        assertEquals("/var/jenkins/workspace/job@2/m2",
                MavenConfigService.stripOptions("/var/jenkins/workspace/job@2/m2"));
    }

}
