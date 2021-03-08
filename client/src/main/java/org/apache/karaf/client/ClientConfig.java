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

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.properties.TypedProperties;
import org.apache.karaf.util.config.PropertiesLoader;

import java.io.File;
import java.util.*;

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

    private TypedProperties configuration;

    public ClientConfig(String[] args) throws Exception {
        File karafEtc = new File(System.getProperty("karaf.etc"));
        PropertiesLoader.loadSystemProperties(new File(karafEtc, "system.properties"));
        Properties configProps = PropertiesLoader.loadConfigProperties(new File(karafEtc, "config.properties"));
        configuration = loadProps(new File(karafEtc, "org.apache.karaf.shell.cfg"), configProps);

        host = getString("sshHost", "localhost");
        if (host.contains("0.0.0.0")) {
            host = "localhost";
        }
        port = getInt("sshPort", 8101);
        level = getInt("logLevel", 0);
        retryAttempts = 0;
        retryDelay = 2;
        idleTimeout = getLong("sshIdleTimeout", 1800000L);
        batch = false;
        file = null;
        user = null;
        password = null;
        StringBuilder commandBuilder = new StringBuilder();
        boolean endOfOptionsMarkerReached = false;
        
        for (int i = 0; i < args.length; i++) {
            if (!endOfOptionsMarkerReached && args[i].charAt(0) == '-') {
                switch (args[i]) {
                    case "-a":
                        if (args.length <= ++i) {
                            System.err.println("miss the port");
                            System.exit(1);
                        } else {
                            port = Integer.parseInt(args[i]);
                        }
                        break;
                    case "-h":
                        if (args.length <= ++i) {
                            System.err.println("miss the host");
                            System.exit(1);
                        } else {
                            host = args[i];
                        }
                        break;
                    case "-u":
                        if (args.length <= ++i) {
                            System.err.println("miss the user");
                            System.exit(1);
                        } else {
                            user = args[i];
                            interactiveMode = true;
                            password = null;//get chance to input the password with interactive way
                        }
                        break;
                    case "-v":
                        level++;
                        break;
                    case "-l":
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
                        break;
                    case "-r":
                        if (args.length <= ++i) {
                            System.err.println("miss the attempts");
                            System.exit(1);
                        } else {
                            retryAttempts = Integer.parseInt(args[i]);
                        }

                        break;
                    case "-p":
                        if (args.length <= ++i) {
                            System.err.println("miss the password");
                            System.exit(1);
                        } else {
                            password = args[i];
                            interactiveMode = false;
                            inputPassword = true;
                        }
                        break;
                    case "-d":
                        if (args.length <= ++i) {
                            System.err.println("miss the delay in seconds");
                            System.exit(1);
                        } else {
                            retryDelay = Integer.parseInt(args[i]);
                        }
                        break;
                    case "-b":
                        batch = true;
                        break;
                    case "-f":
                        if (args.length <= ++i) {
                            System.err.println("miss the commands file");
                            System.exit(1);
                        } else {
                            file = args[i];
                        }
                        break;
                    case "-k":
                        if (args.length <= ++i) {
                            System.err.println("miss the key file");
                            System.exit(1);
                        } else {
                            keyFile = args[i];
                        }
                        break;
                    case "-t":
                        if (args.length <= ++i) {
                            System.err.println("miss the idle timeout");
                            System.exit(1);
                        } else {
                            idleTimeout = Long.parseLong(args[i]);
                        }
                        break;
                    case "--help":
                        showHelp();
                        break;
                    case "--":
                        endOfOptionsMarkerReached = true;
                        break;
                    default:
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

        File userPropertiesFile = new File(karafEtc,"users.properties");
        if (userPropertiesFile.exists()) {
	        Map<String, String> usersCfg = PropertiesLoader.loadPropertiesFile(userPropertiesFile.toURI().toURL(), false);
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
        System.out.println("  -t [timeout]  define the client idle timeout (in milliseconds)");
        System.out.println("  [commands] [--]   commands to run");
        System.out.println("If no commands are specified, the client will be put in an interactive mode");
        System.exit(0);
    }

    private static TypedProperties loadProps(File file, Properties configProperties) {
        // TypedProperties props = new TypedProperties((name, key, value) -> context.getProperty(value));
        TypedProperties props = new TypedProperties();
        try {
            props.load(file);
        } catch (Exception e) {
            System.err.println("Warning: could not load properties from: " + file + ": " + e);
        }
        // interpolation
        // 0. configProperties
        for (String key : props.keySet()) {
            Object value = props.get(key);
            if (configProperties.get(value) != null) {
                props.put(key, configProperties.get(value));
            }
        }
        // 1. check "implicit" system property
        String pid = "org.apache.karaf.shell";
        for (String key : props.keySet()) {
            String env = (pid + "." + key).toUpperCase().replaceAll("\\.", "_");
            String sys = pid + "." + key;
            if (System.getenv(env) != null) {
                String value = InterpolationHelper.substVars(System.getenv(env), null,null, convertDictionaryToMap(props));
                if (props.get(key) != null && (props.get(key) instanceof Number)) {
                    props.put(key, Integer.parseInt(value));
                } else {
                    props.put(key, value);
                }
            } else if (System.getProperty(sys) != null) {
                String value = InterpolationHelper.substVars(System.getProperty(sys), null, null, convertDictionaryToMap(props));
                if (props.get(key) != null && (props.get(key) instanceof Number)) {
                    props.put(key, Integer.parseInt(value));
                } else {
                    props.put(key, value);
                }
            }
        }
        // 2. check ${env:*}
        for (String key : props.keySet()) {
            String value = ((String) props.get(key));
            if (value.startsWith("${env:")) {
                String env = value.substring("${env:".length() + 1);
                if (env.lastIndexOf(":") != -1) {
                    env = value.substring(0, env.lastIndexOf(":"));
                }
                if (env.lastIndexOf("}") != -1) {
                    env = value.substring(0, env.lastIndexOf("}"));
                }
                props.put(key, System.getenv(env));
            }
        }
        // 3. check ${prop:*}
        for (String key : props.keySet()) {
            String value = (String) props.get(key);
            if (value.startsWith("${prop:")) {
                String prop = value.substring("${prop:".length() + 1);
                if (prop.lastIndexOf(":") != -1) {
                    prop = value.substring(0, prop.lastIndexOf(":"));
                }
                if (prop.lastIndexOf("}") != -1) {
                    prop = value.substring(0, prop.lastIndexOf("}"));
                }
                props.put(key, System.getProperty(prop));
            }
        }
        return props;
    }

    protected int getInt(String key, int def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                try {
                    return Integer.parseInt(val.toString());
                } catch (Exception e) {
                    System.err.println("Invalid value for " + key + ", using default " + def);
                    return def;
                }
            }
        }
        return def;
    }

    protected long getLong(String key, long def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val != null) {
                try {
                    return Long.parseLong(val.toString());
                } catch (Exception e) {
                    System.err.println("Invalid value for " + key + ", using default " + def);
                    return def;
                }
            }
        }
        return def;
    }

    protected String getString(String key, String def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
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

    private static Map<String, String> convertDictionaryToMap(TypedProperties dictionary) {
        Map<String, String> converted = new HashMap<>();
        Set<String> keys = dictionary.keySet();
        for (String key : keys) {
            converted.put(key, dictionary.get(key).toString());
        }
        return converted;
    }

}
