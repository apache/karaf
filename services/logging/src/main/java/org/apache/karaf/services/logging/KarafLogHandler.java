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
import org.osgi.service.log.LogService;

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
            int level;
            LogLevel logLevel;
            Level julLevel = record.getLevel();
            if (julLevel.intValue() >= Level.SEVERE.intValue()) {
                level = LogService.LOG_ERROR;
                logLevel = LogLevel.ERROR;
            } else if (julLevel.intValue() >= Level.WARNING.intValue()) {
                level = LogService.LOG_WARNING;
                logLevel = LogLevel.WARN;
            } else if (julLevel.intValue() >= Level.INFO.intValue()) {
                level = LogService.LOG_INFO;
                logLevel = LogLevel.INFO;
            } else if (julLevel.intValue() >= Level.FINE.intValue()) {
                level = LogService.LOG_DEBUG;
                logLevel = LogLevel.DEBUG;
            } else {
                level = LogService.LOG_DEBUG;
                logLevel = LogLevel.TRACE;
            }

            Bundle bundle = null;
            ServiceReference<?> serviceReference = null;
            if (record instanceof KarafLogRecord) {
                KarafLogRecord klr = (KarafLogRecord) record;
                bundle = klr.getBundle();
                serviceReference = klr.getServiceReference();
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

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
}
