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

import java.util.EnumSet;

import org.apache.karaf.features.FeaturesService;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the jms scope commands as defined in the
 * {@code org.apache.karaf.command.acl.jms} configuration provided by the {@code jms} feature.
 *
 * <p>{@code jms:create} stores an arbitrary broker URL that pax-jms-config turns into a live
 * connection factory, and {@code jms:send}, {@code jms:consume} and {@code jms:move} write to
 * or destructively read from broker destinations, so these commands together with
 * {@code jms:delete} are restricted to the {@code admin} role. The read-only commands require
 * the {@code viewer} role.</p>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JmsSshCommandSecurityTest extends SshCommandTestBase {

    private static int counter = 0;

    @Test
    public void testJmsCommandSecurityViaSsh() throws Exception {
        // Skip on Windows where PTY output can be garbled,
        // when upgrading to Junit5, this can be replaced with @DisabledOnOs(OS.WINDOWS)
        Assume.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"));

        featureService.installFeature("jms", EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));

        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // read-only commands are available to a viewer
        assertCommand(vieweruser, "jms:connectionfactories", Result.OK);

        // create / delete / send / consume / move are admin-only: a viewer and a manager
        // must not even see the commands
        assertCommand(vieweruser, "jms:create", Result.NOT_FOUND);
        assertCommand(vieweruser, "jms:delete", Result.NOT_FOUND);
        assertCommand(vieweruser, "jms:send", Result.NOT_FOUND);
        assertCommand(vieweruser, "jms:consume", Result.NOT_FOUND);
        assertCommand(vieweruser, "jms:move", Result.NOT_FOUND);

        assertCommand(manageruser, "jms:create", Result.NOT_FOUND);
        assertCommand(manageruser, "jms:send", Result.NOT_FOUND);
        assertCommand(manageruser, "jms:consume", Result.NOT_FOUND);
        assertCommand(manageruser, "jms:move", Result.NOT_FOUND);

        // the admin user can see and run the jms commands (a bare invocation reports a
        // missing-argument error, not "Command not found")
        assertCommand("karaf", "jms:connectionfactories", Result.OK);
        assertCommand("karaf", "jms:create", Result.OK);
        assertCommand("karaf", "jms:delete", Result.OK);
        assertCommand("karaf", "jms:send", Result.OK);
        assertCommand("karaf", "jms:consume", Result.OK);
        assertCommand("karaf", "jms:move", Result.OK);
    }
}
