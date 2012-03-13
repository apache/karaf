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
package org.apache.karaf.shell.itests;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.editConfigurationFileExtend;
import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.maven;

import javax.inject.Inject;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.tooling.exam.options.configs.FeaturesCfg;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.ops4j.pax.exam.junit.ProbeBuilder;
import org.ops4j.pax.exam.util.Filter;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;

@RunWith(JUnit4TestRunner.class)
public class FeaturesTest {

    @Inject
    @Filter(value = "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.obr", timeout = 20000)
    private BlueprintContainer obrService;

    @Inject
    @Filter(value = "osgi.blueprint.container.symbolicname=org.apache.karaf.wrapper.core", timeout = 20000)
    private BlueprintContainer wrapperService;

    @Inject
    @Filter(value = "osgi.blueprint.container.symbolicname=org.apache.karaf.wrapper.commands", timeout = 20000)
    private BlueprintContainer wrapperCommandsService;

    @Inject
    private CommandProcessor cp;

    @ProbeBuilder
    public TestProbeBuilder probeConfiguration(TestProbeBuilder probe) {
        probe.setHeader(Constants.DYNAMICIMPORT_PACKAGE, "*,org.apache.felix.service.*;status=provisional");
        return probe;
    }

    @Configuration
    public Option[] config() {
        return new Option[]{
            karafDistributionConfiguration().frameworkUrl(
                maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip")
                    .versionAsInProject()), editConfigurationFileExtend(FeaturesCfg.BOOT, ",obr,wrapper") };
    }

    @Test
    public void testFeatures() throws Exception {
        // Make sure the command services are available
        assertNotNull(obrService);
        assertNotNull(wrapperService);
        // Run some commands to make sure they are installed properly
        CommandSession cs = cp.createSession(System.in, System.out, System.err);
        cs.execute("obr:url-list");
        cs.execute("wrapper:install --help");
        cs.close();
    }

}
