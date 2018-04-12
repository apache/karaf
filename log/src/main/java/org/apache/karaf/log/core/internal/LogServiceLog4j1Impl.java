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

import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import org.apache.karaf.log.core.Level;

public class LogServiceLog4j1Impl implements LogServiceInternal {

    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String LOGGER_PREFIX = "log4j.logger.";

    private final Dictionary<String, Object> config;

    public LogServiceLog4j1Impl(Dictionary<String, Object> config) {
        this.config = config;
    }

    public Map<String, String> getLevel(String logger) {
        Map<String, String> loggers = new TreeMap<>();

        if (ROOT_LOGGER.equalsIgnoreCase(logger)) {
            logger = null;
        }
        if (ALL_LOGGER.equalsIgnoreCase(logger)) {
            String root = getLevelFromProperty((String) config.get(ROOT_LOGGER_PREFIX));
            loggers.put(ROOT_LOGGER, root);
            for (Enumeration<String> e = config.keys(); e.hasMoreElements(); ) {
                String prop = e.nextElement();
                if (prop.startsWith(LOGGER_PREFIX)) {
                    String val = getLevelFromProperty((String) config.get(prop));
                    loggers.put(prop.substring(LOGGER_PREFIX.length()), val);
                }
            }
            return loggers;
        }

        String l = logger;
        String val;
        for (;;) {
            String prop;
            if (l == null) {
                prop = ROOT_LOGGER_PREFIX;
            } else {
                prop = LOGGER_PREFIX + l;
            }
            val = (String) config.get(prop);
            val = getLevelFromProperty(val);
            if (val != null || l == null) {
                break;
            }
            int idx = l.lastIndexOf('.');
            if (idx < 0) {
                l = null;
            } else {
                l = l.substring(0, idx);
            }
        }

        if (logger == null)
            logger = ROOT_LOGGER;

        loggers.put(logger, val);

        return loggers;
    }

    public void setLevel(String logger, String level) {
        String val;
        String prop;
        if (logger == null || LogServiceInternal.ROOT_LOGGER.equalsIgnoreCase(logger)) {
            prop = ROOT_LOGGER_PREFIX;
        } else {
            prop = LOGGER_PREFIX + logger;
        }

        val = (String) config.get(prop);
        if (Level.isDefault(level)) {
            if (val != null) {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = null;
                } else {
                    val = val.substring(idx);
                }
            }
        } else {
            if (val == null) {
                val = level;
            } else {
                val = val.trim();
                int idx = val.indexOf(",");
                if (idx < 0) {
                    val = level;
                } else {
                    val = level + val.substring(idx);
                }
            }
        }
        if (val == null) {
            config.remove(prop);
        } else {
            config.put(prop, val);
        }
    }

    private String getLevelFromProperty(String prop) {
        if (prop == null) {
            return null;
        } else {
            String val = prop.trim();
            int idx = val.indexOf(",");
            if (idx == 0) {
                val = null;
            } else if (idx > 0) {
                val = val.substring(0, idx);
            }
            return val;
        }
    }

}
