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
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;
import static org.osgi.framework.Constants.OBJECTCLASS;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.function.Function;

import javax.inject.Inject;

import org.apache.karaf.itests.KarafTestSupport;
import org.apache.karaf.itests.monitoring.Activator;
import org.apache.karaf.itests.monitoring.ServiceMonitor;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
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

        return new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                mavenBundle().groupId("biz.aQute.bnd").artifactId("biz.aQute.bndlib").version("3.5.0"),
                mavenBundle().groupId("org.ops4j.pax.tinybundles").artifactId("tinybundles").versionAsInProject(),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/startup.properties", "file:../../" + new File(url.toURI()).getName(), "1"),
                composite(editConfigurationFilePut("etc/org.apache.karaf.features.cfg", new File("target/test-classes/etc/org.apache.karaf.features.cfg")))
        };
    }

    private InputStream createMonitorBundle() {
        return bundle()
                .set(Constants.BUNDLE_ACTIVATOR, Activator.class.getName())
                .set(Constants.EXPORT_PACKAGE, ServiceMonitor.class.getPackage().getName())
                .add(Activator.class)
                .add(ServiceMonitor.class)
                .build(withBnd());
    }

    protected long numberOfServiceEventsFor(String serviceName) {
        Function<ServiceEvent, String> getObjectClass = event -> ((String[])event.getServiceReference().getProperty(OBJECTCLASS))[0];
        return serviceMonitor.getEvents().stream().map(getObjectClass).filter(v -> v.equals(serviceName)).count();
    }

}
