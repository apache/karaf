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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.LogService;
import org.osgi.service.log.Logger;

public class LogServiceImpl implements LogService {

    private final Bundle bundle;
    private final Map<String, Logger> loggers = new ConcurrentHashMap<>();

    public LogServiceImpl(Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public void log(int level, String message) {
        log(null, level, message, null);
    }

    @Override
    public void log(int level, String message, Throwable exception) {
        log(null, level, message, exception);
    }

    @Override
    public void log(ServiceReference<?> sr, int level, String message) {
        log(sr, level, message, null);
    }

    @Override
    public void log(ServiceReference<?> sr, int level, String message, Throwable exception) {
        Level julLevel = toJulLevel(level);
        String loggerName = getLoggerName(sr);
        java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(loggerName);
        if (julLogger.isLoggable(julLevel)) {
            KarafLogRecord record = new KarafLogRecord(julLevel, message, bundle != null ? bundle : (sr != null ? sr.getBundle() : null), sr);
            record.setLoggerName(loggerName);
            if (exception != null) {
                record.setThrown(exception);
            }
            julLogger.log(record);
        }
    }

    @Override
    public Logger getLogger(String name) {
        return getLogger(name, Logger.class);
    }

    @Override
    public Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName(), Logger.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L extends Logger> L getLogger(String name, Class<L> loggerType) {
        return getLogger(bundle, name, loggerType);
    }

    @Override
    public <L extends Logger> L getLogger(Class<?> clazz, Class<L> loggerType) {
        return getLogger(bundle, clazz.getName(), loggerType);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <L extends Logger> L getLogger(Bundle b, String name, Class<L> loggerType) {
        String key = (b != null ? b.getBundleId() : -1) + ":" + name + ":" + loggerType.getName();
        return (L) loggers.computeIfAbsent(key, k -> {
            boolean isFormatter = FormatterLogger.class.isAssignableFrom(loggerType);
            java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(name);
            return new OsgiLoggerImpl(name, julLogger, b, isFormatter);
        });
    }

    private Level toJulLevel(int osgiLevel) {
        switch (osgiLevel) {
            case LOG_ERROR:
                return Level.SEVERE;
            case LOG_WARNING:
                return Level.WARNING;
            case LOG_INFO:
                return Level.INFO;
            case LOG_DEBUG:
                return Level.FINE;
            default:
                return Level.FINEST;
        }
    }

    private String getLoggerName(ServiceReference<?> sr) {
        if (sr != null && sr.getBundle() != null && sr.getBundle().getSymbolicName() != null) {
            return sr.getBundle().getSymbolicName();
        }
        if (bundle != null && bundle.getSymbolicName() != null) {
            return bundle.getSymbolicName();
        }
        return "org.osgi.service.log.LogService";
    }
}
