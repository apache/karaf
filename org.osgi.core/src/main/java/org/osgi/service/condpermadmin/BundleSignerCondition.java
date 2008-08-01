/*
 * $Header: /cvshome/build/org.osgi.service.condpermadmin/src/org/osgi/service/condpermadmin/BundleSignerCondition.java,v 1.13 2006/10/24 17:54:27 hargrave Exp $
 * 
 * Copyright (c) OSGi Alliance (2005, 2006). All Rights Reserved.
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

import java.lang.reflect.*;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.framework.Bundle;

/**
 * Condition to test if the signer of a bundle matches a pattern. Since the bundle's signer can
 * only change when the bundle is updated, this condition is immutable.
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
 * @version $Revision: 1.13 $
 */
public class BundleSignerCondition {
	/*
	 * NOTE: A framework implementor may also choose to replace this class in
	 * their distribution with a class that directly interfaces with the
	 * framework implementation. This replacement class MUST NOT alter the
	 * public/protected signature of this class.
	 */

	/*
	 * This class will load the BundleSignerCondition class in the package named
	 * by the org.osgi.vendor.condpermadmin package. This class will delegate
	 * getCondition methods calls to the vendor BundleSignerCondition class.
	 */
	
	private static class ImplHolder implements PrivilegedAction {
		private static final String	packageProperty	= "org.osgi.vendor.condpermadmin";
		static final Method	getCondition;
		static {
			getCondition = (Method) AccessController.doPrivileged(new ImplHolder());
		}

		private ImplHolder() {
		}

		public Object run() {
			String packageName = System
			.getProperty(packageProperty);
			if (packageName == null) {
				throw new NoClassDefFoundError(packageProperty
						+ " property not set");
			}
			
			Class delegateClass;
			try {
				delegateClass = Class.forName(packageName
						+ ".BundleSignerCondition");
			}
			catch (ClassNotFoundException e) {
				throw new NoClassDefFoundError(e.toString());
			}
			
			Method result;
			try {
				result = delegateClass.getMethod("getCondition",
						new Class[] {Bundle.class,
						ConditionInfo.class		});
			}
			catch (NoSuchMethodException e) {
				throw new NoSuchMethodError(e.toString());
			}
			
			if (!Modifier.isStatic(result.getModifiers())) {
				throw new NoSuchMethodError(
						"getCondition method must be static");
			}
			
			return result;
		}
	}
	
	private static final String	CONDITION_TYPE	= "org.osgi.service.condpermadmin.BundleSignerCondition";

	/**
	 * Constructs a Condition that tries to match the passed Bundle's location
	 * to the location pattern.
	 * 
	 * @param bundle The Bundle being evaluated.
	 * @param info The ConditionInfo to construct the condition for. The args of
	 *        the ConditionInfo specify a single String specifying the chain of
	 *        distinguished names pattern to match against the signer of the
	 *        Bundle.
	 * @return A Condition which checks the signers of the specified bundle.        
	 */
	public static Condition getCondition(Bundle bundle, ConditionInfo info) {
		if (!CONDITION_TYPE.equals(info.getType()))
			throw new IllegalArgumentException(
					"ConditionInfo must be of type \"" + CONDITION_TYPE + "\"");
		String[] args = info.getArgs();
		if (args.length != 1)
			throw new IllegalArgumentException("Illegal number of args: "
					+ args.length);

		try {
			try {
				return (Condition) ImplHolder.getCondition.invoke(null, new Object[] {
						bundle, info});
			}
			catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}
		catch (Error e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Throwable e) {
			throw new RuntimeException(e.toString());
		}
	}

	private BundleSignerCondition() {
		// private constructor to prevent objects of this type
	}
}
