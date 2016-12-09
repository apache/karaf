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
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.inject.Inject;

import org.apache.karaf.itests.monitoring.Activator;
import org.apache.karaf.itests.monitoring.RegisteredService;
import org.apache.karaf.itests.monitoring.ServiceMonitor;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.TimeoutOption;
import org.ops4j.store.Handle;
import org.ops4j.store.Store;
import org.ops4j.store.intern.TemporaryStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.karaf.itests.KarafTestSupport.*;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.*;
import static org.ops4j.pax.tinybundles.core.TinyBundles.bundle;

public abstract class KarafMinimalMonitoredTestSupport {

    public static Logger LOG = LoggerFactory.getLogger(KarafMinimalMonitoredTestSupport.class);

    @Inject
    protected BundleContext bundleContext;

    // don't extend, because we don't want @Rule Retry
    private static KarafTestSupport karafTestSupport = new KarafTestSupport();

    public Option[] baseConfig() throws Exception {
        MavenArtifactUrlReference karafUrl = maven()
                .groupId("org.apache.karaf").artifactId("apache-karaf-minimal")
                .versionAsInProject().type("tar.gz");

        InputStream monitorBundle = bundle()
                .set(Constants.BUNDLE_SYMBOLICNAME, "monitor")
                .set(Constants.BUNDLE_ACTIVATOR, Activator.class.getName())
                .set(Constants.IMPORT_PACKAGE, "org.osgi.framework")
                .set(Constants.EXPORT_PACKAGE, Activator.class.getPackage().getName())
                .set(Constants.BUNDLE_VERSION, "1.0.0")
                .set(Constants.BUNDLE_MANIFESTVERSION, "2")
                .add(Activator.class)
                .add(RegisteredService.class)
                .add(ServiceMonitor.class)
                .build();

        String rmiRegistryPort = Integer.toString(karafTestSupport.getAvailablePort(Integer.parseInt(MIN_RMI_REG_PORT), Integer.parseInt(MAX_RMI_REG_PORT)));
        String rmiServerPort = Integer.toString(karafTestSupport.getAvailablePort(Integer.parseInt(MIN_RMI_SERVER_PORT), Integer.parseInt(MAX_RMI_SERVER_PORT)));

        Store<InputStream> store = new TemporaryStore(new File("target/exam"), false);

        Handle handle = store.store(monitorBundle);
        URL url = store.getLocation(handle).toURL();

        Option[] baseOptions = new Option[] {
                karafDistributionConfiguration().frameworkUrl(karafUrl).name("Apache Karaf").unpackDirectory(new File("target/exam")),
                // enable JMX RBAC security, thanks to the KarafMBeanServerBuilder
                configureSecurity().disableKarafMBeanServerBuilder(),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
                editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
                editConfigurationFilePut("etc/startup.properties", "file:../../" + new File(url.toURI()).getName(), "1"),
//                new TimeoutOption(3600000),
//                KarafDistributionOption.debugConfiguration("8889", true),
        };
        List<Option> options = new LinkedList<>(Arrays.asList(baseOptions));

        options.addAll(Arrays.asList(
                editConfigurationFilePut("etc/org.apache.karaf.features.cfg", new File("target/test-classes/etc/org.apache.karaf.features.cfg"))
        ));

        return options.toArray(new Option[options.size()]);
    }

}
