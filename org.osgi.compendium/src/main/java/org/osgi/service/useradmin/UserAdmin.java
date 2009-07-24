/*
 * Copyright (c) OSGi Alliance (2001, 2008). All Rights Reserved.
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
package org.osgi.service.useradmin;

import org.osgi.framework.InvalidSyntaxException;

/**
 * This interface is used to manage a database of named <code>Role</code> objects,
 * which can be used for authentication and authorization purposes.
 * 
 * <p>
 * This version of the User Admin service defines two types of <code>Role</code>
 * objects: "User" and "Group". Each type of role is represented by an
 * <code>int</code> constant and an interface. The range of positive integers is
 * reserved for new types of roles that may be added in the future. When
 * defining proprietary role types, negative constant values must be used.
 * 
 * <p>
 * Every role has a name and a type.
 * 
 * <p>
 * A {@link User} object can be configured with credentials (e.g., a password)
 * and properties (e.g., a street address, phone number, etc.).
 * <p>
 * A {@link Group} object represents an aggregation of {@link User} and
 * {@link Group} objects. In other words, the members of a <code>Group</code>
 * object are roles themselves.
 * <p>
 * Every User Admin service manages and maintains its own namespace of
 * <code>Role</code> objects, in which each <code>Role</code> object has a unique
 * name.
 * 
 * @version $Revision: 5673 $
 */
public interface UserAdmin {
	/**
	 * Creates a <code>Role</code> object with the given name and of the given
	 * type.
	 * 
	 * <p>
	 * If a <code>Role</code> object was created, a <code>UserAdminEvent</code>
	 * object of type {@link UserAdminEvent#ROLE_CREATED} is broadcast to any
	 * <code>UserAdminListener</code> object.
	 * 
	 * @param name The <code>name</code> of the <code>Role</code> object to create.
	 * @param type The type of the <code>Role</code> object to create. Must be
	 *        either a {@link Role#USER} type or {@link Role#GROUP} type.
	 * 
	 * @return The newly created <code>Role</code> object, or <code>null</code> if a
	 *         role with the given name already exists.
	 * 
	 * @throws IllegalArgumentException if <code>type</code> is invalid.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> with name
	 *         <code>admin</code>.
	 */
	public Role createRole(String name, int type);

	/**
	 * Removes the <code>Role</code> object with the given name from this User
	 * Admin service and all groups it is a member of.
	 * 
	 * <p>
	 * If the <code>Role</code> object was removed, a <code>UserAdminEvent</code>
	 * object of type {@link UserAdminEvent#ROLE_REMOVED} is broadcast to any
	 * <code>UserAdminListener</code> object.
	 * 
	 * @param name The name of the <code>Role</code> object to remove.
	 * 
	 * @return <code>true</code> If a <code>Role</code> object with the given name
	 *         is present in this User Admin service and could be removed,
	 *         otherwise <code>false</code>.
	 * 
	 * @throws SecurityException If a security manager exists and the caller
	 *         does not have the <code>UserAdminPermission</code> with name
	 *         <code>admin</code>.
	 */
	public boolean removeRole(String name);

	/**
	 * Gets the <code>Role</code> object with the given <code>name</code> from this
	 * User Admin service.
	 * 
	 * @param name The name of the <code>Role</code> object to get.
	 * 
	 * @return The requested <code>Role</code> object, or <code>null</code> if this
	 *         User Admin service does not have a <code>Role</code> object with
	 *         the given <code>name</code>.
	 */
	public Role getRole(String name);

	/**
	 * Gets the <code>Role</code> objects managed by this User Admin service that
	 * have properties matching the specified LDAP filter criteria. See
	 * <code>org.osgi.framework.Filter</code> for a description of the filter
	 * syntax. If a <code>null</code> filter is specified, all Role objects
	 * managed by this User Admin service are returned.
	 * 
	 * @param filter The filter criteria to match.
	 * 
	 * @return The <code>Role</code> objects managed by this User Admin service
	 *         whose properties match the specified filter criteria, or all
	 *         <code>Role</code> objects if a <code>null</code> filter is specified.
	 *         If no roles match the filter, <code>null</code> will be returned.
	 * @throws InvalidSyntaxException If the filter is not well formed.
	 *  
	 */
	public Role[] getRoles(String filter) throws InvalidSyntaxException;

	/**
	 * Gets the user with the given property <code>key</code>-<code>value</code>
	 * pair from the User Admin service database. This is a convenience method
	 * for retrieving a <code>User</code> object based on a property for which
	 * every <code>User</code> object is supposed to have a unique value (within
	 * the scope of this User Admin service), such as for example a X.500
	 * distinguished name.
	 * 
	 * @param key The property key to look for.
	 * @param value The property value to compare with.
	 * 
	 * @return A matching user, if <em>exactly</em> one is found. If zero or
	 *         more than one matching users are found, <code>null</code> is
	 *         returned.
	 */
	public User getUser(String key, String value);

	/**
	 * Creates an <code>Authorization</code> object that encapsulates the
	 * specified <code>User</code> object and the <code>Role</code> objects it
	 * possesses. The <code>null</code> user is interpreted as the anonymous user.
	 * The anonymous user represents a user that has not been authenticated. An
	 * <code>Authorization</code> object for an anonymous user will be unnamed,
	 * and will only imply groups that user.anyone implies.
	 * 
	 * @param user The <code>User</code> object to create an
	 *        <code>Authorization</code> object for, or <code>null</code> for the
	 *        anonymous user.
	 * 
	 * @return the <code>Authorization</code> object for the specified
	 *         <code>User</code> object.
	 */
	public Authorization getAuthorization(User user);
}
