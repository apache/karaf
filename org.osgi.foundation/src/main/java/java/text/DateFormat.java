/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/DateFormat.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
 *
 * (C) Copyright 2001 Sun Microsystems, Inc.
 * Copyright (c) OSGi Alliance (2001, 2005). All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.text;
public abstract class DateFormat extends java.text.Format {
	protected DateFormat() { }
	public java.lang.Object clone() { return null; }
	public boolean equals(java.lang.Object var0) { return false; }
	public final java.lang.StringBuffer format(java.lang.Object var0, java.lang.StringBuffer var1, java.text.FieldPosition var2) { return null; }
	public final java.lang.String format(java.util.Date var0) { return null; }
	public abstract java.lang.StringBuffer format(java.util.Date var0, java.lang.StringBuffer var1, java.text.FieldPosition var2);
	public static java.util.Locale[] getAvailableLocales() { return null; }
	public java.util.Calendar getCalendar() { return null; }
	public final static java.text.DateFormat getDateInstance() { return null; }
	public final static java.text.DateFormat getDateInstance(int var0) { return null; }
	public final static java.text.DateFormat getDateInstance(int var0, java.util.Locale var1) { return null; }
	public final static java.text.DateFormat getDateTimeInstance() { return null; }
	public final static java.text.DateFormat getDateTimeInstance(int var0, int var1) { return null; }
	public final static java.text.DateFormat getDateTimeInstance(int var0, int var1, java.util.Locale var2) { return null; }
	public final static java.text.DateFormat getInstance() { return null; }
	public java.text.NumberFormat getNumberFormat() { return null; }
	public final static java.text.DateFormat getTimeInstance() { return null; }
	public final static java.text.DateFormat getTimeInstance(int var0) { return null; }
	public final static java.text.DateFormat getTimeInstance(int var0, java.util.Locale var1) { return null; }
	public java.util.TimeZone getTimeZone() { return null; }
	public int hashCode() { return 0; }
	public boolean isLenient() { return false; }
	public java.util.Date parse(java.lang.String var0) throws java.text.ParseException { return null; }
	public abstract java.util.Date parse(java.lang.String var0, java.text.ParsePosition var1);
	public java.lang.Object parseObject(java.lang.String var0, java.text.ParsePosition var1) { return null; }
	public void setCalendar(java.util.Calendar var0) { }
	public void setLenient(boolean var0) { }
	public void setNumberFormat(java.text.NumberFormat var0) { }
	public void setTimeZone(java.util.TimeZone var0) { }
	protected java.util.Calendar calendar;
	protected java.text.NumberFormat numberFormat;
	public final static int DEFAULT = 2;
	public final static int FULL = 0;
	public final static int LONG = 1;
	public final static int MEDIUM = 2;
	public final static int SHORT = 3;
	public final static int ERA_FIELD = 0;
	public final static int YEAR_FIELD = 1;
	public final static int MONTH_FIELD = 2;
	public final static int DATE_FIELD = 3;
	public final static int HOUR_OF_DAY1_FIELD = 4;
	public final static int HOUR_OF_DAY0_FIELD = 5;
	public final static int MINUTE_FIELD = 6;
	public final static int SECOND_FIELD = 7;
	public final static int MILLISECOND_FIELD = 8;
	public final static int DAY_OF_WEEK_FIELD = 9;
	public final static int DAY_OF_YEAR_FIELD = 10;
	public final static int DAY_OF_WEEK_IN_MONTH_FIELD = 11;
	public final static int WEEK_OF_YEAR_FIELD = 12;
	public final static int WEEK_OF_MONTH_FIELD = 13;
	public final static int AM_PM_FIELD = 14;
	public final static int HOUR1_FIELD = 15;
	public final static int HOUR0_FIELD = 16;
	public final static int TIMEZONE_FIELD = 17;
}

