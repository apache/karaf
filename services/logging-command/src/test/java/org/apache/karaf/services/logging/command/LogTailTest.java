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
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogLevel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class LogTailTest {

    @Test
    public void testNewEntries() throws Exception {
        LogEntry old = new TestEntry(LogLevel.INFO, "foo", "old");
        LogEntry both = new TestEntry(LogLevel.INFO, "foo", "both");
        LogEntry added = new TestEntry(LogLevel.INFO, "foo", "added");
        LogEntry filtered = new TestEntry(LogLevel.INFO, "bar", "filtered");
        LogTail.Tail tail = new LogTail.Tail(entry -> !"bar".equals(entry.getLoggerName()));

        // logged once the listener is added, but before the log is read
        tail.logged(both);
        assertEquals(Arrays.asList(old, both), tail.start(new ArrayList<>(Arrays.asList(both, old)), 50));
        tail.logged(filtered);
        tail.logged(added);

        // "both" is skipped as it has already been displayed
        assertSame(added, tail.next(10, TimeUnit.MILLISECONDS));
        assertNull(tail.next(10, TimeUnit.MILLISECONDS));
        assertEquals(0, tail.dropped());
    }

    @Test
    public void testDropped() throws Exception {
        LogTail.Tail tail = new LogTail.Tail(entry -> true);
        tail.start(new ArrayList<>(), 50);
        for (int i = 0; i < LogTail.QUEUE_CAPACITY + 3; i++) {
            tail.logged(new TestEntry(LogLevel.INFO, "foo", "m" + i));
        }
        assertEquals(3, tail.dropped());
        assertEquals(0, tail.dropped());
        assertEquals("m0", tail.next(10, TimeUnit.MILLISECONDS).getMessage());
    }

    @Test
    public void testSshdEntries() {
        assertTrue(LogTail.isSshdEntry(new TestEntry(LogLevel.DEBUG, "org.apache.sshd.common.io.nio2.Nio2Session", "m")));
        assertTrue(LogTail.isSshdEntry(new TestEntry(LogLevel.WARN, "org.apache.sshd.server.session.ServerSessionImpl", "m")));
        assertFalse(LogTail.isSshdEntry(new TestEntry(LogLevel.ERROR, "org.apache.sshd.server.session.ServerSessionImpl", "m")));
        assertFalse(LogTail.isSshdEntry(new TestEntry(LogLevel.DEBUG, "org.apache.karaf.shell.ssh", "m")));
        assertFalse(LogTail.isSshdEntry(new TestEntry(LogLevel.DEBUG, null, "m")));
    }

}
