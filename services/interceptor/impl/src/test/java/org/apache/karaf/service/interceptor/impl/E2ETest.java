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
package org.apache.karaf.service.interceptor.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.systemTimeout;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.container.remote.RBCRemoteTargetOptions.waitForRBCFor;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureSecurity;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

import java.io.File;
import java.net.MalformedURLException;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.karaf.service.interceptor.impl.test.InterceptedService;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeBuilder;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption;
import org.ops4j.pax.exam.options.UrlProvisionOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Constants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class E2ETest {
    @Inject
    private InterceptedService interceptedService;

    @Test
    @Ignore
    public void run() {
        assertTrue(interceptedService.getClass().getName().contains("$$KarafInterceptorProxy"));
        assertEquals("wrapped>from 'org.apache.karaf.service.interceptor.impl.test.InterceptedService'<", interceptedService.wrap());
        assertEquals("wrapped>'bar'(suffixed)<", interceptedService.wrapAndSuffix("bar"));
    }

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(final TestProbeBuilder probe) {
        probe.setHeader(Constants.EXPORT_PACKAGE, "org.apache.karaf.service.interceptor.impl.test");
        probe.setHeader("Service-Component",
                "OSGI-INF/org.apache.karaf.service.interceptor.impl.test.InterceptedService.xml," +
                "OSGI-INF/org.apache.karaf.service.interceptor.impl.test.SuffixingInterceptor.xml," +
                "OSGI-INF/org.apache.karaf.service.interceptor.impl.test.WrappingInterceptor.xml");
        return probe;
    }

    @Configuration
    public Option[] config() throws MalformedURLException {
        final String localRepository = System.getProperty("org.ops4j.pax.url.mvn.localRepository", "");
        final UrlReference karafUrl = url(new File("target/libs/karaf.tar.gz").toURI().toURL().toExternalForm());
        final UrlReference asmUrl = url(new File("target/libs/asm.jar").toURI().toURL().toExternalForm());
        final UrlProvisionOption apiBundle = url(Optional.ofNullable(new File("../api/target")
                .listFiles((dir, name) -> name.startsWith("org.apache.karaf.services.interceptor.api-") && isNotReleaseArtifact(name)))
            .map(files -> files[0])
            .orElseThrow(() -> new IllegalArgumentException("No interceptor api bundle found, ensure api module was built"))
            .toURI().toURL().toExternalForm());
        final UrlProvisionOption implBundle = url(Optional.ofNullable(new File("target")
                .listFiles((dir, name) -> name.startsWith("org.apache.karaf.services.interceptor.impl-") && isNotReleaseArtifact(name)))
            .map(files -> files[0])
            .orElseThrow(() -> new IllegalArgumentException("No interceptor impl bundle found, ensure impl module was built"))
            .toURI().toURL().toExternalForm());
        return new Option[]{
                karafDistributionConfiguration()
                        .frameworkUrl(karafUrl.getURL())
                        .name("Apache Karaf")
                        .runEmbedded(true)
                        .unpackDirectory(new File("target/exam")),
                configureSecurity().disableKarafMBeanServerBuilder(),
                configureConsole().ignoreLocalConsole(),
                keepRuntimeFolder(),
                logLevel(LogLevelOption.LogLevel.INFO),
                systemTimeout(3600000),
                waitForRBCFor(3600000),
                editConfigurationFilePut("etc/org.apache.karaf.features.cfg", "updateSnapshots", "none"),
                editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.localRepository", localRepository),
                editConfigurationFilePut("etc/branding.properties", "welcome", ""), // No welcome banner
                editConfigurationFilePut("etc/branding-ssh.properties", "welcome", ""),
                features("mvn:org.apache.karaf.features/standard/" + System.getProperty("karaf.version") + "/xml/features", "scr"),
                bundle(asmUrl.getURL()),
                bundle(apiBundle.getURL()),
                bundle(implBundle.getURL())
        };
    }

    private boolean isNotReleaseArtifact(final String name) {
        return name.endsWith(".jar") && !name.contains("-sources") && !name.contains("javadoc");
    }
}
