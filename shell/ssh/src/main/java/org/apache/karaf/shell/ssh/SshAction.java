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
package org.apache.karaf.shell.ssh;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Signal;
import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.apache.sshd.common.util.io.NoCloseInputStream;
import org.apache.sshd.common.util.io.NoCloseOutputStream;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(scope = "ssh", name = "ssh", description = "Connects to a remote SSH server")
@Service
public class SshAction implements Action {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Option(name = "-l", aliases = {"--username"}, description = "The user name for remote login", required = false, multiValued = false)
    private String username;

    @Option(name = "-P", aliases = {"--password"}, description = "The password for remote login", required = false, multiValued = false)
    private String password;

    @Option(name = "-p", aliases = {"--port"}, description = "The port to use for SSH connection", required = false, multiValued = false)
    private int port = 22;

    @Option(name = "-k", aliases = {"--keyfile"}, description = "The private keyFile location when using key login, need have BouncyCastle registered as security provider using this flag", required = false, multiValued = false)
    private String keyFile;

    @Option(name = "-q", description = "Quiet Mode. Do not ask for confirmations", required = false, multiValued = false)
    private boolean quiet;

    @Option(name = "-r", aliases = {"--retries"}, description = "retry connection establishment (up to attempts times)", required = false, multiValued = false)
    private int retries = 0;

    @Argument(index = 0, name = "hostname", description = "The host name to connect to via SSH", required = true, multiValued = false)
    private String hostname;

    @Argument(index = 1, name = "command", description = "Optional command to execute", required = false, multiValued = true)
    private List<String> command;

    @Reference
    private Session session;


