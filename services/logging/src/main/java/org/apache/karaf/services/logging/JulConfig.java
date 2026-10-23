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
package org.apache.karaf.services.logging;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JulConfig {

    private ConsoleHandler consoleHandler;
    private FileHandler fileHandler;
    private KarafLogHandler karafLogHandler;

    public void init(LogReaderServiceImpl logReaderService) {
        Logger rootLogger = Logger.getLogger("");

        // Check if etc/org.apache.karaf.logging.cfg exists
        Properties initialProps = new Properties();
        String etc = System.getProperty("karaf.etc");
        if (etc == null && System.getProperty("karaf.base") != null) {
            etc = System.getProperty("karaf.base") + File.separator + "etc";
        }
        if (etc != null) {
            File karafLoggingCfg = new File(etc, Activator.CONFIGURATION_PID + ".cfg");
            if (karafLoggingCfg.isFile()) {
                try (InputStream is = new FileInputStream(karafLoggingCfg)) {
                    initialProps.load(is);
                } catch (Exception e) {
                    System.err.println("Failed to read " + karafLoggingCfg + ": " + e.getMessage());
                }
            }
        }

        // Determine default root level
        String rootLevelStr = initialProps.getProperty("level",
                initialProps.getProperty("log.level",
                System.getProperty("karaf.log.level", "INFO")));
        Level rootLevel = parseLevel(rootLevelStr, Level.INFO);
        rootLogger.setLevel(rootLevel);

        // Remove existing default console handlers from root logger
        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                rootLogger.removeHandler(h);
            }
        }

        // Configure ConsoleHandler
        String consoleProp = initialProps.getProperty("console",
                initialProps.getProperty("console.level",
                System.getProperty("karaf.log.console", "INFO")));
        if (!"OFF".equalsIgnoreCase(consoleProp) && !"false".equalsIgnoreCase(consoleProp)) {
            Level consoleLevel = parseLevel(consoleProp, rootLevel);
            consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(consoleLevel);
            consoleHandler.setFormatter(new KarafLogFormatter());
            rootLogger.addHandler(consoleHandler);
        }

        // Configure FileHandler
        String customFilePath = initialProps.getProperty("file");
        File logFile;
        if (customFilePath != null && !customFilePath.trim().isEmpty()) {
            logFile = new File(customFilePath.trim());
            if (logFile.getParentFile() != null) {
                logFile.getParentFile().mkdirs();
            }
        } else {
            String logDir = System.getProperty("karaf.data");
            if (logDir == null) {
                logDir = System.getProperty("karaf.log");
            }
            if (logDir == null) {
                File target = new File("target");
                if (target.isDirectory()) {
                    logDir = "target/data";
                } else {
                    logDir = "data";
                }
            }
            File logDirectory = new File(logDir, "log");
            logDirectory.mkdirs();
            logFile = new File(logDirectory, "karaf.log");
        }

        try {
            int maxBackup = 0;
            String maxBackupStr = initialProps.getProperty("file.maxBackupIndex");
            if (maxBackupStr != null) {
                try {
                    maxBackup = Integer.parseInt(maxBackupStr.trim());
                } catch (NumberFormatException ignored) {
                }
            }
            int maxSize = 10 * 1024 * 1024;
            String maxSizeStr = initialProps.getProperty("file.maxSize");
            if (maxSizeStr != null) {
                try {
                    String s = maxSizeStr.trim().toUpperCase();
                    if (s.endsWith("M")) {
                        maxSize = Integer.parseInt(s.substring(0, s.length() - 1)) * 1024 * 1024;
                    } else if (s.endsWith("K")) {
                        maxSize = Integer.parseInt(s.substring(0, s.length() - 1)) * 1024;
                    } else {
                        maxSize = Integer.parseInt(s);
                    }
                } catch (Exception ignored) {
                }
            }

            if (maxBackup > 1) {
                String pattern = logFile.getAbsolutePath() + ".%g";
                fileHandler = new FileHandler(pattern, maxSize, maxBackup, true);
            } else {
                fileHandler = new FileHandler(logFile.getAbsolutePath(), true);
            }
            fileHandler.setLevel(rootLevel);
            fileHandler.setFormatter(new KarafLogFormatter());
            rootLogger.addHandler(fileHandler);
        } catch (Exception e) {
            System.err.println("Failed to configure Karaf logging file handler: " + e.getMessage());
        }

        // Attach KarafLogHandler to feed LogReaderService
        karafLogHandler = new KarafLogHandler(logReaderService);
        karafLogHandler.setLevel(Level.ALL);
        rootLogger.addHandler(karafLogHandler);

        // Apply any logger-specific levels from initialProps
        if (!initialProps.isEmpty()) {
            @SuppressWarnings({"unchecked", "rawtypes"})
            Dictionary<String, ?> dict = (Dictionary) initialProps;
            update(dict);
        }
    }

    public void update(Dictionary<String, ?> properties) {
        if (properties == null) {
            return;
        }

        Logger rootLogger = Logger.getLogger("");

        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object val = properties.get(key);
            if (val == null) {
                continue;
            }
            String value = val.toString().trim();

            if ("level".equalsIgnoreCase(key) || "log.level".equalsIgnoreCase(key) || "rootLogger.level".equalsIgnoreCase(key)) {
                Level level = parseLevel(value, Level.INFO);
                rootLogger.setLevel(level);
                if (fileHandler != null) {
                    fileHandler.setLevel(level);
                }
            } else if ("console".equalsIgnoreCase(key) || "console.level".equalsIgnoreCase(key)) {
                if ("OFF".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                    if (consoleHandler != null) {
                        consoleHandler.setLevel(Level.OFF);
                    }
                } else {
                    Level consoleLevel = parseLevel(value, Level.INFO);
                    if (consoleHandler == null) {
                        consoleHandler = new ConsoleHandler();
                        consoleHandler.setFormatter(new KarafLogFormatter());
                        rootLogger.addHandler(consoleHandler);
                    }
                    consoleHandler.setLevel(consoleLevel);
                }
            } else if (key.startsWith("logger.")) {
                String loggerName = key.substring("logger.".length());
                if (loggerName.endsWith(".level")) {
                    loggerName = loggerName.substring(0, loggerName.length() - ".level".length());
                }
                Logger.getLogger(loggerName).setLevel(parseLevel(value, Level.INFO));
            }
        }
    }

    public void close() {
        Logger rootLogger = Logger.getLogger("");
        if (karafLogHandler != null) {
            rootLogger.removeHandler(karafLogHandler);
            karafLogHandler.close();
            karafLogHandler = null;
        }
        if (consoleHandler != null) {
            rootLogger.removeHandler(consoleHandler);
            consoleHandler.close();
            consoleHandler = null;
        }
        if (fileHandler != null) {
            rootLogger.removeHandler(fileHandler);
            fileHandler.close();
            fileHandler = null;
        }
    }

    public static Level parseLevel(String levelName, Level defaultLevel) {
        if (levelName == null || levelName.trim().isEmpty()) {
            return defaultLevel;
        }
        String normalized = levelName.trim().toUpperCase();
        switch (normalized) {
            case "TRACE":
                return Level.FINEST;
            case "DEBUG":
                return Level.FINE;
            case "INFO":
                return Level.INFO;
            case "WARN":
            case "WARNING":
                return Level.WARNING;
            case "ERROR":
            case "SEVERE":
            case "FATAL":
                return Level.SEVERE;
            case "OFF":
                return Level.OFF;
            case "ALL":
                return Level.ALL;
            default:
                try {
                    return Level.parse(normalized);
                } catch (IllegalArgumentException e) {
                    return defaultLevel;
                }
        }
    }
}
