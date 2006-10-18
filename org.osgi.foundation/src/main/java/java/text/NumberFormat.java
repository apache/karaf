/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/NumberFormat.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public abstract class NumberFormat extends java.text.Format {
	public NumberFormat() { }
	public java.lang.Object clone() { return null; }
	public boolean equals(java.lang.Object var0) { return false; }
	public final java.lang.String format(double var0) { return null; }
	public abstract java.lang.StringBuffer format(double var0, java.lang.StringBuffer var1, java.text.FieldPosition var2);
	public final java.lang.String format(long var0) { return null; }
	public abstract java.lang.StringBuffer format(long var0, java.lang.StringBuffer var1, java.text.FieldPosition var2);
	public final java.lang.StringBuffer format(java.lang.Object var0, java.lang.StringBuffer var1, java.text.FieldPosition var2) { return null; }
	public static java.util.Locale[] getAvailableLocales() { return null; }
	public final static java.text.NumberFormat getCurrencyInstance() { return null; }
	public static java.text.NumberFormat getCurrencyInstance(java.util.Locale var0) { return null; }
	public final static java.text.NumberFormat getInstance() { return null; }
	public static java.text.NumberFormat getInstance(java.util.Locale var0) { return null; }
	public int getMaximumFractionDigits() { return 0; }
	public int getMaximumIntegerDigits() { return 0; }
	public int getMinimumFractionDigits() { return 0; }
	public int getMinimumIntegerDigits() { return 0; }
	public final static java.text.NumberFormat getNumberInstance() { return null; }
	public static java.text.NumberFormat getNumberInstance(java.util.Locale var0) { return null; }
	public final static java.text.NumberFormat getPercentInstance() { return null; }
	public static java.text.NumberFormat getPercentInstance(java.util.Locale var0) { return null; }
	public int hashCode() { return 0; }
	public boolean isGroupingUsed() { return false; }
	public boolean isParseIntegerOnly() { return false; }
	public java.lang.Number parse(java.lang.String var0) throws java.text.ParseException { return null; }
	public abstract java.lang.Number parse(java.lang.String var0, java.text.ParsePosition var1);
	public final java.lang.Object parseObject(java.lang.String var0, java.text.ParsePosition var1) { return null; }
	public void setGroupingUsed(boolean var0) { }
	public void setMaximumFractionDigits(int var0) { }
	public void setMaximumIntegerDigits(int var0) { }
	public void setMinimumFractionDigits(int var0) { }
	public void setMinimumIntegerDigits(int var0) { }
	public void setParseIntegerOnly(boolean var0) { }
	public final static int INTEGER_FIELD = 0;
	public final static int FRACTION_FIELD = 1;
}

