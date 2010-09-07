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
package org.apache.karaf.archetypes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.UUID;

import junit.framework.TestCase;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.cli.ConsoleDownloadMonitor;
import org.apache.maven.embedder.MavenEmbedder;
import org.apache.maven.embedder.MavenEmbedderConsoleLogger;
import org.apache.maven.embedder.MavenEmbedderLogger;
import org.apache.maven.embedder.PlexusLoggerAdapter;
import org.apache.maven.monitor.event.DefaultEventMonitor;
import org.apache.maven.monitor.event.EventMonitor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

public abstract class AbstractArchetypeTest extends TestCase {

    private static final String MAVEN_REPO_LOCAL = "maven.repo.local";
    private static final File baseDir = new File(System.getProperty("basedir", ".")).getAbsoluteFile();
    private MavenEmbedder maven;
    private Properties sysProps = System.getProperties();
    private String version;

    protected void setUp() throws Exception {
        maven = new MavenEmbedder();
        maven.setOffline(false);
        System.err.println(sysProps.getProperty(MAVEN_REPO_LOCAL));
        if (StringUtils.isNotEmpty(sysProps.getProperty(MAVEN_REPO_LOCAL))) {
            maven.setLocalRepositoryDirectory(new File(sysProps.getProperty(MAVEN_REPO_LOCAL)));
        }

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        maven.setClassLoader(classLoader);
        MavenEmbedderLogger logger = new MavenEmbedderConsoleLogger();
        maven.setLogger(logger);
        maven.start();

        Field f = maven.getClass().getDeclaredField("wagonManager");
        f.setAccessible(true);
        WagonManager wagon = (WagonManager) f.get(maven);
        wagon.setOnline(true);

        MavenProject project = maven.readProject(new File(baseDir, "pom.xml"));
        maven.setInteractiveMode(false);
        version = project.getVersion();
    }

    protected void tearDown() throws Exception {
        maven.stop();
        maven = null;
        System.gc();
    }

    protected void testKarafArchetype(String artifactId) throws Exception {
        testArchetype("org.apache.karaf.archetypes", artifactId, version, null);
    }

    protected void testKarafArchetype(String artifactId, Properties archetypeRequiredParameters) throws Exception {
        testArchetype("org.apache.karaf.archetypes",  artifactId, version, archetypeRequiredParameters);
    }

    protected void testArchetype(String groupId, String artifactId, String version, Properties archetypeRequiredParameters) throws Exception {
        File targetDir = new File(baseDir, "target/archetypes/" + artifactId);
        FileUtils.deleteDirectory(targetDir);
        targetDir.mkdirs();
        EventMonitor eventMonitor = new DefaultEventMonitor(new PlexusLoggerAdapter(
                new MavenEmbedderConsoleLogger()));

        Properties props = new Properties();
        props.setProperty("archetypeGroupId", groupId);
        props.setProperty("archetypeArtifactId", artifactId);
        props.setProperty("archetypeVersion", version);
        props.setProperty("interactiveMode", "false");
        props.setProperty("groupId", "sample");
        props.setProperty("artifactId", UUID.randomUUID().toString());
        props.setProperty("user.dir", targetDir.getAbsolutePath());
        props.setProperty("basedir", targetDir.getAbsolutePath());

        //Some archetypes require additional parameters
        if (archetypeRequiredParameters != null) {
            for (Enumeration e = archetypeRequiredParameters.propertyNames(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                props.setProperty(key, archetypeRequiredParameters.getProperty(key));
            }
        }

        MavenProject parent = maven.readProject(getDefaultArchetypePom(new File(targetDir, "pom.xml")));
        System.setProperties((Properties) sysProps.clone());

        maven.execute(parent,
                Collections.singletonList("archetype:generate"),
                eventMonitor,
                new ConsoleDownloadMonitor(),
                props,
                targetDir);


        System.setProperties((Properties) sysProps.clone());
        targetDir = new File(targetDir, props.getProperty("artifactId"));
        MavenProject prj = maven.readProject(new File(targetDir, "pom.xml"));

        maven.execute(prj,
                Collections.singletonList("package"),
                eventMonitor,
                new ConsoleDownloadMonitor(),
                new Properties(),
                targetDir);
    }

    private File getDefaultArchetypePom(File pomFile) throws IOException {
        URL archetypePom = getClass().getClassLoader().getResource("archetype-pom.xml");
        FileUtils.copyURLToFile(archetypePom, pomFile);
        return pomFile;
    }
}
