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

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;
import org.osgi.service.log.LogReaderService;

/**
 * Displays the last log entries kept by the {@link LogReaderService}.
 */
@Command(scope = "log", name = "display", description = "Displays log entries.")
@Service
public class DisplayLog implements Action {

    @Option(name = "-n", description = "Number of entries to display", required = false, multiValued = false)
    int entries;

    @Option(name = "-p", description = "Pattern for formatting the output", required = false, multiValued = false)
    String overriddenPattern;

    @Option(name = "--no-color", description = "Disable syntax coloring of log events", required = false, multiValued = false)
    boolean noColor;

    @Option(name = "-l", aliases = { "--level" }, description = "The minimal log level to display", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "TRACE", "DEBUG", "INFO", "WARN", "ERROR", "DEFAULT" })
    String level;

    @Argument(index = 0, name = "logger", description = "Only display the entries of the loggers whose name contains this value", required = false, multiValued = false)
    String logger;

    @Reference
    LogReaderService logReaderService;

    @Reference
    ConfigurationAdmin configAdmin;

    @Override
    public Object execute() throws Exception {
        LogLevel minLevel = getMinLevel(level);
        LogEntryFormatter formatter = createFormatter();
        PrintStream out = System.out;
        for (LogEntry entry : lastEntries(Collections.list(logReaderService.getLog()), entries)) {
            if (accept(entry, minLevel)) {
                out.append(formatter.format(entry, noColor));
            }
        }
        out.println();
        out.flush();
        return null;
    }

    LogEntryFormatter createFormatter() throws IOException {
        return new LogEntryFormatter(LoggerLevels.getProperties(configAdmin, LogEntryFormatter.CONFIGURATION_PID), overriddenPattern);
    }

    /**
     * @param log   the log entries, most recent first (as returned by {@link LogReaderService#getLog()}).
     * @param count the number of entries to keep, or 0 for all of them.
     * @return the <code>count</code> most recent entries, oldest first.
     */
    static List<LogEntry> lastEntries(List<LogEntry> log, int count) {
        List<LogEntry> last = count > 0 && count < log.size() ? log.subList(0, count) : log;
        Collections.reverse(last);
        return last;
    }

    /**
     * @return the minimal level to display, or <code>null</code> to display all the entries.
     */
    static LogLevel getMinLevel(String level) {
        if (level == null) {
            return null;
        }
        switch (level.trim().toUpperCase(Locale.ENGLISH)) {
            case "TRACE":
            case "DEFAULT":
                return null;
            case "DEBUG":
                return LogLevel.DEBUG;
            case "INFO":
                return LogLevel.INFO;
            case "WARN":
            case "WARNING":
                return LogLevel.WARN;
            case "ERROR":
            case "FATAL":
                return LogLevel.ERROR;
            default:
                throw new IllegalArgumentException("Invalid log level " + level + ", expected one of TRACE, DEBUG, INFO, WARN, ERROR, DEFAULT");
        }
    }

    boolean accept(LogEntry entry, LogLevel minLevel) {
        if (minLevel != null && !minLevel.implies(PatternLayout.level(entry))) {
            return false;
        }
        return logger == null || (entry.getLoggerName() != null && entry.getLoggerName().contains(logger));
    }

}
