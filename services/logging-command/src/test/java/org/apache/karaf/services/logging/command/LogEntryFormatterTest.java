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

import java.util.Hashtable;

import org.junit.Test;
import org.osgi.service.log.LogLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogEntryFormatterTest {

    private static final String ESC = "\u001b[";

    @Test
    public void testDefaultPattern() {
        LogEntryFormatter formatter = new LogEntryFormatter(new Hashtable<>(), null);
        TestEntry entry = new TestEntry(LogLevel.ERROR, "foo", "m");
        StringBuilder expected = new StringBuilder();
        new PatternLayout(LogEntryFormatter.DEFAULT_PATTERN).format(expected, entry, false);
        assertEquals(expected + ESC + "0m", formatter.format(entry, false));
        assertEquals(expected.toString(), formatter.format(entry, true));
    }

    @Test
    public void testConfiguredPattern() {
        Hashtable<String, Object> config = new Hashtable<>();
        config.put("pattern", "[%p] %m");
        LogEntryFormatter formatter = new LogEntryFormatter(config, null);
        assertEquals("[ERROR] m" + ESC + "0m", formatter.format(new TestEntry(LogLevel.ERROR, "foo", "m"), false));
        // -p overrides the configured pattern
        formatter = new LogEntryFormatter(config, "%m");
        assertEquals("m" + ESC + "0m", formatter.format(new TestEntry(LogLevel.INFO, "foo", "m"), false));
        assertEquals("m", formatter.format(new TestEntry(LogLevel.INFO, "foo", "m"), true));
    }

    @Test
    public void testNoColor() {
        LogEntryFormatter formatter = new LogEntryFormatter(null, "\u001b[90m%p\u001b[0m %h{%m}");
        TestEntry entry = new TestEntry(LogLevel.ERROR, "foo", "m");
        assertEquals(ESC + "90mERROR" + ESC + "0m " + ESC + "1;31mm" + ESC + "m" + ESC + "0m", formatter.format(entry, false));
        String formatted = formatter.format(entry, true);
        assertEquals("ERROR m", formatted);
        assertFalse(formatted.contains("\u001b"));
    }

    @Test
    public void testException() {
        TestEntry entry = new TestEntry(LogLevel.ERROR, "foo", "failed");
        entry.exception = new IllegalStateException("boom");
        String formatted = new LogEntryFormatter(null, "%m%n").format(entry, true);
        assertTrue(formatted, formatted.startsWith("failed" + System.lineSeparator()
                + "java.lang.IllegalStateException: boom\n\tat "));
        assertTrue(formatted, formatted.endsWith("\n"));
    }

}
