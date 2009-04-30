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
import org.ops4j.pax.exam.Option;
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

        b.stop();

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
    
    @Test
    public void testCommandGroupAfterInstall() throws Exception {
        Bundle b = getInstalledBundle("org.apache.servicemix.kernel.gshell.log");
        b.start();
        Shell shell = getOsgiService(Shell.class);
        shell.execute("log");
        shell.execute("help");
        shell.execute("..");
    }

    @Configuration
    public static Option[] configuration() {
        return AbstractIntegrationTest.configuration();
    }

}
