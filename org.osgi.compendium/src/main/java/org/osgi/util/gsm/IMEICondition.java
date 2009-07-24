/*
 * Copyright (c) OSGi Alliance (2004, 2009). All Rights Reserved.
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
package org.osgi.util.gsm;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;
import org.osgi.service.condpermadmin.Condition;
import org.osgi.service.condpermadmin.ConditionInfo;

/**
 * Class representing an IMEI condition. Instances of this class contain a
 * string value that is matched against the IMEI of the device.
 * 
 * @ThreadSafe
 * @version $Revision: 6439 $
 */
public class IMEICondition {
	private static final String	ORG_OSGI_UTIL_GSM_IMEI	= "org.osgi.util.gsm.imei";
	private static final String	IMEI;
	private static final int	IMEI_LENGTH				= 15;

	static {
		IMEI = (String) AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return System.getProperty(ORG_OSGI_UTIL_GSM_IMEI);
			}
		});
	}

	private IMEICondition() {
		// prevent instances being constructed
	}

	/**
	 * Creates an IMEI condition object.
	 * 
	 * @param bundle This parameter is ignored, as the IMEI number is a property
	 *        of the mobile device and thus is the same for all bundles.
	 * @param conditionInfo Contains the IMEI value against which to match the
	 *        device's IMEI. Its {@link ConditionInfo#getArgs()} method should
	 *        return a String array with one value: the IMEI string. The IMEI is
	 *        15 digits without hyphens. Limited pattern matching is allowed:
	 *        the string is 0 to 14 digits, followed by an asterisk (
	 *        <code>*</code>).
	 * @return A Condition object that indicates whether the specified IMEI
	 *         number matches that of the device. If the number ends with an
	 *         asterisk ( <code>*</code>), then the beginning of the IMEI is
	 *         compared to the pattern.
	 * @throws IllegalArgumentException If the IMEI is not a string of 15
	 *         digits, or 0 to 14 digits with an <code>*</code> at the end.
	 */
	public static Condition getCondition(Bundle bundle,
			ConditionInfo conditionInfo) {
		String imei = conditionInfo.getArgs()[0];
		int length = imei.length();
		if (length > IMEI_LENGTH) {
			throw new IllegalArgumentException("IMEI too long: " + imei);
		}
		if (imei.endsWith("*")) {
			length--;
			imei = imei.substring(0, length);
		}
		else {
			if (length < IMEI_LENGTH) {
				throw new IllegalArgumentException("IMEI too short: " + imei);
			}
		}
		for (int i = 0; i < length; i++) {
			char c = imei.charAt(i);
			if (('0' <= c) && (c <= '9')) {
				continue;
			}
			throw new IllegalArgumentException("not a valid IMEI: " + imei);
		}
		if (IMEI == null) {
			System.err
					.println("The OSGi implementation of org.osgi.util.gsm.IMEICondition needs the system property "
							+ ORG_OSGI_UTIL_GSM_IMEI + " set.");
			return Condition.FALSE;
		}
		return IMEI.startsWith(imei) ? Condition.TRUE : Condition.FALSE;
	}
}
