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

import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.log.core.Level;
import org.apache.karaf.log.core.LogService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class LogServiceImpl implements LogService {
    static final String CONFIGURATION_PID = "org.ops4j.pax.logging";
    static final String ROOT_LOGGER_PREFIX = "log4j.rootLogger";
    static final String LOGGER_PREFIX = "log4j.logger.";
    static final String ROOT_LOGGER = "ROOT";
    
    private final ConfigurationAdmin configAdmin;
    private final LruList events;

    public LogServiceImpl(ConfigurationAdmin configAdmin, LruList events) {
        this.configAdmin = configAdmin;
        this.events = events;
    }

    public Level getLevel() {
        return getLevel(null);
    }

    public Level getLevel(String logger) {
        Configuration cfg;
        try {
            cfg = configAdmin.getConfiguration(CONFIGURATION_PID, null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving Log information from config admin", e);
        }
        @SuppressWarnings("rawtypes")
        Dictionary props = cfg.getProperties();

        if (ROOT_LOGGER.equalsIgnoreCase(logger)) {
            logger = null;
        }

        String val;
        for (; ; ) {
            String prop;
            if (logger == null) {
                prop = ROOT_LOGGER_PREFIX;
            } else {
                prop = LOGGER_PREFIX + logger;
            }
            val = (String) props.get(prop);
            val = getLevelFromProperty(val);
            if (val != null || logger == null) {
                break;
            }
            int idx = logger.lastIndexOf('.');
            if (idx < 0) {
                logger = null;
            } else {
                logger = logger.substring(0, idx);
            }
        }
        return Level.valueOf(val);
    }

    public void setLevel(Level level) {
        setLevel(null, level);
    }

    @SuppressWarnings("unchecked")
    public void setLevel(String logger, Level logLevel) {
        if (ROOT_LOGGER.equalsIgnoreCase(logger)) {
            logger = null;
        }
        
        if (logLevel == Level.DEFAULT && logger == null) {
            throw new RuntimeException("Can not unset the ROOT logger");
        }

        Configuration cfg = getConfiguration();
        @SuppressWarnings("rawtypes")
        Dictionary props = cfg.getProperties();
        
        String level = logLevel.toString();

        String val;
        String prop;
        if (logger == null) {
            prop = ROOT_LOGGER_PREFIX;
        } else {
            prop = LOGGER_PREFIX + logger;
        }
        val = (String) props.get(prop);
        if (level.equals("DEFAULT")) {
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
            props.remove(prop);
        } else {
            props.put(prop, val);
        }
        try {
            cfg.update(props);
        } catch (IOException e) {
            throw new RuntimeException("Error writing log config to config admin", e);
        }
    }

    private boolean checkIfFromRequestedLog(PaxLoggingEvent event, String logger) {
        return (event.getLoggerName().lastIndexOf(logger) >= 0) ? true : false;
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

    private Configuration getConfiguration() {
        try {
            return configAdmin.getConfiguration(CONFIGURATION_PID, null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving Log information from config admin", e);
        }
    }

    @Override
    public Iterable<PaxLoggingEvent> getEvents() {
        return events.getElements();
    }

    @Override
    public Iterable<PaxLoggingEvent> getEvents(int maxNum) {
        return events.getElements(maxNum);
    }

    @Override
    public void clearEvents() {
        events.clear();
    }
    
    @Override
    public PaxLoggingEvent getLastException(String logger) {
        PaxLoggingEvent throwableEvent = null;
        Iterable<PaxLoggingEvent> le = getEvents();
        for (PaxLoggingEvent event : le) {
            // if this is an exception, and the log is the same as the requested log,
            // then save this exception and continue iterating from oldest to newest
            if ((event.getThrowableStrRep() != null)
                    &&(logger != null)
                    &&(checkIfFromRequestedLog(event, logger))) {
                throwableEvent = event;
              // Do not break, as we iterate from the oldest to the newest event
            } else if ((event.getThrowableStrRep() != null)&&(logger == null)) {
                // now check if there has been no log passed in, and if this is an exception
                // then save this exception and continue iterating from oldest to newest
                throwableEvent = event;             
            }
        }

        return throwableEvent;
    }

    @Override
    public void addAppender(PaxAppender appender) {
        events.addAppender(appender);
    }

    @Override
    public void removeAppender(PaxAppender appender) {
        events.removeAppender(appender);
    }

    @Override
    public String getLevelSt() {
        return getLevel().toString();
    }

    @Override
    public String getLevelSt(String logger) {
        return getLevel(logger).toString();
    }

    @Override
    public void setLevelSt(String level) {
        setLevel(convertToLevel(level));
    }

    @Override
    public void setLevelSt(String logger, String level) {
        setLevel(logger, convertToLevel(level));
    }

    public Level convertToLevel(String level) {
        level = level.toUpperCase();
        Level res = Level.valueOf(level);
        if (res == null) {
            throw new IllegalArgumentException("level must be set to TRACE, DEBUG, INFO, WARN or ERROR (or DEFAULT to unset it)");
        }
        return res;
    }

}
