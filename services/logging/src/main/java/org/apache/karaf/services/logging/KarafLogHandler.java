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

import java.text.MessageFormat;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogLevel;

public class KarafLogHandler extends Handler {

    private static final ThreadLocal<Boolean> IN_HANDLER = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final LogReaderServiceImpl logReaderService;

    public KarafLogHandler(LogReaderServiceImpl logReaderService) {
        this.logReaderService = logReaderService;
    }

    @Override
    public void publish(LogRecord record) {
        if (record == null || IN_HANDLER.get()) {
            return;
        }
        IN_HANDLER.set(Boolean.TRUE);
        try {
            LogLevel logLevel = toLogLevel(record.getLevel());
            // The legacy LogService levels (LOG_ERROR = 1 ... LOG_DEBUG = 4) match the LogLevel ordinals
            int level = logLevel.ordinal();

            Bundle bundle = null;
            ServiceReference<?> serviceReference = null;
            if (record instanceof KarafLogRecord) {
                KarafLogRecord klr = (KarafLogRecord) record;
                bundle = klr.getBundle();
                serviceReference = klr.getServiceReference();
                if (klr.getLogServiceLevel() != null) {
                    level = klr.getLogServiceLevel();
                }
            }

            String message = record.getMessage();
            if (message != null && record.getParameters() != null && record.getParameters().length > 0) {
                try {
                    message = MessageFormat.format(message, record.getParameters());
                } catch (Exception e) {
                    // Fallback to unformatted message
                }
            }

            StackTraceElement location = null;
            if (record.getSourceClassName() != null) {
                location = new StackTraceElement(
                        record.getSourceClassName(),
                        record.getSourceMethodName() != null ? record.getSourceMethodName() : "unknown",
                        null,
                        -1
                );
            }

            LogEntryImpl entry = new LogEntryImpl(
                    bundle,
                    serviceReference,
                    level,
                    logLevel,
                    message,
                    record.getThrown(),
                    record.getMillis(),
                    record.getLoggerName(),
                    logReaderService.nextSequence(),
                    Thread.currentThread().getName(),
                    location
            );

            logReaderService.fire(entry);
        } finally {
            IN_HANDLER.set(Boolean.FALSE);
        }
    }

    static LogLevel toLogLevel(Level julLevel) {
        int value = julLevel.intValue();
        if (value >= AuditLevel.AUDIT.intValue()) {
            return LogLevel.AUDIT;
        } else if (value >= Level.SEVERE.intValue()) {
            return LogLevel.ERROR;
        } else if (value >= Level.WARNING.intValue()) {
            return LogLevel.WARN;
        } else if (value >= Level.INFO.intValue()) {
            return LogLevel.INFO;
        } else if (value >= Level.FINE.intValue()) {
            return LogLevel.DEBUG;
        } else {
            return LogLevel.TRACE;
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
