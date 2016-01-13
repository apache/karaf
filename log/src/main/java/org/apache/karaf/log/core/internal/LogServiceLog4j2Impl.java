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

import org.apache.karaf.log.core.Level;

public class LogServiceLog4j2Impl implements LogServiceInternal {

    static final String ROOT_LOGGER_LEVEL = "log4j2.rootLogger.level";
    static final String LOGGERS = "log4j2.loggers";
    static final String LOGGER_PREFIX = "log4j2.logger.";
    static final String NAME_SUFFIX = ".name";
    static final String LEVEL_SUFFIX = ".level";

    private final Dictionary<String, Object> config;

    public LogServiceLog4j2Impl(Dictionary<String, Object> config) {
        this.config = config;
    }

    public Map<String, String> getLevel(String logger) {
        Map<String, String> loggers = new TreeMap<>();

        String root = (String) config.get(ROOT_LOGGER_LEVEL);
        loggers.put(ROOT_LOGGER, root);
        if (ROOT_LOGGER.equals(logger)) {
            return loggers;
        }

        String ids = (String) config.get(LOGGERS);
        if (ids != null) {
            for (String id : ids.split(",")) {
                id = id.trim();
                if (!id.equalsIgnoreCase(ROOT_LOGGER)) {
                    String name = (String) config.get(name(id));
                    String level = (String) config.get(level(id));
                    if (name != null && level != null) {
                        loggers.put(name, level);
                    }
                }
            }
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

    public void setLevel(String logger, String level) {
        if (logger == null || LogServiceInternal.ROOT_LOGGER.equalsIgnoreCase(logger)) {
            config.put(ROOT_LOGGER_LEVEL, level);
        } else {
            Map<String, String> names = new HashMap<>();
            String ids = (String) config.get(LOGGERS);
            if (ids != null) {
                for (String id : ids.split(",")) {
                    id = id.trim();
                    if (!id.equalsIgnoreCase(ROOT_LOGGER)) {
                        String name = (String) config.get(name(id));
                        if (name != null) {
                            names.put(name, id);
                        }
                    }
                }
            }

            if (Level.isDefault(level)) {
                if (names.containsKey(logger)) {
                    config.remove(level(names.get(logger)));
                }
            }
            else {
                if (names.containsKey(logger)) {
                    config.put(level(names.get(logger)), level);
                }
                else {
                    String id = logger.toLowerCase().replace('.', '_').replace('$', '_');
                    config.put(name(id), logger);
                    if (ids != null) {
                        config.put(LOGGERS, ids + "," + id);
                    } else {
                        config.put(LOGGERS, id);
                    }
                    config.put(level(id), level);
                }
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
