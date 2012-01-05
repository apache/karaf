/*
 * Copyright (c) OSGi Alliance (2000, 2010). All Rights Reserved.
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

import java.util.Dictionary;
import java.util.Map;

/**
 * An <a href="http://www.ietf.org/rfc/rfc1960.txt">RFC 1960</a>-based Filter.
 * <p>
 * {@code Filter}s can be created by calling {@link BundleContext#createFilter}
 * or {@link FrameworkUtil#createFilter} with a filter string.
 * <p>
 * A {@code Filter} can be used numerous times to determine if the match
 * argument matches the filter string that was used to create the {@code Filter}.
 * <p>
 * Some examples of LDAP filters are:
 * 
 * <pre>
 *  &quot;(cn=Babs Jensen)&quot;
 *  &quot;(!(cn=Tim Howes))&quot;
 *  &quot;(&amp;(&quot; + Constants.OBJECTCLASS + &quot;=Person)(|(sn=Jensen)(cn=Babs J*)))&quot;
 *  &quot;(o=univ*of*mich*)&quot;
 * </pre>
 * 
 * @since 1.1
 * @see "Core Specification, Filters, for a description of the filter string syntax."
 * @ThreadSafe
 * @noimplement
 * @version $Id: 4d21267f4b85d1912d73f7e2c049cc968c4237f9 $
 */
public interface Filter {
	/**
	 * Filter using a service's properties.
	 * <p>
	 * This {@code Filter} is executed using the keys and values of the
	 * referenced service's properties. The keys are looked up in a case
	 * insensitive manner.
	 * 
	 * @param reference The reference to the service whose properties are used
	 *        in the match.
	 * @return {@code true} if the service's properties match this
	 *         {@code Filter}; {@code false} otherwise.
	 */
	boolean match(ServiceReference< ? > reference);

	/**
	 * Filter using a {@code Dictionary} with case insensitive key lookup. This
	 * {@code Filter} is executed using the specified {@code Dictionary}'s keys
	 * and values. The keys are looked up in a case insensitive manner.
	 * 
	 * @param dictionary The {@code Dictionary} whose key/value pairs are used
	 *        in the match.
	 * @return {@code true} if the {@code Dictionary}'s values match this
	 *         filter; {@code false} otherwise.
	 * @throws IllegalArgumentException If {@code dictionary} contains case
	 *         variants of the same key name.
	 */
	boolean match(Dictionary<String, ? > dictionary);

	/**
	 * Returns this {@code Filter}'s filter string.
	 * <p>
	 * The filter string is normalized by removing whitespace which does not
	 * affect the meaning of the filter.
	 * 
	 * @return This {@code Filter}'s filter string.
	 */
	String toString();

	/**
	 * Compares this {@code Filter} to another {@code Filter}.
	 * 
	 * <p>
	 * This implementation returns the result of calling
	 * {@code this.toString().equals(obj.toString())}.
	 * 
	 * @param obj The object to compare against this {@code Filter}.
	 * @return If the other object is a {@code Filter} object, then returns
	 *         the result of calling
	 *         {@code this.toString().equals(obj.toString())};
	 *         {@code false} otherwise.
	 */
	boolean equals(Object obj);

	/**
	 * Returns the hashCode for this {@code Filter}.
	 * 
	 * <p>
	 * This implementation returns the result of calling
	 * {@code this.toString().hashCode()}.
	 * 
	 * @return The hashCode of this {@code Filter}.
	 */
	int hashCode();

	/**
	 * Filter using a {@code Dictionary}. This {@code Filter} is executed using
	 * the specified {@code Dictionary}'s keys and values. The keys are looked
	 * up in a normal manner respecting case.
	 * 
	 * @param dictionary The {@code Dictionary} whose key/value pairs are used
	 *        in the match.
	 * @return {@code true} if the {@code Dictionary}'s values match this
	 *         filter; {@code false} otherwise.
	 * @since 1.3
	 */
	boolean matchCase(Dictionary<String, ? > dictionary);

	/**
	 * Filter using a {@code Map}. This {@code Filter} is executed using the
	 * specified {@code Map}'s keys and values. The keys are looked up in a
	 * normal manner respecting case.
	 * 
	 * @param map The {@code Map} whose key/value pairs are used in the match.
	 *        Maps with {@code null} key or values are not supported. A
	 *        {@code null} value is considered not present to the filter.
	 * @return {@code true} if the {@code Map}'s values match this filter;
	 *         {@code false} otherwise.
	 * @since 1.6
	 */
	boolean matches(Map<String, ? > map);
}
