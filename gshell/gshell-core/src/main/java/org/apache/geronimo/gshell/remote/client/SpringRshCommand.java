/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.geronimo.gshell.remote.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.io.IOException;

import jline.Terminal;
import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.spring.NoCloseInputStream;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.console.PromptReader;
import org.apache.geronimo.gshell.remote.client.handler.ClientMessageHandler;
import org.apache.geronimo.gshell.remote.client.proxy.SpringRemoteShellProxy;
import org.apache.geronimo.gshell.remote.crypto.CryptoContext;
import org.apache.geronimo.gshell.whisper.transport.TransportFactoryLocator;

/**
 * Created by IntelliJ IDEA.
 * User: gnodet
 * Date: Dec 6, 2007
 * Time: 8:38:02 AM
 * To change this template use File | Settings | File Templates.
 */
@CommandComponent(id="gshell-remote:rsh", description="Connect to a remote GShell server")
public class SpringRshCommand extends OsgiCommandSupport {

    @Option(name="-b", aliases={"--bind"}, metaVar="URI", description="Bind local address to URI")
    private URI local;

    @Option(name="-u", aliases={"--username"}, metaVar="USERNAME", description="Remote user name")
    private String username;

    @Option(name="-p", aliases={"--password"}, metaVar="PASSWORD", description="Remote user password")
    private String password;

    @Option(name="-n", aliases={"--name"}, metaVar="NAME", description="Name of the instance to connect to")
    private String name;

    @Argument(metaVar="URI", required=true, index=0, description="Connect to remote server at URI")
    private URI remote;

    @Argument(metaVar="COMMAND", index=1, multiValued=true, description="Execute COMMAND in remote shell")
    private List<String> command = new ArrayList<String>();

    private Terminal terminal;

    private CryptoContext crypto;

    private TransportFactoryLocator locator;

    private List<ClientMessageHandler> handlers;

    public SpringRshCommand(final Terminal terminal,
                            final CryptoContext crypto,
                            final TransportFactoryLocator locator,
                            final List<ClientMessageHandler> handlers) {
        this.terminal = terminal;
        this.crypto = crypto;
        this.locator = locator;
        this.handlers = handlers;
    }

    protected OsgiCommandSupport createCommand() throws Exception {
        return new SpringRshCommand(terminal, crypto, locator, handlers);
    }

    protected Object doExecute() throws Exception {
        io.info("Connecting to: {}", remote);

        final NoCloseInputStream ncis = new NoCloseInputStream(this.io.inputStream);
        final IO io = new IO(ncis, this.io.outputStream, this.io.errorStream);
        final AtomicBoolean disconnected = new AtomicBoolean(false);

        try {
            SpringRshClient client = new SpringRshClient(crypto, locator, handlers) {
                protected void onSessionClosed() {
                    disconnected.set(true);
                    try {
                        ncis.close();
                    } catch (IOException e) {}
                }

                public String getInstanceName() {
                    return SpringRshCommand.this.name;
                }
            };
            client.initialize();
            PromptReader prompter = new PromptReader(terminal, io);
            prompter.initialize();

            client.connect(remote, local);

            this.io.info("Connected");

            // If the username/password was not configured via cli, then prompt the user for the values
            if (username == null || password == null) {
                if (username == null) {
                    username = prompter.readLine("Username: ");
                }

                if (password == null) {
                    password = prompter.readPassword("Password: ");
                }

                //
                // TODO: Handle null inputs...
                //
            }

            client.login(username, password);

            // client.echo("HELLO");
            // Thread.sleep(1 * 1000);

            SpringRemoteShellProxy shell = new SpringRemoteShellProxy(client, io, terminal);

            Object rv = SUCCESS;

            try {
                shell.run(command.toArray());
            }
            catch (ExitNotification n) {
                // Make sure that we catch this notification, so that our parent shell doesn't exit when the remote shell does
                rv = n.code;
            }

            if (!disconnected.get()) {
                shell.close();
            }

            this.io.verbose("Disconnecting");

            client.close();

            this.io.verbose("Disconnected");

            return rv;
        } finally {
            try {
                ncis.close();
            } catch (IOException e) {}
        }
    }
}
