/**
 *  Copyright 2005-2014 Red Hat, Inc.
 *
 *  Red Hat licenses this file to you under the Apache License, version
 *  2.0 (the "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 *  implied.  See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package org.apache.karaf.services.mavenproxy.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.regex.Matcher;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.ops4j.pax.url.mvn.internal.AetherBasedResolver;
import org.ops4j.pax.url.mvn.internal.config.MavenConfigurationImpl;
import shaded.org.apache.commons.io.FileUtils;
import shaded.org.apache.maven.artifact.repository.metadata.io.xpp3.MetadataXpp3Reader;
import shaded.org.apache.maven.settings.Proxy;
import shaded.org.ops4j.util.property.DictionaryPropertyResolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MavenProxyServletTest {

    @Before
    public void setUp() {
        Properties props = new Properties();
        props.setProperty("localRepository", System.getProperty("java.io.tmpdir"));
    }

    @After
    public void tearDown() {

    }

    private MavenResolver createResolver() {
        return createResolver(System.getProperty("java.io.tmpdir"), null, null, null, 0, null, null, null);
    }

    private MavenResolver createResolver(String localRepo, String remoteRepos, String proxyProtocol, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword, String proxyNonProxyHosts) {
        Hashtable<String, String> props = new Hashtable<>();
        props.put("localRepository", localRepo);
        if (remoteRepos != null) {
            props.put("repositories", remoteRepos);
        }
        MavenConfigurationImpl config = new MavenConfigurationImpl(new DictionaryPropertyResolver(props), null);
        if (proxyProtocol != null) {
            Proxy proxy = new Proxy();
            proxy.setProtocol(proxyProtocol);
            proxy.setHost(proxyHost);
            proxy.setPort(proxyPort);
            proxy.setUsername(proxyUsername);
            proxy.setPassword(proxyPassword);
            proxy.setNonProxyHosts(proxyNonProxyHosts);
            config.getSettings().addProxy(proxy);
        }
        return new AetherBasedResolver(config);
    }

    @Test
    public void testMetadataRegex() {
        Matcher m = MavenProxyServlet.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata.xml", m.group(4));

        m = MavenProxyServlet.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-local.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata-local.xml", m.group(4));
        assertEquals("local", m.group(6));

        m = MavenProxyServlet.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata-rep-1234.xml");
        assertTrue(m.matches());
        assertEquals("maven-metadata-rep-1234.xml", m.group(4));
        assertEquals("rep-1234", m.group(6));

        m = MavenProxyServlet.ARTIFACT_METADATA_URL_REGEX.matcher("groupId/artifactId/version/maven-metadata.xml.md5");
        assertTrue(m.matches());
        assertEquals("maven-metadata.xml", m.group(4));
    }

    @Test
    public void testRepoRegex() {
        Matcher m = MavenProxyServlet.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServlet.REPOSITORY_ID_REGEX.matcher("https://repo.fusesource.com/nexus/content/repositories/releases@id=fusereleases");
        assertTrue(m.matches());
        assertEquals("fusereleases", m.group(2));

        m = MavenProxyServlet.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@snapshots@id=central");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServlet.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@id=central@snapshots");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));

        m = MavenProxyServlet.REPOSITORY_ID_REGEX.matcher("repo1.maven.org/maven2@noreleases@id=central@snapshots");
        assertTrue(m.matches());
        assertEquals("central", m.group(2));
    }

    @Test(expected = InvalidMavenArtifactRequest.class)
    public void testConvertNullPath() throws InvalidMavenArtifactRequest {
        MavenProxyServlet servlet = new MavenProxyServlet(createResolver(), 5, null, null, null);
        servlet.convertArtifactPathToCoord(null);
    }

    @Test
    public void testConvertNormalPath() throws InvalidMavenArtifactRequest {
        MavenProxyServlet servlet = new MavenProxyServlet(createResolver(), 5, null, null, null);

        assertEquals("groupId:artifactId:extension:version",servlet.convertArtifactPathToCoord("groupId/artifactId/version/artifactId-version.extension").toString());
        assertEquals("group.id:artifactId:extension:version",servlet.convertArtifactPathToCoord("group/id/artifactId/version/artifactId-version.extension").toString());
        assertEquals("group.id:artifact.id:extension:version",servlet.convertArtifactPathToCoord("group/id/artifact.id/version/artifact.id-version.extension").toString());

        assertEquals("group-id:artifactId:extension:version",servlet.convertArtifactPathToCoord("group-id/artifactId/version/artifactId-version.extension").toString());
        assertEquals("group-id:artifact-id:extension:version",servlet.convertArtifactPathToCoord("group-id/artifact-id/version/artifact-id-version.extension").toString());
        assertEquals("group-id:my-artifact-id:extension:version",servlet.convertArtifactPathToCoord("group-id/my-artifact-id/version/my-artifact-id-version.extension").toString());

        //Some real cases
        assertEquals("org.apache.camel.karaf:apache-camel:jar:LATEST",servlet.convertArtifactPathToCoord("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST.jar").toString());
        assertEquals("org.apache.cxf.karaf:apache-cxf:jar:LATEST",servlet.convertArtifactPathToCoord("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST.jar").toString());
    }

    @Test
    public void testConvertNormalPathWithClassifier() throws InvalidMavenArtifactRequest {
        MavenProxyServlet servlet = new MavenProxyServlet(createResolver(), 5, null, null, null);

        assertEquals("groupId:artifactId:extension:classifier:version",servlet.convertArtifactPathToCoord("groupId/artifactId/version/artifactId-version-classifier.extension").toString());
        assertEquals("group.id:artifactId:extension:classifier:version",servlet.convertArtifactPathToCoord("group/id/artifactId/version/artifactId-version-classifier.extension").toString());
        assertEquals("group.id:artifact.id:extension:classifier:version",servlet.convertArtifactPathToCoord("group/id/artifact.id/version/artifact.id-version-classifier.extension").toString());

        assertEquals("group.id:artifact.id:extension.sha1:classifier:version",servlet.convertArtifactPathToCoord("group/id/artifact.id/version/artifact.id-version-classifier.extension.sha1").toString());
        assertEquals("group.id:artifact.id:extension.md5:classifier:version",servlet.convertArtifactPathToCoord("group/id/artifact.id/version/artifact.id-version-classifier.extension.md5").toString());

        assertEquals("group-id:artifactId:extension:classifier:version",servlet.convertArtifactPathToCoord("group-id/artifactId/version/artifactId-version-classifier.extension").toString());
        assertEquals("group-id:artifact-id:extension:classifier:version",servlet.convertArtifactPathToCoord("group-id/artifact-id/version/artifact-id-version-classifier.extension").toString());
        assertEquals("group-id:my-artifact-id:extension:classifier:version",servlet.convertArtifactPathToCoord("group-id/my-artifact-id/version/my-artifact-id-version-classifier.extension").toString());

        //Some real cases
        assertEquals("org.apache.camel.karaf:apache-camel:xml:features:LATEST",servlet.convertArtifactPathToCoord("org/apache/camel/karaf/apache-camel/LATEST/apache-camel-LATEST-features.xml").toString());
        assertEquals("org.apache.cxf.karaf:apache-cxf:xml:features:LATEST",servlet.convertArtifactPathToCoord("org/apache/cxf/karaf/apache-cxf/LATEST/apache-cxf-LATEST-features.xml").toString());
    }

    @Test
    public void testStartServlet() throws Exception {
        String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        try {
            MavenResolver resolver = createResolver();
            MavenProxyServlet servlet = new MavenProxyServlet(resolver, 5, null, null, null);
            servlet.init();
        } finally {
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    @Test
    public void testDownloadUsingAuthenticatedProxy() throws Exception {
        testDownload(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                String proxyAuth = request.getHeader("Proxy-Authorization");
                if (proxyAuth == null || proxyAuth.trim().equals("")) {
                    response.setStatus(HttpServletResponse.SC_PROXY_AUTHENTICATION_REQUIRED);
                    response.addHeader("Proxy-Authenticate", "Basic realm=\"Proxy Server\"");
                    baseRequest.setHandled(true);
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getOutputStream().write(new byte[] { 0x42 });
                    response.getOutputStream().close();
                }
            }
        });
    }

    @Test
    public void testDownloadUsingNonAuthenticatedProxy() throws Exception {
        testDownload(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_OK);
                baseRequest.setHandled(true);
                response.getOutputStream().write(new byte[] { 0x42 });
                response.getOutputStream().close();
            }
        });
    }

    @Test
    public void testDownloadMetadata() throws Exception {
        final String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        FileUtils.deleteDirectory(new File("target/tmp"));

        Server server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                String result = null;
                if ("/repo1/org/apache/camel/camel-core/maven-metadata.xml".equals(target)) {
                    result =
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<metadata>\n" +
                            "  <groupId>org.apache.camel</groupId>\n" +
                            "  <artifactId>camel-core</artifactId>\n" +
                            "  <versioning>\n" +
                            "    <latest>2.14.0</latest>\n" +
                            "    <release>2.14.0</release>\n" +
                            "    <versions>\n" +
                            "      <version>1.6.1</version>\n" +
                            "      <version>1.6.2</version>\n" +
                            "      <version>1.6.3</version>\n" +
                            "      <version>1.6.4</version>\n" +
                            "      <version>2.0-M2</version>\n" +
                            "      <version>2.0-M3</version>\n" +
                            "      <version>2.0.0</version>\n" +
                            "      <version>2.1.0</version>\n" +
                            "      <version>2.2.0</version>\n" +
                            "      <version>2.3.0</version>\n" +
                            "      <version>2.4.0</version>\n" +
                            "      <version>2.5.0</version>\n" +
                            "      <version>2.6.0</version>\n" +
                            "      <version>2.7.0</version>\n" +
                            "      <version>2.7.1</version>\n" +
                            "      <version>2.7.2</version>\n" +
                            "      <version>2.7.3</version>\n" +
                            "      <version>2.7.4</version>\n" +
                            "      <version>2.7.5</version>\n" +
                            "      <version>2.8.0</version>\n" +
                            "      <version>2.8.1</version>\n" +
                            "      <version>2.8.2</version>\n" +
                            "      <version>2.8.3</version>\n" +
                            "      <version>2.8.4</version>\n" +
                            "      <version>2.8.5</version>\n" +
                            "      <version>2.8.6</version>\n" +
                            "      <version>2.9.0-RC1</version>\n" +
                            "      <version>2.9.0</version>\n" +
                            "      <version>2.9.1</version>\n" +
                            "      <version>2.9.2</version>\n" +
                            "      <version>2.9.3</version>\n" +
                            "      <version>2.9.4</version>\n" +
                            "      <version>2.9.5</version>\n" +
                            "      <version>2.9.6</version>\n" +
                            "      <version>2.9.7</version>\n" +
                            "      <version>2.9.8</version>\n" +
                            "      <version>2.10.0</version>\n" +
                            "      <version>2.10.1</version>\n" +
                            "      <version>2.10.2</version>\n" +
                            "      <version>2.10.3</version>\n" +
                            "      <version>2.10.4</version>\n" +
                            "      <version>2.10.5</version>\n" +
                            "      <version>2.10.6</version>\n" +
                            "      <version>2.10.7</version>\n" +
                            "      <version>2.11.0</version>\n" +
                            "      <version>2.11.1</version>\n" +
                            "      <version>2.11.2</version>\n" +
                            "      <version>2.11.3</version>\n" +
                            "      <version>2.11.4</version>\n" +
                            "      <version>2.12.0</version>\n" +
                            "      <version>2.12.1</version>\n" +
                            "      <version>2.12.2</version>\n" +
                            "      <version>2.12.3</version>\n" +
                            "      <version>2.12.4</version>\n" +
                            "      <version>2.13.0</version>\n" +
                            "      <version>2.13.1</version>\n" +
                            "      <version>2.13.2</version>\n" +
                            "      <version>2.14.0</version>\n" +
                            "    </versions>\n" +
                            "    <lastUpdated>20140918132816</lastUpdated>\n" +
                            "  </versioning>\n" +
                            "</metadata>\n" +
                            "\n";
                } else if ("/repo2/org/apache/camel/camel-core/maven-metadata.xml".equals(target)) {
                    result =
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<metadata modelVersion=\"1.1.0\">\n" +
                            "  <groupId>org.apache.camel</groupId>\n" +
                            "  <artifactId>camel-core</artifactId>\n" +
                            "  <versioning>\n" +
                            "    <latest>2.14.0.redhat-620034</latest>\n" +
                            "    <release>2.14.0.redhat-620034</release>\n" +
                            "    <versions>\n" +
                            "      <version>2.10.0.redhat-60074</version>\n" +
                            "      <version>2.12.0.redhat-610312</version>\n" +
                            "      <version>2.12.0.redhat-610328</version>\n" +
                            "      <version>2.12.0.redhat-610355</version>\n" +
                            "      <version>2.12.0.redhat-610378</version>\n" +
                            "      <version>2.12.0.redhat-610396</version>\n" +
                            "      <version>2.12.0.redhat-610399</version>\n" +
                            "      <version>2.12.0.redhat-610401</version>\n" +
                            "      <version>2.12.0.redhat-610402</version>\n" +
                            "      <version>2.12.0.redhat-611403</version>\n" +
                            "      <version>2.12.0.redhat-611405</version>\n" +
                            "      <version>2.12.0.redhat-611406</version>\n" +
                            "      <version>2.12.0.redhat-611408</version>\n" +
                            "      <version>2.12.0.redhat-611409</version>\n" +
                            "      <version>2.12.0.redhat-611410</version>\n" +
                            "      <version>2.12.0.redhat-611411</version>\n" +
                            "      <version>2.12.0.redhat-611412</version>\n" +
                            "      <version>2.14.0.redhat-620031</version>\n" +
                            "      <version>2.14.0.redhat-620033</version>\n" +
                            "      <version>2.14.0.redhat-620034</version>\n" +
                            "    </versions>\n" +
                            "    <lastUpdated>20141019130841</lastUpdated>\n" +
                            "  </versioning>\n" +
                            "</metadata>\n" +
                            "\n";
                }
                if (result == null) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    baseRequest.setHandled(true);
                    response.getOutputStream().close();
                } else {
                    response.setStatus(HttpServletResponse.SC_OK);
                    baseRequest.setHandled(true);
                    response.getOutputStream().write(result.getBytes());
                    response.getOutputStream().close();
                }
            }
        });
        server.start();

        try {
            int localPort = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            // TODO: local repo should point to target/tmp
            MavenResolver resolver = createResolver("target/tmp", "http://relevant.not/repo1@id=repo1,http://relevant.not/repo2@id=repo2", "http", "localhost", localPort, "fuse", "fuse", null);
            MavenProxyServlet servlet = new MavenProxyServlet(resolver, 5, null, null, null);

            AsyncContext context = EasyMock.createMock(AsyncContext.class);

            HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
            EasyMock.expect(request.getPathInfo()).andReturn("org/apache/camel/camel-core/maven-metadata.xml");
//            EasyMock.expect(request.getPathInfo()).andReturn("org/apache/camel/camel-core/LATEST/camel-core-LATEST.jar");
            EasyMock.expect(request.startAsync()).andReturn(context);
            context.setTimeout(EasyMock.anyInt());
            EasyMock.expectLastCall();

            HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            EasyMock.expect(response.getOutputStream()).andReturn(new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    baos.write(b, off, len);
                }
            }).anyTimes();
            response.setStatus(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentLength(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentType((String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            response.setDateHeader((String) EasyMock.anyObject(), EasyMock.anyLong());
            EasyMock.expectLastCall().anyTimes();
            response.setHeader((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();

            final CountDownLatch latch = new CountDownLatch(1);
            context.complete();
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    latch.countDown();
                    return null;
                }
            });

            EasyMock.makeThreadSafe(context, true);
            EasyMock.replay(request, response, context);

            servlet.init();
            servlet.doGet(request, response);

            latch.await();

            shaded.org.apache.maven.artifact.repository.metadata.Metadata m =
                    new MetadataXpp3Reader().read( new ByteArrayInputStream( baos.toByteArray() ), false );
            assertEquals("2.14.0.redhat-620034", m.getVersioning().getLatest());
            assertTrue(m.getVersioning().getVersions().contains("2.10.4"));
            assertTrue(m.getVersioning().getVersions().contains("2.12.0.redhat-610399"));

            EasyMock.verify(request, response, context);
        } finally {
            server.stop();
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    private void testDownload(Handler serverHandler) throws Exception {
        final String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        FileUtils.deleteDirectory(new File("target/tmp"));

        Server server = new Server(0);
        server.setHandler(serverHandler);
        server.start();

        try {
            int localPort = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            // TODO: local repo should point to target/tmp
            MavenResolver resolver = createResolver("target/tmp", "http://relevant.not/maven2@id=central", "http", "localhost", localPort, "fuse", "fuse", null);
            MavenProxyServlet servlet = new MavenProxyServlet(resolver, 5, null, null, null);

            AsyncContext context = EasyMock.createMock(AsyncContext.class);

            HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
            EasyMock.expect(request.getPathInfo()).andReturn("org.apache.camel/camel-core/2.13.0/camel-core-2.13.0-sources.jar");
            EasyMock.expect(request.startAsync()).andReturn(context);
            context.setTimeout(EasyMock.anyInt());
            EasyMock.expectLastCall();

            HttpServletResponse response = EasyMock.createMock(HttpServletResponse.class);
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            EasyMock.expect(response.getOutputStream()).andReturn(new ServletOutputStream() {
                @Override
                public void write(int b) throws IOException {
                    baos.write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    baos.write(b, off, len);
                }
            }).anyTimes();
            response.setStatus(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentLength(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentType((String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            response.setDateHeader((String) EasyMock.anyObject(), EasyMock.anyLong());
            EasyMock.expectLastCall().anyTimes();
            response.setHeader((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();

            final CountDownLatch latch = new CountDownLatch(1);
            context.complete();
            EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
                @Override
                public Object answer() throws Throwable {
                    latch.countDown();
                    return null;
                }
            });

            EasyMock.makeThreadSafe(context, true);
            EasyMock.replay(request, response, context);

            servlet.init();
            servlet.doGet(request, response);

            latch.await();

            Assert.assertArrayEquals(new byte[] { 0x42 }, baos.toByteArray());

            EasyMock.verify(request, response, context);
        } finally {
            server.stop();
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

    @Test
    public void testJarUploadFullMvnPath() throws Exception {
        String jarPath = "org.acme/acme-core/1.0/acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(jarPath, contents, false);
    }

    @Test
    public void testJarUploadWithMvnPom() throws Exception {
        String jarPath = "org.acme/acme-core/1.0/acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        addPom(jas, "org.acme", "acme-core", "1.0");
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(jarPath, contents, false);
    }

    @Test
    public void testJarUploadNoMvnPath() throws Exception {
        String jarPath = "acme-core-1.0.jar";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "hello.txt", "Hello!".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();
        testUpload(jarPath, contents, true);
    }

    @Test
    public void testWarUploadFullMvnPath() throws Exception {
        String warPath = "org.acme/acme-ui/1.0/acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, false);
    }

    @Test
    public void testWarUploadWithMvnPom() throws Exception {
        String warPath = "org.acme/acme-ui/1.0/acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        addPom(jas, "org.acme", "acme-ui", "1.0");
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, false);
    }

    @Test
    public void testWarUploadNoMvnPath() throws Exception {
        String warPath = "acme-ui-1.0.war";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jas = new JarOutputStream(baos);
        addEntry(jas, "WEB-INF/web.xml", "<web/>".getBytes());
        jas.close();

        byte[] contents = baos.toByteArray();

        testUpload(warPath, contents, true);
    }

    private static void addEntry(JarOutputStream jas, String name, byte[] content) throws Exception {
        JarEntry entry = new JarEntry(name);
        jas.putNextEntry(entry);
        if (content != null) {
            jas.write(content);
        }
        jas.closeEntry();
    }

    private static void addPom(JarOutputStream jas, String groupId, String artifactId, String version) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("groupId", groupId);
        properties.setProperty("artifactId", artifactId);
        properties.setProperty("version", version);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        properties.store(baos, null);
        addEntry(jas, String.format("META-INF/maven/%s/%s/%s/pom.properties", groupId, artifactId, version), baos.toByteArray());
    }

    private Map<String, String> testUpload(String path, final byte[] contents, boolean hasLocationHeader) throws Exception {
        return testUpload(path, contents, null, hasLocationHeader);
    }

    private Map<String, String> testUpload(String path, final byte[] contents, String location, boolean hasLocationHeader) throws Exception {
        return testUpload(path, contents, location, null, null, hasLocationHeader);
    }

    private Map<String, String> testUpload(String path, final byte[] contents, String location, String profile, String version, boolean hasLocationHeader) throws Exception {
        final String old = System.getProperty("karaf.data");
        System.setProperty("karaf.data", new File("target").getCanonicalPath());
        FileUtils.deleteDirectory(new File("target/tmp"));

        Server server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
        });
        server.start();

        try {
            int localPort = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
            MavenResolver resolver = createResolver("target/tmp", "http://relevant.not/maven2@id=central", "http", "localhost", localPort, "fuse", "fuse", null);
            MavenProxyServlet servlet = new MavenProxyServlet(resolver, 5, null, null, null);

            HttpServletRequest request = EasyMock.createMock(HttpServletRequest.class);
            EasyMock.expect(request.getPathInfo()).andReturn(path);
            EasyMock.expect(request.getInputStream()).andReturn(new ServletInputStream() {
                private int i;

                @Override
                public int read() throws IOException {
                    if (i >= contents.length) {
                        return -1;
                    }
                    return (contents[i++] & 0xFF);
                }
            });
            EasyMock.expect(request.getHeader("X-Location")).andReturn(location);

            final Map<String, String> headers = new HashMap<>();

            HttpServletResponse rm = EasyMock.createMock(HttpServletResponse.class);
            HttpServletResponse response = new HttpServletResponseWrapper(rm) {
                @Override
                public void addHeader(String name, String value) {
                    headers.put(name, value);
                }
            };
            response.setStatus(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentLength(EasyMock.anyInt());
            EasyMock.expectLastCall().anyTimes();
            response.setContentType((String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();
            response.setDateHeader((String) EasyMock.anyObject(), EasyMock.anyLong());
            EasyMock.expectLastCall().anyTimes();
            response.setHeader((String) EasyMock.anyObject(), (String) EasyMock.anyObject());
            EasyMock.expectLastCall().anyTimes();

            EasyMock.replay(request, rm);

            servlet.init();
            servlet.doPut(request, response);

            EasyMock.verify(request, rm);

            Assert.assertEquals(hasLocationHeader, headers.containsKey("X-Location"));

            return headers;
        } finally {
            server.stop();
            if (old != null) {
                System.setProperty("karaf.data", old);
            }
        }
    }

}
