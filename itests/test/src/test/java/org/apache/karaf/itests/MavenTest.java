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
package org.apache.karaf.itests;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.NetworkConnector;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.url.mvn.MavenResolver;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.itests.KarafTestSupport.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;
import static org.ops4j.pax.exam.CoreOptions.*;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class MavenTest /*extends BaseTest*/ {

    public static Logger LOG = LoggerFactory.getLogger(MavenTest.class);

    private static Server server;
    private static int port;

    private static ExecutorService pool = Executors.newFixedThreadPool(1);

    private static AtomicBoolean requestAtPort3333Done = new AtomicBoolean(false);

    @Inject
    protected BundleContext bundleContext;

    // don't extend, because we don't want @Rule Retry
    private static KarafTestSupport karafTestSupport = new KarafTestSupport();

    /**
     * This server will act as HTTP proxy. @Test methods will change maven settings, where proxy is configured
     * and update <code>org.ops4j.pax.ul.mvn</code> PID, which will republish {@link MavenResolver} service.
     * @throws Exception
     */
    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request,
                               HttpServletResponse response) throws IOException, ServletException {
                try {
                    int port = baseRequest.getServerPort();
                    if (port == 3333 && request.getRequestURI().endsWith(".jar")) {
                        if (!requestAtPort3333Done.get()) {
                            requestAtPort3333Done.set(true);
                            // explicit timeout at first attempt - higher than the one set by Aether
                            Thread.sleep(4000);
                        }
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.getOutputStream().write(0x42);
                    } else {
                        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    }
                } catch (Exception ignored) {
                } finally {
                    baseRequest.setHandled(true);
                }
            }
        });
        server.start();
        port = ((NetworkConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        server.stop();
        pool.shutdown();
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    @Configuration
    public Option[] config() {
        MavenArtifactUrlReference karafUrl = maven().groupId("org.apache.karaf").artifactId("apache-karaf").versionAsInProject().type("tar.gz");

        String httpPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_HTTP_PORT), Integer.parseInt(MAX_HTTP_PORT)));
        String rmiRegistryPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));
        String sshPort = Integer.toString(getAvailablePort(Integer.parseInt(MIN_SSH_PORT), Integer.parseInt(MAX_SSH_PORT)));

        Option[] baseOptions = new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                replaceConfigurationFile("etc/org.ops4j.pax.logging.cfg", new File("src/test/resources/etc/org.ops4j.pax.logging.cfg")),
                editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "updateSnapshots", "none"),
                editConfigurationFilePut("etc/org.ops4j.pax.web.cfg", "org.osgi.service.http.port", httpPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort),
        };
        List<Option> options = new LinkedList<>(Arrays.asList(baseOptions));

        // Prepare default pax-url-aether configuration
        options.addAll(Arrays.asList(
//                new TimeoutOption(3600000),
//                KarafDistributionOption.debugConfiguration("8889", false),
                bundle("mvn:commons-io/commons-io/2.5"),
                mavenBundle(maven().groupId("javax.servlet").artifactId("javax.servlet-api").versionAsInProject()).noStart(),
                mavenBundle(maven().groupId("org.eclipse.jetty").artifactId("jetty-server").versionAsInProject()).noStart(),
                mavenBundle(maven().groupId("org.eclipse.jetty").artifactId("jetty-http").versionAsInProject()).noStart(),
                mavenBundle(maven().groupId("org.eclipse.jetty").artifactId("jetty-util").versionAsInProject()).noStart(),
                mavenBundle(maven().groupId("org.eclipse.jetty").artifactId("jetty-io").versionAsInProject()).noStart(),
                replaceConfigurationFile("etc/maven-settings.xml", new File("src/test/resources/etc/maven-settings.xml")),
                replaceConfigurationFile("etc/org.ops4j.pax.url.mvn.cfg", new File("src/test/resources/etc/org.ops4j.pax.url.mvn.cfg"))
        ));

        return options.toArray(new Option[options.size()]);
    }

    @Test
    public void smartRetriesTest() throws Exception {
        karafTestSupport.bundleContext = bundleContext;
        final ConfigurationAdmin cm = karafTestSupport.getOsgiService(ConfigurationAdmin.class, 3000);

        updateSettings();

        awaitMavenResolver(() -> {
            try {
                org.osgi.service.cm.Configuration config = cm.getConfiguration("org.ops4j.pax.url.mvn", null);
                Dictionary<String, Object> props = config.getProcessedProperties(null);
                props.put("org.ops4j.pax.url.mvn.globalChecksumPolicy", "ignore");
                props.put("org.ops4j.pax.url.mvn.socket.readTimeout", "2000");
                props.put("org.ops4j.pax.url.mvn.connection.retryCount", "0");
                props.put("org.ops4j.pax.url.mvn.repositories", "http://localhost:1111/repository@id=r1," +
                        "http://localhost:2222/repository@id=r2," +
                        "http://localhost:3333/repository@id=r3");
                config.update(props);
            } catch (Exception e) {
                fail(e.getMessage());
            }
        });

        // grab modified resolver
        MavenResolver resolver = karafTestSupport.getOsgiService(MavenResolver.class, 15000);
        try {
            resolver.resolve("mvn:commons-universalis/commons-universalis/42");
            fail("Should fail at first attempt");
        } catch (IOException e) {
            File f = resolver.resolve("mvn:commons-universalis/commons-universalis/42", e);
            byte[] commonsUniversalis = FileUtils.readFileToByteArray(f);
            assertThat(commonsUniversalis.length, equalTo(1));
            assertThat(commonsUniversalis[0], equalTo((byte) 0x42));
        }
    }

    private void updateSettings() throws IOException {
        File settingsFile = new File(System.getProperty("karaf.home"), "etc/maven-settings.xml");
        String settings = FileUtils.readFileToString(settingsFile, StandardCharsets.UTF_8);
        settings = settings.replace("@@port@@", Integer.toString(port));
        FileUtils.write(settingsFile, settings, StandardCharsets.UTF_8);
    }

    /**
     * Invoke config admin task and await reregistration of {@link MavenResolver} service
     */
    private void awaitMavenResolver(Runnable task) throws Exception {
        final CountDownLatch latch = new CountDownLatch(2);
        ServiceListener listener = event -> {
            if (event.getType() == ServiceEvent.UNREGISTERING || event.getType() == ServiceEvent.REGISTERED) {
                latch.countDown();
            }
        };
        bundleContext.addServiceListener(listener, "(objectClass=org.ops4j.pax.url.mvn.MavenResolver)");
        try {
            task.run();
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            bundleContext.removeServiceListener(listener);
        }
    }

}
