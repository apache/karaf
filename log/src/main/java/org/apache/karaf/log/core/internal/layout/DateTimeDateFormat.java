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
package org.apache.karaf.log.core.internal.layout;

import java.text.DateFormatSymbols;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/** Copied from log4j */
/**
 * Formats a {@link Date} in the format "dd MMM yyyy HH:mm:ss,SSS" for example, "06 Nov 1994
 * 15:49:37,459".
 *
 * @since 0.7.5
 */
public class DateTimeDateFormat extends AbsoluteTimeDateFormat {

    String[] shortMonths;

    public DateTimeDateFormat() {
        super();
        shortMonths = new DateFormatSymbols().getShortMonths();
    }

    public DateTimeDateFormat(TimeZone timeZone) {
        this();
        setCalendar(Calendar.getInstance(timeZone));
    }

    /**
     * Appends to <code>sbuf</code> the date in the format "dd MMM yyyy HH:mm:ss,SSS" for example,
     * "06 Nov 1994 08:49:37,459".
     *
     * @param sbuf the string buffer to write to
     */
    public StringBuffer format(Date date, StringBuffer sbuf, FieldPosition fieldPosition) {

        calendar.setTime(date);

        int day = calendar.get(Calendar.DAY_OF_MONTH);
        if (day < 10) sbuf.append('0');
        sbuf.append(day);
        sbuf.append(' ');
        sbuf.append(shortMonths[calendar.get(Calendar.MONTH)]);
        sbuf.append(' ');

        int year = calendar.get(Calendar.YEAR);
        sbuf.append(year);
        sbuf.append(' ');

        return super.format(date, sbuf, fieldPosition);
    }

    /** This method does not do anything but return <code>null</code>. */
    public Date parse(java.lang.String s, ParsePosition pos) {
        return null;
    }
}
