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

import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

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
            } catch (NoSuchFileException e) {
                System.err.println(config.portFile + " shutdown port file doesn't exist. The container is not running.");
                System.exit(3);
            } catch (IOException e) {
                System.err.println("Can't read " + config.portFile + " shutdown port file: " + e.getMessage());
                System.exit(4);
            }
        }
        if (config.shutdownPort > 0) {
            try (Socket s = new Socket(config.shutdownHost, config.shutdownPort)) {
                s.getOutputStream().write(config.shutdownCommand.getBytes());
                s.getOutputStream().write('\n');
                s.getOutputStream().flush();
            } catch (ConnectException connectException) {
                System.err.println("Can't connect to the container. The container is not running.");
                System.exit(1);
            }
            System.exit(0);
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
        try (var reader = Files.newBufferedReader(Path.of(portFile))) {
            return Integer.parseInt(reader.readLine());
        }
    }

    private static int getPidFromPidFile(String pidFile) throws IOException {
        try (var reader = Files.newBufferedReader(Path.of(pidFile))) {
            return Integer.parseInt(reader.readLine());
        }
    }

}
