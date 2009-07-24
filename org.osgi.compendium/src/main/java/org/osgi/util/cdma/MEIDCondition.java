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
 * Class representing an MEID condition. Instances of this class contain a
 * string value that is matched against the MEID of the device.
 * 
 * @ThreadSafe
 * @version $Revision: 6439 $
 */
public class MEIDCondition {
	private static final String	ORG_OSGI_UTIL_CDMA_MEID	= "org.osgi.util.cdma.meid";
	private static final String	MEID;
	private static final int	MEID_LENGTH				= 14;

	static {
		MEID = (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				String meid = System.getProperty(ORG_OSGI_UTIL_CDMA_MEID);
				if (meid == null) {
					return null;
				}
				return meid.toUpperCase();
			}
		});
	}

	private MEIDCondition() {
		// prevent instances being constructed
	}

	/**
	 * Creates a MEIDCondition object.
	 * 
	 * @param bundle This parameter is ignored, as the MEID number is the
	 *        property of the mobile device, and thus the same for all bundles.
	 * @param conditionInfo Contains the MEID value against which to match the
	 *        device's MEID. Its {@link ConditionInfo#getArgs()} method should
	 *        return a String array with one value, the MEID string. The MEID is
	 *        14 hexadecimal digits (56 bits) without hyphens. Limited pattern
	 *        matching is allowed: the string is 0 to 13 digits, followed by an
	 *        asterisk(<code>*</code>).
	 * @return A Condition object that indicates whether the specified MEID
	 *         number matches that of the device. If the number ends with an
	 *         asterisk ( <code>*</code>), then the beginning of the MEID is
	 *         compared to the pattern.
	 * @throws IllegalArgumentException If the MEID is not a string of 14
	 *         hexadecimal digits, or 0 to 13 hexadecimal digits with an
	 *         <code>*</code> at the end.
	 */
	public static Condition getCondition(Bundle bundle,
			ConditionInfo conditionInfo) {
		String meid = conditionInfo.getArgs()[0].toUpperCase();
		int length = meid.length();
		if (length > MEID_LENGTH) {
			throw new IllegalArgumentException("MEID too long: " + meid);
		}
		if (meid.endsWith("*")) {
			length--;
			meid = meid.substring(0, length);
		}
		else {
			if (length < MEID_LENGTH) {
				throw new IllegalArgumentException("MEID too short: " + meid);
			}
		}
		for (int i = 0; i < length; i++) {
			char c = meid.charAt(i);
			if (('0' <= c) && (c <= '9')) {
				continue;
			}
			if (('A' <= c) && (c <= 'F')) {
				continue;
			}
			throw new IllegalArgumentException("not a valid MEID: " + meid);
		}
		if (MEID == null) {
			System.err
					.println("The OSGi implementation of org.osgi.util.cdma.MEIDCondition needs the system property "
							+ ORG_OSGI_UTIL_CDMA_MEID + " set.");
			return Condition.FALSE;
		}
		return MEID.startsWith(meid) ? Condition.TRUE : Condition.FALSE;
	}
}
