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

import org.apache.karaf.jpm.impl.ProcessBuilderFactoryImpl;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Main class used to stop the root Karaf instance
 */
public class Stop {

    /**
     * Send the shutdown command to the running Karaf instance. Uses either a shut down port configured in config.properties or
     * the port from the shutdown port file.
     *
     * @param args The arguments to the stop main method.
     * @throws Exception In case of failure while stopping.
     */
    public static void main(String[] args) throws Exception {
        ConfigProperties config = new ConfigProperties();
        if (config.shutdownPort == 0 && config.portFile != null) {
            try {
                config.shutdownPort = getPortFromShutdownPortFile(config.portFile);
            } catch (FileNotFoundException fnfe) {
                System.err.println(config.portFile + " shutdown port file doesn't exist. The container is not running.");
                System.exit(3);
            } catch (IOException ioe) {
                System.err.println("Can't read " + config.portFile + " shutdown port file: " + ioe.getMessage());
                System.exit(4);
            }
        }
        if (config.shutdownPort > 0) {
            try (Socket s = new Socket(config.shutdownHost, config.shutdownPort)) {
                s.getOutputStream().write(config.shutdownCommand.getBytes());
                System.exit(0);
            } catch (ConnectException connectException) {
                System.err.println("Can't connect to the container. The container is not running.");
                System.exit(1);
            }
        } else {
            // using the pid file
            int pid = getPidFromPidFile(config.pidFile);
            org.apache.karaf.jpm.Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(pid);
            if (process.isRunning()) {
                process.destroy();
                System.exit(0);
            } else {
                System.out.println("Not Running ...");
                System.exit(1);
            }
        }

    }

    private static int getPortFromShutdownPortFile(String portFile) throws IOException {
        int port;
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
        String portStr = r.readLine();
        port = Integer.parseInt(portStr);
        r.close();
        return port;
    }

    private static int getPidFromPidFile(String pidFile) throws IOException {
        int pid;
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(pidFile)));
        String pidString = r.readLine();
        pid = Integer.parseInt(pidString);
        r.close();
        return pid;
    }

}
