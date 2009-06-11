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

package org.apache.geronimo.gshell.commands.ssh;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.util.NoCloseInputStream;
import org.apache.sshd.common.util.NoCloseOutputStream;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.i18n.MessageSource;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.io.PromptReader;
import org.apache.geronimo.gshell.spring.BeanContainer;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Connect to a SSH server.
 *
 * @version $Rev: 721244 $ $Date: 2008-11-27 18:19:56 +0100 (Thu, 27 Nov 2008) $
 */
public class SshAction
    implements CommandAction, BeanContainerAware
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

    private BeanContainer container;

	private ClientSession session;

    public void setBeanContainer(final BeanContainer container) {
        assert container != null;
        this.container = container;
    }

    /**
     * Helper to validate that prompted username or password is not null or empty.
     */
    private class UsernamePasswordValidator
        implements PromptReader.Validator
    {
        private String type;

        private int count = 0;

        private int max = 3;

        public UsernamePasswordValidator(final String type) {
            assert type != null;

            this.type = type;
        }

        public boolean isValid(final String value) {
            count++;

            if (value != null && value.trim().length() > 0) {
                return true;
            }

            if (count >= max) {
                throw new RuntimeException("Too many attempts; failed to prompt user for " + type + " after " + max + " tries");
            }

            return false;
        }
    }

    public Object execute(final CommandContext context) throws Exception {
        assert context != null;
        IO io = context.getIo();
        MessageSource messages = context.getCommand().getMessages();

        //
        // TODO: Parse hostname for <username>@<hostname>
        //
        
        io.info(messages.format("info.connecting", hostname, port));

        // If the username/password was not configured via cli, then prompt the user for the values
        if (username == null || password == null) {
            PromptReader prompter = new PromptReader(io);
            String text;

            log.debug("Prompting user for credentials");

            if (username == null) {
                text = messages.getMessage("prompt.username");
                username = prompter.readLine(text + ": ", new UsernamePasswordValidator(text));
            }

            if (password == null) {
                text = messages.getMessage("prompt.password");
                password = prompter.readPassword(text + ": ", new UsernamePasswordValidator(text));
            }
        }

        // Create the client from prototype
        SshClient client = container.getBean(SshClient.class);
        log.debug("Created client: {}", client);
        client.start();;

        try {
            ConnectFuture future = client.connect(hostname, port);
            future.await();
            session = future.getSession();
            try {
                io.info(messages.getMessage("info.connected"));

                session.authPassword(username, password);
                int ret = session.waitFor(ClientSession.WAIT_AUTH | ClientSession.CLOSED | ClientSession.AUTHED, 0);
                if ((ret & ClientSession.AUTHED) == 0) {
                    io.err.println("Authentication failed");
                    return Result.FAILURE;
                }

                ClientChannel channel = session.createChannel("shell");
                channel.setIn(new NoCloseInputStream(io.inputStream));
                channel.setOut(new NoCloseOutputStream(io.outputStream));
                channel.setErr(new NoCloseOutputStream(io.errorStream));
                channel.open();
                channel.waitFor(ClientChannel.CLOSED, 0);
            } finally {
                session.close(false);
            }
        } finally {
            client.stop();
        }

        io.verbose(messages.getMessage("verbose.disconnected"));

        return Result.SUCCESS;
    }
}
