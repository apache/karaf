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

package org.osgi.framework;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.x500.X500Principal;

/**
 * Framework Utility class.
 * 
 * <p>
 * This class contains utility methods which access Framework functions that may
 * be useful to bundles.
 * 
 * @since 1.3
 * @ThreadSafe
 * @version $Revision: 7761 $
 */
public class FrameworkUtil {
	/**
	 * FrameworkUtil objects may not be constructed.
	 */
	private FrameworkUtil() {
		// private empty constructor to prevent construction
	}

	/**
	 * Creates a <code>Filter</code> object. This <code>Filter</code> object may
	 * be used to match a <code>ServiceReference</code> object or a
	 * <code>Dictionary</code> object.
	 * 
	 * <p>
	 * If the filter cannot be parsed, an {@link InvalidSyntaxException} will be
	 * thrown with a human readable message where the filter became unparsable.
	 * 
	 * <p>
	 * This method returns a Filter implementation which may not perform as well
	 * as the framework implementation-specific Filter implementation returned
	 * by {@link BundleContext#createFilter(String)}.
	 * 
	 * @param filter The filter string.
	 * @return A <code>Filter</code> object encapsulating the filter string.
	 * @throws InvalidSyntaxException If <code>filter</code> contains an invalid
	 *         filter string that cannot be parsed.
	 * @throws NullPointerException If <code>filter</code> is null.
	 * 
	 * @see Filter
	 */
	public static Filter createFilter(String filter)
			throws InvalidSyntaxException {
		return new org.apache.felix.framework.FilterImpl(filter);
	}

	/**
	 * Match a Distinguished Name (DN) chain against a pattern. DNs can be
	 * matched using wildcards. A wildcard ('*' &#92;u002A) replaces all
	 * possible values. Due to the structure of the DN, the comparison is more
	 * complicated than string-based wildcard matching.
	 * <p>
	 * A wildcard can stand for zero or more DNs in a chain, a number of
	 * relative distinguished names (RDNs) within a DN, or the value of a single
	 * RDN. The DNs in the chain and the matching pattern are canonicalized
	 * before processing. This means, among other things, that spaces must be
	 * ignored, except in values.
	 * <p>
	 * The format of a wildcard match pattern is:
	 * 
	 * <pre>
	 * matchPattern	::= dn-match ( ';' dn-match ) *
	 * dn-match 	::= ( '*' | rdn-match ) ( ',' rdn-match ) * | '-'
	 * rdn-match 	::= name '=' value-match
	 * value-match 	::= '*' | value-star
	 * value-star 	::= &lt; value, requires escaped '*' and '-' &gt;
	 * </pre>
	 * <p>
	 * The most simple case is a single wildcard; it must match any DN. A
	 * wildcard can also replace the first list of RDNs of a DN. The first RDNs
	 * are the least significant. Such lists of matched RDNs can be empty.
	 * <p>
	 * For example, a match pattern with a wildcard that matches all all DNs
	 * that end with RDNs of o=ACME and c=US would look like this:
	 * 
	 * <pre>
	 * *, o=ACME, c=US
	 * </pre>
	 * 
	 * This match pattern would match the following DNs:
	 * 
	 * <pre>
	 * cn = Bugs Bunny, o = ACME, c = US
	 * ou = Carrots, cn=Daffy Duck, o=ACME, c=US
	 * street = 9C\, Avenue St. Drézéry, o=ACME, c=US
	 * dc=www, dc=acme, dc=com, o=ACME, c=US
	 * o=ACME, c=US
	 * </pre>
	 * 
	 * The following DNs would not match:
	 * 
	 * <pre>
	 * street = 9C\, Avenue St. Drézéry, o=ACME, c=FR
	 * dc=www, dc=acme, dc=com, c=US
	 * </pre>
	 * 
	 * If a wildcard is used for a value of an RDN, the value must be exactly *.
	 * The wildcard must match any value, and no substring matching must be
	 * done. For example:
	 * 
	 * <pre>
	 * cn=*,o=ACME,c=*
	 * </pre>
	 * 
	 * This match pattern with wildcard must match the following DNs:
	 * 
	 * <pre>
	 * cn=Bugs Bunny,o=ACME,c=US
	 * cn = Daffy Duck , o = ACME , c = US
	 * cn=Road Runner, o=ACME, c=NL
	 * </pre>
	 * 
	 * But not:
	 * 
	 * <pre>
	 * o=ACME, c=NL
	 * dc=acme.com, cn=Bugs Bunny, o=ACME, c=US
	 * </pre>
	 * 
	 * <p>
	 * A match pattern may contain a chain of DN match patterns. The
	 * semicolon(';' &#92;u003B) must be used to separate DN match patterns in a
	 * chain. Wildcards can also be used to match against a complete DN within a
	 * chain.
	 * <p>
	 * The following example matches a certificate signed by Tweety Inc. in the
	 * US.
	 * </p>
	 * 
	 * <pre>
	 * * ; ou=S &amp; V, o=Tweety Inc., c=US
	 * </pre>
	 * <p>
	 * The wildcard ('*') matches zero or one DN in the chain, however,
	 * sometimes it is necessary to match a longer chain. The minus sign ('-'
	 * &#92;u002D) represents zero or more DNs, whereas the asterisk only
	 * represents a single DN. For example, to match a DN where the Tweety Inc.
	 * is in the DN chain, use the following expression:
	 * </p>
	 * 
	 * <pre>
	 * - ; *, o=Tweety Inc., c=US
	 * </pre>
	 * 
	 * @param matchPattern The pattern against which to match the DN chain.
	 * @param dnChain The DN chain to match against the specified pattern. Each
	 *        element of the chain must be of type <code>String</code> and use
	 *        the format defined in RFC 2253.
	 * @return <code>true</code> If the pattern matches the DN chain; otherwise
	 *         <code>false</code> is returned.
	 * @throws IllegalArgumentException If the specified match pattern or DN
	 *         chain is invalid.
	 * @since 1.5
	 */
	public static boolean matchDistinguishedNameChain(String matchPattern,
			List /* <String> */dnChain) {
		return DNChainMatching.match(matchPattern, dnChain);
	}

