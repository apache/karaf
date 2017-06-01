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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the system scope commands as defined in
 * /framework/src/main/resources/resources/etc/org.apache.karaf.command.acl.system.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SystemCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;
          
    @Test
    public void testSystemCommandSecurityViaSsh() throws Exception {
        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);


        assertCommand(vieweruser, "system:name", Result.OK);
        assertCommand(vieweruser, "system:start-level", Result.OK);
        assertCommand(vieweruser, "system:start-level 150", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "system:property", Result.NOT_FOUND);
        assertCommand(vieweruser, "system:shutdown", Result.NOT_FOUND);

        assertCommand(manageruser, "system:name", Result.OK);
        assertCommand(manageruser, "system:start-level", Result.OK);
        assertCommand(manageruser, "system:start-level 0", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "system:start-level  1 ", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "system:start-level 99", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "system:start-level 105", Result.OK);
        assertCommand(manageruser, "system:property", Result.NOT_FOUND);
        assertCommand(manageruser, "system:shutdown", Result.NOT_FOUND);

        assertCommand("karaf", "system:name", Result.OK);
        assertCommand("karaf", "system:start-level", Result.OK);
        assertCommand("karaf", "system:start-level 99", Result.OK);
        Assert.assertTrue(assertCommand("karaf", "system:start-level", Result.OK).contains("99"));
        assertCommand("karaf", "system:start-level 100", Result.OK);
        assertCommand("karaf", "system:property vieweruser " + vieweruser, Result.OK);
        Assert.assertTrue(assertCommand("karaf", "system:property vieweruser", Result.OK).contains(vieweruser));
        assertCommand("karaf", "system:shutdown --help", Result.OK);
    }
}
