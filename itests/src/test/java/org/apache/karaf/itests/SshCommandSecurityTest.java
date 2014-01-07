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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.HashSet;

import junit.framework.Assert;

import org.apache.karaf.features.Feature;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SshCommandSecurityTest extends KarafTestSupport {
    private enum Result { OK, NOT_FOUND, NO_CREDENTIALS };

    private static int counter = 0;

    private HashSet<Feature> featuresBefore;
    private SshClient client;
    private ClientSession session;
    private ClientChannel channel;

    @Before
    public void installSshFeature() throws Exception {
        featuresBefore = new HashSet<Feature>(Arrays.asList(featureService.listInstalledFeatures()));
        installAndAssertFeature("ssh");
    }

    @After
    public void uninstallSshFeature() throws Exception {
        uninstallNewFeatures(featuresBefore);
    }

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

    private void testConfigEdits(String user, Result expectedEditResult, String pid, boolean isAdmin) throws Exception, IOException {
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
        Assert.assertTrue(result.contains("x = yz"));
        Assert.assertTrue(result.contains("a = b"));
        String result2 = assertCommand(user, "config:edit " + pid + "\n" +
                "config:property-delete a\n" +
                "config:property-list\n" +
                "config:update", Result.OK);
        Assert.assertTrue(result2.contains("x = yz"));
        Assert.assertFalse(result2.contains("a = b"));

        if (isAdmin) {
            assertCommand(user, "config:delete " + pid, Result.OK);
            String result3 = assertCommand(user, "config:edit " + pid + "\n" +
                    "config:property-list", Result.OK);
            Assert.assertFalse(result3.contains("x = yz"));
            Assert.assertFalse(result3.contains("a = b"));
        } else {
            assertCommand(user, "config:delete " + pid, Result.NOT_FOUND);
            String result3 = assertCommand(user, "config:edit " + pid + "\n" +
                    "config:property-list", Result.OK);
            Assert.assertTrue("The delete command should have had no effect", result3.contains("x = yz"));
            Assert.assertFalse(result3.contains("a = b"));
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

    private void testConfigEditsNoSession(String user, Result expectedResult, String pid) throws Exception, IOException {
        assertCommand(user, "config:property-set -p " + pid + " a.b.c d.e.f", expectedResult);
        assertCommand(user, "config:property-append -p " + pid + " a.b.c .g.h", expectedResult);

        if (expectedResult == Result.OK) {
            Assert.assertTrue(assertCommand(user, "config:property-list -p " + pid, Result.OK).contains("a.b.c = d.e.f.g.h"));
        }
        assertCommand(user, "config:property-delete -p " + pid + " a.b.c", expectedResult);
        if (expectedResult == Result.OK) {
            Assert.assertFalse(assertCommand(user, "config:property-list -p " + pid, Result.OK).contains("a.b.c"));
        }
    }

    private String assertCommand(String user, String command, Result result) throws Exception, IOException {
        if (!command.endsWith("\n"))
            command += "\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pipe = openSshChannel(user, user, out, out);
        pipe.write(command.getBytes());
        pipe.flush();

        closeSshChannel(pipe);
        String output = new String(out.toByteArray());

        switch(result) {
        case OK:
            Assert.assertFalse("Should not contain 'Insufficient credentials' or 'Command not found': " + output,
                    output.contains("Insufficient credentials") || output.contains("Command not found"));
            break;
        case NOT_FOUND:
            Assert.assertTrue("Should contain 'Command not found': " + output,
                    output.contains("Command not found"));
            break;
        case NO_CREDENTIALS:
            Assert.assertTrue("Should contain 'Insufficient credentials': " + output,
                    output.contains("Insufficient credentials"));
            break;
        default:
            Assert.fail("Unexpected enum value: " + result);
        }
        return output;
    }

    private void addUsers(String manageruser, String vieweruser) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pipe = openSshChannel("karaf", "karaf", out);
        pipe.write(("jaas:realm-manage --realm=karaf"
                + ";jaas:user-add " + manageruser + " " + manageruser
                + ";jaas:role-add " + manageruser + " manager"
                + ";jaas:role-add " + manageruser + " viewer"
                + ";jaas:user-add " + vieweruser + " " + vieweruser
                + ";jaas:role-add " + vieweruser + " viewer"
                + ";jaas:update;jaas:realm-manage --realm=karaf;jaas:user-list\n").getBytes());
        pipe.flush();
        closeSshChannel(pipe);
        System.out.println(new String(out.toByteArray()));
    }

    private OutputStream openSshChannel(String username, String password, OutputStream ... outputs) throws Exception {
        client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture future = client.connect("localhost", 8101).await();
        session = future.getSession();

        int ret = ClientSession.WAIT_AUTH;
        while ((ret & ClientSession.WAIT_AUTH) != 0) {
            session.authPassword(username, password);
            ret = session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
        }
        if ((ret & ClientSession.CLOSED) != 0) {
            throw new Exception("Could not open SSH channel");
        }
        channel = session.createChannel("shell");
        PipedOutputStream pipe = new PipedOutputStream();
        channel.setIn(new PipedInputStream(pipe));

        OutputStream out;
        if (outputs.length >= 1) {
            out = outputs[0];
        } else {
            out = new ByteArrayOutputStream();
        }
        channel.setOut(out);

        OutputStream err;
        if (outputs.length >= 2) {
            err = outputs[1];
        } else {
            err = new ByteArrayOutputStream();
        }
        channel.setErr(err);
        channel.open();

        return pipe;
    }

    private void closeSshChannel(OutputStream pipe) throws IOException {
        pipe.write("logout\n".getBytes());
        pipe.flush();

        channel.waitFor(ClientChannel.CLOSED, 0);
        session.close(true);
        client.stop();

        client = null;
        channel = null;
        session = null;
    }
}
