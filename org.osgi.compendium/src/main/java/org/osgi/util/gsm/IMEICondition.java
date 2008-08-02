/*
 * $Header: /cvshome/build/org.osgi.util.gsm/src/org/osgi/util/gsm/IMEICondition.java,v 1.21 2007/02/19 21:32:28 hargrave Exp $
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
 * Class representing an IMEI condition. Instances of this class contain a
 * string value that is matched against the IMEI of the device.
 */
public class IMEICondition {
	private static final String ORG_OSGI_UTIL_GSM_IMEI = "org.osgi.util.gsm.imei";
	private static final String imei ;
		
	static {
		imei = (String)
		AccessController.doPrivileged(
				new PrivilegedAction() {
					public Object run() {
					return System.getProperty(ORG_OSGI_UTIL_GSM_IMEI);
					}
				}
				);
	}
	
	private IMEICondition() {
	}

	/**
	 * Creates an IMEICondition object.
	 * 
	 * @param bundle ignored, as the IMEI number is the property of the mobile device,
	 * 					and thus the same for all bundles.
	 * @param conditionInfo contains the IMEI value to match the device's IMEI against. Its
	 * 		{@link ConditionInfo#getArgs()} method should return a String array with one value, the
	 * 		IMEI string. The IMEI is 15 digits without hypens. Limited pattern matching is allowed,
	 * 		then the string is 0 to 14 digits, followed by an asterisk(<code>*</code>).
	 * @return An IMEICondition object, that can tell whether its IMEI number matches that of the device.
	 * 			If the number contains an asterisk(<code>*</code>), then the beginning
	 * 			of the imei is compared to the pattern.
	 * @throws NullPointerException if one of the parameters is <code>null</code>.
	 * @throws IllegalArgumentException if the IMEI is not a string of 15 digits, or 
	 * 		0 to 14 digits with an <code>*</code> at the end.
	 */
	public static Condition getCondition(Bundle bundle, ConditionInfo conditionInfo) {
		if (bundle==null) throw new NullPointerException("bundle");
		String imei = conditionInfo.getArgs()[0];
		if (imei.length()>15) throw new IllegalArgumentException("imei too long: "+imei);
		if (imei.endsWith("*")) {
			imei = imei.substring(0,imei.length()-1);
		} else {
			if (imei.length()!=15) throw new IllegalArgumentException("not a valid imei: "+imei);
		}
		for(int i=0;i<imei.length();i++) {
			int c = imei.charAt(i);
			if (c<'0'||c>'9') throw new IllegalArgumentException("not a valid imei: "+imei);
		}
		if (IMEICondition.imei==null) {
			System.err.println("The OSGi Reference Implementation of org.osgi.util.gsm.IMEICondition ");
			System.err.println("needs the system property "+ORG_OSGI_UTIL_GSM_IMEI+" set.");
			return Condition.FALSE;
		}
		return IMEICondition.imei.startsWith(imei)?Condition.TRUE:Condition.FALSE;
	}
}
