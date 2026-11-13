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
import java.io.IOException;
import java.io.InputStream;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class JulConfig {

    static final long DEFAULT_MAX_SIZE = 10 * 1024 * 1024;
    static final int DEFAULT_MAX_BACKUP_INDEX = 5;

    private static final String LOGGER_PREFIX = "logger.";
    private static final String LEVEL_SUFFIX = ".level";

    // java.util.logging only holds loggers weakly: keep the configured ones so their level is not lost on GC
    private final Map<String, Logger> configuredLoggers = new HashMap<>();
    private Logger rootLogger;
    private Level previousRootLevel;
    private ConsoleHandler consoleHandler;
    private RollingFileHandler fileHandler;
    private KarafLogHandler karafLogHandler;

    public synchronized void init(LogReaderServiceImpl logReaderService) {
        rootLogger = Logger.getLogger("");
        previousRootLevel = rootLogger.getLevel();

        // Remove existing default console handlers from root logger
        for (Handler h : rootLogger.getHandlers()) {
            if (h instanceof ConsoleHandler) {
                rootLogger.removeHandler(h);
            }
        }

        // Attach KarafLogHandler to feed LogReaderService
        karafLogHandler = new KarafLogHandler(logReaderService);
        karafLogHandler.setLevel(Level.ALL);
        rootLogger.addHandler(karafLogHandler);

        apply(loadInitialConfiguration());
    }

    public synchronized void update(Dictionary<String, ?> properties) {
        if (properties == null || rootLogger == null) {
            return;
        }
        Map<String, String> config = new HashMap<>();
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = properties.get(key);
            if (value != null) {
                config.put(key, substitute(value.toString().trim()));
            }
        }
        apply(config);
    }

    public synchronized void close() {
        if (rootLogger == null) {
            return;
        }
        for (Logger logger : configuredLoggers.values()) {
            logger.setLevel(null);
        }
        configuredLoggers.clear();
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
        rootLogger.setLevel(previousRootLevel);
        rootLogger = null;
    }

    private Map<String, String> loadInitialConfiguration() {
        Map<String, String> config = new HashMap<>();
        String etc = System.getProperty("karaf.etc");
        if (etc == null && System.getProperty("karaf.base") != null) {
            etc = System.getProperty("karaf.base") + File.separator + "etc";
        }
        if (etc != null) {
            File karafLoggingCfg = new File(etc, Activator.CONFIGURATION_PID + ".cfg");
            if (karafLoggingCfg.isFile()) {
                Properties props = new Properties();
                try (InputStream is = new FileInputStream(karafLoggingCfg)) {
                    props.load(is);
                } catch (Exception e) {
                    System.err.println("Failed to read " + karafLoggingCfg + ": " + e.getMessage());
                }
                for (String key : props.stringPropertyNames()) {
                    config.put(key, substitute(props.getProperty(key).trim()));
                }
            }
        }
        return config;
    }

    private void apply(Map<String, String> config) {
        String rootLevel = get(config, System.getProperty("karaf.log.level", "INFO"), "level", "log.level", "rootLogger.level");
        rootLogger.setLevel(parseLevel(rootLevel, Level.INFO));

        String console = get(config, System.getProperty("karaf.log.console", "OFF"), "console", "console.level");
        configureConsole(parseLevel(console, Level.OFF));

        configureFile(config);
        configureLoggers(config);
    }

    private void configureConsole(Level level) {
        if (Level.OFF.equals(level)) {
            if (consoleHandler != null) {
                rootLogger.removeHandler(consoleHandler);
                consoleHandler.close();
                consoleHandler = null;
            }
            return;
        }
        if (consoleHandler == null) {
            consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new KarafLogFormatter());
            rootLogger.addHandler(consoleHandler);
        }
        consoleHandler.setLevel(level);
    }

    private void configureFile(Map<String, String> config) {
        String path = get(config, null, "file");
        File logFile = (path != null && !path.isEmpty() ? new File(path) : defaultLogFile()).getAbsoluteFile();
        long maxSize = parseSize(get(config, null, "file.maxSize"), DEFAULT_MAX_SIZE);
        int maxBackupIndex = parseInt(get(config, null, "file.maxBackupIndex"), DEFAULT_MAX_BACKUP_INDEX);

        if (fileHandler != null && fileHandler.getFile().equals(logFile)) {
            fileHandler.setLimits(maxSize, maxBackupIndex);
            return;
        }
        RollingFileHandler handler;
        try {
            handler = new RollingFileHandler(logFile, maxSize, maxBackupIndex);
        } catch (IOException e) {
            System.err.println("Failed to configure Karaf logging file handler for " + logFile + ": " + e.getMessage());
            return;
        }
        // Levels are enforced by the loggers, so that a logger set to DEBUG is written even if the root level is INFO
        handler.setLevel(Level.ALL);
        handler.setFormatter(new KarafLogFormatter());
        rootLogger.addHandler(handler);
        if (fileHandler != null) {
            rootLogger.removeHandler(fileHandler);
            fileHandler.close();
        }
        fileHandler = handler;
    }

    private static File defaultLogFile() {
        String logDir = System.getProperty("karaf.log");
        if (logDir == null) {
            String data = System.getProperty("karaf.data");
            if (data != null) {
                logDir = data + File.separator + "log";
            } else if (new File("target").isDirectory()) {
                logDir = "target/data/log";
            } else {
                logDir = "data/log";
            }
        }
        return new File(logDir, "karaf.log");
    }

    private void configureLoggers(Map<String, String> config) {
        Map<String, Level> levels = new HashMap<>();
        for (Map.Entry<String, String> entry : config.entrySet()) {
            String key = entry.getKey();
            if (!key.startsWith(LOGGER_PREFIX)) {
                continue;
            }
            String loggerName = key.substring(LOGGER_PREFIX.length());
            if (loggerName.endsWith(LEVEL_SUFFIX)) {
                loggerName = loggerName.substring(0, loggerName.length() - LEVEL_SUFFIX.length());
            }
            if (!loggerName.isEmpty()) {
                levels.put(loggerName, parseLevel(entry.getValue(), Level.INFO));
            }
        }
        // Reset the loggers which are no longer configured
        Iterator<Map.Entry<String, Logger>> iterator = configuredLoggers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Logger> entry = iterator.next();
            if (!levels.containsKey(entry.getKey())) {
                entry.getValue().setLevel(null);
                iterator.remove();
            }
        }
        for (Map.Entry<String, Level> entry : levels.entrySet()) {
            configuredLoggers.computeIfAbsent(entry.getKey(), Logger::getLogger).setLevel(entry.getValue());
        }
    }

    private static String get(Map<String, String> config, String defaultValue, String... keys) {
        for (String key : keys) {
            String value = config.get(key);
            if (value != null) {
                return value;
            }
        }
        return defaultValue;
    }

    /**
     * Replace the <code>${name}</code> and <code>${name:-default}</code> placeholders with system properties.
     * Unresolved placeholders are left as is.
     */
    static String substitute(String value) {
        if (value == null || !value.contains("${")) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        int pos = 0;
        int start;
        while ((start = value.indexOf("${", pos)) >= 0) {
            int end = value.indexOf('}', start + 2);
            if (end < 0) {
                break;
            }
            String name = value.substring(start + 2, end);
            String defaultValue = null;
            int idx = name.indexOf(":-");
            if (idx >= 0) {
                defaultValue = name.substring(idx + 2);
                name = name.substring(0, idx);
            }
            String resolved = System.getProperty(name, defaultValue);
            sb.append(value, pos, start).append(resolved != null ? resolved : value.substring(start, end + 1));
            pos = end + 1;
        }
        return sb.append(value.substring(pos)).toString();
    }

    static long parseSize(String size, long defaultSize) {
        if (size == null || size.trim().isEmpty()) {
            return defaultSize;
        }
        String s = size.trim().toUpperCase();
        if (s.endsWith("B")) {
            s = s.substring(0, s.length() - 1);
        }
        long multiplier = 1;
        if (s.endsWith("K")) {
            multiplier = 1024;
        } else if (s.endsWith("M")) {
            multiplier = 1024 * 1024;
        } else if (s.endsWith("G")) {
            multiplier = 1024 * 1024 * 1024;
        }
        if (multiplier > 1) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Long.parseLong(s.trim()) * multiplier;
        } catch (NumberFormatException e) {
            return defaultSize;
        }
    }

    private static int parseInt(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
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
            case "DEFAULT":
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
