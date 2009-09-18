/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.felix.karaf.shell.ssh;

import java.io.IOException;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.apache.felix.karaf.shell.console.BlueprintContainerAware;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.service.blueprint.container.BlueprintContainer;

/**
 * Connect to a SSH server.
 *
 * @version $Rev: 721244 $ $Date: 2008-11-27 18:19:56 +0100 (Thu, 27 Nov 2008) $
 */
@Command(scope = "ssh", name = "ssh", description = "Connect to a remote SSH server")
public class SshAction
    extends OsgiCommandSupport implements BlueprintContainerAware
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name="-l", aliases={"--username"}, description = "Username")
    private String username;

    @Option(name="-P", aliases={"--password"}, description = "Password")
    private String password;

    @Argument(required=true, description = "Host")
    private String hostname;

    @Option(name="-p", aliases={"--port"}, description = "Port")
    private int port = 22;

    private BlueprintContainer container;

	private ClientSession session;
    private String sshClientId;

    public void setBlueprintContainer(final BlueprintContainer container) {
        assert container != null;
        this.container = container;
    }

    public void setSshClientId(String sshClientId) {
        this.sshClientId = sshClientId;
    }

    @Override
    protected Object doExecute() throws Exception {

        //
        // TODO: Parse hostname for <username>@<hostname>
        //

        System.out.println("Connecting to host " + hostname + " on port " + port);

        // If the username/password was not configured via cli, then prompt the user for the values
        if (username == null || password == null) {
            log.debug("Prompting user for credentials");
            if (username == null) {
                username = readLine("Login: ");
            }
            if (password == null) {
                password = readLine("Password: ");
            }
        }

        // Create the client from prototype
        SshClient client = (SshClient) container.getComponentInstance(sshClientId);
        log.debug("Created client: {}", client);
        client.start();

        try {
            ConnectFuture future = client.connect(hostname, port);
            future.await();
            session = future.getSession();
            try {
                System.out.println("Connected");

                session.authPassword(username, password);
                int ret = session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
                if ((ret & ClientSession.AUTHED) == 0) {
                    System.err.println("Authentication failed");
                    return null;
                }

                ClientChannel channel = session.createChannel("shell");
                channel.setIn(new NoCloseInputStream(System.in));
                channel.setOut(new NoCloseOutputStream(System.out));
                channel.setErr(new NoCloseOutputStream(System.err));
                channel.open();
                channel.waitFor(ClientChannel.CLOSED, 0);
            } finally {
                session.close(false);
            }
        } finally {
            client.stop();
        }

        return null;
    }

    public String readLine(String msg) throws IOException {
        StringBuffer sb = new StringBuffer();
        System.err.print(msg);
        System.err.flush();
        for (;;) {
            int c = super.session.getKeyboard().read();
            if (c < 0) {
                return null;
            }
            System.err.print((char) c);
            if (c == '\r' || c == '\n') {
                break;
            }
            sb.append((char) c);
        }
        return sb.toString();
    }

}
