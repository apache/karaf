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
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.io.input.NoCloseInputStream;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
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
            hostname = hostname.substring(hostname.indexOf('@') + 1);
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
        if (this.session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME) != null) {
            client.setAgentFactory(KarafAgentFactory.getInstance());
            String agentSocket = this.session.get(SshAgent.SSH_AUTHSOCKET_ENV_NAME).toString();
            client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentSocket);
        }
        KnownHostsManager knownHostsManager = new KnownHostsManager(new File(System.getProperty("user.home"), ".sshkaraf/known_hosts"));
        ServerKeyVerifier serverKeyVerifier = new ServerKeyVerifierImpl(knownHostsManager, quiet);
        client.setServerKeyVerifier(serverKeyVerifier);
        client.setKeyIdentityProvider(new FileKeyPairProvider());
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
                    final org.jline.terminal.Terminal terminal = (org.jline.terminal.Terminal) session.get(".jline.terminal");
                    Attributes attributes = terminal.enterRawMode();
                    try {
                        Map<PtyMode, Integer> modes = new HashMap<>();
                        // Control chars
                        addMode(modes, PtyMode.VINTR, attributes, ControlChar.VINTR);
                        addMode(modes, PtyMode.VQUIT, attributes, ControlChar.VQUIT);
                        addMode(modes, PtyMode.VERASE, attributes, ControlChar.VERASE);
                        addMode(modes, PtyMode.VKILL, attributes, ControlChar.VKILL);
                        addMode(modes, PtyMode.VEOF, attributes, ControlChar.VEOF);
                        addMode(modes, PtyMode.VEOL, attributes, ControlChar.VEOL);
                        addMode(modes, PtyMode.VEOL2, attributes, ControlChar.VEOL2);
                        addMode(modes, PtyMode.VSTART, attributes, ControlChar.VSTART);
                        addMode(modes, PtyMode.VSTOP, attributes, ControlChar.VSTOP);
                        addMode(modes, PtyMode.VSUSP, attributes, ControlChar.VSUSP);
                        addMode(modes, PtyMode.VDSUSP, attributes, ControlChar.VDSUSP);
                        addMode(modes, PtyMode.VREPRINT, attributes, ControlChar.VREPRINT);
                        addMode(modes, PtyMode.VWERASE, attributes, ControlChar.VWERASE);
                        addMode(modes, PtyMode.VLNEXT, attributes, ControlChar.VLNEXT);
                        addMode(modes, PtyMode.VSTATUS, attributes, ControlChar.VSTATUS);
                        addMode(modes, PtyMode.VDISCARD, attributes, ControlChar.VDISCARD);
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
                        String ctype = (String) session.get("LC_CTYPE");
                        if (ctype == null) {
                            ctype = Locale.getDefault().toString() + "."
                                    + System.getProperty("input.encoding", Charset.defaultCharset().name());
                        }
                        channel.setEnv("LC_CTYPE", ctype);
                        channel.setIn(new NoCloseInputStream(terminal.input()));
                        channel.setOut(new NoCloseOutputStream(terminal.output()));
                        channel.setErr(new NoCloseOutputStream(terminal.output()));
                        channel.open().verify();
                        org.jline.terminal.Terminal.SignalHandler prevWinchHandler = terminal.handle(org.jline.terminal.Terminal.Signal.WINCH, signal -> {
                            try {
                                Size size = terminal.getSize();
                                channel.sendWindowChange(size.getColumns(), size.getRows());
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        org.jline.terminal.Terminal.SignalHandler prevQuitHandler = terminal.handle(org.jline.terminal.Terminal.Signal.QUIT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VQUIT));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        org.jline.terminal.Terminal.SignalHandler prevIntHandler = terminal.handle(org.jline.terminal.Terminal.Signal.INT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VINTR));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        org.jline.terminal.Terminal.SignalHandler prevStopHandler = terminal.handle(org.jline.terminal.Terminal.Signal.TSTP, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VDSUSP));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        try {
                            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                        } finally {
                            terminal.handle(org.jline.terminal.Terminal.Signal.WINCH, prevWinchHandler);
                            terminal.handle(org.jline.terminal.Terminal.Signal.INT, prevIntHandler);
                            terminal.handle(org.jline.terminal.Terminal.Signal.TSTP, prevStopHandler);
                            terminal.handle(org.jline.terminal.Terminal.Signal.QUIT, prevQuitHandler);
                        }
                    } finally {
                        terminal.setAttributes(attributes);
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

    private static void addMode(Map<PtyMode, Integer> modes, PtyMode mode, Attributes attributes, ControlChar ctrl) {
        final int value = attributes.getControlChar(ctrl);
        if (value != -1) {
            modes.put(mode, value);
        }
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

    private static ClientSession connectWithRetries(SshClient client, String username, String host, int port, int maxAttempts) throws Exception {
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

}
