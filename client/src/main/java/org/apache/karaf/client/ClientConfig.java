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
package org.apache.karaf.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.impl.SimpleLogger;

public class ClientConfig {

    private static final String ROLE_DELIMITER = ",";

    private String host;
    private int port;
    private String user;
    private String password;
    private int level;
    private int retryAttempts;
    private int retryDelay;
    private boolean batch;
    private String file = null;
    private String keyFile = null;
    private String command;

    public ClientConfig(String[] args) throws IOException {
        Properties shellCfg = loadProps(new File(System.getProperty("karaf.etc"), "org.apache.karaf.shell.cfg"));

        host = shellCfg.getProperty("sshHost", "localhost");
        port = Integer.parseInt(shellCfg.getProperty("sshPort", "8101"));
        level = SimpleLogger.WARN;
        retryAttempts = 0;
        retryDelay = 2;
        batch = false;
        file = null;
        user = null;
        password = null;
        StringBuilder commandBuilder = new StringBuilder();

        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-a")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the port");
                        System.exit(1);
                    } else {
                        port = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-h")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the host");
                        System.exit(1);
                    } else {
                        host = args[i];
                    }
                } else if (args[i].equals("-u")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the user");
                        System.exit(1);
                    } else {
                        user = args[i];
                    }
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-r")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the attempts");
                        System.exit(1);
                    } else {
                        retryAttempts = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-d")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the delay in seconds");
                        System.exit(1);
                    } else {
                        retryDelay = Integer.parseInt(args[i]);
                    }
                } else if (args[i].equals("-b")) {
                    batch = true;
                } else if (args[i].equals("-f")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the commands file");
                        System.exit(1);
                    } else {
                        file = args[i];
                    }
                } else if (args[i].equals("-k")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the key file");
                        System.exit(1);
                    } else {
                        keyFile = args[i];
                    }
                } else if (args[i].equals("--help")) {
                    showHelp();
                } else {
                    System.err.println("Unknown option: " + args[i]);
                    System.err.println("Run with --help for usage");
                    System.exit(1);
                }
            } else {
                commandBuilder.append(args[i]);
                commandBuilder.append(' ');
            }
        }
        command = commandBuilder.toString();

        Properties usersCfg = loadProps(new File(System.getProperty("karaf.etc") + "/users.properties"));
        if (!usersCfg.isEmpty()) {
            if (user == null) {
                user = (String) usersCfg.keySet().iterator().next();
            }
            password = (String) usersCfg.getProperty(user);
            if (password != null && password.contains(ROLE_DELIMITER)) {
                password = password.substring(0, password.indexOf(ROLE_DELIMITER));
            }
        }

    }
    
    private static void showHelp() {
        System.out.println("Apache Karaf client");
        System.out.println("  -a [port]     specify the port to connect to");
        System.out.println("  -h [host]     specify the host to connect to");
        System.out.println("  -u [user]     specify the user name");
        System.out.println("  --help        shows this help message");
        System.out.println("  -v            raise verbosity");
        System.out.println("  -r [attempts] retry connection establishment (up to attempts times)");
        System.out.println("  -d [delay]    intra-retry delay (defaults to 2 seconds)");
        System.out.println("  -b            batch mode, specify multiple commands via standard input");
        System.out.println("  -f [file]     read commands from the specified file");
        System.out.println("  -k [keyFile]    specify the private keyFile location when using key login, need have BouncyCastle registered as security provider using this flag");
        System.out.println("  [commands]    commands to run");
        System.out.println("If no commands are specified, the client will be put in an interactive mode");
        System.exit(0);
    }

    private static Properties loadProps(File file) {
        Properties props = new Properties();
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            if (is != null) {
                props.load(is);
            }

        } catch (Exception e) {
                System.err.println("Could not load properties from: " + file + ", Reason: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }
        return props;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public int getLevel() {
        return level;
    }

    public int getRetryAttempts() {
        return retryAttempts;
    }

    public int getRetryDelay() {
        return retryDelay;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public boolean isBatch() {
        return batch;
    }

    public String getFile() {
        return file;
    }

    public String getKeyFile() {
        return keyFile;
    }
}
