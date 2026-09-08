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
 * This test exercises the Shell Command ACL for the jdbc scope commands as defined in the
 * {@code org.apache.karaf.command.acl.jdbc} configuration provided by the {@code jdbc} feature.
 *
 * <p>{@code jdbc:ds-create} stores an arbitrary JDBC URL that pax-jdbc-config turns into a live
 * datasource - several JDBC drivers run code at connection time based on URL parameters (for
 * instance H2 {@code INIT=RUNSCRIPT}) - so it, together with {@code jdbc:ds-delete},
 * {@code jdbc:execute} and {@code jdbc:query} (arbitrary SQL), is restricted to the
 * {@code admin} role. The read-only commands require the {@code viewer} role.</p>
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class JdbcSshCommandSecurityTest extends SshCommandTestBase {

    private static int counter = 0;

    @Test
    public void testJdbcCommandSecurityViaSsh() throws Exception {
        // Skip on Windows where PTY output can be garbled,
        // when upgrading to Junit5, this can be replaced with @DisabledOnOs(OS.WINDOWS)
        Assume.assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"));

        featureService.installFeature("jdbc", EnumSet.of(FeaturesService.Option.NoAutoRefreshBundles));

        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // read-only commands are available to a viewer
        assertCommand(vieweruser, "jdbc:ds-list", Result.OK);
        assertCommand(vieweruser, "jdbc:ds-factories", Result.OK);

        // ds-create / ds-delete / execute / query are admin-only: a viewer and a manager
        // must not even see the commands
        assertCommand(vieweruser, "jdbc:ds-create", Result.NOT_FOUND);
        assertCommand(vieweruser, "jdbc:ds-delete", Result.NOT_FOUND);
        assertCommand(vieweruser, "jdbc:execute", Result.NOT_FOUND);
        assertCommand(vieweruser, "jdbc:query", Result.NOT_FOUND);

        assertCommand(manageruser, "jdbc:ds-create", Result.NOT_FOUND);
        assertCommand(manageruser, "jdbc:execute", Result.NOT_FOUND);
        assertCommand(manageruser, "jdbc:query", Result.NOT_FOUND);

        // the admin user can see and run the jdbc commands (a bare invocation reports a
        // missing-argument / missing-driver error, not "Command not found")
        assertCommand("karaf", "jdbc:ds-list", Result.OK);
        assertCommand("karaf", "jdbc:ds-factories", Result.OK);
        assertCommand("karaf", "jdbc:ds-create", Result.OK);
        assertCommand("karaf", "jdbc:execute", Result.OK);
        assertCommand("karaf", "jdbc:query", Result.OK);
        assertCommand("karaf", "jdbc:ds-delete", Result.OK);
    }
}
