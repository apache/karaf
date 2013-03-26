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
import java.net.URL;
import java.security.KeyPair;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import jline.NoInterruptUnixTerminal;
import jline.Terminal;
import jline.TerminalFactory;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
import org.apache.sshd.agent.local.LocalAgentFactory;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.RuntimeSshException;
import org.fusesource.jansi.AnsiConsole;
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
        try {
            client = SshClient.setUpDefaultClient();
            setupAgent(config.getUser(), client);
            client.start();
            ClientSession session = connectWithRetries(client, config);
            if (!session.authAgent(config.getUser()).await().isSuccess()) {
                String password = null;
                Console console = System.console();
                if (console != null) {
                    char[] readPassword = console.readPassword("Password: ");
                    if (readPassword != null) {
                        password = new String(readPassword);
                    }
                } else {
                    throw new Exception("Unable to prompt password: could not get system console");
                }
                if (!session.authPassword(config.getUser(), password).await().isSuccess()) {
                    throw new Exception("Authentication failure");
                }
            }
            ClientChannel channel;
            if (config.getCommand().length() > 0) {
                channel = session.createChannel("exec", config.getCommand() + "\n");
                channel.setIn(new ByteArrayInputStream(new byte[0]));
            } else {
                TerminalFactory.registerFlavor(TerminalFactory.Flavor.UNIX, NoInterruptUnixTerminal.class);
                terminal = TerminalFactory.create();
                channel = session.createChannel("shell");
                ConsoleInputStream in = new ConsoleInputStream(terminal.wrapInIfNeeded(System.in));
                new Thread(in).start();
                channel.setIn(in);
                ((ChannelShell) channel).setPtyColumns(terminal != null ? terminal.getWidth() : 80);
                ((ChannelShell) channel).setupSensibleDefaultPty();
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
            channel.open();
            channel.waitFor(ClientChannel.CLOSED, 0);
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
        System.exit(0);
    }

    private static void setupAgent(String user, SshClient client) {
        SshAgent agent;
        URL builtInPrivateKey = Main.class.getClassLoader().getResource("karaf.key");
        agent = startAgent(user, builtInPrivateKey);
        client.setAgentFactory(new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
    }

    private static ClientSession connectWithRetries(SshClient client, ClientConfig config) throws Exception, InterruptedException {
        ClientSession session = null;
        int retries = 0;
        do {
            ConnectFuture future = client.connect(config.getHost(), config.getPort());
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

    private static SshAgent startAgent(String user, URL privateKeyUrl) {
        InputStream is = null;
        try {
            SshAgent agent = new AgentImpl();
            is = privateKeyUrl.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            is.close();
            agent.addIdentity(keyPair, user);
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
