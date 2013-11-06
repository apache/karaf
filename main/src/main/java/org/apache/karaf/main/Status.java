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

import java.io.*;
import java.net.ConnectException;
import java.net.Socket;

/**
 * Main class used to check the status of the root Karaf instance.
 */
public class Status {

    /**
     * Checks if the shutdown port is bound. The shutdown port can be configured in config.properties
     * or in the shutdown port file.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ConfigProperties config = new ConfigProperties();
        if (config.shutdownPort == 0 && config.portFile != null) {
            try {
                config.shutdownPort = getPortFromShutdownPortFile(config.portFile);
            } catch (FileNotFoundException fnfe) {
                System.err.println(config.portFile + " port file doesn't exist. The container is not running.");
                System.exit(3);
            } catch (IOException ioe) {
                System.err.println("Can't read " + config.portFile + " port file: " + ioe.getMessage());
                System.exit(4);
            }
        }
        if (config.shutdownPort > 0) {
            Socket s = null;
            try {
                s = new Socket(config.shutdownHost, config.shutdownPort);
                if (s.isBound()) {
                    System.out.println("Running ...");
                    System.exit(0);
                } else {
                    System.out.println("Not Running ...");
                    System.exit(1);
                }
            } catch (ConnectException connectException) {
                System.out.println("Not Running ...");
                System.exit(1);
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        } else {
            System.err.println("Unable to find port...");
            System.exit(2);
        }
    }

    private static int getPortFromShutdownPortFile(String portFile) throws FileNotFoundException, IOException {
        int port;
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
        String portStr = r.readLine();
        port = Integer.parseInt(portStr);
        r.close();
        return port;
    }

}
