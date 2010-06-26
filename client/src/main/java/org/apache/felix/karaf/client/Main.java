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
package org.apache.felix.karaf.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;

import jline.Terminal;
import org.apache.felix.karaf.shell.console.jline.TerminalFactory;
import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
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
        String password = "karaf";
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
                } else if (args[i].equals("-p")) {
                    password = args[++i];
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-r")) {
                    retryAttempts = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-d")) {
                    retryDelay = Integer.parseInt(args[++i]);
                } else if (args[i].equals("--help")) {
                    System.out.println("Apache Felix Karaf client");
                    System.out.println("  -a [port]     specify the port to connect to");
                    System.out.println("  -h [host]     specify the host to connect to");
                    System.out.println("  -u [user]     specify the user name");
                    System.out.println("  -p [password] specify the password");
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
        try {
            client = SshClient.setUpDefaultClient();
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
            if (!session.authPassword(user, password).await().isSuccess()) {
                throw new Exception("Authentication failure");
            }
            ClientChannel channel;
			if (sb.length() > 0) {
 				channel = session.createChannel("exec");
	            channel.setIn(new ByteArrayInputStream(sb.append("\n").toString().getBytes()));
			} else {
                terminal = new TerminalFactory().getTerminal();
 				channel = session.createChannel("shell");
                channel.setIn(System.in);
                ((ChannelShell) channel).setupSensibleDefaultPty();
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
                    terminal.restoreTerminal();
                }
            } catch (Throwable t) { }
        }
        System.exit(0);
    }

}
