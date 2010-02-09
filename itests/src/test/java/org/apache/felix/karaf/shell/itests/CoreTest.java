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

import org.apache.felix.karaf.testing.AbstractIntegrationTest;
import org.apache.felix.karaf.testing.Helper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.JUnit4TestRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.command.CommandProcessor;
import org.osgi.service.command.CommandSession;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.equinox;
import static org.ops4j.pax.exam.CoreOptions.felix;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.waitForFrameworkStartup;
import static org.ops4j.pax.exam.OptionUtils.combine;

import static org.ops4j.pax.exam.container.def.PaxRunnerOptions.workingDirectory;

@RunWith(JUnit4TestRunner.class)
public class CoreTest extends AbstractIntegrationTest {

    @Test
    public void testHelp() throws Exception {
        Thread.sleep(10000);

        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(System.in, System.out, System.err);
        cs.execute("osgi:list --help");
        cs.close();
    }

    @Test
    public void testInstallCommand() throws Exception {
        Thread.sleep(12000);

        CommandProcessor cp = getOsgiService(CommandProcessor.class);
        CommandSession cs = cp.createSession(System.in, System.out, System.err);

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        Bundle b = getInstalledBundle("org.apache.felix.karaf.shell.log");
        b.start();

        Thread.sleep(1000);

        cs.execute("log:display");

        b.stop();

        Thread.sleep(1000);

        try {
            cs.execute("log:display");
            fail("command should not exist");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().indexOf("Command not found") >= 0);
        }

        cs.close();
    }

//    @Test
//    public void testCommandGroup() throws Exception {
//        Thread.sleep(5000);
//
//        Shell shell = getOsgiService(Shell.class);
//        shell.execute("osgi");
//        shell.execute("help");
//        shell.execute("..");
//    }
//
//    @Test
//    public void testCommandGroupAfterInstall() throws Exception {
//        Bundle b = getInstalledBundle("org.apache.felix.karaf.shell.log");
//        b.start();
//
//        Thread.sleep(5000);
//
//        Shell shell = getOsgiService(Shell.class);
//        shell.execute("log");
//        shell.execute("help");
//        shell.execute("..");
//    }
//
    @Configuration
    public static Option[] configuration() throws Exception {
        Option[] options = combine(
            // Default karaf environment
            Helper.getDefaultOptions(
                // this is how you set the default log level when using pax logging (logProfile)
                systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("DEBUG")),

            workingDirectory("target/paxrunner/core/"),

            waitForFrameworkStartup(),

            // Test on both equinox and felix
            equinox(), felix()
        );
        // Stop the shell log bundle 
        Helper.findMaven(options, "org.apache.felix.karaf.shell", "org.apache.felix.karaf.shell.log").noStart();
        return options;
    }

}
