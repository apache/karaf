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
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.util.Iterator;
import java.util.Locale;
import java.util.Properties;
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
import org.apache.sshd.client.UserInteraction;
import org.apache.sshd.client.channel.ChannelShell;
import org.apache.sshd.client.future.ConnectFuture;
import org.apache.sshd.common.RuntimeSshException;
import org.apache.sshd.common.keyprovider.FileKeyPairProvider;
import org.fusesource.jansi.AnsiConsole;
import org.slf4j.impl.SimpleLogger;

/**
 * A very simple
 */
public class Main {

    private static final String ROLE_DELIMITER = ",";
    private static final String GROUP_PREFIX = "_g_";

    public static void main(String[] args) throws Exception {
        Properties shellCfg = loadProps(new File(System.getProperty("karaf.etc"), "org.apache.karaf.shell.cfg"));

        String host = shellCfg.getProperty("sshHost", "localhost");
        int port = Integer.parseInt(shellCfg.getProperty("sshPort", "8101"));
        int level = 1;
        int retryAttempts = 0;
        int retryDelay = 2;
        boolean batch = false;
        String file = null;
        String user = "karaf";
        String password = null;
        StringBuilder command = new StringBuilder();
        String keyFile = null;

        Properties usersCfg = loadProps(new File(System.getProperty("karaf.etc"), "users.properties"));
        if (!usersCfg.isEmpty()) {
            Iterator iter = usersCfg.keySet().iterator();
            while (iter.hasNext()) {
              user = (String) iter.next();
              if (!user.startsWith(GROUP_PREFIX)) {
                  break;
              }
            }
            password = (String) usersCfg.getProperty(user);
            if (password.contains(ROLE_DELIMITER)) {
                password = password.substring(0, password.indexOf(ROLE_DELIMITER));
            }
        }

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-a")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the port");
                        System.exit(1);
                    } else {
                        port = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-h")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the host");
                        System.exit(1);
                    } else {
                        host = args[i];
                    }
                } else if (args[i].equals("-u")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the user");
                        System.exit(1);
                    } else {
                        user = args[i];
                    }
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-r")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the attempts");
                        System.exit(1);
                    } else {
                        retryAttempts = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-p")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the password");
                        System.exit(1);
                    } else {
                        password = args[i];
                    }
                } else if (args[i].equals("-d")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the delay in seconds");
                        System.exit(1);
                    } else {
                        retryDelay = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-b")) {
                    batch = true;
                } else if (args[i].equals("-f")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the commands file");
                        System.exit(1);
                    } else {
                        file = args[i];
                    }
                } else if (args[i].equals("-k")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the key file");
                        System.exit(1);
                    } else {
                        keyFile = args[i];
                    }
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
                    System.out.println("  -b            batch mode, specify multiple commands via standard input");
                    System.out.println("  -f [file]    read commands from the specified file");
                    System.out.println("  -k [keyFile]    specify the private keyFile location when using key login, need have BouncyCastle registered as security provider using this flag");
                    System.out.println("  [commands]    commands to run");
                    System.out.println("If no commands are specified, the client will be put in an interactive mode");
                    System.exit(0);
                } else {
                    System.err.println("Unknown option: " + args[i]);
                    System.err.println("Run with --help for usage");
                    System.exit(1);
                }
            } else {
                command.append(args[i]);
                command.append(' ');
            }
        }
        SimpleLogger.setLevel(level);

        if (file != null) {
            Reader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            try {
                command.setLength(0);
                for (int c = reader.read(); c >= 0; c = reader.read()) {
                    command.append((char) c);
                }
            } finally {
                reader.close();
            }
        } else if (batch) {
            // read all stdin
            Reader reader = new BufferedReader(new InputStreamReader(System.in));
            command.setLength(0);
            for (int c = reader.read(); c >= 0; c = reader.read()) {
                command.append((char) c);
            }
        }

        SshClient client = null;
        Terminal terminal = null;
        SshAgent agent = null;
        int exitStatus = 0;
        try {

            final Console console = System.console();
            client = SshClient.setUpDefaultClient();
            setupAgent(user, client, keyFile);
            client.setUserInteraction(new UserInteraction() {
                public void welcome(String banner) {
                    System.out.println(banner);
                }

                public String[] interactive(String destination, String name, String instruction, String[] prompt, boolean[] echo) {
                    String[] answers = new String[prompt.length];
                    try {
                        for (int i = 0; i < prompt.length; i++) {
                            if (console != null) {
                                if (echo[i]) {
                                    answers[i] = console.readLine(prompt[i] + " ");
                                } else {
                                    answers[i] = new String(console.readPassword(prompt[i] + " "));
                                }
                            }
                        }
                    } catch (IOError e) {
                    }
                    return answers;
                }
            });
            client.start();
            if (console != null) {
                console.printf("Logging in as %s\n", user);
            }
            
            ClientSession session = null;
            int retries = 0;
            do {
                ConnectFuture future = client.connect(user, host, port);
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
            
            
            if (password != null) {
                session.addPasswordIdentity(password);
            }
            session.auth().verify();

            ClientChannel channel;
			if (command.length() > 0) {
                channel = session.createChannel("exec", command.append("\n").toString());
                channel.setIn(new ByteArrayInputStream(new byte[0]));
			} else {
                terminal = new TerminalFactory().getTerminal();
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
            if (channel.getExitStatus() != null) {
                exitStatus = channel.getExitStatus();
            }
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
        System.exit(exitStatus);
    }


    private static Properties loadProps(File file) {
        Properties props = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            if (is != null) {
                props.load(is);
            }
        } catch (Exception e) {
            System.err.println("Could not load properties from: " + file + ", Reason: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore
                }
            }
        }
        return props;
    }

    private static void setupAgent(String user, SshClient client, String keyFile) {
        SshAgent agent;
        URL builtInPrivateKey = Main.class.getClassLoader().getResource("karaf.key");
        agent = startAgent(user, builtInPrivateKey, keyFile);
        client.setAgentFactory(new LocalAgentFactory(agent));
        client.getProperties().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, "local");
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
                String[] keyFiles = new String[]{keyFile};
                FileKeyPairProvider fileKeyPairProvider = new FileKeyPairProvider(keyFiles);
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
