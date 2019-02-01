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

import java.io.IOException;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class FastDateFormat {

    public static final String YYYY_MM_DD = "yyyy-MM-dd";
    public static final String MMM_D2 = "MMM d2";
    public static final String XXX = "XXX";

    private final TimeZone timeZone;
    private final Locale locale;
    private long midnightTomorrow;
    private long midnightToday;
    private final int[] dstOffsets = new int[25];

    private Map<String, String> cache = new HashMap<>();

    public FastDateFormat() {
        this(TimeZone.getDefault(), Locale.ENGLISH);
    }

    public FastDateFormat(TimeZone timeZone, Locale locale) {
        this.timeZone = timeZone;
        this.locale = locale;
    }

    /**
     * Check whether the given instant if in the same day as the previous one.
     */
    public boolean sameDay(long now) {
        if (now >= midnightTomorrow || now < midnightToday) {
            updateMidnightMillis(now);
            updateDaylightSavingTime();
            cache.clear();
            return false;
        } else {
            return true;
        }
    }

    /**
     * Get the date formatted with the given pattern.
     */
    public String getDate(long now, String pattern) {
        sameDay(now);
        String date = cache.get(pattern);
        if (date == null) {
            if (MMM_D2.equals(pattern)) {
                StringBuffer sb = new StringBuffer();
                FieldPosition fp = new FieldPosition(DateFormat.Field.DAY_OF_MONTH);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", locale);
                sdf.setCalendar(Calendar.getInstance(timeZone, locale));
                sdf.format(new Date(now), sb, fp);
                if (sb.charAt(fp.getBeginIndex()) == '0') {
                    sb.setCharAt(fp.getBeginIndex(), ' ');
                }
                date = sb.toString();
            } else {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
                sdf.setCalendar(Calendar.getInstance(timeZone, locale));
                date = sdf.format(new Date(now));
            }
            cache.put(pattern, date);
        }
        return date;
    }

    /**
     * Write the time in the HH:MM:SS[.sss] format to the given <code>Appendable</code>.
     */
    public void writeTime(long now, boolean writeMillis, Appendable buffer) throws IOException {
        int ms = millisSinceMidnight(now);

        final int hourOfDay = ms / 3600000;
        final int hours = hourOfDay + daylightSavingTime(hourOfDay) / 3600000;
        ms -= 3600000 * hourOfDay;

        final int minutes = ms / 60000;
        ms -= 60000 * minutes;

        final int seconds = ms / 1000;
        ms -= 1000 * seconds;

        // Hour
        int temp = hours / 10;
        buffer.append((char) (temp + '0'));
        buffer.append ((char) (hours - 10 * temp + '0'));
        buffer.append(':');

        // Minute
        temp = minutes / 10;
        buffer.append((char) (temp + '0'));
        buffer.append((char) (minutes - 10 * temp + '0'));
        buffer.append(':');

        // Second
        temp = seconds / 10;
        buffer.append((char) (temp + '0'));
        buffer.append((char) (seconds - 10 * temp + '0'));

        // Millisecond
        if (writeMillis) {
            buffer.append('.');
            temp = ms / 100;
            buffer.append((char) (temp + '0'));
            ms -= 100 * temp;
            temp = ms / 10;
            buffer.append((char) (temp + '0'));
            ms -= 10 * temp;
            buffer.append((char) (ms + '0'));
        }
    }

    private int millisSinceMidnight(final long now) {
        sameDay(now);
        return (int) (now - midnightToday);
    }

    private int daylightSavingTime(final int hourOfDay) {
        return hourOfDay > 23 ? dstOffsets[23] : dstOffsets[hourOfDay];
    }

    private void updateMidnightMillis(final long now) {
        final Calendar cal = Calendar.getInstance(timeZone);
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        midnightToday = cal.getTimeInMillis();
        cal.add(Calendar.DATE, 1);
        midnightTomorrow = cal.getTimeInMillis();
    }

    private void updateDaylightSavingTime() {
        Arrays.fill(dstOffsets, 0);
        final int ONE_HOUR = (int) TimeUnit.HOURS.toMillis(1);
        if (timeZone.getOffset(midnightToday) != timeZone.getOffset(midnightToday + 23 * ONE_HOUR)) {
            for (int i = 0; i < dstOffsets.length; i++) {
                final long time = midnightToday + i * ONE_HOUR;
                dstOffsets[i] = timeZone.getOffset(time) - timeZone.getRawOffset();
            }
            if (dstOffsets[0] > dstOffsets[23]) { // clock is moved backwards.
                // we obtain midnightTonight with Calendar.getInstance(TimeZone), so it already includes raw offset
                for (int i = dstOffsets.length - 1; i >= 0; i--) {
                    dstOffsets[i] -= dstOffsets[0]; //
                }
            }
        }
    }
}
