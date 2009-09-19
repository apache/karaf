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
package org.apache.felix.karaf.shell.itests;

import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.scanFeatures;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.service.blueprint.container.BlueprintContainer;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;

@RunWith(JUnit4TestRunner.class)
public class FeaturesTest extends AbstractIntegrationTest {

    @Test
    public void testFeatures() throws Exception {
        // Make sure the command services are available
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.felix.karaf.shell.obr", 20000));
        assertNotNull(getOsgiService(BlueprintContainer.class, "osgi.blueprint.container.symbolicname=org.apache.felix.karaf.shell.wrapper", 20000));
        // Run some commands to make sure they are installed properly
        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(System.in, System.out, System.err);
        cs.execute("obr:listUrl");
        cs.execute("wrapper:install --help");
        cs.close();
    }

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG"),
            systemProperty("karaf.name").value("root"),
            systemProperty("karaf.home").value("target/karaf.home"),
            systemProperty("karaf.base").value("target/karaf.home"),
            systemProperty("karaf.startLocalConsole").value("false"),
            systemProperty("karaf.startRemoteShell").value("false"),

            // hack system packages
            systemPackages("org.apache.felix.karaf.main.spi;version=1.0.0", "org.apache.felix.karaf.jaas.boot;version=0.9.0"),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.boot")).afterFramework(),
            bootClasspathLibrary(mavenBundle("org.apache.felix.karaf", "org.apache.felix.karaf.main")).afterFramework(),

            // Log
            mavenBundle("org.ops4j.pax.logging", "pax-logging-api"),
            mavenBundle("org.ops4j.pax.logging", "pax-logging-service"),
            // Felix Config Admin
            mavenBundle("org.apache.felix", "org.apache.felix.configadmin"),
            // Felix Preferences Service
            mavenBundle("org.apache.felix", "org.apache.felix.prefs"),
            // Blueprint
            mavenBundle("org.apache.geronimo.blueprint", "geronimo-blueprint"),

            // Bundles
            mavenBundle("org.apache.mina", "mina-core"),
            mavenBundle("org.apache.sshd", "sshd-core"),
            mavenBundle("org.apache.felix.karaf.jaas", "org.apache.felix.karaf.jaas.config"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.console"),
            mavenBundle("org.apache.felix.gogo", "org.apache.felix.gogo.runtime"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.osgi"),
            mavenBundle("org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.log").noStart(),

            scanFeatures(
                    maven().groupId("org.apache.felix.karaf").artifactId("apache-felix-karaf").type("xml").classifier("features").versionAsInProject(),
                    "obr", "wrapper"
            ),

            equinox()
        );
        return options;
    }

}
