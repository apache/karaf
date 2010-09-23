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

import java.io.InputStream;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.karaf.testing.AbstractIntegrationTest;
import org.apache.karaf.testing.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Customizer;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Constants;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;

import static org.apache.karaf.testing.Helper.felixProvisionalApis;
import static org.junit.Assert.assertNotNull;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;

import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;
import static org.ops4j.pax.swissbox.tinybundles.core.TinyBundles.modifyBundle;

@RunWith(JUnit4TestRunner.class)
public class FeaturesTest extends AbstractIntegrationTest {

    @Test
    public void testFeatures() throws Exception {
        // Make sure the command services are available
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.obr", 20000));
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.karaf.shell.wrapper", 20000));
        // Run some commands to make sure they are installed properly
        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(System.in, System.out, System.err);
        cs.execute("obr:listUrl");
        cs.execute("wrapper:install --help");
        cs.close();
    }

    @Configuration
    public static Option[] configuration() throws Exception{
        return combine(
            // Default karaf environment
            Helper.getDefaultOptions(
                // this is how you set the default log level when using pax logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG")),

            // add two features
            scanFeatures(
                    maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("xml").classifier("features").versionAsInProject(),
                    "obr", "wrapper"
            ),

            workingDirectory("target/paxrunner/features/"),

            waitForFrameworkStartup(),
            
            // Test on both equinox and felix
            equinox(), felix(),

            felixProvisionalApis()
        );
    }

}
