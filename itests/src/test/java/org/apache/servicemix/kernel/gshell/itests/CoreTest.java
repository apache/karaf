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
package org.apache.servicemix.kernel.gshell.itests;

import org.apache.geronimo.gshell.commandline.CommandLineExecutionFailed;
import org.apache.geronimo.gshell.registry.NoSuchCommandException;
import org.apache.geronimo.gshell.shell.Shell;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.mavenConfiguration;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackages;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.bootClasspathLibrary;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.OptionUtils;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.configProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.logProfile;
import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.profile;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;

@RunWith(JUnit4TestRunner.class)
public class CoreTest extends AbstractIntegrationTest{

    @Test
    public void testHelp() throws Exception {
        Shell shell = getOsgiService(Shell.class);
        shell.execute("help");
    }

    @Test
    public void testInstallCommand() throws Exception {
        Shell shell = getOsgiService(Shell.class);

        try {
            shell.execute("log/display");
            fail("command should not exist");
        } catch (CommandLineExecutionFailed e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof NoSuchCommandException);
        }

        Bundle b = getInstalledBundle("org.apache.servicemix.kernel.gshell.log");
        b.start();

        shell.execute("log/display");

        b.uninstall();

        try {
            shell.execute("log/display");
            fail("command should not exist");
        } catch (CommandLineExecutionFailed e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof NoSuchCommandException);
        }
    }

    @Test
    public void testCommandGroup() throws Exception {
        Shell shell = getOsgiService(Shell.class);
        shell.execute("osgi");
        shell.execute("help");
        shell.execute("..");
    }
    
//    @Test
//    public void testInstallFeature() throws Exception {
//        Shell shell = getOsgiService(Shell.class);
//
//        try {
//            shell.execute("obr");
//            fail("command should not exist");
//        } catch (CommandLineExecutionFailed e) {
//            assertNotNull(e.getCause());
//            assertTrue(e.getCause() instanceof NoSuchCommandException);
//        }
//        try {
//            shell.execute("wrapper");
//            fail("command should not exist");
//        } catch (CommandLineExecutionFailed e) {
//            assertNotNull(e.getCause());
//            assertTrue(e.getCause() instanceof NoSuchCommandException);
//        }
//        String url = getClass().getClassLoader().getResource("features.xml").toString();
//        addFeatureRepo(url);
//        installFeature("obr");
//        installFeature("wrapper");
//        shell.execute("obr");
//        shell.execute("wrapper");
//    }

    /**
     * TODO: This test seems to fail, there must be a timing issue somewhere
     *
    public void testCommandGroupAfterInstall() throws Exception {
        Bundle b = installBundle("org.apache.servicemix.kernel.gshell", "org.apache.servicemix.kernel.gshell.log", null, "jar");
        Shell shell = getOsgiService(Shell.class);
        shell.execute("log");
        shell.execute("help");
        shell.execute("..");
    }
     */

    @Configuration
    public static Option[] configuration() {
        Option[] options = options(
            // install log service using pax runners profile abstraction (there are more profiles, like DS)
            logProfile(),
            configProfile(),
            profile("spring.dm").version("1.2.0-rc1"),

            // this is how you set the default log level when using pax logging (logProfile)
            systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("INFO"),
            systemProperty("servicemix.name").value("root"),
            systemProperty("servicemix.base").value("target/smx.base"),
            systemProperty("servicemix.startLocalConsole").value("false"),
            systemProperty("servicemix.startRemoteShell").value("false"),

            // hack system packages
            systemPackages("org.apache.servicemix.kernel.main.spi;version=1.0.0", "org.apache.servicemix.kernel.jaas.boot"),
            bootClasspathLibrary(mavenBundle("org.apache.servicemix.kernel.jaas", "org.apache.servicemix.kernel.jaas.boot")).afterFramework(),
            bootClasspathLibrary(mavenBundle("org.apache.servicemix.kernel", "org.apache.servicemix.kernel.main")).afterFramework(),

            // Bundles
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jline"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-httpclient"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-jexl"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.commons-vfs"),
            mavenBundle("org.apache.mina", "mina-core"),
            mavenBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.oro"),
            mavenBundle("org.apache.servicemix.kernel.jaas", "org.apache.servicemix.kernel.jaas.config"),
            mavenBundle("org.apache.sshd", "sshd-core"),
            mavenBundle("org.apache.servicemix.kernel.gshell", "org.apache.servicemix.kernel.gshell.core"),
            mavenBundle("org.apache.servicemix.kernel.gshell", "org.apache.servicemix.kernel.gshell.osgi"),
            mavenBundle("org.apache.servicemix.kernel.gshell", "org.apache.servicemix.kernel.gshell.log").noStart(),

            felix()
        );

        // use config generated by the Maven plugin (until PAXEXAM-62/64 get resolved)
        if (CoreTest.class.getClassLoader().getResource("META-INF/maven/paxexam-config.args") != null) {
            options = OptionUtils.combine(options, mavenConfiguration());
        }

        return options;
    }

}
