package org.apache.karaf.main;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.AccessControlException;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.osgi.framework.launch.Framework;

class ShutdownSocketThread extends Thread {
	Logger LOG = Logger.getLogger(this.getClass().getName());

    /**
	 * 
	 */
	private final String shutdown;
    private Random random = null;
    private ServerSocket shutdownSocket;
	private Framework framework;

    public ShutdownSocketThread(String shutdown, ServerSocket shutdownSocket, Framework framework) {
		this.shutdown = shutdown;
		this.shutdownSocket = shutdownSocket;
		this.framework = framework;
    }

    public void run() {
        try {
            while (true) {
                // Wait for the next connection
                Socket socket = null;
                InputStream stream = null;
                try {
                    socket = shutdownSocket.accept();
                    socket.setSoTimeout(10 * 1000);  // Ten seconds
                    stream = socket.getInputStream();
                } catch (AccessControlException ace) {
                    LOG.log(Level.WARNING, "Karaf shutdown socket: security exception: "
                                       + ace.getMessage(), ace);
                    continue;
                } catch (IOException e) {
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
                    LOG.log(Level.WARNING, "Karaf shutdown socket:  Invalid command '" +
                                       command.toString() + "' received");
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