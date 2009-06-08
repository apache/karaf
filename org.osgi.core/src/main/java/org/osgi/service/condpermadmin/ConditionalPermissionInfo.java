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

package org.osgi.service.condpermadmin;

import org.osgi.service.permissionadmin.PermissionInfo;

/**
 * A list of Permissions guarded by a list of conditions with an access
 * decision. Instances of this interface are obtained from the Conditional
 * Permission Admin service.
 * 
 * @Immutable
 * @version $Revision: 6492 $
 */
public interface ConditionalPermissionInfo {
	/**
	 * This string is used to indicate that a row in the Conditional Permission
	 * Table should return an access decision of &quot;allow&quot; if the
	 * conditions are all satisfied and at least one of the permissions is
	 * implied.
	 * 
	 * @since 1.1
	 */
	public final static String	ALLOW	= "allow";

	/**
	 * This string is used to indicate that a row in the Conditional Permission
	 * Table should return an access decision of &quot;deny&quot; if the
	 * conditions are all satisfied and at least one of the permissions is
	 * implied.
	 * 
	 * @since 1.1
	 */
	public final static String	DENY	= "deny";

	/**
	 * Returns the Condition Infos for the Conditions that must be satisfied to
	 * enable the Permissions.
	 * 
	 * @return The Condition Infos for the Conditions in this Conditional
	 *         Permission Info.
	 */
	ConditionInfo[] getConditionInfos();

	/**
	 * Returns the Permission Infos for the Permissions in this Conditional
	 * Permission Info.
	 * 
	 * @return The Permission Infos for the Permissions in this Conditional
	 *         Permission Info.
	 */
	PermissionInfo[] getPermissionInfos();

	/**
	 * Removes this Conditional Permission Info from the Conditional Permission
	 * Table.
	 * <p>
	 * Since this method changes the underlying permission table, any
	 * {@link ConditionalPermissionUpdate}s that were created prior to calling
	 * this method can no longer be committed.
	 * 
	 * @throws UnsupportedOperationException If this object was created by
	 *         {@link ConditionalPermissionAdmin#newConditionalPermissionInfo}
	 *         or obtained from a {@link ConditionalPermissionUpdate}. This
	 *         method only functions if this object was obtained from one of the
	 *         {@link ConditionalPermissionAdmin} methods deprecated in version
	 *         1.1.
	 * @throws SecurityException If the caller does not have
	 *         <code>AllPermission</code>.
	 * @deprecated Since 1.1. Use
	 *             {@link ConditionalPermissionAdmin#newConditionalPermissionUpdate()}
	 *             instead to manage the Conditional Permissions.
	 */
	void delete();

	/**
	 * Returns the name of this Conditional Permission Info.
	 * 
	 * @return The name of this Conditional Permission Info. This can be
	 *         <code>null</code> if this Conditional Permission Info was created
	 *         without a name.
	 */
	String getName();

	/**
	 * Returns the access decision for this Conditional Permission Info.
	 * 
	 * @return One of the following values:
	 *         <ul>
	 *         <li>{@link #ALLOW allow} - The access decision is
	 *         &quot;allow&quot;.</li>
	 *         <li>{@link #DENY deny} - The access decision is &quot;deny&quot;.
	 *         </li>
	 *         </ul>
	 * @since 1.1
	 */
	String getAccessDecision();

	/**
	 * Returns the string encoding of this
	 * <code>ConditionalPermissionInfo</code> in a form suitable for restoring
	 * this <code>ConditionalPermissionInfo</code>.
	 * 
	 * <p>
	 * The encoded format is:
	 * 
	 * <pre>
	 *   access {conditions permissions} name
	 * </pre>
	 * 
	 * where <i>access</i> is the access decision, <i>conditions</i> is zero or
	 * more {@link ConditionInfo#getEncoded() encoded conditions},
	 * <i>permissions</i> is one or more {@link PermissionInfo#getEncoded()
	 * encoded permissions} and <i>name</i> is the name of the
	 * <code>ConditionalPermissionInfo</code>.
	 * 
	 * <p>
	 * <i>name</i> is optional. If <i>name</i> is present in the encoded string,
	 * it must quoted, beginning and ending with <code>&quot;</code>. The
	 * <i>name</i> value must be encoded for proper parsing. Specifically, the
	 * <code>&quot;</code>, <code>\</code>, carriage return, and line feed
	 * characters must be escaped using <code>\&quot;</code>, <code>\\</code>,
	 * <code>\r</code>, and <code>\n</code>, respectively.
	 * 
	 * <p>
	 * The encoded string contains no leading or trailing whitespace characters.
	 * A single space character is used between <i>access</i> and <code>{</code>
	 * and between <code>}</code> and <i>name</i>, if <i>name</i> is present.
	 * All encoded conditions and permissions are separated by a single space
	 * character.
	 * 
	 * @return The string encoding of this
	 *         <code>ConditionalPermissionInfo</code>.
	 * @since 1.1
	 */
	String getEncoded();

	/**
	 * Returns the string representation of this
	 * <code>ConditionalPermissionInfo</code>. The string is created by calling
	 * the <code>getEncoded</code> method on this
	 * <code>ConditionalPermissionInfo</code>.
	 * 
	 * @return The string representation of this
	 *         <code>ConditionalPermissionInfo</code>.
	 * @since 1.1
	 */
	String toString();

	/**
	 * Determines the equality of two <code>ConditionalPermissionInfo</code>
	 * objects.
	 * 
	 * This method checks that specified object has the same access decision,
	 * conditions, permissions and name as this
	 * <code>ConditionalPermissionInfo</code> object.
	 * 
	 * @param obj The object to test for equality with this
	 *        <code>ConditionalPermissionInfo</code> object.
	 * @return <code>true</code> if <code>obj</code> is a
	 *         <code>ConditionalPermissionInfo</code>, and has the same access
	 *         decision, conditions, permissions and name as this
	 *         <code>ConditionalPermissionInfo</code> object; <code>false</code>
	 *         otherwise.
	 * @since 1.1
	 */
	 boolean equals(Object obj);
 
	/**
	 * Returns the hash code value for this object.
	 * 
	 * @return A hash code value for this object.
	 * @since 1.1
	 */
	int hashCode();
}
