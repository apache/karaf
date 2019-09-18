/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.AccessControlException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.launch.Framework;

class ShutdownSocketThread extends Thread implements AutoCloseable {

	private final static Logger LOG = Logger.getLogger(ShutdownSocketThread.class.getName());

	private final String shutdown;
    private Random random = null;
    private ServerSocket shutdownSocket;
	private Framework framework;
	private boolean closing;

    public ShutdownSocketThread(String shutdown, ServerSocket shutdownSocket, Framework framework) {
        super("Karaf Shutdown Socket Thread");
        setDaemon(true);
		this.shutdown = shutdown;
		this.shutdownSocket = shutdownSocket;
		this.framework = framework;
    }

    @Override
    public void close() throws Exception {
        closing = true;
        shutdownSocket.close();
    }

    public void run() {
        try {
            LOG.info("Shutdown socket thread is listening on " + shutdownSocket.getInetAddress().getHostAddress() + ":" + shutdownSocket.getLocalPort());
            while (true) {
                // Wait for the next connection
                Socket socket = null;
                InputStream stream = null;
                long acceptStartTime = System.currentTimeMillis();
                try {
                    socket = shutdownSocket.accept();
                    socket.setSoTimeout(10 * 1000);  // Ten seconds
                    stream = socket.getInputStream();
                } catch (SocketTimeoutException ste) {
                    // This should never happen but bug 3325 suggests that it does
                    LOG.log(Level.WARNING, "Karaf shutdown socket: "
                                       + "The socket listening for the shutdown command experienced "
                                       + "an unexpected timeout "
                                       + "[" + (System.currentTimeMillis() - acceptStartTime) + "] milliseconds "
                                       + "after the call to accept(). Is this an instance of bug 3325?", ste);
                    continue;
                } catch (AccessControlException ace) {
                    LOG.log(Level.WARNING, "Karaf shutdown socket: security exception: "
                                       + ace.getMessage(), ace);
                    continue;
                } catch (IOException e) {
                    if (closing) {
                        return;
                    }
                    LOG.log(Level.SEVERE, "Karaf shutdown socket: accept: ", e);
                    System.exit(1);
                }

                // Read a set of characters from the socket
                StringBuilder command = new StringBuilder();
                int expected = 1024; // Cut off to avoid DoS attack
                while (expected < shutdown.length()) {
                    if (random == null) {
                        random = new Random();
                    }
                    expected += (random.nextInt() % 1024);
                }
                while (expected > 0) {
                    int ch;
                    try {
                        ch = stream.read();
                    } catch (IOException e) {
                        LOG.log(Level.WARNING, "Karaf shutdown socket:  read: ", e);
                        ch = -1;
                    }
                    if (ch < 32) {  // Control character or EOF terminates loop
                        break;
                    }
                    command.append((char) ch);
                    expected--;
                }

                // Close the socket now that we are done with it
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }

                // Match against our command string
                boolean match = command.toString().equals(shutdown);
                if (match) {
                    LOG.log(Level.INFO, "Karaf shutdown socket: received shutdown command. Stopping framework...");
                    framework.stop();
                    break;
                } else {
                    if (!command.toString().isEmpty()) {
                        LOG.log(Level.WARNING, "Karaf shutdown socket:  Invalid command received");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                shutdownSocket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

}
