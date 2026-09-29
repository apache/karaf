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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

public class LogEntryImpl implements LogEntry {

    private final Bundle bundle;
    private final ServiceReference<?> serviceReference;
    private final int level;
    private final LogLevel logLevel;
    private final String message;
    private final Throwable exception;
    private final long time;
    private final String loggerName;
    private final long sequence;
    private final String threadInfo;
    private final StackTraceElement location;

    public LogEntryImpl(Bundle bundle, ServiceReference<?> serviceReference, int level, LogLevel logLevel,
                        String message, Throwable exception, long time, String loggerName,
                        long sequence, String threadInfo, StackTraceElement location) {
        this.bundle = bundle;
        this.serviceReference = serviceReference;
        this.level = level;
        this.logLevel = logLevel;
        this.message = message;
        this.exception = exception;
        this.time = time;
        this.loggerName = loggerName;
        this.sequence = sequence;
        this.threadInfo = threadInfo;
        this.location = location;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public ServiceReference<?> getServiceReference() {
        return serviceReference;
    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public Throwable getException() {
        return exception;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public LogLevel getLogLevel() {
        return logLevel;
    }

    @Override
    public String getLoggerName() {
        return loggerName;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public String getThreadInfo() {
        return threadInfo;
    }

    @Override
    public StackTraceElement getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return "LogEntry[" + time + " " + logLevel + " " + loggerName + ": " + message + "]";
    }
}
