/*
 * Copyright (c) OSGi Alliance (2009). All Rights Reserved.
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

package org.osgi.framework;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Package private class used by permissions for filter matching on signer key
 * during filter expression evaluation in the permission implies method.
 * 
 * @Immutable
 * @version $Revision: 6479 $
 */
class SignerProperty {
	private final Bundle	bundle;
	private final String	pattern;

	/**
	 * String constructor used by the filter matching algorithm to construct a
	 * SignerProperty from the attribute value in a filter expression.
	 * 
	 * @param pattern Attribute value in the filter expression.
	 */
	public SignerProperty(String pattern) {
		this.pattern = pattern;
		this.bundle = null;
	}

	/**
	 * Used by the permission implies method to build the properties for a
	 * filter match.
	 * 
	 * @param bundle The bundle whose signers are to be matched.
	 */
	SignerProperty(Bundle bundle) {
		this.bundle = bundle;
		this.pattern = null;
	}

	/**
	 * Used by the filter matching algorithm. This methods does NOT satisfy the
	 * normal equals contract. Since the class is only used in filter expression
	 * evaluations, it only needs to support comparing an instance created with
	 * a Bundle to an instance created with a pattern string from the filter
	 * expression.
	 * 
	 * @param o SignerProperty to compare against.
	 * @return true if the DN name chain matches the pattern.
	 */
	public boolean equals(Object o) {
		if (!(o instanceof SignerProperty))
			return false;
		SignerProperty other = (SignerProperty) o;
		Bundle matchBundle = bundle != null ? bundle : other.bundle;
		String matchPattern = bundle != null ? other.pattern : pattern;
		Map/* <X509Certificate, List<X509Certificate>> */signers = matchBundle
				.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
		for (Iterator iSigners = signers.values().iterator(); iSigners
				.hasNext();) {
			List/* <X509Certificate> */signerCerts = (List) iSigners.next();
			List/* <String> */dnChain = new ArrayList(signerCerts.size());
			for (Iterator iCerts = signerCerts.iterator(); iCerts.hasNext();) {
				dnChain.add(((X509Certificate) iCerts.next()).getSubjectDN()
						.getName());
			}
			if (FrameworkUtil
					.matchDistinguishedNameChain(matchPattern, dnChain)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Since the equals method does not obey the general equals contract, this
	 * method cannot generate hash codes which obey the equals contract.
	 */
	public int hashCode() {
		return 31;
	}

	/**
	 * Check if the bundle is signed.
	 * 
	 * @return true if constructed with a bundle that is signed.
	 */
	boolean isBundleSigned() {
		if (bundle == null) {
			return false;
		}
		Map/* <X509Certificate, List<X509Certificate>> */signers = bundle
				.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
		return !signers.isEmpty();
	}
}
