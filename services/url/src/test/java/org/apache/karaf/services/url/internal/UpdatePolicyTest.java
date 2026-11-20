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
package org.apache.karaf.services.url.internal;

import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.*;

public class UpdatePolicyTest {

    private static final long MINUTE = 60 * 1000;

    @Test
    public void testParse() {
        assertSame(UpdatePolicy.DAILY, UpdatePolicy.parse(null));
        assertSame(UpdatePolicy.DAILY, UpdatePolicy.parse("daily"));
        assertSame(UpdatePolicy.ALWAYS, UpdatePolicy.parse("ALWAYS"));
        assertSame(UpdatePolicy.NEVER, UpdatePolicy.parse(" never "));
        assertSame(UpdatePolicy.DAILY, UpdatePolicy.parse("weekly"));
        assertSame(UpdatePolicy.DAILY, UpdatePolicy.parse("interval:x"));
        assertEquals("interval:30", UpdatePolicy.parse("interval:30").toString());
    }

    @Test
    public void testIsUpdateRequired() {
        long now = System.currentTimeMillis();
        assertTrue(UpdatePolicy.ALWAYS.isUpdateRequired(now, now));
        assertFalse(UpdatePolicy.NEVER.isUpdateRequired(0, now));
        assertTrue(UpdatePolicy.parse("interval:30").isUpdateRequired(now - 31 * MINUTE, now));
        assertFalse(UpdatePolicy.parse("interval:30").isUpdateRequired(now - 29 * MINUTE, now));
        // never checked
        assertTrue(UpdatePolicy.DAILY.isUpdateRequired(0, now));
    }

    @Test
    public void testDailyChecksOncePerDay() {
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 12);
        today.set(Calendar.MINUTE, 0);
        long noon = today.getTimeInMillis();
        today.set(Calendar.HOUR_OF_DAY, 1);
        long earlyMorning = today.getTimeInMillis();

        assertFalse(UpdatePolicy.DAILY.isUpdateRequired(earlyMorning, noon));
        assertTrue(UpdatePolicy.DAILY.isUpdateRequired(earlyMorning - 2 * 60 * MINUTE, noon));
    }

}
