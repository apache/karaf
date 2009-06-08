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

import java.io.ByteArrayInputStream;

import org.apache.sshd.ClientChannel;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;

import jline.ConsoleReader;

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
                } else if (args[i].equals("--help")) {
                    System.out.println("Apache Felix Karaf client");
                    System.out.println("  -a [port]     specify the port to connect to");
                    System.out.println("  -h [host]     specify the host to connect to");
                    System.out.println("  -u [user]     specify the user name");
                    System.out.println("  -p [password] specify the password");
                    System.out.println("  --help        shows this help message");
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

        // TODO: implement sending a direct command

        SshClient client = null;
        try {
            client = SshClient.setUpDefaultClient();
            client.start();
            ConnectFuture future = client.connect(host, port);
            future.await();
            ClientSession session = future.getSession();
            session.authPassword(user, password);
            ClientChannel channel;
			if (sb.length() > 0) {
 				channel = session.createChannel("exec");
	            channel.setIn(new ByteArrayInputStream(sb.append("\n").toString().getBytes()));
			} else {
 				channel = session.createChannel("shell");
	            channel.setIn(new ConsoleReader().getInput());
			}
            channel.setOut(System.out);
            channel.setErr(System.err);
            channel.open();
            channel.waitFor(ClientChannel.CLOSED, 0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        } finally {
            try {
                client.stop();
            } catch (Throwable t) { }
        }
        System.exit(0);
    }

}
