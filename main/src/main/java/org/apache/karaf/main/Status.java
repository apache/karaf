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

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Main class used to check the status of the root Karaf instance.
 */
public class Status {

    private final static String RUNNING = "Running ...";
    private final static String NOT_RUNNING = "Not Running ...";

    /**
     * Checks if the shutdown port is bound. The shutdown port can be configured in config.properties
     * or in the shutdown port file.
     *
     * @param args The arguments to the status main method.
     * @throws Exception If an error occurs while checking the status.
     */
    public static void main(String[] args) throws Exception {
        ConfigProperties config = new ConfigProperties();
        if (config.shutdownPort == 0 && config.portFile != null) {
            try {
                config.shutdownPort = getPortFromShutdownPortFile(config.portFile);
            } catch (FileNotFoundException fnfe) {
                System.out.println(NOT_RUNNING);
                // cause with exit code 3: shutdown port file doesn't exist. The container is not running.
                System.exit(3);
            } catch (IOException ioe) {
                System.out.println(NOT_RUNNING);
                // cause with exit code 4: can't read port file
                System.exit(4);
            }
        }
        if (config.shutdownPort > 0) {
            try (Socket s = new Socket(config.shutdownHost, config.shutdownPort)) {
                if (s.isBound()) {
                    System.out.println(RUNNING);
                    System.exit(0);
                } else {
                    System.out.println(NOT_RUNNING);
                    System.exit(1);
                }
            } catch (ConnectException connectException) {
                System.out.println(NOT_RUNNING);
                System.exit(1);
            }
        } else {
            // using the pid file
            int pid = getPidFromPidFile(config.pidFile);
            org.apache.karaf.jpm.Process process = new ProcessBuilderFactoryImpl().newBuilder().attach(pid);
            if (process.isRunning()) {
                System.out.println(RUNNING + " (pid " + pid + ")");
                System.exit(0);
            } else {
                System.out.println(NOT_RUNNING);
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
