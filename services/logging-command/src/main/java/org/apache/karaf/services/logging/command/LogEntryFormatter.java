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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Dictionary;

import org.osgi.service.log.LogEntry;

/**
 * Formats the log entries as the <code>log:display</code> command of the Apache Karaf distribution, with the
 * <code>pattern</code> of the <code>org.apache.karaf.log</code> configuration. The colors are the ones of the
 * pattern (escape sequences and <code>%h</code>): they are removed with <code>--no-color</code>.
 */
final class LogEntryFormatter {

    static final String CONFIGURATION_PID = "org.apache.karaf.log";
    static final String DEFAULT_PATTERN = "%d{ABSOLUTE} | %-5.5p | %-16.16t | %-32.32c{1} | %-32.32C %4L | %m%n";

    private static final String RESET = "\u001b[0m";

    private final PatternLayout layout;

    /**
     * @param config            the <code>org.apache.karaf.log</code> configuration, or <code>null</code>.
     * @param overriddenPattern the pattern to use instead of the configured one, or <code>null</code>.
     */
    LogEntryFormatter(Dictionary<String, ?> config, String overriddenPattern) {
        Object pattern = overriddenPattern != null ? overriddenPattern : config != null ? config.get("pattern") : null;
        this.layout = new PatternLayout(pattern != null ? pattern.toString() : DEFAULT_PATTERN);
    }

    String format(LogEntry entry, boolean noColor) {
        StringBuilder sb = new StringBuilder();
        layout.format(sb, entry, noColor);
        Throwable exception = entry.getException();
        if (exception != null) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                exception.printStackTrace(pw);
            }
            for (String line : sw.toString().split("\\R")) {
                sb.append(line).append('\n');
            }
        }
        if (!noColor) {
            // don't leave a color of the pattern on
            sb.append(RESET);
        }
        return sb.toString();
    }

}
