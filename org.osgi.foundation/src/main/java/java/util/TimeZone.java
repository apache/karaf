/*
 * $Header: /cvshome/build/ee.foundation/src/java/util/TimeZone.java,v 1.6 2006/03/14 01:20:26 hargrave Exp $
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
public abstract class TimeZone implements java.io.Serializable, java.lang.Cloneable {
	public TimeZone() { }
	public java.lang.Object clone() { return null; }
	public static java.lang.String[] getAvailableIDs() { return null; }
	public static java.lang.String[] getAvailableIDs(int var0) { return null; }
	public static java.util.TimeZone getDefault() { return null; }
	public final java.lang.String getDisplayName() { return null; }
	public final java.lang.String getDisplayName(java.util.Locale var0) { return null; }
	public final java.lang.String getDisplayName(boolean var0, int var1) { return null; }
	public java.lang.String getDisplayName(boolean var0, int var1, java.util.Locale var2) { return null; }
	public java.lang.String getID() { return null; }
	public abstract int getOffset(int var0, int var1, int var2, int var3, int var4, int var5);
	public abstract int getRawOffset();
	public static java.util.TimeZone getTimeZone(java.lang.String var0) { return null; }
	public boolean hasSameRules(java.util.TimeZone var0) { return false; }
	public abstract boolean inDaylightTime(java.util.Date var0);
	public static void setDefault(java.util.TimeZone var0) { }
	public void setID(java.lang.String var0) { }
	public abstract void setRawOffset(int var0);
	public abstract boolean useDaylightTime();
	public final static int SHORT = 0;
	public final static int LONG = 1;
}

