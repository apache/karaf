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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientConfig {

    private static final String ROLE_DELIMITER = ",";
    private static final String GROUP_PREFIX = "_g_:";

    private String host;
    private int port;
    private String user;
    private String password;
    private int level;
    private int retryAttempts;
    private int retryDelay;
    private long idleTimeout;
    private boolean batch;
    private String file = null;
    private String keyFile = null;
    private String command;
    private boolean interactiveMode = false;
    private boolean inputPassword = false;

    public ClientConfig(String[] args) throws IOException {
        Properties shellCfg = loadProps(new File(System.getProperty("karaf.etc"), "org.apache.karaf.shell.cfg"), null);
        Properties customCfg = loadProps(new File(System.getProperty("karaf.etc"), "custom.properties"), null);
        
        host = shellCfg.getProperty("sshHost", "localhost");
        host = expandEnvVars(host);
        String portString = shellCfg.getProperty("sshPort", "8101");
        portString = expandEnvVars(portString);
        
        // if sshHost of sshPort properties contain a reference to another property (coming from 
        // , we try to use the custom.properties value
        if (host.contains("${")) {
            host = replaceVariable(host, "localhost", customCfg);
        }
        if (host.contains("0.0.0.0")) {
            host = "localhost";
        }
        if (portString.contains("${")) {
            portString = replaceVariable(portString, "8101", customCfg);
        }
        port = Integer.parseInt(portString);
        level = Integer.parseInt(shellCfg.getProperty("logLevel", "0"));
        retryAttempts = 0;
        retryDelay = 2;
        idleTimeout = Long.parseLong(shellCfg.getProperty("sshIdleTimeout", "1800000"));
        batch = false;
        file = null;
        user = null;
        password = null;
        StringBuilder commandBuilder = new StringBuilder();
        boolean endOfOptionsMarkerReached = false;
        
        for (int i = 0; i < args.length; i++) {
            if (!endOfOptionsMarkerReached && args[i].charAt(0) == '-') {
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
                        interactiveMode = true;
                        password = null;//get chance to input the password with interactive way
                    }
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-l")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the log level");
                        System.exit(1);
                    } else {
                        int levelValue = Integer.parseInt(args[i]);
                        if (levelValue < 0 || levelValue > 4) {
                            System.err.println("log level can only be 0, 1, 2, 3, or 4");
                            System.exit(1);
                        } else {
                            level = levelValue;
                        }
                    }
                } else if (args[i].equals("-r")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the attempts");
                        System.exit(1);
                    } else {
                        retryAttempts = Integer.parseInt(args[i]);
                    }
                    
                } else if (args[i].equals("-p")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the password");
                        System.exit(1);
                    } else {
                        password = args[i];
                        interactiveMode = false;
                        inputPassword = true;
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
                } else if (args[i].equals("-t")) {
                    if (args.length <= ++i) {
                        System.err.println("miss the idle timeout");
                        System.exit(1);
                    } else {
                        idleTimeout = Long.parseLong(args[i]);
                    }
                } else if (args[i].equals("--help")) {
                    showHelp();
                } else if (args[i].equals("--")) {
                    endOfOptionsMarkerReached = true;
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

        Map<String, String> usersCfg = new LinkedHashMap<>();
        File userPropertiesFile = new File(System.getProperty("karaf.etc") + "/users.properties");
        if (userPropertiesFile.exists()) {
            loadProps(new File(System.getProperty("karaf.etc") + "/users.properties"), usersCfg);
            if (!usersCfg.isEmpty()) {
                Set<String> users = new LinkedHashSet<>();
                for (String user : usersCfg.keySet()) {
                    if (!user.startsWith(GROUP_PREFIX)) {
                        users.add(user);
                    }
                }
                if (user == null) {
                    if (users.iterator().hasNext()) {
                        user = users.iterator().next();
                    }
                }
                if (interactiveMode && !inputPassword) {
                    password = null;
                } else if (!inputPassword) {
                    password = usersCfg.get(user);
                    if (password != null && password.contains(ROLE_DELIMITER)) {
                        password = password.substring(0, password.indexOf(ROLE_DELIMITER));
                    }
                }
            }
        }

    }
    
    private static void showHelp() {
        System.out.println("Apache Karaf client");
        System.out.println("  -a [port]     specify the port to connect to");
        System.out.println("  -h [host]     specify the host to connect to");
        System.out.println("  -u [user]     specify the user name");
        System.out.println("  -p [password] specify the password (optional, if not provided, the password is prompted)");
        System.out.println("  --help        shows this help message");
        System.out.println("  -v            raise verbosity");
        System.out.println("  -l            set client logging level. Set to 0 for ERROR logging and up to 4 for TRACE");
        System.out.println("  -r [attempts] retry connection establishment (up to attempts times)");
        System.out.println("  -d [delay]    intra-retry delay (defaults to 2 seconds)");
        System.out.println("  -b            batch mode, specify multiple commands via standard input");
        System.out.println("  -f [file]     read commands from the specified file");
        System.out.println("  -k [keyFile]  specify the private keyFile location when using key login, need have BouncyCastle registered as security provider using this flag");
        System.out.println("  -t [timeout]  define the client idle timeout");
        System.out.println("  [commands] [--]   commands to run");
        System.out.println("If no commands are specified, the client will be put in an interactive mode");
        System.exit(0);
    }

    // tries a very basic variable substitution
    private static String replaceVariable(String input, String defaultValue, Properties customCfg) {
        try {
            int indexOfDollar = input.indexOf('$');
            int indexOfClosingBrace = input.indexOf('}', indexOfDollar + 1);
            String varName = input.substring(indexOfDollar + 2, indexOfClosingBrace);
            String varValue = customCfg.getProperty(varName, defaultValue);
            return input.replace("${" + varName + "}", varValue);
        } catch (Exception e) {
            return input;
        }
    }

    private static Properties loadProps(File file, final Map<String, String> additionalStorage) {
        Properties props = new Properties() {
            @Override
            public synchronized Object put(Object key, Object value) {
                if (additionalStorage != null) {
                    additionalStorage.put((String) key, (String) value);
                }
                return super.put(key, value);
            }
        };
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            props.load(is);

        } catch (Exception e) {
                System.err.println("Warning: could not load properties from: " + file + ", Reason: " + e.getMessage());
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

    
    private static String expandEnvVars(String text) {
        Map<String, String> envMap = System.getenv();
        String pattern = "\\$\\{([A-Za-z0-9]+)\\}";
        Pattern expr = Pattern.compile(pattern);
        Matcher matcher = expr.matcher(text);
        while (matcher.find()) {
            String envValue = envMap.get(matcher.group(1).toUpperCase());
            if (envValue != null) {
                envValue = envValue.replace("\\", "\\\\");
                Pattern subexpr = Pattern.compile(Pattern.quote(matcher.group(0)));
                text = subexpr.matcher(text).replaceAll(envValue);
            }
        }
        return text;
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

    public void setUser(String user) {
        this.user = user;
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

    public long getIdleTimeout() {
        return idleTimeout;
    }

}
