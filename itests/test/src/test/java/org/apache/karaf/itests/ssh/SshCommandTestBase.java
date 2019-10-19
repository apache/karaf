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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.EnumSet;
import java.util.Set;

import org.apache.karaf.itests.BaseTest;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.client.session.ClientSession.ClientSessionEvent;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class SshCommandTestBase extends BaseTest {

    enum Result { OK, NOT_FOUND, NO_CREDENTIALS }

    private SshClient client;
    private ClientChannel channel;
    private ClientSession session;

    void addUsers(String manageruser, String vieweruser) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pipe = openSshChannel("karaf", "karaf", out);
        pipe.write(("jaas:realm-manage --realm=karaf"
                + ";jaas:user-add " + manageruser + " " + manageruser
                + ";jaas:role-add " + manageruser + " manager"
                + ";jaas:role-add " + manageruser + " viewer"
                + ";jaas:role-add " + manageruser + " ssh"
                + ";jaas:user-add " + vieweruser + " " + vieweruser
                + ";jaas:role-add " + vieweruser + " viewer"
                + ";jaas:role-add " + vieweruser + " ssh"
                + ";jaas:update;jaas:realm-manage --realm=karaf;jaas:user-list\n").getBytes());
        pipe.flush();
        closeSshChannel(pipe);
        System.out.println(new String(out.toByteArray()));
    }

    void addViewer(String vieweruser) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream pipe = openSshChannel("karaf", "karaf", out);
        pipe.write(("jaas:realm-manage --realm=karaf"
                + ";jaas:user-add " + vieweruser + " " + vieweruser
                + ";jaas:role-add " + vieweruser + " viewer"
                + ";jaas:role-add " + vieweruser + " ssh"
                + ";jaas:update;jaas:realm-manage --realm=karaf;jaas:user-list\n").getBytes());
        pipe.flush();
        closeSshChannel(pipe);
        System.out.println(new String(out.toByteArray()));
    }

    String assertCommand(String user, String command, Result result) throws Exception {
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

    private OutputStream openSshChannel(String username, String password, OutputStream ... outputs) throws Exception {
        client = SshClient.setUpDefaultClient();
        client.start();
        String sshPort = getSshPort();
        Awaitility.await().ignoreExceptions().until(() -> {
            ConnectFuture future = client.connect(username, "localhost", Integer.parseInt(sshPort));
            future.await();
            session = future.getSession();
            Set<ClientSessionEvent> ret = EnumSet.of(ClientSessionEvent.WAIT_AUTH);
            while (ret.contains(ClientSessionEvent.WAIT_AUTH)) {
                session.addPasswordIdentity(password);
                session.auth().verify();
                ret = session.waitFor(EnumSet.of(ClientSessionEvent.WAIT_AUTH, ClientSessionEvent.CLOSED, ClientSessionEvent.AUTHED), 0);
            }
            if (ret.contains(ClientSessionEvent.CLOSED)) {
                throw new Exception("Could not open SSH channel");
            }
            return true;
        });

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

        channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
        session.close(true);
        client.stop();

        client = null;
        channel = null;
        session = null;
    }
}
