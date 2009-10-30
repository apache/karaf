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
package org.slf4j.impl;

import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

/**
 * A simple logger that can be controlled from the ssh client
 */
public class SimpleLogger extends MarkerIgnoringBase {

    public static final int ERROR = 0;
    public static final int WARN = 1;
    public static final int INFO = 2;
    public static final int DEBUG = 3;
    public static final int TRACE = 4;

    private static int level = 0;
    private static long startTime = System.currentTimeMillis();
    public static final String LINE_SEPARATOR = System.getProperty("line.separator");

    public static int getLevel() {
        return level;
    }

    public static void setLevel(int level) {
        SimpleLogger.level = level;
    }

    private String name;

    SimpleLogger(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isTraceEnabled() {
        return isLogEnabled(TRACE);
    }

    public void trace(String msg) {
        log(TRACE, msg);
    }

    public void trace(String format, Object arg) {
        log(TRACE, format, arg);
    }

    public void trace(String format, Object arg1, Object arg2) {
        log(TRACE, format, arg1, arg2);
    }

    public void trace(String format, Object[] argArray) {
        log(TRACE, format, argArray);
    }

    public void trace(String msg, Throwable t) {
        log(TRACE, msg, t);
    }

    public boolean isDebugEnabled() {
        return isLogEnabled(DEBUG);
    }

    public void debug(String msg) {
        log(DEBUG, msg);
    }

    public void debug(String format, Object arg) {
		log(DEBUG, format, arg);
    }

    public void debug(String format, Object arg1, Object arg2) {
        log(DEBUG, format, arg1, arg2);
    }

    public void debug(String format, Object[] argArray) {
		log(DEBUG, format, argArray, null);
    }

    public void debug(String msg, Throwable t) {
        log(DEBUG, msg, t);
    }

    public boolean isInfoEnabled() {
        return isLogEnabled(INFO);
    }

    public void info(String msg) {
		log(INFO, msg);
    }

    public void info(String format, Object arg) {
		log(INFO, format, arg);
    }

    public void info(String format, Object arg1, Object arg2) {
		log(INFO, format, arg1, arg2);
    }

    public void info(String format, Object[] argArray) {
		log(INFO, format, argArray);
    }

    public void info(String msg, Throwable t) {
		log(INFO, msg, t);
    }

    public boolean isWarnEnabled() {
        return isLogEnabled(WARN);
    }

    public void warn(String msg) {
		log(WARN, msg);
    }

    public void warn(String format, Object arg) {
        log(WARN, format, arg);
    }

    public void warn(String format, Object arg1, Object arg2) {
        log(WARN, format, arg1, arg2);
    }

    public void warn(String format, Object[] argArray) {
 		log(WARN, format, argArray);
    }

    public void warn(String msg, Throwable t) {
 		log(WARN, msg, t);
    }

    public boolean isErrorEnabled() {
        return isLogEnabled(ERROR);
    }

    public void error(String msg) {
        log(ERROR, msg);
    }

    public void error(String format, Object arg) {
		log(ERROR, format, arg);
    }

    public void error(String format, Object arg1, Object arg2) {
        log(ERROR, format, arg1, arg2);
    }

    public void error(String format, Object[] argArray) {
		log(ERROR, format, argArray);
    }

    public void error(String msg, Throwable t) {
		log(ERROR, msg, t);
    }

	protected boolean isLogEnabled(int level) {
		return SimpleLogger.level >= level;
	}

    protected void log(int level, String msg) {
        if (isLogEnabled(level)) {
            doLog(level, msg, null);
        }
    }

    protected void log(int level, String format, Object arg) {
        if (isLogEnabled(level)) {
            String msg = MessageFormatter.format(format, arg);
            doLog(level, msg, null);
        }
    }

    protected void log(int level, String format, Object arg1, Object arg2) {
        if (isLogEnabled(level)) {
            String msg = MessageFormatter.format(format, arg1, arg2);
            doLog(level, msg, null);
        }
    }

    protected void log(int level, String format, Object[] args) {
        if (isLogEnabled(level)) {
            String msg = MessageFormatter.format(format, args);
            doLog(level, msg, null);
        }
    }

	protected void log(int level, String msg, Throwable t) {
        if (isLogEnabled(level)) {
            doLog(level, msg, t);
        }
	}

    protected void doLog(int level, String msg, Throwable t) {
        StringBuffer buf = new StringBuffer();
        long millis = System.currentTimeMillis();
        buf.append(millis - startTime);
        buf.append(" [");
        buf.append(Thread.currentThread().getName());
        buf.append("] ");
        switch (level) {
            case TRACE:
                buf.append("TRACE"); break;
            case DEBUG:
                buf.append("DEBUG"); break;
            case INFO:
                buf.append("INFO"); break;
            case WARN:
                buf.append("WARN"); break;
            case ERROR:
                buf.append("ERROR"); break;
        }
        buf.append(" ");
        buf.append(name);
        buf.append(" - ");
        buf.append(msg);
        buf.append(LINE_SEPARATOR);
        System.err.print(buf.toString());
        if (t != null) {
            t.printStackTrace(System.err);
        }
        System.err.flush();
    }
}
