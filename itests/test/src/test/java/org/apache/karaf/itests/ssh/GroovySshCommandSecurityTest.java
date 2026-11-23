/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.itests.ssh;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the groovy scope commands as defined in the
 * {@code org.apache.karaf.command.acl.groovy} configuration provided by the {@code shell} feature
 * when the {@code groovy} feature is installed.
 *
 * <p>{@code groovy:exec} and {@code groovy:exec-file} evaluate arbitrary Groovy code in the Karaf
 * JVM, so they are restricted to the {@code admin} role, like {@code shell:exec} and
 * {@code shell:java}.</p>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class GroovySshCommandSecurityTest extends SshCommandTestBase {

    private static int counter = 0;

    @Test
    public void testGroovyCommandSecurityViaSsh() throws Exception {
        // Skip on Windows where PTY output can be garbled,
        // when upgrading to Junit5, this can be replaced with @DisabledOnOs(OS.WINDOWS)
        Assume.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"));

        installAndAssertFeature("groovy");

        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // the admin user can see and run the groovy commands (a bare invocation reports a
        // missing-argument error, not "Command not found")
        assertCommand("karaf", "groovy:exec", Result.OK);
        assertCommand("karaf", "groovy:exec-file", Result.OK);

        // exec / exec-file are admin-only: a viewer and a manager must not even see the commands
        assertCommand(vieweruser, "groovy:exec", Result.NOT_FOUND);
        assertCommand(vieweruser, "groovy:exec-file", Result.NOT_FOUND);

        assertCommand(manageruser, "groovy:exec", Result.NOT_FOUND);
        assertCommand(manageruser, "groovy:exec-file", Result.NOT_FOUND);
    }
}
