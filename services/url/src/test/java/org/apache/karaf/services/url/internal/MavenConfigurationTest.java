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
package org.apache.karaf.services.url.internal;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.Hashtable;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

public class MavenConfigurationTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testDefaultConfiguration() throws Exception {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        MavenConfiguration config = new MavenConfiguration(context, emptySettings());

        assertEquals(new File(System.getProperty("user.home"), ".m2/repository"), config.getLocalRepository());
        // not in a Karaf instance: no default repositories
        assertTrue(config.getDefaultRepositories().isEmpty());
        assertEquals(Arrays.asList("https://repo1.maven.org/maven2@id=central",
                "https://repository.apache.org/content/groups/snapshots-group@id=apache@snapshots@noreleases"),
                config.getRepositories());
        assertNull(config.getUpdatePolicy());
        assertNull(config.getChecksumPolicy());
        assertEquals(5000, config.getConnectionTimeout());
        assertEquals(30000, config.getReadTimeout());
        assertTrue(config.isCertificateCheck());
        assertEquals(3, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testCustomLocalRepository() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("localRepository", "/tmp/test-repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/tmp/test-repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testFullyQualifiedPropertyKey() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("org.apache.karaf.url.mvn.localRepository", "/tmp/fq-repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/tmp/fq-repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testRepositoriesParsing() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("repositories", "https://repo1.maven.org/maven2@id=central, https://repo.apache.org@id=apache");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(2, config.getRepositories().size());
        assertEquals("https://repo1.maven.org/maven2@id=central", config.getRepositories().get(0));
        assertEquals("https://repo.apache.org@id=apache", config.getRepositories().get(1));

        verify(context);
    }

    @Test
    public void testDefaultRepositoriesParsing() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("defaultRepositories", "/opt/karaf/system@id=system@snapshots, /opt/karaf/data/kar@id=kar");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(2, config.getDefaultRepositories().size());

        verify(context);
    }

    @Test
    public void testTimeoutConfiguration() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("timeout", "10000");
        props.put("socket.connectionTimeout", "3000");
        props.put("socket.readTimeout", "60000");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(3000, config.getConnectionTimeout());
        assertEquals(60000, config.getReadTimeout());