    @Override
    public Object execute() throws Exception {
        if (hostname.indexOf('@') >= 0) {
            if (username == null) {
                username = hostname.substring(0, hostname.indexOf('@'));
            }
            hostname = hostname.substring(hostname.indexOf('@') + 1, hostname.length());
        }

        System.out.println("Connecting to host " + hostname + " on port " + port);

        // If not specified, assume the current user name
        if (username == null) {
            username = (String) this.session.get("USER");
        }
        // If the username was not configured via cli, then prompt the user for the values
        if (username == null) {
            log.debug("Prompting user for login");
            if (username == null) {
                username = session.readLine("Login: ", null);
            }
        }

        SshClient client = SshClient.setUpDefaultClient();
        setupAgent(username, keyFile, client);
        KnownHostsManager knownHostsManager = new KnownHostsManager(new File(System.getProperty("user.home"), ".sshkaraf/known_hosts"));
        ServerKeyVerifier serverKeyVerifier = new ServerKeyVerifierImpl(knownHostsManager, quiet);
        client.setServerKeyVerifier(serverKeyVerifier);
        log.debug("Created client: {}", client);
        client.setUserInteraction(new UserInteraction() {
            @Override
            public void welcome(ClientSession session, String banner, String lang) {
                System.out.println(banner);
            }

            @Override
            public String[] interactive(ClientSession s, String name, String instruction, String lang, String[] prompt, boolean[] echo) {
                String[] answers = new String[prompt.length];
                try {
                    for (int i = 0; i < prompt.length; i++) {
                        answers[i] = session.readLine(prompt[i] + " ", echo[i] ? null : '*');
                    }
                } catch (IOException e) {
                }
                return answers;
            }
            @Override
            public boolean isInteractionAllowed(ClientSession session) {
                return true;
            }
            @Override
            public void serverVersionInfo(ClientSession session, List<String> lines) {
            }
            @Override
            public String getUpdatedPassword(ClientSession session, String prompt, String lang) {
                return null;
            }
        });
        client.start();

        try {
            ClientSession sshSession = connectWithRetries(client, username, hostname, port, retries);
            Object oldIgnoreInterrupts = this.session.get(Session.IGNORE_INTERRUPTS);

            try {
                if (password != null) {
                    sshSession.addPasswordIdentity(password);
                }

                sshSession.auth().verify();

                System.out.println("Connected");
                this.session.put(Session.IGNORE_INTERRUPTS, Boolean.TRUE);

                StringBuilder sb = new StringBuilder();
                if (command != null) {
                    for (String cmd : command) {
                        if (sb.length() > 0) {
                            sb.append(' ');
                        }
                        sb.append(cmd);
                    }
                }
                if (sb.length() > 0) {
                    ClientChannel channel = sshSession.createChannel("exec", sb.append("\n").toString());
                    channel.setIn(new ByteArrayInputStream(new byte[0]));
                    channel.setOut(new NoCloseOutputStream(System.out));
                    channel.setErr(new NoCloseOutputStream(System.err));
                    channel.open().verify();
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                } else if (session.getTerminal() != null) {
                    final ChannelShell channel = sshSession.createShellChannel();
                    final org.jline.terminal.Terminal jlineTerminal = (org.jline.terminal.Terminal) session.get(".jline.terminal");
                    Attributes attributes = jlineTerminal.enterRawMode();
                    try {
                        Map<PtyMode, Integer> modes = new HashMap<>();
                        // Control chars
                        modes.put(PtyMode.VINTR, attributes.getControlChar(ControlChar.VINTR));
                        modes.put(PtyMode.VQUIT, attributes.getControlChar(ControlChar.VQUIT));
                        modes.put(PtyMode.VERASE, attributes.getControlChar(ControlChar.VERASE));
                        modes.put(PtyMode.VKILL, attributes.getControlChar(ControlChar.VKILL));
                        modes.put(PtyMode.VEOF, attributes.getControlChar(ControlChar.VEOF));
                        modes.put(PtyMode.VEOL, attributes.getControlChar(ControlChar.VEOL));
                        modes.put(PtyMode.VEOL2, attributes.getControlChar(ControlChar.VEOL2));
                        modes.put(PtyMode.VSTART, attributes.getControlChar(ControlChar.VSTART));
                        modes.put(PtyMode.VSTOP, attributes.getControlChar(ControlChar.VSTOP));
                        modes.put(PtyMode.VSUSP, attributes.getControlChar(ControlChar.VSUSP));
                        modes.put(PtyMode.VDSUSP, attributes.getControlChar(ControlChar.VDSUSP));
                        modes.put(PtyMode.VREPRINT, attributes.getControlChar(ControlChar.VREPRINT));
                        modes.put(PtyMode.VWERASE, attributes.getControlChar(ControlChar.VWERASE));
                        modes.put(PtyMode.VLNEXT, attributes.getControlChar(ControlChar.VLNEXT));
                        modes.put(PtyMode.VSTATUS, attributes.getControlChar(ControlChar.VSTATUS));
                        modes.put(PtyMode.VDISCARD, attributes.getControlChar(ControlChar.VDISCARD));
                        // Input flags
                        modes.put(PtyMode.IGNPAR, getFlag(attributes, InputFlag.IGNPAR));
                        modes.put(PtyMode.PARMRK, getFlag(attributes, InputFlag.PARMRK));
                        modes.put(PtyMode.INPCK, getFlag(attributes, InputFlag.INPCK));
                        modes.put(PtyMode.ISTRIP, getFlag(attributes, InputFlag.ISTRIP));
                        modes.put(PtyMode.INLCR, getFlag(attributes, InputFlag.INLCR));
                        modes.put(PtyMode.IGNCR, getFlag(attributes, InputFlag.IGNCR));
                        modes.put(PtyMode.ICRNL, getFlag(attributes, InputFlag.ICRNL));
                        modes.put(PtyMode.IXON, getFlag(attributes, InputFlag.IXON));
                        modes.put(PtyMode.IXANY, getFlag(attributes, InputFlag.IXANY));
                        modes.put(PtyMode.IXOFF, getFlag(attributes, InputFlag.IXOFF));
                        // Local flags
                        modes.put(PtyMode.ISIG, getFlag(attributes, LocalFlag.ISIG));
                        modes.put(PtyMode.ICANON, getFlag(attributes, LocalFlag.ICANON));
                        modes.put(PtyMode.ECHO, getFlag(attributes, LocalFlag.ECHO));
                        modes.put(PtyMode.ECHOE, getFlag(attributes, LocalFlag.ECHOE));
                        modes.put(PtyMode.ECHOK, getFlag(attributes, LocalFlag.ECHOK));
                        modes.put(PtyMode.ECHONL, getFlag(attributes, LocalFlag.ECHONL));
                        modes.put(PtyMode.NOFLSH, getFlag(attributes, LocalFlag.NOFLSH));
                        modes.put(PtyMode.TOSTOP, getFlag(attributes, LocalFlag.TOSTOP));
                        modes.put(PtyMode.IEXTEN, getFlag(attributes, LocalFlag.IEXTEN));
                        // Output flags
                        modes.put(PtyMode.OPOST, getFlag(attributes, OutputFlag.OPOST));
                        modes.put(PtyMode.ONLCR, getFlag(attributes, OutputFlag.ONLCR));
                        modes.put(PtyMode.OCRNL, getFlag(attributes, OutputFlag.OCRNL));
                        modes.put(PtyMode.ONOCR, getFlag(attributes, OutputFlag.ONOCR));
                        modes.put(PtyMode.ONLRET, getFlag(attributes, OutputFlag.ONLRET));
                        channel.setPtyModes(modes);
                        channel.setPtyColumns(getTermWidth());
                        channel.setPtyLines(getTermHeight());
                        channel.setAgentForwarding(true);
                        channel.setEnv("TERM", session.getTerminal().getType());
                        Object ctype = session.get("LC_CTYPE");
                        if (ctype != null) {
                            channel.setEnv("LC_CTYPE", ctype.toString());
                        }
                        channel.setIn(new NoCloseInputStream(System.in));
                        channel.setOut(new NoCloseOutputStream(System.out));
                        channel.setErr(new NoCloseOutputStream(System.err));
                        channel.open().verify();
                        SignalListener signalListener = signal -> {
                            try {
                                Size size = jlineTerminal.getSize();
                                channel.sendWindowChange(size.getColumns(), size.getRows());
                            } catch (IOException e) {
                                // Ignore
                            }
                        };
                        session.getTerminal().addSignalListener(signalListener, Signal.WINCH);
                        try {
                            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                        } finally {
                            session.getTerminal().removeSignalListener(signalListener);
                        }
                    } finally {
                        jlineTerminal.setAttributes(attributes);
                    }
                } else {
                    throw new IllegalStateException("No terminal for interactive ssh session");
                }
            } finally {
                session.put(Session.IGNORE_INTERRUPTS, oldIgnoreInterrupts);
                sshSession.close(false);
            }
        } finally {
            client.stop();
        }

        return null;
    }

