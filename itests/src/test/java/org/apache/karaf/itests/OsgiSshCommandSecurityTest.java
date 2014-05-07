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
package org.apache.karaf.itests;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

/**
 * This test exercises the Shell Command ACL for the osgi scope commands as defined in
 * apache-karaf/src/main/distribution/text/etc/org.apache.karaf.command.acl.osgi.cfg
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OsgiSshCommandSecurityTest extends SshCommandTestBase {
    private static int counter = 0;

    @Test
    public void testOsgiCommandSecurityViaSsh() throws Exception {
        String manageruser = "man" + System.nanoTime() + "_" + counter++;
        String vieweruser = "view" + System.nanoTime() + "_" + counter++;

        addUsers(manageruser, vieweruser);

        assertCommand(vieweruser, "osgi:refresh 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:refresh --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:refresh 999", Result.OK);
        assertCommand("karaf", "osgi:refresh --force 999", Result.OK);
        assertCommand("karaf", "osgi:refresh 999", Result.OK);

        assertCommand(vieweruser, "osgi:restart 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:restart --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:restart 999", Result.OK);
        assertCommand("karaf", "osgi:restart --force 999", Result.OK);
        assertCommand("karaf", "osgi:restart 999", Result.OK);

        assertCommand(vieweruser, "osgi:start 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:start --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:start 999", Result.OK);
        assertCommand("karaf", "osgi:start --force 999", Result.OK);
        assertCommand("karaf", "osgi:start 999", Result.OK);

        assertCommand(vieweruser, "osgi:stop 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:stop --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:stop 999", Result.OK);
        assertCommand("karaf", "osgi:stop --force 999", Result.OK);
        assertCommand("karaf", "osgi:stop 999", Result.OK);

        assertCommand(vieweruser, "osgi:uninstall 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:uninstall --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:uninstall 999", Result.OK);
        assertCommand("karaf", "osgi:uninstall --force 999", Result.OK);
        assertCommand("karaf", "osgi:uninstall 999", Result.OK);

        assertCommand(vieweruser, "osgi:update 999", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:update --force 999", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:update 999", Result.OK);
        assertCommand("karaf", "osgi:update --force 999", Result.OK);
        assertCommand("karaf", "osgi:update 999", Result.OK);

        assertCommand(vieweruser, "osgi:install xyz", Result.NOT_FOUND);
        assertCommand(manageruser, "osgi:install xyz", Result.NOT_FOUND);
        assertCommand("karaf", "osgi:install xyz", Result.OK);
        
        assertCommand(vieweruser, "osgi:name", Result.OK);
        assertCommand(vieweruser, "osgi:start-level", Result.OK);
        assertCommand(vieweruser, "osgi:start-level 150", Result.NO_CREDENTIALS);
        assertCommand(vieweruser, "osgi:shutdown", Result.NOT_FOUND);

        assertCommand(manageruser, "osgi:name", Result.OK);
        assertCommand(manageruser, "osgi:start-level", Result.OK);
        assertCommand(manageruser, "osgi:start-level 0", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:start-level  1 ", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:start-level 99", Result.NO_CREDENTIALS);
        assertCommand(manageruser, "osgi:start-level 105", Result.OK);
        assertCommand(manageruser, "osgi:shutdown", Result.NOT_FOUND);

        assertCommand("karaf", "osgi:name", Result.OK);
        assertCommand("karaf", "osgi:start-level", Result.OK);
        assertCommand("karaf", "osgi:start-level 99", Result.OK);
        Assert.assertTrue(assertCommand("karaf", "osgi:start-level", Result.OK).contains("99"));
        assertCommand("karaf", "osgi:start-level 100", Result.OK);
        assertCommand("karaf", "osgi:shutdown --help", Result.OK);

    }
}
