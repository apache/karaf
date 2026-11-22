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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DisplayLogTest {

    private static List<LogEntry> log(LogEntry... entries) {
        return new ArrayList<>(Arrays.asList(entries));
    }

    @Test
    public void testLastEntries() {
        LogEntry e1 = new TestEntry(LogLevel.INFO, "foo", "1");
        LogEntry e2 = new TestEntry(LogLevel.INFO, "foo", "2");
        LogEntry e3 = new TestEntry(LogLevel.INFO, "foo", "3");
        // LogReaderService.getLog() returns the most recent entry first
        assertEquals(Arrays.asList(e1, e2, e3), DisplayLog.lastEntries(log(e3, e2, e1), 0));
        assertEquals(Arrays.asList(e2, e3), DisplayLog.lastEntries(log(e3, e2, e1), 2));
        assertEquals(Arrays.asList(e1, e2, e3), DisplayLog.lastEntries(log(e3, e2, e1), 5));
    }

    @Test
    public void testMinLevel() {
        assertNull(DisplayLog.getMinLevel(null));
        assertNull(DisplayLog.getMinLevel("TRACE"));
        assertNull(DisplayLog.getMinLevel("default"));
        assertEquals(LogLevel.DEBUG, DisplayLog.getMinLevel("debug"));
        assertEquals(LogLevel.WARN, DisplayLog.getMinLevel("WARN"));
        assertEquals(LogLevel.ERROR, DisplayLog.getMinLevel("FATAL"));
        try {
            DisplayLog.getMinLevel("VERBOSE");
            fail("Exception expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void testAccept() {
        DisplayLog command = new DisplayLog();
        LogLevel warn = LogLevel.WARN;
        assertTrue(command.accept(new TestEntry(LogLevel.AUDIT, "foo", "m"), warn));
        assertTrue(command.accept(new TestEntry(LogLevel.ERROR, "foo", "m"), warn));
        assertTrue(command.accept(new TestEntry(LogLevel.WARN, "foo", "m"), warn));
        assertFalse(command.accept(new TestEntry(LogLevel.INFO, "foo", "m"), warn));
        assertTrue(command.accept(new TestEntry(LogLevel.TRACE, "foo", "m"), null));

        command.logger = "karaf.features";
        assertTrue(command.accept(new TestEntry(LogLevel.INFO, "org.apache.karaf.features.core", "m"), null));
        assertFalse(command.accept(new TestEntry(LogLevel.INFO, "org.apache.karaf.shell", "m"), null));
        assertFalse(command.accept(new TestEntry(LogLevel.INFO, null, "m"), null));
    }

}
