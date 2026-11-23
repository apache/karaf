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
package org.apache.karaf.services.logging.command;

import java.io.IOException;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The logger levels of the Karaf logging service configuration (<code>org.apache.karaf.logging</code>): the root
 * level is <code>level</code> (or <code>log.level</code>, <code>rootLogger.level</code>), and the level of a logger
 * is <code>logger.&lt;name&gt;</code> (or <code>logger.&lt;name&gt;.level</code>).
 */
final class LoggerLevels {

    static final String CONFIGURATION_PID = "org.apache.karaf.logging";
    static final String ROOT_LOGGER = "ROOT";
    static final String ALL_LOGGER = "ALL";
    static final String DEFAULT_LEVEL = "DEFAULT";
    static final List<String> LEVELS = List.of("OFF", "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "FATAL", DEFAULT_LEVEL);

    // in the order the Karaf logging service reads them
    private static final String[] ROOT_KEYS = {"level", "log.level", "rootLogger.level"};
    private static final String LOGGER_PREFIX = "logger.";
    private static final String LEVEL_SUFFIX = ".level";

    private LoggerLevels() {
    }

    /**
     * @return the ROOT level and the level of the configured loggers, sorted by name.
     */
    static Map<String, String> getLevels(Dictionary<String, ?> config) {
        Map<String, String> levels = new TreeMap<>();
        levels.put(ROOT_LOGGER, getRootLevel(config));
        // logger.<name> takes precedence over logger.<name>.level
        for (boolean suffixed : new boolean[]{true, false}) {
            for (String key : Collections.list(config.keys())) {
                if (!key.startsWith(LOGGER_PREFIX) || key.endsWith(LEVEL_SUFFIX) != suffixed) {
                    continue;
                }
                String name = key.substring(LOGGER_PREFIX.length());
                if (suffixed) {
                    name = name.substring(0, name.length() - LEVEL_SUFFIX.length());
                }
                Object value = config.get(key);
                if (!name.isEmpty() && value != null) {
                    levels.put(name, value.toString().trim());
                }
            }
        }
        return levels;
    }

    /**
     * @return the level of the logger, or of its closest configured parent (by name), or the ROOT level.
     */
    static String getEffectiveLevel(Dictionary<String, ?> config, String logger) {
        Map<String, String> levels = getLevels(config);
        String name = logger;
        while (name != null) {
            String level = levels.get(name);
            if (level != null) {
                return level;
            }
            int idx = name.lastIndexOf('.');
            name = idx > 0 ? name.substring(0, idx) : null;
        }
        return levels.get(ROOT_LOGGER);
    }

    private static String getRootLevel(Dictionary<String, ?> config) {
        for (String key : ROOT_KEYS) {
            Object value = config.get(key);
            if (value != null) {
                return value.toString().trim();
            }
        }
        // same default as the Karaf logging service
        return System.getProperty("karaf.log.level", "INFO");
    }

    /**
     * Set the level of a logger in the configuration properties, updating the keys already used for this logger.
     *
     * @param logger the logger name, or <code>null</code> or <code>ROOT</code> for the root logger.
     * @param level  the level, or <code>DEFAULT</code> to remove the level of the logger.
     */
    static void setLevel(Dictionary<String, Object> config, String logger, String level) {
        String lvl = level != null ? level.trim().toUpperCase(Locale.ENGLISH) : null;
        if (!LEVELS.contains(lvl)) {
            throw new IllegalArgumentException("Invalid log level " + level + ", expected one of " + String.join(", ", LEVELS));
        }
        if (logger == null || ROOT_LOGGER.equalsIgnoreCase(logger)) {
            if (DEFAULT_LEVEL.equals(lvl)) {
                throw new IllegalStateException("Can not unset the ROOT logger");
            }
            set(config, lvl, ROOT_KEYS);
            return;
        }
        // logger.<name>.level is read as the level of <name>: a logger named *.level can only use the suffixed key
        String suffixedKey = LOGGER_PREFIX + logger + LEVEL_SUFFIX;
        String[] keys = logger.endsWith(LEVEL_SUFFIX)
                ? new String[]{suffixedKey}
                : new String[]{LOGGER_PREFIX + logger, suffixedKey};
        if (DEFAULT_LEVEL.equals(lvl)) {
            for (String key : keys) {
                config.remove(key);
            }
        } else {
            set(config, lvl, keys);
        }
    }

    private static void set(Dictionary<String, Object> config, String level, String... keys) {
        boolean updated = false;
        for (String key : keys) {
            if (config.get(key) != null) {
                config.put(key, level);
                updated = true;
            }
        }
        if (!updated) {
            config.put(keys[0], level);
        }
    }

    /**
     * @return the (processed) properties of the configuration, or <code>null</code> if it doesn't exist.
     */
    static Dictionary<String, Object> getProperties(ConfigurationAdmin configAdmin, String pid) throws IOException {
        try {
            Configuration[] configurations = configAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")");
            if (configurations != null && configurations.length > 0) {
                return configurations[0].getProcessedProperties(null);
            }
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    static Map<String, String> getLevels(ConfigurationAdmin configAdmin) throws IOException {
        return getLevels(orEmpty(getProperties(configAdmin, CONFIGURATION_PID)));
    }

    static String getEffectiveLevel(ConfigurationAdmin configAdmin, String logger) throws IOException {
        return getEffectiveLevel(orEmpty(getProperties(configAdmin, CONFIGURATION_PID)), logger);
    }

    static void setLevel(ConfigurationAdmin configAdmin, String logger, String level) throws IOException {
        Configuration configuration = configAdmin.getConfiguration(CONFIGURATION_PID, null);
        // update the raw properties, so that the placeholders are kept
        Dictionary<String, Object> properties = orEmpty(configuration.getProperties());
        setLevel(properties, logger, level);
        try {
            configuration.update(properties);
        } catch (UnsupportedOperationException e) {
            throw new IllegalStateException("The " + CONFIGURATION_PID + " configuration can't be updated", e);
        }
    }

    private static Dictionary<String, Object> orEmpty(Dictionary<String, Object> properties) {
        return properties != null ? properties : new Hashtable<>();
    }

}
