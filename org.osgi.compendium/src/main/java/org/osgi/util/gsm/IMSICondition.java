/*
 * $Header: /cvshome/build/org.osgi.util.gsm/src/org/osgi/util/gsm/IMSICondition.java,v 1.23 2007/02/19 21:32:28 hargrave Exp $
 *
 * Copyright (c) OSGi Alliance (2004, 2006). All Rights Reserved.
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
 * Class representing an IMSI condition. Instances of this class contain a
 * string value that is matched against the IMSI of the subscriber.
 */
public class IMSICondition {
	private static final String ORG_OSGI_UTIL_GSM_IMSI = "org.osgi.util.gsm.imsi";
	private static final String imsi;
	
	static {
		imsi = (String)
		AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
					return System.getProperty(ORG_OSGI_UTIL_GSM_IMSI);
					}
				}
				);
	}

	private IMSICondition() {}

	/**
	 * Creates an IMSI condition object.
	 * 
	 * @param bundle ignored, as the IMSI number is the same for all bundles.
	 * @param conditionInfo contains the IMSI value to match the device's IMSI against. Its
	 * 		{@link ConditionInfo#getArgs()} method should return a String array with one value, the
	 * 		IMSI string. The IMSI is 15 digits without hypens. Limited pattern matching is allowed,
	 * 		then the string is 0 to 14 digits, followed by an asterisk(<code>*</code>).
	 * @return An IMSICondition object, that can tell whether its IMSI number matches that of the device.
	 * 			If the number contains an asterisk(<code>*</code>), then the beginning
	 * 			of the IMSI is compared to the pattern.
	 * @throws NullPointerException if one of the parameters is <code>null</code>.
	 * @throws IllegalArgumentException if the IMSI is not a string of 15 digits, or 
	 * 		0 to 14 digits with an <code>*</code> at the end.
	 */
	public static Condition getCondition(Bundle bundle, ConditionInfo conditionInfo) {
		if (bundle==null) throw new NullPointerException("bundle");
		if (conditionInfo==null) throw new NullPointerException("conditionInfo");
		String imsi = conditionInfo.getArgs()[0];
		if (imsi.length()>15) throw new IllegalArgumentException("imsi too long: "+imsi);
		if (imsi.endsWith("*")) {
			imsi = imsi.substring(0,imsi.length()-1);
		} else {
			if (imsi.length()!=15) throw new IllegalArgumentException("not a valid imei: "+imsi);
		}
		for(int i=0;i<imsi.length();i++) {
			int c = imsi.charAt(i);
			if (c<'0'||c>'9') throw new IllegalArgumentException("not a valid imei: "+imsi);
		}
		if (IMSICondition.imsi==null) {
			System.err.println("The OSGi Reference Implementation of org.osgi.util.gsm.IMSICondition ");
			System.err.println("needs the system property "+ORG_OSGI_UTIL_GSM_IMSI+" set.");
			return Condition.FALSE;
		}
		return (IMSICondition.imsi.startsWith(imsi))?Condition.TRUE:Condition.FALSE;
	}
}
