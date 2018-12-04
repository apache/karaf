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
package org.apache.karaf.log.core.internal;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.collections.DictionaryAsMap;
import org.apache.karaf.log.core.Level;

public class LogServiceLog4j2Impl implements LogServiceInternal {

    static final String ROOT_LOGGER_LEVEL = "log4j2.rootLogger.level";
    static final String LOGGER_PREFIX = "log4j2.logger.";
    static final String NAME_SUFFIX = ".name";
    static final String LEVEL_SUFFIX = ".level";

    private final Map<String, Object> config;
    private Pattern namePattern;
    private Pattern levelPattern;

    public LogServiceLog4j2Impl(Dictionary<String, Object> config) {
        this.config = new DictionaryAsMap<>(config);
        namePattern = Pattern.compile("log4j2\\.logger\\.([a-zA-Z0-9_]+)\\.name");
        levelPattern = Pattern.compile("log4j2\\.logger\\.([a-zA-Z0-9_]+)\\.level");
    }

    public Map<String, String> getLevel(String logger) {
        Map<String, String> loggers = new TreeMap<>();

        String root = (String) config.get(ROOT_LOGGER_LEVEL);
        loggers.put(ROOT_LOGGER, root);
        if (ROOT_LOGGER.equals(logger)) {
            return loggers;
        }

        Map<String, String> names = new HashMap<>();
        Map<String, String> levels = new HashMap<>();
        for (String key : config.keySet()) {
            String loggerName = getMatching(namePattern, key);
            if (loggerName != null) {
                names.put(loggerName, config.get(key).toString());
            }
            
            loggerName = getMatching(levelPattern, key);
            if (loggerName != null) {
                levels.put(loggerName, config.get(key).toString());
            }
        }
        for (Map.Entry<String, String> e : names.entrySet()) {
            loggers.put(e.getValue(), levels.get(e.getKey()));
        }
        if (ALL_LOGGER.equalsIgnoreCase(logger)) {
            return loggers;
        }

        String l = logger;
        String val;
        for (;;) {
            val = loggers.get(l != null ? l : ROOT_LOGGER);
            if (val != null || l == null) {
                return Collections.singletonMap(logger, val);
            }
            int idx = l.lastIndexOf('.');
            if (idx < 0) {
                l = null;
            } else {
                l = l.substring(0, idx);
            }
        }
    }

    private String getMatching(Pattern pattern, String key) {
        Matcher matcher = pattern.matcher(key);
        return (matcher.matches()) ? matcher.group(1) : null;
    }

    public void setLevel(String logger, String level) {
        if (logger == null || LogServiceInternal.ROOT_LOGGER.equalsIgnoreCase(logger)) {
            config.put(ROOT_LOGGER_LEVEL, level);
        } else {
            String loggerKey = null;
            for (String key : config.keySet()) {
                Matcher matcher = Pattern.compile("\\Q" + LOGGER_PREFIX + "\\E([a-zA-Z_]+)\\Q" + NAME_SUFFIX + "\\E").matcher(key);
                if (matcher.matches()) {
                    String name = config.get(key).toString();
                    if (name.matches(logger)) {
                        loggerKey = matcher.group(1);
                        break;
                    }
                }
            }

            if (loggerKey != null) {
                if (Level.isDefault(level)) {
                    config.remove(level(loggerKey));
                } else {
                    config.put(level(loggerKey), level);
                }
            } else {
                loggerKey = logger.replace('.', '_').toLowerCase();
                config.put(name(loggerKey), logger);
                config.put(level(loggerKey), level);
            }
        }
    }

    private String name(String logger) {
        return LOGGER_PREFIX + logger + NAME_SUFFIX;
    }

    private String level(String logger) {
        return LOGGER_PREFIX + logger + LEVEL_SUFFIX;
    }

}
