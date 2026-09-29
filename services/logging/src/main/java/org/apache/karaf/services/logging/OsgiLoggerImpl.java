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

import java.util.logging.Level;

import org.osgi.framework.Bundle;
import org.osgi.service.log.FormatterLogger;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerConsumer;
import org.slf4j.helpers.FormattingTuple;
import org.slf4j.helpers.MessageFormatter;

public class OsgiLoggerImpl implements Logger, FormatterLogger {

    private final String name;
    private final java.util.logging.Logger julLogger;
    private final Bundle bundle;
    private final boolean isFormatter;

    public OsgiLoggerImpl(String name, java.util.logging.Logger julLogger, Bundle bundle, boolean isFormatter) {
        this.name = name;
        this.julLogger = julLogger;
        this.bundle = bundle;
        this.isFormatter = isFormatter;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isTraceEnabled() {
        return julLogger.isLoggable(Level.FINEST);
    }

    @Override
    public void trace(String message) {
        log(Level.FINEST, message, null, null);
    }

    @Override
    public void trace(String message, Object arg) {
        log(Level.FINEST, message, new Object[]{arg}, null);
    }

    @Override
    public void trace(String message, Object arg1, Object arg2) {
        log(Level.FINEST, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void trace(String message, Object... args) {
        log(Level.FINEST, message, args, null);
    }

    @Override
    public <E extends Exception> void trace(LoggerConsumer<E> consumer) throws E {
        if (isTraceEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return julLogger.isLoggable(Level.FINE);
    }

    @Override
    public void debug(String message) {
        log(Level.FINE, message, null, null);
    }

    @Override
    public void debug(String message, Object arg) {
        log(Level.FINE, message, new Object[]{arg}, null);
    }

    @Override
    public void debug(String message, Object arg1, Object arg2) {
        log(Level.FINE, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void debug(String message, Object... args) {
        log(Level.FINE, message, args, null);
    }

    @Override
    public <E extends Exception> void debug(LoggerConsumer<E> consumer) throws E {
        if (isDebugEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return julLogger.isLoggable(Level.INFO);
    }

    @Override
    public void info(String message) {
        log(Level.INFO, message, null, null);
    }

    @Override
    public void info(String message, Object arg) {
        log(Level.INFO, message, new Object[]{arg}, null);
    }

    @Override
    public void info(String message, Object arg1, Object arg2) {
        log(Level.INFO, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void info(String message, Object... args) {
        log(Level.INFO, message, args, null);
    }

    @Override
    public <E extends Exception> void info(LoggerConsumer<E> consumer) throws E {
        if (isInfoEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return julLogger.isLoggable(Level.WARNING);
    }

    @Override
    public void warn(String message) {
        log(Level.WARNING, message, null, null);
    }

    @Override
    public void warn(String message, Object arg) {
        log(Level.WARNING, message, new Object[]{arg}, null);
    }

    @Override
    public void warn(String message, Object arg1, Object arg2) {
        log(Level.WARNING, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void warn(String message, Object... args) {
        log(Level.WARNING, message, args, null);
    }

    @Override
    public <E extends Exception> void warn(LoggerConsumer<E> consumer) throws E {
        if (isWarnEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return julLogger.isLoggable(Level.SEVERE);
    }

    @Override
    public void error(String message) {
        log(Level.SEVERE, message, null, null);
    }

    @Override
    public void error(String message, Object arg) {
        log(Level.SEVERE, message, new Object[]{arg}, null);
    }

    @Override
    public void error(String message, Object arg1, Object arg2) {
        log(Level.SEVERE, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void error(String message, Object... args) {
        log(Level.SEVERE, message, args, null);
    }

    @Override
    public <E extends Exception> void error(LoggerConsumer<E> consumer) throws E {
        if (isErrorEnabled()) {
            consumer.accept(this);
        }
    }

    @Override
    public void audit(String message) {
        log(Level.INFO, message, null, null);
    }

    @Override
    public void audit(String message, Object arg) {
        log(Level.INFO, message, new Object[]{arg}, null);
    }

    @Override
    public void audit(String message, Object arg1, Object arg2) {
        log(Level.INFO, message, new Object[]{arg1, arg2}, null);
    }

    @Override
    public void audit(String message, Object... args) {
        log(Level.INFO, message, args, null);
    }

    private void log(Level level, String message, Object[] args, Throwable exception) {
        if (!julLogger.isLoggable(level)) {
            return;
        }
        String formattedMessage = message;
        Throwable thrown = exception;

        if (args != null && args.length > 0) {
            if (isFormatter) {
                // Check if last arg is a Throwable
                Object lastArg = args[args.length - 1];
                if (thrown == null && lastArg instanceof Throwable) {
                    thrown = (Throwable) lastArg;
                    Object[] subArgs = new Object[args.length - 1];
                    System.arraycopy(args, 0, subArgs, 0, subArgs.length);
                    args = subArgs;
                }
                try {
                    formattedMessage = String.format(message, args);
                } catch (Exception e) {
                    formattedMessage = message;
                }
            } else {
                FormattingTuple ft = MessageFormatter.arrayFormat(message, args);
                formattedMessage = ft.getMessage();
                if (thrown == null) {
                    thrown = ft.getThrowable();
                }
            }
        }

        KarafLogRecord record = new KarafLogRecord(level, formattedMessage, bundle, null);
        record.setLoggerName(name);
        if (thrown != null) {
            record.setThrown(thrown);
        }
        julLogger.log(record);
    }
}
