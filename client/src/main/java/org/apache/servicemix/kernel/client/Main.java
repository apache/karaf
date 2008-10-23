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
package org.apache.servicemix.kernel.client;

import java.net.URI;
import java.util.List;
import java.util.LinkedList;
import java.io.InputStreamReader;
import java.io.BufferedReader;

import org.apache.geronimo.gshell.remote.client.RemoteExecuteException;
import org.apache.geronimo.gshell.remote.client.RshClient;
import org.apache.geronimo.gshell.remote.client.handler.EchoHandler;
import org.apache.geronimo.gshell.remote.client.handler.ClientMessageHandler;
import org.apache.geronimo.gshell.whisper.transport.TransportException;
import org.apache.geronimo.gshell.whisper.transport.TransportFactory;
import org.apache.geronimo.gshell.whisper.transport.TransportFactoryLocator;
import org.apache.geronimo.gshell.whisper.transport.tcp.TcpTransportFactory;
import org.apache.geronimo.gshell.whisper.stream.StreamFeeder;
import org.apache.geronimo.gshell.notification.ExitNotification;
import org.apache.geronimo.gshell.security.crypto.CryptoContextImpl;
import org.apache.geronimo.gshell.security.crypto.CryptoContext;

/**
 * A very simple
 */
public class Main {

    public static void main(String[] args) throws Exception {
        URI address = new URI("tcp://127.0.0.1:8101/");
        String user = "smx";
        String password = "smx";
        StringBuilder sb = new StringBuilder();

        boolean options = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-a")) {
                    address = new URI(args[++i]);
                } else if (args[i].equals("-u")) {
                    user = args[++i];
                } else if (args[i].equals("-p")) {
                    password = args[++i];
                } else if (args[i].equals("--help")) {
                    System.out.println("Apache ServiceMix Kernel client");
                    System.out.println("  -a [address]  specify the URL to connect to");
                    System.out.println("  -u [user]     specify the user name");
                    System.out.println("  -p [password] specify the password");
                    System.out.println("  --help        shows this help message");
                    System.out.println("  [commands]    commands to run");
                    System.out.println("If no commands are specified, the client will be put in an interactive mode");
                } else {
                    System.err.println("Unknown option: " + args[i]);
                    System.err.println("Run with --help for usage");
                    System.exit(1);
                }
            } else {
                sb.append(args[i]);
                sb.append(' ');
                options = false;
            }
        }
        RshClient client = null;
        try {
            CryptoContext context = new CryptoContextImpl();
            List<ClientMessageHandler> handlers = new LinkedList<ClientMessageHandler>();
            handlers.add(new EchoHandler());
            client = new RshClient(context, new Locator(), handlers) {
                protected void onSessionClosed() {
                    System.exit(2);
                }
            };

            client.connect(address, new URI("tcp://0.0.0.0:0"));
            client.login(user, password);
            StreamFeeder outputFeeder = new StreamFeeder(client.getInputStream(), System.out);
            outputFeeder.createThread().start();
            client.openShell();
            System.out.println("Connected");

            String commandLine = sb.toString().trim();
            if (commandLine.length() > 0) {
                client.execute(commandLine);
            } else {
                BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                for (;;) {
                    System.out.print("> ");
                    String s = r.readLine().trim();
                    if (s.length() > 0) {
                        try {
                            client.execute(s);
                        } catch (RemoteExecuteException e) {
                            String name = e.getCause().getClass().getName();
                            name = name.substring(name.lastIndexOf('.') + 1);
                            System.err.println(name + ": " + e.getCause().getMessage());
                        }
                    }
                }
            }
        } catch (ExitNotification e) {
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        } finally {
            try {
                client.closeShell();
                client.close();
            } catch (Throwable t) { }
        }
        System.exit(0);
    }

    private static class Locator implements TransportFactoryLocator {
        TcpTransportFactory factory = new TcpTransportFactory();

        public TransportFactory locate(URI arg0) throws TransportException {
            return factory;
        }

    }
}