        verify(context);
    }

    @Test
    public void testTimeoutFallsBackToDefaultTimeout() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("timeout", "7000");

        MavenConfiguration config = new MavenConfiguration(context, props);

        // connectionTimeout should fall back to the "timeout" value
        assertEquals(7000, config.getConnectionTimeout());

        verify(context);
    }

    @Test
    public void testCertificateCheckDisabled() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("certificateCheck", "false");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertFalse(config.isCertificateCheck());

        verify(context);
    }

    @Test
    public void testRetryCount() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("connection.retryCount", "5");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(5, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testPolicies() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("globalUpdatePolicy", "daily");
        props.put("globalChecksumPolicy", "fail");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals("daily", config.getUpdatePolicy());
        assertEquals("fail", config.getChecksumPolicy());

        verify(context);
    }

    @Test
    public void testVariableSubstitution() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home")).andReturn("/opt/karaf").anyTimes();
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("defaultRepositories", "${karaf.home}/system@id=system");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(1, config.getDefaultRepositories().size());
        assertEquals("/opt/karaf/system@id=system", config.getDefaultRepositories().get(0));

        verify(context);
    }

    @Test
    public void testRelativeLocalRepositoryResolved() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home")).andReturn("/opt/karaf").anyTimes();
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("localRepository", "data/repo");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/opt/karaf", "data/repo"), config.getLocalRepository());

        verify(context);
    }

    @Test
    public void testLocalRepositoryStripsOptionsAndResolvesFileUri() throws Exception {
        File karafHome = tempFolder.newFolder("karaf-home");
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home")).andReturn(karafHome.getAbsolutePath()).anyTimes();
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        File jobRepo = new File(tempFolder.getRoot(), "jenkins/workspace/job@2/m2").getAbsoluteFile();
        Hashtable<String, Object> props = emptySettings();
        props.put("localRepository", jobRepo.getAbsolutePath() + "@id=local@snapshots");
        assertEquals(jobRepo, new MavenConfiguration(context, props).getLocalRepository());

        File uriRepo = new File(tempFolder.getRoot(), "opt/m2").getAbsoluteFile();
        props.put("localRepository", uriRepo.toURI() + "@id=local");
        assertEquals(uriRepo, new MavenConfiguration(context, props).getLocalRepository());

        props.put("localRepository", "file:data/m2@id=local");
        assertEquals(new File(karafHome, "data/m2"), new MavenConfiguration(context, props).getLocalRepository());
    }

    @Test
    public void testInvalidIntPropertyUsesDefault() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("connection.retryCount", "notanumber");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(3, config.getRetryCount());

        verify(context);
    }

    @Test
    public void testEmptyRepositoriesString() {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = new Hashtable<>();
        props.put("repositories", "   ");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertTrue(config.getRepositories().isEmpty());

        verify(context);
    }

    @Test
    public void testKarafDefaultRepositoriesWhenNotConfigured() throws Exception {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty("karaf.home.uri")).andReturn("file:/opt/karaf/").anyTimes();
        expect(context.getProperty("karaf.base.uri")).andReturn("file:/opt/instance/").anyTimes();
        expect(context.getProperty("karaf.data.uri")).andReturn("file:/opt/instance/data/").anyTimes();
        expect(context.getProperty("karaf.default.repository")).andReturn("system").anyTimes();
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        MavenConfiguration config = new MavenConfiguration(context, emptySettings());

        assertEquals(Arrays.asList("file:/opt/karaf/system@id=system.repository@snapshots",
                "file:/opt/instance/data/kar@id=kar.repository@multi@snapshots",
                "file:/opt/instance/system@id=child.system.repository@snapshots"), config.getDefaultRepositories());
    }

    @Test
    public void testDefaultValueOfVariable() throws Exception {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = emptySettings();
        props.put("defaultRepositories", "/opt/karaf/${karaf.default.repository:-system}@id=system");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(Collections.singletonList("/opt/karaf/system@id=system"), config.getDefaultRepositories());
    }

    @Test
    public void testSettings() throws Exception {
        BundleContext context = createMock(BundleContext.class);
        expect(context.getProperty(anyString())).andReturn(null).anyTimes();
        replay(context);

        Hashtable<String, Object> props = settings("<localRepository>/opt/m2</localRepository>"
                + "<profiles><profile><id>company</id><repositories><repository><id>company</id>"
                + "<url>https://repo.example.com/maven2</url><releases><enabled>false</enabled></releases>"
                + "</repository></repositories></profile></profiles>"
                + "<activeProfiles><activeProfile>company</activeProfile></activeProfiles>");

        MavenConfiguration config = new MavenConfiguration(context, props);

        assertEquals(new File("/opt/m2"), config.getLocalRepository());
        assertEquals(Collections.singletonList("https://repo.example.com/maven2@id=company@snapshots@noreleases"),
                config.getRepositories());

        // the configured local repository and repositories have the priority
        props.put("localRepository", "/opt/repository");
        props.put("repositories", "https://repo1.maven.org/maven2@id=central");
        config = new MavenConfiguration(context, props);

        assertEquals(new File("/opt/repository"), config.getLocalRepository());
        assertEquals(Collections.singletonList("https://repo1.maven.org/maven2@id=central"), config.getRepositories());

        // unless they start with +
        props.put("repositories", "+https://repo1.maven.org/maven2@id=central");
        config = new MavenConfiguration(context, props);

        assertEquals(Arrays.asList("https://repo1.maven.org/maven2@id=central",
                "https://repo.example.com/maven2@id=company@snapshots@noreleases"), config.getRepositories());
    }

    private Hashtable<String, Object> emptySettings() throws IOException {
        return settings("");
    }

    /**
     * A configuration using a settings file with the given content, so the tests don't depend on the settings of the
     * user.
     */
    private Hashtable<String, Object> settings(String content) throws IOException {
        File file = tempFolder.newFile();
        Files.write(file.toPath(), ("<settings>" + content + "</settings>").getBytes(StandardCharsets.UTF_8));
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("settings", file.getAbsolutePath());
        return props;
    }

}
