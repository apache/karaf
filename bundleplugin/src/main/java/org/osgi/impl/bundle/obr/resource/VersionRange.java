/*
 * $Id: VersionRange.java 45 2007-10-01 12:56:02Z peter.kriens@aqute.biz $
 * 
 * Copyright (c) OSGi Alliance (2002, 2006, 2007). All Rights Reserved.
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

package org.osgi.impl.bundle.obr.resource;

import java.util.regex.*;

import org.osgi.framework.*;

public class VersionRange implements Comparable {
	Version high;
	Version low;
	char start = '[';
	char end = ']';

	static String V = "\\s*[0-9]+(\\.[0-9]+(\\.[0-9]+(\\.[a-zA-Z0-9_-]+)?)?)?\\s*";
	static Pattern RANGE = Pattern.compile("(\\(|\\[)(" + V + "),(" + V
			+ ")(\\)|\\])");

	public VersionRange(String string) {
		string = string.trim();
		Matcher m = RANGE.matcher(string);
		if (m.matches()) {
			start = m.group(1).charAt(0);
			low = new Version(m.group(2).trim());
			high = new Version(m.group(6).trim());
			end = m.group(10).charAt(0);
			if (low.compareTo(high) >= 0)
				throw new IllegalArgumentException(
						"Low Range is higher than High Range: " + low + "-"
								+ high);

		} else
			high = low = new Version(string);
	}

	public boolean isRange() {
		return high != low;
	}

	public boolean includeLow() {
		return start == '[';
	}

	public boolean includeHigh() {
		return end == ']';
	}

	public String toString() {
		if (high == low)
			return high.toString();

		StringBuffer sb = new StringBuffer();
		sb.append(start);
		sb.append(low);
		sb.append(',');
		sb.append(high);
		sb.append(end);
		return sb.toString();
	}

	public boolean equals(Object other) {
		if (other instanceof VersionRange) {
			return compareTo(other)==0;
		}
		return false;
	}

	public int hashCode() {
		return low.hashCode() * high.hashCode();
	}

	public int compareTo(Object other) {
		VersionRange range = (VersionRange) other;
		VersionRange a = this, b = range;
		if (range.isRange()) {
			a = range;
			b = this;
		} else {
			if ( !isRange() )
				return low.compareTo(range.high);
		}
		int l = a.low.compareTo(b.low);
		boolean ll = false;
		if (a.includeLow())
			ll = l <= 0;
		else
			ll = l < 0;

		if (!ll)
			return -1;

		int h = a.high.compareTo(b.high);
		if (a.includeHigh())
			ll = h >= 0;
		else
			ll = h > 0;

		if (ll)
			return 0;
		else
			return 1;
	}
}