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
package org.apache.karaf.client;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.time.Duration;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ChannelExec;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.apache.sshd.common.util.io.input.NoCloseInputStream;
import org.apache.sshd.common.util.io.output.NoCloseOutputStream;
import org.apache.sshd.core.CoreModuleProperties;
import org.jline.terminal.Attributes;
import org.jline.terminal.Attributes.ControlChar;
import org.jline.terminal.Attributes.InputFlag;
import org.jline.terminal.Attributes.LocalFlag;
import org.jline.terminal.Attributes.OutputFlag;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.slf4j.impl.SimpleLogger;

/**
 * A very simple
 */
public class Main {

    public static void main(String[] args) throws Exception {
        ClientConfig config = new ClientConfig(args);
        SimpleLogger.setLevel(config.getLevel());

        if (config.getFile() != null) {
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            try (Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config.getFile())))) {
                for (int c = reader.read(); c >= 0; c = reader.read()) {
                    sb.append((char) c);
                }
            }
            config.setCommand(sb.toString());
        } else if (config.isBatch()) {
            StringBuilder sb = new StringBuilder();
            sb.setLength(0);
            Reader reader = new BufferedReader(new InputStreamReader(System.in));
            for (int c = reader.read(); c >= 0; c = reader.read()) {
                sb.append((char) c);
            }
            config.setCommand(sb.toString());
        }

        try (SshClient client = ClientBuilder.builder().build()) {
            FilePasswordProvider passwordProvider = null;
            final Console console = System.console();
            if (console != null) {
                passwordProvider = (session, resourceKey, retryIndex) -> {
                    char[] pwd = console.readPassword("Enter password for " + resourceKey + ": ");
                    return new String(pwd);
                };
                client.setFilePasswordProvider(passwordProvider);
                client.setUserInteraction(new UserInteraction() {
                    @Override
                    public void welcome(ClientSession s, String banner, String lang) {
                        System.out.println(banner);
                    }
                    @Override
                    public String[] interactive(ClientSession s, String name, String instruction, String lang, String[] prompt, boolean[] echo) {
                        String[] answers = new String[prompt.length];
                        try {
                            for (int i = 0; i < prompt.length; i++) {
                                if (echo[i]) {
                                    answers[i] = console.readLine(prompt[i] + " ");
                                } else {
                                    answers[i] = new String(console.readPassword(prompt[i] + " "));
                                }
                                if (answers[i] == null) {
                                    return null;
                                }
                            }
                            return answers;
                        } catch (IOError e) {
                            return null;
                        }
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
            }

            if (config.getUser()==null || config.getUser().isEmpty()) {
            	while (true) {
            		String user = console.readLine("Enter user: ");
            		if (user==null || user.isEmpty()) {
            			System.err.println("User must not be empty!");
            		}
            		else {
                    	config.setUser(user);
                    	break;
            		}
            	}
            }
            else if (console != null) {
                console.printf("Logging in as %s\n", config.getUser());
            }

            setupAgent(config.getUser(), config.getKeyFile(), client, passwordProvider);

            // define hearbeat (for the keep alive) and timeouts
            // TODO this should be dealt by Apache SSH client directly using .ssh/config
            CoreModuleProperties.HEARTBEAT_INTERVAL.set(client, Duration.ofMillis(60000));
            CoreModuleProperties.IDLE_TIMEOUT.set(client, Duration.ofMillis(config.getIdleTimeout()));
            CoreModuleProperties.NIO2_READ_TIMEOUT.set(client, Duration.ofMillis(config.getIdleTimeout()));

            // TODO: remove the line below when SSHD-732 is fixed
            // client.setKeyPairProvider(new FileKeyPairProvider());
            client.start();
            ClientSession session = connectWithRetries(client, config);
            if (config.getPassword() != null) {
                session.addPasswordIdentity(config.getPassword());
            }
            session.auth().verify();

            int exitStatus = 0;
            String type = System.getProperty(TerminalBuilder.PROP_TYPE);
            if (type == null) {
                type = System.getenv("TERM");
            }
            if (type == null) {
                type = Terminal.TYPE_DUMB;
            }
            try (Terminal terminal = TerminalBuilder.builder()
                        .nativeSignals(true)
                        .type(type)
                        .signalHandler(Terminal.SignalHandler.SIG_IGN)
                        .build()) {
                if (config.getCommand().length() > 0) {
                    ChannelExec channel = session.createExecChannel(config.getCommand() + "\n");
                    channel.setIn(new ByteArrayInputStream(new byte[0]));
                    if (!config.isBatch()) {
                        new Thread(() -> {
                            while (true) {
                                try {
                                    int a = System.in.read();
                                    if (a == -1) {
                                        channel.close(true);
                                        break;
                                    }
                                    Thread.sleep(1000);
                                } catch (Exception e) {
                                    //ignore
                                }
                            }
                        }).start();
                    }
                    channel.setAgentForwarding(true);
                    NoCloseOutputStream output = new NoCloseOutputStream(terminal.output());
                    channel.setOut(output);
                    channel.setErr(output);
                    channel.open().verify();
                    channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
                    if (channel.getExitStatus() != null) {
                        exitStatus = channel.getExitStatus();
                    }

                } else {
                    ChannelShell channel = session.createShellChannel();
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
                        channel.setPtyColumns(terminal.getWidth());
                        channel.setPtyLines(terminal.getHeight());
                        channel.setAgentForwarding(true);
                        channel.setEnv("TERM", terminal.getType());
                        String ctype = System.getenv("LC_CTYPE");
                        if (ctype == null) {
                            ctype = Locale.getDefault().toString() + "."
                                    + System.getProperty("input.encoding", Charset.defaultCharset().name());
                        }
                        channel.setEnv("LC_CTYPE", ctype);
                        channel.setIn(new NoCloseInputStream(terminal.input()));
                        channel.setOut(new NoCloseOutputStream(terminal.output()));
                        channel.setErr(new NoCloseOutputStream(terminal.output()));
                        channel.open().verify();
                        Terminal.SignalHandler prevWinchHandler = terminal.handle(Terminal.Signal.WINCH, signal -> {
                            try {
                                Size size = terminal.getSize();
                                channel.sendWindowChange(size.getColumns(), size.getRows());
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevQuitHandler = terminal.handle(Terminal.Signal.QUIT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VQUIT));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevIntHandler = terminal.handle(Terminal.Signal.INT, signal -> {
                            try {
                                channel.getInvertedIn().write(attributes.getControlChar(Attributes.ControlChar.VINTR));
                                channel.getInvertedIn().flush();
                            } catch (IOException e) {
                                // Ignore
                            }
                        });
                        Terminal.SignalHandler prevStopHandler = terminal.handle(Terminal.Signal.TSTP, signal -> {
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
                            terminal.handle(Terminal.Signal.WINCH, prevWinchHandler);
                            terminal.handle(Terminal.Signal.INT, prevIntHandler);
                            terminal.handle(Terminal.Signal.TSTP, prevStopHandler);
                            terminal.handle(Terminal.Signal.QUIT, prevQuitHandler);
                        }
                        if (channel.getExitStatus() != null) {
                            exitStatus = channel.getExitStatus();
                        }
                    } finally {
                        terminal.setAttributes(attributes);
                    }
                }
            }
            System.exit(exitStatus);
        } catch (Throwable t) {
            if (config.getLevel() > SimpleLogger.WARN) {
                t.printStackTrace();
            } else {
                System.err.println(t.getMessage());
            }
            System.exit(1);
        }
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

    private static void setupAgent(String user, String keyFile, SshClient client, FilePasswordProvider passwordProvider) {
        SshAgent agent;
        agent = startAgent(user, keyFile, passwordProvider);
        client.setAgentFactory(new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
    }

    private static ClientSession connectWithRetries(SshClient client, ClientConfig config) throws Exception {
        ClientSession session = null;
        int retries = 0;
        do {
            ConnectFuture future = client.connect(config.getUser(), config.getHost(), config.getPort());
            future.await();
            try {
                session = future.getSession();
            } catch (RuntimeSshException ex) {
                if (retries++ < config.getRetryAttempts()) {
                    Thread.sleep(config.getRetryDelay() * 1000L);
                    System.out.println("retrying (attempt " + retries + ") ...");
                } else {
                    throw ex;
                }
            }
        } while (session == null);
        return session;
    }

    private static SshAgent startAgent(String user, String keyFile, FilePasswordProvider passwordProvider) {
        try {
            SshAgent agent = new AgentImpl();
            if (keyFile != null) {
                FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider(Paths.get(keyFile));
                fileKeyPairProvider.setPasswordFinder(passwordProvider);
                for (KeyPair key : fileKeyPairProvider.loadKeys(null)) {
                    agent.addIdentity(key, user);
                }
            }
            return agent;
        } catch (Throwable e) {
            System.err.println("Error starting ssh agent for: " + e.getMessage());
            return null;
        }
    }

    private static void registerSignalHandler(final Terminal terminal, final PtyCapableChannelSession channel) {
        try {
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            // Implement signal handler
            Object signalHandler = Proxy.newProxyInstance(Main.class.getClassLoader(),
                    new Class<?>[]{signalHandlerClass}, (proxy, method, args) -> {
                        Size size = terminal.getSize();
                        channel.sendWindowChange(size.getColumns(), size.getRows());
                        return null;
                    }
            );
            // Register the signal handler, this code is equivalent to:
            // Signal.handle(new Signal("CONT"), signalHandler);
            signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(
                    null,
                    signalClass.getConstructor(String.class).newInstance("WINCH"),
                    signalHandler);
        } catch (Exception e) {
            // Ignore this exception, if the above failed, the signal API is incompatible with what we're expecting

        }
    }

    private static void unregisterSignalHandler() {
        try {
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");

            Object signalHandler = signalHandlerClass.getField("SIG_DFL").get(null);
            // Register the signal handler, this code is equivalent to:
            // Signal.handle(new Signal("CONT"), signalHandler);
            signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(
                    null,
                    signalClass.getConstructor(String.class).newInstance("WINCH"),
                    signalHandler);
        } catch (Exception e) {
            // Ignore this exception, if the above failed, the signal API is incompatible with what we're expecting

        }
    }

}
