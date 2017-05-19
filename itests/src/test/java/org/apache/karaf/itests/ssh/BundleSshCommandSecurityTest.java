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
 * This test exercises the Shell Command ACL for the bundle scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.bundle.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class BundleSshCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;
    


    @Test
    public void testBundleCommandSecurityViaSsh() throws Exception {
        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        assertCommand(vieweruser, "bundle:refresh 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:refresh -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:refresh 999", Result.OK);
        assertCommand("karaf", "bundle:refresh -f 999", Result.OK);
        assertCommand("karaf", "bundle:refresh 999", Result.OK);

        assertCommand(vieweruser, "bundle:restart 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:restart -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:restart 999", Result.OK);
        assertCommand("karaf", "bundle:restart -f 999", Result.OK);
        assertCommand("karaf", "bundle:restart 999", Result.OK);

        assertCommand(vieweruser, "bundle:start 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:start -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:start 999", Result.OK);
        assertCommand("karaf", "bundle:start -f 999", Result.OK);
        assertCommand("karaf", "bundle:start 999", Result.OK);

        assertCommand(vieweruser, "bundle:stop 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:stop -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:stop 999", Result.OK);
        assertCommand("karaf", "bundle:stop -f 999", Result.OK);
        assertCommand("karaf", "bundle:stop 999", Result.OK);

        assertCommand(vieweruser, "bundle:uninstall 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:uninstall -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:uninstall 999", Result.OK);
        assertCommand("karaf", "bundle:uninstall -f 999", Result.OK);
        assertCommand("karaf", "bundle:uninstall 999", Result.OK);

        assertCommand(vieweruser, "bundle:update 999", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:update -f 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "bundle:update 999", Result.OK);
        assertCommand("karaf", "bundle:update -f 999", Result.OK);
        assertCommand("karaf", "bundle:update 999", Result.OK);

        assertCommand(vieweruser, "bundle:install xyz", Result.NOT_FOUND);
        assertCommand(manageruser, "bundle:install xyz", Result.NOT_FOUND);
        assertCommand("karaf", "bundle:install xyz", Result.OK);
    }
}