    private static int getFlag(Attributes attributes, InputFlag flag) {
        return attributes.getInputFlag(flag) ? 1 : 0;
    }

    private static int getFlag(Attributes attributes, OutputFlag flag) {
        return attributes.getOutputFlag(flag) ? 1 : 0;
    }

    private static int getFlag(Attributes attributes, LocalFlag flag) {
        return attributes.getLocalFlag(flag) ? 1 : 0;
    }


    private int getTermWidth() {
        Terminal term = session.getTerminal();
        return term != null ? term.getWidth() : 80;
    }

    private int getTermHeight() {
        Terminal term = session.getTerminal();
        return term != null ? term.getHeight() : 25;
    }

    private void setupAgent(String user, String keyFile, SshClient client) {
        SshAgent agent;
        URL url = getClass().getClassLoader().getResource("karaf.key");
        agent = startAgent(user, url, keyFile);
        client.setAgentFactory(new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
    }

    private SshAgent startAgent(String user, URL privateKeyUrl, String keyFile) {
        InputStream is = null;
        try {
            SshAgent agent = new AgentImpl();
            is = privateKeyUrl.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            is.close();
            agent.addIdentity(keyPair, user);
            if (keyFile != null) {
                AbstractFileKeyPairProvider fileKeyPairProvider = SecurityUtils.createFileKeyPairProvider();
                fileKeyPairProvider.setPaths(Collections.singleton(Paths.get(keyFile)));
                for (KeyPair key : fileKeyPairProvider.loadKeys()) {
                    agent.addIdentity(key, user);
                }
            }
            return agent;
        } catch (Throwable e) {
            close(is);
            System.err.println("Error starting ssh agent for: " + e.getMessage());
            return null;
        }
    }

    private static ClientSession connectWithRetries(SshClient client, String username, String host, int port, int maxAttempts) throws Exception, InterruptedException {
        ClientSession session = null;
        int retries = 0;
        do {
            ConnectFuture future = client.connect(username, host, port);
            future.await();
            try {
                session = future.getSession();
            } catch (Exception ex) {
                if (retries++ < maxAttempts) {
                    Thread.sleep(2 * 1000);
                    System.out.println("retrying (attempt " + retries + ") ...");
                } else {
                    throw ex;
                }
            }
        } while (session == null);
        return session;
    }

    private void close(Closeable is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }

}
