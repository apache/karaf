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
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.karaf.log.core.Level;
import org.apache.karaf.log.core.LogService;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class LogServiceImpl implements LogService, PaxAppender {

    static final String CONFIGURATION_PID = "org.ops4j.pax.logging";

    private final ConfigurationAdmin configAdmin;
    private volatile CircularBuffer buffer;
    private List<PaxAppender> appenders;


    public LogServiceImpl(ConfigurationAdmin configAdmin, int size) {
        this.configAdmin = configAdmin;
        this.appenders = new CopyOnWriteArrayList<>();
        this.buffer = new CircularBuffer(size);
    }

    private LogServiceInternal getDelegate(Dictionary<String, Object> config) {
        if (config.get("log4j.rootLogger") != null) {
            return new LogServiceLog4j1Impl(config);
        }
        else if (config.get("log4j2.rootLogger.level") != null) {
            return new LogServiceLog4j2Impl(config);
        }
        else if (config.get("org.ops4j.pax.logging.log4j2.config.file") != null) {
            String file = config.get("org.ops4j.pax.logging.log4j2.config.file").toString();
            if (file.endsWith(".xml")) {
                return new LogServiceLog4j2XmlImpl(file);
            } else {
                throw new IllegalStateException("Unsupported Log4j2 configuration type: " + file);
            }
        }
        else {
            throw new IllegalStateException("Unrecognized configuration");
        }
    }

    public String getLevel() {
        return getLevel(LogServiceInternal.ROOT_LOGGER).get(LogServiceInternal.ROOT_LOGGER);
    }

    public Map<String, String> getLevel(String logger) {
        Configuration cfg;
        try {
            cfg = configAdmin.getConfiguration(CONFIGURATION_PID, null);
        } catch (IOException e) {
            throw new RuntimeException("Error retrieving Log information from config admin", e);
        }
        if (logger == null) {
            logger = LogServiceInternal.ROOT_LOGGER;
        }
        return getDelegate(cfg.getProcessedProperties(null)).getLevel(logger);
    }

    public void setLevel(String level) {
        setLevel(LogServiceInternal.ROOT_LOGGER, level);
    }

    public void setLevel(String logger, String level) {
        // make sure both uppercase and lowercase levels are supported
        level = level.toUpperCase();
        // check if the level is valid
        Level lvl = Level.valueOf(level);
        // Default logger
        if (logger == null) {
            logger = LogServiceInternal.ROOT_LOGGER;
        }
        // Verify
        if (lvl == Level.DEFAULT && LogServiceInternal.ROOT_LOGGER.equals(logger)) {
            throw new IllegalStateException("Can not unset the ROOT logger");
        }

        // Get config
        Configuration cfg = getConfiguration();
        Dictionary<String, Object> props = cfg.getProcessedProperties(null);
        // Update
        getDelegate(props).setLevel(logger, level);
        // Save
        try {
            cfg.update(props);
        } catch (IOException e) {
            throw new RuntimeException("Error writing log config to config admin", e);
        }
    }

    private boolean checkIfFromRequestedLog(PaxLoggingEvent event, String logger) {
        return (event.getLoggerName().lastIndexOf(logger) >= 0);
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
        return buffer.getElements(buffer.maxSize());
    }

    @Override
    public Iterable<PaxLoggingEvent> getEvents(int maxNum) {
        return buffer.getElements(maxNum);
    }

    @Override
    public void clearEvents() { // just reset the buffer, reduce the number of "write locked" operations in the buffer
        final int size = this.buffer.maxSize();
        this.buffer = new CircularBuffer(size);
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
        this.appenders.add(appender);
    }

    @Override
    public void removeAppender(PaxAppender appender) {
        this.appenders.remove(appender);
    }

    @Override
    public void doAppend(PaxLoggingEvent event) {
        event.getProperties(); // ensure MDC properties are copied
        KarafLogEvent eventCopy = new KarafLogEvent(event);
        this.buffer.add(eventCopy);
        for (PaxAppender appender : appenders) {
            try {
                appender.doAppend(eventCopy);
            } catch (Throwable t) {
                // Ignore
            }
        }
    }

}
