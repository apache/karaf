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
package org.apache.karaf.itests.mavenresolver;

import static org.apache.karaf.itests.KarafTestSupport.MAX_RMI_REG_PORT;
import static org.apache.karaf.itests.KarafTestSupport.MAX_RMI_SERVER_PORT;
import static org.apache.karaf.itests.KarafTestSupport.MIN_RMI_REG_PORT;
import static org.apache.karaf.itests.KarafTestSupport.MIN_RMI_SERVER_PORT;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import static org.osgi.framework.Constants.OBJECTCLASS;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.karaf.itests.KarafTestSupport;
import org.apache.karaf.itests.monitoring.Activator;
import org.apache.karaf.itests.monitoring.ServiceMonitor;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.container.internal.JavaVersionUtil;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.ops4j.store.intern.TemporaryStore;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Don't extend KarafTestSupport, because we don't want @Rule Retry
public abstract class KarafMinimalMonitoredTestSupport {

    public static Logger LOG = LoggerFactory.getLogger(KarafMinimalMonitoredTestSupport.class);

    protected static final String MAVEN_RESOLVER = "org.ops4j.pax.url.mvn.MavenResolver";

    /**
     * Service property set by pax-url-aether on each MavenResolver it registers: {@code bundlecontext}
     * when built without ConfigAdmin properties (at bundle start, or for a {@code updated(null)} from
     * ConfigAdmin), {@code configadmin} when built from the {@code org.ops4j.pax.url.mvn} configuration.
     */
    protected static final String CONFIGURATION = "configuration";

    private static final long SERVICE_TIMEOUT = 30000;

    @Inject
    protected ServiceMonitor serviceMonitor;
    
    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.IMPORT_PACKAGE, ServiceMonitor.class.getPackage().getName());
        return probe;
    }

    public Option[] baseConfig() throws Exception {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf").artifactId("apache-karaf-minimal")
                .versionAsInProject().type("tar.gz");

        String rmiRegistryPort = Integer.toString(KarafTestSupport.getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(KarafTestSupport.getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));

        Store<InputStream> store = new TemporaryStore(new File("target/exam"), false);
        Handle handle = store.store(createMonitorBundle());
        URL url = store.getLocation(handle).toURL();
        if (JavaVersionUtil.getMajorVersion() >= 9) {
            return new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/startup.properties", "file:../../" + new File(url.toURI()).getName(), "1"),
                composite(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", new File("target/test-classes/etc/org.apache.karaf.features.cfg"))),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.security=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.net=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.lang=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/java.util=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.base/sun.nio.ch=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.naming/javax.naming.spi=ALL-UNNAMED"),
                new VMOption("--add-opens"),
                new VMOption("java.rmi/sun.rmi.transport.tcp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.file=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.ftp=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.http=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.https=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.protocol.jar=ALL-UNNAMED"),
                new VMOption("--add-exports=java.base/sun.net.www.content.text=ALL-UNNAMED"),
                new VMOption("--add-exports=jdk.naming.rmi/com.sun.jndi.url.rmi=ALL-UNNAMED"),
                new VMOption("--add-exports=java.rmi/sun.rmi.registry=ALL-UNNAMED"),
                new VMOption("-classpath"),
                new VMOption("lib/jdk9plus/*" + File.pathSeparator + "lib/boot/*")
            };
        } else {
            return new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/startup.properties", "file:../../" + new File(url.toURI()).getName(), "1"),
                composite(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", new File("target/test-classes/etc/org.apache.karaf.features.cfg")))
            };
        }
    }

    private InputStream createMonitorBundle() {
        return TinyBundles.bundle()
                .setHeader(Constants.BUNDLE_ACTIVATOR, Activator.class.getName())
                .setHeader(Constants.EXPORT_PACKAGE, ServiceMonitor.class.getPackage().getName())
                .addClass(Activator.class)
                .addClass(ServiceMonitor.class)
                .build(TinyBundles.bndBuilder());
    }

    protected List<ServiceEvent> serviceEventsFor(String serviceName) {
        return serviceMonitor.getEvents().stream()
                .filter(event -> serviceName.equals(((String[]) event.getServiceReference().getProperty(OBJECTCLASS))[0]))
                .collect(Collectors.toList());
    }

    /**
     * Waits until a single service of the given class is left registered, and that service has
     * {@code property=value}. The registrations happen asynchronously (ConfigAdmin calls
     * ManagedService.updated() on its own thread), so the probe can run before the last one.
     *
     * @return the {@link ServiceEvent#REGISTERED} events of that service class, in order
     */
    protected List<ServiceEvent> awaitFinalRegistration(String serviceName, String property, Object value) throws InterruptedException {
        long deadline = System.currentTimeMillis() + SERVICE_TIMEOUT;
        while (true) {
            List<ServiceEvent> events = serviceEventsFor(serviceName);
            List<ServiceEvent> registered = events.stream()
                    .filter(event -> event.getType() == ServiceEvent.REGISTERED)
                    .collect(Collectors.toList());
            long unregistering = events.stream().filter(event -> event.getType() == ServiceEvent.UNREGISTERING).count();
            if (registered.size() - unregistering == 1
                    && value.equals(registered.get(registered.size() - 1).getServiceReference().getProperty(property))) {
                return registered;
            }
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("No single " + serviceName + " left registered with " + property + "=" + value
                        + " after " + SERVICE_TIMEOUT + " ms, events: " + describe(events, property));
            }
            Thread.sleep(100);
        }
    }

    /**
     * @return the type and the given property of each event, e.g. {@code [REGISTERED(bundlecontext), ...]}
     */
    protected String describe(List<ServiceEvent> events, String property) {
        return events.stream().map(event -> {
            String type;
            switch (event.getType()) {
                case ServiceEvent.REGISTERED: type = "REGISTERED"; break;
                case ServiceEvent.UNREGISTERING: type = "UNREGISTERING"; break;
                case ServiceEvent.MODIFIED: type = "MODIFIED"; break;
                default: type = Integer.toString(event.getType());
            }
            return type + "(" + event.getServiceReference().getProperty(property) + ")";
        }).collect(Collectors.joining(", ", "[", "]"));
    }

}
