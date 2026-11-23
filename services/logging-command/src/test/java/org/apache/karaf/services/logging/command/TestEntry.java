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

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

class TestEntry implements LogEntry {

    // 2026-11-22 10:15:30.123 UTC
    static final long TIME = 1795342530123L;

    LogLevel level = LogLevel.INFO;
    String logger = "org.apache.karaf.test.Foo";
    String message = "hello";
    String thread = "main";
    StackTraceElement location = new StackTraceElement("org.apache.karaf.test.Foo", "bar", "Foo.java", 42);
    Throwable exception;
    Bundle bundle;
    long time = TIME;

    TestEntry() {
    }

    TestEntry(LogLevel level, String logger, String message) {
        this.level = level;
        this.logger = logger;
        this.message = message;
    }

    @Override
    public Bundle getBundle() {
        return bundle;
    }

    @Override
    public ServiceReference<?> getServiceReference() {
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLevel() {
        return level.ordinal();
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
        return level;
    }

    @Override
    public String getLoggerName() {
        return logger;
    }

    @Override
    public long getSequence() {
        return 0;
    }

    @Override
    public String getThreadInfo() {
        return thread;
    }

    @Override
    public StackTraceElement getLocation() {
        return location;
    }

    @Override
    public String toString() {
        return level + " " + logger + ": " + message;
    }
}
