/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.log.core.internal;

import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.junit.Test;
import org.ops4j.pax.logging.log4j2.internal.spi.PaxLoggingEventImpl;
import org.ops4j.pax.logging.spi.PaxLoggingEvent;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

public class CircularBufferTest {
    @Test
    public void use() {
        final CircularBuffer buffer = new CircularBuffer(3);
        assertEquals(0, buffer.getElements(buffer.maxSize()).size());
        final PaxLoggingEvent e1 = addAndAssertEvent(buffer);
        final PaxLoggingEvent e2 = addAndAssertEvent(buffer, e1);
        final PaxLoggingEvent e3 = addAndAssertEvent(buffer, e1, e2);
        final PaxLoggingEvent e4 = addAndAssertEvent(buffer, e2, e3);
        final PaxLoggingEvent e5 = addAndAssertEvent(buffer, e3, e4);
        final PaxLoggingEvent e6 = addAndAssertEvent(buffer, e4, e5);
        addAndAssertEvent(buffer, e5, e6);
    }

    private PaxLoggingEvent addAndAssertEvent(final CircularBuffer buffer, final PaxLoggingEvent... previous) {
        final PaxLoggingEvent e4 = newEvent();
        buffer.add(e4);
        assertEquals(Stream.concat(Stream.of(previous), Stream.of(e4)).collect(toList()), buffer.getElements(buffer.maxSize()));
        return e4;
    }

    private PaxLoggingEvent newEvent() {
        return new PaxLoggingEventImpl(new Log4jLogEvent());
    }
}
