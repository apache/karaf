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
		return FilterImpl.newInstance(filter);
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
	 * RFC 1960-based Filter. Filter objects can be created by calling the
	 * constructor with the desired filter string. A Filter object can be called
	 * numerous times to determine if the match argument matches the filter
	 * string that was used to create the Filter object.
	 * 
	 * <p>
	 * The syntax of a filter string is the string representation of LDAP search
	 * filters as defined in RFC 1960: <i>A String Representation of LDAP Search
	 * Filters</i> (available at http://www.ietf.org/rfc/rfc1960.txt). It should
	 * be noted that RFC 2254: <i>A String Representation of LDAP Search
	 * Filters</i> (available at http://www.ietf.org/rfc/rfc2254.txt) supersedes
	 * RFC 1960 but only adds extensible matching and is not applicable for this
	 * API.
	 * 
	 * <p>
	 * The string representation of an LDAP search filter is defined by the
	 * following grammar. It uses a prefix format.
	 * 
	 * <pre>
	 *   &lt;filter&gt; ::= '(' &lt;filtercomp&gt; ')'
	 *   &lt;filtercomp&gt; ::= &lt;and&gt; | &lt;or&gt; | &lt;not&gt; | &lt;item&gt;
	 *   &lt;and&gt; ::= '&amp;' &lt;filterlist&gt;
	 *   &lt;or&gt; ::= '|' &lt;filterlist&gt;
	 *   &lt;not&gt; ::= '!' &lt;filter&gt;
	 *   &lt;filterlist&gt; ::= &lt;filter&gt; | &lt;filter&gt; &lt;filterlist&gt;
	 *   &lt;item&gt; ::= &lt;simple&gt; | &lt;present&gt; | &lt;substring&gt;
	 *   &lt;simple&gt; ::= &lt;attr&gt; &lt;filtertype&gt; &lt;value&gt;
	 *   &lt;filtertype&gt; ::= &lt;equal&gt; | &lt;approx&gt; | &lt;greater&gt; | &lt;less&gt;
	 *   &lt;equal&gt; ::= '='
	 *   &lt;approx&gt; ::= '&tilde;='
	 *   &lt;greater&gt; ::= '&gt;='
	 *   &lt;less&gt; ::= '&lt;='
	 *   &lt;present&gt; ::= &lt;attr&gt; '=*'
	 *   &lt;substring&gt; ::= &lt;attr&gt; '=' &lt;initial&gt; &lt;any&gt; &lt;final&gt;
	 *   &lt;initial&gt; ::= NULL | &lt;value&gt;
	 *   &lt;any&gt; ::= '*' &lt;starval&gt;
	 *   &lt;starval&gt; ::= NULL | &lt;value&gt; '*' &lt;starval&gt;
	 *   &lt;final&gt; ::= NULL | &lt;value&gt;
	 * </pre>
	 * 
	 * <code>&lt;attr&gt;</code> is a string representing an attribute, or key,
	 * in the properties objects of the registered services. Attribute names are
	 * not case sensitive; that is cn and CN both refer to the same attribute.
	 * <code>&lt;value&gt;</code> is a string representing the value, or part of
	 * one, of a key in the properties objects of the registered services. If a
	 * <code>&lt;value&gt;</code> must contain one of the characters '
	 * <code>*</code>' or '<code>(</code>' or '<code>)</code>', these characters
	 * should be escaped by preceding them with the backslash '<code>\</code>'
	 * character. Note that although both the <code>&lt;substring&gt;</code> and
	 * <code>&lt;present&gt;</code> productions can produce the <code>'attr=*'</code>
	 * construct, this construct is used only to denote a presence filter.
	 * 
	 * <p>
	 * Examples of LDAP filters are:
	 * 
	 * <pre>
	 *   &quot;(cn=Babs Jensen)&quot;
	 *   &quot;(!(cn=Tim Howes))&quot;
	 *   &quot;(&amp;(&quot; + Constants.OBJECTCLASS + &quot;=Person)(|(sn=Jensen)(cn=Babs J*)))&quot;
	 *   &quot;(o=univ*of*mich*)&quot;
	 * </pre>
	 * 
	 * <p>
	 * The approximate match (<code>~=</code>) is implementation specific but
	 * should at least ignore case and white space differences. Optional are
	 * codes like soundex or other smart "closeness" comparisons.
	 * 
	 * <p>
	 * Comparison of values is not straightforward. Strings are compared
	 * differently than numbers and it is possible for a key to have multiple
	 * values. Note that that keys in the match argument must always be strings.
	 * The comparison is defined by the object type of the key's value. The
	 * following rules apply for comparison:
	 * 
	 * <blockquote>
	 * <TABLE BORDER=0>
	 * <TR>
	 * <TD><b>Property Value Type </b></TD>
	 * <TD><b>Comparison Type</b></TD>
	 * </TR>
	 * <TR>
	 * <TD>String</TD>
	 * <TD>String comparison</TD>
	 * </TR>
	 * <TR valign=top>
	 * <TD>Integer, Long, Float, Double, Byte, Short, BigInteger, BigDecimal</TD>
	 * <TD>numerical comparison</TD>
	 * </TR>
	 * <TR>
	 * <TD>Character</TD>
	 * <TD>character comparison</TD>
	 * </TR>
	 * <TR>
	 * <TD>Boolean</TD>
	 * <TD>equality comparisons only</TD>
	 * </TR>
	 * <TR>
	 * <TD>[] (array)</TD>
	 * <TD>recursively applied to values</TD>
	 * </TR>
	 * <TR>
	 * <TD>Collection</TD>
	 * <TD>recursively applied to values</TD>
	 * </TR>
	 * </TABLE>
	 * Note: arrays of primitives are also supported. </blockquote>
	 * 
	 * A filter matches a key that has multiple values if it matches at least
	 * one of those values. For example,
	 * 
	 * <pre>
	 * Dictionary d = new Hashtable();
	 * d.put(&quot;cn&quot;, new String[] {&quot;a&quot;, &quot;b&quot;, &quot;c&quot;});
	 * </pre>
	 * 
	 * d will match <code>(cn=a)</code> and also <code>(cn=b)</code>
	 * 
	 * <p>
	 * A filter component that references a key having an unrecognizable data
	 * type will evaluate to <code>false</code> .
	 */
	private static class FilterImpl implements Filter {
		/* filter operators */
		private static final int			EQUAL		= 1;
		private static final int			APPROX		= 2;
		private static final int			GREATER		= 3;
		private static final int			LESS		= 4;
		private static final int			PRESENT		= 5;
		private static final int			SUBSTRING	= 6;
		private static final int			AND			= 7;
		private static final int			OR			= 8;
		private static final int			NOT			= 9;

		/** filter operation */
		private final int					op;
		/** filter attribute or null if operation AND, OR or NOT */
		private final String				attr;
		/** filter operands */
		private final Object				value;

		/* normalized filter string for Filter object */
		private transient volatile String	filterString;

		/**
		 * Constructs a {@link FilterImpl} object. This filter object may be
		 * used to match a {@link ServiceReference} or a Dictionary.
		 * 
		 * <p>
		 * If the filter cannot be parsed, an {@link InvalidSyntaxException}
		 * will be thrown with a human readable message where the filter became
		 * unparsable.
		 * 
		 * @param filterString the filter string.
		 * @exception InvalidSyntaxException If the filter parameter contains an
		 *            invalid filter string that cannot be parsed.
		 */
		static FilterImpl newInstance(String filterString)
				throws InvalidSyntaxException {
			return new Parser(filterString).parse();
		}

		FilterImpl(int operation, String attr, Object value) {
			this.op = operation;
			this.attr = attr;
			this.value = value;
		}

		/**
		 * Filter using a service's properties.
		 * <p>
		 * This <code>Filter</code> is executed using the keys and values of the
		 * referenced service's properties. The keys are case insensitively
		 * matched with this <code>Filter</code>.
		 * 
		 * @param reference The reference to the service whose properties are
		 *        used in the match.
		 * @return <code>true</code> if the service's properties match this
		 *         <code>Filter</code>; <code>false</code> otherwise.
		 */
		public boolean match(ServiceReference reference) {
			return match0(new ServiceReferenceDictionary(reference));
		}

		/**
		 * Filter using a <code>Dictionary</code>. This <code>Filter</code> is
		 * executed using the specified <code>Dictionary</code>'s keys and
		 * values. The keys are case insensitively matched with this
		 * <code>Filter</code>.
		 * 
		 * @param dictionary The <code>Dictionary</code> whose keys are used in
		 *        the match.
		 * @return <code>true</code> if the <code>Dictionary</code>'s keys and
		 *         values match this filter; <code>false</code> otherwise.
		 * @throws IllegalArgumentException If <code>dictionary</code> contains
		 *         case variants of the same key name.
		 */
		public boolean match(Dictionary dictionary) {
			return match0(new CaseInsensitiveDictionary(dictionary));
		}

		/**
		 * Filter with case sensitivity using a <code>Dictionary</code>. This
		 * <code>Filter</code> is executed using the specified
		 * <code>Dictionary</code>'s keys and values. The keys are case
		 * sensitively matched with this <code>Filter</code>.
		 * 
		 * @param dictionary The <code>Dictionary</code> whose keys are used in
		 *        the match.
		 * @return <code>true</code> if the <code>Dictionary</code>'s keys and
		 *         values match this filter; <code>false</code> otherwise.
		 * @since 1.3
		 */
		public boolean matchCase(Dictionary dictionary) {
			return match0(dictionary);
		}

		/**
		 * Returns this <code>Filter</code>'s filter string.
		 * <p>
		 * The filter string is normalized by removing whitespace which does not
		 * affect the meaning of the filter.
		 * 
		 * @return This <code>Filter</code>'s filter string.
		 */
		public String toString() {
			String result = filterString;
			if (result == null) {
				filterString = result = normalize();
			}
			return result;
		}

		/**
		 * Returns this <code>Filter</code>'s normalized filter string.
		 * <p>
		 * The filter string is normalized by removing whitespace which does not
		 * affect the meaning of the filter.
		 * 
		 * @return This <code>Filter</code>'s filter string.
		 */
		private String normalize() {
			StringBuffer sb = new StringBuffer();
			sb.append('(');

			switch (op) {
				case AND : {
					sb.append('&');

					FilterImpl[] filters = (FilterImpl[]) value;
					for (int i = 0, size = filters.length; i < size; i++) {
						sb.append(filters[i].normalize());
					}

					break;
				}

				case OR : {
					sb.append('|');

					FilterImpl[] filters = (FilterImpl[]) value;
					for (int i = 0, size = filters.length; i < size; i++) {
						sb.append(filters[i].normalize());
					}

					break;
				}

				case NOT : {
					sb.append('!');
					FilterImpl filter = (FilterImpl) value;
					sb.append(filter.normalize());

					break;
				}

				case SUBSTRING : {
					sb.append(attr);
					sb.append('=');

					String[] substrings = (String[]) value;

					for (int i = 0, size = substrings.length; i < size; i++) {
						String substr = substrings[i];

						if (substr == null) /* * */{
							sb.append('*');
						}
						else /* xxx */{
							sb.append(encodeValue(substr));
						}
					}

					break;
				}
				case EQUAL : {
					sb.append(attr);
					sb.append('=');
					sb.append(encodeValue((String) value));

					break;
				}
				case GREATER : {
					sb.append(attr);
					sb.append(">=");
					sb.append(encodeValue((String) value));

					break;
				}
				case LESS : {
					sb.append(attr);
					sb.append("<=");
					sb.append(encodeValue((String) value));

					break;
				}
				case APPROX : {
					sb.append(attr);
					sb.append("~=");
					sb.append(encodeValue(approxString((String) value)));

					break;
				}

				case PRESENT : {
					sb.append(attr);
					sb.append("=*");

					break;
				}
			}

			sb.append(')');

			return sb.toString();
		}

		/**
		 * Compares this <code>Filter</code> to another <code>Filter</code>.
		 * 
		 * <p>
		 * This implementation returns the result of calling
		 * <code>this.toString().equals(obj.toString()</code>.
		 * 
		 * @param obj The object to compare against this <code>Filter</code>.
		 * @return If the other object is a <code>Filter</code> object, then
		 *         returns the result of calling
		 *         <code>this.toString().equals(obj.toString()</code>;
		 *         <code>false</code> otherwise.
		 */
		public boolean equals(Object obj) {
			if (obj == this) {
				return true;
			}

			if (!(obj instanceof Filter)) {
				return false;
			}

			return this.toString().equals(obj.toString());
		}

		/**
		 * Returns the hashCode for this <code>Filter</code>.
		 * 
		 * <p>
		 * This implementation returns the result of calling
		 * <code>this.toString().hashCode()</code>.
		 * 
		 * @return The hashCode of this <code>Filter</code>.
		 */
		public int hashCode() {
			return this.toString().hashCode();
		}

		/**
		 * Internal match routine. Dictionary parameter must support
		 * case-insensitive get.
		 * 
		 * @param properties A dictionary whose keys are used in the match.
		 * @return If the Dictionary's keys match the filter, return
		 *         <code>true</code>. Otherwise, return <code>false</code>.
		 */
		private boolean match0(Dictionary properties) {
			switch (op) {
				case AND : {
					FilterImpl[] filters = (FilterImpl[]) value;
					for (int i = 0, size = filters.length; i < size; i++) {
						if (!filters[i].match0(properties)) {
							return false;
						}
					}

					return true;
				}

				case OR : {
					FilterImpl[] filters = (FilterImpl[]) value;
					for (int i = 0, size = filters.length; i < size; i++) {
						if (filters[i].match0(properties)) {
							return true;
						}
					}

					return false;
				}

				case NOT : {
					FilterImpl filter = (FilterImpl) value;

					return !filter.match0(properties);
				}

				case SUBSTRING :
				case EQUAL :
				case GREATER :
				case LESS :
				case APPROX : {
					Object prop = (properties == null) ? null : properties
							.get(attr);

					return compare(op, prop, value);
				}

				case PRESENT : {
					Object prop = (properties == null) ? null : properties
							.get(attr);

					return prop != null;
				}
			}

			return false;
		}

		/**
		 * Encode the value string such that '(', '*', ')' and '\' are escaped.
		 * 
		 * @param value unencoded value string.
		 * @return encoded value string.
		 */
		private static String encodeValue(String value) {
			boolean encoded = false;
			int inlen = value.length();
			int outlen = inlen << 1; /* inlen 2 */

			char[] output = new char[outlen];
			value.getChars(0, inlen, output, inlen);

			int cursor = 0;
			for (int i = inlen; i < outlen; i++) {
				char c = output[i];

				switch (c) {
					case '(' :
					case '*' :
					case ')' :
					case '\\' : {
						output[cursor] = '\\';
						cursor++;
						encoded = true;

						break;
					}
				}

				output[cursor] = c;
				cursor++;
			}

			return encoded ? new String(output, 0, cursor) : value;
		}

		private boolean compare(int operation, Object value1, Object value2) {
			if (value1 == null) {
				return false;
			}
			if (value1 instanceof String) {
				return compare_String(operation, (String) value1, value2);
			}

			Class clazz = value1.getClass();
			if (clazz.isArray()) {
				Class type = clazz.getComponentType();
				if (type.isPrimitive()) {
					return compare_PrimitiveArray(operation, type, value1,
							value2);
				}
				return compare_ObjectArray(operation, (Object[]) value1, value2);
			}
			if (value1 instanceof Collection) {
				return compare_Collection(operation, (Collection) value1,
						value2);
			}
			if (value1 instanceof Integer) {
				return compare_Integer(operation,
						((Integer) value1).intValue(), value2);
			}
			if (value1 instanceof Long) {
				return compare_Long(operation, ((Long) value1).longValue(),
						value2);
			}
			if (value1 instanceof Byte) {
				return compare_Byte(operation, ((Byte) value1).byteValue(),
						value2);
			}
			if (value1 instanceof Short) {
				return compare_Short(operation, ((Short) value1).shortValue(),
						value2);
			}
			if (value1 instanceof Character) {
				return compare_Character(operation, ((Character) value1)
						.charValue(), value2);
			}
			if (value1 instanceof Float) {
				return compare_Float(operation, ((Float) value1).floatValue(),
						value2);
			}
			if (value1 instanceof Double) {
				return compare_Double(operation, ((Double) value1)
						.doubleValue(), value2);
			}
			if (value1 instanceof Boolean) {
				return compare_Boolean(operation, ((Boolean) value1)
						.booleanValue(), value2);
			}
			if (value1 instanceof Comparable) {
				return compare_Comparable(operation, (Comparable) value1,
						value2);
			}
			return compare_Unknown(operation, value1, value2); // RFC 59
		}

		private boolean compare_Collection(int operation,
				Collection collection, Object value2) {
			for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
				if (compare(operation, iterator.next(), value2)) {
					return true;
				}
			}
			return false;
		}

		private boolean compare_ObjectArray(int operation, Object[] array,
				Object value2) {
			for (int i = 0, size = array.length; i < size; i++) {
				if (compare(operation, array[i], value2)) {
					return true;
				}
			}
			return false;
		}

		private boolean compare_PrimitiveArray(int operation, Class type,
				Object primarray, Object value2) {
			if (Integer.TYPE.isAssignableFrom(type)) {
				int[] array = (int[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Integer(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Long.TYPE.isAssignableFrom(type)) {
				long[] array = (long[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Long(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Byte.TYPE.isAssignableFrom(type)) {
				byte[] array = (byte[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Byte(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Short.TYPE.isAssignableFrom(type)) {
				short[] array = (short[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Short(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Character.TYPE.isAssignableFrom(type)) {
				char[] array = (char[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Character(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Float.TYPE.isAssignableFrom(type)) {
				float[] array = (float[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Float(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Double.TYPE.isAssignableFrom(type)) {
				double[] array = (double[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Double(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			if (Boolean.TYPE.isAssignableFrom(type)) {
				boolean[] array = (boolean[]) primarray;
				for (int i = 0, size = array.length; i < size; i++) {
					if (compare_Boolean(operation, array[i], value2)) {
						return true;
					}
				}
				return false;
			}
			return false;
		}

		private boolean compare_String(int operation, String string,
				Object value2) {
			switch (operation) {
				case SUBSTRING : {
					String[] substrings = (String[]) value2;
					int pos = 0;
					for (int i = 0, size = substrings.length; i < size; i++) {
						String substr = substrings[i];

						if (i + 1 < size) /* if this is not that last substr */{
							if (substr == null) /* * */{
								String substr2 = substrings[i + 1];

								if (substr2 == null) /* ** */
									continue; /* ignore first star */
								/* xxx */
								int index = string.indexOf(substr2, pos);
								if (index == -1) {
									return false;
								}

								pos = index + substr2.length();
								if (i + 2 < size) // if there are more
									// substrings, increment
									// over the string we just
									// matched; otherwise need
									// to do the last substr
									// check
									i++;
							}
							else /* xxx */{
								int len = substr.length();
								if (string.regionMatches(pos, substr, 0, len)) {
									pos += len;
								}
								else {
									return false;
								}
							}
						}
						else /* last substr */{
							if (substr == null) /* * */{
								return true;
							}
							/* xxx */
							return string.endsWith(substr);
						}
					}

					return true;
				}
				case EQUAL : {
					return string.equals(value2);
				}
				case APPROX : {
					string = approxString(string);
					String string2 = approxString((String) value2);

					return string.equalsIgnoreCase(string2);
				}
				case GREATER : {
					return string.compareTo((String) value2) >= 0;
				}
				case LESS : {
					return string.compareTo((String) value2) <= 0;
				}
			}
			return false;
		}

		private boolean compare_Integer(int operation, int intval, Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			int intval2 = Integer.parseInt(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return intval == intval2;
				}
				case GREATER : {
					return intval >= intval2;
				}
				case LESS : {
					return intval <= intval2;
				}
			}
			return false;
		}

		private boolean compare_Long(int operation, long longval, Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			long longval2 = Long.parseLong(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return longval == longval2;
				}
				case GREATER : {
					return longval >= longval2;
				}
				case LESS : {
					return longval <= longval2;
				}
			}
			return false;
		}

		private boolean compare_Byte(int operation, byte byteval, Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			byte byteval2 = Byte.parseByte(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return byteval == byteval2;
				}
				case GREATER : {
					return byteval >= byteval2;
				}
				case LESS : {
					return byteval <= byteval2;
				}
			}
			return false;
		}

		private boolean compare_Short(int operation, short shortval,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			short shortval2 = Short.parseShort(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return shortval == shortval2;
				}
				case GREATER : {
					return shortval >= shortval2;
				}
				case LESS : {
					return shortval <= shortval2;
				}
			}
			return false;
		}

		private boolean compare_Character(int operation, char charval,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			char charval2 = (((String) value2).trim()).charAt(0);
			switch (operation) {
				case EQUAL : {
					return charval == charval2;
				}
				case APPROX : {
					return (charval == charval2)
							|| (Character.toUpperCase(charval) == Character
									.toUpperCase(charval2))
							|| (Character.toLowerCase(charval) == Character
									.toLowerCase(charval2));
				}
				case GREATER : {
					return charval >= charval2;
				}
				case LESS : {
					return charval <= charval2;
				}
			}
			return false;
		}

		private boolean compare_Boolean(int operation, boolean boolval,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			boolean boolval2 = Boolean.valueOf(((String) value2).trim())
					.booleanValue();
			switch (operation) {
				case APPROX :
				case EQUAL :
				case GREATER :
				case LESS : {
					return boolval == boolval2;
				}
			}
			return false;
		}

		private boolean compare_Float(int operation, float floatval,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			float floatval2 = Float.parseFloat(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return Float.compare(floatval, floatval2) == 0;
				}
				case GREATER : {
					return Float.compare(floatval, floatval2) >= 0;
				}
				case LESS : {
					return Float.compare(floatval, floatval2) <= 0;
				}
			}
			return false;
		}

		private boolean compare_Double(int operation, double doubleval,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			double doubleval2 = Double.parseDouble(((String) value2).trim());
			switch (operation) {
				case APPROX :
				case EQUAL : {
					return Double.compare(doubleval, doubleval2) == 0;
				}
				case GREATER : {
					return Double.compare(doubleval, doubleval2) >= 0;
				}
				case LESS : {
					return Double.compare(doubleval, doubleval2) <= 0;
				}
			}
			return false;
		}

		private static final Class[]	constructorType	= new Class[] {String.class};

		private boolean compare_Comparable(int operation, Comparable value1,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			Constructor constructor;
			try {
				constructor = value1.getClass().getConstructor(constructorType);
			}
			catch (NoSuchMethodException e) {
				return false;
			}
			try {
				if (!constructor.isAccessible())
					AccessController.doPrivileged(new SetAccessibleAction(
							constructor));
				value2 = constructor
						.newInstance(new Object[] {((String) value2).trim()});
			}
			catch (IllegalAccessException e) {
				return false;
			}
			catch (InvocationTargetException e) {
				return false;
			}
			catch (InstantiationException e) {
				return false;
			}

			switch (operation) {
				case APPROX :
				case EQUAL : {
					return value1.compareTo(value2) == 0;
				}
				case GREATER : {
					return value1.compareTo(value2) >= 0;
				}
				case LESS : {
					return value1.compareTo(value2) <= 0;
				}
			}
			return false;
		}

		private boolean compare_Unknown(int operation, Object value1,
				Object value2) {
			if (operation == SUBSTRING) {
				return false;
			}
			Constructor constructor;
			try {
				constructor = value1.getClass().getConstructor(constructorType);
			}
			catch (NoSuchMethodException e) {
				return false;
			}
			try {
				if (!constructor.isAccessible())
					AccessController.doPrivileged(new SetAccessibleAction(
							constructor));
				value2 = constructor
						.newInstance(new Object[] {((String) value2).trim()});
			}
			catch (IllegalAccessException e) {
				return false;
			}
			catch (InvocationTargetException e) {
				return false;
			}
			catch (InstantiationException e) {
				return false;
			}

			switch (operation) {
				case APPROX :
				case EQUAL :
				case GREATER :
				case LESS : {
					return value1.equals(value2);
				}
			}
			return false;
		}

		/**
		 * Map a string for an APPROX (~=) comparison.
		 * 
		 * This implementation removes white spaces. This is the minimum
		 * implementation allowed by the OSGi spec.
		 * 
		 * @param input Input string.
		 * @return String ready for APPROX comparison.
		 */
		private static String approxString(String input) {
			boolean changed = false;
			char[] output = input.toCharArray();
			int cursor = 0;
			for (int i = 0, length = output.length; i < length; i++) {
				char c = output[i];

				if (Character.isWhitespace(c)) {
					changed = true;
					continue;
				}

				output[cursor] = c;
				cursor++;
			}

			return changed ? new String(output, 0, cursor) : input;
		}

		/**
		 * Parser class for OSGi filter strings. This class parses the complete
		 * filter string and builds a tree of Filter objects rooted at the
		 * parent.
		 */
		private static class Parser {
			private final String	filterstring;
			private final char[]	filterChars;
			private int				pos;

			Parser(String filterstring) {
				this.filterstring = filterstring;
				filterChars = filterstring.toCharArray();
				pos = 0;
			}

			FilterImpl parse() throws InvalidSyntaxException {
				FilterImpl filter;
				try {
					filter = parse_filter();
				}
				catch (ArrayIndexOutOfBoundsException e) {
					throw new InvalidSyntaxException("Filter ended abruptly",
							filterstring);
				}

				if (pos != filterChars.length) {
					throw new InvalidSyntaxException(
							"Extraneous trailing characters: "
									+ filterstring.substring(pos), filterstring);
				}
				return filter;
			}

			private FilterImpl parse_filter() throws InvalidSyntaxException {
				FilterImpl filter;
				skipWhiteSpace();

				if (filterChars[pos] != '(') {
					throw new InvalidSyntaxException("Missing '(': "
							+ filterstring.substring(pos), filterstring);
				}

				pos++;

				filter = parse_filtercomp();

				skipWhiteSpace();

				if (filterChars[pos] != ')') {
					throw new InvalidSyntaxException("Missing ')': "
							+ filterstring.substring(pos), filterstring);
				}

				pos++;

				skipWhiteSpace();

				return filter;
			}

			private FilterImpl parse_filtercomp() throws InvalidSyntaxException {
				skipWhiteSpace();

				char c = filterChars[pos];

				switch (c) {
					case '&' : {
						pos++;
						return parse_and();
					}
					case '|' : {
						pos++;
						return parse_or();
					}
					case '!' : {
						pos++;
						return parse_not();
					}
				}
				return parse_item();
			}

			private FilterImpl parse_and() throws InvalidSyntaxException {
				skipWhiteSpace();

				if (filterChars[pos] != '(') {
					throw new InvalidSyntaxException("Missing '(': "
							+ filterstring.substring(pos), filterstring);
				}

				List operands = new ArrayList(10);

				while (filterChars[pos] == '(') {
					FilterImpl child = parse_filter();
					operands.add(child);
				}

				return new FilterImpl(FilterImpl.AND, null, operands
						.toArray(new FilterImpl[operands.size()]));
			}

			private FilterImpl parse_or() throws InvalidSyntaxException {
				skipWhiteSpace();

				if (filterChars[pos] != '(') {
					throw new InvalidSyntaxException("Missing '(': "
							+ filterstring.substring(pos), filterstring);
				}

				List operands = new ArrayList(10);

				while (filterChars[pos] == '(') {
					FilterImpl child = parse_filter();
					operands.add(child);
				}

				return new FilterImpl(FilterImpl.OR, null, operands
						.toArray(new FilterImpl[operands.size()]));
			}

			private FilterImpl parse_not() throws InvalidSyntaxException {
				skipWhiteSpace();

				if (filterChars[pos] != '(') {
					throw new InvalidSyntaxException("Missing '(': "
							+ filterstring.substring(pos), filterstring);
				}

				FilterImpl child = parse_filter();

				return new FilterImpl(FilterImpl.NOT, null, child);
			}

			private FilterImpl parse_item() throws InvalidSyntaxException {
				String attr = parse_attr();

				skipWhiteSpace();

				switch (filterChars[pos]) {
					case '~' : {
						if (filterChars[pos + 1] == '=') {
							pos += 2;
							return new FilterImpl(FilterImpl.APPROX, attr,
									parse_value());
						}
						break;
					}
					case '>' : {
						if (filterChars[pos + 1] == '=') {
							pos += 2;
							return new FilterImpl(FilterImpl.GREATER, attr,
									parse_value());
						}
						break;
					}
					case '<' : {
						if (filterChars[pos + 1] == '=') {
							pos += 2;
							return new FilterImpl(FilterImpl.LESS, attr,
									parse_value());
						}
						break;
					}
					case '=' : {
						if (filterChars[pos + 1] == '*') {
							int oldpos = pos;
							pos += 2;
							skipWhiteSpace();
							if (filterChars[pos] == ')') {
								return new FilterImpl(FilterImpl.PRESENT, attr,
										null);
							}
							pos = oldpos;
						}

						pos++;
						Object string = parse_substring();

						if (string instanceof String) {
							return new FilterImpl(FilterImpl.EQUAL, attr,
									string);
						}
						return new FilterImpl(FilterImpl.SUBSTRING, attr,
								string);
					}
				}

				throw new InvalidSyntaxException("Invalid operator: "
						+ filterstring.substring(pos), filterstring);
			}

			private String parse_attr() throws InvalidSyntaxException {
				skipWhiteSpace();

				int begin = pos;
				int end = pos;

				char c = filterChars[pos];

				while (c != '~' && c != '<' && c != '>' && c != '=' && c != '('
						&& c != ')') {
					pos++;

					if (!Character.isWhitespace(c)) {
						end = pos;
					}

					c = filterChars[pos];
				}

				int length = end - begin;

				if (length == 0) {
					throw new InvalidSyntaxException("Missing attr: "
							+ filterstring.substring(pos), filterstring);
				}

				return new String(filterChars, begin, length);
			}

			private String parse_value() throws InvalidSyntaxException {
				StringBuffer sb = new StringBuffer(filterChars.length - pos);

				parseloop: while (true) {
					char c = filterChars[pos];

					switch (c) {
						case ')' : {
							break parseloop;
						}

						case '(' : {
							throw new InvalidSyntaxException("Invalid value: "
									+ filterstring.substring(pos), filterstring);
						}

						case '\\' : {
							pos++;
							c = filterChars[pos];
							/* fall through into default */
						}

						default : {
							sb.append(c);
							pos++;
							break;
						}
					}
				}

				if (sb.length() == 0) {
					throw new InvalidSyntaxException("Missing value: "
							+ filterstring.substring(pos), filterstring);
				}

				return sb.toString();
			}

			private Object parse_substring() throws InvalidSyntaxException {
				StringBuffer sb = new StringBuffer(filterChars.length - pos);

				List operands = new ArrayList(10);

				parseloop: while (true) {
					char c = filterChars[pos];

					switch (c) {
						case ')' : {
							if (sb.length() > 0) {
								operands.add(sb.toString());
							}

							break parseloop;
						}

						case '(' : {
							throw new InvalidSyntaxException("Invalid value: "
									+ filterstring.substring(pos), filterstring);
						}

						case '*' : {
							if (sb.length() > 0) {
								operands.add(sb.toString());
							}

							sb.setLength(0);

							operands.add(null);
							pos++;

							break;
						}

						case '\\' : {
							pos++;
							c = filterChars[pos];
							/* fall through into default */
						}

						default : {
							sb.append(c);
							pos++;
							break;
						}
					}
				}

				int size = operands.size();

				if (size == 0) {
					throw new InvalidSyntaxException("Missing value: "
							+ filterstring.substring(pos), filterstring);
				}

				if (size == 1) {
					Object single = operands.get(0);

					if (single != null) {
						return single;
					}
				}

				return operands.toArray(new String[size]);
			}

			private void skipWhiteSpace() {
				for (int length = filterChars.length; (pos < length)
						&& Character.isWhitespace(filterChars[pos]);) {
					pos++;
				}
			}
		}
	}

	/**
	 * This Dictionary is used for case-insensitive key lookup during filter
	 * evaluation. This Dictionary implementation only supports the get
	 * operation using a String key as no other operations are used by the
	 * Filter implementation.
	 */
	private static class CaseInsensitiveDictionary extends Dictionary {
		private final Dictionary	dictionary;
		private final String[]		keys;

		/**
		 * Create a case insensitive dictionary from the specified dictionary.
		 * 
		 * @param dictionary
		 * @throws IllegalArgumentException If <code>dictionary</code> contains
		 *         case variants of the same key name.
		 */
		CaseInsensitiveDictionary(Dictionary dictionary) {
			if (dictionary == null) {
				this.dictionary = null;
				this.keys = new String[0];
				return;
			}
			this.dictionary = dictionary;
			List keyList = new ArrayList(dictionary.size());
			for (Enumeration e = dictionary.keys(); e.hasMoreElements();) {
				Object k = e.nextElement();
				if (k instanceof String) {
					String key = (String) k;
					for (Iterator i = keyList.iterator(); i.hasNext();) {
						if (key.equalsIgnoreCase((String) i.next())) {
							throw new IllegalArgumentException();
						}
					}
					keyList.add(key);
				}
			}
			this.keys = (String[]) keyList.toArray(new String[keyList.size()]);
		}

		public Object get(Object o) {
			String k = (String) o;
			for (int i = 0, length = keys.length; i < length; i++) {
				String key = keys[i];
				if (key.equalsIgnoreCase(k)) {
					return dictionary.get(key);
				}
			}
			return null;
		}

		public boolean isEmpty() {
			throw new UnsupportedOperationException();
		}

		public Enumeration keys() {
			throw new UnsupportedOperationException();
		}

		public Enumeration elements() {
			throw new UnsupportedOperationException();
		}

		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * This Dictionary is used for key lookup from a ServiceReference during
	 * filter evaluation. This Dictionary implementation only supports the get
	 * operation using a String key as no other operations are used by the
	 * Filter implementation.
	 */
	private static class ServiceReferenceDictionary extends Dictionary {
		private final ServiceReference	reference;

		ServiceReferenceDictionary(ServiceReference reference) {
			this.reference = reference;
		}

		public Object get(Object key) {
			if (reference == null) {
				return null;
			}
			return reference.getProperty((String) key);
		}

		public boolean isEmpty() {
			throw new UnsupportedOperationException();
		}

		public Enumeration keys() {
			throw new UnsupportedOperationException();
		}

		public Enumeration elements() {
			throw new UnsupportedOperationException();
		}

		public Object put(Object key, Object value) {
			throw new UnsupportedOperationException();
		}

		public Object remove(Object key) {
			throw new UnsupportedOperationException();
		}

		public int size() {
			throw new UnsupportedOperationException();
		}
	}

	private static class SetAccessibleAction implements PrivilegedAction {
		private final AccessibleObject	accessible;

		SetAccessibleAction(AccessibleObject accessible) {
			this.accessible = accessible;
		}

		public Object run() {
			accessible.setAccessible(true);
			return null;
		}
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
