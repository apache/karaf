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
package org.apache.karaf.audit.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class FastDateFormatTest {

    @Test
    public void test() throws Exception {
        FastDateFormat cal = new FastDateFormat();

        long time = new SimpleDateFormat("yyyy-MM-dd").parse("2017-11-05").getTime();
        assertEquals("Nov  5", cal.getDate(time, FastDateFormat.MMM_D2));
        assertEquals("2017-11-05", cal.getDate(time, FastDateFormat.YYYY_MM_DD));

        time += TimeUnit.DAYS.toMillis(5) + TimeUnit.HOURS.toMillis(1);
        assertEquals("Nov 10", cal.getDate(time, FastDateFormat.MMM_D2));
        assertEquals("2017-11-10", cal.getDate(time, FastDateFormat.YYYY_MM_DD));
    }

    @Test
    public void testTimeZone() throws Exception {
        long time = new SimpleDateFormat("yyyy-MM-dd").parse("2017-11-05").getTime();
        FastDateFormat cal = new FastDateFormat(TimeZone.getTimeZone("GMT+5"), Locale.ENGLISH);
        assertEquals("+05:00", cal.getDate(time, FastDateFormat.XXX));
    }
}