	/**
	 * Return a <code>Bundle</code> for the specified bundle class. The returned
	 * <code>Bundle</code> is the bundle associated with the bundle class loader
	 * which defined the specified class.
	 * 
	 * @param classFromBundle A class defined by a bundle class loader.
	 * @return A <code>Bundle</code> for the specified bundle class or
	 *         <code>null</code> if the specified class was not defined by a
	 *         bundle class loader.
	 * @since 1.5
	 */
	public static Bundle getBundle(final Class classFromBundle) {
		// We use doPriv since the caller may not have permission
		// to call getClassLoader.
		Object cl = AccessController.doPrivileged(new PrivilegedAction() {
			public Object run() {
				return classFromBundle.getClassLoader();
			}
		});

		if (cl instanceof BundleReference) {
			return ((BundleReference) cl).getBundle();
		}
		return null;
	}

	/**
	 * This class contains a method to match a distinguished name (DN) chain
	 * against and DN chain pattern.
	 * <p>
	 * The format of DNs are given in RFC 2253. We represent a signature chain
	 * for an X.509 certificate as a semicolon separated list of DNs. This is
	 * what we refer to as the DN chain. Each DN is made up of relative
	 * distinguished names (RDN) which in turn are made up of key value pairs.
	 * For example:
	 * 
	 * <pre>
	 *   cn=ben+ou=research,o=ACME,c=us;ou=Super CA,c=CA
	 * </pre>
	 * 
	 * is made up of two DNs: "<code>cn=ben+ou=research,o=ACME,c=us</code>
	 * " and " <code>ou=Super CA,c=CA</code>
	 * ". The first DN is made of of three RDNs: "
	 * <code>cn=ben+ou=research</code>" and "<code>o=ACME</code>" and "
	 * <code>c=us</code>". The first RDN has two name value pairs: "
	 * <code>cn=ben</code>" and "<code>ou=research</code>".
	 * <p>
	 * A chain pattern makes use of wildcards ('*' or '-') to match against DNs,
	 * and wildcards ('*') to match againts DN prefixes, and value. If a DN in a
	 * match pattern chain is made up of a wildcard ("*"), that wildcard will
	 * match zero or one DNs in the chain. If a DN in a match pattern chain is
	 * made up of a wildcard ("-"), that wildcard will match zero or more DNs in
	 * the chain. If the first RDN of a DN is the wildcard ("*"), that DN will
	 * match any other DN with the same suffix (the DN with the wildcard RDN
	 * removed). If a value of a name/value pair is a wildcard ("*"), the value
	 * will match any value for that name.
	 */
	private static class DNChainMatching {
		private static final String	MINUS_WILDCARD	= "-";
		private static final String	STAR_WILDCARD	= "*";

