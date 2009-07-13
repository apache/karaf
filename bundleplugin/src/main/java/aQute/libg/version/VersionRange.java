package aQute.libg.version;

import java.util.regex.*;

public class VersionRange {
	Version			high;
	Version			low;
	char			start	= '[';
	char			end		= ']';

	static Pattern	RANGE	= Pattern.compile("(\\(|\\[)\\s*(" +
									Version.VERSION_STRING + ")\\s*,\\s*(" +
									Version.VERSION_STRING + ")\\s*(\\)|\\])");

	public VersionRange(String string) {
		string = string.trim();
		Matcher m = RANGE.matcher(string);
		if (m.matches()) {
			start = m.group(1).charAt(0);
			String v1 = m.group(2);
			String v2 = m.group(10);
			low = new Version(v1);
			high = new Version(v2);
			end = m.group(18).charAt(0);
			if (low.compareTo(high) > 0)
				throw new IllegalArgumentException(
						"Low Range is higher than High Range: " + low + "-" +
								high);

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

	public Version getLow() {
		return low;
	}

	public Version getHigh() {
		return high;
	}

	public boolean includes(Version v) {
		if ( !isRange() ) {
			return low.compareTo(v) <=0;
		}
		if (includeLow()) {
			if (v.compareTo(low) < 0)
				return false;
		} else if (v.compareTo(low) <= 0)
			return false;

		if (includeHigh()) {
			if (v.compareTo(high) > 0)
				return false;
		} else if (v.compareTo(high) >= 0)
			return false;
		
		return true;
	}
}