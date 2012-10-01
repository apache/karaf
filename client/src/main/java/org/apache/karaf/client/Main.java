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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.KeyPair;
import java.io.InterruptedIOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import jline.Terminal;
import org.apache.karaf.shell.console.jline.TerminalFactory;
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
        String host = "localhost";
        int port = 8101;
        String user = "karaf";
        String password = null;
        StringBuilder sb = new StringBuilder();
        int level = 1;
        int retryAttempts = 0;
        int retryDelay = 2;

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-a")) {
                    port = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-h")) {
                    host = args[++i];
                } else if (args[i].equals("-u")) {
                    user = args[++i];
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-r")) {
                    retryAttempts = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-p")) {
                    password = args[++i];
                } else if (args[i].equals("-d")) {
                    retryDelay = Integer.parseInt(args[++i]);
                } else if (args[i].equals("--help")) {
                    System.out.println("Apache Karaf client");
                    System.out.println("  -a [port]     specify the port to connect to");
                    System.out.println("  -h [host]     specify the host to connect to");
                    System.out.println("  -u [user]     specify the user name");
                    System.out.println("  -p [password] specify the password (optional, if not provided, the password is prompted)");
                    System.out.println("                NB: this option is deprecated and will be removed in next Karaf version");
                    System.out.println("  --help        shows this help message");
                    System.out.println("  -v            raise verbosity");
                    System.out.println("  -r [attempts] retry connection establishment (up to attempts times)");
                    System.out.println("  -d [delay]    intra-retry delay (defaults to 2 seconds)");
                    System.out.println("  [commands]    commands to run");
                    System.out.println("If no commands are specified, the client will be put in an interactive mode");
                    System.exit(0);
                } else {
                    System.err.println("Unknown option: " + args[i]);
                    System.err.println("Run with --help for usage");
                    System.exit(1);
                }
            } else {
                sb.append(args[i]);
                sb.append(' ');
            }
        }
        SimpleLogger.setLevel(level);

        SshClient client = null;
        Terminal terminal = null;
        SshAgent agent = null;
        try {
            agent = startAgent(user);
            client = SshClient.setUpDefaultClient();
            client.setAgentFactory(new LocalAgentFactory(agent));
            client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
            client.start();
            int retries = 0;
            ClientSession session = null;
            do {
                ConnectFuture future = client.connect(host, port);
                future.await();
                try { 
                    session = future.getSession();
                } catch (RuntimeSshException ex) {
                    if (retries++ < retryAttempts) {
                        Thread.sleep(retryDelay * 1000);
                        System.out.println("retrying (attempt " + retries + ") ...");
                    } else {
                        throw ex;
                    }
                }
            } while (session == null);
            if (!session.authAgent(user).await().isSuccess()) {
                if (password == null) {
                    password = readLine("Password: ");
                }
                if (!session.authPassword(user, password).await().isSuccess()) {
                    throw new Exception("Authentication failure");
                }
            }
            ClientChannel channel;
			if (sb.length() > 0) {
                channel = session.createChannel("exec", sb.append("\n").toString());
                channel.setIn(new ByteArrayInputStream(new byte[0]));
			} else {
                terminal = new TerminalFactory().getTerminal();
 				channel = session.createChannel("shell");
                ConsoleInputStream in = new ConsoleInputStream(terminal.wrapInIfNeeded(System.in));
                new Thread(in).start();
                channel.setIn(in);
                ((ChannelShell) channel).setupSensibleDefaultPty();
                ((ChannelShell) channel).setAgentForwarding(true);
            }
            channel.setOut(AnsiConsole.wrapOutputStream(System.out));
            channel.setErr(AnsiConsole.wrapOutputStream(System.err));
            channel.open();
            channel.waitFor(ClientChannel.CLOSED, 0);
        } catch (Throwable t) {
            if (level > 1) {
                t.printStackTrace();
            } else {
                System.err.println(t.getMessage());
            }
            System.exit(1);
        } finally {
            try {
                client.stop();
            } catch (Throwable t) { }
            try {
                if (terminal != null) {
                    terminal.restore();
                }
            } catch (Throwable t) { }
        }
        System.exit(0);
    }

    protected static SshAgent startAgent(String user) {
        try {
            SshAgent local = new AgentImpl();
            URL url = Main.class.getClassLoader().getResource("karaf.key");
            InputStream is = url.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            local.addIdentity(keyPair, "karaf");
            return local;
        } catch (Throwable e) {
            System.err.println("Error starting ssh agent for: " + e.getMessage());
            return null;
        }
    }

    public static String readLine(String msg) throws IOException {
        StringBuffer sb = new StringBuffer();
        System.err.print(msg);
        System.err.flush();
        for (;;) {
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

        private int read(boolean wait) throws IOException
        {
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
        public int read() throws IOException
        {
            return read(true);
        }

        @Override
        public int read(byte b[], int off, int len) throws IOException
        {
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
