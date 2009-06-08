/*
 * Copyright (c) OSGi Alliance (2005, 2009). All Rights Reserved.
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

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * Condition to test if the signer of a bundle matches or does not match a
 * pattern. Since the bundle's signer can only change when the bundle is
 * updated, this condition is immutable.
 * <p>
 * The condition expressed using a single String that specifies a Distinguished
 * Name (DN) chain to match bundle signers against. DN's are encoded using IETF
 * RFC 2253. Usually signers use certificates that are issued by certificate
 * authorities, which also have a corresponding DN and certificate. The
 * certificate authorities can form a chain of trust where the last DN and
 * certificate is known by the framework. The signer of a bundle is expressed as
 * signers DN followed by the DN of its issuer followed by the DN of the next
 * issuer until the DN of the root certificate authority. Each DN is separated
 * by a semicolon.
 * <p>
 * A bundle can satisfy this condition if one of its signers has a DN chain that
 * matches the DN chain used to construct this condition. Wildcards (`*') can be
 * used to allow greater flexibility in specifying the DN chains. Wildcards can
 * be used in place of DNs, RDNs, or the value in an RDN. If a wildcard is used
 * for a value of an RDN, the value must be exactly "*" and will match any value
 * for the corresponding type in that RDN. If a wildcard is used for a RDN, it
 * must be the first RDN and will match any number of RDNs (including zero
 * RDNs).
 * 
 * @ThreadSafe
 * @version $Revision: 6860 $
 */
public class BundleSignerCondition {
	private static final String	CONDITION_TYPE	= "org.osgi.service.condpermadmin.BundleSignerCondition";

	/**
	 * Constructs a Condition that tries to match the passed Bundle's location
	 * to the location pattern.
	 * 
	 * @param bundle The Bundle being evaluated.
	 * @param info The ConditionInfo from which to construct the condition. The
	 *        ConditionInfo must specify one or two arguments. The first
	 *        argument of the ConditionInfo specifies the chain of distinguished
	 *        names pattern to match against the signer of the bundle. The
	 *        Condition is satisfied if the signer of the bundle matches the
	 *        pattern. The second argument of the ConditionInfo is optional. If
	 *        a second argument is present and equal to "!", then the
	 *        satisfaction of the Condition is negated. That is, the Condition
	 *        is satisfied if the signer of the bundle does NOT match the
	 *        pattern. If the second argument is present but does not equal "!",
	 *        then the second argument is ignored.
	 * @return A Condition which checks the signers of the specified bundle.
	 */
	public static Condition getCondition(final Bundle bundle,
			final ConditionInfo info) {
		if (!CONDITION_TYPE.equals(info.getType()))
			throw new IllegalArgumentException(
					"ConditionInfo must be of type \"" + CONDITION_TYPE + "\"");
		String[] args = info.getArgs();
		if (args.length != 1 && args.length != 2)
			throw new IllegalArgumentException("Illegal number of args: "
					+ args.length);

		Map/* <X509Certificate, List<X509Certificate>> */signers = bundle
				.getSignerCertificates(Bundle.SIGNERS_TRUSTED);
		boolean match = false;
		for (Iterator iSigners = signers.values().iterator(); iSigners
				.hasNext();) {
			List/* <X509Certificate> */signerCerts = (List) iSigners.next();
			List/* <String> */dnChain = new ArrayList(signerCerts.size());
			for (Iterator iCerts = signerCerts.iterator(); iCerts.hasNext();) {
				dnChain.add(((X509Certificate) iCerts.next()).getSubjectDN()
						.getName());
			}
			if (FrameworkUtil.matchDistinguishedNameChain(args[0], dnChain)) {
				match = true;
				break;
			}
		}

		boolean negate = (args.length == 2) ? "!".equals(args[1]) : false;
		return negate ^ match ? Condition.TRUE : Condition.FALSE;
	}

	private BundleSignerCondition() {
		// private constructor to prevent objects of this type
	}
}
