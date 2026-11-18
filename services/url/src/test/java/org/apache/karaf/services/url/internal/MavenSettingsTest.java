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
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.*;

public class MavenSettingsTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testLocalRepositoryWithVariables() throws Exception {
        MavenSettings settings = load("<localRepository>${user.home}/m2</localRepository>");
        assertEquals(System.getProperty("user.home") + "/m2", settings.localRepository);
    }

    @Test
    public void testNamespacedSettings() throws Exception {
        File file = tempFolder.newFile();
        Files.write(file.toPath(), ("<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\">"
                + "<localRepository>/opt/m2</localRepository></settings>").getBytes(StandardCharsets.UTF_8));
        assertEquals("/opt/m2", MavenSettings.load(file).localRepository);
    }

    @Test
    public void testActiveProfiles() throws Exception {
        String profiles = "<profiles>"
                + "<profile><id>default</id><activation><activeByDefault>true</activeByDefault></activation>"
                + "<repositories><repository><id>default</id><url>https://default.example.com</url>"
                + "<snapshots><enabled>false</enabled></snapshots></repository></repositories></profile>"
                + "<profile><id>company</id><repositories><repository><id>company</id>"
                + "<url>https://repo.example.com</url></repository></repositories></profile>"
                + "</profiles>";
        // as Maven, a profile active by default is only active when no other profile is
        assertEquals(Collections.singletonList("https://default.example.com@id=default"),
                load(profiles).repositories);
        assertEquals(Collections.singletonList("https://repo.example.com@id=company@snapshots"),
                load(profiles + "<activeProfiles><activeProfile>company</activeProfile></activeProfiles>").repositories);
    }

    @Test
    public void testServers() throws Exception {
        MavenSettings settings = load("<servers><server><id>private</id><username>user</username>"
                + "<password>secret</password></server><server><id>encrypted</id><username>user</username>"
                + "<password>{COQLCE6DU6GtcS5P=}</password></server></servers>");
        assertEquals("user", settings.getServer("private").username);
        assertEquals("secret", settings.getServer("private").password);
        // not supported
        assertNull(settings.getServer("encrypted"));
        assertNull(settings.getServer(null));
    }

    @Test
    public void testMirrors() throws Exception {
        MavenSettings settings = load("<mirrors>"
                + "<mirror><id>all</id><mirrorOf>*,!apache</mirrorOf><url>https://all.example.com</url></mirror>"
                + "<mirror><id>central-mirror</id><mirrorOf>central</mirrorOf><url>https://central.example.com</url></mirror>"
                + "<mirror><id>external</id><mirrorOf>external:*</mirrorOf><url>https://external.example.com</url></mirror>"
                + "</mirrors>");
        // the mirror of the repository id first
        assertEquals("central-mirror", settings.getMirror("central", "https://repo1.maven.org/maven2").id);
        assertEquals("all", settings.getMirror("other", "https://other.example.com").id);
        // excluded from the first one
        assertEquals("external", settings.getMirror("apache", "https://repository.apache.org").id);
        assertNull(settings.getMirror("apache", "http://localhost:8080/repo"));
    }

    @Test
    public void testProxies() throws Exception {
        MavenSettings settings = load("<proxies>"
                + "<proxy><active>false</active><protocol>https</protocol><host>inactive</host></proxy>"
                + "<proxy><protocol>https</protocol><host>proxy</host><port>3128</port>"
                + "<nonProxyHosts>localhost|*.internal.com</nonProxyHosts></proxy>"
                + "</proxies>");
        MavenSettings.Proxy proxy = settings.getProxy(new URL("https://repo1.maven.org/maven2"));
        assertEquals("proxy", proxy.host);
        assertEquals(3128, proxy.port);
        assertNull(settings.getProxy(new URL("https://repo.internal.com/maven2")));
        assertNull(settings.getProxy(new URL("https://localhost/maven2")));
        // the proxy protocol is the one of the repository
        assertNull(settings.getProxy(new URL("http://repo1.maven.org/maven2")));
    }

    @Test
    public void testHttpProxyIsUsedForHttpsWithoutHttpsProxy() throws Exception {
        MavenSettings settings = load("<proxies>"
                + "<proxy><protocol>http</protocol><host>proxy</host><port>3128</port>"
                + "<nonProxyHosts>*.internal.com</nonProxyHosts></proxy>"
                + "</proxies>");
        assertEquals("proxy", settings.getProxy(new URL("https://repo1.maven.org/maven2")).host);
        assertEquals("proxy", settings.getProxy(new URL("http://repo1.maven.org/maven2")).host);
        assertNull(settings.getProxy(new URL("https://repo.internal.com/maven2")));

        // an https proxy first
        settings = load("<proxies>"
                + "<proxy><protocol>http</protocol><host>http-proxy</host></proxy>"
                + "<proxy><protocol>https</protocol><host>https-proxy</host></proxy>"
                + "</proxies>");
        assertEquals("https-proxy", settings.getProxy(new URL("https://repo1.maven.org/maven2")).host);
        assertEquals("http-proxy", settings.getProxy(new URL("http://repo1.maven.org/maven2")).host);
    }

    @Test(expected = java.io.IOException.class)
    public void testDoctypeIsRejected() throws Exception {
        load("<!DOCTYPE settings [<!ENTITY x SYSTEM \"file:///etc/passwd\">]><settings>&x;</settings>");
    }

    @Test
    public void testLocate() throws Exception {
        File file = tempFolder.newFile();
        assertEquals(file, MavenSettings.locate(file.getAbsolutePath()));
        assertEquals(file, MavenSettings.locate(file.toURI().toString()));
    }

    private MavenSettings load(String content) throws Exception {
        File file = tempFolder.newFile();
        String xml = content.startsWith("<!DOCTYPE") ? content : "<settings>" + content + "</settings>";
        Files.write(file.toPath(), xml.getBytes(StandardCharsets.UTF_8));
        return MavenSettings.load(file);
    }

}
