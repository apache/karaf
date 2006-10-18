/*
 * $Header: /cvshome/build/ee.foundation/src/java/text/DecimalFormat.java,v 1.6 2006/03/14 01:20:30 hargrave Exp $
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
public class DecimalFormat extends java.text.NumberFormat {
	public DecimalFormat() { }
	public DecimalFormat(java.lang.String var0) { }
	public DecimalFormat(java.lang.String var0, java.text.DecimalFormatSymbols var1) { }
	public void applyLocalizedPattern(java.lang.String var0) { }
	public void applyPattern(java.lang.String var0) { }
	public java.lang.Object clone() { return null; }
	public boolean equals(java.lang.Object var0) { return false; }
	public java.lang.StringBuffer format(double var0, java.lang.StringBuffer var1, java.text.FieldPosition var2) { return null; }
	public java.lang.StringBuffer format(long var0, java.lang.StringBuffer var1, java.text.FieldPosition var2) { return null; }
	public java.text.DecimalFormatSymbols getDecimalFormatSymbols() { return null; }
	public int getGroupingSize() { return 0; }
	public int getMultiplier() { return 0; }
	public java.lang.String getNegativePrefix() { return null; }
	public java.lang.String getNegativeSuffix() { return null; }
	public java.lang.String getPositivePrefix() { return null; }
	public java.lang.String getPositiveSuffix() { return null; }
	public int hashCode() { return 0; }
	public boolean isDecimalSeparatorAlwaysShown() { return false; }
	public java.lang.Number parse(java.lang.String var0, java.text.ParsePosition var1) { return null; }
	public void setDecimalFormatSymbols(java.text.DecimalFormatSymbols var0) { }
	public void setDecimalSeparatorAlwaysShown(boolean var0) { }
	public void setGroupingSize(int var0) { }
	public void setMaximumFractionDigits(int var0) { }
	public void setMaximumIntegerDigits(int var0) { }
	public void setMinimumFractionDigits(int var0) { }
	public void setMinimumIntegerDigits(int var0) { }
	public void setMultiplier(int var0) { }
	public void setNegativePrefix(java.lang.String var0) { }
	public void setNegativeSuffix(java.lang.String var0) { }
	public void setPositivePrefix(java.lang.String var0) { }
	public void setPositiveSuffix(java.lang.String var0) { }
	public java.lang.String toLocalizedPattern() { return null; }
	public java.lang.String toPattern() { return null; }
}

