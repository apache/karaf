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
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

/**
 * Main class used to stop the root Karaf instance
 */
public class Stop {

    public static void main(String[] args) throws Exception {
        File karafHome = Utils.getKarafHome();
        File karafBase = Utils.getKarafDirectory(Main.PROP_KARAF_BASE, Main.ENV_KARAF_BASE, karafHome, false, true);
        File karafData = Utils.getKarafDirectory(Main.PROP_KARAF_DATA, Main.ENV_KARAF_DATA, new File(karafBase.getPath(), "data"), true, true);

        System.setProperty(Main.PROP_KARAF_HOME, karafHome.getPath());
        System.setProperty(Main.PROP_KARAF_BASE, karafBase.getPath());
        System.setProperty(Main.PROP_KARAF_DATA, karafData.getPath());

        // Load system properties.
        Main.loadSystemProperties(karafBase);

        File file = new File(new File(karafBase, "etc"), Main.CONFIG_PROPERTIES_FILE_NAME);
        URL configPropURL = file.toURI().toURL();
        Properties props = Main.loadPropertiesFile(configPropURL, false);
        Main.copySystemProperties(props);

        // Perform variable substitution for system properties.
        for (Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String name = (String) e.nextElement();
            props.setProperty(name, Main.substVars(props.getProperty(name), name, null, props));
        }

        int port = Integer.parseInt(props.getProperty(Main.KARAF_SHUTDOWN_PORT, "0"));
        String host = props.getProperty(Main.KARAF_SHUTDOWN_HOST, "localhost");
        String portFile = props.getProperty(Main.KARAF_SHUTDOWN_PORT_FILE);
        String shutdown = props.getProperty(Main.KARAF_SHUTDOWN_COMMAND, Main.DEFAULT_SHUTDOWN_COMMAND);
        if (port == 0 && portFile != null) {
            try {
                BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(portFile)));
                String portStr = r.readLine();
                port = Integer.parseInt(portStr);
                r.close();
            } catch (FileNotFoundException fnfe) {
                System.err.println(portFile + " shutdown port file doesn't exist. The container is not running.");
                System.exit(3);
            } catch (IOException ioe) {
                System.err.println("Can't read the " + portFile + " shutdown port file: " + ioe.getMessage());
                System.exit(4);
            }
        }
        if (port > 0) {
            Socket s = null;
            try {
                s = new Socket(host, port);
                s.getOutputStream().write(shutdown.getBytes());
            } catch (ConnectException connectException) {
                System.err.println("Can't connect to the container. The container is not running.");
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
}
