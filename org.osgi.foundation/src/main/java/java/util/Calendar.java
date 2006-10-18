/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/Calendar.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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

package java.util;
public abstract class Calendar implements java.io.Serializable, java.lang.Cloneable {
	protected Calendar() { }
	protected Calendar(java.util.TimeZone var0, java.util.Locale var1) { }
	public abstract void add(int var0, int var1);
	public boolean after(java.lang.Object var0) { return false; }
	public boolean before(java.lang.Object var0) { return false; }
	public final void clear() { }
	public final void clear(int var0) { }
	public java.lang.Object clone() { return null; }
	protected void complete() { }
	protected abstract void computeFields();
	protected abstract void computeTime();
	public boolean equals(java.lang.Object var0) { return false; }
	public final int get(int var0) { return 0; }
	public int getActualMaximum(int var0) { return 0; }
	public int getActualMinimum(int var0) { return 0; }
	public static java.util.Locale[] getAvailableLocales() { return null; }
	public int getFirstDayOfWeek() { return 0; }
	public abstract int getGreatestMinimum(int var0);
	public static java.util.Calendar getInstance() { return null; }
	public static java.util.Calendar getInstance(java.util.Locale var0) { return null; }
	public static java.util.Calendar getInstance(java.util.TimeZone var0) { return null; }
	public static java.util.Calendar getInstance(java.util.TimeZone var0, java.util.Locale var1) { return null; }
	public abstract int getLeastMaximum(int var0);
	public abstract int getMaximum(int var0);
	public int getMinimalDaysInFirstWeek() { return 0; }
	public abstract int getMinimum(int var0);
	public final java.util.Date getTime() { return null; }
	protected long getTimeInMillis() { return 0l; }
	public java.util.TimeZone getTimeZone() { return null; }
	public int hashCode() { return 0; }
	protected final int internalGet(int var0) { return 0; }
	public boolean isLenient() { return false; }
	public final boolean isSet(int var0) { return false; }
	public void roll(int var0, int var1) { }
	public abstract void roll(int var0, boolean var1);
	public final void set(int var0, int var1) { }
	public final void set(int var0, int var1, int var2) { }
	public final void set(int var0, int var1, int var2, int var3, int var4) { }
	public final void set(int var0, int var1, int var2, int var3, int var4, int var5) { }
	public void setFirstDayOfWeek(int var0) { }
	public void setLenient(boolean var0) { }
	public void setMinimalDaysInFirstWeek(int var0) { }
	public final void setTime(java.util.Date var0) { }
	protected void setTimeInMillis(long var0) { }
	public void setTimeZone(java.util.TimeZone var0) { }
	public java.lang.String toString() { return null; }
	protected boolean areFieldsSet;
	protected int[] fields;
	protected boolean[] isSet;
	protected boolean isTimeSet;
	protected long time;
	public final static int JANUARY = 0;
	public final static int FEBRUARY = 1;
	public final static int MARCH = 2;
	public final static int APRIL = 3;
	public final static int MAY = 4;
	public final static int JUNE = 5;
	public final static int JULY = 6;
	public final static int AUGUST = 7;
	public final static int SEPTEMBER = 8;
	public final static int OCTOBER = 9;
	public final static int NOVEMBER = 10;
	public final static int DECEMBER = 11;
	public final static int UNDECIMBER = 12;
	public final static int SUNDAY = 1;
	public final static int MONDAY = 2;
	public final static int TUESDAY = 3;
	public final static int WEDNESDAY = 4;
	public final static int THURSDAY = 5;
	public final static int FRIDAY = 6;
	public final static int SATURDAY = 7;
	public final static int ERA = 0;
	public final static int YEAR = 1;
	public final static int MONTH = 2;
	public final static int WEEK_OF_YEAR = 3;
	public final static int WEEK_OF_MONTH = 4;
	public final static int DATE = 5;
	public final static int DAY_OF_MONTH = 5;
	public final static int DAY_OF_YEAR = 6;
	public final static int DAY_OF_WEEK = 7;
	public final static int DAY_OF_WEEK_IN_MONTH = 8;
	public final static int AM_PM = 9;
	public final static int HOUR = 10;
	public final static int HOUR_OF_DAY = 11;
	public final static int MINUTE = 12;
	public final static int SECOND = 13;
	public final static int MILLISECOND = 14;
	public final static int ZONE_OFFSET = 15;
	public final static int DST_OFFSET = 16;
	public final static int FIELD_COUNT = 17;
	public final static int AM = 0;
	public final static int PM = 1;
}

