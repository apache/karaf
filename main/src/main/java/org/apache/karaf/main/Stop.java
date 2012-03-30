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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Properties;

import org.apache.karaf.main.util.Utils;

/**
 * Main class used to stop the root Karaf instance
 */
public class Stop {

    /**
     * Sends the shutdown command to the running karaf instance. Uses either a shut down port configured in config.properties or
     * the port from the shutdown port file.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        File karafHome = Utils.getKarafHome(Stop.class, Main.PROP_KARAF_HOME, Main.ENV_KARAF_HOME);
        File karafBase = Utils.getKarafDirectory(Main.PROP_KARAF_BASE, Main.ENV_KARAF_BASE, karafHome, false, true);
        File karafData = Utils.getKarafDirectory(Main.PROP_KARAF_DATA, Main.ENV_KARAF_DATA, new File(karafBase.getPath(), "data"), true, true);

        System.setProperty(Main.PROP_KARAF_HOME, karafHome.getPath());
        System.setProperty(Main.PROP_KARAF_BASE, karafBase.getPath());
        System.setProperty(Main.PROP_KARAF_DATA, karafData.getPath());

        // Load system properties.
        PropertiesLoader.loadSystemProperties(karafBase);

        Properties props = PropertiesLoader.loadConfigProperties(karafBase);

        int port = Integer.parseInt(props.getProperty(Main.KARAF_SHUTDOWN_PORT, "0"));
        String host = props.getProperty(Main.KARAF_SHUTDOWN_HOST, "localhost");
        String portFile = props.getProperty(Main.KARAF_SHUTDOWN_PORT_FILE);
        String shutdown = props.getProperty(Main.KARAF_SHUTDOWN_COMMAND, Main.DEFAULT_SHUTDOWN_COMMAND);
        if (port == 0 && portFile != null) {
            port = getPortFromShutdownPortFile(portFile);
        }
        if (port > 0) {
            Socket s = new Socket(host, port);
            s.getOutputStream().write(shutdown.getBytes());
            s.close();
        } else {
            System.err.println("Unable to find port...");
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
