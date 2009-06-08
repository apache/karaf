/*
 * Copyright (c) OSGi Alliance (2005, 2008). All Rights Reserved.
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

package org.osgi.service.condpermadmin;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Condition to test if the location of a bundle matches or does not match a
 * pattern. Since the bundle's location cannot be changed, this condition is
 * immutable.
 * 
 * <p>
 * Pattern matching is done according to the filter string matching rules.
 * 
 * @ThreadSafe
 * @version $Revision: 5901 $
 */
public class BundleLocationCondition {
	private static final String	CONDITION_TYPE	= "org.osgi.service.condpermadmin.BundleLocationCondition";

	/**
	 * Constructs a condition that tries to match the passed Bundle's location
	 * to the location pattern.
	 * 
	 * @param bundle The Bundle being evaluated.
	 * @param info The ConditionInfo from which to construct the condition. The
	 *        ConditionInfo must specify one or two arguments. The first
	 *        argument of the ConditionInfo specifies the location pattern
	 *        against which to match the bundle location. Matching is done
	 *        according to the filter string matching rules. Any '*' characters
	 *        in the first argument are used as wildcards when matching bundle
	 *        locations unless they are escaped with a '\' character. The
	 *        Condition is satisfied if the bundle location matches the pattern.
	 *        The second argument of the ConditionInfo is optional. If a second
	 *        argument is present and equal to "!", then the satisfaction of the
	 *        Condition is negated. That is, the Condition is satisfied if the
	 *        bundle location does NOT match the pattern. If the second argument
	 *        is present but does not equal "!", then the second argument is
	 *        ignored.
	 * @return Condition object for the requested condition.
	 */
	static public Condition getCondition(final Bundle bundle,
			final ConditionInfo info) {
		if (!CONDITION_TYPE.equals(info.getType()))
			throw new IllegalArgumentException(
					"ConditionInfo must be of type \"" + CONDITION_TYPE + "\"");
		String[] args = info.getArgs();
		if (args.length != 1 && args.length != 2)
			throw new IllegalArgumentException("Illegal number of args: " + args.length);
		String bundleLocation = (String) AccessController.doPrivileged(new PrivilegedAction() {
					public Object run() {
						return bundle.getLocation();
					}
				});
		Filter filter = null;
		try {
			filter = FrameworkUtil.createFilter("(location="
					+ escapeLocation(args[0]) + ")");
		}
		catch (InvalidSyntaxException e) {
			// this should never happen, but just in case
			throw new RuntimeException("Invalid filter: " + e.getFilter(), e);
		}
		Hashtable matchProps = new Hashtable(2);
		matchProps.put("location", bundleLocation);
		boolean negate = (args.length == 2) ? "!".equals(args[1]) : false;
		return (negate ^ filter.match(matchProps)) ? Condition.TRUE
				: Condition.FALSE;
	}

	private BundleLocationCondition() {
		// private constructor to prevent objects of this type
	}

	/**
	 * Escape the value string such that '(', ')' and '\' are escaped. The '\'
	 * char is only escaped if it is not followed by a '*'.
	 * 
	 * @param value unescaped value string.
	 * @return escaped value string.
	 */
	private static String escapeLocation(final String value) {
		boolean escaped = false;
		int inlen = value.length();
		int outlen = inlen << 1; /* inlen * 2 */

		char[] output = new char[outlen];
		value.getChars(0, inlen, output, inlen);

		int cursor = 0;
		for (int i = inlen; i < outlen; i++) {
			char c = output[i];
			switch (c) {
				case '\\' :
					if (i + 1 < outlen && output[i + 1] == '*')
						break;
				case '(' :
				case ')' :
					output[cursor] = '\\';
					cursor++;
					escaped = true;
					break;
			}

			output[cursor] = c;
			cursor++;
		}

		return escaped ? new String(output, 0, cursor) : value;
	}
}
