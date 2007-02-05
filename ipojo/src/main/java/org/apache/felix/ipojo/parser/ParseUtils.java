package org.apache.felix.ipojo.parser;

public class ParseUtils {
	
	/**
	 * Parse the string form of an array as {a, b, c}
	 * @param str : the string form
	 * @return the resulting string array
	 */
	public static String[] parseArrays(String str) {
		// Remove { and }
		if(str.startsWith("{") && str.endsWith("}")) {
			String m = str.substring(1, str.length() - 1);
			String[] values = m.split(",");
			for(int i = 0; i < values.length; i++) {
				values[i] = values[i].trim();
			}
			return values;
		} else {
			return new String[] {str};
		}
	}

}
