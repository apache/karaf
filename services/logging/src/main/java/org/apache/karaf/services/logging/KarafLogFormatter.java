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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class KarafLogFormatter extends Formatter {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    @Override
    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        Instant instant = Instant.ofEpochMilli(record.getMillis());
        sb.append(DATE_FORMATTER.format(instant)).append(" | ");
        sb.append(String.format("%-5s", record.getLevel().getName())).append(" | ");
        sb.append("[").append(Thread.currentThread().getName()).append("] | ");
        sb.append(record.getLoggerName() != null ? record.getLoggerName() : "").append(" | ");
        sb.append(formatMessage(record)).append(System.lineSeparator());
        if (record.getThrown() != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            record.getThrown().printStackTrace(pw);
            sb.append(sw);
        }
        return sb.toString();
    }
}
