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

import java.io.*;
import java.io.Closeable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import jline.NoInterruptUnixTerminal;
import jline.Terminal;
import jline.TerminalFactory;

import jline.UnixTerminal;
import jline.internal.TerminalLineSettings;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.ClientBuilder;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.auth.keyboard.UserInteraction;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.channel.ClientChannelEvent;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.FactoryManager;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.client.channel.ClientChannel;
import org.apache.sshd.client.channel.PtyCapableChannelSession;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.SshConstants;
import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.kex.KeyExchange;
import org.apache.sshd.common.keyprovider.AbstractFileKeyPairProvider;
import org.apache.sshd.common.session.Session;
import org.apache.sshd.common.util.SecurityUtils;
import org.apache.sshd.common.util.buffer.Buffer;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.internal.CLibrary;
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
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(config.getFile())));
            try {
                for (int c = reader.read(); c >= 0; c = reader.read()) {
                    sb.append((char) c);
                }
            } finally {
                reader.close();
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

        SshClient client = null;
        Terminal terminal = null;
        int exitStatus = 0;
        try {
            ClientBuilder clientBuilder = ClientBuilder.builder();

            client = clientBuilder.build();
            setupAgent(config.getUser(), config.getKeyFile(), client);
            client.getProperties().put(FactoryManager.IDLE_TIMEOUT, String.valueOf(config.getIdleTimeout()));
            final Console console = System.console();
            if (console != null) {
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
            client.start();
            if (console != null) {
                console.printf("Logging in as %s\n", config.getUser());
            }
            ClientSession session = connectWithRetries(client, config);
            if (config.getPassword() != null) {
                session.addPasswordIdentity(config.getPassword());
            }
            session.auth().verify();

            ClientChannel channel;
            if (config.getCommand().length() > 0) {
                channel = session.createChannel("exec", config.getCommand() + "\n");
                channel.setIn(new ByteArrayInputStream(new byte[0]));
            } else {
                TerminalFactory.registerFlavor(TerminalFactory.Flavor.UNIX, UnixTerminal.class);
                terminal = TerminalFactory.create();
                if (terminal instanceof UnixTerminal) {
                    TerminalLineSettings settings = ((UnixTerminal) terminal).getSettings();
                    settings.undef("vlnext");
                    settings.undef("vintr");
                }
                channel = session.createChannel("shell");
                ConsoleInputStream in = new ConsoleInputStream(terminal.wrapInIfNeeded(System.in));
                new Thread(in).start();
                channel.setIn(in);
                if (terminal instanceof UnixTerminal) {
                    TerminalLineSettings settings = ((UnixTerminal) terminal).getSettings();
                    Map<PtyMode, Integer> modes = new HashMap<>();
                    // Control chars
                    modes.put(PtyMode.VINTR, settings.getProperty("vintr"));
                    modes.put(PtyMode.VQUIT, settings.getProperty("vquit"));
                    modes.put(PtyMode.VERASE, settings.getProperty("verase"));
                    modes.put(PtyMode.VKILL, settings.getProperty("vkill"));
                    modes.put(PtyMode.VEOF, settings.getProperty("veof"));
                    modes.put(PtyMode.VEOL, settings.getProperty("veol"));
                    modes.put(PtyMode.VEOL2, settings.getProperty("veol2"));
                    modes.put(PtyMode.VSTART, settings.getProperty("vstart"));
                    modes.put(PtyMode.VSTOP, settings.getProperty("vstop"));
                    modes.put(PtyMode.VSUSP, settings.getProperty("vsusp"));
                    modes.put(PtyMode.VDSUSP, settings.getProperty("vdusp"));
                    modes.put(PtyMode.VREPRINT, settings.getProperty("vreprint"));
                    modes.put(PtyMode.VWERASE, settings.getProperty("vwerase"));
                    modes.put(PtyMode.VLNEXT, settings.getProperty("vlnext"));
                    modes.put(PtyMode.VSTATUS, settings.getProperty("vstatus"));
                    modes.put(PtyMode.VDISCARD, settings.getProperty("vdiscard"));
                    // Input flags
                    modes.put(PtyMode.IGNPAR, getFlag(settings, PtyMode.IGNPAR));
                    modes.put(PtyMode.PARMRK, getFlag(settings, PtyMode.PARMRK));
                    modes.put(PtyMode.INPCK, getFlag(settings, PtyMode.INPCK));
                    modes.put(PtyMode.ISTRIP, getFlag(settings, PtyMode.ISTRIP));
                    modes.put(PtyMode.INLCR, getFlag(settings, PtyMode.INLCR));
                    modes.put(PtyMode.IGNCR, getFlag(settings, PtyMode.IGNCR));
                    modes.put(PtyMode.ICRNL, getFlag(settings, PtyMode.ICRNL));
                    modes.put(PtyMode.IXON, getFlag(settings, PtyMode.IXON));
                    modes.put(PtyMode.IXANY, getFlag(settings, PtyMode.IXANY));
                    modes.put(PtyMode.IXOFF, getFlag(settings, PtyMode.IXOFF));
                    // Local flags
                    modes.put(PtyMode.ISIG, getFlag(settings, PtyMode.ISIG));
                    modes.put(PtyMode.ICANON, getFlag(settings, PtyMode.ICANON));
                    modes.put(PtyMode.ECHO, getFlag(settings, PtyMode.ECHO));
                    modes.put(PtyMode.ECHOE, getFlag(settings, PtyMode.ECHOE));
                    modes.put(PtyMode.ECHOK, getFlag(settings, PtyMode.ECHOK));
                    modes.put(PtyMode.ECHONL, getFlag(settings, PtyMode.ECHONL));
                    modes.put(PtyMode.NOFLSH, getFlag(settings, PtyMode.NOFLSH));
                    modes.put(PtyMode.TOSTOP, getFlag(settings, PtyMode.TOSTOP));
                    modes.put(PtyMode.IEXTEN, getFlag(settings, PtyMode.IEXTEN));
                    // Output flags
                    modes.put(PtyMode.OPOST, getFlag(settings, PtyMode.OPOST));
                    modes.put(PtyMode.OLCUC, getFlag(settings, PtyMode.OLCUC));
                    modes.put(PtyMode.ONLCR, getFlag(settings, PtyMode.ONLCR));
                    modes.put(PtyMode.OCRNL, getFlag(settings, PtyMode.OCRNL));
                    modes.put(PtyMode.ONOCR, getFlag(settings, PtyMode.ONOCR));
                    modes.put(PtyMode.ONLRET, getFlag(settings, PtyMode.ONLRET));
                    ((ChannelShell) channel).setPtyModes(modes);
                } else {
                    ((ChannelShell) channel).setupSensibleDefaultPty();
                }
                ((ChannelShell) channel).setPtyColumns(terminal.getWidth());
                ((ChannelShell) channel).setPtyLines(terminal.getHeight());
                ((ChannelShell) channel).setAgentForwarding(true);
                String ctype = System.getenv("LC_CTYPE");
                if (ctype == null) {
                    ctype = Locale.getDefault().toString() + "."
                            + System.getProperty("input.encoding", Charset.defaultCharset().name());
                }
                ((ChannelShell) channel).setEnv("LC_CTYPE", ctype);
            }
            channel.setOut(AnsiConsole.wrapOutputStream(System.out));
            channel.setErr(AnsiConsole.wrapOutputStream(System.err));
            channel.open().verify();
            if (channel instanceof PtyCapableChannelSession) {
                registerSignalHandler(terminal, (PtyCapableChannelSession) channel);
            }
            channel.waitFor(EnumSet.of(ClientChannelEvent.CLOSED), 0);
            if (channel.getExitStatus() != null) {
                exitStatus = channel.getExitStatus();
            }
        } catch (Throwable t) {
            if (config.getLevel() > SimpleLogger.WARN) {
                t.printStackTrace();
            } else {
                System.err.println(t.getMessage());
            }
            System.exit(1);
        } finally {
            try {
                client.stop();
            } catch (Throwable t) {
            }
            try {
                if (terminal != null) {
                    terminal.restore();
                }
            } catch (Throwable t) {
            }
        }
        System.exit(exitStatus);
    }

    private static int getFlag(TerminalLineSettings settings, PtyMode mode) {
        String name = mode.toString().toLowerCase();
        return (settings.getPropertyAsString(name) != null) ? 1 : 0;
    }

    private static void setupAgent(String user, String keyFile, SshClient client) {
        SshAgent agent;
        URL builtInPrivateKey = Main.class.getClassLoader().getResource("karaf.key");
        agent = startAgent(user, builtInPrivateKey, keyFile);
        client.setAgentFactory(new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
    }

    private static ClientSession connectWithRetries(SshClient client, ClientConfig config) throws Exception, InterruptedException {
        ClientSession session = null;
        int retries = 0;
        do {
            ConnectFuture future = client.connect(config.getUser(), config.getHost(), config.getPort());
            future.await();
            try {
                session = future.getSession();
            } catch (RuntimeSshException ex) {
                if (retries++ < config.getRetryAttempts()) {
                    Thread.sleep(config.getRetryDelay() * 1000);
                    System.out.println("retrying (attempt " + retries + ") ...");
                } else {
                    throw ex;
                }
            }
        } while (session == null);
        return session;
    }

    private static SshAgent startAgent(String user, URL privateKeyUrl, String keyFile) {
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

    private static void close(Closeable is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e1) {
                // Ignore
            }
        }
    }

    private static String readLine(String msg) throws IOException {
        StringBuffer sb = new StringBuffer();
        System.err.print(msg);
        System.err.flush();
        for (; ; ) {
            int c = System.in.read();
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

    private static void registerSignalHandler(final Terminal terminal, final PtyCapableChannelSession channel) {
        try {
            Class<?> signalClass = Class.forName("sun.misc.Signal");
            Class<?> signalHandlerClass = Class.forName("sun.misc.SignalHandler");
            // Implement signal handler
            Object signalHandler = Proxy.newProxyInstance(Main.class.getClassLoader(),
                    new Class<?>[]{signalHandlerClass}, new InvocationHandler() {
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            // Ugly hack to force the jline unix terminal to retrieve the width/height of the terminal
                            // because results are cached for 1 second.
                            try {
                                Field field = terminal.getClass().getSuperclass().getDeclaredField("settings");
                                field.setAccessible(true);
                                Object settings = field.get(terminal);
                                field = settings.getClass().getDeclaredField("configLastFetched");
                                field.setAccessible(true);
                                field.setLong(settings, 0L);
                            } catch (Throwable t) {
                                // Ignore
                            }
                            // TODO: replace with PtyCapableChannelSession#sendWindowChange
                            Session session = channel.getSession();
                            Buffer buffer = session.createBuffer(SshConstants.SSH_MSG_CHANNEL_REQUEST);
                            buffer.putInt(channel.getRecipient());
                            buffer.putString("window-change");
                            buffer.putBoolean(false);
                            buffer.putInt(terminal.getWidth());
                            buffer.putInt(terminal.getHeight());
                            buffer.putInt(0);
                            buffer.putInt(0);
                            session.writePacket(buffer);
                            return null;
                        }
                    }
            );
            // Register the signal handler, this code is equivalent to:
            // Signal.handle(new Signal("CONT"), signalHandler);
            signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(null, signalClass.getConstructor(String.class).newInstance("WINCH"), signalHandler);
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
            signalClass.getMethod("handle", signalClass, signalHandlerClass).invoke(null, signalClass.getConstructor(String.class).newInstance("WINCH"), signalHandler);
        } catch (Exception e) {
            // Ignore this exception, if the above failed, the signal API is incompatible with what we're expecting

        }
    }

    private static class ConsoleInputStream extends InputStream implements Runnable {

        private InputStream in;
        private boolean eof = false;
        private final BlockingQueue<Integer> queue = new ArrayBlockingQueue<Integer>(1024);

        public ConsoleInputStream(InputStream in) {
            this.in = in;
        }

        private int read(boolean wait) throws IOException {
            if (eof && queue.isEmpty()) {
                return -1;
            }
            Integer i;
            if (wait) {
                try {
                    i = queue.take();
                } catch (InterruptedException e) {
                    throw new InterruptedIOException();
                }
            } else {
                i = queue.poll();
            }
            if (i == null) {
                return -1;
            }
            return i;
        }

        @Override
        public int read() throws IOException {
            return read(true);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException();
            } else if (off < 0 || len < 0 || len > b.length - off) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return 0;
            }

            int nb = 1;
            int i = read(true);
            if (i < 0) {
                return -1;
            }
            b[off++] = (byte) i;
            while (nb < len) {
                i = read(false);
                if (i < 0) {
                    return nb;
                }
                b[off++] = (byte) i;
                nb++;
            }
            return nb;
        }

        @Override
        public int available() throws IOException {
            return queue.size();
        }

        public void run() {
            try {
                while (true) {
                    try {
                        int c = in.read();
                        if (c == -1) {
                            return;
                        }
                        queue.put(c);
                    } catch (Throwable t) {
                        return;
                    }
                }
            } finally {
                eof = true;
                try {
                    queue.put(-1);
                } catch (InterruptedException e) {
                }
            }
        }
    }

}
