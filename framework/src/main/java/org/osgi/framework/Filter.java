/*
 * Copyright (c) OSGi Alliance (2000, 2009). All Rights Reserved.
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

/**
 * An RFC 1960-based Filter.
 * <p>
 * <code>Filter</code>s can be created by calling
 * {@link BundleContext#createFilter} or {@link FrameworkUtil#createFilter} with
 * a filter string.
 * <p>
 * A <code>Filter</code> can be used numerous times to determine if the match
 * argument matches the filter string that was used to create the
 * <code>Filter</code>.
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
 * @see "Core Specification, section 5.5, for a description of the filter string syntax."
 * @ThreadSafe
 * @version $Revision: 6860 $
 */
public interface Filter {
	/**
	 * Filter using a service's properties.
	 * <p>
	 * This <code>Filter</code> is executed using the keys and values of the
	 * referenced service's properties. The keys are case insensitively matched
	 * with this <code>Filter</code>.
	 * 
	 * @param reference The reference to the service whose properties are used
	 *        in the match.
	 * @return <code>true</code> if the service's properties match this
	 *         <code>Filter</code>; <code>false</code> otherwise.
	 */
	public boolean match(ServiceReference reference);

	/**
	 * Filter using a <code>Dictionary</code>. This <code>Filter</code> is
	 * executed using the specified <code>Dictionary</code>'s keys and values.
	 * The keys are case insensitively matched with this <code>Filter</code>.
	 * 
	 * @param dictionary The <code>Dictionary</code> whose keys are used in the
	 *        match.
	 * @return <code>true</code> if the <code>Dictionary</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 * @throws IllegalArgumentException If <code>dictionary</code> contains case
	 *         variants of the same key name.
	 */
	public boolean match(Dictionary dictionary);

	/**
	 * Returns this <code>Filter</code>'s filter string.
	 * <p>
	 * The filter string is normalized by removing whitespace which does not
	 * affect the meaning of the filter.
	 * 
	 * @return This <code>Filter</code>'s filter string.
	 */
	public String toString();

	/**
	 * Compares this <code>Filter</code> to another <code>Filter</code>.
	 * 
	 * <p>
	 * This method returns the result of calling
	 * <code>this.toString().equals(obj.toString())</code>.
	 * 
	 * @param obj The object to compare against this <code>Filter</code>.
	 * @return If the other object is a <code>Filter</code> object, then returns
	 *         the result of calling
	 *         <code>this.toString().equals(obj.toString())</code>;
	 *         <code>false</code> otherwise.
	 */
	public boolean equals(Object obj);

	/**
	 * Returns the hashCode for this <code>Filter</code>.
	 * 
	 * <p>
	 * This method returns the result of calling
	 * <code>this.toString().hashCode()</code>.
	 * 
	 * @return The hashCode of this <code>Filter</code>.
	 */
	public int hashCode();

	/**
	 * Filter with case sensitivity using a <code>Dictionary</code>. This
	 * <code>Filter</code> is executed using the specified
	 * <code>Dictionary</code>'s keys and values. The keys are case sensitively
	 * matched with this <code>Filter</code>.
	 * 
	 * @param dictionary The <code>Dictionary</code> whose keys are used in the
	 *        match.
	 * @return <code>true</code> if the <code>Dictionary</code>'s keys and
	 *         values match this filter; <code>false</code> otherwise.
	 * @since 1.3
	 */
	public boolean matchCase(Dictionary dictionary);
}
