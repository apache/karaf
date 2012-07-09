package org.apache.karaf.client;

import org.slf4j.impl.SimpleLogger;

public class ClientConfig {
    
    private String host;
    private int port;
    private String user;
    private int level;
    private int retryAttempts;
    private int retryDelay;
    private String command;

    public ClientConfig(String[] args) {
        host = "localhost";
        port = 8101;
        user = "karaf";
        level = SimpleLogger.WARN;
        retryAttempts = 0;
        retryDelay = 2;
        StringBuilder commandBuilder = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (args[i].charAt(0) == '-') {
                if (args[i].equals("-a")) {
                    port = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-h")) {
                    host = args[++i];
                } else if (args[i].equals("-u")) {
                    user = args[++i];
                } else if (args[i].equals("-v")) {
                    level++;
                } else if (args[i].equals("-r")) {
                    retryAttempts = Integer.parseInt(args[++i]);
                } else if (args[i].equals("-d")) {
                    retryDelay = Integer.parseInt(args[++i]);
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
        System.out.println("  [commands]    commands to run");
        System.out.println("If no commands are specified, the client will be put in an interactive mode");
        System.exit(0);
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
    
    
}
