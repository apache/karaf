/*
 * Copyright (c) OSGi Alliance (2007, 2009). All Rights Reserved.
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
package org.osgi.util.cdma;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * Class representing an ESN condition. Instances of this class contain a string
 * value that is matched against the ESN of the device.
 * 
 * @ThreadSafe
 * @version $Revision: 6439 $
 */
public class ESNCondition {
	private static final String	ORG_OSGI_UTIL_CDMA_ESN	= "org.osgi.util.cdma.esn";
	private static final String	ESN;
	private static final int	ESN_LENGTH				= 8;

	static {
		ESN = (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				String esn = System.getProperty(ORG_OSGI_UTIL_CDMA_ESN);
				if (esn == null) {
					return null;
				}
				return esn.toUpperCase();
			}
		});
	}

	private ESNCondition() {
		// prevent instances being constructed
	}

	/**
	 * Creates an ESNCondition object.
	 * 
	 * @param bundle This parameter is ignored, as the ESN number is the
	 *        property of the mobile device, and thus the same for all bundles.
	 * @param conditionInfo Contains the ESN value against which to match the
	 *        device's ESN. Its {@link ConditionInfo#getArgs()} method should
	 *        return a String array with one value, the ESN string. The ESN is 8
	 *        hexadecimal digits (32 bits) without hyphens. Limited pattern
	 *        matching is allowed: the string is 0 to 7 digits, followed by an
	 *        asterisk(<code>*</code>).
	 * @return A Condition object that indicates whether the specified ESN
	 *         number matches that of the device. If the number ends with an
	 *         asterisk ( <code>*</code>), then the beginning of the ESN is
	 *         compared to the pattern.
	 * @throws IllegalArgumentException If the ESN is not a string of 8
	 *         hexadecimal digits, or 0 to 7 hexadecimal digits with an
	 *         <code>*</code> at the end.
	 */
	public static Condition getCondition(Bundle bundle,
			ConditionInfo conditionInfo) {
		String esn = conditionInfo.getArgs()[0].toUpperCase();
		int length = esn.length();
		if (length > ESN_LENGTH) {
			throw new IllegalArgumentException("ESN too long: " + esn);
		}
		if (esn.endsWith("*")) {
			length--;
			esn = esn.substring(0, length);
		}
		else {
			if (length < ESN_LENGTH) {
				throw new IllegalArgumentException("ESN too short: " + esn);
			}
		}
		for (int i = 0; i < length; i++) {
			char c = esn.charAt(i);
			if (('0' <= c) && (c <= '9')) {
				continue;
			}
	        if (('A' <= c) && (c <= 'F')) {
				continue;
			}
			throw new IllegalArgumentException("not a valid ESN: " + esn);
		}
		if (ESN == null) {
			System.err
					.println("The OSGi implementation of org.osgi.util.cdma.ESNCondition needs the system property "
							+ ORG_OSGI_UTIL_CDMA_ESN + " set.");
			return Condition.FALSE;
		}
		return ESN.startsWith(esn) ? Condition.TRUE : Condition.FALSE;
	}
}