		/**
		 * Check the name/value pairs of the rdn against the pattern.
		 * 
		 * @param rdn List of name value pairs for a given RDN.
		 * @param rdnPattern List of name value pattern pairs.
		 * @return true if the list of name value pairs match the pattern.
		 */
		private static boolean rdnmatch(List rdn, List rdnPattern) {
			if (rdn.size() != rdnPattern.size()) {
				return false;
			}
			for (int i = 0; i < rdn.size(); i++) {
				String rdnNameValue = (String) rdn.get(i);
				String patNameValue = (String) rdnPattern.get(i);
				int rdnNameEnd = rdnNameValue.indexOf('=');
				int patNameEnd = patNameValue.indexOf('=');
				if (rdnNameEnd != patNameEnd
						|| !rdnNameValue.regionMatches(0, patNameValue, 0,
								rdnNameEnd)) {
					return false;
				}
				String patValue = patNameValue.substring(patNameEnd);
				String rdnValue = rdnNameValue.substring(rdnNameEnd);
				if (!rdnValue.equals(patValue) && !patValue.equals("=*")
						&& !patValue.equals("=#16012a")) {
					return false;
				}
			}
			return true;
		}

		private static boolean dnmatch(List dn, List dnPattern) {
			int dnStart = 0;
			int patStart = 0;
			int patLen = dnPattern.size();
			if (patLen == 0) {
				return false;
			}
			if (dnPattern.get(0).equals(STAR_WILDCARD)) {
				patStart = 1;
				patLen--;
			}
			if (dn.size() < patLen) {
				return false;
			}
			else {
				if (dn.size() > patLen) {
					if (!dnPattern.get(0).equals(STAR_WILDCARD)) {
						// If the number of rdns do not match we must have a
						// prefix map
						return false;
					}
					// The rdnPattern and rdn must have the same number of
					// elements
					dnStart = dn.size() - patLen;
				}
			}
			for (int i = 0; i < patLen; i++) {
				if (!rdnmatch((List) dn.get(i + dnStart), (List) dnPattern
						.get(i + patStart))) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Parses a distinguished name chain pattern and returns a List where
		 * each element represents a distinguished name (DN) in the chain of
		 * DNs. Each element will be either a String, if the element represents
		 * a wildcard ("*" or "-"), or a List representing an RDN. Each element
		 * in the RDN List will be a String, if the element represents a
		 * wildcard ("*"), or a List of Strings, each String representing a
		 * name/value pair in the RDN.
		 * 
		 * @param dnChain
		 * @return a list of DNs.
		 * @throws IllegalArgumentException
		 */
		private static List parseDNchainPattern(String dnChain) {
			if (dnChain == null) {
				throw new IllegalArgumentException(
						"The DN chain must not be null.");
			}
			List parsed = new ArrayList();
			int startIndex = 0;
			startIndex = skipSpaces(dnChain, startIndex);
			while (startIndex < dnChain.length()) {
				int endIndex = startIndex;
				boolean inQuote = false;
				out: while (endIndex < dnChain.length()) {
					char c = dnChain.charAt(endIndex);
					switch (c) {
						case '"' :
							inQuote = !inQuote;
							break;
						case '\\' :
							endIndex++; // skip the escaped char
							break;
						case ';' :
							if (!inQuote)
								break out;
					}
					endIndex++;
				}
				if (endIndex > dnChain.length()) {
					throw new IllegalArgumentException("unterminated escape");
				}
				parsed.add(dnChain.substring(startIndex, endIndex));
				startIndex = endIndex + 1;
				startIndex = skipSpaces(dnChain, startIndex);
			}
			return parseDNchain(parsed);
		}

		private static List parseDNchain(List chain) {
			if (chain == null) {
				throw new IllegalArgumentException("DN chain must not be null.");
			}
			chain = new ArrayList(chain);
			// Now we parse is a list of strings, lets make List of rdn out
			// of them
			for (int i = 0; i < chain.size(); i++) {
				String dn = (String) chain.get(i);
				if (dn.equals(STAR_WILDCARD) || dn.equals(MINUS_WILDCARD)) {
					continue;
				}
				List rdns = new ArrayList();
				if (dn.charAt(0) == '*') {
					if (dn.charAt(1) != ',') {
						throw new IllegalArgumentException(
								"invalid wildcard prefix");
					}
					rdns.add(STAR_WILDCARD);
					dn = new X500Principal(dn.substring(2))
							.getName(X500Principal.CANONICAL);
				}
				else {
					dn = new X500Principal(dn).getName(X500Principal.CANONICAL);
				}
				// Now dn is a nice CANONICAL DN
				parseDN(dn, rdns);
				chain.set(i, rdns);
			}
			if (chain.size() == 0) {
				throw new IllegalArgumentException("empty DN chain");
			}
			return chain;
		}

		/**
		 * Increment startIndex until the end of dnChain is hit or until it is
		 * the index of a non-space character.
		 */
		private static int skipSpaces(String dnChain, int startIndex) {
			while (startIndex < dnChain.length()
					&& dnChain.charAt(startIndex) == ' ') {
				startIndex++;
			}
			return startIndex;
		}

		/**
		 * Takes a distinguished name in canonical form and fills in the
		 * rdnArray with the extracted RDNs.
		 * 
		 * @param dn the distinguished name in canonical form.
		 * @param rdn the list to fill in with RDNs extracted from the dn
		 * @throws IllegalArgumentException if a formatting error is found.
		 */
		private static void parseDN(String dn, List rdn) {
			int startIndex = 0;
			char c = '\0';
			List nameValues = new ArrayList();
			while (startIndex < dn.length()) {
				int endIndex;
				for (endIndex = startIndex; endIndex < dn.length(); endIndex++) {
					c = dn.charAt(endIndex);
					if (c == ',' || c == '+') {
						break;
					}
					if (c == '\\') {
						endIndex++; // skip the escaped char
					}
				}
				if (endIndex > dn.length()) {
					throw new IllegalArgumentException("unterminated escape "
							+ dn);
				}
				nameValues.add(dn.substring(startIndex, endIndex));
				if (c != '+') {
					rdn.add(nameValues);
					if (endIndex != dn.length()) {
						nameValues = new ArrayList();
					}
					else {
						nameValues = null;
					}
				}
				startIndex = endIndex + 1;
			}
			if (nameValues != null) {
				throw new IllegalArgumentException("improperly terminated DN "
						+ dn);
			}
		}

		/**
		 * This method will return an 'index' which points to a non-wildcard DN
		 * or the end-of-list.
		 */
		private static int skipWildCards(List dnChainPattern,
				int dnChainPatternIndex) {
			int i;
			for (i = dnChainPatternIndex; i < dnChainPattern.size(); i++) {
				Object dnPattern = dnChainPattern.get(i);
				if (dnPattern instanceof String) {
					if (!dnPattern.equals(STAR_WILDCARD)
							&& !dnPattern.equals(MINUS_WILDCARD)) {
						throw new IllegalArgumentException(
								"expected wildcard in DN pattern");
					}
					// otherwise continue skipping over wild cards
				}
				else {
					if (dnPattern instanceof List) {
						// if its a list then we have our 'non-wildcard' DN
						break;
					}
					else {
						// unknown member of the DNChainPattern
						throw new IllegalArgumentException(
								"expected String or List in DN Pattern");
					}
				}
			}
			// i either points to end-of-list, or to the first
			// non-wildcard pattern after dnChainPatternIndex
			return i;
		}

		/**
		 * recursively attempt to match the DNChain, and the DNChainPattern
		 * where DNChain is of the format: "DN;DN;DN;" and DNChainPattern is of
		 * the format: "DNPattern;*;DNPattern" (or combinations of this)
		 */
		private static boolean dnChainMatch(List dnChain, int dnChainIndex,
				List dnChainPattern, int dnChainPatternIndex)
				throws IllegalArgumentException {
			if (dnChainIndex >= dnChain.size()) {
				return false;
			}
			if (dnChainPatternIndex >= dnChainPattern.size()) {
				return false;
			}
			// check to see what the pattern starts with
			Object dnPattern = dnChainPattern.get(dnChainPatternIndex);
			if (dnPattern instanceof String) {
				if (!dnPattern.equals(STAR_WILDCARD)
						&& !dnPattern.equals(MINUS_WILDCARD)) {
					throw new IllegalArgumentException(
							"expected wildcard in DN pattern");
				}
				// here we are processing a wild card as the first DN
				// skip all wildcard DN's
				if (dnPattern.equals(MINUS_WILDCARD)) {
					dnChainPatternIndex = skipWildCards(dnChainPattern,
							dnChainPatternIndex);
				}
				else {
					dnChainPatternIndex++; // only skip the '*' wildcard
				}
				if (dnChainPatternIndex >= dnChainPattern.size()) {
					// return true iff the wild card is '-' or if we are at the
					// end of the chain
					return dnPattern.equals(MINUS_WILDCARD) ? true : dnChain
							.size() - 1 == dnChainIndex;
				}
				//
				// we will now recursively call to see if the rest of the
				// DNChainPattern matches increasingly smaller portions of the
				// rest of the DNChain
				//
				if (dnPattern.equals(STAR_WILDCARD)) {
					// '*' option: only wildcard on 0 or 1
					return dnChainMatch(dnChain, dnChainIndex, dnChainPattern,
							dnChainPatternIndex)
							|| dnChainMatch(dnChain, dnChainIndex + 1,
									dnChainPattern, dnChainPatternIndex);
				}
				for (int i = dnChainIndex; i < dnChain.size(); i++) {
					// '-' option: wildcard 0 or more
					if (dnChainMatch(dnChain, i, dnChainPattern,
							dnChainPatternIndex)) {
						return true;
					}
				}
				// if we are here, then we didn't find a match.. fall through to
				// failure
			}
			else {
				if (dnPattern instanceof List) {
					// here we have to do a deeper check for each DN in the
					// pattern until we hit a wild card
					do {
						if (!dnmatch((List) dnChain.get(dnChainIndex),
								(List) dnPattern)) {
							return false;
						}
						// go to the next set of DN's in both chains
						dnChainIndex++;
						dnChainPatternIndex++;
						// if we finished the pattern then it all matched
						if ((dnChainIndex >= dnChain.size())
								&& (dnChainPatternIndex >= dnChainPattern
										.size())) {
							return true;
						}
						// if the DN Chain is finished, but the pattern isn't
						// finished then if the rest of the pattern is not
						// wildcard then we are done
						if (dnChainIndex >= dnChain.size()) {
							dnChainPatternIndex = skipWildCards(dnChainPattern,
									dnChainPatternIndex);
							// return TRUE iff the pattern index moved past the
							// list-size (implying that the rest of the pattern
							// is all wildcards)
							return dnChainPatternIndex >= dnChainPattern.size();
						}
						// if the pattern finished, but the chain continues then
						// we have a mis-match
						if (dnChainPatternIndex >= dnChainPattern.size()) {
							return false;
						}
						// get the next DN Pattern
						dnPattern = dnChainPattern.get(dnChainPatternIndex);
						if (dnPattern instanceof String) {
							if (!dnPattern.equals(STAR_WILDCARD)
									&& !dnPattern.equals(MINUS_WILDCARD)) {
								throw new IllegalArgumentException(
										"expected wildcard in DN pattern");
							}
							// if the next DN is a 'wildcard', then we will
							// recurse
							return dnChainMatch(dnChain, dnChainIndex,
									dnChainPattern, dnChainPatternIndex);
						}
						else {
							if (!(dnPattern instanceof List)) {
								throw new IllegalArgumentException(
										"expected String or List in DN Pattern");
							}
						}
						// if we are here, then we will just continue to the
						// match the next set of DN's from the DNChain, and the
						// DNChainPattern since both are lists
					} while (true);
					// should never reach here?
				}
				else {
					throw new IllegalArgumentException(
							"expected String or List in DN Pattern");
				}
			}
			// if we get here, the the default return is 'mis-match'
			return false;
		}

		/**
		 * Matches a distinguished name chain against a pattern of a
		 * distinguished name chain.
		 * 
		 * @param dnChain
		 * @param pattern the pattern of distinguished name (DN) chains to match
		 *        against the dnChain. Wildcards ("*" or "-") can be used in
		 *        three cases:
		 *        <ol>
		 *        <li>As a DN. In this case, the DN will consist of just the "*"
		 *        or "-". When "*" is used it will match zero or one DNs. When
		 *        "-" is used it will match zero or more DNs. For example,
		 *        "cn=me,c=US;*;cn=you" will match
		 *        "cn=me,c=US";cn=you" and "cn=me,c=US;cn=her;cn=you". The
		 *        pattern "cn=me,c=US;-;cn=you" will match "cn=me,c=US";cn=you"
		 *        and "cn=me,c=US;cn=her;cn=him;cn=you".
		 *        <li>As a DN prefix. In this case, the DN must start with "*,".
		 *        The wild card will match zero or more RDNs at the start of a
		 *        DN. For example, "*,cn=me,c=US;cn=you" will match
		 *        "cn=me,c=US";cn=you" and
		 *        "ou=my org unit,o=my org,cn=me,c=US;cn=you"</li>
		 *        <li>As a value. In this case the value of a name value pair in
		 *        an RDN will be a "*". The wildcard will match any value for
		 *        the given name. For example, "cn=*,c=US;cn=you" will match
		 *        "cn=me,c=US";cn=you" and "cn=her,c=US;cn=you", but it will not
		 *        match "ou=my org unit,c=US;cn=you". If the wildcard does not
		 *        occur by itself in the value, it will not be used as a
		 *        wildcard. In other words, "cn=m*,c=US;cn=you" represents the
		 *        common name of "m*" not any common name starting with "m".</li>
		 *        </ol>
		 * @return true if dnChain matches the pattern.
		 * @throws IllegalArgumentException
		 */
		static boolean match(String pattern, List/* <String> */dnChain) {
			List parsedDNChain;
			List parsedDNPattern;
			try {
				parsedDNChain = parseDNchain(dnChain);
			}
			catch (RuntimeException e) {
				IllegalArgumentException iae = new IllegalArgumentException(
						"Invalid DN chain: " + toString(dnChain));
				iae.initCause(e);
				throw iae;
			}
			try {
				parsedDNPattern = parseDNchainPattern(pattern);
			}
			catch (RuntimeException e) {
				IllegalArgumentException iae = new IllegalArgumentException(
						"Invalid match pattern: " + pattern);
				iae.initCause(e);
				throw iae;
			}
			return dnChainMatch(parsedDNChain, 0, parsedDNPattern, 0);
		}

		private static String toString(List dnChain) {
			if (dnChain == null) {
				return null;
			}
			StringBuffer sb = new StringBuffer();
			for (Iterator iChain = dnChain.iterator(); iChain.hasNext();) {
				sb.append(iChain.next());
				if (iChain.hasNext()) {
					sb.append("; ");
				}
			}
			return sb.toString();
		}
	}
}
