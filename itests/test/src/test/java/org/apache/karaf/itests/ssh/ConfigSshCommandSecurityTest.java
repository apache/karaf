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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the config scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.config.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ConfigSshCommandSecurityTest extends SshCommandTestBase {

    private static int counter = 0;
    
    

    @Test
    public void testConfigCommandSecurityViaSsh() throws Exception {
        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // A viewer cannot do anything to ConfigAdmin
        assertCommand(vieweruser, "config:edit cfg." + vieweruser, Result.NOT_FOUND);
        assertCommand(vieweruser, "config:delete cfg." + vieweruser, Result.NOT_FOUND);

        testConfigEdits(manageruser, Result.OK, "cfg." + manageruser, false);
        testConfigEdits(manageruser, Result.NO_CREDENTIALS, "jmx.acl.test_" + counter++, false);
        testConfigEdits(manageruser, Result.NO_CREDENTIALS, "org.apache.karaf.command.acl.test_" + counter++, false);
        testConfigEdits(manageruser, Result.NO_CREDENTIALS, "org.apache.karaf.service.acl.test_" + counter++, false);
        testConfigEdits("karaf", Result.OK, "cfg.karaf_" + counter++, true);
        testConfigEdits("karaf", Result.OK, "jmx.acl.test_" + counter++, true);
        testConfigEdits("karaf", Result.OK, "org.apache.karaf.command.acl.test_" + counter++, true);
        testConfigEdits("karaf", Result.OK, "org.apache.karaf.service.acl.test_" + counter++, true);
    }

    private void testConfigEdits(String user, Result expectedEditResult, String pid, boolean isAdmin) throws Exception {
        assertCommand(user, "config:edit " + pid + "\n" +
                "config:property-set x y\n" +
                "config:property-set a b\n" +
                "config:property-append x z\n" +
                "config:update", expectedEditResult);
        if (expectedEditResult != Result.OK)
            // If we're expecting failure, don't continue any further...
            return;

        String result = assertCommand(user, "config:edit " + pid + "\n" +
                "config:property-list\n" +
                "config:cancel", Result.OK);
        assertContains("a = b", result);
        String result2 = assertCommand(user, "config:edit " + pid + "\n" +
                "config:property-delete a\n" +
                "config:property-list\n" +
                "config:update", Result.OK);
        assertContains("x = yz", result2);
        assertContainsNot("a = b", result2);

        if (isAdmin) {
            assertCommand(user, "config:delete " + pid, Result.OK);
            String result3 = assertCommand(user, "config:edit " + pid + "\n" +
                    "config:property-list", Result.OK);
            assertContainsNot("x = yz", result3);
            assertContainsNot("a = b", result3);
        } else {
            assertCommand(user, "config:delete " + pid, Result.NOT_FOUND);
            String result3 = assertCommand(user, "config:edit " + pid + "\n" +
                    "config:property-list", Result.OK);
            assertContains("x = yz", result3);
            assertContainsNot("a = b", result3);
        }
    }

    @Test
    public void testConfigCommandSecurityWithoutEditSessionViaSsh() throws Exception {
        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        // Test the viewer user. Since the viewer cannot modify anything wrt Config Admin
        // the commands should not even be found...
        testConfigEditsNoSession(vieweruser, Result.NOT_FOUND, "cfg." + vieweruser);
        testConfigEditsNoSession(vieweruser, Result.NOT_FOUND, "jmx.acl.test_" + counter++);
        testConfigEditsNoSession(vieweruser, Result.NOT_FOUND, "org.apache.karaf.command.acl.test_" + counter++);
        testConfigEditsNoSession(vieweruser, Result.NOT_FOUND, "org.apache.karaf.service.acl.test_" + counter++);

        // Test the manager user. The manager can modify some properties, but not the ones associated with security
        // Therefore the config: commands will be found, but in some cases the manager is denied access
        testConfigEditsNoSession(manageruser, Result.OK, "cfg." + manageruser);
        testConfigEditsNoSession(manageruser, Result.NO_CREDENTIALS, "jmx.acl.test_" + counter++);
        testConfigEditsNoSession(manageruser, Result.NO_CREDENTIALS, "org.apache.karaf.command.acl.test_" + counter++);
        testConfigEditsNoSession(manageruser, Result.NO_CREDENTIALS, "org.apache.karaf.service.acl.test_" + counter++);

        // The admin user can modify everything.
        testConfigEditsNoSession("karaf", Result.OK, "cfg.karaf.test_" + counter++);
        testConfigEditsNoSession("karaf", Result.OK, "jmx.acl.test_" + counter++);
        testConfigEditsNoSession("karaf", Result.OK, "org.apache.karaf.command.acl.test_" + counter++);
        testConfigEditsNoSession("karaf", Result.OK, "org.apache.karaf.service.acl.test_" + counter++);
    }

    private void testConfigEditsNoSession(String user, Result expectedResult, String pid) throws Exception {
        assertCommand(user, "config:property-set -p " + pid + " a.b.c d.e.f", expectedResult);
        assertCommand(user, "config:property-append -p " + pid + " a.b.c .g.h", expectedResult);

        if (expectedResult == Result.OK) {
            assertContains("a.b.c = d.e.f.g.h", assertCommand(user, "config:property-list -p " + pid, Result.OK));
        }
        assertCommand(user, "config:property-delete -p " + pid + " a.b.c", expectedResult);
        if (expectedResult == Result.OK) {
            assertContainsNot("a.b.c", assertCommand(user, "config:property-list -p " + pid, Result.OK));
        }
    }
}
